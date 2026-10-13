package com.almalaki.royaltv

import android.content.Context
import android.net.Uri

/**
 * مكتبة الصور المحلية في ROYAL TV.
 * تحفظ مرجع الصورة المختارة لكل قسم على الجهاز.
 */
object RoyalTVLocalMediaLibrary {

    private const val PREFS_NAME = "royal_tv_local_media"

    enum class Section(val key: String) {
        NEW_PRODUCTS("new_products_uri"),
        ADS("ads_uri"),
        ORDERS("orders_uri")
    }

    fun saveImage(
        context: Context,
        section: Section,
        uri: Uri
    ) {
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(section.key, uri.toString())
            .apply()
    }

    fun getImage(
        context: Context,
        section: Section
    ): Uri? {
        val value = context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getString(section.key, null)
            ?: return null

        return runCatching {
            Uri.parse(value)
        }.getOrNull()
    }

    fun clearImage(
        context: Context,
        section: Section
    ) {
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(section.key)
            .apply()
    }
}
