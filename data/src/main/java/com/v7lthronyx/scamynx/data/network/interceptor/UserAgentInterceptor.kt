package com.v7lthronyx.scamynx.data.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor(
    private val userAgent: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val updatedRequest = chain.request().newBuilder()
            .header("User-Agent", userAgent)
            .build()
        return chain.proceed(updatedRequest)
    }
}
