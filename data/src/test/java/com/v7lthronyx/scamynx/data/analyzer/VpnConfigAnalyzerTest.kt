package com.v7lthronyx.scamynx.data.analyzer

import com.v7lthronyx.scamynx.domain.model.VerdictStatus
import java.util.Base64
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VpnConfigAnalyzerTest {

    private lateinit var analyzer: VpnConfigAnalyzer
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        analyzer = VpnConfigAnalyzer(json)
    }

    @Test
    fun jsonConfigWithAllowInsecureProducesHighRisk() {
        val raw = """
            {
              "app": "v2ray",
              "outbounds": [{
                "protocol": "vmess",
                "settings": {
                  "vnext": [{
                    "address": "malicious.example.com",
                    "port": "80"
                  }]
                },
                "streamSettings": {
                  "security": "none",
                  "allowInsecure": true,
                  "network": "ws"
                }
              }]
            }
        """.trimIndent()

        val result = analyzer.analyze(raw)

        assertEquals("malicious.example.com", result.report.serverAddress)
        assertTrue(result.report.issues.any { it.id == "allow_insecure_tls" })
        assertTrue("Risk score should reflect high severity", result.report.riskScore >= 0.6)
        assertEquals(VerdictStatus.MALICIOUS, result.verdict.status)
    }

    @Test
    fun vmessLinkWithTlsRemainsLowRisk() {
        val vmessJson = """
            {
              "add": "secure.example.com",
              "port": "443",
              "tls": "tls",
              "net": "ws",
              "ps": "demo"
            }
        """.trimIndent()
        val encoded = Base64.getEncoder().encodeToString(vmessJson.toByteArray())
        val link = "vmess://$encoded"

        val result = analyzer.analyze(link)

        assertEquals("secure.example.com", result.report.serverAddress)
        assertTrue("Should remain low risk when TLS is enabled", result.report.riskScore < 0.3)
        assertEquals(VerdictStatus.CLEAN, result.verdict.status)
    }

    @Test
    fun trojanWithoutTlsIsFlagged() {
        val link = "trojan://secret@vpn.example.net:443?security=none"

        val result = analyzer.analyze(link)

        assertTrue(result.report.issues.any { it.id == "trojan_without_tls" })
        assertEquals(VerdictStatus.MALICIOUS, result.verdict.status)
    }

    @Test
    fun shadowsocksWithWeakCipherAndPrivateHostElevatesRisk() {
        val config = Base64.getEncoder().encodeToString("aes-128-cfb:password@10.0.0.5:8388".toByteArray())
        val link = "ss://$config"

        val result = analyzer.analyze(link)

        assertTrue(result.report.issues.any { it.id == "ss_insecure_cipher" })
        assertTrue(result.report.issues.any { it.id == "private_endpoint" })
        assertTrue("Risk score should be high for insecure SS config", result.report.riskScore >= 0.7)
        assertEquals(VerdictStatus.MALICIOUS, result.verdict.status)
    }
}
