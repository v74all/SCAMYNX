package com.v7lthronyx.scamynx.data.repository

import com.v7lthronyx.scamynx.data.analyzer.FileStaticAnalyzer
import com.v7lthronyx.scamynx.data.analyzer.InstagramScamAnalyzer
import com.v7lthronyx.scamynx.data.analyzer.VpnConfigAnalyzer
import com.v7lthronyx.scamynx.data.db.ScanDao
import com.v7lthronyx.scamynx.data.db.ScanEntity
import com.v7lthronyx.scamynx.data.db.ScanWithVerdicts
import com.v7lthronyx.scamynx.data.db.ThreatFeedDao
import com.v7lthronyx.scamynx.data.db.ThreatIndicatorEntity
import com.v7lthronyx.scamynx.data.db.VendorVerdictEntity
import com.v7lthronyx.scamynx.data.heuristics.LocalHeuristicsEvaluator
import com.v7lthronyx.scamynx.data.network.api.GoogleSafeBrowsingApi
import com.v7lthronyx.scamynx.data.network.api.PhishStatsApi
import com.v7lthronyx.scamynx.data.network.api.ThreatFoxApi
import com.v7lthronyx.scamynx.data.network.api.UrlHausApi
import com.v7lthronyx.scamynx.data.network.api.UrlScanApi
import com.v7lthronyx.scamynx.data.network.api.VirusTotalApi
import com.v7lthronyx.scamynx.data.network.model.GoogleSafeBrowsingRequestDto
import com.v7lthronyx.scamynx.data.network.model.GoogleSafeBrowsingResponseDto
import com.v7lthronyx.scamynx.data.network.model.PhishStatsRecordDto
import com.v7lthronyx.scamynx.data.network.model.ThreatFoxIndicatorDto
import com.v7lthronyx.scamynx.data.network.model.ThreatFoxSearchRequestDto
import com.v7lthronyx.scamynx.data.network.model.ThreatFoxSearchResponseDto
import com.v7lthronyx.scamynx.data.network.model.UrlHausLookupRequestDto
import com.v7lthronyx.scamynx.data.network.model.UrlHausLookupResponseDto
import com.v7lthronyx.scamynx.data.network.model.UrlScanResultDto
import com.v7lthronyx.scamynx.data.network.model.UrlScanSubmitRequestDto
import com.v7lthronyx.scamynx.data.network.model.UrlScanSubmitResponseDto
import com.v7lthronyx.scamynx.data.network.model.VirusTotalReportDto
import com.v7lthronyx.scamynx.data.network.model.VirusTotalSubmitRequestDto
import com.v7lthronyx.scamynx.data.network.model.VirusTotalSubmitResponseDto
import com.v7lthronyx.scamynx.data.util.ApiCredentials
import com.v7lthronyx.scamynx.data.util.UrlNormalizer
import com.v7lthronyx.scamynx.domain.model.MlReport
import com.v7lthronyx.scamynx.domain.model.NetworkReport
import com.v7lthronyx.scamynx.domain.model.Provider
import com.v7lthronyx.scamynx.domain.model.ScanRequest
import com.v7lthronyx.scamynx.domain.model.ScanState
import com.v7lthronyx.scamynx.domain.model.ScanTargetType
import com.v7lthronyx.scamynx.domain.model.VerdictStatus
import com.v7lthronyx.scamynx.domain.service.MlAnalyzer
import com.v7lthronyx.scamynx.domain.service.NetworkSecurityAnalyzer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.collections.ArrayDeque
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanRepositoryImplFallbackTest {

    @Test
    fun virusTotalFallsBackToGoogleSafeBrowsing() = runTest {
        val failingVirusTotal = FakeVirusTotalApi().apply {
            fetchResponses += RuntimeException("vt-down")
        }
        val googleSafeBrowsingApi = FakeGoogleSafeBrowsingApi(
            response = GoogleSafeBrowsingResponseDto(
                matches = listOf(
                    GoogleSafeBrowsingResponseDto.ThreatMatchDto(
                        threatType = "MALWARE",
                        threat = GoogleSafeBrowsingResponseDto.ThreatMatchDto.ThreatDto(
                            url = "https://attack.example",
                        ),
                    ),
                ),
            ),
        )

        val repository = createRepository(
            virusTotalApi = failingVirusTotal,
            googleSafeBrowsingApi = googleSafeBrowsingApi,
        )

        val states = repository.analyze(
            ScanRequest(
                targetType = ScanTargetType.URL,
                rawInput = "https://attack.example",
            ),
        ).toList()

        val success = states.filterIsInstance<ScanState.Success>().single()
        val vendorsByProvider = success.result.vendors.associateBy { it.provider }

        val virusTotalVerdict = requireNotNull(vendorsByProvider[Provider.VIRUS_TOTAL])
        assertEquals(VerdictStatus.MALICIOUS, virusTotalVerdict.status)
        assertEquals("google_safe_browsing", virusTotalVerdict.details["fallbackProvider"])
        assertEquals("primary_unavailable", virusTotalVerdict.details["fallbackReason"])

        val safeBrowsingVerdict = requireNotNull(vendorsByProvider[Provider.GOOGLE_SAFE_BROWSING])
        assertTrue(
            safeBrowsingVerdict.status == VerdictStatus.MALICIOUS ||
                safeBrowsingVerdict.status == VerdictStatus.SUSPICIOUS,
        )
    }

    @Test
    fun urlHausTriggersSecondaryLookupWhenInitialFallbacksFail() = runTest {
        val fallbackThreatFoxResponse = ThreatFoxSearchResponseDto(
            queryStatus = "ok",
            data = listOf(
                ThreatFoxIndicatorDto(
                    confidenceLevel = 80,
                    threatType = "phishing",
                    malware = "generic",
                ),
            ),
        )

        val urlHausApi = FakeUrlHausApi().apply {
            responses += RuntimeException("service unavailable")
        }
        val threatFoxApi = FakeThreatFoxApi().apply {
            responses += RuntimeException("first call failed")
            responses += fallbackThreatFoxResponse
        }
        val phishStatsApi = FakePhishStatsApi().apply {
            responses += RuntimeException("ps outage")
        }

        val repository = createRepository(
            urlHausApi = urlHausApi,
            threatFoxApi = threatFoxApi,
            phishStatsApi = phishStatsApi,
        )

        val states = repository.analyze(
            ScanRequest(
                targetType = ScanTargetType.URL,
                rawInput = "https://fallback.example",
            ),
        ).toList()

        val success = states.filterIsInstance<ScanState.Success>().single()
        val vendorsByProvider = success.result.vendors.associateBy { it.provider }

        val urlHausVerdict = requireNotNull(vendorsByProvider[Provider.URL_HAUS])
        assertEquals(VerdictStatus.MALICIOUS, urlHausVerdict.status)
        assertEquals("threat_fox", urlHausVerdict.details["fallbackProvider"])
        assertEquals("primary_unavailable", urlHausVerdict.details["fallbackReason"])

        val threatFoxVerdict = requireNotNull(vendorsByProvider[Provider.THREAT_FOX])
        assertEquals(VerdictStatus.MALICIOUS, threatFoxVerdict.status)
        assertTrue(threatFoxVerdict.details["confidence"]?.toInt() ?: 0 >= 70)
    }

    private fun TestScope.createRepository(
        virusTotalApi: VirusTotalApi = FakeVirusTotalApi(),
        googleSafeBrowsingApi: GoogleSafeBrowsingApi = FakeGoogleSafeBrowsingApi(),
        urlScanApi: UrlScanApi = FakeUrlScanApi(),
        urlHausApi: UrlHausApi = FakeUrlHausApi(),
        phishStatsApi: PhishStatsApi = FakePhishStatsApi(),
        threatFoxApi: ThreatFoxApi = FakeThreatFoxApi(),
    ): ScanRepositoryImpl {
        val json = Json { ignoreUnknownKeys = true }
        val dispatcher = StandardTestDispatcher(testScheduler)

        val heuristicsEvaluator = LocalHeuristicsEvaluator(
            threatFeedDao = FakeThreatFeedDao(),
            json = json,
            ioDispatcher = dispatcher,
        )

        return ScanRepositoryImpl(
            virusTotalApi = virusTotalApi,
            googleSafeBrowsingApi = googleSafeBrowsingApi,
            urlScanApi = urlScanApi,
            urlHausApi = urlHausApi,
            phishStatsApi = phishStatsApi,
            threatFoxApi = threatFoxApi,
            networkSecurityAnalyzer = FakeNetworkSecurityAnalyzer(),
            mlAnalyzer = FakeMlAnalyzer(),
            heuristicsEvaluator = heuristicsEvaluator,
            fileStaticAnalyzer = FileStaticAnalyzer(),
            vpnConfigAnalyzer = VpnConfigAnalyzer(json),
            instagramAnalyzer = InstagramScamAnalyzer(),
            scanDao = FakeScanDao(),
            urlNormalizer = UrlNormalizer(),
            credentials = ApiCredentials(
                virusTotalApiKey = "vt-key",
                googleSafeBrowsingApiKey = "gsb-key",
                urlScanApiKey = "urlscan-key",
                telemetryEndpoint = null,
            ),
            json = json,
            ioDispatcher = dispatcher,
        )
    }

    private class FakeVirusTotalApi : VirusTotalApi {
        val fetchResponses: ArrayDeque<Any?> = ArrayDeque()
        val submitResponses: ArrayDeque<Any?> = ArrayDeque()

        override suspend fun submitUrl(request: VirusTotalSubmitRequestDto): VirusTotalSubmitResponseDto {
            val next = submitResponses.removeFirstOrNull()
                ?: return VirusTotalSubmitResponseDto(
                    data = VirusTotalSubmitResponseDto.VirusTotalSubmitDataDto(id = "analysis-id"),
                )
            if (next is Throwable) throw next
            @Suppress("UNCHECKED_CAST")
            return next as VirusTotalSubmitResponseDto
        }

        override suspend fun fetchReport(id: String): VirusTotalReportDto {
            val next = fetchResponses.removeFirstOrNull()
                ?: return VirusTotalReportDto(
                    data = VirusTotalReportDto.VirusTotalReportDataDto(
                        attributes = VirusTotalReportDto.VirusTotalReportDataDto.AttributesDto(
                            lastAnalysisStats = mapOf("harmless" to 1),
                        ),
                    ),
                )
            if (next is Throwable) throw next
            @Suppress("UNCHECKED_CAST")
            return next as VirusTotalReportDto
        }
    }

    private class FakeGoogleSafeBrowsingApi(
        var response: GoogleSafeBrowsingResponseDto = GoogleSafeBrowsingResponseDto(),
    ) : GoogleSafeBrowsingApi {
        override suspend fun findThreats(
            request: GoogleSafeBrowsingRequestDto,
        ): GoogleSafeBrowsingResponseDto = response
    }

    private class FakeUrlScanApi : UrlScanApi {
        var submitResponse: UrlScanSubmitResponseDto = UrlScanSubmitResponseDto(uuid = "scan-id")
        var fetchResponse: UrlScanResultDto = UrlScanResultDto(
            stats = UrlScanResultDto.StatsDto(
                malicious = 0,
                suspicious = 0,
                undetected = 1,
                harmless = 2,
            ),
        )

        override suspend fun submitUrl(request: UrlScanSubmitRequestDto): UrlScanSubmitResponseDto = submitResponse

        override suspend fun fetchResult(uuid: String): UrlScanResultDto = fetchResponse
    }

    private class FakeUrlHausApi : UrlHausApi {
        val responses: ArrayDeque<Any?> = ArrayDeque()

        override suspend fun lookup(request: UrlHausLookupRequestDto): UrlHausLookupResponseDto {
            val next = responses.removeFirstOrNull()
                ?: return UrlHausLookupResponseDto(queryStatus = "no_results")
            if (next is Throwable) throw next
            @Suppress("UNCHECKED_CAST")
            return next as UrlHausLookupResponseDto
        }
    }

    private class FakePhishStatsApi : PhishStatsApi {
        val responses: ArrayDeque<Any?> = ArrayDeque()

        override suspend fun search(url: String, format: String): List<PhishStatsRecordDto> {
            val next = responses.removeFirstOrNull()
                ?: return emptyList()
            if (next is Throwable) throw next
            @Suppress("UNCHECKED_CAST")
            return next as List<PhishStatsRecordDto>
        }
    }

    private class FakeThreatFoxApi : ThreatFoxApi {
        val responses: ArrayDeque<Any?> = ArrayDeque()

        override suspend fun search(request: ThreatFoxSearchRequestDto): ThreatFoxSearchResponseDto {
            val next = responses.removeFirstOrNull()
                ?: return ThreatFoxSearchResponseDto(queryStatus = "no_result")
            if (next is Throwable) throw next
            @Suppress("UNCHECKED_CAST")
            return next as ThreatFoxSearchResponseDto
        }
    }

    private class FakeThreatFeedDao : ThreatFeedDao {
        override suspend fun insertAll(indicators: List<ThreatIndicatorEntity>) = Unit
        override suspend fun clearAll() = Unit
        override fun observeLatest(limit: Int): Flow<List<ThreatIndicatorEntity>> = flowOf(emptyList())
        override suspend fun findByUrl(url: String): List<ThreatIndicatorEntity> = emptyList()
        override suspend fun findByHost(host: String, limit: Int): List<ThreatIndicatorEntity> = emptyList()
    }

    private class FakeScanDao : ScanDao {
        override fun observeHistory(limit: Int, offset: Int): Flow<List<ScanWithVerdicts>> = flowOf(emptyList())
        override suspend fun getHistory(limit: Int, offset: Int): List<ScanWithVerdicts> = emptyList()
        override suspend fun insertScan(scanEntity: ScanEntity) = Unit
        override suspend fun insertVerdicts(verdicts: List<VendorVerdictEntity>) = Unit
        override suspend fun deleteVerdictsForScan(scanId: String) = Unit
        override suspend fun getScan(scanId: String): ScanWithVerdicts? = null
        override suspend fun clearAllScans() = Unit
        override suspend fun clearAllVerdicts() = Unit
    }

    private class FakeNetworkSecurityAnalyzer : NetworkSecurityAnalyzer {
        override suspend fun inspect(url: String): NetworkReport = NetworkReport()
    }

    private class FakeMlAnalyzer : MlAnalyzer {
        override suspend fun evaluate(url: String, htmlSnapshot: String?): MlReport = MlReport(probability = 0.0)
    }
}
