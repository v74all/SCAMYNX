package com.v7lthronyx.scamynx.data.repository

import com.v7lthronyx.scamynx.data.analyzer.FileStaticAnalyzer
import com.v7lthronyx.scamynx.data.analyzer.InstagramScamAnalyzer
import com.v7lthronyx.scamynx.data.analyzer.VpnConfigAnalyzer
import com.v7lthronyx.scamynx.data.db.ScanDao
import com.v7lthronyx.scamynx.data.db.ScanEntity
import com.v7lthronyx.scamynx.data.db.ScanWithVerdicts
import com.v7lthronyx.scamynx.data.db.VendorVerdictEntity
import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.data.di.ThreatIntelJson
import com.v7lthronyx.scamynx.data.heuristics.LocalHeuristicsEvaluator
import com.v7lthronyx.scamynx.data.network.api.GoogleSafeBrowsingApi
import com.v7lthronyx.scamynx.data.network.api.PhishStatsApi
import com.v7lthronyx.scamynx.data.network.api.ThreatFoxApi
import com.v7lthronyx.scamynx.data.network.api.UrlHausApi
import com.v7lthronyx.scamynx.data.network.api.UrlScanApi
import com.v7lthronyx.scamynx.data.network.api.VirusTotalApi
import com.v7lthronyx.scamynx.data.network.model.GoogleSafeBrowsingRequestDto
import com.v7lthronyx.scamynx.data.network.model.ThreatFoxSearchRequestDto
import com.v7lthronyx.scamynx.data.network.model.UrlHausLookupRequestDto
import com.v7lthronyx.scamynx.data.network.model.UrlScanSubmitRequestDto
import com.v7lthronyx.scamynx.data.network.model.UrlScanResultDto
import com.v7lthronyx.scamynx.data.network.model.VirusTotalSubmitRequestDto
import com.v7lthronyx.scamynx.data.util.ApiCredentials
import com.v7lthronyx.scamynx.data.util.UrlNormalizer
import com.v7lthronyx.scamynx.domain.model.FileScanReport
import com.v7lthronyx.scamynx.domain.model.InstagramScanReport
import com.v7lthronyx.scamynx.domain.model.MlReport
import com.v7lthronyx.scamynx.domain.model.NetworkReport
import com.v7lthronyx.scamynx.domain.model.Provider
import com.v7lthronyx.scamynx.domain.model.RiskBreakdown
import com.v7lthronyx.scamynx.domain.model.RiskCategory
import com.v7lthronyx.scamynx.domain.model.ScanRequest
import com.v7lthronyx.scamynx.domain.model.ScanResult
import com.v7lthronyx.scamynx.domain.model.ScanStage
import com.v7lthronyx.scamynx.domain.model.ScanState
import com.v7lthronyx.scamynx.domain.model.ScanTargetType
import com.v7lthronyx.scamynx.domain.model.VendorVerdict
import com.v7lthronyx.scamynx.domain.model.VerdictStatus
import com.v7lthronyx.scamynx.domain.model.VpnConfigReport
import com.v7lthronyx.scamynx.domain.repository.ScanRepository
import com.v7lthronyx.scamynx.domain.service.MlAnalyzer
import com.v7lthronyx.scamynx.domain.service.NetworkSecurityAnalyzer
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.LinkedHashMap
import java.util.UUID
import kotlin.collections.buildMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.min
import kotlin.math.pow

