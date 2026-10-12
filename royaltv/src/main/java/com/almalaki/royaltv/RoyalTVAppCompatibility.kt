package com.almalaki.royaltv

/**
 * يحدد التطبيقات التي نملك لها طريقة عرض ويب داخل نافذة ROYAL TV.
 *
 * لا يشغّل التطبيق الأصلي ولا يفتحه خارج ROYAL TV.
 * التطبيقات غير المدرجة تُعامل على أنها غير مدعومة
 * حتى نضيف لها طريقة عرض متوافقة.
 */
internal object RoyalTVAppCompatibility {

    /**
     * يعيد رابط العرض الداخلي إذا كان التطبيق مدعومًا،
     * أو null إذا لم تتوفر طريقة عرض داخلية.
     */
    fun resolveEmbeddedWebUrl(
        app: RoyalTVInstalledApp
    ): String? {
        return when (app.packageName) {

            // YouTube على الهواتف والأجهزة اللوحية.
            "com.google.android.youtube" ->
                YOUTUBE_TV_URL

            // YouTube المخصص لأجهزة التلفاز.
            "com.google.android.youtube.tv" ->
                YOUTUBE_TV_URL

            // لا نفتح التطبيقات الأصلية خارجيًا.
            else -> null
        }
    }

    private const val YOUTUBE_TV_URL =
        "https://www.youtube.com/tv"
}
