package com.v7lthronyx.scamynx.ui.qrscanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v7lthronyx.scamynx.domain.repository.ThreatFeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QRScannerViewModel @Inject constructor(
    private val threatFeedRepository: ThreatFeedRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(QRScannerUiState())
    val state: StateFlow<QRScannerUiState> = _state.asStateFlow()

    fun onQrCodeDetected(content: String) {
        if (content.isBlank()) return
        _state.update { current ->
            current.copy(
                scannedContent = content,
                analysisResult = null,
            )
        }
    }

    fun analyzeQrCode() {
        val content = _state.value.scannedContent ?: return

        viewModelScope.launch {
            _state.update { it.copy(isAnalyzing = true) }

            try {
                val result = performAnalysis(content)
                _state.update { current ->
                    current.copy(
                        isAnalyzing = false,
                        analysisResult = result,
                    )
                }
            } catch (e: Exception) {
                _state.update { current ->
                    current.copy(
                        isAnalyzing = false,
                        analysisResult = QRAnalysisResult(
                            threatLevel = ThreatLevel.SUSPICIOUS,
                            description = "Could not complete analysis: ${e.message}",
                            recommendations = listOf("Try again later", "Avoid opening unknown links"),
                        ),
                    )
                }
            }
        }
    }

    private suspend fun performAnalysis(content: String): QRAnalysisResult {
        
        val isUrl = content.matches(Regex("^https?://.*"))

        if (isUrl) {
            
            return try {
                val lookupResult = threatFeedRepository.lookupUrl(content)
                when {
                    lookupResult.isMalicious -> QRAnalysisResult(
                        threatLevel = ThreatLevel.DANGEROUS,
                        description = "This QR code contains a malicious URL that may compromise your device or steal your information.",
                        recommendations = listOf(
                            "Do not open this link",
                            "Report this QR code to the source",
                            "Delete any photos of this QR code",
                        ),
                    )
                    else -> {
                        
                        val heuristic = performHeuristicAnalysis(content)
                        if (heuristic.threatLevel != ThreatLevel.SAFE) {
                            heuristic
                        } else {
                            QRAnalysisResult(
                                threatLevel = ThreatLevel.SAFE,
                                description = "This QR code appears to be safe. The linked website has not been flagged by any threat intelligence sources.",
                                recommendations = listOf(
                                    "Always verify URLs before entering sensitive data",
                                ),
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                
                performHeuristicAnalysis(content)
            }
        } else {
            
            return analyzeNonUrlContent(content)
        }
    }

    private fun performHeuristicAnalysis(url: String): QRAnalysisResult {
        val suspiciousPatterns = listOf(
            "bit.ly", "tinyurl", "t.co", "goo.gl",
            "login", "signin", "account", "verify",
            "paypal", "bank", "amazon", "apple",
            "urgent", "suspend", "expire",
        )

        val dangerousPatterns = listOf(
            ".tk",
            ".ml",
            ".ga",
            ".cf",
            ".gq",
            "xn--",
            ".zip",
            ".mov",
        )

        val urlLower = url.lowercase()

        val hasDangerous = dangerousPatterns.any { urlLower.contains(it) }
        val hasSuspicious = suspiciousPatterns.any { urlLower.contains(it) }

        return when {
            hasDangerous -> QRAnalysisResult(
                threatLevel = ThreatLevel.DANGEROUS,
                description = "This URL contains patterns commonly associated with malicious websites.",
                recommendations = listOf(
                    "Do not open this link",
                    "Be extremely cautious with links from unknown sources",
                ),
            )
            hasSuspicious -> QRAnalysisResult(
                threatLevel = ThreatLevel.SUSPICIOUS,
                description = "This URL uses a URL shortener or contains suspicious keywords.",
                recommendations = listOf(
                    "Be cautious - shortened URLs hide the true destination",
                    "Consider using a URL expander tool first",
                ),
            )
            else -> QRAnalysisResult(
                threatLevel = ThreatLevel.SAFE,
                description = "No immediate threats detected based on URL analysis.",
                recommendations = listOf(
                    "Always verify the website before entering credentials",
                ),
            )
        }
    }

    private fun analyzeNonUrlContent(content: String): QRAnalysisResult {
        return when {
            content.startsWith("BEGIN:VCARD") -> QRAnalysisResult(
                threatLevel = ThreatLevel.SAFE,
                description = "This QR code contains contact information (vCard).",
                recommendations = listOf(
                    "Verify the contact details before saving",
                ),
            )
            content.startsWith("WIFI:") -> QRAnalysisResult(
                threatLevel = ThreatLevel.SUSPICIOUS,
                description = "This QR code will connect you to a WiFi network. Unknown networks may monitor your traffic.",
                recommendations = listOf(
                    "Only connect to networks you trust",
                    "Use VPN when connecting to public networks",
                    "Avoid accessing sensitive accounts on unknown networks",
                ),
            )
            content.startsWith("tel:") || content.matches(Regex("^\\+?\\d{10,}$")) -> QRAnalysisResult(
                threatLevel = ThreatLevel.SAFE,
                description = "This QR code contains a phone number.",
                recommendations = listOf(
                    "Verify the phone number before calling",
                ),
            )
            content.startsWith("mailto:") -> QRAnalysisResult(
                threatLevel = ThreatLevel.SAFE,
                description = "This QR code will open your email app.",
                recommendations = listOf(
                    "Verify the email address before sending any information",
                ),
            )
            content.startsWith("sms:") -> QRAnalysisResult(
                threatLevel = ThreatLevel.SUSPICIOUS,
                description = "This QR code will send an SMS. Premium SMS scams may result in charges.",
                recommendations = listOf(
                    "Verify the phone number before sending",
                    "Check for premium rate numbers",
                ),
            )
            else -> QRAnalysisResult(
                threatLevel = ThreatLevel.SAFE,
                description = "This QR code contains plain text content.",
                recommendations = emptyList(),
            )
        }
    }

    fun clearResult() {
        _state.update {
            QRScannerUiState()
        }
    }
}
