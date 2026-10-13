
package com.almalaki.royaltv

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import java.io.File

/**
 * مكتبة الوسائط المحلية في ROYAL TV.
 *
 * تحفظ الصور داخل مساحة التطبيق الخاصة،
 * كي تبقى متاحة بعد الرجوع وإعادة إنشاء الشاشة.
 */
object RoyalTVLocalMediaLibrary {

    private const val PREFS_NAME = "royal_tv_local_media"
    private const val IMAGES_SUFFIX = "_images"
    private const val IMAGE_FOLDER = "royal_tv_images"

    enum class Section(val key: String) {
        NEW_PRODUCTS("new_products_uri"),
        ADS("ads_uri"),
        ORDERS("orders_uri")
    }

    /**
     * إضافة صورة واحدة إلى صور القسم.
     */
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

    /**
     * إرجاع أول صورة محفوظة.
     */
    fun getImage(
        context: Context,
        section: Section
    ): Uri? {
        return getImages(context, section).firstOrNull()
    }

    /**
     * حفظ مجموعة صور بعد نسخها إلى مساحة التطبيق الداخلية.
     */
    fun saveImages(
        context: Context,
        section: Section,
        images: List<Uri>
    ) {
        val uniqueImages = images.distinct()

        val localImages = uniqueImages.mapNotNull { uri ->
            copyIntoAppStorage(context, section, uri)
        }.distinct()

        // إذا تعذّر النسخ لأي سبب، لا نمسح المراجع الأصلية.
        val imagesToSave = if (
            localImages.isEmpty() && uniqueImages.isNotEmpty()
        ) {
            uniqueImages
        } else {
            localImages
        }

        val json = JSONArray()

        imagesToSave.forEach { uri ->
            json.put(uri.toString())
        }

        preferences(context)
            .edit()
            .putString(
                section.key + IMAGES_SUFFIX,
                json.toString()
            )
            .putString(
                section.key,
                imagesToSave.firstOrNull()?.toString()
            )
            .apply()
    }

    /**
     * استعادة الصور المحفوظة، مع ترحيل الصور القديمة
     * إلى التخزين الداخلي متى أمكن.
     */
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
            val savedImages = runCatching {
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

            if (savedImages.isEmpty()) {
                return emptyList()
            }

            val localImages = savedImages.mapNotNull { uri ->
                copyIntoAppStorage(context, section, uri)
            }.distinct()

            if (localImages.isNotEmpty()) {
                if (localImages != savedImages) {
                    saveImages(context, section, localImages)
                }

                return localImages
            }

            // لا نفقد المراجع المحفوظة إذا تعذّر النسخ.
            return savedImages
        }

        // ترحيل الصورة المحفوظة بالإصدار القديم.
        val oldImage = prefs.getString(section.key, null)
            ?: return emptyList()

        val oldUri = runCatching {
            Uri.parse(oldImage)
        }.getOrNull() ?: return emptyList()

        val localUri = copyIntoAppStorage(
            context,
            section,
            oldUri
        )

        val result = listOf(localUri ?: oldUri)

        saveImages(context, section, result)

        return result
    }

    /**
     * حذف الصور المحفوظة في قائمة القسم.
     * لا يحذف ملفات الصور من المجلد الداخلي.
     */
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

    /**
     * نسخ الصورة إلى مجلد داخلي لا يحتاج إلى صلاحية
     * الوصول إلى مزود الملفات الخارجي بعد النسخ.
     */
    private fun copyIntoAppStorage(
        context: Context,
        section: Section,
        uri: Uri
    ): Uri? {
        return runCatching {
            val appFilesPath =
                context.filesDir.absolutePath + File.separator

            // إذا كانت الصورة محفوظة بالفعل داخل التطبيق،
            // نعيد استخدامها دون إنشاء نسخة إضافية.
            if (
                uri.scheme == "file" &&
                uri.path != null &&
                uri.path!!.startsWith(appFilesPath) &&
                File(uri.path!!).exists()
            ) {
                return@runCatching uri
            }

            val directory = File(
                context.filesDir,
                IMAGE_FOLDER + File.separator + section.key
            )

            if (!directory.exists() && !directory.mkdirs()) {
                return@runCatching null
            }

            val destination = File.createTempFile(
                "royal_image_",
                ".img",
                directory
            )

            val input = if (uri.scheme == "file") {
                val path = uri.path ?: return@runCatching null
                File(path).inputStream()
            } else {
                context.contentResolver.openInputStream(uri)
                    ?: return@runCatching null
            }

            input.use { source ->
                destination.outputStream().use { output ->
                    source.copyTo(output)
                }
            }

            Uri.fromFile(destination)
        }.getOrNull()
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
}
