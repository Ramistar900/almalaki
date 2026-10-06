package com.almalaki.cafe.royaltv

import android.content.Context

/**

* الهوية الدائمة لجهاز ROYAL TV.

* 

* يتم إنشاء Device ID مرة واحدة فقط،

* ثم حفظه محليًا على الجهاز.

* 

* لا يعتمد على الإنترنت أو Supabase.

* 

* لذلك يبقى نفس الجهاز معروفًا حتى بعد:

* - إعادة تشغيل الجهاز.

* - إغلاق التطبيق.

* - انقطاع الإنترنت.

* - إعادة الاتصال.
    */
    object RoyalTVDeviceIdentity {
  
  private const val PREFS_NAME =
  "royal_tv_device_identity"
  
  private const val KEY_DEVICE_ID =
  "device_id"
  
  private const val KEY_DEVICE_NAME =
  "device_name"
  
  /**
  
  * الحصول على Device ID الدائم.
  
  * 
  
  * إذا كان الجهاز قد حصل على هوية سابقًا،
  
  * نعيد نفس الهوية.
  
  * 
  
  * وإذا كانت هذه أول مرة،
  
  * ننشئ هوية جديدة ونحفظها محليًا.
    */
    fun getDeviceId(
    context: Context
    ): String {
    
    val prefs =
    context.applicationContext
    .getSharedPreferences(
    PREFS_NAME,
    Context.MODE_PRIVATE
    )
    
    val savedId =
    prefs.getString(
    KEY_DEVICE_ID,
    null
    )
    
    if (!savedId.isNullOrBlank()) {
    return savedId
    }
    
    val newId =
    "ROYAL-TV-" +
    java.util.UUID.randomUUID()
    .toString()
    .replace("-", "")
    .take(12)
    .uppercase()
    
    prefs.edit()
    .putString(
    KEY_DEVICE_ID,
    newId
    )
    .apply()
    
    return newId
    }
  
  /**
  
  * الحصول على اسم الجهاز.
  
  * 
  
  * الاسم الافتراضي يستخدم Device ID
  
  * إلى أن نضيف شاشة تسمح للمالك
  
  * بتسمية الجهاز مثل:
  
  * 
  
  * ROYAL TV الرئيسية
  
  * ROYAL TV صالة الزبائن
  
  * ROYAL TV 2
    */
    fun getDeviceName(
    context: Context
    ): String {
    
    val prefs =
    context.applicationContext
    .getSharedPreferences(
    PREFS_NAME,
    Context.MODE_PRIVATE
    )
    
    val savedName =
    prefs.getString(
    KEY_DEVICE_NAME,
    null
    )
    
    if (!savedName.isNullOrBlank()) {
    return savedName
    }
    
    return getDeviceId(context)
    }
  
  /**
  
  * تغيير اسم الجهاز محليًا.
  
  * 
  
  * لا يغير Device ID.
    */
    fun setDeviceName(
    context: Context,
    name: String
    ) {
    
    val cleanName =
    name.trim()
    
    if (cleanName.isBlank()) {
    return
    }
    
    context.applicationContext
    .getSharedPreferences(
    PREFS_NAME,
    Context.MODE_PRIVATE
    )
    .edit()
    .putString(
    KEY_DEVICE_NAME,
    cleanName
    )
    .apply()
    }
  
  /**
  
  * حذف الهوية المحلية.
  
  * 
  
  * هذه الوظيفة سنستخدمها لاحقًا فقط
  
  * عند وجود خيار "إعادة ربط الجهاز"
  
  * أو "إعادة ضبط هوية ROYAL TV".
    */
    fun resetIdentity(
    context: Context
    ) {
    
    context.applicationContext
    .getSharedPreferences(
    PREFS_NAME,
    Context.MODE_PRIVATE
    )
    .edit()
    .clear()
    .apply()
    }
    }
