package com.almalaki.cafe.royaltv

import android.content.Context
import android.content.SharedPreferences
import java.security.SecureRandom

object RoyalTVPairingManager {

    private const val PREFS_NAME =
        "royal_tv_pairing"

    private const val KEY_PAIRING_CODE =
        "pairing_code"

    private const val KEY_CODE_CREATED_AT =
        "pairing_code_created_at"

    private const val KEY_PAIRED_DEVICES =
        "paired_devices"

    private const val PAIRING_CODE_LENGTH =
        6

    private const val PAIRING_CODE_VALIDITY_MS =
        5 * 60 * 1000L

    private var preferences: SharedPreferences? = null

    fun initialize(
        context: Context
    ) {
        if (preferences != null) {
            return
        }

        preferences =
            context.applicationContext
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
    }

    /**
     * إنشاء رمز اقتران جديد.
     *
     * الرمز صالح لمدة 5 دقائق.
     */
    fun generatePairingCode(
        context: Context
    ): String {

        initialize(context)

        val random =
            SecureRandom()

        val minimum =
            100000

        val maximum =
            999999

        val code =
            (minimum +
                random.nextInt(
                    maximum - minimum + 1
                ))
                .toString()
                .take(
                    PAIRING_CODE_LENGTH
                )

        preferences
            ?.edit()
            ?.putString(
                KEY_PAIRING_CODE,
                code
            )
            ?.putLong(
                KEY_CODE_CREATED_AT,
                System.currentTimeMillis()
            )
            ?.apply()

        return code
    }

    /**
     * الحصول على رمز الاقتران الحالي.
     *
     * إذا انتهت صلاحيته يرجع null.
     */
    fun getCurrentPairingCode(
        context: Context
    ): String? {

        initialize(context)

        val prefs =
            preferences
                ?: return null

        val code =
            prefs.getString(
                KEY_PAIRING_CODE,
                null
            )
                ?: return null

        val createdAt =
            prefs.getLong(
                KEY_CODE_CREATED_AT,
                0L
            )

        if (createdAt <= 0L) {
            return null
        }

        val expired =
            System.currentTimeMillis() -
                createdAt >
                PAIRING_CODE_VALIDITY_MS

        if (expired) {
            clearPairingCode(
                context
            )

            return null
        }

        return code
    }

    /**
     * التحقق من رمز الاقتران.
     */
    fun verifyPairingCode(
        context: Context,
        code: String
    ): Boolean {

        val currentCode =
            getCurrentPairingCode(
                context
            )
                ?: return false

        return currentCode ==
            code.trim()
    }

    /**
     * حذف رمز الاقتران الحالي.
     */
    fun clearPairingCode(
        context: Context
    ) {

        initialize(context)

        preferences
            ?.edit()
            ?.remove(
                KEY_PAIRING_CODE
            )
            ?.remove(
                KEY_CODE_CREATED_AT
            )
            ?.apply()
    }

    /**
     * إضافة جهاز إلى الأجهزة الموثوقة.
     */
    fun pairDevice(
        context: Context,
        deviceId: String
    ): Boolean {

        initialize(context)

        val cleanDeviceId =
            deviceId.trim()

        if (cleanDeviceId.isBlank()) {
            return false
        }

        val devices =
            getPairedDevices(
                context
            ).toMutableSet()

        devices.add(
            cleanDeviceId
        )

        savePairedDevices(
            devices
        )

        return true
    }

    /**
     * التحقق هل الجهاز موثوق.
     */
    fun isDevicePaired(
        context: Context,
        deviceId: String
    ): Boolean {

        val cleanDeviceId =
            deviceId.trim()

        if (cleanDeviceId.isBlank()) {
            return false
        }

        return getPairedDevices(
            context
        ).contains(
            cleanDeviceId
        )
    }

    /**
     * إلغاء اقتران جهاز محدد.
     */
    fun unpairDevice(
        context: Context,
        deviceId: String
    ) {

        initialize(context)

        val cleanDeviceId =
            deviceId.trim()

        if (cleanDeviceId.isBlank()) {
            return
        }

        val devices =
            getPairedDevices(
                context
            ).toMutableSet()

        devices.remove(
            cleanDeviceId
        )

        savePairedDevices(
            devices
        )
    }

    /**
     * حذف جميع الأجهزة الموثوقة.
     */
    fun unpairAllDevices(
        context: Context
    ) {

        initialize(context)

        preferences
            ?.edit()
            ?.remove(
                KEY_PAIRED_DEVICES
            )
            ?.apply()
    }

    /**
     * الحصول على جميع الأجهزة الموثوقة.
     */
    fun getPairedDevices(
        context: Context
    ): List<String> {

        initialize(context)

        val raw =
            preferences
                ?.getString(
                    KEY_PAIRED_DEVICES,
                    ""
                )
                ?: ""

        if (raw.isBlank()) {
            return emptyList()
        }

        return raw
            .split("|")
            .map {
                it.trim()
            }
            .filter {
                it.isNotBlank()
            }
            .distinct()
    }

    /**
     * عدد الأجهزة الموثوقة.
     */
    fun getPairedDeviceCount(
        context: Context
    ): Int {

        return getPairedDevices(
            context
        ).size
    }

     /**
     * هل يوجد رمز اقتران صالح حاليًا؟
     */
    fun hasValidPairingCode(
        context: Context
    ): Boolean {

        return getCurrentPairingCode(
            context
        ) != null
    }

    /**
     * الوقت المتبقي لصلاحية الرمز بالميلي ثانية.
     */
    fun getRemainingCodeTime(
        context: Context
    ): Long {

        initialize(context)

        val prefs =
            preferences
                ?: return 0L

        val createdAt =
            prefs.getLong(
                KEY_CODE_CREATED_AT,
                0L
            )

        if (createdAt <= 0L) {
            return 0L
        }

        val elapsed =
            System.currentTimeMillis() -
                createdAt

        val remaining =
            PAIRING_CODE_VALIDITY_MS -
                elapsed

        return remaining.coerceAtLeast(
            0L
        )
    }

    private fun savePairedDevices(
        devices: Set<String>
    ) {

        preferences
            ?.edit()
            ?.putString(
                KEY_PAIRED_DEVICES,
                devices.joinToString("|")
            )
            ?.apply()
    }
}
