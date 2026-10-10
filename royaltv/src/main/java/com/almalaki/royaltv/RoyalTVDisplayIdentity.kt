
package com.almalaki.royaltv

import android.content.Context

/**
 * الهوية الظاهرة لشاشة ROYAL TV.
 *
 * تحفظ رقم الشاشة ورمزها محليًا.
 * التخصيص هنا محلي، ولا يضمن وحده عدم تكرار
 * الرمز بين أجهزة مختلفة؛ ذلك يحتاج إلى سجل مركزي.
 */
object RoyalTVDisplayIdentity {

    private const val PREFS_NAME = "royal_tv_display_identity"
    private const val KEY_DISPLAY_CODE = "display_code"
    private const val KEY_DISPLAY_NUMBER = "display_number"

    /**
     * الحصول على رقم الشاشة المحلي.
     */
    fun getDisplayNumber(context: Context): Int {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val savedNumber = prefs.getInt(KEY_DISPLAY_NUMBER, 0)

        if (savedNumber > 0) {
            return savedNumber
        }

        val deviceId = RoyalTVDeviceIdentity.getDeviceId(appContext)
        val numericPart = deviceId.filter { it.isDigit() }

        val generatedNumber = numericPart
            .takeLast(6)
            .toIntOrNull()
            ?: 1

        val safeNumber = generatedNumber.coerceAtLeast(1)

        prefs.edit()
            .putInt(KEY_DISPLAY_NUMBER, safeNumber)
            .apply()

        return safeNumber
    }

    /**
     * الحصول على الرمز المحفوظ للشاشة.
     * مثال: ROYAL-TV-001
     */
    fun getDisplayCode(context: Context): String {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val savedCode = prefs.getString(KEY_DISPLAY_CODE, null)

        if (!savedCode.isNullOrBlank()) {
            return savedCode
        }

        val number = getDisplayNumber(appContext)
        val code = "ROYAL-TV-" + number.toString().padStart(3, '0')

        prefs.edit()
            .putString(KEY_DISPLAY_CODE, code)
            .apply()

        return code
    }

    /**
     * التحقق من وجود هوية محفوظة.
     */
    fun hasDisplayIdentity(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        return prefs.contains(KEY_DISPLAY_CODE)
    }

    /**
     * إعادة ضبط هوية الشاشة عند طلب إعادة التسجيل.
     */
    fun reset(context: Context) {
        context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .clear()
            .apply()
    }
}
