package com.almalaki.cafe.royaltv

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * مدير الحالة المركزي لنظام ROYAL TV.
 *
 * هذا المدير مستقل عن واجهات المستخدم.
 *
 * الهاتف والتابلت والتلفزيون وTV Box
 * يمكنهم لاحقًا قراءة الحالة وتحديثها
 * من خلال هذه الطبقة.
 *
 * مهم:
 * خروج المستخدم من شاشة التحكم لا يعني
 * إيقاف تشغيل التلفزيون.
 */
object RoyalTVManager {

    private val _state =
        MutableStateFlow(
            RoyalTVState()
        )

    /**
     * الحالة الحالية لـ ROYAL TV.
     */
    val state: StateFlow<RoyalTVState> =
        _state.asStateFlow()

    /**
     * تحديث الحالة كاملة.
     */
    fun setState(
        newState: RoyalTVState
    ) {
        _state.value = newState
    }

    /**
     * تنفيذ أمر ROYAL TV
     * وتحديث الحالة بناءً على نوع الأمر.
     */
    fun executeCommand(
        command: RoyalTVCommand
    ) {
        val newState =
            when (command.type) {

                RoyalTVCommandType.SHOW_ORDER -> {
                    _state.value.copy(
                        source = RoyalTVSource.ORDER,
                        commandType = command.type,
                        payload = command.payload,
                        isPlaying = true,
                        isFullscreen = true
                    )
                }

                RoyalTVCommandType.SHOW_YOUTUBE -> {
                    _state.value.copy(
                        source = RoyalTVSource.YOUTUBE,
                        commandType = command.type,
                        payload = command.payload,
                        isPlaying = true
                    )
                }

                RoyalTVCommandType.SHOW_MEDIA -> {
                    _state.value.copy(
                        source = RoyalTVSource.MEDIA,
                        commandType = command.type,
                        payload = command.payload,
                        isPlaying = true
                    )
                }

                RoyalTVCommandType.SHOW_AD -> {
                    _state.value.copy(
                        source = RoyalTVSource.AD,
                        commandType = command.type,
                        payload = command.payload,
                        isPlaying = true
                    )
                }

                RoyalTVCommandType.SHOW_CONTENT -> {
                    _state.value.copy(
                        source = RoyalTVSource.CONTENT,
                        commandType = command.type,
                        payload = command.payload,
                        isPlaying = true
                    )
                }

                RoyalTVCommandType.REMOTE_COMMAND -> {
                    _state.value.copy(
                        commandType = command.type,
                        payload = command.payload
                    )
                }

                RoyalTVCommandType.CLEAR_SCREEN -> {
                    RoyalTVState()
                }
            }

        _state.value = newState
    }

    /**
     * تغيير حالة التشغيل فقط.
     */
    fun setPlaying(
        playing: Boolean
    ) {
        _state.value =
            _state.value.copy(
                isPlaying = playing
            )
    }

    /**
     * تغيير وضع ملء الشاشة.
     */
    fun setFullscreen(
        fullscreen: Boolean
    ) {
        _state.value =
            _state.value.copy(
                isFullscreen = fullscreen
            )
    }

    /**
     * إيقاف المحتوى مع إبقاء حالة النظام.
     */
    fun stopPlayback() {
        _state.value =
            _state.value.copy(
                isPlaying = false
            )
    }

    /**
     * مسح الشاشة والعودة للحالة الافتراضية.
     */
    fun clearScreen() {
        _state.value =
            RoyalTVState()
    }
}
