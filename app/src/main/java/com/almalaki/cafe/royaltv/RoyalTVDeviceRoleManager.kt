package com.almalaki.cafe.royaltv

import android.content.Context

/**

* مدير دور جهاز ROYAL TV.

* 

* يسمح للجهاز بالعمل كـ:

* 

* CONTROLLER

* DISPLAY

* CONTROLLER_AND_DISPLAY

* 

* الدور محفوظ محليًا ولا يحتاج إلى الإنترنت.

* 

* هذا مهم لأن Android TV / TV Box / Receiver

* يمكن أن يعمل كشاشة عرض أو كمركز تحكم أو الاثنين معًا.
  */
  object RoyalTVDeviceRoleManager {
  
  private const val PREFS_NAME =
  "royal_tv_device_role"
  
  private const val KEY_ROLE =
  "device_role"
  
  /**
  
  * الحصول على الدور الحالي للجهاز.
  
  * 
  
  * إذا لم يتم تحديد دور سابقًا،
  
  * نستخدم CONTROLLER_AND_DISPLAY
  
  * كخيار افتراضي مرن.
    */
    fun getRole(
    context: Context
    ): RoyalTVDeviceRole {
    
    val prefs =
    context.applicationContext
    .getSharedPreferences(
    PREFS_NAME,
    Context.MODE_PRIVATE
    )
    
    val savedRole =
    prefs.getString(
    KEY_ROLE,
    null
    )
    
    return try {
    if (savedRole.isNullOrBlank()) {
    RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY
    } else {
    RoyalTVDeviceRole.valueOf(
    savedRole
    )
    }
    } catch (_: Exception) {
    RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY
    }
    }
  
  /**
  
  * تغيير دور الجهاز.
  
  * 
  
  * التغيير محلي فقط.
    */
    fun setRole(
    context: Context,
    role: RoyalTVDeviceRole
    ) {
    
    context.applicationContext
    .getSharedPreferences(
    PREFS_NAME,
    Context.MODE_PRIVATE
    )
    .edit()
    .putString(
    KEY_ROLE,
    role.name
    )
    .apply()
    }
  
  /**
  
  * هل الجهاز يستطيع عرض المحتوى؟
    */
    fun canDisplay(
    context: Context
    ): Boolean {
    
    return when (getRole(context)) {
    
     RoyalTVDeviceRole.DISPLAY,
 RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY -> true

 RoyalTVDeviceRole.CONTROLLER -> false
    
    }
    }
  
  /**
  
  * هل الجهاز يستطيع التحكم؟
    */
    fun canControl(
    context: Context
    ): Boolean {
    
    return when (getRole(context)) {
    
     RoyalTVDeviceRole.CONTROLLER,
 RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY -> true

 RoyalTVDeviceRole.DISPLAY -> false
    
    }
    }
  
  /**
  
  * هل الجهاز يعمل كشاشة فقط؟
    */
    fun isDisplayOnly(
    context: Context
    ): Boolean {
    
    return getRole(context) ==
    RoyalTVDeviceRole.DISPLAY
    }
  
  /**
  
  * هل الجهاز يعمل كمتحكم فقط؟
    */
    fun isControllerOnly(
    context: Context
    ): Boolean {
    
    return getRole(context) ==
    RoyalTVDeviceRole.CONTROLLER
    }
  
  /**
  
  * هل الجهاز يعمل كـ Controller + Display؟
    */
    fun isControllerAndDisplay(
    context: Context
    ): Boolean {
    
    return getRole(context) ==
    RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY
    }
  
  /**
  
  * إعادة الدور إلى الوضع المرن الافتراضي.
    */
    fun resetRole(
    context: Context
    ) {
    
    context.applicationContext
    .getSharedPreferences(
    PREFS_NAME,
    Context.MODE_PRIVATE
    )
    .edit()
    .remove(KEY_ROLE)
    .apply()
    }
  }
