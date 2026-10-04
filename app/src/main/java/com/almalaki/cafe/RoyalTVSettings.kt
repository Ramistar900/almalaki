package com.almalaki.cafe

import android.content.Context
import android.content.res.Configuration

/*

* ============================================================
* إعدادات Android TV
* ============================================================
* 
* هذا الملف خاص بالتلفزيون فقط.
* 
* الهاتف والتابلت لا يستخدمان هذه الإعدادات.
* 
* لاحقًا سيتم ربط:
* - حجم الوقت والتاريخ
* - حجم مستطيل الوقت
* - حجم الشريط الإخباري
* - حجم مستطيل الشريط
* - حجم اللوجو السفلي
* - شدة اللمعان
* - سرعة الشريط الإخباري
* - أزرار الريموت واختصاراتها
* - Focus
* - تلميحات التنقل
* 
* ============================================================
  */

/*

* هل الجهاز يعمل بنمط Android TV؟
* 
* يعتمد الكشف على نوع واجهة الجهاز وليس على اسم الجهاز.
  */
  fun isRoyalTV(context: Context): Boolean {
  return (
  context.resources.configuration.uiMode and
  Configuration.UI_MODE_TYPE_MASK
  ) == Configuration.UI_MODE_TYPE_TELEVISION
  }

/*

* ============================================================

* أحجام عناصر التلفزيون

* ============================================================
  */
  data class RoyalTVDisplaySettings(
  
  // الوقت
  val timeScale: Float = 1.35f,
  
  // مستطيل الوقت
  val timeBoxScale: Float = 1.30f,
  
  // التاريخ
  val dateScale: Float = 1.30f,
  
  // الشريط الإخباري
  val tickerScale: Float = 1.35f,
  
  // مستطيل الشريط الإخباري
  val tickerBoxScale: Float = 1.25f,
  
  // اللوجو السفلي
  val bottomLogoScale: Float = 1.35f
  )

/*

* ============================================================

* شدة المؤثرات

* ============================================================
  */
  enum class RoyalTVEffectLevel {
  
  STRONG,
  MEDIUM,
  WEAK,
  OFF
  }

/*

* ============================================================

* سرعة الشريط الإخباري

* ============================================================
  */
  enum class RoyalTVTickerSpeed {
  
  SLOW,
  FAST,
  FASTER
  }

/*

* ============================================================

* إعدادات المؤثرات

* ============================================================
  */
  data class RoyalTVEffectSettings(
  
  // اللمعان العام
  val shineLevel: RoyalTVEffectLevel =
  RoyalTVEffectLevel.WEAK,
  
  // لمعة التركيز Focus
  val focusLevel: RoyalTVEffectLevel =
  RoyalTVEffectLevel.MEDIUM,
  
  // تفعيل لمعة التركيز
  val focusEnabled: Boolean = true
  )

/*

* ============================================================

* إعدادات الشريط الإخباري

* ============================================================
  */
  data class RoyalTVTickerSettings(
  
  val speed: RoyalTVTickerSpeed =
  RoyalTVTickerSpeed.FAST
  )

/*

* ============================================================

* وظائف أزرار الريموت

* ============================================================
  */
  enum class RoyalTVRemoteAction {
  
  MOVE_UP,
  MOVE_DOWN,
  MOVE_LEFT,
  MOVE_RIGHT,
  SELECT,
  BACK
  }

/*

* ============================================================

* إعدادات الريموت

* ============================================================
  */
  data class RoyalTVRemoteSettings(
  
  val upAction: RoyalTVRemoteAction =
  RoyalTVRemoteAction.MOVE_UP,
  
  val downAction: RoyalTVRemoteAction =
  RoyalTVRemoteAction.MOVE_DOWN,
  
  val leftAction: RoyalTVRemoteAction =
  RoyalTVRemoteAction.MOVE_LEFT,
  
  val rightAction: RoyalTVRemoteAction =
  RoyalTVRemoteAction.MOVE_RIGHT,
  
  val okAction: RoyalTVRemoteAction =
  RoyalTVRemoteAction.SELECT,
  
  val exitAction: RoyalTVRemoteAction =
  RoyalTVRemoteAction.BACK
  )

/*

* ============================================================

* تلميحات التنقل

* ============================================================
  */
  data class RoyalTVNavigationSettings(
  
  // تظهر للمستخدم أول مرة فقط
  val showNavigationHintsFirstTime: Boolean = true
  )

/*

* ============================================================

* الإعدادات الكاملة للتلفزيون

* ============================================================
  */
  data class RoyalTVSettings(
  
  val display: RoyalTVDisplaySettings =
  RoyalTVDisplaySettings(),
  
  val effects: RoyalTVEffectSettings =
  RoyalTVEffectSettings(),
  
  val ticker: RoyalTVTickerSettings =
  RoyalTVTickerSettings(),
  
  val remote: RoyalTVRemoteSettings =
  RoyalTVRemoteSettings(),
  
  val navigation: RoyalTVNavigationSettings =
  RoyalTVNavigationSettings()
  )

/*

* ============================================================
* الإعدادات الافتراضية
* ============================================================
  */
  fun defaultRoyalTVSettings(): RoyalTVSettings {
  return RoyalTVSettings()
  }
