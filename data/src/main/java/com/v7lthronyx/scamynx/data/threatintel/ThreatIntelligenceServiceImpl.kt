package com.v7lthronyx.scamynx.data.threatintel

import com.v7lthronyx.scamynx.domain.model.AttackVector
import com.v7lthronyx.scamynx.domain.model.ConfidenceLevel
import com.v7lthronyx.scamynx.domain.model.GeoAttribution
import com.v7lthronyx.scamynx.domain.model.IndicatorOfCompromise
import com.v7lthronyx.scamynx.domain.model.IoCType
import com.v7lthronyx.scamynx.domain.model.IssueSeverity
import com.v7lthronyx.scamynx.domain.model.MitreTactic
import com.v7lthronyx.scamynx.domain.model.MitreTechnique
import com.v7lthronyx.scamynx.domain.model.ThreatActorLevel
import com.v7lthronyx.scamynx.domain.model.ThreatActorProfile
import com.v7lthronyx.scamynx.domain.model.ThreatFeedSource
import com.v7lthronyx.scamynx.domain.model.ThreatFeedStatus
import com.v7lthronyx.scamynx.domain.model.ThreatIntelligenceReport
import com.v7lthronyx.scamynx.domain.model.ThreatLandscapeSummary
import com.v7lthronyx.scamynx.domain.model.ThreatTimelineEvent
import com.v7lthronyx.scamynx.domain.model.ThreatType
import com.v7lthronyx.scamynx.domain.repository.ThreatFeedRepository
import com.v7lthronyx.scamynx.domain.service.IoCFeedback
import com.v7lthronyx.scamynx.domain.service.ThreatIntelligenceService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThreatIntelligenceServiceImpl @Inject constructor(
    private val threatFeedRepository: ThreatFeedRepository,
) : ThreatIntelligenceService {

    override suspend fun lookupIoC(value: String, type: IoCType): IndicatorOfCompromise? {
        
        val match = threatFeedRepository.lookupUrl(value)
        if (match.isMalicious) {
            return IndicatorOfCompromise(
                id = UUID.randomUUID().toString(),
                value = value,
                type = type,
                threatTypes = listOf(ThreatType.PHISHING),
                confidence = ConfidenceLevel.HIGH,
                severity = IssueSeverity.HIGH,
                firstSeen = Clock.System.now().minus(kotlin.time.Duration.parse("24h")),
                lastSeen = Clock.System.now(),
                sources = match.sources.ifEmpty { listOf("SCAMYNX Threat Feed") },
                tags = listOf("scam", "phishing"),
                description = "Indicator found in SCAMYNX threat database. Type: ${match.threatType ?: "Unknown"}",
            )
        }
        return null
    }

    override suspend fun batchLookupIoCs(iocs: Map<String, IoCType>): Map<String, IndicatorOfCompromise?> {
        return iocs.mapValues { (value, type) -> lookupIoC(value, type) }
    }

    override suspend fun searchIoCs(
        query: String,
        types: List<IoCType>?,
        threatTypes: List<ThreatType>?,
        minConfidence: ConfidenceLevel?,
        limit: Int,
    ): List<IndicatorOfCompromise> {
        
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank()) return emptyList()

        val results = mutableListOf<IndicatorOfCompromise>()

        val urlTypes = listOf(IoCType.URL, IoCType.DOMAIN)
        val searchTypes = types ?: urlTypes

        for (type in searchTypes) {
            val lookupResult = threatFeedRepository.lookupUrl(normalizedQuery)
            if (lookupResult.isMalicious) {
                val ioc = IndicatorOfCompromise(
                    id = UUID.randomUUID().toString(),
                    value = normalizedQuery,
                    type = type,
                    threatTypes = threatTypes ?: listOf(
                        when (lookupResult.threatType?.lowercase()) {
                            "phishing" -> ThreatType.PHISHING
                            "malware" -> ThreatType.MALWARE
                            "scam" -> ThreatType.SCAM
                            "ransomware" -> ThreatType.RANSOMWARE
                            else -> ThreatType.UNKNOWN
                        },
                    ),
                    confidence = ConfidenceLevel.HIGH,
                    severity = IssueSeverity.HIGH,
                    firstSeen = Clock.System.now().minus(kotlin.time.Duration.parse("7d")),
                    lastSeen = Clock.System.now(),
                    sources = lookupResult.sources.ifEmpty { listOf("SCAMYNX Threat Feed") },
                    tags = listOf("scam", "phishing", "threat-intel"),
                    description = "Threat indicator matching query: $query. Type: ${lookupResult.threatType ?: "Unknown"}",
                )

                val confidenceOk = minConfidence == null ||
                    ioc.confidence.ordinal >= minConfidence.ordinal

                if (confidenceOk) {
                    results.add(ioc)
                }
            }
        }

        if (results.isEmpty() && normalizedQuery.length >= 3) {
            
            val isHash = normalizedQuery.matches(Regex("^[a-f0-9]{32,64}$"))
            if (isHash) {
                val hashType = when (normalizedQuery.length) {
                    32 -> IoCType.FILE_HASH_MD5
                    40 -> IoCType.FILE_HASH_SHA1
                    64 -> IoCType.FILE_HASH_SHA256
                    else -> IoCType.FILE_HASH_SHA256
                }
                results.add(
                    IndicatorOfCompromise(
                        id = UUID.randomUUID().toString(),
                        value = normalizedQuery,
                        type = hashType,
                        threatTypes = threatTypes ?: listOf(ThreatType.UNKNOWN),
                        confidence = ConfidenceLevel.LOW,
                        severity = IssueSeverity.LOW,
                        firstSeen = Clock.System.now(),
                        lastSeen = Clock.System.now(),
                        sources = listOf("SCAMYNX Analysis"),
                        tags = listOf("hash", "pending-verification"),
                        description = "Hash indicator submitted for analysis: $normalizedQuery",
                    ),
                )
            }

            val isIp = normalizedQuery.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"))
            if (isIp) {
                val lookupResult = threatFeedRepository.lookupUrl(normalizedQuery)
                results.add(
                    IndicatorOfCompromise(
                        id = UUID.randomUUID().toString(),
                        value = normalizedQuery,
                        type = IoCType.IP_ADDRESS,
                        threatTypes = if (lookupResult.isMalicious) {
                            listOf(ThreatType.MALWARE)
                        } else {
                            threatTypes ?: listOf(ThreatType.UNKNOWN)
                        },
                        confidence = if (lookupResult.isMalicious) ConfidenceLevel.HIGH else ConfidenceLevel.LOW,
                        severity = if (lookupResult.isMalicious) IssueSeverity.HIGH else IssueSeverity.LOW,
                        firstSeen = Clock.System.now(),
                        lastSeen = Clock.System.now(),
                        sources = lookupResult.sources.ifEmpty { listOf("SCAMYNX Analysis") },
                        tags = listOf("ip-address"),
                        description = "IP address indicator: $normalizedQuery",
                    ),
                )
            }
        }

        return results.take(limit)
    }

    override suspend fun generateReport(
        target: String,
        targetType: IoCType,
        includeTimeline: Boolean,
        includeRelated: Boolean,
    ): ThreatIntelligenceReport {
        val ioc = lookupIoC(target, targetType)
        val timeline = if (includeTimeline) getThreatTimeline(target, targetType) else emptyList()
        val relatedIoCs = if (includeRelated && ioc != null) getRelatedIoCs(ioc.id) else emptyList()

        val primaryThreatType = ioc?.threatTypes?.firstOrNull() ?: ThreatType.UNKNOWN

        return ThreatIntelligenceReport(
            id = UUID.randomUUID().toString(),
            target = target,
            targetType = targetType,
            threatTypes = ioc?.threatTypes ?: listOf(ThreatType.UNKNOWN),
            primaryThreatType = primaryThreatType,
            attackVectors = listOf(AttackVector.WEB),
            severity = ioc?.severity ?: IssueSeverity.LOW,
            confidence = ioc?.confidence ?: ConfidenceLevel.LOW,
            riskScore = if (ioc != null) 0.8 else 0.1,
            geoAttribution = getGeoAttribution(target),
            indicators = if (ioc != null) listOf(ioc) else emptyList(),
            ttps = getMitreTechniques(primaryThreatType),
            timeline = timeline,
            sources = ioc?.sources ?: emptyList(),
            summary = generateSummary(ioc),
            generatedAt = Clock.System.now(),
        )
    }

    override suspend fun getThreatTimeline(
        target: String,
        targetType: IoCType,
    ): List<ThreatTimelineEvent> {
        
        val ioc = lookupIoC(target, targetType)
        if (ioc == null) return emptyList()

        val events = mutableListOf<ThreatTimelineEvent>()

        ioc.firstSeen?.let { firstSeen ->
            events.add(
                ThreatTimelineEvent(
                    timestamp = firstSeen,
                    eventType = "FIRST_DETECTION",
                    description = "Indicator first observed in threat intelligence feeds",
                    source = ioc.sources.firstOrNull() ?: "SCAMYNX",
                    severity = IssueSeverity.MEDIUM,
                ),
            )
        }

        events.add(
            ThreatTimelineEvent(
                timestamp = ioc.firstSeen ?: Clock.System.now().minus(kotlin.time.Duration.parse("12h")),
                eventType = "CLASSIFICATION",
                description = "Classified as ${ioc.threatTypes.joinToString { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }}",
                source = "SCAMYNX Analysis Engine",
                severity = ioc.severity,
            ),
        )

        val isActiveRecently = ioc.lastSeen?.let {
            it > Clock.System.now().minus(kotlin.time.Duration.parse("7d"))
        } ?: false
        if (isActiveRecently) {
            events.add(
                ThreatTimelineEvent(
                    timestamp = ioc.lastSeen ?: Clock.System.now(),
                    eventType = "ACTIVE_THREAT",
                    description = "Indicator is currently active and poses ongoing risk",
                    source = "SCAMYNX Monitor",
                    severity = IssueSeverity.HIGH,
                ),
            )
        }

        ioc.lastSeen?.let { lastSeen ->
            events.add(
                ThreatTimelineEvent(
                    timestamp = lastSeen,
                    eventType = "LAST_OBSERVED",
                    description = "Most recent observation of this indicator",
                    source = ioc.sources.lastOrNull() ?: "SCAMYNX",
                    severity = IssueSeverity.LOW,
                ),
            )
        }

        return events.sortedBy { it.timestamp }
    }

    override suspend fun getRelatedIoCs(iocId: String): List<IndicatorOfCompromise> {
        
        val allMalicious = searchIoCs("", limit = 50)

        val originalIoC = allMalicious.find { it.id == iocId } ?: return emptyList()

        return allMalicious
            .filter { it.id != iocId }
            .filter { related ->
                
                related.threatTypes.any { it in originalIoC.threatTypes } ||
                    
                    related.tags.any { it in originalIoC.tags } ||
                    
                    related.sources.any { it in originalIoC.sources }
            }
            .take(10)
    }

    override suspend fun getGeoAttribution(target: String): GeoAttribution? {
        
        val ipRegex = Regex("""^(\d{1,3}\.){3}\d{1,3}$""")
        if (!ipRegex.matches(target)) return null

        val octets = target.split(".").mapNotNull { it.toIntOrNull() }
        if (octets.size != 4) return null

        val country = when {
            octets[0] in 1..126 -> "US"
            octets[0] in 128..191 && octets[1] in 0..63 -> "EU"
            octets[0] in 192..223 -> "APAC"
            else -> "Unknown"
        }

        return GeoAttribution(
            countryCode = country,
            countryName = when (country) {
                "US" -> "United States"
                "EU" -> "European Union"
                "APAC" -> "Asia Pacific"
                else -> "Unknown"
            },
        )
    }

    override suspend fun getThreatActor(actorId: String): ThreatActorProfile? {
        
        val knownActors = getKnownThreatActors()
        return knownActors.find { it.id == actorId || it.name.equals(actorId, ignoreCase = true) }
    }

    override suspend fun searchThreatActors(query: String, limit: Int): List<ThreatActorProfile> {
        if (query.isBlank()) return emptyList()

        val normalizedQuery = query.lowercase().trim()
        val knownActors = getKnownThreatActors()

        return knownActors
            .filter { actor ->
                actor.name.lowercase().contains(normalizedQuery) ||
                    actor.aliases.any { it.lowercase().contains(normalizedQuery) } ||
                    actor.motivation?.lowercase()?.contains(normalizedQuery) == true ||
                    actor.targetedSectors.any { it.lowercase().contains(normalizedQuery) }
            }
            .take(limit)
    }

    private fun getKnownThreatActors(): List<ThreatActorProfile> {
        return listOf(
            ThreatActorProfile(
                id = "apt-generic-phishing",
                name = "Generic Phishing Operators",
                aliases = listOf("Phishing Gang", "Credential Harvesters"),
                level = ThreatActorLevel.CYBERCRIMINAL,
                motivation = "Financial",
                targetedSectors = listOf("Finance", "E-commerce", "Social Media"),
                ttps = listOf(
                    MitreTechnique("T1566", "Phishing", MitreTactic.INITIAL_ACCESS),
                    MitreTechnique("T1566.001", "Spearphishing Attachment", MitreTactic.INITIAL_ACCESS),
                    MitreTechnique("T1566.002", "Spearphishing Link", MitreTactic.INITIAL_ACCESS),
                ),
                firstObserved = Clock.System.now().minus(kotlin.time.Duration.parse("365d")),
            ),
            ThreatActorProfile(
                id = "apt-scam-rings",
                name = "Tech Support Scam Networks",
                aliases = listOf("Support Scammers", "Refund Scammers"),
                level = ThreatActorLevel.ORGANIZED_CRIME,
                motivation = "Financial",
                targetedSectors = listOf("Consumer", "Elderly", "Technology"),
                ttps = listOf(
                    MitreTechnique("T1204", "User Execution", MitreTactic.EXECUTION),
                    MitreTechnique("T1566", "Phishing", MitreTactic.INITIAL_ACCESS),
                ),
                firstObserved = Clock.System.now().minus(kotlin.time.Duration.parse("730d")),
            ),
            ThreatActorProfile(
                id = "apt-cryptoscam",
                name = "Cryptocurrency Scam Operators",
                aliases = listOf("Crypto Scammers", "Investment Fraud"),
                level = ThreatActorLevel.CYBERCRIMINAL,
                motivation = "Financial",
                targetedSectors = listOf("Finance", "Cryptocurrency", "Investment"),
                ttps = listOf(
                    MitreTechnique("T1566.002", "Spearphishing Link", MitreTactic.INITIAL_ACCESS),
                    MitreTechnique("T1204", "User Execution", MitreTactic.EXECUTION),
                ),
                firstObserved = Clock.System.now().minus(kotlin.time.Duration.parse("500d")),
            ),
        )
    }

    override suspend fun getMitreTechniques(threatType: ThreatType): List<MitreTechnique> {
        return when (threatType) {
            ThreatType.PHISHING -> listOf(
                MitreTechnique("T1566", "Phishing", MitreTactic.INITIAL_ACCESS, "https://attack.mitre.org/techniques/T1566/"),
                MitreTechnique("T1566.001", "Spearphishing Attachment", MitreTactic.INITIAL_ACCESS),
                MitreTechnique("T1566.002", "Spearphishing Link", MitreTactic.INITIAL_ACCESS),
            )
            ThreatType.MALWARE, ThreatType.TROJAN -> listOf(
                MitreTechnique("T1204", "User Execution", MitreTactic.EXECUTION),
                MitreTechnique("T1059", "Command and Scripting Interpreter", MitreTactic.EXECUTION),
            )
            ThreatType.C2 -> listOf(
                MitreTechnique("T1071", "Application Layer Protocol", MitreTactic.COMMAND_AND_CONTROL),
                MitreTechnique("T1573", "Encrypted Channel", MitreTactic.COMMAND_AND_CONTROL),
            )
            else -> emptyList()
        }
    }

    override suspend fun getMitreTechnique(techniqueId: String): MitreTechnique? {
        return getMitreTechniques(ThreatType.PHISHING).find { it.id == techniqueId }
            ?: getMitreTechniques(ThreatType.MALWARE).find { it.id == techniqueId }
            ?: getMitreTechniques(ThreatType.C2).find { it.id == techniqueId }
    }

    override suspend fun getThreatFeeds(): List<ThreatFeedSource> {
        return listOf(
            ThreatFeedSource("scamynx", "SCAMYNX Threat Feed", "SCAMYNX", "IoC", ThreatFeedStatus.ACTIVE),
            ThreatFeedSource("urlhaus", "URLhaus", "Abuse.ch", "Malware", ThreatFeedStatus.ACTIVE),
            ThreatFeedSource("phishstats", "PhishStats", "PhishStats", "Phishing", ThreatFeedStatus.ACTIVE),
            ThreatFeedSource("threatfox", "ThreatFox", "Abuse.ch", "IoC", ThreatFeedStatus.ACTIVE),
        )
    }

    override suspend fun syncThreatFeed(feedId: String): Int {
        threatFeedRepository.refresh()
        return 0
    }

    override suspend fun syncAllFeeds(): Int {
        threatFeedRepository.refresh()
        return 0
    }

    override fun subscribeThreatFeed(): Flow<IndicatorOfCompromise> = flow {
        
    }

    override suspend fun getThreatLandscape(): ThreatLandscapeSummary {
        return ThreatLandscapeSummary(
            totalThreatsDetected = 0,
            criticalThreats = 0,
            highThreats = 0,
            mediumThreats = 0,
            lowThreats = 0,
            topThreatTypes = emptyMap(),
            topAttackVectors = emptyMap(),
            activeFeeds = 4,
            lastUpdated = Clock.System.now(),
        )
    }

    override suspend fun getTrendingThreats(limit: Int): List<String> {
        return listOf(
            "Phishing campaigns targeting financial institutions",
            "Ransomware-as-a-Service operations",
            "Cryptocurrency exchange scams",
            "Social engineering via fake customer support",
        ).take(limit)
    }

    override suspend fun getEmergingThreats(limit: Int): List<String> {
        return listOf(
            "AI-generated phishing content",
            "QR code based credential theft",
            "Deep fake voice scams",
        ).take(limit)
    }

    override suspend fun reportIoC(ioc: IndicatorOfCompromise): Boolean {
        return false
    }

    override suspend fun submitFeedback(
        iocId: String,
        feedback: IoCFeedback,
        details: String?,
    ): Boolean {
        return false
    }

    private fun generateSummary(ioc: IndicatorOfCompromise?): String {
        return if (ioc != null) {
            "This indicator has been identified as malicious with ${ioc.confidence} confidence. " +
                "It was first observed ${ioc.firstSeen} and remains active."
        } else {
            "No threat intelligence data found for this target."
        }
    }
}
