
package com.almalaki.royaltv

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * إدارة حالة العرض داخل تطبيق ROYAL TV.
 * مستقلة عن تطبيق المالك لأنهما وحدتان منفصلتان.
 */
object RoyalTVDisplayStateManager {

    private val _state = MutableStateFlow(RoyalTVState())

    val state: StateFlow<RoyalTVState> =
        _state.asStateFlow()

    @Synchronized
    fun executeCommand(command: RoyalTVCommand) {
        val current = _state.value

        _state.value = when (command.type) {

            RoyalTVCommandType.SHOW_ORDER ->
                current.copy(
                    source = RoyalTVSource.ORDER,
                    commandType = command.type,
                    payload = command.payload,
                    isPlaying = true,
                    isFullscreen = true
                )

            RoyalTVCommandType.SHOW_YOUTUBE ->
                current.copy(
                    source = RoyalTVSource.YOUTUBE,
                    commandType = command.type,
                    payload = command.payload,
                    isPlaying = true
                )

            RoyalTVCommandType.SHOW_MEDIA ->
                current.copy(
                    source = RoyalTVSource.MEDIA,
                    commandType = command.type,
                    payload = command.payload,
                    isPlaying = true
                )

            RoyalTVCommandType.SHOW_AD ->
                current.copy(
                    source = RoyalTVSource.AD,
                    commandType = command.type,
                    payload = command.payload,
                    isPlaying = true
                )

            RoyalTVCommandType.SHOW_CONTENT ->
                current.copy(
                    source = RoyalTVSource.CONTENT,
                    commandType = command.type,
                    payload = command.payload,
                    isPlaying = true
                )

            RoyalTVCommandType.REMOTE_COMMAND ->
                current.copy(
                    commandType = command.type,
                    payload = command.payload
                )

            RoyalTVCommandType.CLEAR_SCREEN ->
                RoyalTVState()
        }
    }
}
