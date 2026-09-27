package com.almalaki.cafe

import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

private const val STORAGE_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val STORAGE_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

fun uploadProductImage(
    accessToken: String,
    bytes: ByteArray,
    mimeType: String
): String {

    val extension = when (mimeType.lowercase()) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"
    }

    val fileName =
        "${UUID.randomUUID()}.$extension"

    val url = URL(
        "$STORAGE_URL/storage/v1/object/product-images/$fileName"
    )

    val connection =
        url.openConnection() as HttpURLConnection

    connection.requestMethod = "POST"
    connection.doOutput = true

    connection.setRequestProperty(
        "apikey",
        STORAGE_KEY
    )

    connection.setRequestProperty(
        "Authorization",
        "Bearer $accessToken"
    )

    connection.setRequestProperty(
        "Content-Type",
        mimeType
    )

    connection.setRequestProperty(
        "x-upsert",
        "true"
    )

    connection.outputStream.use {
        it.write(bytes)
    }

    val code = connection.responseCode

    if (code !in 200..299) {

        val error =
            connection.errorStream
                ?.bufferedReader()
                ?.readText()
                ?: "فشل رفع الصورة"

        connection.disconnect()

        throw Exception(
            "HTTP $code: $error"
        )
    }

    connection.disconnect()

    return "$STORAGE_URL/storage/v1/object/public/product-images/$fileName"
}
