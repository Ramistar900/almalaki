package com.almalaki.cafe.royaltv

import android.content.Context
import android.content.SharedPreferences

/**

* محرك العرض المحلي لـ ROYAL TV.

* 

* مهم جدًا:

* 

* هذا الملف مستقل عن الإنترنت وSupabase وRealtime.

* 

* وظيفته حفظ واستعادة حالة شاشة العرض محليًا،

* حتى تستمر الشاشة بالعمل عند انقطاع الإنترنت

* وحتى بعد إعادة تشغيل الجهاز.

* 

* الإنترنت سيكون لاحقًا قناة للأوامر والمزامنة فقط،

* وليس شرطًا لتشغيل شاشة العرض.
  */
  object RoyalTVLocalDisplayEngine {
  
  private const val PREFS_NAME =
  "royal_tv_local_display"
  
  private const val KEY_SOURCE =
  "source"
  
  private const val KEY_COMMAND_TYPE =
  "command_type"
  
  private const val KEY_PAYLOAD =
  "payload"
  
  private const val KEY_IS_PLAYING =
  "is_playing"
  
  private const val KEY_IS_FULLSCREEN =
  "is_fullscreen"
  
  private const val KEY_VOLUME =
  "volume"
  
  private const val DEFAULT_VOLUME =
  100
  
  private var preferences: SharedPreferences? = null
  
  /**
  
  * تهيئة المحرك المحلي.
  
  * 
  
  * لا يوجد هنا أي اتصال بالإنترنت.
    */
    fun initialize(
    context: Context
    ) {
    if (preferences != null) {
    return
    }
    
    preferences =
    context.applicationContext.getSharedPreferences(
    PREFS_NAME,
    Context.MODE_PRIVATE
    )
    }
  
  /**
  
  * حفظ حالة العرض الحالية على الجهاز.
  
  * 
  
  * هذه العملية محلية بالكامل.
    */
    fun saveState(
    state: RoyalTVState
    ) {
    val prefs =
    preferences
    ?: return
    
    prefs.edit()
    .putString(
    KEY_SOURCE,
    state.source.name
    )
    .putString(
    KEY_COMMAND_TYPE,
    state.commandType?.name
    )
    .putString(
    KEY_PAYLOAD,
    state.payload
    )
    .putBoolean(
    KEY_IS_PLAYING,
    state.isPlaying
    )
    .putBoolean(
    KEY_IS_FULLSCREEN,
    state.isFullscreen
    )
    .apply()
    }
  
  /**
  
  * استعادة آخر حالة عرض محفوظة.
  
  * 
  
  * إذا لم توجد حالة سابقة،
  
  * نعيد الحالة الافتراضية.
    */
    fun restoreState(): RoyalTVState {
    
    val prefs =
    preferences
    ?: return RoyalTVState()
    
    val source =
    try {
    RoyalTVSource.valueOf(
    prefs.getString(
    KEY_SOURCE,
    RoyalTVSource.IDLE.name
    ) ?: RoyalTVSource.IDLE.name
    )
    } catch (_: Exception) {
    RoyalTVSource.IDLE
    }
    
    val commandType =
    try {
    prefs.getString(
    KEY_COMMAND_TYPE,
    null
    )?.let {
    RoyalTVCommandType.valueOf(it)
    }
    } catch (_: Exception) {
    null
    }
    
    val payload =
    prefs.getString(
    KEY_PAYLOAD,
    ""
    ) ?: ""
    
    val isPlaying =
    prefs.getBoolean(
    KEY_IS_PLAYING,
    false
    )
    
    val isFullscreen =
    prefs.getBoolean(
    KEY_IS_FULLSCREEN,
    true
    )
    
    return RoyalTVState(
    source = source,
    commandType = commandType,
    payload = payload,
    isPlaying = isPlaying,
    isFullscreen = isFullscreen
    )
    }
  
  /**
  
  * حفظ مستوى الصوت محليًا.
  
  * 
  
  * لا يعتمد على الإنترنت.
    */
    fun saveVolume(
    volume: Int
    ) {
    val prefs =
    preferences
    ?: return
    
    val safeVolume =
    volume.coerceIn(0, 100)
    
    prefs.edit()
    .putInt(
    KEY_VOLUME,
    safeVolume
    )
    .apply()
    }
  
  /**
  
  * استعادة مستوى الصوت المحلي.
    */
    fun restoreVolume(): Int {
    
    val prefs =
    preferences
    ?: return DEFAULT_VOLUME
    
    return prefs.getInt(
    KEY_VOLUME,
    DEFAULT_VOLUME
    ).coerceIn(0, 100)
    }
  
  /**
  
  * مسح الحالة المحلية فقط.
  
  * 
  
  * لا يحذف أي شيء من Supabase
  
  * ولا يرسل أي أمر للشبكة.
    */
    fun clearLocalState() {
    
    val prefs =
    preferences
    ?: return
    
    prefs.edit()
    .remove(KEY_SOURCE)
    .remove(KEY_COMMAND_TYPE)
    .remove(KEY_PAYLOAD)
    .remove(KEY_IS_PLAYING)
    .remove(KEY_IS_FULLSCREEN)
    .apply()
    }
  
  /**
  
  * هل توجد حالة عرض محفوظة؟
    */
    fun hasSavedState(): Boolean {
    
    val prefs =
    preferences
    ?: return false
    
    return prefs.contains(KEY_SOURCE)
    }
  }
