package com.v7lthronyx.scamynx.data.passwordsecurity

interface PasswordBreachDataSource {
    suspend fun lookup(password: String): Int
}
