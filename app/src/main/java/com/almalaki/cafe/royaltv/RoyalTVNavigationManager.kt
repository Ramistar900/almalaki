package com.almalaki.cafe.royaltv

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RoyalTVNavigationAction {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    SELECT,
    BACK,
    HOME,
    MENU
}

data class RoyalTVNavigationEvent(
    val action: RoyalTVNavigationAction,
    val createdAt: Long = System.currentTimeMillis()
)

object RoyalTVNavigationManager {

    private val _lastEvent =
        MutableStateFlow<RoyalTVNavigationEvent?>(null)

    val lastEvent:
        StateFlow<RoyalTVNavigationEvent?> =
        _lastEvent.asStateFlow()

    private var eventHandler:
        ((RoyalTVNavigationEvent) -> Unit)? = null

    fun initialize(
        handler:
            ((RoyalTVNavigationEvent) -> Unit)? = null
    ) {

        eventHandler =
            handler
    }

    fun execute(
        command: RoyalTVRemoteCommand
    ) {

        val action =
            when (command.action) {

                RoyalTVRemoteAction.NAVIGATE_UP ->
                    RoyalTVNavigationAction.UP

                RoyalTVRemoteAction.NAVIGATE_DOWN ->
                    RoyalTVNavigationAction.DOWN

                RoyalTVRemoteAction.NAVIGATE_LEFT ->
                    RoyalTVNavigationAction.LEFT

                RoyalTVRemoteAction.NAVIGATE_RIGHT ->
                    RoyalTVNavigationAction.RIGHT

                RoyalTVRemoteAction.SELECT ->
                    RoyalTVNavigationAction.SELECT

                RoyalTVRemoteAction.BACK ->
                    RoyalTVNavigationAction.BACK

                RoyalTVRemoteAction.HOME ->
                    RoyalTVNavigationAction.HOME

                RoyalTVRemoteAction.MENU ->
                    RoyalTVNavigationAction.MENU

                else ->
                    return
            }

        val event =
            RoyalTVNavigationEvent(
                action = action
            )

        _lastEvent.value =
            event

        eventHandler?.invoke(
            event
        )
    }

    fun send(
        action: RoyalTVNavigationAction
    ) {

        val event =
            RoyalTVNavigationEvent(
                action = action
            )

        _lastEvent.value =
            event

        eventHandler?.invoke(
            event
        )
    }

    fun up() {
        send(
            RoyalTVNavigationAction.UP
        )
    }

    fun down() {
        send(
            RoyalTVNavigationAction.DOWN
        )
    }

    fun left() {
        send(
            RoyalTVNavigationAction.LEFT
        )
    }

    fun right() {
        send(
            RoyalTVNavigationAction.RIGHT
        )
    }

    fun select() {
        send(
            RoyalTVNavigationAction.SELECT
        )
    }

    fun back() {
        send(
            RoyalTVNavigationAction.BACK
        )
    }

    fun home() {
        send(
            RoyalTVNavigationAction.HOME
        )
    }

    fun menu() {
        send(
            RoyalTVNavigationAction.MENU
        )
    }

    fun setEventHandler(
        handler:
            ((RoyalTVNavigationEvent) -> Unit)?
    ) {

        eventHandler =
            handler
    }

    fun clearLastEvent() {

        _lastEvent.value =
            null
    }

    fun stop() {

        eventHandler =
            null

        _lastEvent.value =
            null
    }
}
