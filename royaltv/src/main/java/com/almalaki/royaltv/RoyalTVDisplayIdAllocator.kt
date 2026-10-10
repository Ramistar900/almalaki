
package com.almalaki.royaltv

import android.content.Context
import android.content.SharedPreferences

/**
 * مولّد معرّف شاشة ROYAL TV.
 *
 * يحفظ معرّف الشاشة محليًا على الجهاز.
 * الترقيم هنا محلي، وليس تسجيلًا مركزيًا
 * يضمن التفرد بين أجهزة التلفزيون المختلفة.
 */
object RoyalTVDisplayIdAllocator {

    private const val PREFS_NAME =
        "royal_tv_display_id_allocator"

    private const val KEY_DISPLAY_ID =
        "display_id"

    private const val KEY_NEXT_NUMBER =
        "next_number"

    private const val FIRST_NUMBER = 1

    private const val DISPLAY_PREFIX = "ROYAL-TV-"

    @Volatile
    private var preferences: SharedPreferences? = null

    /**
     * تهيئة التخزين المحلي.
     */
    fun initialize(context: Context) {
        if (preferences != null) return

        synchronized(this) {
            if (preferences == null) {
                preferences = context.applicationContext
                    .getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                    )
            }
        }
    }

    /**
     * الحصول على معرّف الشاشة.
     * يُنشأ مرة واحدة ثم يُحفظ محليًا.
     */
    fun getDisplayId(context: Context): String {
        initialize(context)

        val prefs = preferences
            ?: return createDisplayId(FIRST_NUMBER)

        synchronized(this) {
            val savedId = prefs.getString(
                KEY_DISPLAY_ID,
                null
            )

            if (!savedId.isNullOrBlank()) {
                return savedId
            }

            val nextNumber = getNextNumber(prefs)
            val displayId = createDisplayId(nextNumber)

            prefs.edit()
                .putString(KEY_DISPLAY_ID, displayId)
                .putInt(
                    KEY_NEXT_NUMBER,
                    nextNumber + 1
                )
                .apply()

            return displayId
        }
    }

    /**
     * التحقق من وجود معرّف محفوظ.
     */
    fun hasDisplayId(context: Context): Boolean {
        initialize(context)

        return preferences?.contains(KEY_DISPLAY_ID)
            ?: false
    }

    /**
     * استخراج الرقم من معرّف الشاشة.
     */
    fun getDisplayNumber(context: Context): Int {
        val displayId = getDisplayId(context)

        return extractNumber(displayId) ?: FIRST_NUMBER
    }

    /**
     * تعيين معرّف مخصص، مثل ROYAL-TV-015.
     *
     * يعيد false إذا كانت صيغة المعرّف غير صحيحة.
     */
    fun setDisplayId(
        context: Context,
        displayId: String
    ): Boolean {
        initialize(context)

        val cleanId = displayId.trim().uppercase()

        if (!isValidDisplayId(cleanId)) {
            return false
        }

        val prefs = preferences ?: return false
        val number = extractNumber(cleanId) ?: return false

        synchronized(this) {
            val currentNext = prefs.getInt(
                KEY_NEXT_NUMBER,
                FIRST_NUMBER
            )

            val editor = prefs.edit()
                .putString(KEY_DISPLAY_ID, cleanId)

            if (number >= currentNext) {
                editor.putInt(
                    KEY_NEXT_NUMBER,
                    number + 1
                )
            }

            editor.apply()
        }

        return true
    }

    /**
     * حذف معرّف الشاشة المحفوظ.
     *
     * عند طلب معرّف جديد لاحقًا، سيُنشأ محليًا
     * وفق العداد المحفوظ أو يبدأ من 001 إذا حُذف العداد.
     */
    fun reset(context: Context) {
        initialize(context)

        synchronized(this) {
            preferences?.edit()
                ?.remove(KEY_DISPLAY_ID)
                ?.remove(KEY_NEXT_NUMBER)
                ?.apply()
        }
    }

    private fun getNextNumber(
        prefs: SharedPreferences
    ): Int {
        return prefs.getInt(
            KEY_NEXT_NUMBER,
            FIRST_NUMBER
        ).coerceAtLeast(FIRST_NUMBER)
    }

    private fun createDisplayId(number: Int): String {
        return DISPLAY_PREFIX +
            number.coerceAtLeast(FIRST_NUMBER)
                .toString()
                .padStart(3, '0')
    }

    private fun isValidDisplayId(
        displayId: String
    ): Boolean {
        val number = extractNumber(displayId)
        return number != null && number >= FIRST_NUMBER
    }

    private fun extractNumber(
        displayId: String
    ): Int? {
        if (!displayId.startsWith(DISPLAY_PREFIX)) {
            return null
        }

        val numberPart = displayId.removePrefix(
            DISPLAY_PREFIX
        )

        if (numberPart.isBlank() ||
            numberPart.any { !it.isDigit() }
        ) {
            return null
        }

        return numberPart.toIntOrNull()
    }
}
