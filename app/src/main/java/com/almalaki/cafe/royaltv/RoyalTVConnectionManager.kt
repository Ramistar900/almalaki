package com.almalaki.cafe.royaltv

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

/**

* مدير اتصال ROYAL TV.

* 

* يجمع بين:

* 

* - مراقبة الشبكة.

* - RoyalTVRealtime.

* - Auto Reconnect.

* 

* مهم:

* 

* هذا المدير لا يتحكم في تشغيل شاشة العرض المحلية.

* 

* عند انقطاع الإنترنت:

* الشاشة تستمر بالعمل من

* RoyalTVLocalDisplayEngine.
  */
  class RoyalTVConnectionManager(
  context: Context
  ) {
  
  private val appContext =
  context.applicationContext
  
  private val scope =
  CoroutineScope(
  SupervisorJob() +
  Dispatchers.IO
  )
  
  private val networkMonitor =
  RoyalTVNetworkMonitor(
  appContext
  )
  
  private var reconnectJob:
  Job? = null
  
  private var deviceId:
  String = ""
  
  private var started =
  false
  
  private var lastNetworkState =
  RoyalTVNetworkState.OFFLINE
  
  /**
  
  * بدء مدير الاتصال.
  
  * 
  
  * يتم الحصول على Device ID محليًا،
  
  * ثم مراقبة الشبكة.
    */
    fun start(
    onCommand:
    (RoyalTVCommand) -> Unit,
    onConnectionStateChanged:
    (RoyalTVNetworkState) -> Unit = {}
    ) {
    
    if (started) {
    return
    }
    
    started = true
    
    deviceId =
    RoyalTVDeviceIdentity.getDeviceId(
    appContext
    )
    
    networkMonitor.start { state ->
    
     lastNetworkState = state

 onConnectionStateChanged(state)

 when (state) {

     RoyalTVNetworkState.ONLINE -> {
         scheduleReconnect(
             onCommand
         )
     }

     RoyalTVNetworkState.OFFLINE -> {
         cancelReconnect()

         /*
          * إيقاف قناة الإنترنت فقط.
          *
          * لا نوقف:
          * RoyalTVLocalDisplayEngine
          *
          * ولا نغير حالة المحتوى المحلي.
          */
         RoyalTVRealtime.stop()
     }
 }
    
    }
    }
  
  /**
  
  * إعادة الاتصال تلقائيًا.
  
  * 
  
  * ننتظر قليلًا بعد عودة الشبكة
  
  * حتى لا نبدأ الاتصال أثناء تقلب
  
  * الشبكة بين Online / Offline.
    */
    private fun scheduleReconnect(
    onCommand:
    (RoyalTVCommand) -> Unit
    ) {
    
    reconnectJob?.cancel()
    
    reconnectJob =
    scope.launch {
    
         delay(2000L)

     if (
         !started ||
         lastNetworkState !=
         RoyalTVNetworkState.ONLINE
     ) {
         return@launch
     }

     try {

         RoyalTVRealtime.start(
             deviceId = deviceId,
             onCommand = onCommand
         )

     } catch (e: Exception) {

         android.util.Log.e(
             "RoyalTVConnection",
             "Reconnect error: ${e.message}",
             e
         )
     }
 }
  
  }
  
  /**
  
  * إلغاء محاولة إعادة الاتصال الحالية.
    */
    private fun cancelReconnect() {
    
    reconnectJob?.cancel()
    reconnectJob = null
    }
  
  /**
  
  * حالة الشبكة الحالية.
    */
    fun getNetworkState():
    RoyalTVNetworkState {
    
    return lastNetworkState
    }
  
  /**
  
  * هل مدير الاتصال يعمل؟
    */
    fun isStarted(): Boolean {
    return started
    }
  
  /**
  
  * إيقاف مدير الاتصال.
  
  * 
  
  * هذا لا يمسح حالة شاشة العرض المحلية.
    */
    fun stop() {
    
    if (!started) {
    return
    }
    
    started = false
    
    cancelReconnect()
    
    networkMonitor.stop()
    
    RoyalTVRealtime.stop()
    
    scope.cancel()
    }
  }
