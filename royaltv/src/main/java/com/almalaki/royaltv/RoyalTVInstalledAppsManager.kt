
package com.almalaki.royaltv

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * بيانات تطبيق مثبت يمكن تشغيله من جهاز ROYAL TV.
 */
data class RoyalTVInstalledApp(
    val packageName: String,
    val label: String
)

object RoyalTVInstalledAppsManager {

    /**
     * اكتشاف التطبيقات التي تملك أيقونة تشغيل.
     */
    fun getLaunchableApps(
        context: Context
    ): List<RoyalTVInstalledApp> {
        val packageManager = context.packageManager

        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return try {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.MATCH_ALL
            )
                .mapNotNull { info ->
                    val activity = info.activityInfo
                        ?: return@mapNotNull null

                    val packageName = activity.packageName
                    if (packageName.isBlank()) {
                        return@mapNotNull null
                    }

                    val label = info
                        .loadLabel(packageManager)
                        .toString()
                        .trim()
                        .ifBlank { packageName }

                    RoyalTVInstalledApp(
                        packageName = packageName,
                        label = label
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy {
                    it.label.lowercase()
                }
        } catch (_: RuntimeException) {
            emptyList()
        }
    }

    /**
     * فتح تطبيق على الجهاز الحالي.
     * true تعني أن أمر التشغيل أُرسل بنجاح.
     */
    fun launchApp(
        context: Context,
        packageName: String
    ): Boolean {
        if (packageName.isBlank()) return false

        return try {
            val intent = context.packageManager
                .getLaunchIntentForPackage(packageName)
                ?: return false

            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
