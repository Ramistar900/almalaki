package com.almalaki.cafe.royaltv

object RoyalTVKeyboardRemoteBridge {

    private var initialized = false

    fun initialize() {

        if (initialized) {
            return
        }

        initialized = true

        RoyalTVRemoteExecutor.setActionHandler {
            handleRemoteCommand(it)
        }
    }

    fun handleRemoteCommand(
        command: RoyalTVRemoteCommand
    ) {

        when (command.action) {

            RoyalTVRemoteAction.NAVIGATE_UP -> {
                RoyalTVKeyboardNavigation.up()
            }

            RoyalTVRemoteAction.NAVIGATE_DOWN -> {
                RoyalTVKeyboardNavigation.down()
            }

            RoyalTVRemoteAction.NAVIGATE_LEFT -> {
                RoyalTVKeyboardNavigation.left()
            }

            RoyalTVRemoteAction.NAVIGATE_RIGHT -> {
                RoyalTVKeyboardNavigation.right()
            }

            RoyalTVRemoteAction.SELECT -> {
                RoyalTVKeyboardNavigation.select()
            }

            RoyalTVRemoteAction.BACK -> {
                RoyalTVKeyboardManager.hide()
            }

            else -> {
                // الأوامر الأخرى تبقى للأنظمة الخاصة بها.
            }
        }
    }

    fun stop() {

        initialized = false
    }

    fun isInitialized(): Boolean {

        return initialized
    }
}
