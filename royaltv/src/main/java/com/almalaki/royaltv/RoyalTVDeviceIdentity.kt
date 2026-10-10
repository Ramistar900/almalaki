
package com.almalaki.royaltv

import android.content.Context
import java.util.UUID

/**
 * الهوية الدائمة لجهاز ROYAL TV.
 *
 * تحفظ الهوية محليًا وتبقى بعد إغلاق التطبيق
 * وإعادة تشغيل الجهاز، ما دامت بيانات التطبيق محفوظة.
 */
object RoyalTVDeviceIdentity {

    private const val PREFS_NAME = "royal_tv_device_identity"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_DEVICE_NAME = "device_name"

    /**
     * إنشاء المعرّف عند أول استخدام فقط.
     */
    fun getDeviceId(context: Context): String {
        val prefs = context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val savedId = prefs.getString(KEY_DEVICE_ID, null)

        if (!savedId.isNullOrBlank()) {
            return savedId
        }

        val newId = "ROYAL-TV-" +
            UUID.randomUUID()
                .toString()
                .replace("-", "")
                .take(12)
                .uppercase()

        prefs.edit()
            .putString(KEY_DEVICE_ID, newId)
            .apply()

        return newId
    }

    /**
     * استرجاع اسم الجهاز أو استخدام المعرّف اسمًا افتراضيًا.
     */
    fun getDeviceName(context: Context): String {
        val prefs = context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val savedName = prefs.getString(KEY_DEVICE_NAME, null)

        return if (!savedName.isNullOrBlank()) {
            savedName
        } else {
            getDeviceId(context)
        }
    }

    /**
     * تغيير اسم الجهاز دون تغيير معرّفه.
     */
    fun setDeviceName(context: Context, name: String) {
        val cleanName = name.trim()

        if (cleanName.isBlank()) return

        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(KEY_DEVICE_NAME, cleanName)
            .apply()
    }

    /**
     * إعادة ضبط الهوية عند طلب المستخدم صراحةً.
     */
    fun resetIdentity(context: Context) {
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .clear()
            .apply()
    }
}
