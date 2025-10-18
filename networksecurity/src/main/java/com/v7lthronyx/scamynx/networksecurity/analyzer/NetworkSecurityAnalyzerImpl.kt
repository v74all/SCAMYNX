package com.v7lthronyx.scamynx.networksecurity.analyzer

import com.v7lthronyx.scamynx.domain.model.NetworkReport
import com.v7lthronyx.scamynx.domain.service.NetworkSecurityAnalyzer
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.v7lthronyx.scamynx.networksecurity.di.NetworkSecurityClient

@Singleton
class NetworkSecurityAnalyzerImpl @Inject constructor(
    @NetworkSecurityClient private val client: OkHttpClient,
) : NetworkSecurityAnalyzer {

    private val interestingHeaders = listOf(
        "Strict-Transport-Security",
        "Content-Security-Policy",
        "X-Frame-Options",
        "X-Content-Type-Options",
        "Referrer-Policy",
    )

    override suspend fun inspect(url: String): NetworkReport = withContext(Dispatchers.IO) {
        val headRequest = Request.Builder().url(url).method("HEAD", null).build()
        val response = runCatching { client.newCall(headRequest).execute() }.getOrNull()
            ?: runCatching { client.newCall(Request.Builder().url(url).get().build()).execute() }.getOrNull()
            ?: return@withContext NetworkReport(headers = emptyMap())

        response.use { resp ->
            val handshake = resp.handshake
            val tlsVersion = handshake?.tlsVersion?.javaName
            val cipherSuite = handshake?.cipherSuite?.javaName
            val headers = mutableMapOf<String, String>()
            interestingHeaders.forEach { key ->
                resp.header(key)?.let { headers[key] = it }
            }
            val certValid = evaluateCertificate(handshake?.peerCertificates?.firstOrNull())
            return@withContext NetworkReport(
                tlsVersion = tlsVersion,
                cipherSuite = cipherSuite,
                certValid = certValid,
                headers = headers,
                dnssecSignal = null,
            )
        }
    }

    private fun evaluateCertificate(certificate: java.security.cert.Certificate?): Boolean? {
        val x509 = certificate as? X509Certificate ?: return null
        return try {
            x509.checkValidity()
            true
        } catch (_: CertificateExpiredException) {
            false
        } catch (_: CertificateNotYetValidException) {
            false
        } catch (_: Exception) {
            null
        }
    }
}
