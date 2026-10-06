package com.almalaki.cafe.royaltv

import android.content.Context
import android.content.SharedPreferences

object RoyalTVResumeManager {

    private const val PREFS_NAME =
        "royal_tv_resume"

    private const val KEY_SOURCE =
        "source"

    private const val KEY_COMMAND_TYPE =
        "command_type"

    private const val KEY_PAYLOAD =
        "payload"

    private const val KEY_IS_PLAYING =
        "is_playing"

    private const val KEY_IS_FULLSCREEN =
        "is_fullscreen"

    private const val KEY_POSITION =
        "position"

    private const val KEY_SAVED_AT =
        "saved_at"

    private var preferences:
        SharedPreferences? = null

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
     * حفظ الحالة الحالية حتى نستطيع العودة إليها
     * بعد انتهاء المحتوى المؤقت مثل الطلب الجاهز.
     */
    fun saveCurrentState(
        context: Context,
        state: RoyalTVState,
        position: Long = 0L
    ) {

        initialize(context)

        preferences
            ?.edit()
            ?.putString(
                KEY_SOURCE,
                state.source.name
            )
            ?.putString(
                KEY_COMMAND_TYPE,
                state.commandType?.name
            )
            ?.putString(
                KEY_PAYLOAD,
                state.payload
            )
            ?.putBoolean(
                KEY_IS_PLAYING,
                state.isPlaying
            )
            ?.putBoolean(
                KEY_IS_FULLSCREEN,
                state.isFullscreen
            )
            ?.putLong(
                KEY_POSITION,
                position.coerceAtLeast(0L)
            )
            ?.putLong(
                KEY_SAVED_AT,
                System.currentTimeMillis()
            )
            ?.apply()
    }

    /**
     * استعادة آخر حالة محفوظة.
     */
    fun restoreState(
        context: Context
    ): RoyalTVState? {

        initialize(context)

        val prefs =
            preferences
                ?: return null

        if (
            !prefs.contains(
                KEY_SOURCE
            )
        ) {
            return null
        }

        val source =
            try {

                RoyalTVSource.valueOf(
                    prefs.getString(
                        KEY_SOURCE,
                        RoyalTVSource.IDLE.name
                    ) ?: RoyalTVSource.IDLE.name
                )

            } catch (_: Exception) {

                RoyalTVSource.IDLE
            }

        val commandType =
            try {

                prefs.getString(
                    KEY_COMMAND_TYPE,
                    null
                )?.let {
                    RoyalTVCommandType.valueOf(
                        it
                    )
                }

            } catch (_: Exception) {

                null
            }

        val payload =
            prefs.getString(
                KEY_PAYLOAD,
                ""
            ) ?: ""

        val isPlaying =
            prefs.getBoolean(
                KEY_IS_PLAYING,
                false
            )

        val isFullscreen =
            prefs.getBoolean(
                KEY_IS_FULLSCREEN,
                true
            )

        return RoyalTVState(
            source = source,
            commandType = commandType,
            payload = payload,
            isPlaying = isPlaying,
            isFullscreen = isFullscreen
        )
    }

    /**
     * الحصول على موضع التشغيل السابق.
     */
    fun getSavedPosition(
        context: Context
    ): Long {

        initialize(context)

        return preferences
            ?.getLong(
                KEY_POSITION,
                0L
            )
            ?.coerceAtLeast(0L)
            ?: 0L
    }

    /**
     * وقت آخر حفظ للحالة.
     */
    fun getSavedAt(
        context: Context
    ): Long {

        initialize(context)

        return preferences
            ?.getLong(
                KEY_SAVED_AT,
                0L
            )
            ?: 0L
    }

    /**
     * هل توجد حالة يمكن استعادتها؟
     */
    fun hasSavedState(
        context: Context
    ): Boolean {

        initialize(context)

        return preferences
            ?.contains(
                KEY_SOURCE
            )
            ?: false
    }

    /**
     * حذف الحالة المحفوظة.
     */
    fun clear(
        context: Context
    ) {

        initialize(context)

        preferences
            ?.edit()
            ?.clear()
            ?.apply()
    }

    /**
     * حفظ الحالة الحالية ثم تنفيذ محتوى مؤقت.
     *
     * تستخدم لاحقًا مع الطلبات الجاهزة
     * والمحتوى ذي الأولوية العالية.
     */
    fun saveBeforeInterrupt(
        context: Context,
        currentState: RoyalTVState,
        currentPosition: Long = 0L
    ) {

        saveCurrentState(
            context = context,
            state = currentState,
            position = currentPosition
        )
    }

    /**
     * استعادة الحالة السابقة بعد انتهاء المحتوى المؤقت.
     */
    fun resumePreviousState(
        context: Context
    ): RoyalTVState? {

        return restoreState(
            context
        )
    }
}
