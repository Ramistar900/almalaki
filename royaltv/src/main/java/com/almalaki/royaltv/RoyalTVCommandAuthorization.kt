
package com.almalaki.royaltv

import android.content.Context

/**
 * ROYAL TV — نتائج التحقق من صلاحية الأوامر.
 */
enum class RoyalTVAuthorizationResult {
    AUTHORIZED,
    SENDER_NOT_PAIRED,
    SENDER_NOT_REGISTERED,
    SENDER_CANNOT_CONTROL,
    TARGET_NOT_REGISTERED,
    TARGET_CANNOT_DISPLAY,
    INVALID_SENDER_ID,
    INVALID_TARGET_ID
}

/**
 * التحقق من صلاحية إرسال الأوامر بين أجهزة ROYAL TV.
 */
object RoyalTVCommandAuthorization {

    fun authorize(
        context: Context,
        senderDeviceId: String,
        targetDeviceId: String,
        command: RoyalTVCommand
    ): RoyalTVAuthorizationResult {

        val senderId = senderDeviceId.trim()
        val targetId = targetDeviceId.trim()

        if (senderId.isBlank()) {
            return RoyalTVAuthorizationResult.INVALID_SENDER_ID
        }

        if (targetId.isBlank()) {
            return RoyalTVAuthorizationResult.INVALID_TARGET_ID
        }

        val sender = RoyalTVDeviceRegistry.getDevice(senderId)
            ?: return RoyalTVAuthorizationResult.SENDER_NOT_REGISTERED

        val target = RoyalTVDeviceRegistry.getDevice(targetId)
            ?: return RoyalTVAuthorizationResult.TARGET_NOT_REGISTERED

        val senderCanControl =
            sender.role == RoyalTVDeviceRole.CONTROLLER ||
            sender.role == RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY

        if (!senderCanControl) {
            return RoyalTVAuthorizationResult.SENDER_CANNOT_CONTROL
        }

        val targetCanDisplay =
            target.role == RoyalTVDeviceRole.DISPLAY ||
            target.role == RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY

        if (!targetCanDisplay) {
            return RoyalTVAuthorizationResult.TARGET_CANNOT_DISPLAY
        }

        // السماح للجهاز بالتحكم بنفسه دون اقتران منفصل.
        if (senderId != targetId) {
            val paired = RoyalTVPairingManager.isDevicePaired(
                context,
                senderId
            )

            if (!paired) {
                return RoyalTVAuthorizationResult.SENDER_NOT_PAIRED
            }
        }

        return when (command.type) {
            RoyalTVCommandType.SHOW_ORDER,
            RoyalTVCommandType.SHOW_YOUTUBE,
            RoyalTVCommandType.SHOW_MEDIA,
            RoyalTVCommandType.SHOW_AD,
            RoyalTVCommandType.SHOW_CONTENT,
            RoyalTVCommandType.REMOTE_COMMAND,
            RoyalTVCommandType.CLEAR_SCREEN ->
                RoyalTVAuthorizationResult.AUTHORIZED
        }
    }

    fun canControl(
        context: Context,
        deviceId: String
    ): Boolean {
        val device = RoyalTVDeviceRegistry.getDevice(
            deviceId.trim()
        ) ?: return false

        return device.role == RoyalTVDeviceRole.CONTROLLER ||
            device.role == RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY
    }

    fun canDisplay(
        context: Context,
        deviceId: String
    ): Boolean {
        val device = RoyalTVDeviceRegistry.getDevice(
            deviceId.trim()
        ) ?: return false

        return device.role == RoyalTVDeviceRole.DISPLAY ||
            device.role == RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY
    }

    fun isTrustedController(
        context: Context,
        deviceId: String
    ): Boolean {
        return canControl(context, deviceId) &&
            RoyalTVPairingManager.isDevicePaired(
                context,
                deviceId
            )
    }

    fun isValidDisplayTarget(
        context: Context,
        deviceId: String
    ): Boolean {
        return canDisplay(context, deviceId)
    }

    fun isAuthorized(
        context: Context,
        senderDeviceId: String,
        targetDeviceId: String,
        command: RoyalTVCommand
    ): Boolean {
        return authorize(
            context = context,
            senderDeviceId = senderDeviceId,
            targetDeviceId = targetDeviceId,
            command = command
        ) == RoyalTVAuthorizationResult.AUTHORIZED
    }
}
