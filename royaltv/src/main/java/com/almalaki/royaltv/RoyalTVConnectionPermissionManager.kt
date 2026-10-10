
package com.almalaki.royaltv

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build

/**
 * مدير أذونات الاتصال في مشروع ROYAL TV المستقل.
 */
object RoyalTVConnectionPermissionManager {

    const val REQUEST_CODE = 39871

    /**
     * تحديد الأذونات المطلوبة حسب إصدار Android.
     */
    fun requiredPermissions(): Array<String> {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        return permissions.toTypedArray()
    }

    /**
     * إرجاع الأذونات التي لم يمنحها المستخدم.
     */
    fun missingPermissions(context: Context): Array<String> {
        return requiredPermissions().filter { permission ->
            context.checkSelfPermission(permission) !=
                PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }

    /**
     * التحقق من منح جميع الأذونات.
     */
    fun hasAllPermissions(context: Context): Boolean {
        return missingPermissions(context).isEmpty()
    }

    /**
     * طلب الأذونات الناقصة من المستخدم.
     * تعيد true عند بدء طلب الأذونات.
     */
    fun requestMissingPermissions(activity: Activity): Boolean {
        val missing = missingPermissions(activity)

        if (missing.isEmpty()) {
            return false
        }

        activity.requestPermissions(missing, REQUEST_CODE)
        return true
    }

    /**
     * العثور على Activity انطلاقًا من Context.
     */
    fun findActivity(context: Context): Activity? {
        var current: Context = context

        while (current is ContextWrapper) {
            if (current is Activity) {
                return current
            }

            val next = current.baseContext

            if (next === current) {
                return null
            }

            current = next
        }

        return current as? Activity
    }
}
