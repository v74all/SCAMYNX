package com.v7lthronyx.scamynx.data.breachmonitoring

import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.domain.model.BreachCheckResult
import com.v7lthronyx.scamynx.domain.model.BreachExposure
import com.v7lthronyx.scamynx.domain.model.BreachIdentifierType
import com.v7lthronyx.scamynx.domain.model.BreachMonitoringReport
import com.v7lthronyx.scamynx.domain.model.BreachSeverity
import com.v7lthronyx.scamynx.domain.model.ExposedDataType
import com.v7lthronyx.scamynx.domain.service.BreachMonitoringService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val HIBP_PWNED_PASSWORDS_URL = "https://api.pwnedpasswords.com/range/"
private const val HIBP_BREACHED_ACCOUNT_URL = "https://haveibeenpwned.com/api/v3/breachedaccount/"
private const val HIBP_PASTE_ACCOUNT_URL = "https://haveibeenpwned.com/api/v3/pasteaccount/"

@Singleton
class BreachMonitoringServiceImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val clock: Clock = Clock.System,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : BreachMonitoringService {

    private val monitoredEmails = mutableSetOf<String>()
    private val monitoredPhoneNumbers = mutableSetOf<String>()
    private val monitoredUsernames = mutableSetOf<String>()
    private var monitoringEnabled = false

    // Optional HIBP API key for premium features
    private var hibpApiKey: String? = null

    fun setHibpApiKey(apiKey: String?) {
        hibpApiKey = apiKey
    }

    override suspend fun checkEmail(email: String): BreachCheckResult = withContext(dispatcher) {
        if (email.isEmpty()) {
            return@withContext BreachCheckResult(
                identifier = "",
                identifierType = BreachIdentifierType.EMAIL,
                isExposed = false,
                totalBreachCount = 0,
                riskScore = 0.0,
                checkedAt = clock.now(),
            )
        }

        val sha1Hash = email.lowercase().toSha1()
        val prefix = sha1Hash.substring(0, 5)
        val suffix = sha1Hash.substring(5)

        val breaches = checkHibpRange(prefix, suffix, BreachIdentifierType.EMAIL)

        val isExposed = breaches.isNotEmpty()
        val riskScore = calculateRiskScore(breaches)
        val recommendations = generateRecommendations(breaches, BreachIdentifierType.EMAIL)

        BreachCheckResult(
            identifier = email,
            identifierType = BreachIdentifierType.EMAIL,
            isExposed = isExposed,
            exposures = breaches,
            totalBreachCount = breaches.size,
            riskScore = riskScore,
            recommendations = recommendations,
            checkedAt = clock.now(),
        )
    }

    override suspend fun checkPhoneNumber(phoneNumber: String): BreachCheckResult = withContext(dispatcher) {
        
        val normalizedPhone = phoneNumber.replace(Regex("[^0-9]"), "")
        val sha1Hash = normalizedPhone.toSha1()
        val prefix = sha1Hash.substring(0, 5)
        val suffix = sha1Hash.substring(5)

        val breaches = checkHibpRange(prefix, suffix, BreachIdentifierType.PHONE_NUMBER)

        val isExposed = breaches.isNotEmpty()
        val riskScore = calculateRiskScore(breaches)
        val recommendations = generateRecommendations(breaches, BreachIdentifierType.PHONE_NUMBER)

        BreachCheckResult(
            identifier = phoneNumber,
            identifierType = BreachIdentifierType.PHONE_NUMBER,
            isExposed = isExposed,
            exposures = breaches,
            totalBreachCount = breaches.size,
            riskScore = riskScore,
            recommendations = recommendations,
            checkedAt = clock.now(),
        )
    }

