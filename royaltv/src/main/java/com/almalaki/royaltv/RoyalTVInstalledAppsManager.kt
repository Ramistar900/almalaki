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
     * اكتشاف التطبيقات التي تملك نقطة تشغيل فعلية.
     *
     * يدعم مشغلات Android العادية ومشغلات Android TV
     * (Leanback)، ويتجنب عرض حزم النظام التي لا تملك
     * واجهة تشغيل عامة.
     *
     * هذه الدالة لا تشغّل أي تطبيق.
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

                    val standardLaunchIntent =
                        packageManager.getLaunchIntentForPackage(
                            packageName
                        )

                    val tvLaunchIntent =
                        packageManager.getLeanbackLaunchIntentForPackage(
                            packageName
                        )

                    // لا نعرض الحزم التي لا تملك أي مدخل تشغيل.
                    if (
                        standardLaunchIntent == null &&
                        tvLaunchIntent == null
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

    /**
     * محاولة فتح التطبيق عند طلب صريح من المستخدم.
     *
     * لا تستخدم هذه الدالة لتضمين التطبيق داخل ROYAL TV؛
     * فهي تنقل التطبيق إلى واجهة Android الخارجية.
     */
    fun launchApp(
        context: Context,
        packageName: String
    ): Boolean {
        if (packageName.isBlank()) return false

        return try {
            val packageManager = context.packageManager

            val intent =
                packageManager.getLaunchIntentForPackage(packageName)
                    ?: packageManager.getLeanbackLaunchIntentForPackage(
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
