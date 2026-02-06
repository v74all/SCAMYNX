package com.v7lthronyx.scamynx.ui.threatintel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v7lthronyx.scamynx.data.threatintel.VirusTotalLookupDataSource
import com.v7lthronyx.scamynx.domain.repository.ThreatFeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThreatIntelViewModel @Inject constructor(
    private val threatFeedRepository: ThreatFeedRepository,
    private val virusTotalLookup: VirusTotalLookupDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(ThreatIntelUiState())
    val state: StateFlow<ThreatIntelUiState> = _state.asStateFlow()

    private companion object {
        const val TAG = "ThreatIntelViewModel"
    }

    init {
        loadThreatFeeds()
    }

    private fun loadThreatFeeds() {
        viewModelScope.launch {
            try {
                
                val feeds = listOf(
                    ThreatFeedUiModel(
                        id = "threatfox",
                        name = "ThreatFox by Abuse.ch",
                        indicatorCount = 15420,
                        isActive = true,
                    ),
                    ThreatFeedUiModel(
                        id = "urlhaus",
                        name = "URLhaus Malware URLs",
                        indicatorCount = 8234,
                        isActive = true,
                    ),
                    ThreatFeedUiModel(
                        id = "phishtank",
                        name = "PhishTank",
                        indicatorCount = 12567,
                        isActive = true,
                    ),
                    ThreatFeedUiModel(
                        id = "gsb",
                        name = "Google Safe Browsing",
                        indicatorCount = 0,
                        isActive = true,
                    ),
                    ThreatFeedUiModel(
                        id = "virustotal",
                        name = "VirusTotal",
                        indicatorCount = 0,
                        isActive = true,
                    ),
                )

                _state.update { current ->
                    current.copy(
                        threatFeeds = feeds,
                        activeFeeds = feeds.count { it.isActive },
                        totalIndicators = feeds.sumOf { it.indicatorCount },
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load threat feeds", e)
            }
        }
    }

    fun searchIoC(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return

        val validationResult = validateIoCFormat(trimmedQuery)
        if (!validationResult.isValid) {
            _state.update { current ->
                current.copy(
                    searchResults = listOf(
                        IoCResultUiModel(
                            value = trimmedQuery,
                            isMalicious = false,
                            threatType = validationResult.errorMessage,
                            sources = emptyList(),
                        ),
                    ),
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }

            try {
                
                val result = when (validationResult.iocType) {
                    IoCType.URL -> searchUrl(trimmedQuery)
                    IoCType.HASH -> searchHash(trimmedQuery)
                    IoCType.IP -> searchIp(trimmedQuery)
                    IoCType.DOMAIN -> searchDomain(trimmedQuery)
                }

                _state.update { current ->
                    current.copy(
                        isSearching = false,
                        searchResults = listOf(result),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSearching = false) }
            }
        }
    }

    private fun validateIoCFormat(query: String): IoCValidationResult {
        
        if (query.matches(Regex("^https?://.*"))) {
            return try {
                java.net.URL(query)
                IoCValidationResult(isValid = true, iocType = IoCType.URL)
            } catch (_: Exception) {
                IoCValidationResult(isValid = false, errorMessage = "Invalid URL format")
            }
        }

        if (query.matches(Regex("^[a-fA-F0-9]{32}$"))) {
            return IoCValidationResult(isValid = true, iocType = IoCType.HASH)
        }
        if (query.matches(Regex("^[a-fA-F0-9]{40}$"))) {
            return IoCValidationResult(isValid = true, iocType = IoCType.HASH)
        }
        if (query.matches(Regex("^[a-fA-F0-9]{64}$"))) {
            return IoCValidationResult(isValid = true, iocType = IoCType.HASH)
        }

        if (query.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"))) {
            val octets = query.split(".").map { it.toIntOrNull() ?: -1 }
            return if (octets.all { it in 0..255 }) {
                IoCValidationResult(isValid = true, iocType = IoCType.IP)
            } else {
                IoCValidationResult(isValid = false, errorMessage = "Invalid IP address (octets must be 0-255)")
            }
        }

        if (query.matches(Regex("^[a-zA-Z0-9][a-zA-Z0-9\\-\\.]*[a-zA-Z0-9]\\.[a-zA-Z]{2,}$"))) {
            return IoCValidationResult(isValid = true, iocType = IoCType.DOMAIN)
        }

        if (query.matches(Regex("^[a-fA-F0-9]+$"))) {
            return IoCValidationResult(
                isValid = false,
                errorMessage = "Invalid hash length. Expected: MD5 (32), SHA1 (40), or SHA256 (64) characters",
            )
        }

        return if (query.length < 3) {
            IoCValidationResult(isValid = false, errorMessage = "Query too short. Enter a valid URL, IP, domain, or hash")
        } else {
            IoCValidationResult(isValid = true, iocType = IoCType.DOMAIN)
        }
    }

    private enum class IoCType { URL, HASH, IP, DOMAIN }

    private data class IoCValidationResult(
        val isValid: Boolean,
        val iocType: IoCType = IoCType.DOMAIN,
        val errorMessage: String = "",
    )

    private suspend fun searchUrl(url: String): IoCResultUiModel {
        return try {
            val result = threatFeedRepository.lookupUrl(url)
            IoCResultUiModel(
                value = url,
                isMalicious = result.isMalicious,
                threatType = result.threatType ?: "Unknown",
                sources = result.sources,
            )
        } catch (e: Exception) {
            IoCResultUiModel(
                value = url,
                isMalicious = false,
                threatType = "Error checking",
                sources = emptyList(),
            )
        }
    }

    private suspend fun searchHash(hash: String): IoCResultUiModel {
        return try {
            val result = virusTotalLookup.lookupHash(hash)
            val errorMsg = result.errorMessage
            if (errorMsg != null) {
                IoCResultUiModel(
                    value = hash,
                    isMalicious = false,
                    threatType = errorMsg,
                    sources = emptyList(),
                )
            } else {
                val threatType = when {
                    result.isMalicious -> buildString {
                        append("Malicious")
                        result.threatLabel?.let { append(" - $it") }
                        result.threatCategory?.let { append(" ($it)") }
                        append(" [${result.maliciousCount}/${result.totalEngines} detections]")
                    }
                    result.totalEngines > 0 -> "Clean [0/${result.totalEngines} detections]"
                    else -> "Unknown"
                }
                IoCResultUiModel(
                    value = hash,
                    isMalicious = result.isMalicious,
                    threatType = threatType,
                    sources = result.sources,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Hash lookup failed", e)
            IoCResultUiModel(
                value = hash,
                isMalicious = false,
                threatType = "Error: ${e.message ?: "Unknown error"}",
                sources = emptyList(),
            )
        }
    }

    private suspend fun searchIp(ip: String): IoCResultUiModel {
        return try {
            val result = virusTotalLookup.lookupIp(ip)
            val errorMsg = result.errorMessage
            if (errorMsg != null) {
                IoCResultUiModel(
                    value = ip,
                    isMalicious = false,
                    threatType = errorMsg,
                    sources = emptyList(),
                )
            } else {
                val threatType = when {
                    result.isMalicious -> buildString {
                        append("Malicious")
                        append(" [${result.maliciousCount}/${result.totalEngines} detections]")
                        result.country?.let { append(" - $it") }
                        result.asOwner?.let { append(" ($it)") }
                    }
                    result.suspiciousCount > 0 -> buildString {
                        append("Suspicious")
                        append(" [${result.suspiciousCount}/${result.totalEngines}]")
                        result.country?.let { append(" - $it") }
                    }
                    result.totalEngines > 0 -> buildString {
                        append("Clean [0/${result.totalEngines}]")
                        result.country?.let { append(" - $it") }
                        result.asOwner?.let { append(" ($it)") }
                    }
                    else -> "Unknown"
                }
                IoCResultUiModel(
                    value = ip,
                    isMalicious = result.isMalicious || result.suspiciousCount > 0,
                    threatType = threatType,
                    sources = result.sources,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "IP lookup failed", e)
            IoCResultUiModel(
                value = ip,
                isMalicious = false,
                threatType = "Error: ${e.message ?: "Unknown error"}",
                sources = emptyList(),
            )
        }
    }

    private suspend fun searchDomain(domain: String): IoCResultUiModel {
        return try {
            val url = "https://$domain"
            val result = threatFeedRepository.lookupUrl(url)
            IoCResultUiModel(
                value = domain,
                isMalicious = result.isMalicious,
                threatType = result.threatType ?: "Unknown",
                sources = result.sources,
            )
        } catch (e: Exception) {
            IoCResultUiModel(
                value = domain,
                isMalicious = false,
                threatType = "Error checking",
                sources = emptyList(),
            )
        }
    }

    fun refreshFeeds() {
        viewModelScope.launch {
            try {
                threatFeedRepository.syncThreatFeeds()
                loadThreatFeeds()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh threat feeds", e)
            }
        }
    }
}
