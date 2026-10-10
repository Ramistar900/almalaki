
package com.almalaki.cafe.royaltv

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * مدير الحالة المركزي لنظام ROYAL TV.
 *
 * مسؤول عن:
 * - إدارة الحالة الحالية.
 * - تنفيذ أوامر ROYAL TV.
 * - حفظ الحالة محليًا.
 * - استعادة آخر حالة عند التهيئة.
 * - إبقاء تحديثات شريط الأخبار مستقلة عن المحتوى الجاري.
 *
 * لا يؤدي الخروج من شاشة التحكم إلى إيقاف العرض.
 */
object RoyalTVManager {

    private val _state =
        MutableStateFlow(RoyalTVState())

    /**
     * الحالة الحالية لـ ROYAL TV.
     */
    val state: StateFlow<RoyalTVState> =
        _state.asStateFlow()

    /**
     * سياق التطبيق فقط، وليس سياق Activity.
     */
    private var appContext: Context? = null

    /**
     * تهيئة المدير ومحركات التخزين المحلية.
     *
     * تستعيد آخر حالة محفوظة عند توفرها.
     */
    @Synchronized
    fun initialize(context: Context) {
        val applicationContext =
            context.applicationContext

        appContext = applicationContext

        RoyalTVLocalDisplayEngine.initialize(
            context = applicationContext
        )

        RoyalTVTickerManager.initialize(
            context = applicationContext
        )

        if (RoyalTVLocalDisplayEngine.hasSavedState()) {
            _state.value =
                RoyalTVLocalDisplayEngine.restoreState()
        }
    }

    /**
     * تحديث الحالة وحفظها محليًا.
     */
    @Synchronized
    fun setState(newState: RoyalTVState) {
        _state.value = newState

        RoyalTVLocalDisplayEngine.saveState(
            state = newState
        )
    }

    /**
     * تنفيذ أمر ROYAL TV.
     */
    fun executeCommand(command: RoyalTVCommand) {

        /*
         * تحديث شريط الأخبار عملية مستقلة.
         * إذا تعرّف المستقبل على الأمر وطبّقه،
         * فلا نغيّر مصدر العرض الحالي.
         */
        if (command.type == RoyalTVCommandType.SHOW_CONTENT) {
            val context = appContext

            if (
                context != null &&
                RoyalTVTickerReceiver.receive(
                    context = context,
                    command = command
                )
            ) {
                return
            }
        }

        val currentState = _state.value

        val newState =
            when (command.type) {

                RoyalTVCommandType.SHOW_ORDER -> {
                    currentState.copy(
                        source = RoyalTVSource.ORDER,
                        commandType = command.type,
                        payload = command.payload,
                        isPlaying = true,
                        isFullscreen = true
                    )
                }

                RoyalTVCommandType.SHOW_YOUTUBE -> {
                    currentState.copy(
                        source = RoyalTVSource.YOUTUBE,
                        commandType = command.type,
                        payload = command.payload,
                        isPlaying = true
                    )
                }

                RoyalTVCommandType.SHOW_MEDIA -> {
                    currentState.copy(
                        source = RoyalTVSource.MEDIA,
                        commandType = command.type,
                        payload = command.payload,
                        isPlaying = true
                    )
                }

                RoyalTVCommandType.SHOW_AD -> {
                    currentState.copy(
                        source = RoyalTVSource.AD,
                        commandType = command.type,
                        payload = command.payload,
                        isPlaying = true
                    )
                }

                RoyalTVCommandType.SHOW_CONTENT -> {
                    currentState.copy(
                        source = RoyalTVSource.CONTENT,
                        commandType = command.type,
                        payload = command.payload,
                        isPlaying = true
                    )
                }

                RoyalTVCommandType.REMOTE_COMMAND -> {
                    currentState.copy(
                        commandType = command.type,
                        payload = command.payload
                    )
                }

                RoyalTVCommandType.CLEAR_SCREEN -> {
                    RoyalTVState()
                }
            }

        setState(newState)
    }

    /**
     * تغيير حالة التشغيل فقط.
     */
    fun setPlaying(playing: Boolean) {
        setState(
            _state.value.copy(
                isPlaying = playing
            )
        )
    }

    /**
     * تغيير وضع ملء الشاشة.
     */
    fun setFullscreen(fullscreen: Boolean) {
        setState(
            _state.value.copy(
                isFullscreen = fullscreen
            )
        )
    }

    /**
     * إيقاف التشغيل مع الاحتفاظ بمعلومات المحتوى.
     */
    fun stopPlayback() {
        setPlaying(false)
    }

    /**
     * مسح الشاشة والعودة إلى الحالة الافتراضية.
     * تُحفظ الحالة الافتراضية محليًا أيضًا.
     */
    fun clearScreen() {
        setState(
            RoyalTVState()
        )
    }
}
