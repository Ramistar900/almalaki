
package com.almalaki.royaltv

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

object RoyalTVUpdateChecker {

    private const val TAG = "RoyalTVUpdateChecker"

    private const val VERSION_URL =
        "https://raw.githubusercontent.com/Ramistar900/almalaki/main/updates/versions.json"

    fun check(activity: Activity) {
        Thread {
            try {
                val connection =
                    URL(VERSION_URL).openConnection() as HttpURLConnection

                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.requestMethod = "GET"

                try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        return@Thread
                    }

                    val response = connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                    val root = JSONObject(response)

                    val appKey = when (activity.packageName) {
                        "com.almalaki.royaltv" -> "royaltv"
                        "com.almalaki.cafe" -> "almalaki"
                        else -> return@Thread
                    }

                    val remote = root.optJSONObject(appKey)
                        ?: return@Thread

                    val packageInfo =
                        activity.packageManager.getPackageInfo(
                            activity.packageName,
                            0
                        )

                    val currentVersionCode =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            packageInfo.longVersionCode
                        } else {
                            @Suppress("DEPRECATION")
                            packageInfo.versionCode.toLong()
                        }

                    val latestVersionCode =
                        remote.optLong("versionCode", 0L)

                    val latestVersionName =
                        remote.optString("versionName", "")

                    val githubDownloadUrl =
                        remote.optString("downloadUrl", "").trim()

                    val mediaFireDownloadUrl =
                        remote.optString("mediaFireDownloadUrl", "").trim()

                    val releaseNotes =
                        remote.optString("releaseNotes", "")

                    val githubUri = Uri.parse(githubDownloadUrl)
                    val mediaFireUri = Uri.parse(mediaFireDownloadUrl)

                    val hasGitHubUrl =
                        githubDownloadUrl.startsWith("https://") &&
                            githubUri.host == "github.com"

                    val mediaFireHost =
                        mediaFireUri.host.orEmpty()

                    val hasMediaFireUrl =
                        mediaFireDownloadUrl.startsWith("https://") &&
                            (
                                mediaFireHost == "mediafire.com" ||
                                    mediaFireHost.endsWith(".mediafire.com")
                            )

                    if (
                        latestVersionCode > currentVersionCode &&
                        (hasGitHubUrl || hasMediaFireUrl)
                    ) {
                        activity.runOnUiThread {
                            if (
                                activity.isFinishing ||
                                (
                                    Build.VERSION.SDK_INT >=
                                        Build.VERSION_CODES.JELLY_BEAN_MR1 &&
                                        activity.isDestroyed
                                )
                            ) {
                                return@runOnUiThread
                            }

                            val sources = mutableListOf<String>()
                            val urls = mutableListOf<String>()

                            if (hasGitHubUrl) {
                                sources.add("GitHub — المصدر الرسمي")
                                urls.add(githubDownloadUrl)
                            }

                            if (hasMediaFireUrl) {
                                sources.add("MediaFire — مصدر بديل")
                                urls.add(mediaFireDownloadUrl)
                            }

                            AlertDialog.Builder(activity)
                                .setTitle("يتوفر تحديث جديد")
                                .setMessage(
                                    "الإصدار: $latestVersionName\n\n" +
                                        releaseNotes +
                                        "\n\nاختر مصدر تنزيل التحديث:"
                                )
                                .setNegativeButton("لاحقًا", null)
                                .setItems(sources.toTypedArray()) { _, which ->
                                    try {
                                        activity.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(urls[which])
                                            )
                                        )
                                    } catch (error: Exception) {
                                        Log.e(
                                            TAG,
                                            "تعذر فتح رابط التحديث",
                                            error
                                        )
                                    }
                                }
                                .show()
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (error: Exception) {
                Log.w(TAG, "تعذر التحقق من التحديثات", error)
            }
        }.start()
    }
}
