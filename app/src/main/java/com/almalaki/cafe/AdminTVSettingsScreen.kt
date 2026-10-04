package com.almalaki.cafe

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val ROYAL_TV_PREFS = "royal_tv_settings"

private const val KEY_TITLE = "tv_title"
private const val KEY_SUBTITLE = "tv_subtitle"
private const val KEY_NEWS = "tv_news"
private const val KEY_TIME_NOTE = "tv_time_note"
private const val KEY_LOGO_URI = "tv_logo_uri"

private const val KEY_TIME_SCALE = "tv_time_scale"
private const val KEY_DATE_SCALE = "tv_date_scale"
private const val KEY_TIME_BOX_SCALE = "tv_time_box_scale"
private const val KEY_NOTE_SCALE = "tv_note_scale"
private const val KEY_NOTE_BOX_SCALE = "tv_note_box_scale"
private const val KEY_TICKER_SCALE = "tv_ticker_scale"
private const val KEY_TICKER_BOX_SCALE = "tv_ticker_box_scale"
private const val KEY_LOGO_SCALE = "tv_logo_scale"

private const val KEY_TICKER_SPEED = "tv_ticker_speed"
private const val KEY_SHINE_LEVEL = "tv_shine_level"
private const val KEY_FOCUS_LEVEL = "tv_focus_level"
private const val KEY_FOCUS_ENABLED = "tv_focus_enabled"

private const val KEY_NAV_HINTS = "tv_navigation_hints"
private const val KEY_PERFORMANCE = "tv_performance"

