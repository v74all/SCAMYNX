package com.v7lthronyx.scamynx.data.heuristics

import com.v7lthronyx.scamynx.data.db.ThreatIndicatorEntity
import com.v7lthronyx.scamynx.data.db.ThreatFeedDao
import com.v7lthronyx.scamynx.domain.model.Provider
import java.util.LinkedHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LocalHeuristicsEvaluatorTest {

    private val dispatcher = StandardTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true }
    private val dao = FakeThreatFeedDao()
    private lateinit var evaluator: LocalHeuristicsEvaluator

    @Before
    fun setUp() {
        evaluator = LocalHeuristicsEvaluator(dao, json, dispatcher)
        dao.reset()
    }

    @Test
    fun flagsBrandSpoofUrlAsMalicious() = runTest(dispatcher) {
        val verdict = evaluator.evaluate("http://secure-login-paypal-support.com/update/account")

        assertEquals(Provider.LOCAL_HEURISTIC, verdict.provider)
        assertEquals(
            "Expected brand spoof to be classified as malicious",
            com.v7lthronyx.scamynx.domain.model.VerdictStatus.MALICIOUS,
            verdict.status,
        )
        assertTrue("score=${verdict.score} should be high for spoofed brand", verdict.score >= 0.7)
        assertEquals("paypal", verdict.details["spoof_brand"])
    }

    @Test
    fun integratesThreatFeedMatchesIntoVerdict() = runTest(dispatcher) {
        val tagSerializer = ListSerializer(String.serializer())
        dao.insertAll(
            listOf(
                ThreatIndicatorEntity(
                    indicatorId = "id-001",
                    url = "https://phishy-alert.biz/login",
                    riskScore = 92.0,
                    tagsJson = json.encodeToString(tagSerializer, listOf("phishing", "credential-harvest")),
                    lastSeenEpochMillis = 1_700_000_000_000,
                    source = "scamfeed",
                    fetchedAtEpochMillis = 1_700_000_000_500,
                ),
            ),
        )

        val verdict = evaluator.evaluate("https://phishy-alert.biz/login?token=123")

        assertEquals(
            com.v7lthronyx.scamynx.domain.model.VerdictStatus.MALICIOUS,
            verdict.status,
        )
        assertEquals("scamfeed", verdict.details["threat_feed_sources"])
        assertTrue(
            "threat feed score should reflect high risk",
            verdict.details["threat_feed_score"]?.toDoubleOrNull()?.let { it >= 0.9 } == true,
        )
        assertEquals("phishing, credential-harvest", verdict.details["threat_feed_tags"])
    }

    @Test
    fun leavesLegitimateBrandDomainClean() = runTest(dispatcher) {
        val verdict = evaluator.evaluate("https://accounts.google.com/signin/v2/identifier")

        assertEquals(
            com.v7lthronyx.scamynx.domain.model.VerdictStatus.CLEAN,
            verdict.status,
        )
        assertTrue("Legitimate brand domain should have low score", verdict.score < 0.3)
    }

    private class FakeThreatFeedDao : ThreatFeedDao {
        private val clock = AtomicLong(0L)
        private val storage = LinkedHashMap<String, ThreatIndicatorEntity>()

        override suspend fun insertAll(indicators: List<ThreatIndicatorEntity>) {
            indicators.forEach { entity ->
                storage[entity.indicatorId] = entity.copy(
                    fetchedAtEpochMillis = entity.fetchedAtEpochMillis.takeIf { it > 0 } ?: clock.incrementAndGet(),
                )
            }
        }

        override suspend fun clearAll() {
            storage.clear()
            clock.set(0L)
        }

        override fun observeLatest(limit: Int): Flow<List<ThreatIndicatorEntity>> {
            return flowOf(storage.values.sortedByDescending { it.fetchedAtEpochMillis }.take(limit))
        }

        override suspend fun findByUrl(url: String): List<ThreatIndicatorEntity> {
            return storage.values.filter { it.url.equals(url, ignoreCase = true) }
        }

        override suspend fun findByHost(host: String, limit: Int): List<ThreatIndicatorEntity> {
            if (host.isBlank()) return emptyList()
            return storage.values
                .filter { it.url.contains(host, ignoreCase = true) }
                .sortedByDescending { it.fetchedAtEpochMillis }
                .take(limit)
        }

        fun reset() {
            storage.clear()
            clock.set(0L)
        }
    }
}
