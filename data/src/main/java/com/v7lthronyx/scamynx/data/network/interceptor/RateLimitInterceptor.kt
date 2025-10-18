package com.v7lthronyx.scamynx.data.network.interceptor

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.pow
import okhttp3.Interceptor
import okhttp3.Response

class RateLimitInterceptor(
    private val minimumIntervalMillis: Long,
    private val maxRetries: Int = 3,
) : Interceptor {
    private val lastRequestAt = ConcurrentHashMap<String, Long>()
    private val requestCounts = ConcurrentHashMap<String, Int>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host
        
        // Apply rate limiting
        enforceRateLimit(host)
        
        var response: Response? = null
        var attempt = 0
        
        while (attempt <= maxRetries) {
            try {
                response = chain.proceed(request)
                
                // If successful or client error (4xx), don't retry
                if (response.isSuccessful || response.code in 400..499) {
                    return response
                }
                
                // Handle rate limiting (429) or server errors (5xx)
                if (response.code == 429 || response.code >= 500) {
                    response.close()
                    
                    if (attempt < maxRetries) {
                        val backoffDelay = calculateBackoffDelay(attempt, response.code)
                        Thread.sleep(backoffDelay)
                        attempt++
                        continue
                    }
                }
                
                return response
            } catch (e: Exception) {
                response?.close()
                if (attempt >= maxRetries) {
                    throw e
                }
                Thread.sleep(calculateBackoffDelay(attempt, null))
                attempt++
            }
        }
        
        return response ?: throw IllegalStateException("No response after $maxRetries retries")
    }
    
    private fun enforceRateLimit(host: String) {
        val now = System.nanoTime()
        val last = lastRequestAt[host]
        if (last != null) {
            val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(now - last)
            val waitMillis = max(0, minimumIntervalMillis - elapsedMillis)
            if (waitMillis > 0) {
                Thread.sleep(waitMillis)
            }
        }
        lastRequestAt[host] = System.nanoTime()
    }
    
    private fun calculateBackoffDelay(attempt: Int, responseCode: Int?): Long {
        val baseDelay = when (responseCode) {
            429 -> 5000L // 5 seconds for rate limiting
            in 500..599 -> 2000L // 2 seconds for server errors
            else -> 1000L // 1 second for other errors
        }
        // Exponential backoff with jitter
        val exponentialDelay = baseDelay * (2.0.pow(attempt.toDouble())).toLong()
        val jitter = (0..500).random()
        return exponentialDelay + jitter
    }
}
