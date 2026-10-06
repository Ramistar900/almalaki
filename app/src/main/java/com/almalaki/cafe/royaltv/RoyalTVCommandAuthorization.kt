package com.almalaki.cafe.royaltv

import android.content.Context

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

object RoyalTVCommandAuthorization {

    /**
     * التحقق من إمكانية إرسال أمر من جهاز
     * إلى جهاز ROYAL TV آخر.
     */
    fun authorize(
        context: Context,
        senderDeviceId: String,
        targetDeviceId: String,
        command: RoyalTVCommand
    ): RoyalTVAuthorizationResult {

        val cleanSenderId =
            senderDeviceId.trim()

        val cleanTargetId =
            targetDeviceId.trim()

        if (cleanSenderId.isBlank()) {
            return RoyalTVAuthorizationResult
                .INVALID_SENDER_ID
        }

        if (cleanTargetId.isBlank()) {
            return RoyalTVAuthorizationResult
                .INVALID_TARGET_ID
        }

        val sender =
            RoyalTVDeviceRegistry.getDevice(
                cleanSenderId
            )
                ?: return RoyalTVAuthorizationResult
                    .SENDER_NOT_REGISTERED

        val target =
            RoyalTVDeviceRegistry.getDevice(
                cleanTargetId
            )
                ?: return RoyalTVAuthorizationResult
                    .TARGET_NOT_REGISTERED

        /*
         * الجهاز المرسل يجب أن يكون قادرًا على التحكم.
         */
        val senderCanControl =
            sender.role ==
                RoyalTVDeviceRole.CONTROLLER ||
                sender.role ==
                RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY

        if (!senderCanControl) {
            return RoyalTVAuthorizationResult
                .SENDER_CANNOT_CONTROL
        }

        /*
         * الجهاز الهدف يجب أن يكون قادرًا على العرض.
         */
        val targetCanDisplay =
            target.role ==
                RoyalTVDeviceRole.DISPLAY ||
                target.role ==
                RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY

        if (!targetCanDisplay) {
            return RoyalTVAuthorizationResult
                .TARGET_CANNOT_DISPLAY
        }

        /*
         * لا نطلب Pairing عندما يكون الجهاز
         * يرسل أمرًا إلى نفسه.
         *
         * هذا مهم للـ TV الذي يعمل Controller + Display.
         */
        if (cleanSenderId != cleanTargetId) {

            val senderIsPaired =
                RoyalTVPairingManager
                    .isDevicePaired(
                        context,
                        cleanSenderId
                    )

            if (!senderIsPaired) {
                return RoyalTVAuthorizationResult
                    .SENDER_NOT_PAIRED
            }
        }

        /*
         * الأوامر الخاصة بالعرض مسموحة للأجهزة
         * التي تم التحقق من صلاحياتها أعلاه.
         */
        return when (command.type) {

            RoyalTVCommandType.SHOW_ORDER,
            RoyalTVCommandType.SHOW_YOUTUBE,
            RoyalTVCommandType.SHOW_MEDIA,
            RoyalTVCommandType.SHOW_AD,
            RoyalTVCommandType.SHOW_CONTENT,
            RoyalTVCommandType.REMOTE_COMMAND,
            RoyalTVCommandType.CLEAR_SCREEN ->
                RoyalTVAuthorizationResult
                    .AUTHORIZED
        }
    }

    /**
     * تحقق سريع من قدرة جهاز على التحكم.
     */
    fun canControl(
        context: Context,
        deviceId: String
    ): Boolean {

        val device =
            RoyalTVDeviceRegistry.getDevice(
                deviceId
            )
                ?: return false

        return device.role ==
            RoyalTVDeviceRole.CONTROLLER ||
            device.role ==
            RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY
    }

    /**
     * تحقق سريع من قدرة جهاز على العرض.
     */
    fun canDisplay(
        context: Context,
        deviceId: String
    ): Boolean {

        val device =
            RoyalTVDeviceRegistry.getDevice(
                deviceId
            )
                ?: return false

        return device.role ==
            RoyalTVDeviceRole.DISPLAY ||
            device.role ==
            RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY
    }

    /**
     * تحقق من أن جهازًا موثوق به ويمكنه التحكم.
     */
    fun isTrustedController(
        context: Context,
        deviceId: String
    ): Boolean {

        if (!canControl(
                context,
                deviceId
            )
        ) {
            return false
        }

        return RoyalTVPairingManager
            .isDevicePaired(
                context,
                deviceId
            )
    }

    /**
     * تحقق من أن الجهاز الهدف شاشة صالحة.
     */
    fun isValidDisplayTarget(
        context: Context,
        deviceId: String
    ): Boolean {

        return canDisplay(
            context,
            deviceId
        )
    }

    /**
     * تحويل نتيجة التفويض إلى نجاح/فشل بسيط.
     */
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
        ) ==
            RoyalTVAuthorizationResult.AUTHORIZED
    }
}
