package com.v7lthronyx.scamynx.data.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class ApiKeyInterceptor(
    private val headerName: String,
    private val valueProvider: () -> String?,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val key = valueProvider()
        val builder = chain.request().newBuilder()
        if (!key.isNullOrBlank()) {
            builder.header(headerName, key)
        }
        return chain.proceed(builder.build())
    }
}
