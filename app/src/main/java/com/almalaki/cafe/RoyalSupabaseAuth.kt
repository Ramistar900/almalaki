package com.almalaki.cafe

import android.content.Intent
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.OAuthProvider
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.providers.builtin.Phone
import io.github.jan.supabase.auth.handleDeeplinks

/**
 * Public client configuration only. Never place a Supabase service-role key in the Android app.
 */
object RoyalSupabaseAuth {
    const val PROJECT_URL = "https://duvxxskgdmgrtaleedqu.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

    private val client by lazy {
        createSupabaseClient(
            supabaseUrl = PROJECT_URL,
            supabaseKey = PUBLISHABLE_KEY
        ) {
            install(Auth) {
                scheme = "com.almalaki"
                host = "auth-callback"
                flowType = FlowType.PKCE
            }
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

    suspend fun signInWithFacebook() {
        client.auth.signInWith(OAuthProvider("facebook"))
    }

    suspend fun sendWhatsAppOtp(phoneNumber: String) {
        require(phoneNumber.startsWith("+") && phoneNumber.length in 8..16) {
            "أدخل رقم الهاتف بالصيغة الدولية، مثل +905XXXXXXXXX."
        }
        client.auth.signInWith(OTP) {
            phone = phoneNumber.trim()
            channel = Phone.Channel.WHATSAPP
        }
    }

    suspend fun verifyWhatsAppOtp(phoneNumber: String, code: String) {
        require(code.length == 6 && code.all(Char::isDigit)) {
            "أدخل رمز التحقق المكوّن من 6 أرقام."
        }
        client.auth.verifyPhoneOtp(
            type = OtpType.Phone.SMS,
            phone = phoneNumber.trim(),
            token = code
        )
    }

    suspend fun sendPasswordReset(email: String) {
        require(email.isNotBlank()) { "أدخل البريد الإلكتروني أولًا." }
        client.auth.resetPasswordForEmail(email.trim())
    }

    suspend fun signOut() {
        client.auth.signOut()
    }

    fun handleDeepLink(intent: Intent) {
        client.handleDeeplinks(intent)
    }

    fun sessionStatusEvents() = client.auth.sessionStatus

    fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    fun currentEmail(): String? = client.auth.currentUserOrNull()?.email

    fun accessTokenOrNull(): String? = client.auth.currentSessionOrNull()?.accessToken
}