@Singleton
class ScanRepositoryImpl @Inject constructor(
    private val virusTotalApi: VirusTotalApi,
    private val googleSafeBrowsingApi: GoogleSafeBrowsingApi,
    private val urlScanApi: UrlScanApi,
    private val urlHausApi: UrlHausApi,
    private val phishStatsApi: PhishStatsApi,
    private val threatFoxApi: ThreatFoxApi,
    private val networkSecurityAnalyzer: NetworkSecurityAnalyzer,
    private val mlAnalyzer: MlAnalyzer,
    private val heuristicsEvaluator: LocalHeuristicsEvaluator,
    private val fileStaticAnalyzer: FileStaticAnalyzer,
    private val vpnConfigAnalyzer: VpnConfigAnalyzer,
    private val instagramAnalyzer: InstagramScamAnalyzer,
    private val scanDao: ScanDao,
    private val urlNormalizer: UrlNormalizer,
    private val credentials: ApiCredentials,
    @ThreatIntelJson private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ScanRepository {

    override fun analyze(request: ScanRequest): Flow<ScanState> = flow {
        val sessionId = UUID.randomUUID().toString()
        emit(ScanState.Progress(sessionId, ScanStage.INITIALIZING))

        val progressEmitter: suspend (ScanStage, String?) -> Unit = { stage, message ->
            emit(ScanState.Progress(sessionId, stage, message))
        }

        val result = runCatching {
            when (request.targetType) {
                ScanTargetType.URL -> analyzeUrl(sessionId, request.rawInput, progressEmitter)
                ScanTargetType.FILE -> analyzeFile(sessionId, request, progressEmitter)
                ScanTargetType.VPN_CONFIG -> analyzeVpnConfig(sessionId, request, progressEmitter)
                ScanTargetType.INSTAGRAM -> analyzeInstagram(sessionId, request, progressEmitter)
            }
        }.getOrElse { throwable ->
            if (throwable is kotlinx.coroutines.CancellationException) throw throwable
            emit(ScanState.Failure(sessionId, throwable))
            return@flow
        }

        withContext(ioDispatcher) {
            persistResult(sessionId, result)
        }

        emit(ScanState.Success(sessionId, result))
    }.flowOn(ioDispatcher)

    override fun observeHistory(limit: Int): Flow<List<ScanResult>> =
        scanDao.observeHistory(limit = limit, offset = 0)
            .map { entries -> entries.map { it.toDomain(json) } }
            .flowOn(ioDispatcher)

    override suspend fun getHistory(limit: Int, offset: Int): List<ScanResult> = withContext(ioDispatcher) {
        scanDao.getHistory(limit, offset).map { it.toDomain(json) }
    }

    override suspend fun getScan(sessionId: String): ScanResult? = withContext(ioDispatcher) {
        scanDao.getScan(sessionId)?.toDomain(json)
    }

    override suspend fun clearHistory() = withContext(ioDispatcher) {
        scanDao.clearAllVerdicts()
        scanDao.clearAllScans()
    }

    private suspend fun analyzeUrl(
        sessionId: String,
        rawUrl: String,
        progress: suspend (ScanStage, String?) -> Unit,
    ): ScanResult {
        val normalizedUrl = urlNormalizer.normalize(rawUrl)
        progress(ScanStage.NORMALIZING, null)

        val (vendorVerdicts, networkReport, mlReport) = coroutineScope {
            progress(ScanStage.FETCHING_THREAT_INTEL, null)
            val threatIntel = async(ioDispatcher) { collectThreatIntel(normalizedUrl) }
            val network = async(ioDispatcher) { runNetworkAnalysis(normalizedUrl) }
            val ml = async(ioDispatcher) { runMlAnalysis(normalizedUrl) }

            val vendors = threatIntel.await()
            progress(ScanStage.ANALYZING_NETWORK_SECURITY, null)
            val networkResult = network.await()
            progress(ScanStage.RUNNING_ML, null)
            val mlResult = ml.await()
            Triple(vendors, networkResult, mlResult)
        }

        progress(ScanStage.AGGREGATING, null)
        val (riskScore, breakdown) = RiskScorer.aggregateRisk(
            targetType = ScanTargetType.URL,
            vendorVerdicts = vendorVerdicts,
            mlReport = mlReport,
            networkReport = networkReport,
            fileReport = null,
            vpnReport = null,
            instagramReport = null,
        )

        return ScanResult(
            sessionId = sessionId,
            targetType = ScanTargetType.URL,
            targetLabel = normalizedUrl,
            normalizedUrl = normalizedUrl,
            vendors = vendorVerdicts,
            network = networkReport,
            ml = mlReport,
            file = null,
            vpn = null,
            instagram = null,
            risk = riskScore,
            breakdown = breakdown,
            createdAt = Clock.System.now(),
        )
    }

    private suspend fun analyzeFile(
        sessionId: String,
        request: ScanRequest,
        progress: suspend (ScanStage, String?) -> Unit,
    ): ScanResult {
        progress(ScanStage.ANALYZING_FILE, null)
        val fileBytes = decodeFileBytes(request.rawInput)
        val sizeBytes = request.metadata["sizeBytes"]?.toLongOrNull()
            ?: fileBytes?.size?.toLong()
        val analysis = fileStaticAnalyzer.analyze(
            FileStaticAnalyzer.Input(
                content = fileBytes,
                fileName = request.metadata["fileName"],
                mimeType = request.metadata["mimeType"],
                sizeBytes = sizeBytes,
            ),
        )

        val vendorVerdicts = listOf(analysis.verdict)
        progress(ScanStage.AGGREGATING, null)
        val (riskScore, breakdown) = RiskScorer.aggregateRisk(
            targetType = ScanTargetType.FILE,
            vendorVerdicts = vendorVerdicts,
            mlReport = null,
            networkReport = null,
            fileReport = analysis.report,
            vpnReport = null,
            instagramReport = null,
        )

        return ScanResult(
            sessionId = sessionId,
            targetType = ScanTargetType.FILE,
            targetLabel = analysis.report.fileName,
            normalizedUrl = null,
            vendors = vendorVerdicts,
            network = null,
            ml = null,
            file = analysis.report,
            vpn = null,
            instagram = null,
            risk = riskScore,
            breakdown = breakdown,
            createdAt = Clock.System.now(),
        )
    }

    private suspend fun analyzeVpnConfig(
        sessionId: String,
        request: ScanRequest,
        progress: suspend (ScanStage, String?) -> Unit,
    ): ScanResult {
        progress(ScanStage.ANALYZING_VPN_CONFIG, null)
        val analysis = vpnConfigAnalyzer.analyze(request.rawInput)
        val vendorVerdicts = listOf(analysis.verdict)

        progress(ScanStage.AGGREGATING, null)
        val (riskScore, breakdown) = RiskScorer.aggregateRisk(
            targetType = ScanTargetType.VPN_CONFIG,
            vendorVerdicts = vendorVerdicts,
            mlReport = null,
            networkReport = null,
            fileReport = null,
            vpnReport = analysis.report,
            instagramReport = null,
        )

        val label = request.metadata["profileName"]
            ?: analysis.report.serverAddress
            ?: "VPN configuration"

        return ScanResult(
            sessionId = sessionId,
            targetType = ScanTargetType.VPN_CONFIG,
            targetLabel = label,
            normalizedUrl = null,
            vendors = vendorVerdicts,
            network = null,
            ml = null,
            file = null,
            vpn = analysis.report,
            instagram = null,
            risk = riskScore,
            breakdown = breakdown,
            createdAt = Clock.System.now(),
        )
    }

    private suspend fun analyzeInstagram(
        sessionId: String,
        request: ScanRequest,
        progress: suspend (ScanStage, String?) -> Unit,
    ): ScanResult {
        progress(ScanStage.ANALYZING_INSTAGRAM, null)
        val analysis = instagramAnalyzer.analyze(request.rawInput, request.metadata)
        val vendorVerdicts = listOf(analysis.verdict)

        progress(ScanStage.AGGREGATING, null)
        val (riskScore, breakdown) = RiskScorer.aggregateRisk(
            targetType = ScanTargetType.INSTAGRAM,
            vendorVerdicts = vendorVerdicts,
            mlReport = null,
            networkReport = null,
            fileReport = null,
            vpnReport = null,
            instagramReport = analysis.report,
        )

        return ScanResult(
            sessionId = sessionId,
            targetType = ScanTargetType.INSTAGRAM,
            targetLabel = analysis.report.handle,
            normalizedUrl = null,
            vendors = vendorVerdicts,
            network = null,
            ml = null,
            file = null,
            vpn = null,
            instagram = analysis.report,
            risk = riskScore,
            breakdown = breakdown,
            createdAt = Clock.System.now(),
        )
    }

    private suspend fun collectThreatIntel(url: String): List<VendorVerdict> = coroutineScope {
        val initialVerdicts = listOf(
            async { runLocalHeuristics(url) },
            async { queryVirusTotal(url) },
            async { queryGoogleSafeBrowsing(url) },
            async { queryUrlScan(url) },
            async { queryUrlHaus(url) },
            async { queryPhishStats(url) },
            async { queryThreatFox(url) },
        ).awaitAll()

        val verdictMap = LinkedHashMap<Provider, VendorVerdict>(initialVerdicts.size)
        initialVerdicts.forEach { verdict -> verdictMap[verdict.provider] = verdict }

        applyFallbacks(url, verdictMap)
    }

    private suspend fun applyFallbacks(
        url: String,
        verdicts: MutableMap<Provider, VendorVerdict>,
    ): List<VendorVerdict> {
        val fallbackMatrix: Map<Provider, List<Provider>> = mapOf(
            Provider.VIRUS_TOTAL to listOf(Provider.GOOGLE_SAFE_BROWSING, Provider.URL_SCAN, Provider.THREAT_FOX),
            Provider.GOOGLE_SAFE_BROWSING to listOf(Provider.URL_SCAN, Provider.URL_HAUS, Provider.PHISH_STATS),
            Provider.URL_SCAN to listOf(Provider.VIRUS_TOTAL, Provider.GOOGLE_SAFE_BROWSING, Provider.PHISH_STATS),
            Provider.URL_HAUS to listOf(Provider.THREAT_FOX, Provider.PHISH_STATS),
            Provider.PHISH_STATS to listOf(Provider.URL_HAUS, Provider.THREAT_FOX),
            Provider.THREAT_FOX to listOf(Provider.PHISH_STATS, Provider.URL_HAUS),
        )

        fallbackMatrix.forEach { (primary, fallbackCandidates) ->
            val currentVerdict = verdicts[primary] ?: return@forEach
            if (currentVerdict.status == VerdictStatus.ERROR || currentVerdict.status == VerdictStatus.UNKNOWN) {
                val existingFallback = fallbackCandidates.asSequence()
                    .mapNotNull { provider -> verdicts[provider]?.takeIf { it.status != VerdictStatus.ERROR && it.status != VerdictStatus.UNKNOWN }?.let { provider to it } }
                    .firstOrNull()

                val (fallbackProvider, fallbackVerdict) = existingFallback
                    ?: fetchFallbackVerdict(url, fallbackCandidates)?.also { (provider, verdict) ->
                        verdicts[provider] = verdict
                    }
                    ?: return@forEach

                verdicts[primary] = currentVerdict.copy(
                    status = fallbackVerdict.status,
                    score = fallbackVerdict.score,
                    details = currentVerdict.details + fallbackVerdict.details + mapOf(
                        "fallbackProvider" to fallbackProvider.name.lowercase(),
                        "fallbackReason" to "primary_unavailable",
                    ),
                )
            }
        }

        return verdicts.values.toList()
    }

    private suspend fun fetchFallbackVerdict(
        url: String,
        candidates: List<Provider>,
    ): Pair<Provider, VendorVerdict>? {
        candidates.forEach { provider ->
            val verdict = runCatching {
                when (provider) {
                    Provider.VIRUS_TOTAL -> queryVirusTotal(url)
                    Provider.GOOGLE_SAFE_BROWSING -> queryGoogleSafeBrowsing(url)
                    Provider.URL_SCAN -> queryUrlScan(url)
                    Provider.URL_HAUS -> queryUrlHaus(url)
                    Provider.PHISH_STATS -> queryPhishStats(url)
                    Provider.THREAT_FOX -> queryThreatFox(url)
                    Provider.LOCAL_HEURISTIC -> runLocalHeuristics(url)
                    else -> null
                }
            }.getOrNull() ?: return@forEach

            if (verdict.status != VerdictStatus.ERROR && verdict.status != VerdictStatus.UNKNOWN) {
                return provider to verdict
            }
        }
        return null
    }

    private suspend fun runLocalHeuristics(url: String): VendorVerdict = runCatching {
        heuristicsEvaluator.evaluate(url)
    }.getOrElse { throwable ->
        VendorVerdict(
            provider = Provider.LOCAL_HEURISTIC,
            status = VerdictStatus.ERROR,
            score = 0.0,
            details = mapOf("message" to (throwable.message ?: "heuristics failed")),
        )
    }

    private suspend fun queryVirusTotal(url: String): VendorVerdict {
        if (credentials.virusTotalApiKey.isNullOrBlank()) {
            return VendorVerdict(
                provider = Provider.VIRUS_TOTAL,
                status = VerdictStatus.UNKNOWN,
                score = 0.0,
                details = mapOf("message" to "API key not configured"),
            )
        }
        
        return runCatching {
            val encoded = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(url.toByteArray(StandardCharsets.UTF_8))

            var report = virusTotalApi.fetchReport(encoded)
            var analysisId: String? = null

            if (report.data?.attributes?.lastAnalysisStats.isNullOrEmpty()) {
                val submitResponse = virusTotalApi.submitUrl(VirusTotalSubmitRequestDto(url))
                analysisId = submitResponse.data?.id

                if (analysisId != null) {
                    for (attempt in 0 until 5) {
                        val delayMillis = 2000L * (attempt + 1)
                        kotlinx.coroutines.delay(delayMillis)
                        report = virusTotalApi.fetchReport(encoded)
                        if (!report.data?.attributes?.lastAnalysisStats.isNullOrEmpty()) {
                            break
                        }
                    }
                }
            }

            val stats = report.data?.attributes?.lastAnalysisStats
            val malicious = stats?.get("malicious") ?: 0
            val suspicious = stats?.get("suspicious") ?: 0
            val harmless = stats?.get("harmless") ?: 0
            val undetected = stats?.get("undetected") ?: 0
            val totalEngines = stats?.values?.sum() ?: 0
            val status = when {
                totalEngines == 0 -> VerdictStatus.UNKNOWN
                malicious > 0 -> VerdictStatus.MALICIOUS
                suspicious > 0 -> VerdictStatus.SUSPICIOUS
                harmless > 0 -> VerdictStatus.CLEAN
                else -> VerdictStatus.UNKNOWN
            }
            val score = if (totalEngines > 0) {
                (malicious + suspicious * 0.5) / totalEngines.toDouble()
            } else {
                0.0
            }

            VendorVerdict(
                provider = Provider.VIRUS_TOTAL,
                status = status,
                score = score,
                details = buildMap {
                    put("malicious", malicious.toString())
                    put("suspicious", suspicious.toString())
                    put("harmless", harmless.toString())
                    put("undetected", undetected.toString())
                    put("total_engines", totalEngines.toString())
                    analysisId?.let { put("analysis_id", it) }
                    if (stats.isNullOrEmpty() || totalEngines == 0) {
                        put("message", "analysis_pending")
                    }
                },
            )
        }.getOrElse { throwable ->
            VendorVerdict(
                provider = Provider.VIRUS_TOTAL,
                status = VerdictStatus.ERROR,
                score = 0.0,
                details = mapOf("error" to (throwable.message ?: "Unknown error")),
            )
        }
    }

    private suspend fun queryGoogleSafeBrowsing(url: String): VendorVerdict {
        if (credentials.googleSafeBrowsingApiKey.isNullOrBlank()) {
            return VendorVerdict(
                provider = Provider.GOOGLE_SAFE_BROWSING,
                status = VerdictStatus.UNKNOWN,
                score = 0.0,
                details = mapOf("message" to "API key not configured"),
            )
        }
        
        return runCatching {
            val request = GoogleSafeBrowsingRequestDto(
                threatInfo = GoogleSafeBrowsingRequestDto.ThreatInfoDto(
                    threatTypes = listOf("MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION"),
                    platformTypes = listOf("ANY_PLATFORM"),
                    threatEntryTypes = listOf("URL"),
                    threatEntries = listOf(
                        GoogleSafeBrowsingRequestDto.ThreatInfoDto.ThreatEntryDto(url),
                    ),
                ),
            )
            
            val response = googleSafeBrowsingApi.findThreats(request)
            val matches = response.matches ?: emptyList()
            val hasMatches = matches.isNotEmpty()
            
            // Calculate threat severity score based on threat types
            val maxSeverityScore = matches.maxOfOrNull { match ->
                when (match.threatType) {
                    "MALWARE" -> 1.0
                    "SOCIAL_ENGINEERING" -> 0.9
                    "UNWANTED_SOFTWARE" -> 0.7
                    "POTENTIALLY_HARMFUL_APPLICATION" -> 0.6
                    else -> 0.5
                }
            } ?: 0.0
            
            val status = when {
                maxSeverityScore >= 0.8 -> VerdictStatus.MALICIOUS
                maxSeverityScore >= 0.5 -> VerdictStatus.SUSPICIOUS
                hasMatches -> VerdictStatus.SUSPICIOUS
                else -> VerdictStatus.CLEAN
            }
            
            VendorVerdict(
                provider = Provider.GOOGLE_SAFE_BROWSING,
                status = status,
                score = maxSeverityScore,
                details = buildMap {
                    put("matchCount", matches.size.toString())
                    matches.forEach { match ->
                        match.threatType?.let { put("threatType_${it}", "found") }
                    }
                },
            )
        }.getOrElse { throwable ->
            VendorVerdict(
                provider = Provider.GOOGLE_SAFE_BROWSING,
                status = VerdictStatus.ERROR,
                score = 0.0,
                details = mapOf("error" to (throwable.message ?: "API request failed")),
            )
        }
    }

    private suspend fun queryUrlScan(url: String): VendorVerdict {
        if (credentials.urlScanApiKey.isNullOrBlank()) {
            return VendorVerdict(
                provider = Provider.URL_SCAN,
                status = VerdictStatus.UNKNOWN,
                score = 0.0,
                details = mapOf("message" to "API key not configured"),
            )
        }
        
        return runCatching {
            // Submit URL for scanning
            val submitResponse = urlScanApi.submitUrl(UrlScanSubmitRequestDto(url = url))
            val uuid = submitResponse.uuid
                ?: return@runCatching VendorVerdict(
                    provider = Provider.URL_SCAN,
                    status = VerdictStatus.ERROR,
                    score = 0.0,
                    details = mapOf("error" to "No UUID returned from submission"),
                )
            
            // Poll for results with exponential backoff
            var result: UrlScanResultDto? = null
            val maxAttempts = 6
            var attempt = 0
            
            while (attempt < maxAttempts && result == null) {
                val backoff = (1000.0 * 1.5.pow(attempt.toDouble())).toLong()
                kotlinx.coroutines.delay(min(backoff, 30000L))
                
                result = runCatching {
                    urlScanApi.fetchResult(uuid)
                }.getOrNull()
                
                // If we get a valid result or it's clearly an error, break
                if (result?.stats != null || attempt >= maxAttempts - 1) {
                    break
                }
                
                attempt++
            }
            
            val stats = result?.stats
            val malicious = stats?.malicious ?: 0
            val suspicious = stats?.suspicious ?: 0
            val total = malicious + suspicious
            
            val status = when {
                malicious > 0 -> VerdictStatus.MALICIOUS
                suspicious > 0 -> VerdictStatus.SUSPICIOUS
                stats != null -> VerdictStatus.CLEAN
                else -> VerdictStatus.UNKNOWN
            }
            
            val score = if (total > 0) {
                (malicious + suspicious * 0.5) / total.toDouble()
            } else 0.0
            
            VendorVerdict(
                provider = Provider.URL_SCAN,
                status = status,
                score = score,
                details = buildMap {
                    put("uuid", uuid)
                    put("malicious", malicious.toString())
                    put("suspicious", suspicious.toString())
                    put("polling_attempts", attempt.toString())
                    result?.page?.domain?.let { put("domain", it) }
                },
            )
        }.getOrElse { throwable ->
            VendorVerdict(
                provider = Provider.URL_SCAN,
                status = VerdictStatus.ERROR,
                score = 0.0,
                details = mapOf("error" to (throwable.message ?: "URLScan request failed")),
            )
        }
    }

    private suspend fun queryUrlHaus(url: String): VendorVerdict {
        val response = runCatching {
            urlHausApi.lookup(UrlHausLookupRequestDto(url = url))
        }.getOrNull()

        if (response == null) {
            return VendorVerdict(
                provider = Provider.URL_HAUS,
                status = VerdictStatus.ERROR,
                score = 0.0,
                details = mapOf("message" to "lookup failed"),
            )
        }

        if (response.queryStatus.equals("no_results", ignoreCase = true)) {
            return VendorVerdict(
                provider = Provider.URL_HAUS,
                status = VerdictStatus.CLEAN,
                score = 0.0,
                details = mapOf("message" to "not listed"),
            )
        }

        if (!response.queryStatus.equals("ok", ignoreCase = true)) {
            return VendorVerdict(
                provider = Provider.URL_HAUS,
                status = VerdictStatus.UNKNOWN,
                score = 0.0,
                details = mapOf("status" to response.queryStatus),
            )
        }

        val threatDescriptor = response.threat?.lowercase()
        val urlStatus = response.urlStatus?.lowercase()
        val isActive = urlStatus == "online" || urlStatus == "malicious"
        val isOffline = urlStatus == "offline"
        val status = when {
            isActive -> VerdictStatus.MALICIOUS
            threatDescriptor != null -> VerdictStatus.SUSPICIOUS
            isOffline -> VerdictStatus.SUSPICIOUS
            else -> VerdictStatus.CLEAN
        }
        val score = when (status) {
            VerdictStatus.MALICIOUS -> if (isActive) 0.95 else 0.8
            VerdictStatus.SUSPICIOUS -> 0.6
            VerdictStatus.CLEAN -> 0.0
            VerdictStatus.UNKNOWN -> 0.4
            VerdictStatus.ERROR -> 0.0
        }

        val blacklistSummary = response.blacklists?.let { blacklists ->
            listOfNotNull(
                blacklists.urlhaus?.status?.let { "urlhaus=$it" },
                blacklists.surbl?.status?.let { "surbl=$it" },
                blacklists.phishTank?.status?.let { "phishtank=$it" },
                blacklists.googleSafeBrowsing?.status?.let { "safebrowsing=$it" },
            ).takeIf { it.isNotEmpty() }?.joinToString()
        }

        return VendorVerdict(
            provider = Provider.URL_HAUS,
            status = status,
            score = score,
            details = mapOf(
                "url_status" to (response.urlStatus ?: "unknown"),
                "threat" to (response.threat ?: "unknown"),
                "last_online" to (response.lastOnline ?: "unknown"),
                "blacklists" to (blacklistSummary ?: "n/a"),
            ),
        )
    }

    private suspend fun queryPhishStats(url: String): VendorVerdict {
        val records = runCatching { phishStatsApi.search(url = url) }.getOrNull()
            ?: return VendorVerdict(
                provider = Provider.PHISH_STATS,
                status = VerdictStatus.ERROR,
                score = 0.0,
                details = mapOf("message" to "lookup failed"),
            )

        if (records.isEmpty()) {
            return VendorVerdict(
                provider = Provider.PHISH_STATS,
                status = VerdictStatus.CLEAN,
                score = 0.0,
                details = emptyMap(),
            )
        }

        val topRecord = records.firstOrNull { record ->
            record.status?.contains("active", ignoreCase = true) == true ||
                record.status?.contains("online", ignoreCase = true) == true
        } ?: records.first()
        val recordStatus = topRecord.status?.lowercase()
        val isActive = recordStatus == null || recordStatus.contains("active") || recordStatus.contains("online")
        val verdictStatus = if (isActive) VerdictStatus.MALICIOUS else VerdictStatus.SUSPICIOUS
        val score = if (isActive) 0.9 else 0.6
        return VendorVerdict(
            provider = Provider.PHISH_STATS,
            status = verdictStatus,
            score = score,
            details = mapOf(
                "host" to (topRecord.host ?: "unknown"),
                "target" to (topRecord.target ?: "unknown"),
                "status" to (topRecord.status ?: "unknown"),
                "first_seen" to (topRecord.firstSeen ?: "unknown"),
            ),
        )
    }

    private suspend fun queryThreatFox(url: String): VendorVerdict {
        val response = runCatching {
            threatFoxApi.search(
                ThreatFoxSearchRequestDto(
                    query = "search_url",
                    searchTerm = url,
                ),
            )
        }.getOrNull()

        if (response == null) {
            return VendorVerdict(
                provider = Provider.THREAT_FOX,
                status = VerdictStatus.ERROR,
                score = 0.0,
                details = mapOf("message" to "lookup failed"),
            )
        }

        if (response.queryStatus.equals("no_result", ignoreCase = true)) {
            return VendorVerdict(
                provider = Provider.THREAT_FOX,
                status = VerdictStatus.CLEAN,
                score = 0.0,
                details = emptyMap(),
            )
        }

        if (!response.queryStatus.equals("ok", ignoreCase = true)) {
            return VendorVerdict(
                provider = Provider.THREAT_FOX,
                status = VerdictStatus.UNKNOWN,
                score = 0.0,
                details = mapOf(
                    "status" to response.queryStatus,
                    "error" to (response.errorMessage ?: "none"),
                ),
            )
        }

        val indicators = response.data.orEmpty()
        if (indicators.isEmpty()) {
            return VendorVerdict(
                provider = Provider.THREAT_FOX,
                status = VerdictStatus.CLEAN,
                score = 0.0,
                details = emptyMap(),
            )
        }

        val highest = indicators.maxByOrNull { it.confidenceLevel ?: 0 }
        val confidence = (highest?.confidenceLevel ?: 50).coerceIn(0, 100)
        val score = (confidence / 100.0).coerceIn(0.0, 1.0)
        val verdictStatus = if (confidence >= 70) {
            VerdictStatus.MALICIOUS
        } else {
            VerdictStatus.SUSPICIOUS
        }

        return VendorVerdict(
            provider = Provider.THREAT_FOX,
            status = verdictStatus,
            score = score,
            details = buildMap {
                put("confidence", confidence.toString())
                put("threat_type", highest?.threatType ?: "unknown")
                put("malware", highest?.malware ?: "none")
                highest?.reference?.let { put("reference", it) }
                highest?.tags?.takeIf { it.isNotEmpty() }?.let { put("tags", it.joinToString()) }
            },
        )
    }

    private suspend fun runNetworkAnalysis(url: String): NetworkReport = runCatching {
        networkSecurityAnalyzer.inspect(url)
    }.getOrElse { NetworkReport(headers = emptyMap()) }

    private suspend fun runMlAnalysis(url: String): MlReport = runCatching {
        mlAnalyzer.evaluate(url)
    }.getOrElse { MlReport(probability = 0.0, topFeatures = emptyList()) }

    private suspend fun persistResult(sessionId: String, result: ScanResult) {
        val breakdownSerializer = MapSerializer(String.serializer(), Double.serializer())
        val sanitizedBreakdown = result.breakdown.sanitized()
        val breakdownJson = json.encodeToString(
            breakdownSerializer,
            sanitizedBreakdown.categories.map { (category, value) -> category.name to value }.toMap(),
        )
        val sanitizedMl = result.ml?.sanitized()
        val mlJson = sanitizedMl?.let { json.encodeToString(MlReport.serializer(), it) }
        val networkJson = result.network?.let { json.encodeToString(NetworkReport.serializer(), it) }
        val sanitizedFile = result.file?.sanitized()
        val fileJson = sanitizedFile?.let { json.encodeToString(FileScanReport.serializer(), it) }
        val sanitizedVpn = result.vpn?.sanitized()
        val vpnJson = sanitizedVpn?.let { json.encodeToString(VpnConfigReport.serializer(), it) }
        val sanitizedInstagram = result.instagram?.sanitized()
        val instagramJson = sanitizedInstagram?.let { json.encodeToString(InstagramScanReport.serializer(), it) }

        val scanEntity = ScanEntity(
            scanId = sessionId,
            targetType = result.targetType.name,
            targetLabel = result.targetLabel,
            normalizedUrl = result.normalizedUrl,
            riskScore = result.risk.sanitizeRiskScore(),
            breakdownJson = breakdownJson,
            networkJson = networkJson,
            mlJson = mlJson,
            fileJson = fileJson,
            vpnJson = vpnJson,
            instagramJson = instagramJson,
            createdAtEpochMillis = result.createdAt.toEpochMilliseconds(),
        )
        val detailsSerializer = MapSerializer(String.serializer(), String.serializer().nullable)
        val verdictEntities = result.vendors.map { verdict ->
            val sanitizedVerdict = verdict.sanitized()
            VendorVerdictEntity(
                scanId = sessionId,
                provider = sanitizedVerdict.provider.name,
                status = sanitizedVerdict.status.name,
                score = sanitizedVerdict.score,
                detailsJson = json.encodeToString(detailsSerializer, sanitizedVerdict.details),
            )
        }
        scanDao.insertScan(scanEntity)
        scanDao.deleteVerdictsForScan(sessionId)
        scanDao.insertVerdicts(verdictEntities)
    }

    private fun decodeFileBytes(raw: String): ByteArray? {
        if (raw.isBlank()) return null
        val base64 = runCatching { Base64.getDecoder().decode(raw) }.getOrNull()
        if (base64 != null && base64.isNotEmpty()) {
            return base64
        }
        return raw.toByteArray(StandardCharsets.UTF_8)
    }
}

private fun ScanWithVerdicts.toDomain(json: Json): ScanResult {
    val breakdownSerializer = MapSerializer(String.serializer(), Double.serializer())
    val breakdownRaw = if (scan.breakdownJson.isNotBlank()) {
        json.decodeFromString(breakdownSerializer, scan.breakdownJson)
    } else {
        emptyMap()
    }
    val breakdown = RiskBreakdown(
        breakdownRaw.mapKeysNotNull { key -> runCatching { RiskCategory.valueOf(key) }.getOrNull() },
    )
    val networkReport = scan.networkJson?.let { json.decodeFromString(NetworkReport.serializer(), it) }
    val mlReport = scan.mlJson?.let { json.decodeFromString(MlReport.serializer(), it) }
    val fileReport = scan.fileJson?.let { json.decodeFromString(FileScanReport.serializer(), it) }
    val vpnReport = scan.vpnJson?.let { json.decodeFromString(VpnConfigReport.serializer(), it) }
    val instagramReport = scan.instagramJson?.let { json.decodeFromString(InstagramScanReport.serializer(), it) }
    val detailsSerializer = MapSerializer(String.serializer(), String.serializer().nullable)
    val vendors = verdicts.map { entity ->
        val details = json.decodeFromString(detailsSerializer, entity.detailsJson)
        VendorVerdict(
            provider = runCatching { Provider.valueOf(entity.provider) }.getOrDefault(Provider.MANUAL),
            status = runCatching { VerdictStatus.valueOf(entity.status) }.getOrDefault(VerdictStatus.UNKNOWN),
            score = entity.score,
            details = details,
        )
    }
    val targetType = runCatching { ScanTargetType.valueOf(scan.targetType) }.getOrElse { ScanTargetType.URL }
    return ScanResult(
        sessionId = scan.scanId,
        targetType = targetType,
        targetLabel = scan.targetLabel,
        normalizedUrl = scan.normalizedUrl,
        vendors = vendors,
        network = networkReport,
        ml = mlReport,
        file = fileReport,
        vpn = vpnReport,
        instagram = instagramReport,
        risk = scan.riskScore,
        breakdown = breakdown,
        createdAt = Instant.fromEpochMilliseconds(scan.createdAtEpochMillis),
    )
}

private inline fun <K, V> Map<String, V>.mapKeysNotNull(transform: (String) -> K?): Map<K, V> {
    val destination = LinkedHashMap<K, V>()
    for ((key, value) in this) {
        val mappedKey = transform(key) ?: continue
        destination[mappedKey] = value
    }
    return destination
}

private fun Double.sanitizeFinite(default: Double = 0.0): Double = if (isFinite()) this else default

private fun Double.sanitizeProbability(): Double = sanitizeFinite().coerceIn(0.0, 1.0)

private fun Double.sanitizeRiskScore(): Double = sanitizeFinite().coerceIn(0.0, 5.0)

private fun MlReport.sanitized(): MlReport = copy(
    probability = probability.sanitizeProbability(),
    topFeatures = topFeatures.map { it.copy(weight = it.weight.sanitizeProbability()) },
)

private fun FileScanReport.sanitized(): FileScanReport = copy(
    riskScore = riskScore.sanitizeProbability(),
)

private fun VpnConfigReport.sanitized(): VpnConfigReport = copy(
    riskScore = riskScore.sanitizeProbability(),
)

private fun InstagramScanReport.sanitized(): InstagramScanReport = copy(
    riskScore = riskScore.sanitizeProbability(),
)

private fun RiskBreakdown.sanitized(): RiskBreakdown = RiskBreakdown(
    categories = categories.mapValues { (_, value) -> value.sanitizeProbability() },
)

private fun VendorVerdict.sanitized(): VendorVerdict = copy(
    score = score.sanitizeProbability(),
    details = details,
)
