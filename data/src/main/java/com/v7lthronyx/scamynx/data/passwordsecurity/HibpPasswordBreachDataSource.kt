package com.v7lthronyx.scamynx.data.passwordsecurity

import com.v7lthronyx.scamynx.data.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val PWNED_PASSWORDS_RANGE_URL = "https://api.pwnedpasswords.com/range/"

@Singleton
class HibpPasswordBreachDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : PasswordBreachDataSource {

    override suspend fun lookup(password: String): Int = withContext(dispatcher) {
        if (password.isEmpty()) return@withContext 0
        val sha1Hex = password.toSha1()
        val prefix = sha1Hex.substring(0, 5)
        val suffix = sha1Hex.substring(5)
        val request = Request.Builder()
            .url("$PWNED_PASSWORDS_RANGE_URL$prefix")
            .header("Add-Padding", "true")
            .header("Cache-Control", "no-cache")
            .build()
        runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use 0
                val body = response.body?.string().orEmpty()
                parseResponse(body, suffix)
            }
        }.getOrElse { throwable ->
            if (throwable is IOException) 0 else throw throwable
        }
    }

    private fun parseResponse(body: String, suffix: String): Int {
        val normalizedSuffix = suffix.uppercase(Locale.US)
        body.lineSequence().forEach { line ->
            val parts = line.split(':')
            if (parts.size == 2) {
                val hashSuffix = parts[0].trim()
                if (hashSuffix.equals(normalizedSuffix, ignoreCase = true)) {
                    return parts[1].trim().toIntOrNull() ?: 0
                }
            }
        }
        return 0
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
