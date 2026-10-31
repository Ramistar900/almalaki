package com.almalaki.cafe

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email

/**
 * Public client configuration only. Never place a Supabase service-role key in the Android app.
 */
object RoyalSupabaseAuth {
    private const val PROJECT_URL = "https://duvxxskgdmgrtaleedqu.supabase.co"
    private const val PUBLISHABLE_KEY = "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

    private val client by lazy {
        createSupabaseClient(
            supabaseUrl = PROJECT_URL,
            supabaseKey = PUBLISHABLE_KEY
        ) {
            install(Auth)
        }
    }

    suspend fun register(email: String, password: String) {
        require(email.isNotBlank()) { "أدخل البريد الإلكتروني." }
        require(password.length >= 8) { "يجب أن تتكون كلمة المرور من 8 أحرف على الأقل." }

        client.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    suspend fun signIn(email: String, password: String) {
        require(email.isNotBlank()) { "أدخل البريد الإلكتروني." }
        require(password.isNotBlank()) { "أدخل كلمة المرور." }

        client.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    suspend fun sendPasswordReset(email: String) {
        require(email.isNotBlank()) { "أدخل البريد الإلكتروني أولًا." }
        client.auth.resetPasswordForEmail(email.trim())
    }

    suspend fun signOut() {
        client.auth.signOut()
    }

    fun currentEmail(): String? = client.auth.currentUserOrNull()?.email
}
