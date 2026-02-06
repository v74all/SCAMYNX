package com.v7lthronyx.scamynx.data.antiphishing

import com.v7lthronyx.scamynx.domain.model.LinkDisposition
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeuristicAntiPhishingAnalyzerTest {

    private val analyzer = HeuristicAntiPhishingAnalyzer()

    @Test
    fun `ip based url is flagged as malicious`() = runBlocking {
        val result = analyzer.analyze("http://23.15.9.1/login/update")
        assertTrue(result.disposition != LinkDisposition.SAFE)
        assertTrue(result.score >= 0.6)
    }

    @Test
    fun `trusted https host stays safe`() = runBlocking {
        val result = analyzer.analyze("https://example.com/welcome")
        assertEquals(LinkDisposition.SAFE, result.disposition)
    }

    @Test
    fun `reputation indicator boosts score`() = runBlocking {
        val result = analyzer.analyze("https://secure-meta-help.example.com/login")
        assertTrue(result.reputationMatches.isNotEmpty())
        assertTrue(result.score >= 0.45)
    }
}
