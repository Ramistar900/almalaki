package com.almalaki.cafe.royaltv

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

/**

* نوع الجهاز الذي يعمل عليه ROYAL TV.

* 

* هذا التصنيف لا يحدد دور الجهاز.

* 

* الدور يتم تحديده بواسطة:

* RoyalTVDeviceRoleManager

* 

* أما هذا الملف فيحدد نوع الجهاز وقدراته الأساسية.
  */
  enum class RoyalTVDeviceType {
  
  PHONE,
  
  TABLET,
  
  ANDROID_TV,
  
  TV_BOX,
  
  ANDROID_RECEIVER,
  
  UNKNOWN
  }

/**

* القدرات الأساسية لجهاز ROYAL TV.

* 

* هذه المعلومات محلية ولا تحتاج إلى الإنترنت.
  */
  data class RoyalTVDeviceCapabilities(
  
  val deviceType: RoyalTVDeviceType,
  
  /**
  
  * هل الجهاز مناسب للعرض الكبير؟
    */
    val supportsDisplay: Boolean,
  
  /**
  
  * هل الجهاز مناسب للتحكم؟
    */
    val supportsController: Boolean,
  
  /**
  
  * هل الجهاز يعمل بواجهة تلفزيون Android TV؟
    */
    val isAndroidTV: Boolean,
  
  /**
  
  * هل الجهاز يملك شاشة عرض مباشرة؟
    */
    val hasBuiltInDisplay: Boolean,
  
  /**
  
  * هل يمكن استخدامه كجهاز تحكم محلي؟
    */
    val supportsLocalControl: Boolean
    )

/**

* اكتشاف نوع وقدرات جهاز ROYAL TV.

* 

* مهم:

* 

* هذا الملف لا يتصل بالإنترنت.

* ولا يتصل بـ Supabase.

* ولا يبدأ Realtime.

* 

* وظيفته فقط معرفة إمكانيات الجهاز.
  */
  object RoyalTVDeviceCapabilitiesDetector {
  
  /**
  
  * اكتشاف قدرات الجهاز الحالي.
    */
    fun detect(
    context: Context
    ): RoyalTVDeviceCapabilities {
    
    val appContext =
    context.applicationContext
    
    val packageManager =
    appContext.packageManager
    
    val configuration =
    appContext.resources.configuration
    
    val isTelevision =
    packageManager.hasSystemFeature(
    PackageManager.FEATURE_LEANBACK
    )
    
    val isAndroidTV =
    isTelevision
    
    val isTablet =
    !isTelevision &&
    (
    configuration.screenLayout
    and Configuration.SCREENLAYOUT_SIZE_MASK
    ) >=
    Configuration.SCREENLAYOUT_SIZE_LARGE
    
    val deviceType =
    when {
    
         isAndroidTV ->
         detectAndroidTVFamily(
             packageManager
         )

     isTablet ->
         RoyalTVDeviceType.TABLET

     else ->
         RoyalTVDeviceType.PHONE
 }
    
    val supportsDisplay =
    when (deviceType) {
    
         RoyalTVDeviceType.ANDROID_TV,
     RoyalTVDeviceType.TV_BOX,
     RoyalTVDeviceType.ANDROID_RECEIVER ->
         true

     RoyalTVDeviceType.PHONE,
     RoyalTVDeviceType.TABLET,
     RoyalTVDeviceType.UNKNOWN ->
         false
 }
    
    val supportsController =
    true
    
    val hasBuiltInDisplay =
    when (deviceType) {
    
         RoyalTVDeviceType.ANDROID_TV,
     RoyalTVDeviceType.TV_BOX,
     RoyalTVDeviceType.ANDROID_RECEIVER,
     RoyalTVDeviceType.PHONE,
     RoyalTVDeviceType.TABLET ->
         true

     RoyalTVDeviceType.UNKNOWN ->
         false
 }
    
    return RoyalTVDeviceCapabilities(
    deviceType = deviceType,
    supportsDisplay = supportsDisplay,
    supportsController = supportsController,
    isAndroidTV = isAndroidTV,
    hasBuiltInDisplay = hasBuiltInDisplay,
    supportsLocalControl = true
    )
    }
  
  /**
  
  * محاولة تحديد عائلة جهاز Android TV.
  
  * 
  
  * Android لا يوفر API موحدًا يميز دائمًا
  
  * بين Android TV وTV Box وReceiver.
  
  * 
  
  * لذلك نبدأ بالتعرف على Android TV/Leanback،
  
  * ثم نستخدم معلومات النظام المتاحة كإشارات مساعدة.
  
  * 
  
  * لا نعتمد على اسم الشركة وحده.
    */
    private fun detectAndroidTVFamily(
    packageManager: PackageManager
    ): RoyalTVDeviceType {
    
    val hasLeanback =
    packageManager.hasSystemFeature(
    PackageManager.FEATURE_LEANBACK
    )
    
    if (hasLeanback) {
    return RoyalTVDeviceType.ANDROID_TV
    }
    
    return RoyalTVDeviceType.UNKNOWN
    }
  
  /**
  
  * هل الجهاز يستطيع تشغيل واجهة Display؟
    */
    fun supportsDisplay(
    context: Context
    ): Boolean {
    
    return detect(context)
    .supportsDisplay
    }
  
  /**
  
  * هل الجهاز يستطيع العمل كمتحكم؟
    */
    fun supportsController(
    context: Context
    ): Boolean {
    
    return detect(context)
    .supportsController
    }
  
  /**
  
  * هل الجهاز Android TV؟
    */
    fun isAndroidTV(
    context: Context
    ): Boolean {
    
    return detect(context)
    .isAndroidTV
    }
  
  /**
  
  * هل الجهاز هاتف؟
    */
    fun isPhone(
    context: Context
    ): Boolean {
    
    return detect(context)
    .deviceType ==
    RoyalTVDeviceType.PHONE
    }
  
  /**
  
  * هل الجهاز تابلت؟
    */
    fun isTablet(
    context: Context
    ): Boolean {
    
    return detect(context)
    .deviceType ==
    RoyalTVDeviceType.TABLET
    }
  
  /**
  
  * الحصول على نوع الجهاز فقط.
    */
    fun getDeviceType(
    context: Context
    ): RoyalTVDeviceType {
    
    return detect(context)
    .deviceType
    }
  }
