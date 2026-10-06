package com.almalaki.cafe.royaltv

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

/**

* حالة اتصال ROYAL TV بالشبكة.

* 

* هذه الحالة تصف توفر الشبكة فقط.

* 

* لا تعني أن Supabase Realtime متصل بالفعل.

* سيتم ربطها لاحقًا مع RoyalTVRealtime.
  */
  enum class RoyalTVNetworkState {
  
  /**
  
  * توجد شبكة يمكن استخدامها.
    */
    ONLINE,
  
  /**
  
  * لا توجد شبكة متاحة.
    */
    OFFLINE
    }

/**

* مراقب شبكة ROYAL TV.

* 

* مهم:

* 

* هذا الملف لا يتحكم في تشغيل شاشة العرض.

* 

* عند OFFLINE:

* الشاشة تستمر بالعمل محليًا.

* 

* وعند ONLINE:

* سنستخدم الإشعار لاحقًا لإعادة

* الاتصال بـ ROYAL TV Core تلقائيًا.
  */
  class RoyalTVNetworkMonitor(
  context: Context
  ) {
  
  private val appContext =
  context.applicationContext
  
  private val connectivityManager =
  appContext.getSystemService(
  Context.CONNECTIVITY_SERVICE
  ) as ConnectivityManager
  
  private var callback:
  ConnectivityManager.NetworkCallback? = null
  
  private var currentState =
  RoyalTVNetworkState.OFFLINE
  
  /**
  
  * الحالة الحالية للشبكة.
    */
    fun getCurrentState():
    RoyalTVNetworkState {
    
    return currentState
    }
  
  /**
  
  * بدء مراقبة الشبكة.
  
  * 
  
  * إذا كانت الشبكة متاحة بالفعل،
  
  * سيتم إرسال ONLINE مباشرة.
    */
    fun start(
    onStateChanged:
    (RoyalTVNetworkState) -> Unit
    ) {
    
    stop()
    
    currentState =
    detectCurrentState()
    
    onStateChanged(currentState)
    
    val networkCallback =
    object :
    ConnectivityManager.NetworkCallback() {
    
         override fun onAvailable(
         network: Network
     ) {

         updateState(
             RoyalTVNetworkState.ONLINE,
             onStateChanged
         )
     }

     override fun onLost(
         network: Network
     ) {

         /*
          * لا نفترض أن فقدان شبكة واحدة
          * يعني فقدان الاتصال بالكامل.
          *
          * نعيد الفحص الحقيقي للشبكات المتاحة.
          */
         val newState =
             detectCurrentState()

         updateState(
             newState,
             onStateChanged
         )
     }

     override fun onUnavailable() {

         updateState(
             RoyalTVNetworkState.OFFLINE,
             onStateChanged
         )
     }
 }
    
    callback =
    networkCallback
    
    val request =
    NetworkRequest.Builder()
    .addCapability(
    NetworkCapabilities
    .NET_CAPABILITY_INTERNET
    )
    .build()
    
    try {
    
     connectivityManager
     .registerNetworkCallback(
         request,
         networkCallback
     )
    
    } catch (e: Exception) {
    
     android.util.Log.e(
     "RoyalTVNetwork",
     "Unable to register network callback: ${e.message}",
     e
 )

 updateState(
     RoyalTVNetworkState.OFFLINE,
     onStateChanged
 )
    
    }
    }
  
  /**
  
  * إيقاف مراقبة الشبكة.
  
  * 
  
  * هذا لا يوقف تشغيل ROYAL TV.
    */
    fun stop() {
    
    val currentCallback =
    callback
    
    callback = null
    
    if (currentCallback != null) {
    
     try {

     connectivityManager
         .unregisterNetworkCallback(
             currentCallback
         )

 } catch (_: Exception) {
     // تم إلغاء التسجيل مسبقًا.
 }
    
    }
    }
  
  /**
  
  * فحص الحالة الحالية للشبكة.
    */
    private fun detectCurrentState():
    RoyalTVNetworkState {
    
    return try {
    
     val activeNetwork =
     connectivityManager
         .activeNetwork

 if (activeNetwork == null) {

     RoyalTVNetworkState.OFFLINE

 } else {

     val capabilities =
         connectivityManager
             .getNetworkCapabilities(
                 activeNetwork
             )

     val hasInternetCapability =
         capabilities?.hasCapability(
             NetworkCapabilities
                 .NET_CAPABILITY_INTERNET
         ) == true

     if (hasInternetCapability) {
         RoyalTVNetworkState.ONLINE
     } else {
         RoyalTVNetworkState.OFFLINE
     }
 }
    
    } catch (e: Exception) {
    
     android.util.Log.e(
     "RoyalTVNetwork",
     "Network check error: ${e.message}",
     e
 )

 RoyalTVNetworkState.OFFLINE
    
    }
    }
  
  /**
  
  * تحديث الحالة فقط عند حدوث تغيير حقيقي.
    */
    private fun updateState(
    newState: RoyalTVNetworkState,
    onStateChanged:
    (RoyalTVNetworkState) -> Unit
    ) {
    
    if (currentState == newState) {
    return
    }
    
    currentState =
    newState
    
    onStateChanged(newState)
    }
  }