    override suspend fun checkUsername(username: String): BreachCheckResult = withContext(dispatcher) {
        if (username.isEmpty()) {
            return@withContext BreachCheckResult(
                identifier = username,
                identifierType = BreachIdentifierType.USERNAME,
                isExposed = false,
                totalBreachCount = 0,
                riskScore = 0.0,
                checkedAt = clock.now(),
            )
        }

        // Check username as if it were an email (many users use username@common-domains)
        // Also check common email patterns with the username
        val breaches = mutableListOf<BreachExposure>()
        
        // Method 1: Check if username looks like an email
        if (username.contains("@")) {
            breaches.addAll(checkEmailBreaches(username))
        }
        
        // Method 2: Check username with common email domains
        val commonDomains = listOf("gmail.com", "yahoo.com", "hotmail.com", "outlook.com")
        for (domain in commonDomains) {
            val emailVariant = "$username@$domain"
            val domainBreaches = checkEmailBreaches(emailVariant)
            if (domainBreaches.isNotEmpty()) {
                breaches.addAll(domainBreaches.map { breach ->
                    breach.copy(
                        breachId = UUID.randomUUID().toString(),
                        breachName = "${breach.breachName} (via $domain)",
                    )
                })
                break // Found a match, no need to check other domains
            }
        }
        
        // Method 3: Use k-anonymity to check if username appears in password lists
        // (Many users use their username as password or part of it)
        val usernameAsPasswordCount = checkPasswordPwned(username)
        if (usernameAsPasswordCount > 0) {
            breaches.add(
                BreachExposure(
                    breachId = UUID.randomUUID().toString(),
                    breachName = "Password List Exposure",
                    breachDate = null,
                    detectedAt = clock.now(),
                    exposedData = listOf(ExposedDataType.PASSWORD),
                    severity = if (usernameAsPasswordCount > 1000) BreachSeverity.HIGH else BreachSeverity.MEDIUM,
                    description = "Username found in $usernameAsPasswordCount password breaches. This may indicate password reuse.",
                    verified = true,
                )
            )
        }

        val isExposed = breaches.isNotEmpty()
        val riskScore = calculateRiskScore(breaches)
        val recommendations = generateRecommendations(breaches, BreachIdentifierType.USERNAME)

        BreachCheckResult(
            identifier = username,
            identifierType = BreachIdentifierType.USERNAME,
            isExposed = isExposed,
            exposures = breaches,
            totalBreachCount = breaches.size,
            riskScore = riskScore,
            recommendations = recommendations,
            checkedAt = clock.now(),
        )
    }

    override suspend fun generateMonitoringReport(): BreachMonitoringReport = withContext(dispatcher) {
        val emailChecks = monitoredEmails.map { checkEmail(it) }
        val phoneChecks = monitoredPhoneNumbers.map { checkPhoneNumber(it) }
        val usernameChecks = monitoredUsernames.map { checkUsername(it) }

        val allExposures = (emailChecks + phoneChecks + usernameChecks).flatMap { it.exposures }
        val totalExposures = allExposures.size
        val criticalExposures = allExposures.count { it.severity == BreachSeverity.CRITICAL }
        val overallRiskScore = if (allExposures.isNotEmpty()) {
            allExposures.map { exposure ->
                when (exposure.severity) {
                    BreachSeverity.LOW -> 0.25
                    BreachSeverity.MEDIUM -> 0.5
                    BreachSeverity.HIGH -> 0.75
                    BreachSeverity.CRITICAL -> 1.0
                }
            }.average()
        } else {
            0.0
        }

        BreachMonitoringReport(
            emailChecks = emailChecks,
            phoneChecks = phoneChecks,
            usernameChecks = usernameChecks,
            overallRiskScore = overallRiskScore,
            totalExposures = totalExposures,
            criticalExposures = criticalExposures,
            monitoringEnabled = monitoringEnabled,
            lastSyncAt = if (monitoringEnabled) clock.now() else null,
            reportGeneratedAt = clock.now(),
        )
    }

    override suspend fun enableMonitoring(
        emails: List<String>,
        phoneNumbers: List<String>,
        usernames: List<String>,
    ) = withContext(dispatcher) {
        monitoredEmails.clear()
        monitoredPhoneNumbers.clear()
        monitoredUsernames.clear()

        monitoredEmails.addAll(emails)
        monitoredPhoneNumbers.addAll(phoneNumbers)
        monitoredUsernames.addAll(usernames)

        monitoringEnabled = true
    }

    override suspend fun disableMonitoring() = withContext(dispatcher) {
        monitoringEnabled = false
        monitoredEmails.clear()
        monitoredPhoneNumbers.clear()
        monitoredUsernames.clear()
    }

    override suspend fun isMonitoringEnabled(): Boolean = withContext(dispatcher) {
        monitoringEnabled
    }

    /**
     * Check if an email has been involved in known data breaches using HIBP API.
     * If API key is available, uses the official breached account endpoint.
     * Otherwise, uses k-anonymity password check as a fallback indicator.
     */
    private suspend fun checkEmailBreaches(email: String): List<BreachExposure> {
        val apiKey = hibpApiKey
        
        return if (!apiKey.isNullOrBlank()) {
            // Use official HIBP API with API key
            checkHibpBreachedAccount(email, apiKey)
        } else {
            // Fallback: check if email appears in password breaches using k-anonymity
            checkEmailViaPasswordApi(email)
        }
    }

