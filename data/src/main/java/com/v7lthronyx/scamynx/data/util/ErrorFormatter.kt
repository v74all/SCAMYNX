package com.v7lthronyx.scamynx.data.util

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorFormatter @Inject constructor() {

    fun formatError(throwable: Throwable): String {
        return when (throwable) {
            is SocketTimeoutException -> "Connection timeout. Please check your internet connection and try again."
            is ConnectException -> "Unable to connect. Please check your internet connection."
            is UnknownHostException -> "Unable to resolve host. Please check the URL or your internet connection."
            is IOException -> "Network error: ${throwable.message ?: "Unable to complete the request"}"
            is IllegalArgumentException -> "Invalid input: ${throwable.message ?: "Please check your input and try again"}"
            is IllegalStateException -> "Operation failed: ${throwable.message ?: "Please try again"}"
            is SecurityException -> "Permission denied: ${throwable.message ?: "Please check app permissions"}"
            else -> {
                val message = throwable.message
                when {
                    message?.contains("rate limit", ignoreCase = true) == true -> "Rate limit exceeded. Please wait a moment and try again."
                    message?.contains("api key", ignoreCase = true) == true -> "API key error. Please check your API key configuration."
                    message?.contains("timeout", ignoreCase = true) == true -> "Request timed out. Please try again."
                    message?.contains("network", ignoreCase = true) == true -> "Network error. Please check your connection."
                    message?.contains("unauthorized", ignoreCase = true) == true -> "Authentication failed. Please check your API keys."
                    message?.contains("forbidden", ignoreCase = true) == true -> "Access denied. Please check your API permissions."
                    message?.contains("not found", ignoreCase = true) == true -> "Resource not found. Please verify the input."
                    message != null -> message
                    else -> "An unexpected error occurred. Please try again."
                }
            }
        }
    }

    fun getErrorSummary(throwable: Throwable): String {
        return when (throwable) {
            is SocketTimeoutException -> "Connection timeout"
            is ConnectException -> "Connection failed"
            is UnknownHostException -> "Host not found"
            is IOException -> "Network error"
            is IllegalArgumentException -> "Invalid input"
            is IllegalStateException -> "Operation failed"
            is SecurityException -> "Permission denied"
            else -> "Error occurred"
        }
    }
}
