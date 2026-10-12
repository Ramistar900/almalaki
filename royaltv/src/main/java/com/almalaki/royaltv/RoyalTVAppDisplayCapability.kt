package com.almalaki.royaltv

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri

/**
 * نتيجة فحص طريقة عرض تطبيق داخل ROYAL TV.
 *
 * وجود نقطة تشغيل لا يعني أن Activity خارجية
 * قابلة للتضمين داخل نافذة ROYAL TV.
 */
internal enum class RoyalTVAppDisplayMode {
    EMBEDDED_WEB,
    NATIVE_LAUNCH_ONLY,
    UNAVAILABLE
}

internal data class RoyalTVAppDisplayCapability(
    val mode: RoyalTVAppDisplayMode,
    val message: String,
    val embeddedUrl: String? = null
)

internal object RoyalTVAppDisplayCapabilityResolver {

    /**
     * اسم metadata اختياري يمكن للتطبيق الخارجي إعلانه.
     */
    const val EMBEDDED_WEB_URL_METADATA =
        "com.almalaki.royaltv.EMBEDDED_WEB_URL"

    /**
     * يفحص طرق العرض المتاحة دون تشغيل التطبيق خارجيًا.
     */
    fun resolve(
        context: Context,
        app: RoyalTVInstalledApp
    ): RoyalTVAppDisplayCapability {

        // 1. البحث عن تكامل معروف لهذا التطبيق.
        val compatibilityUrl =
            RoyalTVAppCompatibility.resolveEmbeddedWebUrl(app)

        if (isSafeWebUrl(compatibilityUrl)) {
            return RoyalTVAppDisplayCapability(
                mode = RoyalTVAppDisplayMode.EMBEDDED_WEB,
                message =
                    "تم العثور على مصدر ويب يمكن عرضه داخل نافذة ROYAL TV.",
                embeddedUrl = compatibilityUrl
            )
        }

        // 2. البحث عن مصدر يعلنه التطبيق الخارجي بنفسه.
        val declaredUrl =
            readDeclaredEmbeddedUrl(context, app)

        if (isSafeWebUrl(declaredUrl)) {
            return RoyalTVAppDisplayCapability(
                mode = RoyalTVAppDisplayMode.EMBEDDED_WEB,
                message =
                    "هذا التطبيق يعلن مصدر ويب متوافقًا مع العرض داخل ROYAL TV.",
                embeddedUrl = declaredUrl
            )
        }

        // 3. التحقق من وجود نقطة تشغيل عامة.
        val hasLaunchIntent = try {
            val packageManager = context.packageManager

            packageManager.getLaunchIntentForPackage(
                app.packageName
            ) != null ||
                packageManager.getLeanbackLaunchIntentForPackage(
                    app.packageName
                ) != null
        } catch (_: Exception) {
            false
        }

        return if (hasLaunchIntent) {
            RoyalTVAppDisplayCapability(
                mode = RoyalTVAppDisplayMode.NATIVE_LAUNCH_ONLY,
                message =
                    "التطبيق مثبت، لكنه لا يعلن حاليًا طريقة تضمين متوافقة. " +
                    "يمكن إضافة التكامل إذا وفر التطبيق واجهة مخصصة " +
                    "أو دعمًا مناسبًا من النظام."
            )
        } else {
            RoyalTVAppDisplayCapability(
                mode = RoyalTVAppDisplayMode.UNAVAILABLE,
                message =
                    "لم يتم العثور على مصدر عرض مضمّن " +
                    "أو نقطة تشغيل عامة لهذا التطبيق."
            )
        }
    }

    /**
     * قراءة عنوان العرض الذي يعلنه التطبيق الخارجي.
     */
    private fun readDeclaredEmbeddedUrl(
        context: Context,
        app: RoyalTVInstalledApp
    ): String? {
        return try {
            @Suppress("DEPRECATION")
            val applicationInfo =
                context.packageManager.getApplicationInfo(
                    app.packageName,
                    PackageManager.GET_META_DATA
                )

            applicationInfo.metaData
                ?.getString(EMBEDDED_WEB_URL_METADATA)
                ?.trim()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * السماح بعناوين HTTP وHTTPS فقط.
     */
    private fun isSafeWebUrl(
        url: String?
    ): Boolean {
        if (url.isNullOrBlank()) return false

        return try {
            val uri = Uri.parse(url)

            (uri.scheme == "https" ||
                uri.scheme == "http") &&
                !uri.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }
}