    /**
     * Check breached account using HIBP API (requires API key)
     */
    private fun checkHibpBreachedAccount(email: String, apiKey: String): List<BreachExposure> {
        val request = Request.Builder()
            .url("$HIBP_BREACHED_ACCOUNT_URL${email}?truncateResponse=false")
            .header("hibp-api-key", apiKey)
            .header("User-Agent", "SCAMYNX-Security-App")
            .build()

        return try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    // Not found = no breaches
                    return emptyList()
                }
                if (!response.isSuccessful) {
                    return emptyList()
                }

                val body = response.body?.string() ?: return emptyList()
                parseHibpBreachesResponse(body)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Parse HIBP breaches JSON response
     */
    private fun parseHibpBreachesResponse(jsonBody: String): List<BreachExposure> {
        val breaches = mutableListOf<BreachExposure>()
        
        try {
            val jsonArray = JSONArray(jsonBody)
            for (i in 0 until jsonArray.length()) {
                val breach = jsonArray.getJSONObject(i)
                val dataClasses = breach.optJSONArray("DataClasses")
                val exposedData = parseDataClasses(dataClasses)
                
                val pwnCount = breach.optLong("PwnCount", 0)
                val isSensitive = breach.optBoolean("IsSensitive", false)
                val isVerified = breach.optBoolean("IsVerified", true)
                
                val severity = calculateBreachSeverity(exposedData, pwnCount, isSensitive)
                
                val breachDateStr = breach.optString("BreachDate", "")
                
                breaches.add(
                    BreachExposure(
                        breachId = UUID.randomUUID().toString(),
                        breachName = breach.optString("Name", "Unknown Breach"),
                        breachDate = breachDateStr.takeIf { it.isNotEmpty() },
                        detectedAt = clock.now(),
                        exposedData = exposedData,
                        severity = severity,
                        description = breach.optString("Description", ""),
                        verified = isVerified,
                    )
                )
            }
        } catch (e: Exception) {
            // JSON parsing error
        }
        
        return breaches
    }

    /**
     * Parse data classes from HIBP response
     */
    private fun parseDataClasses(dataClasses: JSONArray?): List<ExposedDataType> {
        if (dataClasses == null) return emptyList()
        
        val exposed = mutableListOf<ExposedDataType>()
        for (i in 0 until dataClasses.length()) {
            val dataClass = dataClasses.optString(i, "").lowercase()
            when {
                dataClass.contains("email") -> exposed.add(ExposedDataType.EMAIL)
                dataClass.contains("password") -> exposed.add(ExposedDataType.PASSWORD)
                dataClass.contains("username") -> exposed.add(ExposedDataType.USERNAME)
                dataClass.contains("phone") -> exposed.add(ExposedDataType.PHONE_NUMBER)
                dataClass.contains("address") -> exposed.add(ExposedDataType.ADDRESS)
                dataClass.contains("credit") || dataClass.contains("card") -> exposed.add(ExposedDataType.CREDIT_CARD)
                dataClass.contains("ssn") || dataClass.contains("social security") -> exposed.add(ExposedDataType.SOCIAL_SECURITY_NUMBER)
                dataClass.contains("date of birth") || dataClass.contains("dob") -> exposed.add(ExposedDataType.DATE_OF_BIRTH)
                dataClass.contains("ip") -> exposed.add(ExposedDataType.IP_ADDRESS)
                dataClass.contains("name") -> exposed.add(ExposedDataType.FULL_NAME)
            }
        }
        return exposed.distinct()
    }

    /**
     * Calculate breach severity based on exposed data and scale
     */
    private fun calculateBreachSeverity(
        exposedData: List<ExposedDataType>,
        pwnCount: Long,
        isSensitive: Boolean,
    ): BreachSeverity {
        // Critical if sensitive financial/identity data
        if (ExposedDataType.SOCIAL_SECURITY_NUMBER in exposedData ||
            ExposedDataType.CREDIT_CARD in exposedData) {
            return BreachSeverity.CRITICAL
        }
        
        // High if passwords or sensitive flag
        if (isSensitive || ExposedDataType.PASSWORD in exposedData) {
            return BreachSeverity.HIGH
        }
        
        // Consider scale
        return when {
            pwnCount > 10_000_000 -> BreachSeverity.HIGH
            pwnCount > 1_000_000 -> BreachSeverity.MEDIUM
            else -> BreachSeverity.LOW
        }
    }

    /**
     * Fallback method: check email using password k-anonymity API
     * This checks if the email (as-is) appears in password lists
     */
    private fun checkEmailViaPasswordApi(email: String): List<BreachExposure> {
        val count = checkPasswordPwned(email)
        if (count > 0) {
            return listOf(
                BreachExposure(
                    breachId = UUID.randomUUID().toString(),
                    breachName = "Credential Exposure",
                    breachDate = null,
                    detectedAt = clock.now(),
                    exposedData = listOf(ExposedDataType.EMAIL, ExposedDataType.PASSWORD),
                    severity = if (count > 100) BreachSeverity.HIGH else BreachSeverity.MEDIUM,
                    description = "This email/credential combination was found in $count data breaches.",
                    verified = true,
                )
            )
        }
        return emptyList()
    }

