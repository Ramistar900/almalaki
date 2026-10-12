package com.almalaki.royaltv

import android.content.Context

/**
 * نتيجة فحص طريقة عرض تطبيق داخل ROYAL TV.
 *
 * وجود نقطة تشغيل لا يعني أن الواجهة الأصلية
 * قابلة للتضمين داخل Activity أخرى.
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
     * يفحص طريقة العرض المتاحة دون تشغيل التطبيق.
     */
    fun resolve(
        context: Context,
        app: RoyalTVInstalledApp
    ): RoyalTVAppDisplayCapability {

        // أولًا: هل توجد طريقة ويب داخلية معروفة؟
        val embeddedUrl =
            RoyalTVAppCompatibility.resolveEmbeddedWebUrl(app)

        if (!embeddedUrl.isNullOrBlank()) {
            return RoyalTVAppDisplayCapability(
                mode = RoyalTVAppDisplayMode.EMBEDDED_WEB,
                message = "تتوفر طريقة عرض ويب داخل ROYAL TV.",
                embeddedUrl = embeddedUrl
            )
        }

        // ثانيًا: هل توجد نقطة تشغيل على الجهاز؟
        val hasLaunchIntent = try {
            val packageManager = context.packageManager

            packageManager.getLaunchIntentForPackage(
                app.packageName
            ) != null ||
                packageManager.getLeanbackLaunchIntentForPackage(
                    app.packageName
                ) != null
        } catch (_: RuntimeException) {
            false
        }

        return if (hasLaunchIntent) {
            RoyalTVAppDisplayCapability(
                mode = RoyalTVAppDisplayMode.NATIVE_LAUNCH_ONLY,
                message = "التطبيق قابل للتشغيل، لكن تضمين واجهته الأصلية داخل ROYAL TV غير مضمون."
            )
        } else {
            RoyalTVAppDisplayCapability(
                mode = RoyalTVAppDisplayMode.UNAVAILABLE,
                message = "لا توجد طريقة تشغيل أو عرض داخلية معروفة لهذا التطبيق."
            )
        }
    }
}
