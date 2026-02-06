package com.v7lthronyx.scamynx.data.darkwebmonitoring

import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.domain.model.ActivityType
import com.v7lthronyx.scamynx.domain.model.AlertAction
import com.v7lthronyx.scamynx.domain.model.AlertPreferences
import com.v7lthronyx.scamynx.domain.model.AlertType
import com.v7lthronyx.scamynx.domain.model.AssetType
import com.v7lthronyx.scamynx.domain.model.BreachSeverity
import com.v7lthronyx.scamynx.domain.model.ConfidenceLevel
import com.v7lthronyx.scamynx.domain.model.CredentialIntelligence
import com.v7lthronyx.scamynx.domain.model.CredentialRiskFactor
import com.v7lthronyx.scamynx.domain.model.DarkWebActivity
import com.v7lthronyx.scamynx.domain.model.DarkWebAlert
import com.v7lthronyx.scamynx.domain.model.DarkWebExposure
import com.v7lthronyx.scamynx.domain.model.DarkWebMonitoringConfig
import com.v7lthronyx.scamynx.domain.model.DarkWebMonitoringReport
import com.v7lthronyx.scamynx.domain.model.ExposedDataType
import com.v7lthronyx.scamynx.domain.model.ExposureSource
import com.v7lthronyx.scamynx.domain.model.ExposureType
import com.v7lthronyx.scamynx.domain.model.IdentityProtectionLevel
import com.v7lthronyx.scamynx.domain.model.IdentityProtectionStatus
import com.v7lthronyx.scamynx.domain.model.MonitoredAsset
import com.v7lthronyx.scamynx.domain.model.MonitoringStatus
import com.v7lthronyx.scamynx.domain.model.PasswordStrength
import com.v7lthronyx.scamynx.domain.model.RemediationAction
import com.v7lthronyx.scamynx.domain.model.RemediationActionType
import com.v7lthronyx.scamynx.domain.model.RemediationStatus
import com.v7lthronyx.scamynx.domain.model.ReportPeriod
import com.v7lthronyx.scamynx.domain.model.RiskTrend
import com.v7lthronyx.scamynx.domain.model.ScanFrequency
import com.v7lthronyx.scamynx.domain.repository.DarkWebMonitoringRepository
import com.v7lthronyx.scamynx.domain.service.DarkWebMonitoringService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days