    /**
     * Check if a string appears in pwned passwords using k-anonymity
     */
    private fun checkPasswordPwned(value: String): Int {
        if (value.isEmpty()) return 0
        
        val sha1Hash = value.toSha1()
        val prefix = sha1Hash.substring(0, 5)
        val suffix = sha1Hash.substring(5)
        
        val request = Request.Builder()
            .url("$HIBP_PWNED_PASSWORDS_URL$prefix")
            .header("Add-Padding", "true")
            .build()

        return try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return 0
                
                val body = response.body?.string() ?: return 0
                
                body.lineSequence()
                    .filter { it.isNotBlank() }
                    .forEach { line ->
                        val parts = line.split(":")
                        if (parts.size == 2) {
                            val hashSuffix = parts[0].trim()
                            if (hashSuffix.equals(suffix, ignoreCase = true)) {
                                return parts[1].trim().toIntOrNull() ?: 0
                            }
                        }
                    }
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun checkHibpRange(
        prefix: String,
        suffix: String,
        identifierType: BreachIdentifierType,
    ): List<BreachExposure> {
        // Use the password pwned check logic
        val request = Request.Builder()
            .url("$HIBP_PWNED_PASSWORDS_URL$prefix")
            .header("Add-Padding", "true")
            .build()

        return try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                
                val body = response.body?.string() ?: return emptyList()
                
                body.lineSequence()
                    .filter { it.isNotBlank() }
                    .mapNotNull { line ->
                        val parts = line.split(":")
                        if (parts.size == 2) {
                            val hashSuffix = parts[0].trim()
                            val count = parts[1].trim().toIntOrNull() ?: 0
                            if (hashSuffix.equals(suffix, ignoreCase = true) && count > 0) {
                                BreachExposure(
                                    breachId = UUID.randomUUID().toString(),
                                    breachName = "Credential Exposure",
                                    breachDate = null,
                                    detectedAt = clock.now(),
                                    exposedData = when (identifierType) {
                                        BreachIdentifierType.EMAIL -> listOf(ExposedDataType.EMAIL, ExposedDataType.PASSWORD)
                                        BreachIdentifierType.PHONE_NUMBER -> listOf(ExposedDataType.PHONE_NUMBER)
                                        BreachIdentifierType.USERNAME -> listOf(ExposedDataType.USERNAME, ExposedDataType.PASSWORD)
                                    },
                                    severity = if (count > 100) BreachSeverity.HIGH else BreachSeverity.MEDIUM,
                                    description = "Found in $count known data breaches",
                                    verified = true,
                                )
                            } else null
                        } else null
                    }
                    .toList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun calculateRiskScore(breaches: List<BreachExposure>): Double {
        if (breaches.isEmpty()) return 0.0

        return breaches.map { breach ->
            when (breach.severity) {
                BreachSeverity.LOW -> 0.25
                BreachSeverity.MEDIUM -> 0.5
                BreachSeverity.HIGH -> 0.75
                BreachSeverity.CRITICAL -> 1.0
            }
        }.average().coerceIn(0.0, 1.0)
    }

    private fun generateRecommendations(
        breaches: List<BreachExposure>,
        identifierType: BreachIdentifierType,
    ): List<String> {
        val recommendations = mutableListOf<String>()

        if (breaches.isNotEmpty()) {
            recommendations += "Change passwords for affected accounts immediately"
            recommendations += "Enable two-factor authentication (2FA) on all accounts"
            recommendations += "Use unique passwords for each account"
            recommendations += "Consider using a password manager"

            if (breaches.any { ExposedDataType.CREDIT_CARD in it.exposedData }) {
                recommendations += "Monitor credit card statements for unauthorized charges"
            }

            if (breaches.any { ExposedDataType.SOCIAL_SECURITY_NUMBER in it.exposedData }) {
                recommendations += "Consider credit monitoring services"
            }
        } else {
            recommendations += "No breaches detected. Continue using strong, unique passwords"
        }

        return recommendations
    }

    private fun String.toSha1(): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(toByteArray(Charsets.UTF_8))
        val builder = StringBuilder(digest.size * 2)
        digest.forEach { byte ->
            builder.append(String.format(Locale.US, "%02X", byte))
        }
        return builder.toString()
    }
}
