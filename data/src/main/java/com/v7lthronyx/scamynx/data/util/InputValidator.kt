package com.v7lthronyx.scamynx.data.util

import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InputValidator @Inject constructor() {

    fun validateUrl(url: String): ValidationResult {
        val trimmed = url.trim()

        return when {
            trimmed.isEmpty() -> ValidationResult.Error("URL cannot be empty")
            trimmed.length > MAX_URL_LENGTH -> ValidationResult.Error("URL is too long (max $MAX_URL_LENGTH characters)")
            !isValidUrlFormat(trimmed) -> ValidationResult.Error("Invalid URL format. Please include http:// or https://")
            else -> {
                try {
                    val normalized = normalizeUrl(trimmed)
                    if (normalized.length > MAX_URL_LENGTH) {
                        ValidationResult.Error("Normalized URL exceeds maximum length")
                    } else {
                        ValidationResult.Success(normalized)
                    }
                } catch (e: Exception) {
                    ValidationResult.Error("Invalid URL: ${e.message ?: "Unknown error"}")
                }
            }
        }
    }

    fun validateVpnConfig(config: String): ValidationResult {
        val trimmed = config.trim()

        return when {
            trimmed.isEmpty() -> ValidationResult.Error("VPN configuration cannot be empty")
            trimmed.length > MAX_VPN_CONFIG_LENGTH -> ValidationResult.Error("Configuration is too long (max $MAX_VPN_CONFIG_LENGTH characters)")
            !containsVpnKeywords(trimmed) -> ValidationResult.Error("Configuration doesn't appear to be a valid VPN config")
            else -> ValidationResult.Success(trimmed)
        }
    }

    fun validateInstagramHandle(handle: String): ValidationResult {
        val trimmed = handle.trim().removePrefix("@")

        return when {
            trimmed.isEmpty() -> ValidationResult.Error("Instagram handle cannot be empty")
            trimmed.length > MAX_INSTAGRAM_HANDLE_LENGTH -> ValidationResult.Error("Handle is too long (max $MAX_INSTAGRAM_HANDLE_LENGTH characters)")
            !isValidInstagramHandle(trimmed) -> ValidationResult.Error("Invalid Instagram handle format")
            else -> ValidationResult.Success(trimmed)
        }
    }

    fun validateFileSize(sizeBytes: Long): ValidationResult {
        return when {
            sizeBytes <= 0 -> ValidationResult.Error("File size must be greater than 0")
            sizeBytes > MAX_FILE_SIZE_BYTES -> ValidationResult.Error("File is too large (max ${MAX_FILE_SIZE_BYTES / (1024 * 1024)} MB)")
            else -> ValidationResult.Success(null)
        }
    }

    fun validateFileType(fileName: String, mimeType: String?): ValidationResult {
        val lowerFileName = fileName.lowercase()
        val lowerMimeType = mimeType?.lowercase() ?: ""

        val extension = lowerFileName.substringAfterLast('.', "")

        val dangerousExtensions = setOf(
            "apk", "exe", "bat", "cmd", "com", "pif", "scr", "vbs", "js", "jar",
            "msi", "dll", "sys", "drv", "ocx", "cpl", "app", "deb", "rpm",
            "sh", "bin", "run", "dmg", "pkg", "appimage",
        )

        val dangerousMimeTypes = setOf(
            "application/vnd.android.package-archive",
            "application/x-msdownload",
            "application/x-msdos-program",
            "application/x-executable",
            "application/x-ms-installer",
            "application/x-sharedlib",
            "application/x-dll",
            "application/x-shellscript",
            "application/x-binary",
            "application/x-ms-wmz",
            "application/x-ms-wmd",
        )

        return when {
            extension.isEmpty() && mimeType.isNullOrBlank() -> {
                ValidationResult.Error("File type cannot be determined. Please ensure the file has a valid extension.")
            }
            extension in dangerousExtensions -> {
                val fileTypeName = getFileTypeDisplayName(extension)
                ValidationResult.Warning(
                    extension = extension,
                    message = "This file type ($fileTypeName) may be potentially dangerous. Executable files can harm your device. Please ensure you trust the source before scanning.",
                )
            }
            lowerMimeType in dangerousMimeTypes -> {
                val fileTypeName = getFileTypeDisplayName(extension.ifEmpty { "executable" })
                ValidationResult.Warning(
                    extension = extension.ifEmpty { "executable" },
                    message = "This file type ($fileTypeName) may be potentially dangerous. Executable files can harm your device. Please ensure you trust the source before scanning.",
                )
            }
            else -> ValidationResult.Success(extension)
        }
    }

    private fun getFileTypeDisplayName(extension: String): String {
        return when (extension.lowercase()) {
            "apk" -> "Android Package (APK)"
            "exe" -> "Windows Executable (EXE)"
            "bat", "cmd" -> "Batch Script"
            "msi" -> "Windows Installer (MSI)"
            "dll" -> "Dynamic Link Library (DLL)"
            "sh" -> "Shell Script"
            "app" -> "macOS Application"
            "deb" -> "Debian Package"
            "rpm" -> "RPM Package"
            "dmg" -> "macOS Disk Image"
            "pkg" -> "macOS Installer Package"
            "appimage" -> "AppImage"
            else -> extension.uppercase()
        }
    }

    private fun isValidUrlFormat(url: String): Boolean {
        return try {
            val hasScheme = url.matches(SCHEME_REGEX)
            val hasValidChars = url.matches(VALID_URL_CHARS_REGEX)
            hasScheme || hasValidChars
        } catch (e: Exception) {
            false
        }
    }

    private fun normalizeUrl(url: String): String {
        val candidate = if (url.matches(SCHEME_REGEX)) url else "https://$url"
        val uri = URI(candidate)
        requireNotNull(uri.host) { "URL must have a valid host" }
        return candidate
    }

    private fun containsVpnKeywords(config: String): Boolean {
        val lower = config.lowercase()
        return VPN_KEYWORDS.any { lower.contains(it) }
    }

    private fun isValidInstagramHandle(handle: String): Boolean {
        return handle.matches(INSTAGRAM_HANDLE_REGEX)
    }

    sealed class ValidationResult {
        data class Success(val value: String?) : ValidationResult()
        data class Error(val message: String) : ValidationResult()
        data class Warning(
            val extension: String,
            val message: String,
        ) : ValidationResult()

        val isValid: Boolean get() = this is Success || this is Warning
        val errorMessage: String? get() = (this as? Error)?.message
        val warningMessage: String? get() = (this as? Warning)?.message
    }

    companion object {
        private const val MAX_URL_LENGTH = 2048
        private const val MAX_VPN_CONFIG_LENGTH = 10000
        private const val MAX_INSTAGRAM_HANDLE_LENGTH = 30
        private const val MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024L

        private val SCHEME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
        private val VALID_URL_CHARS_REGEX = Regex("^[a-zA-Z0-9][a-zA-Z0-9.-]*\\.[a-zA-Z]{2,}")
        private val INSTAGRAM_HANDLE_REGEX = Regex("^[a-zA-Z0-9._]{1,30}$")

        private val VPN_KEYWORDS = listOf(
            "vless", "vmess", "trojan", "shadowsocks", "ss", "ssr",
            "server", "port", "uuid", "password", "encryption", "method",
            "network", "security", "tls", "sni", "alpn",
        )

        val DANGEROUS_FILE_EXTENSIONS = setOf(
            "apk", "exe", "bat", "cmd", "com", "pif", "scr", "vbs", "js", "jar",
            "msi", "dll", "sys", "drv", "ocx", "cpl", "app", "deb", "rpm",
            "sh", "bin", "run", "dmg", "pkg", "appimage",
        )
    }
}
