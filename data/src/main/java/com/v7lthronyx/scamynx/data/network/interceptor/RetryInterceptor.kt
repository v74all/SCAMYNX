package com.v7lthronyx.scamynx.data.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import kotlin.math.pow

/**
 * Dedicated retry interceptor for API calls that need more sophisticated retry logic
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 1000L,
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var exception: Exception? = null
        
        repeat(maxRetries + 1) { attempt ->
            try {
                response?.close() // Close previous response if exists
                response = chain.proceed(request)
                
                // Success or client error - don't retry
                if (response!!.isSuccessful || response!!.code in 400..499) {
                    return response!!
                }
                
                // Server error or rate limit - retry with backoff
                if (attempt < maxRetries && (response!!.code >= 500 || response!!.code == 429)) {
                    val delay = initialDelayMs * (2.0.pow(attempt.toDouble())).toLong()
                    Thread.sleep(delay)
                } else {
                    return response!!
                }
                
            } catch (e: Exception) {
                exception = e
                if (attempt < maxRetries) {
                    val delay = initialDelayMs * (2.0.pow(attempt.toDouble())).toLong()
                    Thread.sleep(delay)
                } else {
                    throw e
                }
            }
        }
        
        return response ?: throw (exception ?: IllegalStateException("Unexpected retry state"))
    }
}