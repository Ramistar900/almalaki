package com.almalaki.cafe.royaltv

object RoyalTVRemoteExecutor {

    private var initialized = false

    private var actionHandler:
        ((RoyalTVRemoteCommand) -> Unit)? = null

    private var lastExecutedCommand:
        RoyalTVRemoteCommand? = null

    fun initialize(
        handler:
            ((RoyalTVRemoteCommand) -> Unit)? = null
    ) {

        actionHandler =
            handler

        initialized =
            true

        RoyalTVRemoteManager.initialize {
            execute(it)
        }
    }

    fun execute(
        command: RoyalTVRemoteCommand
    ) {

        if (!initialized) {
            initialize()
        }

        lastExecutedCommand =
            command

        try {

            when (command.action) {

                RoyalTVRemoteAction.NAVIGATE_UP,
                RoyalTVRemoteAction.NAVIGATE_DOWN,
                RoyalTVRemoteAction.NAVIGATE_LEFT,
                RoyalTVRemoteAction.NAVIGATE_RIGHT,
                RoyalTVRemoteAction.SELECT,
                RoyalTVRemoteAction.BACK,
                RoyalTVRemoteAction.HOME,
                RoyalTVRemoteAction.MENU,
                RoyalTVRemoteAction.PLAY_PAUSE,
                RoyalTVRemoteAction.STOP,
                RoyalTVRemoteAction.SEEK_BACKWARD,
                RoyalTVRemoteAction.SEEK_FORWARD,
                RoyalTVRemoteAction.VOLUME_UP,
                RoyalTVRemoteAction.VOLUME_DOWN,
                RoyalTVRemoteAction.MUTE,
                RoyalTVRemoteAction.FULLSCREEN,
                RoyalTVRemoteAction.EXIT_FULLSCREEN,
                RoyalTVRemoteAction.MOUSE_MOVE,
                RoyalTVRemoteAction.MOUSE_CLICK,
                RoyalTVRemoteAction.MOUSE_DOUBLE_CLICK,
                RoyalTVRemoteAction.MOUSE_LONG_PRESS,
                RoyalTVRemoteAction.TEXT_INPUT,
                RoyalTVRemoteAction.TEXT_DELETE,
                RoyalTVRemoteAction.TEXT_CLEAR,
                RoyalTVRemoteAction.TEXT_ENTER,
                RoyalTVRemoteAction.YOUTUBE_SEARCH,
                RoyalTVRemoteAction.TICKER_UPDATE -> {

                    actionHandler?.invoke(
                        command
                    )
                }
            }

        } catch (e: Exception) {

            android.util.Log.e(
                "RoyalTVRemoteExecutor",
                "Execution error: ${e.message}",
                e
            )
        }
    }

    fun setActionHandler(
        handler:
            ((RoyalTVRemoteCommand) -> Unit)?
    ) {

        actionHandler =
            handler
    }

    fun getLastExecutedCommand():
        RoyalTVRemoteCommand? {

        return lastExecutedCommand
    }

    fun clearLastExecutedCommand() {

        lastExecutedCommand =
            null
    }

    fun isInitialized(): Boolean {

        return initialized
    }

    fun stop() {

        actionHandler =
            null

        lastExecutedCommand =
            null

        initialized =
            false
    }
}
