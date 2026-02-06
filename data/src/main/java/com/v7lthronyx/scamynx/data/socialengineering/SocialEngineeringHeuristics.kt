package com.v7lthronyx.scamynx.data.socialengineering

import com.v7lthronyx.scamynx.domain.model.ScamIndicator
import com.v7lthronyx.scamynx.domain.model.SocialEngineeringReport
import com.v7lthronyx.scamynx.domain.model.SocialEngineeringRisk
import kotlinx.datetime.Clock
import kotlin.math.min

private val urgencyKeywords = listOf("فوری", "immediately", "urgent", "اقدام فوری", "now", "asap")
private val paymentKeywords = listOf("gift card", "گیفت کارت", "bitcoin", "crypto", "واریز", "transfer fee", "western union")
private val takeoverKeywords = listOf("account locked", "حساب مسدود", "verify identity", "reset password", "تایید حساب")
private val authorityKeywords = listOf("police", "نیروی انتظامی", "fbi", "tax", "مالیات", "دادگاه")
private val giveawayKeywords = listOf("lottery", "قرعه کشی", "winner", "جایزه", "bonus", "airdrop")
private val impersonationKeywords = listOf("official", "پشتیبانی", "support agent", "representative", "bank officer")
private val linkIndicators = listOf("bit.ly", "tinyurl", ".ru", ".cn", "://t.me", "://wa.me")

internal fun analyzeMessageHeuristics(
    message: String,
): Triple<Double, List<ScamIndicator>, List<String>> {
    val normalized = message.lowercase()
    val indicators = mutableSetOf<ScamIndicator>()
    val snippets = mutableListOf<String>()

    fun scan(keywords: List<String>, indicator: ScamIndicator) {
        val hits = keywords.filter { keyword ->
            normalized.contains(keyword.lowercase())
        }
        if (hits.isNotEmpty()) {
            indicators += indicator
            snippets += hits.take(2)
        }
    }

    scan(urgencyKeywords, ScamIndicator.URGENCY)
    scan(paymentKeywords, ScamIndicator.PAYMENT_REQUEST)
    scan(takeoverKeywords, ScamIndicator.ACCOUNT_TAKEOVER)
    scan(authorityKeywords, ScamIndicator.GOVERNMENT_THREAT)
    scan(giveawayKeywords, ScamIndicator.GIVEAWAY)
    scan(impersonationKeywords, ScamIndicator.IMPERSONATION)
    scan(linkIndicators, ScamIndicator.LINK_OBFUSCATION)

    val linkSuspicion = Regex("http(s)?://[^\\s]+").findAll(message).count()
    val allCaps = Regex("[A-Z]{5,}").containsMatchIn(message)
    if (linkSuspicion > 0) {
        indicators += ScamIndicator.LINK_OBFUSCATION
    }
    if (allCaps) {
        indicators += ScamIndicator.URGENCY
    }

    var score = indicators.size * 0.15
    if (linkSuspicion > 0) score += 0.1
    if (allCaps) score += 0.05
    if (message.length < 40) score += 0.05

    return Triple(min(score, 1.0), indicators.toList(), snippets)
}

internal fun toRiskLevel(score: Double): SocialEngineeringRisk = when {
    score >= 0.65 -> SocialEngineeringRisk.HIGH
    score >= 0.35 -> SocialEngineeringRisk.MEDIUM
    else -> SocialEngineeringRisk.LOW
}

internal fun recommendationsFor(indicators: List<ScamIndicator>, breached: Boolean = false): List<String> {
    val suggestions = mutableSetOf<String>()
    if (ScamIndicator.PAYMENT_REQUEST in indicators || ScamIndicator.GIVEAWAY in indicators) {
        suggestions += "هرگز پرداخت یا کد کارت هدیه ارسال نکنید تا اعتبار منبع را حضوری تایید کنید."
    }
    if (ScamIndicator.ACCOUNT_TAKEOVER in indicators || ScamIndicator.IMPERSONATION in indicators) {
        suggestions += "به‌جای کلیک روی لینک پیام، مستقیماً از اپلیکیشن یا وب‌سایت رسمی وارد شوید."
    }
    if (ScamIndicator.GOVERNMENT_THREAT in indicators) {
        suggestions += "نهادهای رسمی هیچ‌گاه از پیام فوری یا تهدید دستگیری برای دریافت پول استفاده نمی‌کنند."
    }
    if (ScamIndicator.LINK_OBFUSCATION in indicators) {
        suggestions += "لینک‌های کوتاه‌شده یا ناشناس را قبل از باز کردن در اسکمینکس اسکن کنید."
    }
    if (suggestions.isEmpty()) {
        suggestions += "اگر از صحت پیام مطمئن نیستید، از کانال رسمی با فرستنده تماس بگیرید."
    }
    return suggestions.toList()
}

internal fun buildReport(message: String): SocialEngineeringReport {
    val triple = analyzeMessageHeuristics(message)
    val score = triple.first
    val indicators = triple.second
    val snippets = triple.third
    val risk = toRiskLevel(score)
    val recos = recommendationsFor(indicators)
    return SocialEngineeringReport(
        originalMessage = message,
        normalizedMessage = message.lowercase(),
        riskScore = score,
        riskLevel = risk,
        indicators = indicators,
        highlightSnippets = snippets,
        recommendations = recos,
        timestamp = Clock.System.now(),
    )
}