@Singleton
class DarkWebMonitoringServiceImpl @Inject constructor(
    private val repository: DarkWebMonitoringRepository,
    private val okHttpClient: OkHttpClient,
    private val clock: Clock,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : DarkWebMonitoringService {

    companion object {
        private const val HIBP_API_URL = "https://api.pwnedpasswords.com/range/"
        private const val HIBP_BREACHES_URL = "https://haveibeenpwned.com/api/v3/breachedaccount/"
    }

    // ==================== Configuration ====================

    override suspend fun getMonitoringConfig(): DarkWebMonitoringConfig = withContext(dispatcher) {
        repository.getConfig()
    }

    override suspend fun enableMonitoring() = withContext(dispatcher) {
        repository.setMonitoringEnabled(true)
        scheduleNextScan()
    }

    override suspend fun disableMonitoring() = withContext(dispatcher) {
        repository.setMonitoringEnabled(false)
    }

    override suspend fun setScanFrequency(frequency: ScanFrequency) = withContext(dispatcher) {
        repository.setScanFrequency(frequency)
        scheduleNextScan()
    }

    override suspend fun updateAlertPreferences(preferences: AlertPreferences) = withContext(dispatcher) {
        repository.setAlertPreferences(preferences)
    }

    private suspend fun scheduleNextScan() {
        val config = repository.getConfig()
        val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
        val nextScan = when (config.scanFrequency) {
            ScanFrequency.REALTIME -> clock.now()
            ScanFrequency.DAILY -> clock.now().plus(1, DateTimeUnit.DAY, tz)
            ScanFrequency.WEEKLY -> clock.now().plus(7, DateTimeUnit.DAY, tz)
            ScanFrequency.MONTHLY -> clock.now().plus(30, DateTimeUnit.DAY, tz)
        }
        repository.updateNextScan(nextScan)
    }

    // ==================== Asset Management ====================

    override suspend fun addMonitoredAsset(type: AssetType, value: String): MonitoredAsset = withContext(dispatcher) {
        val maskedValue = maskAssetValue(type, value)
        val asset = MonitoredAsset(
            id = UUID.randomUUID().toString(),
            type = type,
            value = value,
            maskedValue = maskedValue,
            isVerified = false,
            addedAt = clock.now(),
            lastChecked = null,
            exposureCount = 0,
            status = MonitoringStatus.PENDING_VERIFICATION,
        )
        repository.insertAsset(asset)
        asset
    }

    override suspend fun removeMonitoredAsset(assetId: String) = withContext(dispatcher) {
        repository.deleteAsset(assetId)
    }

    override suspend fun getMonitoredAssets(): List<MonitoredAsset> = withContext(dispatcher) {
        repository.getAllAssets()
    }

    override fun observeMonitoredAssets(): Flow<List<MonitoredAsset>> {
        return repository.observeAssets()
    }

    override suspend fun requestAssetVerification(assetId: String) = withContext(dispatcher) {
        // In a real implementation, this would send a verification email/SMS
        // For now, we'll just mark it as pending verification
        val asset = repository.getAsset(assetId) ?: return@withContext
        repository.updateAsset(asset.copy(status = MonitoringStatus.PENDING_VERIFICATION))
    }

    override suspend fun confirmAssetVerification(assetId: String, code: String): Boolean = withContext(dispatcher) {
        // In a real implementation, this would verify the code
        // For now, we'll accept any 6-digit code
        if (code.length == 6 && code.all { it.isDigit() }) {
            val asset = repository.getAsset(assetId) ?: return@withContext false
            repository.updateAsset(
                asset.copy(
                    isVerified = true,
                    status = MonitoringStatus.ACTIVE,
                )
            )
            true
        } else {
            false
        }
    }

    private fun maskAssetValue(type: AssetType, value: String): String {
        return when (type) {
            AssetType.EMAIL -> {
                val parts = value.split("@")
                if (parts.size == 2) {
                    val localPart = parts[0]
                    val domain = parts[1]
                    val maskedLocal = if (localPart.length > 2) {
                        "${localPart.first()}${"*".repeat(localPart.length - 2)}${localPart.last()}"
                    } else {
                        "*".repeat(localPart.length)
                    }
                    "$maskedLocal@$domain"
                } else {
                    value.take(2) + "*".repeat(value.length - 2)
                }
            }
            AssetType.PHONE -> {
                if (value.length > 4) {
                    "*".repeat(value.length - 4) + value.takeLast(4)
                } else {
                    "*".repeat(value.length)
                }
            }
            AssetType.CREDIT_CARD -> {
                if (value.length >= 16) {
                    "**** **** **** " + value.takeLast(4)
                } else {
                    "*".repeat(value.length - 4) + value.takeLast(4)
                }
            }
            AssetType.SSN -> "***-**-" + value.takeLast(4)
            AssetType.USERNAME -> {
                if (value.length > 2) {
                    "${value.first()}${"*".repeat(value.length - 2)}${value.last()}"
                } else {
                    "*".repeat(value.length)
                }
            }
            else -> {
                if (value.length > 4) {
                    value.take(2) + "*".repeat(value.length - 4) + value.takeLast(2)
                } else {
                    "*".repeat(value.length)
                }
            }
        }
    }

    // ==================== Scanning ====================

    override suspend fun triggerManualScan(): DarkWebMonitoringReport = withContext(dispatcher) {
        val assets = repository.getAllAssets().filter { it.status == MonitoringStatus.ACTIVE }
        val allExposures = mutableListOf<DarkWebExposure>()

        for (asset in assets) {
            val exposures = performAssetScan(asset)
            allExposures.addAll(exposures)
            repository.updateAssetLastChecked(asset.id, clock.now())
            repository.updateAssetExposureCount(asset.id, exposures.size)
        }

        repository.insertExposures(allExposures)
        repository.updateLastScan(clock.now())
        scheduleNextScan()

        // Generate alerts for new critical exposures
        allExposures.filter { it.severity == BreachSeverity.CRITICAL }.forEach { exposure ->
            createAlertForExposure(exposure)
        }

        generateReport()
    }

    override suspend fun scanAsset(assetId: String): List<DarkWebExposure> = withContext(dispatcher) {
        val asset = repository.getAsset(assetId) ?: return@withContext emptyList()
        val exposures = performAssetScan(asset)
        repository.insertExposures(exposures)
        repository.updateAssetLastChecked(assetId, clock.now())
        repository.updateAssetExposureCount(assetId, exposures.size)
        exposures
    }

    override suspend fun quickCheck(type: AssetType, value: String): List<DarkWebExposure> = withContext(dispatcher) {
        when (type) {
            AssetType.EMAIL -> checkEmailBreaches(value, "quick-check")
            AssetType.PHONE -> checkPhoneBreaches(value, "quick-check")
            else -> emptyList()
        }
    }

    private suspend fun performAssetScan(asset: MonitoredAsset): List<DarkWebExposure> {
        return when (asset.type) {
            AssetType.EMAIL -> checkEmailBreaches(asset.value, asset.id)
            AssetType.PHONE -> checkPhoneBreaches(asset.value, asset.id)
            AssetType.USERNAME -> checkUsernameBreaches(asset.value, asset.id)
            else -> emptyList()
        }
    }

    private suspend fun checkEmailBreaches(email: String, assetId: String): List<DarkWebExposure> {
        val sha1Hash = email.lowercase(Locale.US).toSha1()
        val prefix = sha1Hash.substring(0, 5)
        val suffix = sha1Hash.substring(5)

        return try {
            val request = Request.Builder()
                .url("$HIBP_API_URL$prefix")
                .header("Add-Padding", "true")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string() ?: return emptyList()
            val exposures = mutableListOf<DarkWebExposure>()

            body.lines()
                .filter { it.isNotBlank() }
                .forEach { line ->
                    val parts = line.split(":")
                    if (parts.size == 2) {
                        val hashSuffix = parts[0]
                        val count = parts[1].trim().toIntOrNull() ?: 0
                        if (hashSuffix.equals(suffix, ignoreCase = true) && count > 0) {
                            exposures.add(
                                DarkWebExposure(
                                    id = UUID.randomUUID().toString(),
                                    assetId = assetId,
                                    assetType = AssetType.EMAIL,
                                    maskedAsset = maskAssetValue(AssetType.EMAIL, email),
                                    source = ExposureSource.DATA_DUMP,
                                    exposureType = ExposureType.CREDENTIAL_LEAK,
                                    exposedData = listOf(ExposedDataType.EMAIL, ExposedDataType.PASSWORD),
                                    severity = calculateSeverity(count),
                                    confidence = ConfidenceLevel.HIGH,
                                    discoveredAt = clock.now(),
                                    recommendations = generateRecommendations(AssetType.EMAIL),
                                )
                            )
                        }
                    }
                }

            exposures
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun checkPhoneBreaches(phone: String, assetId: String): List<DarkWebExposure> {
        // Phone breach checking - use k-anonymity to check if phone appears in password lists
        val normalizedPhone = phone.replace(Regex("[^0-9]"), "")
        if (normalizedPhone.length < 6) return emptyList()
        
        val count = checkPasswordPwned(normalizedPhone)
        if (count > 0) {
            return listOf(
                DarkWebExposure(
                    id = UUID.randomUUID().toString(),
                    assetId = assetId,
                    assetType = AssetType.PHONE,
                    maskedAsset = maskAssetValue(AssetType.PHONE, phone),
                    source = ExposureSource.DATA_DUMP,
                    exposureType = ExposureType.PII_LEAK,
                    exposedData = listOf(ExposedDataType.PHONE_NUMBER),
                    severity = calculateSeverity(count),
                    confidence = ConfidenceLevel.MEDIUM,
                    discoveredAt = clock.now(),
                    recommendations = generateRecommendations(AssetType.PHONE),
                )
            )
        }
        return emptyList()
    }

    private suspend fun checkUsernameBreaches(username: String, assetId: String): List<DarkWebExposure> {
        if (username.isEmpty()) return emptyList()
        
        val exposures = mutableListOf<DarkWebExposure>()
        
        // Method 1: Check if username appears in password lists (indicates credential reuse)
        val usernameAsPasswordCount = checkPasswordPwned(username)
        if (usernameAsPasswordCount > 0) {
            exposures.add(
                DarkWebExposure(
                    id = UUID.randomUUID().toString(),
                    assetId = assetId,
                    assetType = AssetType.USERNAME,
                    maskedAsset = maskAssetValue(AssetType.USERNAME, username),
                    source = ExposureSource.COMBO_LIST,
                    exposureType = ExposureType.CREDENTIAL_LEAK,
                    exposedData = listOf(ExposedDataType.USERNAME, ExposedDataType.PASSWORD),
                    severity = if (usernameAsPasswordCount > 1000) BreachSeverity.HIGH else BreachSeverity.MEDIUM,
                    confidence = ConfidenceLevel.MEDIUM,
                    discoveredAt = clock.now(),
                    recommendations = listOf(
                        "Never use your username as a password",
                        "Change passwords on accounts using this username",
                        "Use a password manager for unique passwords",
                        "Enable two-factor authentication",
                    ),
                )
            )
        }
        
        // Method 2: Check username with common email domains
        val commonDomains = listOf("gmail.com", "yahoo.com", "hotmail.com", "outlook.com")
        for (domain in commonDomains) {
            val emailVariant = "$username@$domain"
            val emailBreaches = checkEmailBreaches(emailVariant, assetId)
            if (emailBreaches.isNotEmpty()) {
                exposures.addAll(emailBreaches.map { breach ->
                    breach.copy(
                        id = UUID.randomUUID().toString(),
                        assetType = AssetType.USERNAME,
                        maskedAsset = maskAssetValue(AssetType.USERNAME, username),
                    )
                })
                break // Found matches, no need to check other domains
            }
        }
        
        return exposures
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
            .url("$HIBP_API_URL$prefix")
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

    private fun calculateSeverity(exposureCount: Int): BreachSeverity {
        return when {
            exposureCount > 100 -> BreachSeverity.CRITICAL
            exposureCount > 50 -> BreachSeverity.HIGH
            exposureCount > 10 -> BreachSeverity.MEDIUM
            else -> BreachSeverity.LOW
        }
    }

    private fun generateRecommendations(assetType: AssetType): List<String> {
        return when (assetType) {
            AssetType.EMAIL -> listOf(
                "Change your password immediately",
                "Enable two-factor authentication",
                "Check for unauthorized account access",
                "Use a password manager for unique passwords",
            )
            AssetType.PHONE -> listOf(
                "Be cautious of phishing calls",
                "Enable SIM PIN protection",
                "Monitor for unauthorized account changes",
            )
            AssetType.CREDIT_CARD -> listOf(
                "Contact your bank immediately",
                "Request a new card",
                "Monitor transactions for fraud",
                "Consider a credit freeze",
            )
            else -> listOf(
                "Monitor for suspicious activity",
                "Change associated passwords",
                "Enable additional security measures",
            )
        }
    }

    // ==================== Exposures ====================

    override suspend fun getAllExposures(): List<DarkWebExposure> = withContext(dispatcher) {
        repository.getAllExposures()
    }

    override suspend fun getExposuresForAsset(assetId: String): List<DarkWebExposure> = withContext(dispatcher) {
        repository.getExposuresForAsset(assetId)
    }

    override fun observeExposures(): Flow<List<DarkWebExposure>> {
        return repository.observeExposures()
    }

    override suspend fun acknowledgeExposure(exposureId: String) = withContext(dispatcher) {
        repository.acknowledgeExposure(exposureId, clock.now())
    }

    override suspend fun updateRemediationStatus(exposureId: String, status: RemediationStatus) = withContext(dispatcher) {
        repository.updateRemediationStatus(exposureId, status)
    }

    override suspend fun getExposureDetails(exposureId: String): DarkWebExposure? = withContext(dispatcher) {
        repository.getExposure(exposureId)
    }

    // ==================== Reports ====================

    override suspend fun generateReport(): DarkWebMonitoringReport = withContext(dispatcher) {
        val exposures = repository.getAllExposures()
        val assets = repository.getAllAssets()
        val config = repository.getConfig()

        val activeExposures = exposures.filter { it.isActive }
        val resolvedExposures = exposures.filter { it.remediationStatus == RemediationStatus.COMPLETED }
        val criticalExposures = activeExposures.filter { it.severity == BreachSeverity.CRITICAL }
        val newExposures = activeExposures.filter { !it.isAcknowledged }

        val exposuresByAsset = exposures.groupBy { it.assetId }.mapValues { it.value.size }
        val exposuresByType = exposures.groupBy { it.exposureType }.mapValues { it.value.size }
        val exposuresBySource = exposures.groupBy { it.source }.mapValues { it.value.size }
        val exposuresBySeverity = exposures.groupBy { it.severity }.mapValues { it.value.size }

        val riskScore = calculateOverallRiskScore(exposures)
        val riskLevel = when {
            riskScore > 0.75 -> BreachSeverity.CRITICAL
            riskScore > 0.5 -> BreachSeverity.HIGH
            riskScore > 0.25 -> BreachSeverity.MEDIUM
            else -> BreachSeverity.LOW
        }

        val recentActivity = exposures.sortedByDescending { it.discoveredAt }.take(10).map { exposure ->
            DarkWebActivity(
                id = UUID.randomUUID().toString(),
                activityType = ActivityType.NEW_EXPOSURE,
                description = "New exposure detected for ${exposure.maskedAsset}",
                severity = exposure.severity,
                timestamp = exposure.discoveredAt,
                relatedExposureId = exposure.id,
            )
        }

        val prioritizedActions = generatePrioritizedActions(exposures)

        DarkWebMonitoringReport(
            totalExposures = exposures.size,
            newExposures = newExposures.size,
            criticalExposures = criticalExposures.size,
            activeExposures = activeExposures.size,
            resolvedExposures = resolvedExposures.size,
            overallRiskScore = riskScore,
            riskLevel = riskLevel,
            riskTrend = RiskTrend.STABLE, // Would require historical data
            exposuresByAsset = exposuresByAsset,
            exposuresByType = exposuresByType,
            exposuresBySource = exposuresBySource,
            exposuresBySeverity = exposuresBySeverity,
            exposures = exposures,
            recentActivity = recentActivity,
            prioritizedActions = prioritizedActions,
            securityTips = listOf(
                "Use unique passwords for each account",
                "Enable two-factor authentication where possible",
                "Regularly monitor your accounts for suspicious activity",
                "Consider using a password manager",
                "Be cautious of phishing emails and messages",
            ),
            monitoredAssets = assets.size,
            lastFullScan = config.lastScan ?: clock.now(),
            reportGeneratedAt = clock.now(),
            reportPeriod = ReportPeriod(
                start = clock.now().minus(30.days),
                end = clock.now(),
            ),
        )
    }

    private fun calculateOverallRiskScore(exposures: List<DarkWebExposure>): Double {
        if (exposures.isEmpty()) return 0.0
        val activeExposures = exposures.filter { it.isActive }
        if (activeExposures.isEmpty()) return 0.0

        val severityScores = activeExposures.map { exposure ->
            when (exposure.severity) {
                BreachSeverity.LOW -> 0.25
                BreachSeverity.MEDIUM -> 0.5
                BreachSeverity.HIGH -> 0.75
                BreachSeverity.CRITICAL -> 1.0
            }
        }
        return severityScores.average()
    }

    private fun generatePrioritizedActions(exposures: List<DarkWebExposure>): List<RemediationAction> {
        val actions = mutableListOf<RemediationAction>()
        var priority = 1

        val criticalExposures = exposures.filter { it.severity == BreachSeverity.CRITICAL && it.isActive }
        if (criticalExposures.isNotEmpty()) {
            actions.add(
                RemediationAction(
                    id = UUID.randomUUID().toString(),
                    priority = priority++,
                    title = "Change compromised passwords immediately",
                    description = "Your credentials have been found in critical breaches. Change passwords now.",
                    actionType = RemediationActionType.CHANGE_PASSWORD,
                    affectedAssets = criticalExposures.map { it.assetId },
                    estimatedTime = "5-10 minutes",
                    automatable = false,
                )
            )
        }

        val emailExposures = exposures.filter { it.assetType == AssetType.EMAIL && it.isActive }
        if (emailExposures.isNotEmpty()) {
            actions.add(
                RemediationAction(
                    id = UUID.randomUUID().toString(),
                    priority = priority++,
                    title = "Enable two-factor authentication",
                    description = "Protect exposed accounts with 2FA",
                    actionType = RemediationActionType.ENABLE_2FA,
                    affectedAssets = emailExposures.map { it.assetId }.distinct(),
                    estimatedTime = "15-20 minutes",
                    automatable = false,
                )
            )
        }

        return actions
    }

    override suspend fun getCredentialIntelligence(email: String): CredentialIntelligence = withContext(dispatcher) {
        val exposures = quickCheck(AssetType.EMAIL, email)
        val maskedEmail = maskAssetValue(AssetType.EMAIL, email)

        CredentialIntelligence(
            email = email,
            maskedEmail = maskedEmail,
            totalExposures = exposures.size,
            uniquePasswords = 0, // Would require deeper breach analysis
            plaintextPasswords = 0,
            hashedPasswords = exposures.size,
            passwordPatterns = emptyList(),
            commonPasswords = emptyList(),
            passwordStrengthDistribution = mapOf(
                PasswordStrength.WEAK to 0,
                PasswordStrength.FAIR to 0,
                PasswordStrength.STRONG to 0,
            ),
            associatedUsernames = emptyList(),
            associatedPhones = emptyList(),
            associatedNames = emptyList(),
            associatedAddresses = emptyList(),
            riskFactors = if (exposures.isNotEmpty()) {
                listOf(
                    CredentialRiskFactor(
                        factor = "Password Reuse Risk",
                        severity = BreachSeverity.HIGH,
                        description = "Email found in data breaches, password may be compromised",
                        affectedBreaches = exposures.size,
                    )
                )
            } else {
                emptyList()
            },
            recommendations = generateRecommendations(AssetType.EMAIL),
            analyzedAt = clock.now(),
        )
    }

    // ==================== Alerts ====================

    override suspend fun getAlerts(): List<DarkWebAlert> = withContext(dispatcher) {
        repository.getAllAlerts()
    }

    override fun observeAlerts(): Flow<List<DarkWebAlert>> {
        return repository.observeAlerts()
    }

    override suspend fun getUnreadAlertsCount(): Int = withContext(dispatcher) {
        repository.getUnreadAlertsCount()
    }

    override suspend fun markAlertAsRead(alertId: String) = withContext(dispatcher) {
        repository.markAlertAsRead(alertId)
    }

    override suspend fun markAllAlertsAsRead() = withContext(dispatcher) {
        repository.markAllAlertsAsRead()
    }

    override suspend fun dismissAlert(alertId: String) = withContext(dispatcher) {
        repository.deleteAlert(alertId)
    }

    private suspend fun createAlertForExposure(exposure: DarkWebExposure) {
        val alert = DarkWebAlert(
            id = UUID.randomUUID().toString(),
            type = if (exposure.severity == BreachSeverity.CRITICAL) AlertType.CRITICAL_EXPOSURE else AlertType.NEW_EXPOSURE,
            severity = exposure.severity,
            title = "New Dark Web Exposure Detected",
            message = "Your ${exposure.assetType.name.lowercase()} was found in a data breach",
            exposureId = exposure.id,
            assetId = exposure.assetId,
            isRead = false,
            isActioned = false,
            createdAt = clock.now(),
            expiresAt = clock.now().plus(30, DateTimeUnit.DAY, kotlinx.datetime.TimeZone.currentSystemDefault()),
            actions = listOf(
                AlertAction(
                    id = "view",
                    label = "View Details",
                    actionType = "view_exposure",
                    isPrimary = true,
                ),
                AlertAction(
                    id = "dismiss",
                    label = "Dismiss",
                    actionType = "dismiss",
                    isPrimary = false,
                ),
            ),
        )
        repository.insertAlert(alert)
    }

    // ==================== Identity Protection ====================

    override suspend fun getIdentityProtectionStatus(): IdentityProtectionStatus = withContext(dispatcher) {
        val config = repository.getConfig()
        val activeAlerts = repository.getUnreadAlertsCount()
        val exposures = repository.getAllExposures()
        val resolvedAlerts = exposures.count { it.remediationStatus == RemediationStatus.COMPLETED }

        IdentityProtectionStatus(
            isProtected = config.isEnabled,
            protectionLevel = if (config.isPremium) IdentityProtectionLevel.PREMIUM else IdentityProtectionLevel.BASIC,
            coveredAssets = config.monitoredAssets.map { it.type }.distinct(),
            activeAlerts = activeAlerts,
            resolvedAlerts = resolvedAlerts,
            lastIncident = exposures.maxByOrNull { it.discoveredAt }?.discoveredAt,
            insuranceCoverage = null, // Premium feature
            restorationServices = config.isPremium,
        )
    }

    // ==================== Utility Functions ====================

    private fun String.toSha1(): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val hashBytes = digest.digest(this.toByteArray())
        return hashBytes.joinToString("") { "%02X".format(it) }
    }
}
