package com.almalaki.royaltv

/**
 * يحدد التطبيقات التي نملك لها طريقة عرض ويب داخل نافذة ROYAL TV.
 *
 * YouTube Web له مربع مستقل في واجهة ROYAL TV.
 * أما YouTube المثبّت فيُعامل كتطبيق أصلي من مربع التطبيقات.
 */
internal object RoyalTVAppCompatibility {

    /**
     * يعيد رابط ويب داخليًا للتطبيقات التي نقرر دعمها بهذه الطريقة.
     *
     * لا نحول تطبيق YouTube المثبّت إلى صفحة ويب هنا؛
     * تشغيله يتم من مسار التطبيقات الأصلي.
     */
    fun resolveEmbeddedWebUrl(
        app: RoyalTVInstalledApp
    ): String? {
        return when (app.packageName) {
            // لا توجد حاليًا تطبيقات مخصصة في هذا الملف.
            // يبقى YouTube Web في مربع «يوتيوب» المستقل.
            else -> null
        }
    }
}
