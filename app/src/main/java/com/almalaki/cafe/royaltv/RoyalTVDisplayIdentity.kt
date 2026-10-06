package com.almalaki.cafe.royaltv

import android.content.Context

/**

* الهوية الظاهرة لشاشة ROYAL TV.

* 

* كل جهاز Display يحصل على رقم ثابت مثل:

* 

* ROYAL-TV-001

* ROYAL-TV-002

* ROYAL-TV-003

* 

* الرقم محفوظ محليًا ولا يعتمد على الإنترنت.
  */
  object RoyalTVDisplayIdentity {
  
  private const val PREFS_NAME =
  "royal_tv_display_identity"
  
  private const val KEY_DISPLAY_CODE =
  "display_code"
  
  private const val KEY_DISPLAY_NUMBER =
  "display_number"
  
  /**
  
  * الحصول على رقم الشاشة المحلي.
  
  * 
  
  * يتم إنشاؤه مرة واحدة فقط على الجهاز.
    */
    fun getDisplayNumber(
    context: Context
    ): Int {
    
    val prefs =
    context.applicationContext
    .getSharedPreferences(
    PREFS_NAME,
    Context.MODE_PRIVATE
    )
    
    val savedNumber =
    prefs.getInt(
    KEY_DISPLAY_NUMBER,
    0
    )
    
    if (savedNumber > 0) {
    return savedNumber
    }
    
    /*
    
    * في هذه المرحلة نستخدم رقمًا محليًا
    * مشتقًا من الهوية التقنية للجهاز.
    * 
    * لاحقًا، عند إضافة ROYAL TV Core،
    * سيصبح Core هو المسؤول عن تخصيص
    * أرقام 001 / 002 / 003 بشكل مركزي
    * لمنع التكرار بين الأجهزة.
      */
      val deviceId =
      RoyalTVDeviceIdentity.getDeviceId(
      context
      )
    
    val numericPart =
    deviceId
    .filter { it.isDigit() }
    
    val generatedNumber =
    if (numericPart.isNotEmpty()) {
    numericPart
    .takeLast(6)
    .toIntOrNull()
    ?: 1
    } else {
    1
    }
    
    val safeNumber =
    generatedNumber
    .coerceAtLeast(1)
    
    prefs.edit()
    .putInt(
    KEY_DISPLAY_NUMBER,
    safeNumber
    )
    .apply()
    
    return safeNumber
    }
  
  /**
  
  * الحصول على الرمز الظاهر للشاشة.
  
  * 
  
  * مثال:
  
  * ROYAL-TV-001
    */
    fun getDisplayCode(
    context: Context
    ): String {
    
    val prefs =
    context.applicationContext
    .getSharedPreferences(
    PREFS_NAME,
    Context.MODE_PRIVATE
    )
    
    val savedCode =
    prefs.getString(
    KEY_DISPLAY_CODE,
    null
    )
    
    if (!savedCode.isNullOrBlank()) {
    return savedCode
    }
    
    val number =
    getDisplayNumber(context)
    
    val code =
    "ROYAL-TV-" +
    number
    .toString()
    .padStart(
    3,
    '0'
    )
    
    prefs.edit()
    .putString(
    KEY_DISPLAY_CODE,
    code
    )
    .apply()
    
    return code
    }
  
  /**
  
  * هل تم إنشاء هوية Display؟
    */
    fun hasDisplayIdentity(
    context: Context
    ): Boolean {
    
    val prefs =
    context.applicationContext
    .getSharedPreferences(
    PREFS_NAME,
    Context.MODE_PRIVATE
    )
    
    return prefs.contains(
    KEY_DISPLAY_CODE
    )
    }
  
  /**
  
  * حذف الهوية المحلية.
  
  * 
  
  * سنستخدمها لاحقًا فقط في حالة
  
  * إعادة تسجيل الشاشة أو إعادة ربطها
  
  * بـ ROYAL TV Core.
    */
    fun reset(
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
