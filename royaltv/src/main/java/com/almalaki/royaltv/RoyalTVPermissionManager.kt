package com.almalaki.royaltv

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * ROYAL TV — Runtime Permission Manager
 *
 * مسؤول عن فحص وطلب أذونات الاتصال المحلي.
 * لا يطلب أذونات الإنترنت العادية وقت التشغيل.
 */
object RoyalTVPermissionManager {

    const val REQUEST_CODE = 39870

    /**
     * الأذونات الخطرة المطلوبة حسب إصدار Android.
     */
    fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            permissions.add(
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        } else {
            // Wi-Fi Direct وBluetooth discovery على الإصدارات الأقدم
            permissions.add(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            permissions.add(
                Manifest.permission.BLUETOOTH_SCAN
            )
            permissions.add(
                Manifest.permission.BLUETOOTH_CONNECT
            )
        }

        return permissions.toTypedArray()
    }

    /**
     * إرجاع الأذونات التي لم يمنحها المستخدم بعد.
     */
    fun getMissingPermissions(
        context: Context
    ): Array<String> {
        return getRequiredPermissions().filter { permission ->
            context.checkSelfPermission(permission) !=
                PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }

    /**
     * هل جميع الأذونات المطلوبة ممنوحة؟
     */
    fun hasAllRequiredPermissions(
        context: Context
    ): Boolean {
        return getMissingPermissions(context).isEmpty()
    }

    /**
     * طلب الأذونات الناقصة من المستخدم.
     *
     * يرجع true إذا أرسل طلبًا بالفعل،
     * وfalse إذا لم تكن هناك أذونات ناقصة.
     */
    fun requestMissingPermissions(
        activity: Activity,
        requestCode: Int = REQUEST_CODE
    ): Boolean {
        val missing = getMissingPermissions(activity)

        if (missing.isEmpty()) {
            return false
        }

        activity.requestPermissions(
            missing,
            requestCode
        )

        return true
    }

    /**
     * فحص نتيجة طلب الأذونات.
     * استدعِ هذه الدالة بعد وصول نتيجة الطلب.
     */
    fun arePermissionsGranted(
        context: Context,
        permissions: Array<String>,
        grantResults: IntArray
    ): Boolean {
        if (permissions.isEmpty() ||
            permissions.size != grantResults.size
        ) {
            return hasAllRequiredPermissions(context)
        }

        val allReturnedPermissionsGranted =
            grantResults.all {
                it == PackageManager.PERMISSION_GRANTED
            }

        return allReturnedPermissionsGranted &&
            hasAllRequiredPermissions(context)
    }

    /**
     * هل يحتاج إذن معين إلى شرح قبل إعادة الطلب؟
     */
    fun shouldShowRationale(
        activity: Activity,
        permission: String
    ): Boolean {
        return activity.shouldShowRequestPermissionRationale(
            permission
        )
    }
}
