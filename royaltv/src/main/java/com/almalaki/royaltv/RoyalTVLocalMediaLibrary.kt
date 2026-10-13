package com.almalaki.royaltv

import android.content.Context
import android.net.Uri
import org.json.JSONArray

/**
 * مكتبة الوسائط المحلية في ROYAL TV.
 * تدعم حفظ عدة صور لكل قسم بصورة دائمة على الجهاز.
 */
object RoyalTVLocalMediaLibrary {

    private const val PREFS_NAME = "royal_tv_local_media"
    private const val IMAGES_SUFFIX = "_images"

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
        val images = getImages(context, section).toMutableList()

        if (uri !in images) {
            images.add(uri)
        }

        saveImages(context, section, images)
    }

    fun getImage(
        context: Context,
        section: Section
    ): Uri? {
        val images = getImages(context, section)
        if (images.isNotEmpty()) {
            return images.first()
        }

        // التوافق مع الصور المحفوظة بالإصدار القديم.
        val value = preferences(context)
            .getString(section.key, null)
            ?: return null

        return runCatching {
            Uri.parse(value)
        }.getOrNull()
    }

    fun saveImages(
        context: Context,
        section: Section,
        images: List<Uri>
    ) {
        val json = JSONArray()

        images.distinct().forEach { uri ->
            json.put(uri.toString())
        }

        preferences(context)
            .edit()
            .putString(
                section.key + IMAGES_SUFFIX,
                json.toString()
            )
            // نحافظ على الصورة الأولى لتوافق الدوال القديمة.
            .putString(
                section.key,
                images.firstOrNull()?.toString()
            )
            .apply()
    }

    fun getImages(
        context: Context,
        section: Section
    ): List<Uri> {
        val prefs = preferences(context)
        val saved = prefs.getString(
            section.key + IMAGES_SUFFIX,
            null
        )

        if (!saved.isNullOrBlank()) {
            return runCatching {
                val json = JSONArray(saved)
                buildList {
                    for (index in 0 until json.length()) {
                        val value = json.optString(index)
                        if (value.isNotBlank()) {
                            add(Uri.parse(value))
                        }
                    }
                }
            }.getOrDefault(emptyList())
        }

        // ترحيل الصورة القديمة تلقائيًا إلى قائمة الصور.
        val oldImage = prefs.getString(section.key, null)
            ?: return emptyList()

        val uri = runCatching {
            Uri.parse(oldImage)
        }.getOrNull() ?: return emptyList()

        saveImages(context, section, listOf(uri))
        return listOf(uri)
    }

    fun clearImage(
        context: Context,
        section: Section
    ) {
        preferences(context)
            .edit()
            .remove(section.key)
            .remove(section.key + IMAGES_SUFFIX)
            .apply()
    }

    fun clearImages(
        context: Context,
        section: Section
    ) {
        clearImage(context, section)
    }

    private fun preferences(
        context: Context
    ) = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
}
