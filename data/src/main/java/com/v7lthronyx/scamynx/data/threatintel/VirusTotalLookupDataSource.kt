package com.v7lthronyx.scamynx.data.threatintel

import android.util.Log
import com.v7lthronyx.scamynx.data.network.api.VirusTotalApi
import com.v7lthronyx.scamynx.data.util.ApiCredentials
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for VirusTotal hash and IP lookups.
 * Uses VirusTotal API v3 for file hash and IP address reports.
 */
@Singleton
class VirusTotalLookupDataSource @Inject constructor(
    private val virusTotalApi: VirusTotalApi,
    private val credentials: ApiCredentials,
) {
    companion object {
        private const val TAG = "VirusTotalLookup"
    }

    /**
     * Result of a hash lookup
     */
    data class HashLookupResult(
        val hash: String,
        val isMalicious: Boolean,
        val maliciousCount: Int,
        val totalEngines: Int,
        val threatLabel: String?,
        val threatCategory: String?,
        val fileType: String?,
        val sources: List<String>,
        val errorMessage: String? = null,
    )

    /**
     * Result of an IP lookup
     */
    data class IpLookupResult(
        val ip: String,
        val isMalicious: Boolean,
        val maliciousCount: Int,
        val suspiciousCount: Int,
        val totalEngines: Int,
        val country: String?,
        val asOwner: String?,
        val reputation: Int,
        val sources: List<String>,
        val tags: List<String>,
        val errorMessage: String? = null,
    )

    /**
     * Lookup a file hash (MD5, SHA-1, or SHA-256) in VirusTotal.
     */
    suspend fun lookupHash(hash: String): HashLookupResult {
        if (credentials.virusTotalApiKey.isNullOrBlank()) {
            return HashLookupResult(
                hash = hash,
                isMalicious = false,
                maliciousCount = 0,
                totalEngines = 0,
                threatLabel = null,
                threatCategory = null,
                fileType = null,
                sources = emptyList(),
                errorMessage = "VirusTotal API key not configured",
            )
        }

        return try {
            val response = virusTotalApi.getFileReport(hash.lowercase())

            if (response.error != null) {
                Log.w(TAG, "VirusTotal file lookup error: ${response.error.code} - ${response.error.message}")
                return HashLookupResult(
                    hash = hash,
                    isMalicious = false,
                    maliciousCount = 0,
                    totalEngines = 0,
                    threatLabel = null,
                    threatCategory = null,
                    fileType = null,
                    sources = emptyList(),
                    errorMessage = response.error.message ?: "Unknown error",
                )
            }

            val attributes = response.data?.attributes
            val stats = attributes?.lastAnalysisStats

            val maliciousCount = stats?.malicious ?: 0
            val suspiciousCount = stats?.suspicious ?: 0
            val totalEngines = (stats?.harmless ?: 0) + maliciousCount + suspiciousCount +
                (stats?.undetected ?: 0) + (stats?.timeout ?: 0) + (stats?.failure ?: 0)

            // Get malicious engine names
            val maliciousEngines = attributes?.lastAnalysisResults
                ?.filter { it.value.category == "malicious" || it.value.category == "suspicious" }
                ?.keys
                ?.take(5)
                ?.toList()
                ?: emptyList()

            val threatClassification = attributes?.popularThreatClassification
            val threatLabel = threatClassification?.suggestedThreatLabel
            val threatCategory = threatClassification?.popularThreatCategory?.firstOrNull()?.value

            HashLookupResult(
                hash = hash,
                isMalicious = maliciousCount > 0 || suspiciousCount > 0,
                maliciousCount = maliciousCount + suspiciousCount,
                totalEngines = totalEngines,
                threatLabel = threatLabel,
                threatCategory = threatCategory,
                fileType = attributes?.typeDescription ?: attributes?.typeTag,
                sources = if (maliciousEngines.isNotEmpty()) maliciousEngines else listOf("VirusTotal"),
            )
        } catch (e: retrofit2.HttpException) {
            val errorMessage = when (e.code()) {
                404 -> "Hash not found in VirusTotal database"
                401 -> "Invalid VirusTotal API key"
                429 -> "VirusTotal rate limit exceeded"
                else -> "HTTP error: ${e.code()}"
            }
            Log.w(TAG, "VirusTotal file lookup HTTP error: ${e.code()}", e)
            HashLookupResult(
                hash = hash,
                isMalicious = false,
                maliciousCount = 0,
                totalEngines = 0,
                threatLabel = null,
                threatCategory = null,
                fileType = null,
                sources = emptyList(),
                errorMessage = errorMessage,
            )
        } catch (e: Exception) {
            Log.e(TAG, "VirusTotal file lookup failed", e)
            HashLookupResult(
                hash = hash,
                isMalicious = false,
                maliciousCount = 0,
                totalEngines = 0,
                threatLabel = null,
                threatCategory = null,
                fileType = null,
                sources = emptyList(),
                errorMessage = "Network error: ${e.message}",
            )
        }
    }

    /**
     * Lookup an IP address in VirusTotal.
     */
    suspend fun lookupIp(ip: String): IpLookupResult {
        if (credentials.virusTotalApiKey.isNullOrBlank()) {
            return IpLookupResult(
                ip = ip,
                isMalicious = false,
                maliciousCount = 0,
                suspiciousCount = 0,
                totalEngines = 0,
                country = null,
                asOwner = null,
                reputation = 0,
                sources = emptyList(),
                tags = emptyList(),
                errorMessage = "VirusTotal API key not configured",
            )
        }

        return try {
            val response = virusTotalApi.getIpReport(ip)

            if (response.error != null) {
                Log.w(TAG, "VirusTotal IP lookup error: ${response.error.code} - ${response.error.message}")
                return IpLookupResult(
                    ip = ip,
                    isMalicious = false,
                    maliciousCount = 0,
                    suspiciousCount = 0,
                    totalEngines = 0,
                    country = null,
                    asOwner = null,
                    reputation = 0,
                    sources = emptyList(),
                    tags = emptyList(),
                    errorMessage = response.error.message ?: "Unknown error",
                )
            }

            val attributes = response.data?.attributes
            val stats = attributes?.lastAnalysisStats

            val maliciousCount = stats?.malicious ?: 0
            val suspiciousCount = stats?.suspicious ?: 0
            val totalEngines = (stats?.harmless ?: 0) + maliciousCount + suspiciousCount +
                (stats?.undetected ?: 0) + (stats?.timeout ?: 0)

            // Get malicious engine names
            val maliciousEngines = attributes?.lastAnalysisResults
                ?.filter { it.value.category == "malicious" || it.value.category == "suspicious" }
                ?.keys
                ?.take(5)
                ?.toList()
                ?: emptyList()

            IpLookupResult(
                ip = ip,
                isMalicious = maliciousCount > 0,
                maliciousCount = maliciousCount,
                suspiciousCount = suspiciousCount,
                totalEngines = totalEngines,
                country = attributes?.country,
                asOwner = attributes?.asOwner,
                reputation = attributes?.reputation ?: 0,
                sources = if (maliciousEngines.isNotEmpty()) maliciousEngines else listOf("VirusTotal"),
                tags = attributes?.tags ?: emptyList(),
            )
        } catch (e: retrofit2.HttpException) {
            val errorMessage = when (e.code()) {
                404 -> "IP address not found in VirusTotal database"
                401 -> "Invalid VirusTotal API key"
                429 -> "VirusTotal rate limit exceeded"
                else -> "HTTP error: ${e.code()}"
            }
            Log.w(TAG, "VirusTotal IP lookup HTTP error: ${e.code()}", e)
            IpLookupResult(
                ip = ip,
                isMalicious = false,
                maliciousCount = 0,
                suspiciousCount = 0,
                totalEngines = 0,
                country = null,
                asOwner = null,
                reputation = 0,
                sources = emptyList(),
                tags = emptyList(),
                errorMessage = errorMessage,
            )
        } catch (e: Exception) {
            Log.e(TAG, "VirusTotal IP lookup failed", e)
            IpLookupResult(
                ip = ip,
                isMalicious = false,
                maliciousCount = 0,
                suspiciousCount = 0,
                totalEngines = 0,
                country = null,
                asOwner = null,
                reputation = 0,
                sources = emptyList(),
                tags = emptyList(),
                errorMessage = "Network error: ${e.message}",
            )
        }
    }
}
