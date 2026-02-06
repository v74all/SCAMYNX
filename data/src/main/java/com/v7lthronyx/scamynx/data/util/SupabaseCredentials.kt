package com.v7lthronyx.scamynx.data.util

data class SupabaseCredentials(
    val url: String,
    val anonKey: String,
    val functionJwt: String?,
) {
    val isConfigured: Boolean
        get() = url.isNotBlank() && anonKey.isNotBlank()
}
