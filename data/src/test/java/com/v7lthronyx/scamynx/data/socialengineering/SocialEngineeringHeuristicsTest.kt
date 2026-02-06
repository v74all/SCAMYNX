package com.v7lthronyx.scamynx.data.socialengineering

import com.v7lthronyx.scamynx.domain.model.ScamIndicator
import com.v7lthronyx.scamynx.domain.model.SocialEngineeringRisk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialEngineeringHeuristicsTest {

    @Test
    fun `urgent payment request returns high score`() {
        val report = buildReport("URGENT: pay the tax fee in gift cards now!")
        assertTrue(report.riskLevel == SocialEngineeringRisk.HIGH || report.riskLevel == SocialEngineeringRisk.MEDIUM)
        assertTrue(report.indicators.contains(ScamIndicator.PAYMENT_REQUEST))
        assertTrue(report.indicators.contains(ScamIndicator.URGENCY))
    }

    @Test
    fun `benign greeting yields low risk`() {
        val report = buildReport("سلام، فردا جلسه ساعت ۱۰ برگزار میشه؟")
        assertEquals(SocialEngineeringRisk.LOW, report.riskLevel)
        assertTrue(report.indicators.isEmpty())
    }
}