@Composable
fun AdminTVSettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    /*
     * هذه الشاشة خاصة بالتلفزيون فقط.
     */
    if (!isRoyalTV(context)) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(AdminBlack),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📺 إعدادات التلفزيون متاحة على Android TV فقط",
                color = AdminGold,
                fontSize = 20.sp
            )
        }
        return
    }

    val prefs = remember {
        context.getSharedPreferences(
            ROYAL_TV_PREFS,
            android.content.Context.MODE_PRIVATE
        )
    }

    var title by rememberSaveable {
        mutableStateOf(
            prefs.getString(
                KEY_TITLE,
                "قائمة Royal"
            ) ?: "قائمة Royal"
        )
    }

    var subtitle by rememberSaveable {
        mutableStateOf(
            prefs.getString(
                KEY_SUBTITLE,
                "طعمٌ يستحق التجربة"
            ) ?: "طعمٌ يستحق التجربة"
        )
    }

    var newsText by rememberSaveable {
        mutableStateOf(
            prefs.getString(
                KEY_NEWS,
                ""
            ) ?: ""
        )
    }

    var timeNote by rememberSaveable {
        mutableStateOf(
            prefs.getString(
                KEY_TIME_NOTE,
                "تجريبي"
            ) ?: "تجريبي"
        )
    }

    var logoUri by rememberSaveable {
        mutableStateOf(
            prefs.getString(
                KEY_LOGO_URI,
                ""
            ) ?: ""
        )
    }

    var timeScale by rememberSaveable {
        mutableFloatStateOf(
            prefs.getFloat(KEY_TIME_SCALE, 1.35f)
        )
    }

    var dateScale by rememberSaveable {
        mutableFloatStateOf(
            prefs.getFloat(KEY_DATE_SCALE, 1.30f)
        )
    }

    var timeBoxScale by rememberSaveable {
        mutableFloatStateOf(
            prefs.getFloat(KEY_TIME_BOX_SCALE, 1.30f)
        )
    }

    var noteScale by rememberSaveable {
        mutableFloatStateOf(
            prefs.getFloat(KEY_NOTE_SCALE, 1.00f)
        )
    }

    var noteBoxScale by rememberSaveable {
        mutableFloatStateOf(
            prefs.getFloat(KEY_NOTE_BOX_SCALE, 1.00f)
        )
    }

    var tickerScale by rememberSaveable {
        mutableFloatStateOf(
            prefs.getFloat(KEY_TICKER_SCALE, 1.35f)
        )
    }

    var tickerBoxScale by rememberSaveable {
        mutableFloatStateOf(
            prefs.getFloat(KEY_TICKER_BOX_SCALE, 1.25f)
        )
    }

    var logoScale by rememberSaveable {
        mutableFloatStateOf(
            prefs.getFloat(KEY_LOGO_SCALE, 1.35f)
        )
    }

    var tickerSpeed by rememberSaveable {
        mutableStateOf(
            prefs.getString(
                KEY_TICKER_SPEED,
                "FAST"
            ) ?: "FAST"
        )
    }

    var shineLevel by rememberSaveable {
        mutableStateOf(
            prefs.getString(
                KEY_SHINE_LEVEL,
                "WEAK"
            ) ?: "WEAK"
        )
    }

    var focusLevel by rememberSaveable {
        mutableStateOf(
            prefs.getString(
                KEY_FOCUS_LEVEL,
                "MEDIUM"
            ) ?: "MEDIUM"
        )
    }

    var focusEnabled by rememberSaveable {
        mutableStateOf(
            prefs.getBoolean(
                KEY_FOCUS_ENABLED,
                true
            )
        )
    }

    var navigationHints by rememberSaveable {
        mutableStateOf(
            prefs.getBoolean(
                KEY_NAV_HINTS,
                true
            )
        )
    }

    var performanceMode by rememberSaveable {
        mutableStateOf(
            prefs.getString(
                KEY_PERFORMANCE,
                "SMOOTH"
            ) ?: "SMOOTH"
        )
    }

    var savedMessage by rememberSaveable {
        mutableStateOf(false)
    }

    /*
     * اختيار صورة اللوجو من الجهاز.
     */
    val logoPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->

            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // بعض الأجهزة قد لا تسمح بهذا النوع من الصلاحية.
                }

                logoUri = uri.toString()
            }
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AdminBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Text(
            text = "📺 إعدادات التلفزيون",
            color = AdminGold,
            fontSize = 30.sp
        )

        Text(
            text = "تحرير واجهة التلفزيون وإعداداتها",
            color = AdminCream,
            fontSize = 16.sp
        )

        Divider(color = AdminGold)

        // =====================================================
        // تحرير الواجهة
        // =====================================================

        Text(
            text = "📺 تحرير واجهة التلفزيون",
            color = AdminGold,
            fontSize = 24.sp
        )

        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                savedMessage = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("قائمة Royal")
            },
            supportingText = {
                Text("النص قابل للتعديل")
            }
        )

        OutlinedTextField(
            value = subtitle,
            onValueChange = {
                subtitle = it
                savedMessage = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("العبارة أسفل العنوان")
            },
            supportingText = {
                Text("طعمٌ يستحق التجربة")
            }
        )

        /*
         * مهم:
         * لا يوجد maxLength هنا.
         * يمكن إدخال نص طويل جدًا.
         */
        OutlinedTextField(
            value = newsText,
            onValueChange = {
                newsText = it
                savedMessage = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            label = {
                Text("📰 نص الشريط الإخباري")
            },
            supportingText = {
                Text(
                    "النص مفتوح الطول، وسيتمدّد الشريط حسب عرض الشاشة."
                )
            },
            minLines = 4
        )

        // =====================================================
        // النص أسفل الوقت
        // =====================================================

        Text(
            text = "📝 النص أسفل الوقت",
            color = AdminGold,
            fontSize = 22.sp
        )

        OutlinedTextField(
            value = timeNote,
            onValueChange = {
                timeNote = it
                savedMessage = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("النص")
            },
            supportingText = {
                Text("الافتراضي: تجريبي")
            }
        )

        // =====================================================
        // اللوجو
        // =====================================================

        Text(
            text = "🪩 اللوجو",
            color = AdminGold,
            fontSize = 22.sp
        )

        Button(
            onClick = {
                logoPicker.launch(
                    arrayOf(
                        "image/png",
                        "image/*"
                    )
                )
            }
        ) {
            Text("اختيار صورة اللوجو")
        }

        if (logoUri.isNotBlank()) {
            Text(
                text = "✅ تم اختيار اللوجو",
                color = AdminGold,
                fontSize = 15.sp
            )

            TextButton(
                onClick = {
                    logoUri = ""
                    savedMessage = false
                }
            ) {
                Text(
                    text = "إزالة اللوجو المختار",
                    color = AdminCream
                )
            }
        }

        Divider(color = AdminGold)

        // =====================================================
        // الوقت والتاريخ
        // =====================================================

        Text(
            text = "🕐 الوقت والتاريخ",
            color = AdminGold,
            fontSize = 24.sp
        )

        RoyalTVScaleSetting(
            title = "حجم الوقت",
            value = timeScale,
            min = 0.70f,
            max = 2.50f,
            onValueChange = {
                timeScale = it
                savedMessage = false
            }
        )

        RoyalTVScaleSetting(
            title = "حجم التاريخ",
            value = dateScale,
            min = 0.70f,
            max = 2.50f,
            onValueChange = {
                dateScale = it
                savedMessage = false
            }
        )

        RoyalTVScaleSetting(
            title = "حجم مستطيل الوقت",
            value = timeBoxScale,
            min = 0.70f,
            max = 2.50f,
            onValueChange = {
                timeBoxScale = it
                savedMessage = false
            }
        )

        // =====================================================
        // النص أسفل الوقت - الحجم
        // =====================================================

        Text(
            text = "📝 حجم النص أسفل الوقت",
            color = AdminGold,
            fontSize = 22.sp
        )

        RoyalTVScaleSetting(
            title = "حجم النص",
            value = noteScale,
            min = 0.70f,
            max = 2.50f,
            onValueChange = {
                noteScale = it
                savedMessage = false
            }
        )

        RoyalTVScaleSetting(
            title = "حجم المستطيل",
            value = noteBoxScale,
            min = 0.70f,
            max = 2.50f,
            onValueChange = {
                noteBoxScale = it
                savedMessage = false
            }
        )

        // =====================================================
        // الشريط الإخباري
        // =====================================================

        Text(
            text = "📰 إعدادات الشريط الإخباري",
            color = AdminGold,
            fontSize = 24.sp
        )

        Text(
            text = "عرض الشريط يمتد تلقائيًا حسب مساحة شاشة التلفزيون.",
            color = AdminCream,
            fontSize = 15.sp
        )

        RoyalTVScaleSetting(
            title = "حجم نص الأخبار",
            value = tickerScale,
            min = 0.70f,
            max = 2.50f,
            onValueChange = {
                tickerScale = it
                savedMessage = false
            }
        )

        RoyalTVScaleSetting(
            title = "حجم مستطيل الأخبار",
            value = tickerBoxScale,
            min = 0.70f,
            max = 2.50f,
            onValueChange = {
                tickerBoxScale = it
                savedMessage = false
            }
        )

        Text(
            text = "سرعة الشريط",
            color = AdminCream,
            fontSize = 18.sp
        )

        RoyalTVChoiceRow(
            values = listOf(
                "SLOW" to "◀️ بطيء",
                "FAST" to "⏪ سريع",
                "FASTER" to "⚡⏮️ أسرع"
            ),
            selected = tickerSpeed,
            onSelected = {
                tickerSpeed = it
                savedMessage = false
            }
        )

        // =====================================================
        // حجم اللوجو
        // =====================================================

        Text(
            text = "🌠 حجم اللوجو السفلي",
            color = AdminGold,
            fontSize = 22.sp
        )

        RoyalTVScaleSetting(
            title = "الحجم",
            value = logoScale,
            min = 0.60f,
            max = 2.50f,
            onValueChange = {
                logoScale = it
                savedMessage = false
            }
        )

        // =====================================================
        // اللمعان
        // =====================================================

        Text(
            text = "✨ مستوى اللمعان",
            color = AdminGold,
            fontSize = 22.sp
        )

        RoyalTVChoiceRow(
            values = listOf(
                "STRONG" to "🔥 قوي",
                "MEDIUM" to "✨ متوسط",
                "WEAK" to "◽ ضعيف",
                "OFF" to "⛔ إيقاف"
            ),
            selected = shineLevel,
            onSelected = {
                shineLevel = it
                savedMessage = false
            }
        )

        // =====================================================
        // Focus
        // =====================================================

        Text(
            text = "🎯 إعدادات التركيز Focus",
            color = AdminGold,
            fontSize = 22.sp
        )

        RoyalTVChoiceRow(
            values = listOf(
                "STRONG" to "قوي",
                "MEDIUM" to "متوسط",
                "WEAK" to "ضعيف",
                "OFF" to "إيقاف"
            ),
            selected = focusLevel,
            onSelected = {
                focusLevel = it
                savedMessage = false
            }
        )

        RoyalTVToggleButton(
            text = if (focusEnabled) {
                "🎯 Focus مفعل"
            } else {
                "🎯 Focus متوقف"
            },
            enabled = focusEnabled,
            onClick = {
                focusEnabled = !focusEnabled
                savedMessage = false
            }
        )

        // =====================================================
        // الريموت
        // =====================================================

        Text(
            text = "🎮 الريموت والتنقل",
            color = AdminGold,
            fontSize = 24.sp
        )

        Text(
            text = "↑ ↓ ← → للتنقل • OK للاختيار • Back / Exit للرجوع",
            color = AdminCream,
            fontSize = 15.sp
        )

        RoyalTVToggleButton(
            text = if (navigationHints) {
                "💡 التلميحات مفعلة"
            } else {
                "💡 التلميحات متوقفة"
            },
            enabled = navigationHints,
            onClick = {
                navigationHints = !navigationHints
                savedMessage = false
            }
        )

        // =====================================================
        // الأداء
        // =====================================================

        Text(
            text = "🚀 أداء التلفزيون",
            color = AdminGold,
            fontSize = 24.sp
        )

        RoyalTVChoiceRow(
            values = listOf(
                "SMOOTH" to "سلاسة",
                "BALANCED" to "متوازن",
                "PERFORMANCE" to "أداء أعلى"
            ),
            selected = performanceMode,
            onSelected = {
                performanceMode = it
                savedMessage = false
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // =====================================================
        // زر الحفظ
        // =====================================================
Button(
    onClick = {
        prefs.edit()
            .putString(KEY_TITLE, title)
            .putString(KEY_SUBTITLE, subtitle)
            .putString(KEY_NEWS, newsText)
            .putString(KEY_TIME_NOTE, timeNote)
            .putString(KEY_LOGO_URI, logoUri)
            .putFloat(KEY_TIME_SCALE, timeScale)
            .putFloat(KEY_DATE_SCALE, dateScale)
            .putFloat(KEY_TIME_BOX_SCALE, timeBoxScale)
            .putFloat(KEY_NOTE_SCALE, noteScale)
            .putFloat(KEY_NOTE_BOX_SCALE, noteBoxScale)
            .putFloat(KEY_TICKER_SCALE, tickerScale)
            .putFloat(KEY_TICKER_BOX_SCALE, tickerBoxScale)
            .putFloat(KEY_LOGO_SCALE, logoScale)
            .putString(KEY_TICKER_SPEED, tickerSpeed)
            .putString(KEY_SHINE_LEVEL, shineLevel)
            .putString(KEY_FOCUS_LEVEL, focusLevel)
            .putBoolean(KEY_FOCUS_ENABLED, focusEnabled)
            .putBoolean(KEY_NAV_HINTS, navigationHints)
            .putString(KEY_PERFORMANCE, performanceMode)
            .apply()

        savedMessage = true
    },
    modifier = Modifier
        .fillMaxWidth()
        .height(58.dp)
) {
    Text(
        text = "حفظ جميع التعديلات",
        fontSize = 19.sp
    )
}

        if (savedMessage) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "✅ تم حفظ التعديلات بنجاح",
                    color = AdminGold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun RoyalTVScaleSetting(
    title: String,
    value: Float,
    min: Float,
    max: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "$title  ${"%.2f".format(value)}×",
            color = AdminCream,
            fontSize = 16.sp
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RoyalTVChoiceRow(
    values: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        values.forEach { (key, label) ->
            val isSelected = selected == key

            if (isSelected) {
                Button(
                    onClick = {
                        onSelected(key)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(label)
                }
            } else {
                OutlinedButton(
                    onClick = {
                        onSelected(key)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(label)
                }
            }
        }
    }
}

@Composable
private fun RoyalTVToggleButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    if (enabled) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text)
        }
    }
}
