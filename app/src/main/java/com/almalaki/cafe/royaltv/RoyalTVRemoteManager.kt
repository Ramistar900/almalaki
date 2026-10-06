package com.almalaki.cafe.royaltv

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RoyalTVRemoteManager {

    private val _lastCommand =
        MutableStateFlow<RoyalTVRemoteCommand?>(null)

    val lastCommand:
        StateFlow<RoyalTVRemoteCommand?> =
        _lastCommand.asStateFlow()

    private val _isConnected =
        MutableStateFlow(false)

    val isConnected:
        StateFlow<Boolean> =
        _isConnected.asStateFlow()

    private var commandHandler:
        ((RoyalTVRemoteCommand) -> Unit)? = null

    fun initialize(
        handler:
            ((RoyalTVRemoteCommand) -> Unit)? = null
    ) {

        commandHandler = handler

        RoyalTVCommandQueue.initialize {
            RoyalTVManager.executeCommand(it)
        }
    }

    fun setConnected(
        connected: Boolean
    ) {

        _isConnected.value =
            connected
    }

    fun isReady(): Boolean {
        return commandHandler != null ||
            RoyalTVCommandQueue.isProcessing() ||
            !RoyalTVCommandQueue.isEmpty()
    }

    fun send(
        command: RoyalTVRemoteCommand
    ) {

        _lastCommand.value =
            command

        commandHandler?.invoke(
            command
        )
    }

    fun sendAction(
        action: RoyalTVRemoteAction,
        value: String = "",
        x: Float = 0f,
        y: Float = 0f
    ) {

        send(
            RoyalTVRemoteCommand(
                action = action,
                value = value,
                x = x,
                y = y
            )
        )
    }

    fun navigateUp() {

        sendAction(
            RoyalTVRemoteAction.NAVIGATE_UP
        )
    }

    fun navigateDown() {

        sendAction(
            RoyalTVRemoteAction.NAVIGATE_DOWN
        )
    }

    fun navigateLeft() {

        sendAction(
            RoyalTVRemoteAction.NAVIGATE_LEFT
        )
    }

    fun navigateRight() {

        sendAction(
            RoyalTVRemoteAction.NAVIGATE_RIGHT
        )
    }

    fun select() {

        sendAction(
            RoyalTVRemoteAction.SELECT
        )
    }

    fun back() {

        sendAction(
            RoyalTVRemoteAction.BACK
        )
    }

    fun home() {

        sendAction(
            RoyalTVRemoteAction.HOME
        )
    }

    fun menu() {

        sendAction(
            RoyalTVRemoteAction.MENU
        )
    }

    fun playPause() {

        sendAction(
            RoyalTVRemoteAction.PLAY_PAUSE
        )
    }

    fun stop() {

        sendAction(
            RoyalTVRemoteAction.STOP
        )
    }

    fun seekBackward() {

        sendAction(
            RoyalTVRemoteAction.SEEK_BACKWARD
        )
    }

    fun seekForward() {

        sendAction(
            RoyalTVRemoteAction.SEEK_FORWARD
        )
    }

    fun volumeUp() {

        sendAction(
            RoyalTVRemoteAction.VOLUME_UP
        )
    }

    fun volumeDown() {

        sendAction(
            RoyalTVRemoteAction.VOLUME_DOWN
        )
    }

    fun mute() {

        sendAction(
            RoyalTVRemoteAction.MUTE
        )
    }

    fun fullscreen() {

        sendAction(
            RoyalTVRemoteAction.FULLSCREEN
        )
    }

    fun exitFullscreen() {

        sendAction(
            RoyalTVRemoteAction.EXIT_FULLSCREEN
        )
    }

    fun moveMouse(
        x: Float,
        y: Float
    ) {

        sendAction(
            action =
                RoyalTVRemoteAction.MOUSE_MOVE,
            x = x,
            y = y
        )
    }

    fun mouseClick() {

        sendAction(
            RoyalTVRemoteAction.MOUSE_CLICK
        )
    }

    fun mouseDoubleClick() {

        sendAction(
            RoyalTVRemoteAction.MOUSE_DOUBLE_CLICK
        )
    }

    fun mouseLongPress() {

        sendAction(
            RoyalTVRemoteAction.MOUSE_LONG_PRESS
        )
    }

    fun inputText(
        text: String
    ) {

        if (text.isEmpty()) {
            return
        }

        sendAction(
            action =
                RoyalTVRemoteAction.TEXT_INPUT,
            value = text
        )
    }

    fun deleteText() {

        sendAction(
            RoyalTVRemoteAction.TEXT_DELETE
        )
    }

    fun clearText() {

        sendAction(
            RoyalTVRemoteAction.TEXT_CLEAR
        )
    }

    fun enterText() {

        sendAction(
            RoyalTVRemoteAction.TEXT_ENTER
        )
    }

    fun searchYouTube(
        query: String
    ) {

        val cleanQuery =
            query.trim()

        if (cleanQuery.isEmpty()) {
            return
        }

        sendAction(
            action =
                RoyalTVRemoteAction.YOUTUBE_SEARCH,
            value = cleanQuery
        )
    }

    fun updateTicker(
        text: String
    ) {

        sendAction(
            action =
                RoyalTVRemoteAction.TICKER_UPDATE,
            value = text
        )
    }

    fun clearLastCommand() {

        _lastCommand.value =
            null
    }

    fun stop() {

        commandHandler =
            null

        _isConnected.value =
            false

        _lastCommand.value =
            null
    }
}
