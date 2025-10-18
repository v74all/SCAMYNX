package com.v7lthronyx.scamynx.data.analyzer

import com.v7lthronyx.scamynx.data.di.ThreatIntelJson
import com.v7lthronyx.scamynx.domain.model.IssueSeverity
import com.v7lthronyx.scamynx.domain.model.Provider
import com.v7lthronyx.scamynx.domain.model.ScanIssue
import com.v7lthronyx.scamynx.domain.model.VendorVerdict
import com.v7lthronyx.scamynx.domain.model.VerdictStatus
import com.v7lthronyx.scamynx.domain.model.VpnConfigReport
import java.net.IDN
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray

private val V2RAY_LINK_PREFIXES = listOf(
    "vmess://",
    "vless://",
    "trojan://",
    "ss://",
)

private val SEVERITY_WEIGHTS = mapOf(
    IssueSeverity.LOW to 0.12,
    IssueSeverity.MEDIUM to 0.35,
    IssueSeverity.HIGH to 0.6,
    IssueSeverity.CRITICAL to 0.85,
)

@Singleton
class VpnConfigAnalyzer @Inject constructor(
    @ThreatIntelJson private val json: Json,
) {

    data class Result(
        val report: VpnConfigReport,
        val verdict: VendorVerdict,
    )

    fun analyze(rawConfig: String): Result {
        val normalized = rawConfig.trim()
        val issues = mutableListOf<ScanIssue>()
        val insecureTransports = mutableListOf<String>()

        var serverAddress: String? = null
        var port: Int? = null
        var tlsEnabled: Boolean? = null
        var outboundType: String? = null
        var clientType: String? = null

        val candidates = mutableListOf<String>()
        decodeBase64(normalized)?.let { candidates += it }
        candidates += normalized
        candidates += extractPrefixedPayloads(normalized)

        candidates.forEach { candidate ->
            val trimmed = candidate.trim()
            if (trimmed.isBlank()) return@forEach
            when {
                looksLikeJson(trimmed) -> parseJsonConfig(
                    trimmed,
                    issues,
                    insecureTransports,
                ).also { parsed ->
                    parsed.server?.let { if (serverAddress == null) serverAddress = it }
                    parsed.port?.let { if (port == null) port = it }
                    parsed.tlsEnabled?.let { tlsEnabled = tlsEnabled ?: it }
                    parsed.outbound?.let { outboundType = outboundType ?: it }
                    parsed.client?.let { clientType = clientType ?: it }
                    insecureTransports += parsed.insecureTransports
                }
                trimmed.startsWith("vmess://", ignoreCase = true) -> parseVmessLink(
                    trimmed,
                    issues,
                    insecureTransports,
                ).also { parsed ->
                    parsed.server?.let { if (serverAddress == null) serverAddress = it }
                    parsed.port?.let { if (port == null) port = it }
                    parsed.tlsEnabled?.let { tlsEnabled = tlsEnabled ?: it }
                    parsed.outbound?.let { outboundType = outboundType ?: it }
                    parsed.client?.let { clientType = clientType ?: it }
                    insecureTransports += parsed.insecureTransports
                }
                trimmed.startsWith("vless://", ignoreCase = true) -> parseVlessLink(
                    trimmed,
                    issues,
                    insecureTransports,
                ).also { parsed ->
                    parsed.server?.let { if (serverAddress == null) serverAddress = it }
                    parsed.port?.let { if (port == null) port = it }
                    parsed.tlsEnabled?.let { tlsEnabled = tlsEnabled ?: it }
                    parsed.outbound?.let { outboundType = outboundType ?: it }
                    insecureTransports += parsed.insecureTransports
                }
                trimmed.startsWith("trojan://", ignoreCase = true) -> parseTrojanLink(
                    trimmed,
                    issues,
                    insecureTransports,
                ).also { parsed ->
                    parsed.server?.let { if (serverAddress == null) serverAddress = it }
                    parsed.port?.let { if (port == null) port = it }
                    parsed.tlsEnabled?.let { tlsEnabled = tlsEnabled ?: it }
                    parsed.outbound?.let { outboundType = outboundType ?: it }
                    parsed.client?.let { clientType = clientType ?: it }
                    insecureTransports += parsed.insecureTransports
                }
                trimmed.startsWith("ss://", ignoreCase = true) -> parseShadowsocksLink(
                    trimmed,
                    issues,
                    insecureTransports,
                ).also { parsed ->
                    parsed.server?.let { if (serverAddress == null) serverAddress = it }
                    parsed.port?.let { if (port == null) port = it }
                    parsed.tlsEnabled?.let { tlsEnabled = tlsEnabled ?: it }
                    parsed.outbound?.let { outboundType = outboundType ?: it }
                    parsed.client?.let { clientType = clientType ?: it }
                    insecureTransports += parsed.insecureTransports
                }
            }
        }

        if (tlsEnabled == null && insecureTransports.isEmpty()) {
            issues.addIfAbsent(
                ScanIssue(
                    id = "unknown_transport",
                    title = "Transport security unknown",
                    severity = IssueSeverity.LOW,
                    description = "Unable to confirm whether TLS is enabled for this configuration.",
                ),
            )
        }

        if (serverAddress != null && isPrivateEndpoint(serverAddress!!)) {
            issues.addIfAbsent(
                ScanIssue(
                    id = "private_endpoint",
                    title = "Private or reserved endpoint",
                    severity = IssueSeverity.MEDIUM,
                    description = "Endpoint $serverAddress is a private or reserved address which is uncommon for public VPN tunnels.",
                ),
            )
        }

        if (tlsEnabled == false && port != null && port == 443) {
            issues.addIfAbsent(
                ScanIssue(
                    id = "no_tls_on_tls_port",
                    title = "TLS disabled on port 443",
                    severity = IssueSeverity.HIGH,
                    description = "Connection is configured for port 443 but TLS is disabled, suggesting an insecure tunnel.",
                ),
            )
        }

        if (tlsEnabled != true && insecureTransports.any { it.equals("grpc", true) || it.equals("ws", true) }) {
            issues.addIfAbsent(
                ScanIssue(
                    id = "non_tls_overlay",
                    title = "Overlay transport without TLS",
                    severity = IssueSeverity.MEDIUM,
                    description = "Overlay transports such as gRPC or WebSocket should be paired with TLS to prevent interception.",
                ),
            )
        }

        val riskScore = issues.sumOf { SEVERITY_WEIGHTS[it.severity] ?: 0.0 }
            .coerceIn(0.0, 1.0)
        val status = when {
            riskScore >= 0.7 -> VerdictStatus.MALICIOUS
            riskScore >= 0.45 -> VerdictStatus.SUSPICIOUS
            riskScore >= 0.2 -> VerdictStatus.UNKNOWN
            else -> VerdictStatus.CLEAN
        }

        val report = VpnConfigReport(
            clientType = clientType,
            outboundType = outboundType,
            serverAddress = serverAddress,
            port = port,
            tlsEnabled = tlsEnabled,
            insecureTransports = insecureTransports.distinct(),
            issues = issues,
            riskScore = riskScore,
        )
        val verdict = VendorVerdict(
            provider = Provider.VPN_CONFIG,
            status = status,
            score = riskScore,
            details = mapOf(
                "server" to serverAddress,
                "port" to port?.toString(),
                "tls" to tlsEnabled?.toString(),
                "issues" to issues.size.takeIf { it > 0 }?.toString(),
            ).filterValues { it != null },
        )
        return Result(report = report, verdict = verdict)
    }

    private fun parseJsonConfig(
        candidate: String,
        issues: MutableList<ScanIssue>,
        insecureTransports: MutableList<String>,
    ): ParsedConfig {
        val element = runCatching { json.parseToJsonElement(candidate) }.getOrNull()
        val obj = element as? JsonObject ?: return ParsedConfig()
        val outbounds = obj["outbounds"]
        val firstOutbound = outbounds?.asJsonArrayOrNull()?.firstOrNull()?.jsonObjectOrNull()
        val protocol = firstOutbound?.get("protocol")?.asString()
        val streamSettings = firstOutbound?.get("streamSettings")?.jsonObjectOrNull()
        val security = streamSettings?.get("security")?.asString()
        val tlsSettings = streamSettings?.get("tlsSettings")?.jsonObjectOrNull()
        val allowInsecure = tlsSettings?.get("allowInsecure")?.asBoolean()
            ?: streamSettings?.get("allowInsecure")?.asBoolean()
        val wsSettings = streamSettings?.get("wsSettings")?.jsonObjectOrNull()
        val network = streamSettings?.get("network")?.asString()
            ?: firstOutbound?.get("transport")?.asString()

        val server = firstOutbound
            ?.get("settings")?.jsonObjectOrNull()
            ?.get("vnext")?.asJsonArrayOrNull()
            ?.firstOrNull()?.jsonObjectOrNull()
            ?.get("address")?.asString()

        val port = firstOutbound
            ?.get("settings")?.jsonObjectOrNull()
            ?.get("vnext")?.asJsonArrayOrNull()
            ?.firstOrNull()?.jsonObjectOrNull()
            ?.get("port")?.asString()?.toIntOrNull()
            ?: obj["port"]?.asString()?.toIntOrNull()

        val tlsEnabled = when {
            security.isNullOrBlank() -> false
            security.equals("tls", ignoreCase = true) -> true
            security.equals("reality", ignoreCase = true) -> true
            security.equals("xtls", ignoreCase = true) -> true
            security.equals("none", ignoreCase = true) -> false
            else -> null
        }

        if (allowInsecure == true) {
            issues.addIfAbsent(
                ScanIssue(
                    id = "allow_insecure_tls",
                    title = "Certificate validation disabled",
                    severity = IssueSeverity.HIGH,
                    description = "allowInsecure is enabled which makes the tunnel vulnerable to MITM attacks.",
                ),
            )
        }

        if ((tlsEnabled == false || tlsEnabled == null) && !network.isNullOrBlank()) {
            insecureTransports += network
            issues.addIfAbsent(
                ScanIssue(
                    id = "no_tls_${network.lowercase(Locale.US)}",
                    title = "Transport without TLS",
                    severity = IssueSeverity.MEDIUM,
                    description = "Transport $network does not enforce TLS according to the configuration.",
                ),
            )
        }

        if (wsSettings != null && (tlsEnabled != true)) {
            issues.addIfAbsent(
                ScanIssue(
                    id = "ws_without_tls",
                    title = "Websocket without TLS",
                    severity = IssueSeverity.MEDIUM,
                    description = "Websocket transport must be paired with TLS to prevent interception.",
                ),
            )
        }

        if (port != null && port in setOf(80, 8080, 8000, 2086)) {
            issues.addIfAbsent(
                ScanIssue(
                    id = "unencrypted_port",
                    title = "Uses unencrypted port $port",
                    severity = IssueSeverity.HIGH,
                    description = "Common unencrypted ports indicate the tunnel may be exposed.",
                ),
            )
        }

        return ParsedConfig(
            server = server,
            port = port,
            tlsEnabled = tlsEnabled,
            outbound = protocol,
            client = obj["app"]?.asString() ?: protocol,
            insecureTransports = insecureTransports.toList(),
        )
    }

    private fun parseVlessLink(
        link: String,
        issues: MutableList<ScanIssue>,
        insecureTransports: MutableList<String>,
    ): ParsedConfig {
        val uri = runCatching { URI(link) }.getOrNull() ?: return ParsedConfig()
        val params = decodeQuery(uri.rawQuery)
        val security = params["security"]?.lowercase(Locale.US)
        val flow = params["flow"]?.lowercase(Locale.US)
        val port = if (uri.port >= 0) uri.port else params["port"]?.toIntOrNull()
        val transport = params["type"] ?: params["network"]

        val tlsEnabled = when {
            security == null -> null
            security == "tls" || security == "reality" -> true
            security == "xtls" -> true
            security == "none" -> false
            else -> null
        }

        if (params["allowInsecure"] == "1" || params["allowInsecure"]?.equals("true", true) == true) {
            issues.addIfAbsent(
                ScanIssue(
                    id = "allow_insecure_vless",
                    title = "allowInsecure enabled",
                    severity = IssueSeverity.HIGH,
                    description = "allowInsecure bypasses TLS checks, exposing the tunnel to interception.",
                ),
            )
        }

        if (transport != null && tlsEnabled != true) {
            insecureTransports += transport
        }

        if (!flow.isNullOrBlank() && flow.contains("xtls-rprx-vision") && tlsEnabled != true) {
            issues.addIfAbsent(
                ScanIssue(
                    id = "xtls_without_tls",
                    title = "XTLS without TLS",
                    severity = IssueSeverity.MEDIUM,
                    description = "XTLS flow should run over TLS-enabled connections.",
                ),
            )
        }

        return ParsedConfig(
            server = uri.host,
            port = port,
            tlsEnabled = tlsEnabled,
            outbound = "vless",
            client = params["sni"] ?: params["serviceName"],
            insecureTransports = insecureTransports.toList(),
        )
    }

    private fun parseVmessLink(
        link: String,
        issues: MutableList<ScanIssue>,
        insecureTransports: MutableList<String>,
    ): ParsedConfig {
        val encoded = link.removePrefix("vmess://")
        val decoded = decodeBase64(encoded) ?: return ParsedConfig()
        val element = runCatching { json.parseToJsonElement(decoded) }.getOrNull()
        val obj = element as? JsonObject ?: return ParsedConfig()

        val server = obj["add"]?.asString()
        val port = obj["port"]?.asString()?.toIntOrNull()
        val security = obj["tls"]?.asString()?.lowercase(Locale.US)
        val network = obj["net"]?.asString()?.lowercase(Locale.US)
        val cipher = obj["scy"]?.asString()?.lowercase(Locale.US)
        val host = obj["host"]?.asString() ?: obj["sni"]?.asString()

        val tlsEnabled = when {
            security == null -> null
            security == "tls" || security == "reality" || security == "xtls" -> true
            security == "none" -> false
            else -> null
        }

        if (cipher != null && cipher !in STRONG_SHADOWSOCKS_CIPHERS) {
            issues.addIfAbsent(
                ScanIssue(
                    id = "vmess_weak_cipher",
                    title = "Weak cipher configured",
                    severity = IssueSeverity.MEDIUM,
                    description = "The cipher \"$cipher\" is considered weak for VMess connections.",
                ),
            )
        }

        if (network != null && tlsEnabled != true) {
            insecureTransports += network
        }

        return ParsedConfig(
            server = server,
            port = port,
            tlsEnabled = tlsEnabled,
            outbound = network ?: "vmess",
            client = host ?: obj["ps"]?.asString(),
            insecureTransports = if (network != null && tlsEnabled != true) listOf(network) else emptyList(),
        )
    }

    private fun parseTrojanLink(
        link: String,
        issues: MutableList<ScanIssue>,
        insecureTransports: MutableList<String>,
    ): ParsedConfig {
        val uri = runCatching { URI(link) }.getOrNull() ?: return ParsedConfig()
        val params = decodeQuery(uri.rawQuery)
        val password = uri.userInfo
        val security = params["security"]?.lowercase(Locale.US)
        val sni = params["sni"]
        val host = uri.host
        val port = if (uri.port >= 0) uri.port else params["port"]?.toIntOrNull()

        if (password.isNullOrBlank()) {
            issues.addIfAbsent(
                ScanIssue(
                    id = "trojan_missing_password",
                    title = "Trojan password missing",
                    severity = IssueSeverity.CRITICAL,
                    description = "Trojan connections without a password expose the server to abuse.",
                ),
            )
        }

        val tlsEnabled = when (security) {
            null -> true // trojan typically implies TLS
            "tls", "reality" -> true
            "none" -> false
            else -> null
        }

        if (tlsEnabled != true) {
            issues.addIfAbsent(
                ScanIssue(
                    id = "trojan_without_tls",
                    title = "Trojan without TLS",
                    severity = IssueSeverity.HIGH,
                    description = "Trojan protocol mandates TLS; disablement indicates misconfiguration or insecure tunnel.",
                ),
            )
        }

        if (params["allowInsecure"] == "1" || params["allowInsecure"]?.equals("true", true) == true) {
            issues.addIfAbsent(
                ScanIssue(
                    id = "trojan_allow_insecure",
                    title = "TLS verification disabled",
                    severity = IssueSeverity.HIGH,
                    description = "allowInsecure=true disables certificate validation for the trojan tunnel.",
                ),
            )
        }

        if (tlsEnabled != true) {
            insecureTransports += "trojan"
        }

        return ParsedConfig(
            server = host,
            port = port,
            tlsEnabled = tlsEnabled,
            outbound = "trojan",
            client = sni,
            insecureTransports = if (tlsEnabled != true) listOf("trojan") else emptyList(),
        )
    }

    private fun parseShadowsocksLink(
        link: String,
        issues: MutableList<ScanIssue>,
        insecureTransports: MutableList<String>,
    ): ParsedConfig {
        var payload = link.removePrefix("ss://")
        var tag: String? = null
        val hashIndex = payload.indexOf('#')
        if (hashIndex >= 0) {
            tag = decodeComponent(payload.substring(hashIndex + 1))
            payload = payload.substring(0, hashIndex)
        }

        var query: String? = null
        val queryIndex = payload.indexOf('?')
        if (queryIndex >= 0) {
            query = payload.substring(queryIndex + 1)
            payload = payload.substring(0, queryIndex)
        }

        val decodedBase = decodeBase64(payload)?.takeIf { it.contains("@") } ?: payload
        val credentials = decodedBase.substringBefore('@', missingDelimiterValue = "")
        val endpoint = decodedBase.substringAfter('@', missingDelimiterValue = "")
        val method = credentials.substringBefore(':', missingDelimiterValue = "")
        val password = credentials.substringAfter(':', missingDelimiterValue = "")
        val host = endpoint.substringBefore(':', missingDelimiterValue = "")
        val port = endpoint.substringAfter(':', missingDelimiterValue = "").toIntOrNull()

        if (method.isBlank() || method.lowercase(Locale.US) !in STRONG_SHADOWSOCKS_CIPHERS) {
            issues.addIfAbsent(
                ScanIssue(
                    id = "ss_insecure_cipher",
                    title = "Weak Shadowsocks cipher",
                    severity = IssueSeverity.HIGH,
                    description = "Cipher \"$method\" is not recommended; prefer modern AEAD ciphers such as chacha20-ietf-poly1305.",
                ),
            )
        }

        if (password.isBlank()) {
            issues.addIfAbsent(
                ScanIssue(
                    id = "ss_missing_password",
                    title = "Shadowsocks password missing",
                    severity = IssueSeverity.CRITICAL,
                    description = "A missing Shadowsocks password allows unauthenticated access to the proxy.",
                ),
            )
        }

        val params = decodeQuery(query)
        val plugin = params["plugin"]
        if (!plugin.isNullOrBlank() && plugin.contains("plaintext", ignoreCase = true)) {
            issues.addIfAbsent(
                ScanIssue(
                    id = "ss_insecure_plugin",
                    title = "Insecure Shadowsocks plugin",
                    severity = IssueSeverity.MEDIUM,
                    description = "Plugin \"$plugin\" does not provide additional transport security.",
                ),
            )
        }

        insecureTransports += "shadowsocks"

        return ParsedConfig(
            server = host.ifBlank { null },
            port = port,
            tlsEnabled = false,
            outbound = "shadowsocks",
            client = tag,
            insecureTransports = listOf("shadowsocks"),
        )
    }

    private fun extractPrefixedPayloads(raw: String): List<String> {
        val tokens = raw.split(Regex("\\s+"))
        val results = mutableListOf<String>()
        tokens.forEach { token ->
            val trimmed = token.trim()
            if (trimmed.isBlank()) return@forEach
            val prefix = V2RAY_LINK_PREFIXES.firstOrNull { trimmed.startsWith(it, ignoreCase = true) }
            if (prefix != null) {
                val payload = trimmed.removePrefix(prefix)
                when {
                    prefix.equals("vless://", ignoreCase = true) -> results += trimmed
                    prefix.equals("vmess://", ignoreCase = true) -> {
                        results += trimmed
                        decodeBase64(payload)?.let { results += it }
                    }
                    prefix.equals("trojan://", ignoreCase = true) -> results += trimmed
                    prefix.equals("ss://", ignoreCase = true) -> results += trimmed
                    else -> decodeBase64(payload)?.let { results += it }
                }
            }
        }
        return results
    }

    private fun decodeBase64(value: String): String? {
        val sanitized = value.trim().replace("\\s".toRegex(), "")
        if (sanitized.length < 8) return null
        return runCatching {
            val padding = (4 - sanitized.length % 4) % 4
            val padded = sanitized + "=".repeat(padding)
            String(Base64.getDecoder().decode(padded))
        }.getOrNull()
    }

    private fun looksLikeJson(candidate: String): Boolean {
        val trimmed = candidate.trimStart()
        return trimmed.startsWith("{") || trimmed.startsWith("[")
    }

    private fun decodeQuery(query: String?): Map<String, String> {
        if (query.isNullOrBlank()) return emptyMap()
        return query.split('&')
            .mapNotNull {
                val parts = it.split('=', limit = 2)
                if (parts.size == 2) {
                    val key = decodeComponent(parts[0])
                    val value = decodeComponent(parts[1])
                    key to value
                } else null
            }
            .toMap()
    }

    private fun decodeComponent(component: String?): String {
        if (component.isNullOrEmpty()) return ""
        return runCatching {
            URLDecoder.decode(component, StandardCharsets.UTF_8.name())
        }.getOrDefault(component)
    }

    private fun isPrivateEndpoint(host: String): Boolean {
        val asciiHost = runCatching { IDN.toASCII(host) }.getOrDefault(host)
        val inet = runCatching { InetAddress.getByName(asciiHost) }.getOrNull() ?: return false
        if (inet.isAnyLocalAddress || inet.isLoopbackAddress || inet.isSiteLocalAddress) return true
        if (inet is Inet6Address && (inet.isLinkLocalAddress || inet.isSiteLocalAddress)) return true
        return asciiHost.endsWith(".local", ignoreCase = true)
    }

    private fun JsonElement.asJsonArrayOrNull() = runCatching { jsonArray }.getOrNull()
    private fun JsonElement.jsonObjectOrNull() = this as? JsonObject
    private fun JsonElement.asString(): String? = (this as? JsonPrimitive)?.contentOrNull
    private fun JsonElement.asBoolean(): Boolean? = (this as? JsonPrimitive)?.booleanOrNull

    private data class ParsedConfig(
        val server: String? = null,
        val port: Int? = null,
        val tlsEnabled: Boolean? = null,
        val outbound: String? = null,
        val client: String? = null,
        val insecureTransports: List<String> = emptyList(),
    )

    private fun MutableList<ScanIssue>.addIfAbsent(issue: ScanIssue) {
        if (none { it.id == issue.id }) {
            add(issue)
        }
    }

    companion object {
        private val STRONG_SHADOWSOCKS_CIPHERS = setOf(
            "aes-128-gcm",
            "aes-192-gcm",
            "aes-256-gcm",
            "chacha20-ietf-poly1305",
            "xchacha20-ietf-poly1305",
        )
    }
}
