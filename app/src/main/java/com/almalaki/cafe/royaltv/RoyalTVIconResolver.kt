package com.almalaki.cafe.royaltv

/**
 * ============================================================
 * ROYAL TV ICON SYSTEM
 * ============================================================
 *
 * نظام موحد لأيقونات Royal TV.
 *
 * المبدأ:
 *
 * الوظيفة
 *    ↓
 * RoyalTVIconKey
 *    ↓
 * RoyalTVIconResolver
 *    ↓
 * أيقونة PNG المخصصة عند توفرها
 *
 * لا تعتمد غرفة التحكم على Emoji أو أسماء عشوائية.
 *
 * كل وظيفة لها مفتاح ثابت يمكن ربطه لاحقًا
 * بملف PNG خاص بها.
 *
 * ============================================================
 */

enum class RoyalTVIconKey {
    DISPLAY,
    TICKER,
    IDENTITY,
    FONTS,
    CLOCK,
    LIVE,
    EXTRAS,
    EDITOR,
    CONTENT,
    KEYBOARD,
    CONNECTION,
    STORAGE,
    SECURITY,
    YOUTUBE,
    TV_APPS,
    DEVICE_MEDIA,
    PHONE_APPS,
    PHONE_SCREEN,
    ADS,
    SPORTS,
    ORDERS,
    REMOTE,
    REMOTE_DIAGNOSTICS
}

data class RoyalTVIconDefinition(
    val key: RoyalTVIconKey,
    val assetName: String
)

object RoyalTVIconResolver {

    private val definitions =
        mapOf(
            RoyalTVIconKey.DISPLAY to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.DISPLAY,
                    "royal_tv_icon_display"
                ),

            RoyalTVIconKey.TICKER to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.TICKER,
                    "royal_tv_icon_ticker"
                ),

            RoyalTVIconKey.IDENTITY to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.IDENTITY,
                    "royal_tv_icon_identity"
                ),

            RoyalTVIconKey.FONTS to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.FONTS,
                    "royal_tv_icon_fonts"
                ),

            RoyalTVIconKey.CLOCK to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.CLOCK,
                    "royal_tv_icon_clock"
                ),

            RoyalTVIconKey.LIVE to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.LIVE,
                    "royal_tv_icon_live"
                ),

            RoyalTVIconKey.EXTRAS to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.EXTRAS,
                    "royal_tv_icon_extras"
                ),

            RoyalTVIconKey.EDITOR to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.EDITOR,
                    "royal_tv_icon_editor"
                ),

            RoyalTVIconKey.CONTENT to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.CONTENT,
                    "royal_tv_icon_content"
                ),

            RoyalTVIconKey.KEYBOARD to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.KEYBOARD,
                    "royal_tv_icon_keyboard"
                ),

            RoyalTVIconKey.CONNECTION to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.CONNECTION,
                    "royal_tv_icon_connection"
                ),

            RoyalTVIconKey.STORAGE to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.STORAGE,
                    "royal_tv_icon_storage"
                ),

            RoyalTVIconKey.SECURITY to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.SECURITY,
                    "royal_tv_icon_security"
                ),

            RoyalTVIconKey.YOUTUBE to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.YOUTUBE,
                    "royal_tv_icon_youtube"
                ),

            RoyalTVIconKey.TV_APPS to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.TV_APPS,
                    "royal_tv_icon_tv_apps"
                ),

            RoyalTVIconKey.DEVICE_MEDIA to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.DEVICE_MEDIA,
                    "royal_tv_icon_device_media"
                ),

            RoyalTVIconKey.PHONE_APPS to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.PHONE_APPS,
                    "royal_tv_icon_phone_apps"
                ),

            RoyalTVIconKey.PHONE_SCREEN to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.PHONE_SCREEN,
                    "royal_tv_icon_phone_screen"
                ),

            RoyalTVIconKey.ADS to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.ADS,
                    "royal_tv_icon_ads"
                ),

            RoyalTVIconKey.SPORTS to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.SPORTS,
                    "royal_tv_icon_sports"
                ),

            RoyalTVIconKey.ORDERS to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.ORDERS,
                    "royal_tv_icon_orders"
                ),

            RoyalTVIconKey.REMOTE to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.REMOTE,
                    "royal_tv_icon_remote"
                ),

            RoyalTVIconKey.REMOTE_DIAGNOSTICS to
                RoyalTVIconDefinition(
                    RoyalTVIconKey.REMOTE_DIAGNOSTICS,
                    "royal_tv_icon_remote_diagnostics"
                )
        )

    fun resolve(
        key: RoyalTVIconKey
    ): RoyalTVIconDefinition {
        return definitions.getValue(key)
    }

    fun assetName(
        key: RoyalTVIconKey
    ): String {
        return resolve(key).assetName
    }

    fun contains(
        key: RoyalTVIconKey
    ): Boolean {
        return definitions.containsKey(key)
    }
}
