package com.v7lthronyx.scamynx.domain.service

import com.v7lthronyx.scamynx.domain.model.ConfidenceLevel
import com.v7lthronyx.scamynx.domain.model.GeoAttribution
import com.v7lthronyx.scamynx.domain.model.IndicatorOfCompromise
import com.v7lthronyx.scamynx.domain.model.IoCType
import com.v7lthronyx.scamynx.domain.model.MitreTechnique
import com.v7lthronyx.scamynx.domain.model.ThreatActorProfile
import com.v7lthronyx.scamynx.domain.model.ThreatFeedSource
import com.v7lthronyx.scamynx.domain.model.ThreatIntelligenceReport
import com.v7lthronyx.scamynx.domain.model.ThreatLandscapeSummary
import com.v7lthronyx.scamynx.domain.model.ThreatTimelineEvent
import com.v7lthronyx.scamynx.domain.model.ThreatType
import kotlinx.coroutines.flow.Flow

interface ThreatIntelligenceService {


    suspend fun lookupIoC(value: String, type: IoCType): IndicatorOfCompromise?

    suspend fun batchLookupIoCs(iocs: Map<String, IoCType>): Map<String, IndicatorOfCompromise?>

    suspend fun searchIoCs(
        query: String,
        types: List<IoCType>? = null,
        threatTypes: List<ThreatType>? = null,
        minConfidence: ConfidenceLevel? = null,
        limit: Int = 100,
    ): List<IndicatorOfCompromise>


    suspend fun generateReport(
        target: String,
        targetType: IoCType,
        includeTimeline: Boolean = false,
        includeRelated: Boolean = false,
    ): ThreatIntelligenceReport

    suspend fun getThreatTimeline(
        target: String,
        targetType: IoCType,
    ): List<ThreatTimelineEvent>

    suspend fun getRelatedIoCs(iocId: String): List<IndicatorOfCompromise>


    suspend fun getGeoAttribution(target: String): GeoAttribution?

    suspend fun getThreatActor(actorId: String): ThreatActorProfile?

    suspend fun searchThreatActors(
        query: String,
        limit: Int = 20,
    ): List<ThreatActorProfile>


    suspend fun getMitreTechniques(threatType: ThreatType): List<MitreTechnique>

    suspend fun getMitreTechnique(techniqueId: String): MitreTechnique?


    suspend fun getThreatFeeds(): List<ThreatFeedSource>

    suspend fun syncThreatFeed(feedId: String): Int

    suspend fun syncAllFeeds(): Int

    fun subscribeThreatFeed(): Flow<IndicatorOfCompromise>


    suspend fun getThreatLandscape(): ThreatLandscapeSummary

    suspend fun getTrendingThreats(limit: Int = 10): List<String>

    suspend fun getEmergingThreats(limit: Int = 10): List<String>


    suspend fun reportIoC(ioc: IndicatorOfCompromise): Boolean

    suspend fun submitFeedback(
        iocId: String,
        feedback: IoCFeedback,
        details: String? = null,
    ): Boolean
}

enum class IoCFeedback {
    FALSE_POSITIVE,
    CONFIRMED_THREAT,
    OUTDATED,
    INCORRECT_CLASSIFICATION,
    ADDITIONAL_INFO,
}
