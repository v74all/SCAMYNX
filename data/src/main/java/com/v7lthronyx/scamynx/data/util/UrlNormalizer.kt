package com.v7lthronyx.scamynx.data.util

import java.net.IDN
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UrlNormalizer @Inject constructor() {

    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed

        return runCatching {
            val candidate = if (hasScheme(trimmed)) trimmed else "https://$trimmed"
            val uri = URI(candidate)
            val scheme = (uri.scheme ?: "https").lowercase()
            val host = uri.host?.takeIf { it.isNotBlank() }?.let { IDN.toASCII(it).lowercase() }
                ?: throw URISyntaxException(candidate, "Missing host in URL")
            val port = if (uri.port != -1) ":${uri.port}" else ""
            val userInfo = uri.userInfo?.takeIf { it.isNotBlank() }?.let { "$it@" } ?: ""
            val path = when {
                uri.rawPath.isNullOrBlank() -> "/"
                uri.rawPath.startsWith("/") -> uri.rawPath
                else -> "/${uri.rawPath}"
            }
            val query = uri.rawQuery?.let { "?$it" } ?: ""
            val fragment = uri.rawFragment?.let { "#$it" } ?: ""
            buildString {
                append(scheme)
                append("://")
                append(userInfo)
                append(host)
                append(port)
                append(path)
                append(query)
                append(fragment)
            }
        }.getOrElse { trimmed }
    }

    private fun hasScheme(value: String): Boolean {
        return SCHEME_REGEX.containsMatchIn(value)
    }

    private companion object {
        private val SCHEME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
    }
}
