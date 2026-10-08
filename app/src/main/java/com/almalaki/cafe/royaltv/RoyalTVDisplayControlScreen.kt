
package com.almalaki.cafe.royaltv

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DisplayGold = Color(0xFFD4AF37)
private val DisplayGoldLight = Color(0xFFFFE9A3)
private val DisplayBlack = Color(0xFF050505)
private val DisplayPanel = Color(0xFF111111)
private val DisplayCream = Color(0xFFF5F0E5)

private const val PREFS_NAME = "royal_tv_display_control"
private const val KEY_FULLSCREEN = "fullscreen"
private const val KEY_ORIENTATION = "orientation"
private const val KEY_BACKGROUND = "background"

enum class RoyalTVOrientation {
    PORTRAIT,
    LANDSCAPE,
    AUTO
}

fun royalTVBackgroundColor(context: Context): Color {
    val value = context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getInt(KEY_BACKGROUND, 0xFF030303.toInt())

    return Color(value)
}

@Composable
fun RoyalTVDisplayControlScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val preferences = remember(context) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }

    var fullscreen by remember {
        mutableStateOf(
            preferences.getBoolean(KEY_FULLSCREEN, false)
        )
    }

    var orientation by remember {
        mutableStateOf(
            when (preferences.getString(KEY_ORIENTATION, "AUTO")) {
                "PORTRAIT" -> RoyalTVOrientation.PORTRAIT
                "LANDSCAPE" -> RoyalTVOrientation.LANDSCAPE
                else -> RoyalTVOrientation.AUTO
            }
        )
    }

    var backgroundValue by remember {
        mutableStateOf(
            preferences.getInt(KEY_BACKGROUND, 0xFF030303.toInt())
        )
    }

    DisposableEffect(activity, fullscreen) {
        val decorView = activity?.window?.decorView
        val previousVisibility = decorView?.systemUiVisibility

        if (decorView != null) {
            decorView.systemUiVisibility =
                if (fullscreen) {
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                } else {
                    View.SYSTEM_UI_FLAG_VISIBLE
                }
        }

        onDispose {
            if (decorView != null && previousVisibility != null) {
                decorView.systemUiVisibility = previousVisibility
            }
        }
    }

    DisposableEffect(activity, orientation) {
        val previousOrientation = activity?.requestedOrientation

        activity?.requestedOrientation =
            when (orientation) {
                RoyalTVOrientation.PORTRAIT ->
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

                RoyalTVOrientation.LANDSCAPE ->
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

                RoyalTVOrientation.AUTO ->
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }

        onDispose {
            if (activity != null && previousOrientation != null) {
                activity.requestedOrientation = previousOrientation
            }
        }
    }

    fun saveOrientation(value: RoyalTVOrientation) {
        orientation = value
        preferences.edit()
            .putString(KEY_ORIENTATION, value.name)
            .apply()
    }

    fun saveBackground(value: Int) {
        backgroundValue = value
        preferences.edit()
            .putInt(KEY_BACKGROUND, value)
            .apply()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DisplayBlack)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "العرض والشاشة",
            color = DisplayGold,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Text(
            text = "إعدادات عرض قناة ROYAL TV",
            color = DisplayGoldLight,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = DisplayPanel,
                contentColor = DisplayGoldLight
            )
        ) {
            Text("رجوع")
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = DisplayPanel
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "اتجاه الشاشة",
                    color = DisplayGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DisplayChoiceButton(
                        text = "طولي",
                        selected = orientation == RoyalTVOrientation.PORTRAIT,
                        modifier = Modifier.weight(1f)
                    ) {
                        saveOrientation(RoyalTVOrientation.PORTRAIT)
                    }

                    DisplayChoiceButton(
                        text = "عرضي",
                        selected = orientation == RoyalTVOrientation.LANDSCAPE,
                        modifier = Modifier.weight(1f)
                    ) {
                        saveOrientation(RoyalTVOrientation.LANDSCAPE)
                    }

                    DisplayChoiceButton(
                        text = "تلقائي",
                        selected = orientation == RoyalTVOrientation.AUTO,
                        modifier = Modifier.weight(1f)
                    ) {
                        saveOrientation(RoyalTVOrientation.AUTO)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = DisplayPanel
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ملء الشاشة",
                    color = DisplayGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        fullscreen = !fullscreen
                        preferences.edit()
                            .putBoolean(KEY_FULLSCREEN, fullscreen)
                            .apply()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            if (fullscreen) DisplayGold else DisplayBlack,
                        contentColor =
                            if (fullscreen) DisplayBlack else DisplayGoldLight
                    )
                ) {
                    Text(
                        if (fullscreen) {
                            "ملء الشاشة: مفعّل"
                        } else {
                            "ملء الشاشة: متوقف"
                        }
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = DisplayPanel
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "خلفية العرض",
                    color = DisplayGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DisplayChoiceButton(
                        text = "أسود",
                        selected = backgroundValue == 0xFF030303.toInt(),
                        modifier = Modifier.weight(1f)
                    ) {
                        saveBackground(0xFF030303.toInt())
                    }

                    DisplayChoiceButton(
                        text = "كريمي",
                        selected = backgroundValue == 0xFFF5F0E5.toInt(),
                        modifier = Modifier.weight(1f)
                    ) {
                        saveBackground(0xFFF5F0E5.toInt())
                    }

                    DisplayChoiceButton(
                        text = "ذهبي داكن",
                        selected = backgroundValue == 0xFF17120A.toInt(),
                        modifier = Modifier.weight(1f)
                    ) {
                        saveBackground(0xFF17120A.toInt())
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "تُحفظ الإعدادات على هذا الجهاز. ربط الخلفية بالشاشة الفعلية سيتم في مرحلة الدمج.",
            color = DisplayCream,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DisplayChoiceButton(
    text: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor =
                if (selected) DisplayGold else DisplayBlack,
            contentColor =
                if (selected) DisplayBlack else DisplayGoldLight
        )
    ) {
        Text(text)
    }
}
