package com.v7lthronyx.scamynx.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class EnhancedMlReport(
    val probability: Double,
    val confidence: ConfidenceLevel,
    val modelVersion: String,
    val modelType: MlModelType,
    val topFeatures: List<FeatureWeight>,
    val featureExplanations: List<FeatureExplanation>,
    val similarThreats: List<SimilarThreat>,
    val anomalyScores: AnomalyScores? = null,
    val processingTime: Long,
    val timestamp: Instant,
)

@Serializable
enum class MlModelType {
    @SerialName("url_classifier")
    URL_CLASSIFIER,

    @SerialName("phishing_detector")
    PHISHING_DETECTOR,

    @SerialName("malware_detector")
    MALWARE_DETECTOR,

    @SerialName("social_engineering")
    SOCIAL_ENGINEERING,

    @SerialName("file_analyzer")
    FILE_ANALYZER,

    @SerialName("ensemble")
    ENSEMBLE,
}

@Serializable
data class FeatureExplanation(
    val feature: String,
    val value: String,
    val contribution: Double,
    val explanation: String,
    val category: FeatureCategory,
)

@Serializable
enum class FeatureCategory {
    @SerialName("url_structure")
    URL_STRUCTURE,

    @SerialName("domain_reputation")
    DOMAIN_REPUTATION,

    @SerialName("content_analysis")
    CONTENT_ANALYSIS,

    @SerialName("behavioral")
    BEHAVIORAL,

    @SerialName("network")
    NETWORK,

    @SerialName("historical")
    HISTORICAL,
}

@Serializable
data class SimilarThreat(
    val id: String,
    val similarity: Double,
    val threatType: ThreatType,
    val description: String,
    val firstSeen: Instant? = null,
)

@Serializable
data class AnomalyScores(
    val overallAnomaly: Double,
    val structuralAnomaly: Double,
    val behavioralAnomaly: Double,
    val statisticalAnomaly: Double,
    val isOutlier: Boolean,
)


@Serializable
data class ThreatTimeline(
    val target: String,
    val events: List<TimelineEvent>,
    val firstSeen: Instant? = null,
    val lastSeen: Instant? = null,
    val activityPeriods: List<ActivityPeriod>,
    val trendAnalysis: TrendAnalysis,
)

