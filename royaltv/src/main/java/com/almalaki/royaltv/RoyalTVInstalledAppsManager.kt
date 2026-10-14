package com.almalaki.royaltv

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class RoyalTVInstalledApp(
    val packageName: String,
    val label: String
)

object RoyalTVInstalledAppsManager {

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

                    val standardIntent =
                        packageManager.getLaunchIntentForPackage(
                            packageName
                        )

                    val tvIntent =
                        packageManager.getLeanbackLaunchIntentForPackage(
                            packageName
                        )

                    if (
                        standardIntent == null &&
                        tvIntent == null
                    ) {
                        return@mapNotNull null
                    }

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

    fun launchApp(
        context: Context,
        packageName: String
    ): Boolean {
        if (packageName.isBlank()) return false

        return try {
            val packageManager = context.packageManager

            // نفضّل واجهة التلفاز إذا كان التطبيق يوفرها.
            val intent =
                packageManager.getLeanbackLaunchIntentForPackage(
                    packageName
                )
                    ?: packageManager.getLaunchIntentForPackage(
                        packageName
                    )
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
