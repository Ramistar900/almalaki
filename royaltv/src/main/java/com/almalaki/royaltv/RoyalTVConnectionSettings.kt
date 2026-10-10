
package com.almalaki.royaltv

import android.content.Context

/**
 * إعدادات اتصال ROYAL TV المحفوظة محليًا.
 * لا تتحكم هذه الفئة بتصميم واجهة العرض.
 */
object RoyalTVConnectionSettings {

    private const val PREFS_NAME =
        "royal_tv_connection_settings"

    private const val KEY_AUTO_RECONNECT =
        "auto_reconnect"

    private const val KEY_DISCOVERY_ENABLED =
        "discovery_enabled"

    private const val KEY_PORT =
        "connection_port"

    private const val KEY_RETRY_SECONDS =
        "retry_seconds"

    private const val DEFAULT_PORT =
        RoyalTVLocalTransport.DEFAULT_PORT

    private const val DEFAULT_RETRY_SECONDS = 5L

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    /** إعادة الاتصال تلقائيًا مفعلة افتراضيًا. */
    fun isAutoReconnectEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_RECONNECT, true)

    fun setAutoReconnectEnabled(
        context: Context,
        enabled: Boolean
    ) {
        prefs(context).edit()
            .putBoolean(KEY_AUTO_RECONNECT, enabled)
            .apply()
    }

    /** الاكتشاف التلقائي مفعّل افتراضيًا. */
    fun isDiscoveryEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DISCOVERY_ENABLED, true)

    fun setDiscoveryEnabled(
        context: Context,
        enabled: Boolean
    ) {
        prefs(context).edit()
            .putBoolean(KEY_DISCOVERY_ENABLED, enabled)
            .apply()
    }

    fun getPort(context: Context): Int {
        val saved = prefs(context).getInt(KEY_PORT, DEFAULT_PORT)
        return if (saved in 1..65535) saved else DEFAULT_PORT
    }

    fun setPort(context: Context, port: Int): Boolean {
        if (port !in 1..65535) return false

        prefs(context).edit()
            .putInt(KEY_PORT, port)
            .apply()

        return true
    }

    fun getRetrySeconds(context: Context): Long {
        return prefs(context).getLong(
            KEY_RETRY_SECONDS,
            DEFAULT_RETRY_SECONDS
        ).coerceIn(2L, 60L)
    }

    fun setRetrySeconds(
        context: Context,
        seconds: Long
    ): Boolean {
        if (seconds !in 2L..60L) return false

        prefs(context).edit()
            .putLong(KEY_RETRY_SECONDS, seconds)
            .apply()

        return true
    }

    /** إعادة الإعدادات إلى القيم الافتراضية. */
    fun reset(context: Context) {
        prefs(context).edit()
            .remove(KEY_AUTO_RECONNECT)
            .remove(KEY_DISCOVERY_ENABLED)
            .remove(KEY_PORT)
            .remove(KEY_RETRY_SECONDS)
            .apply()
    }
}
