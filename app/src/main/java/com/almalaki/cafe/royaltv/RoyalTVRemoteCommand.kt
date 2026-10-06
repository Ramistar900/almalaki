package com.almalaki.cafe.royaltv

enum class RoyalTVRemoteAction {

    NAVIGATE_UP,

    NAVIGATE_DOWN,

    NAVIGATE_LEFT,

    NAVIGATE_RIGHT,

    SELECT,

    BACK,

    HOME,

    MENU,

    PLAY_PAUSE,

    STOP,

    SEEK_BACKWARD,

    SEEK_FORWARD,

    VOLUME_UP,

    VOLUME_DOWN,

    MUTE,

    FULLSCREEN,

    EXIT_FULLSCREEN,

    MOUSE_MOVE,

    MOUSE_CLICK,

    MOUSE_DOUBLE_CLICK,

    MOUSE_LONG_PRESS,

    TEXT_INPUT,

    TEXT_DELETE,

    TEXT_CLEAR,

    TEXT_ENTER,

    YOUTUBE_SEARCH,

    TICKER_UPDATE
}

data class RoyalTVRemoteCommand(
    val action: RoyalTVRemoteAction,
    val value: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val createdAt: Long = System.currentTimeMillis()
)
