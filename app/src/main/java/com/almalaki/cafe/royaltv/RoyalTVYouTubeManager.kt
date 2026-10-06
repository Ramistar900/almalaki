package com.almalaki.cafe.royaltv

import android.net.Uri
import org.json.JSONObject

/**
 * مدير YouTube الخاص بـ ROYAL TV.
 *
 * مهم:
 * هذا الملف لا يفتح تطبيق YouTube الخارجي.
 *
 * وظيفته تجهيز وفك أوامر YouTube التي ستعرض لاحقًا
 * داخل مساحة محتوى ROYAL TV نفسها.
 */
object RoyalTVYouTubeManager {

    enum class Action {
        OPEN_HOME,
        OPEN_SEARCH,
        OPEN_VIDEO,
        CLOSE
    }

    data class Request(
        val action: Action,
        val url: String = "",
        val query: String = ""
    )

    /**
     * إنشاء طلب فتح الصفحة الرئيسية لـ YouTube
     * داخل مساحة عرض ROYAL TV.
     */
    fun openHome(): RoyalTVCommand {
        return createCommand(
            Request(
                action = Action.OPEN_HOME
            )
        )
    }

    /**
     * إنشاء طلب بحث في YouTube.
     */
    fun search(
        query: String
    ): RoyalTVCommand {

        val cleanQuery =
            query.trim()

        return createCommand(
            Request(
                action = Action.OPEN_SEARCH,
                query = cleanQuery
            )
        )
    }

    /**
     * إنشاء طلب تشغيل فيديو YouTube
     * داخل مساحة العرض.
     */
    fun openVideo(
        url: String
    ): RoyalTVCommand {

        val cleanUrl =
            url.trim()

        return createCommand(
            Request(
                action = Action.OPEN_VIDEO,
                url = cleanUrl
            )
        )
    }

    /**
     * إنشاء أمر إغلاق طبقة YouTube
     * والعودة إلى محتوى ROYAL TV.
     */
    fun close(): RoyalTVCommand {
        return createCommand(
            Request(
                action = Action.CLOSE
            )
        )
    }

    /**
     * تحويل الطلب إلى Payload آمن.
     *
     * مثال:
     * {
     *   "action": "OPEN_VIDEO",
     *   "url": "...",
     *   "query": ""
     * }
     */
    fun encode(
        request: Request
    ): String {

        return JSONObject().apply {

            put(
                "action",
                request.action.name
            )

            put(
                "url",
                request.url
            )

            put(
                "query",
                request.query
            )

        }.toString()
    }

    /**
     * قراءة Payload القادم من ROYAL TV Core.
     */
    fun decode(
        payload: String
    ): Request? {

        val cleanPayload =
            payload.trim()

        if (cleanPayload.isBlank()) {
            return null
        }

        return try {

            val json =
                JSONObject(
                    cleanPayload
                )

            val actionName =
                json.optString(
                    "action",
                    ""
                )

            val action =
                try {
                    Action.valueOf(
                        actionName
                    )
                } catch (_: Exception) {
                    return null
                }

            Request(
                action = action,
                url =
                    json.optString(
                        "url",
                        ""
                    ),
                query =
                    json.optString(
                        "query",
                        ""
                    )
            )

        } catch (_: Exception) {

            null
        }
    }

    /**
     * إنشاء أمر ROYAL TV من طلب YouTube.
     */
    private fun createCommand(
        request: Request
    ): RoyalTVCommand {

        return RoyalTVCommand(
            type =
                RoyalTVCommandType.SHOW_YOUTUBE,
            payload =
                encode(request)
        )
    }

    /**
     * تحويل نص البحث إلى رابط YouTube.
     *
     * لا يتم فتح الرابط هنا.
     * فقط يتم تجهيز الرابط ليستخدمه
     * مكوّن عرض YouTube داخل ROYAL TV.
     */
    fun buildSearchUrl(
        query: String
    ): String {

        val cleanQuery =
            query.trim()

        if (cleanQuery.isBlank()) {
            return "https://www.youtube.com/"
        }

        return Uri.Builder()
            .scheme("https")
            .authority("www.youtube.com")
            .appendPath("results")
            .appendQueryParameter(
                "search_query",
                cleanQuery
            )
            .build()
            .toString()
    }

    /**
     * تنظيف رابط YouTube قبل عرضه.
     */
    fun normalizeVideoUrl(
        url: String
    ): String {

        return url.trim()
    }
}
