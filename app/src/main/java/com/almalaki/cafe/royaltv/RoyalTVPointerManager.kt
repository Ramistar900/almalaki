package com.almalaki.cafe.royaltv

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RoyalTVPointerState(
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val visible: Boolean = true,
    val pressed: Boolean = false
)

enum class RoyalTVPointerAction {
    MOVE,
    CLICK,
    DOUBLE_CLICK,
    LONG_PRESS
}

data class RoyalTVPointerEvent(
    val action: RoyalTVPointerAction,
    val x: Float,
    val y: Float,
    val createdAt: Long = System.currentTimeMillis()
)

object RoyalTVPointerManager {

    private val _state =
        MutableStateFlow(
            RoyalTVPointerState()
        )

    val state:
        StateFlow<RoyalTVPointerState> =
        _state.asStateFlow()

    private val _lastEvent =
        MutableStateFlow<RoyalTVPointerEvent?>(null)

    val lastEvent:
        StateFlow<RoyalTVPointerEvent?> =
        _lastEvent.asStateFlow()

    private var eventHandler:
        ((RoyalTVPointerEvent) -> Unit)? = null

    fun initialize(
        handler:
            ((RoyalTVPointerEvent) -> Unit)? = null
    ) {

        eventHandler =
            handler
    }

    fun execute(
        command: RoyalTVRemoteCommand
    ) {

        when (command.action) {

            RoyalTVRemoteAction.MOUSE_MOVE -> {

                moveTo(
                    x = command.x,
                    y = command.y
                )
            }

            RoyalTVRemoteAction.MOUSE_CLICK -> {

                click()
            }

            RoyalTVRemoteAction.MOUSE_DOUBLE_CLICK -> {

                doubleClick()
            }

            RoyalTVRemoteAction.MOUSE_LONG_PRESS -> {

                longPress()
            }

            else -> {
                return
            }
        }
    }

    fun moveTo(
        x: Float,
        y: Float
    ) {

        val safeX =
            x.coerceIn(
                0f,
                1f
            )

        val safeY =
            y.coerceIn(
                0f,
                1f
            )

        _state.value =
            _state.value.copy(
                x = safeX,
                y = safeY,
                visible = true,
                pressed = false
            )

        emitEvent(
            action =
                RoyalTVPointerAction.MOVE,
            x = safeX,
            y = safeY
        )
    }

    fun moveBy(
        deltaX: Float,
        deltaY: Float
    ) {

        val current =
            _state.value

        moveTo(
            x =
                current.x +
                    deltaX,
            y =
                current.y +
                    deltaY
        )
    }

    fun click() {

        val current =
            _state.value

        _state.value =
            current.copy(
                visible = true,
                pressed = true
            )

        emitEvent(
            action =
                RoyalTVPointerAction.CLICK,
            x = current.x,
            y = current.y
        )

        _state.value =
            _state.value.copy(
                pressed = false
            )
    }

    fun doubleClick() {

        val current =
            _state.value

        emitEvent(
            action =
                RoyalTVPointerAction.DOUBLE_CLICK,
            x = current.x,
            y = current.y
        )
    }

    fun longPress() {

        val current =
            _state.value

        emitEvent(
            action =
                RoyalTVPointerAction.LONG_PRESS,
            x = current.x,
            y = current.y
        )
    }

    fun show() {

        _state.value =
            _state.value.copy(
                visible = true
            )
    }

    fun hide() {

        _state.value =
            _state.value.copy(
                visible = false,
                pressed = false
            )
    }

    fun toggleVisibility() {

        _state.value =
            _state.value.copy(
                visible =
                    !_state.value.visible
            )
    }

    fun resetPosition() {

        moveTo(
            x = 0.5f,
            y = 0.5f
        )
    }

    fun setEventHandler(
        handler:
            ((RoyalTVPointerEvent) -> Unit)?
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

        _state.value =
            RoyalTVPointerState()
    }

    private fun emitEvent(
        action: RoyalTVPointerAction,
        x: Float,
        y: Float
    ) {

        val event =
            RoyalTVPointerEvent(
                action = action,
                x = x,
                y = y
            )

        _lastEvent.value =
            event

        eventHandler?.invoke(
            event
        )
    }
}
