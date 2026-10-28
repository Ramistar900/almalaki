package com.almalaki.cafe

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class CustomerSection(
    val sectionKey: String,
    val title: String,
    val categoryNames: List<String>,
    val enabled: Boolean,
    val sortOrder: Int
)

private const val CUSTOMER_SECTIONS_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val CUSTOMER_SECTIONS_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

private fun customerSectionsRequest(
    method: String,
    path: String,
    accessToken: String? = null,
    body: String? = null,
    prefer: String? = null
): String {
    val connection = URL(
        "$CUSTOMER_SECTIONS_URL/rest/v1/$path"
    ).openConnection() as HttpURLConnection

    try {
        connection.requestMethod = method
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.doOutput = body != null

        connection.setRequestProperty(
            "apikey",
            CUSTOMER_SECTIONS_KEY
        )

        connection.setRequestProperty(
            "Authorization",
            "Bearer ${accessToken ?: CUSTOMER_SECTIONS_KEY}"
        )

        connection.setRequestProperty(
            "Content-Type",
            "application/json"
        )

        if (prefer != null) {
            connection.setRequestProperty(
                "Prefer",
                prefer
            )
        }

        if (body != null) {
            connection.outputStream.use {
                it.write(body.toByteArray(Charsets.UTF_8))
            }
        }

        val code = connection.responseCode

        val response = (
            if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
        )?.bufferedReader()?.use {
            it.readText()
        }.orEmpty()

        if (code !in 200..299) {
            throw Exception(
                "تعذر التعامل مع الأقسام (HTTP $code): " +
                    response.ifBlank {
                        "خطأ غير معروف"
                    }
            )
        }

        return response
    } finally {
        connection.disconnect()
    }
}

/** تحميل إعدادات الأقسام المشتركة من Supabase. */
fun loadCustomerSections(
    restaurantId: String? = null,
    restaurantCode: String? = null,
    accessToken: String? = null
): List<CustomerSection> {
    val useCodeLookup = !restaurantCode.isNullOrBlank()
    val response = customerSectionsRequest(
        method = if (useCodeLookup) "POST" else "GET",
        path = if (useCodeLookup) {
            "rpc/get_restaurant_sections"
        } else {
            "customer_sections" +
                "?select=section_key,title,category_names,enabled,sort_order" +
                "&order=sort_order.asc" +
                (restaurantId?.let {
                    "&restaurant_id=eq.${URLEncoder.encode(it, "UTF-8")}"
                } ?: "")
        },
        accessToken = accessToken,
        body = if (useCodeLookup) {
            JSONObject().put("p_code", restaurantCode).toString()
        } else {
            null
        }
    )

    val json = JSONArray(response)
    val result = mutableListOf<CustomerSection>()

    for (i in 0 until json.length()) {
        val item = json.getJSONObject(i)
        val names = item.optJSONArray("category_names")
            ?: JSONArray()

        val categories = buildList {
            for (j in 0 until names.length()) {
                val name = names.optString(j).trim()
                if (name.isNotEmpty()) {
                    add(name)
                }
            }
        }

        result.add(
            CustomerSection(
                sectionKey = item.getString("section_key"),
                title = item.getString("title"),
                categoryNames = categories,
                enabled = item.optBoolean("enabled", true),
                sortOrder = item.optInt("sort_order", 0)
            )
        )
    }

    return result
}

/** إضافة قسم أو تحديث إعداداته باستخدام جلسة المالك. */
fun saveCustomerSection(
    accessToken: String,
    section: CustomerSection,
    restaurantId: String? = null
) {
    require(accessToken.isNotBlank()) {
        "جلسة المالك غير متوفرة."
    }

    val categories = JSONArray()

    section.categoryNames.forEach {
        val name = it.trim()
        if (name.isNotEmpty()) {
            categories.put(name)
        }
    }

    val body = JSONObject().apply {
        put("section_key", section.sectionKey.trim())
        put("title", section.title.trim())
        put("category_names", categories)
        put("enabled", section.enabled)
        put("sort_order", section.sortOrder)
        if (!restaurantId.isNullOrBlank()) {
            put("restaurant_id", restaurantId)
        }
    }.toString()

    customerSectionsRequest(
        method = "POST",
        path = "customer_sections?on_conflict=section_key",
        accessToken = accessToken,
        body = body,
        prefer = "resolution=merge-duplicates,return=minimal"
    )
}

/** إظهار القسم أو إخفاؤه دون حذف المنتجات أو صورها. */
fun setCustomerSectionEnabled(
    accessToken: String,
    sectionKey: String,
    enabled: Boolean,
    restaurantId: String? = null
) {
    require(accessToken.isNotBlank()) {
        "جلسة المالك غير متوفرة."
    }

    val encodedKey = URLEncoder.encode(
        sectionKey,
        "UTF-8"
    )

    val body = JSONObject()
        .put("enabled", enabled)
        .toString()

    customerSectionsRequest(
        method = "PATCH",
        path = "customer_sections?section_key=eq.$encodedKey" +
            (restaurantId?.let {
                "&restaurant_id=eq.${URLEncoder.encode(it, "UTF-8")}"
            } ?: ""),
        accessToken = accessToken,
        body = body,
        prefer = "return=minimal"
    )
}