@Serializable
data class TimelineEvent(
    val timestamp: Instant,
    val eventType: TimelineEventType,
    val source: String,
    val description: String,
    val severity: IssueSeverity,
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
enum class TimelineEventType {
    @SerialName("first_observed")
    FIRST_OBSERVED,

    @SerialName("reported")
    REPORTED,

    @SerialName("confirmed")
    CONFIRMED,

    @SerialName("blocked")
    BLOCKED,

    @SerialName("takedown")
    TAKEDOWN,

    @SerialName("reactivated")
    REACTIVATED,

    @SerialName("infrastructure_change")
    INFRASTRUCTURE_CHANGE,

    @SerialName("campaign_linked")
    CAMPAIGN_LINKED,

    @SerialName("ioc_added")
    IOC_ADDED,
}

@Serializable
data class ActivityPeriod(
    val start: Instant,
    val end: Instant? = null,
    val isActive: Boolean,
    val peakActivity: Instant? = null,
    val victimCount: Int? = null,
)

@Serializable
data class TrendAnalysis(
    val trend: ThreatTrend,
    val velocityChange: Double,
    val projectedRisk: Double,
    val confidence: ConfidenceLevel,
)

@Serializable
enum class ThreatTrend {
    @SerialName("emerging")
    EMERGING,

    @SerialName("growing")
    GROWING,

    @SerialName("stable")
    STABLE,

    @SerialName("declining")
    DECLINING,

    @SerialName("dormant")
    DORMANT,

    @SerialName("resurgent")
    RESURGENT,
}


@Serializable
data class EnhancedScanResult(
    
    val sessionId: String,
    val targetType: ScanTargetType,
    val targetLabel: String,
    val normalizedUrl: String? = null,

    val risk: Double,
    val riskCategory: RiskCategory,
    val confidence: ConfidenceLevel,
    val breakdown: EnhancedRiskBreakdown,

    val threatIntelligence: ThreatIntelligenceReport? = null,
    val timeline: ThreatTimeline? = null,

    val vendors: List<VendorVerdict> = emptyList(),
    val network: NetworkReport? = null,
    val ml: EnhancedMlReport? = null,
    val file: FileScanReport? = null,
    val vpn: VpnConfigReport? = null,
    val instagram: InstagramScanReport? = null,

    val mitigations: List<MitigationAction> = emptyList(),
    val relatedThreats: List<RelatedThreat> = emptyList(),
    val iocs: List<IndicatorOfCompromise> = emptyList(),

    val scanDuration: Long,
    val providersQueried: Int,
    val providersResponded: Int,
    val cacheHit: Boolean = false,
    @Serializable(with = InstantIso8601Serializer::class)
    val createdAt: Instant,
    val expiresAt: Instant? = null,
)

@Serializable
data class EnhancedRiskBreakdown(
    val categories: Map<RiskCategory, Double>,
    val threatTypes: Map<ThreatType, Double>,
    val sources: Map<Provider, Double>,
    val factors: List<RiskFactor>,
)

@Serializable
data class RiskFactor(
    val name: String,
    val contribution: Double,
    val severity: IssueSeverity,
    val description: String,
    val source: String,
)

@Serializable
data class RelatedThreat(
    val id: String,
    val type: ThreatType,
    val relationship: ThreatRelationship,
    val description: String,
    val confidence: ConfidenceLevel,
)

@Serializable
enum class ThreatRelationship {
    @SerialName("same_campaign")
    SAME_CAMPAIGN,

    @SerialName("same_actor")
    SAME_ACTOR,

    @SerialName("same_infrastructure")
    SAME_INFRASTRUCTURE,

    @SerialName("similar_technique")
    SIMILAR_TECHNIQUE,

    @SerialName("variant")
    VARIANT,

    @SerialName("successor")
    SUCCESSOR,
}


@Serializable
data class ScanConfiguration(
    val depth: ScanDepth = ScanDepth.STANDARD,
    val includeTimeline: Boolean = false,
    val includeThreatIntel: Boolean = true,
    val includeRelatedThreats: Boolean = false,
    val followRedirects: Boolean = true,
    val maxRedirects: Int = 5,
    val timeout: Long = 30000,
    val enabledProviders: List<Provider> = emptyList(),
    val disabledProviders: List<Provider> = emptyList(),
    val mlModels: List<MlModelType> = listOf(MlModelType.ENSEMBLE),
    val cachePolicy: CachePolicy = CachePolicy.NORMAL,
)

@Serializable
enum class ScanDepth {
    @SerialName("quick")
    QUICK,

    @SerialName("standard")
    STANDARD,

    @SerialName("deep")
    DEEP,

    @SerialName("forensic")
    FORENSIC,
}

@Serializable
enum class CachePolicy {
    @SerialName("force_fresh")
    FORCE_FRESH,

    @SerialName("normal")
    NORMAL,

    @SerialName("prefer_cache")
    PREFER_CACHE,

    @SerialName("offline")
    OFFLINE,
}


@Serializable
data class QueuedScan(
    val id: String,
    val request: ScanRequest,
    val configuration: ScanConfiguration,
    val priority: ScanPriority,
    val status: QueueStatus,
    val progress: Int,
    val currentStage: ScanStage? = null,
    val queuedAt: Instant,
    val startedAt: Instant? = null,
    val estimatedCompletion: Instant? = null,
    val retryCount: Int = 0,
    val lastError: String? = null,
)

@Serializable
enum class ScanPriority {
    @SerialName("low")
    LOW,

    @SerialName("normal")
    NORMAL,

    @SerialName("high")
    HIGH,

    @SerialName("critical")
    CRITICAL,
}

@Serializable
enum class QueueStatus {
    @SerialName("queued")
    QUEUED,

    @SerialName("running")
    RUNNING,

    @SerialName("paused")
    PAUSED,

    @SerialName("completed")
    COMPLETED,

    @SerialName("failed")
    FAILED,

    @SerialName("cancelled")
    CANCELLED,
}


@Serializable
data class BatchScanRequest(
    val id: String,
    val targets: List<ScanRequest>,
    val configuration: ScanConfiguration,
    val priority: ScanPriority = ScanPriority.NORMAL,
    val stopOnFirstThreat: Boolean = false,
    val parallelScans: Int = 3,
)

@Serializable
data class BatchScanResult(
    val id: String,
    val totalTargets: Int,
    val completedScans: Int,
    val failedScans: Int,
    val threatsFound: Int,
    val highRiskCount: Int,
    val results: List<EnhancedScanResult>,
    val failedTargets: List<FailedScan>,
    val duration: Long,
    val startedAt: Instant,
    val completedAt: Instant,
)

@Serializable
data class FailedScan(
    val target: String,
    val targetType: ScanTargetType,
    val error: String,
    val retryable: Boolean,
)
