package com.almalaki.cafe

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient

/**
 * Customer email authentication uses the project's public publishable key only.
 * Authorization of restaurant data must still be enforced by database RLS.
 */
object RoyalCustomerAuth {
    private const val SUPABASE_URL = "https://duvxxskgdmgrtaleedqu.supabase.co"
    private const val SUPABASE_PUBLISHABLE_KEY =
        "sb_publishable_L-BfALjb0TkdTlyLTWbFBIg_aez3fjQd"

    private val client by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Auth)
        }
    }

    suspend fun registerWithEmail(email: String, password: String): Result<Unit> =
        runCatching {
            require(email.trim().contains("@")) { "أدخل بريدًا إلكترونيًا صحيحًا." }
            require(password.length >= 8) { "يجب أن تتكون كلمة المرور من 8 أحرف على الأقل." }
            client.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
            }
        }.map { Unit }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> =
        runCatching {
            require(email.trim().contains("@")) { "أدخل بريدًا إلكترونيًا صحيحًا." }
            require(password.isNotBlank()) { "أدخل كلمة المرور." }
            client.auth.loginWith(Email) {
                this.email = email.trim()
                this.password = password
            }
        }.map { Unit }

    suspend fun sendPasswordReset(email: String): Result<Unit> =
        runCatching {
            require(email.trim().contains("@")) { "أدخل بريدًا إلكترونيًا صحيحًا." }
            client.auth.resetPasswordForEmail(email.trim())
        }.map { Unit }

    fun currentUserEmail(): String? =
        client.auth.currentUserOrNull()?.email

    suspend fun signOut() {
        client.auth.signOut()
    }
}
