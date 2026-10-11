package com.almalaki.royaltv

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * بيانات تطبيق مثبت على الجهاز.
 */
data class RoyalTVInstalledApp(
    val packageName: String,
    val label: String
)

object RoyalTVInstalledAppsManager {

    /**
     * اكتشاف التطبيقات المثبتة، بما فيها تطبيقات النظام،
     * وليس التطبيقات التي تملك أيقونة تشغيل فقط.
     */
    fun getLaunchableApps(
        context: Context
    ): List<RoyalTVInstalledApp> {
        val packageManager = context.packageManager

        return try {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(
                PackageManager.MATCH_ALL
            )
                .mapNotNull { application ->
                    val packageName = application.packageName
                        .takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null

                    val label = packageManager
                        .getApplicationLabel(application)
                        .toString()
                        .trim()
                        .ifBlank { packageName }

                    RoyalTVInstalledApp(
                        packageName = packageName,
                        label = label
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
        } catch (_: RuntimeException) {
            emptyList()
        }
    }

    /**
     * محاولة فتح التطبيق.
     * بعض تطبيقات النظام لا تملك واجهة تشغيل عامة.
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
