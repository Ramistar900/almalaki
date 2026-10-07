package com.almalaki.cafe.royaltv.identity

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almalaki.cafe.RoyalTVResponsiveLogo

private val RoyalGold = Color(0xFFD4AF37)
private val RoyalGoldLight = Color(0xFFFFE9A3)
private val RoyalBlack = Color(0xFF050505)
private val RoyalPanel = Color(0xFF111111)
private val RoyalCream = Color(0xFFF5F0E5)

private const val PREFS_NAME = "royal_tv_identity"

private const val KEY_IDENTITY_ENABLED = "identity_enabled"
private const val KEY_LOGO_ENABLED = "logo_enabled"
private const val KEY_TICKER_ENABLED = "ticker_enabled"
private const val KEY_CLOCK_ENABLED = "clock_enabled"
private const val KEY_LIVE_ENABLED = "live_enabled"
private const val KEY_LIVE_TEXT = "live_text"
private const val KEY_TICKER_TEXT = "ticker_text"
private const val KEY_SPORTS_MODE = "sports_mode"

/*
 * حجم شعار Royal TV.
 *
 * يتم حفظه داخل نفس نظام إعدادات الهوية الحالي.
 */
private const val KEY_LOGO_SIZE_PERCENT = "logo_size_percent"
private const val DEFAULT_LOGO_SIZE_PERCENT = 100

private fun identityPrefs(context: Context) =
    context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

/**
 * قراءة حجم شعار Royal TV المحفوظ.
 */
fun loadRoyalTVLogoSizePercent(
    context: Context
): Int {
    return identityPrefs(context)
        .getInt(
            KEY_LOGO_SIZE_PERCENT,
            DEFAULT_LOGO_SIZE_PERCENT
        )
        .coerceIn(
            RoyalTVResponsiveLogo.MIN_SIZE_PERCENT,
            RoyalTVResponsiveLogo.MAX_SIZE_PERCENT
        )
}

/**
 * حفظ حجم شعار Royal TV.
 */
fun saveRoyalTVLogoSizePercent(
    context: Context,
    percent: Int
) {
    val safePercent =
        percent.coerceIn(
            RoyalTVResponsiveLogo.MIN_SIZE_PERCENT,
            RoyalTVResponsiveLogo.MAX_SIZE_PERCENT
        )

    identityPrefs(context)
        .edit()
        .putInt(
            KEY_LOGO_SIZE_PERCENT,
            safePercent
        )
        .apply()
}

/**
 * استعادة الحجم الأساسي للشعار.
 */
fun resetRoyalTVLogoSizePercent(
    context: Context
) {
    saveRoyalTVLogoSizePercent(
        context = context,
        percent = RoyalTVResponsiveLogo.DEFAULT_SIZE_PERCENT
    )
}

/**
 * شاشة إعداد هوية Royal TV.
 *
 * هذه الشاشة مخصصة للمالك فقط.
 *
 * جميع إعدادات الهوية محفوظة داخل نفس SharedPreferences
 * الحالي حتى لا يتم إنشاء نظام إعدادات مكرر.
 */
@Composable
fun RoyalTVIdentitySettingsScreen(
    modifier: Modifier = Modifier,
    context: Context
) {
    val prefs = identityPrefs(context)

    var identityEnabled by rememberSaveable {
        mutableStateOf(
            prefs.getBoolean(
                KEY_IDENTITY_ENABLED,
                true
            )
        )
    }

    var logoEnabled by rememberSaveable {
        mutableStateOf(
            prefs.getBoolean(
                KEY_LOGO_ENABLED,
                true
            )
        )
    }

    var tickerEnabled by rememberSaveable {
        mutableStateOf(
            prefs.getBoolean(
                KEY_TICKER_ENABLED,
                true
            )
        )
    }

    var clockEnabled by rememberSaveable {
        mutableStateOf(
            prefs.getBoolean(
                KEY_CLOCK_ENABLED,
                true
            )
        )
    }

    var liveEnabled by rememberSaveable {
        mutableStateOf(
            prefs.getBoolean(
                KEY_LIVE_ENABLED,
                true
            )
        )
    }

    var liveText by rememberSaveable {
        mutableStateOf(
            prefs.getString(
                KEY_LIVE_TEXT,
                "مباشر"
            ) ?: "مباشر"
        )
    }

    var tickerText by rememberSaveable {
        mutableStateOf(
            prefs.getString(
                KEY_TICKER_TEXT,
                "Royal Coffee • جودة فاخرة"
            ) ?: "Royal Coffee • جودة فاخرة"
        )
    }

    var sportsMode by rememberSaveable {
        mutableStateOf(
            prefs.getBoolean(
                KEY_SPORTS_MODE,
                false
            )
        )
    }

    var logoSizePercent by rememberSaveable {
        mutableStateOf(
            loadRoyalTVLogoSizePercent(context)
        )
    }

    fun saveBoolean(
        key: String,
        value: Boolean
    ) {
        prefs.edit()
            .putBoolean(
                key,
                value
            )
            .apply()
    }

    fun saveText(
        key: String,
        value: String
    ) {
        prefs.edit()
            .putString(
                key,
                value
            )
            .apply()
    }

    fun changeLogoSize(
        increase: Boolean
    ) {
        val newValue =
            RoyalTVResponsiveLogo.applySizeStep(
                currentPercent = logoSizePercent,
                increase = increase
            )

        if (newValue != logoSizePercent) {
            logoSizePercent = newValue

            saveRoyalTVLogoSizePercent(
                context = context,
                percent = newValue
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RoyalBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(20.dp),
            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = RoyalPanel
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📺",
                        fontSize = 38.sp
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = "هوية شاشة العرض",
                        color = RoyalGold,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text = "Royal Coffee TV",
                        color = RoyalGoldLight,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Text(
                        text =
                            "هذه الإعدادات خاصة بشاشة التلفزيون داخل المقهى.",
                        color = RoyalCream,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            IdentitySwitchCard(
                title = "تفعيل هوية Royal Coffee",
                description =
                    "إظهار الشعار وشريط الأخبار والساعة ومؤشر البث.",
                checked = identityEnabled,
                onCheckedChange = {
                    identityEnabled = it

                    saveBoolean(
                        KEY_IDENTITY_ENABLED,
                        it
                    )
                }
            )

            IdentitySwitchCard(
                title = "👑 شعار Royal Coffee",
                description =
                    "إظهار الشعار المتحرك على شاشة التلفزيون.",
                checked = logoEnabled,
                enabled = identityEnabled,
                onCheckedChange = {
                    logoEnabled = it

                    saveBoolean(
                        KEY_LOGO_ENABLED,
                        it
                    )
                }
            )

            /*
             * حجم الشعار المتجاوب.
             *
             * لا يغيّر طبقات PNG أو الحركة أو النسب.
             */
            if (identityEnabled && logoEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = RoyalPanel
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📐 حجم الشعار",
                            color = RoyalGoldLight,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(5.dp)
                        )

                        Text(
                            text =
                                "الحجم يتكيف تلقائيًا مع الشاشة، ويمكن التحكم بالحجم يدويًا.",
                            color = RoyalCream,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )

                        Text(
                            text = "$logoSizePercent%",
                            color = RoyalGold,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    changeLogoSize(
                                        increase = false
                                    )
                                },
                                enabled =
                                    logoSizePercent >
                                        RoyalTVResponsiveLogo.MIN_SIZE_PERCENT,
                                modifier =
                                    Modifier.weight(1f),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            RoyalPanel,
                                        contentColor =
                                            RoyalGoldLight
                                    )
                            ) {
                                Text("− 5%")
                            }

                            Button(
                                onClick = {
                                    resetRoyalTVLogoSizePercent(
                                        context
                                    )

                                    logoSizePercent =
                                        RoyalTVResponsiveLogo.DEFAULT_SIZE_PERCENT
                                },
                                modifier =
                                    Modifier.weight(1.25f),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            RoyalGold,
                                        contentColor =
                                            RoyalBlack
                                    )
                            ) {
                                Text("استعادة 100%")
                            }

                            Button(
                                onClick = {
                                    changeLogoSize(
                                        increase = true
                                    )
                                },
                                enabled =
                                    logoSizePercent <
                                        RoyalTVResponsiveLogo.MAX_SIZE_PERCENT,
                                modifier =
                                    Modifier.weight(1f),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            RoyalPanel,
                                        contentColor =
                                            RoyalGoldLight
                                    )
                            ) {
                                Text("+ 5%")
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Text(
                            text =
                                "60%  ←  الحد الأدنى   •   100% الأساسي   •   الحد الأقصى  →  160%",
                            color = RoyalCream,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            IdentitySwitchCard(
                title = "📰 شريط الأخبار",
                description =
                    "إظهار شريط الأخبار المتحرك أسفل الشاشة.",
                checked = tickerEnabled,
                enabled = identityEnabled,
                onCheckedChange = {
                    tickerEnabled = it

                    saveBoolean(
                        KEY_TICKER_ENABLED,
                        it
                    )
                }
            )

            if (identityEnabled && tickerEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = RoyalPanel
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "نص شريط الأخبار",
                            color = RoyalGoldLight,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = tickerText,
                            onValueChange = {
                                tickerText = it

                                saveText(
                                    KEY_TICKER_TEXT,
                                    it
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            shape = RoundedCornerShape(14.dp),
                            label = {
                                Text("النص")
                            }
                        )
                    }
                }
            }

            IdentitySwitchCard(
                title = "🕐 الوقت والتاريخ",
                description =
                    "إظهار الوقت والتاريخ على شاشة Royal TV.",
                checked = clockEnabled,
                enabled = identityEnabled,
                onCheckedChange = {
                    clockEnabled = it

                    saveBoolean(
                        KEY_CLOCK_ENABLED,
                        it
                    )
                }
            )

            IdentitySwitchCard(
                title = "🔴 مؤشر البث",
                description =
                    "إظهار النقطة الحمراء والنص على الشاشة.",
                checked = liveEnabled,
                enabled = identityEnabled,
                onCheckedChange = {
                    liveEnabled = it

                    saveBoolean(
                        KEY_LIVE_ENABLED,
                        it
                    )
                }
            )

            if (identityEnabled && liveEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = RoyalPanel
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "نص مؤشر البث",
                            color = RoyalGoldLight,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = liveText,
                            onValueChange = {
                                liveText = it

                                saveText(
                                    KEY_LIVE_TEXT,
                                    it
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            label = {
                                Text("النص")
                            }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        if (sportsMode) {
                            Color(0xFF151515)
                        } else {
                            RoyalPanel
                        }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "⚽ الوضع الرياضي",
                                color = RoyalGoldLight,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    "إخفاء هوية Royal Coffee مؤقتًا أثناء عرض المباراة.",
                                color = RoyalCream,
                                fontSize = 13.sp
                            )
                        }

                        Switch(
                            checked = sportsMode,
                            onCheckedChange = {
                                sportsMode = it

                                saveBoolean(
                                    KEY_SPORTS_MODE,
                                    it
                                )
                            }
                        )
                    }

                    if (sportsMode) {
                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        HorizontalDivider(
                            color = RoyalGold.copy(
                                alpha = 0.25f
                            )
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Text(
                            text =
                                "عند تفعيل الوضع الرياضي يتم إخفاء عناصر الهوية فقط، دون حذف إعداداتها.",
                            color = RoyalGoldLight,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        RoyalGold.copy(alpha = 0.08f)
                )
            ) {
                Text(
                    text =
                        "مهم: إخفاء الهوية لا يحذف الشعار أو النصوص أو الإعدادات. عند إعادة التفعيل تعود الهوية بنفس إعداداتها السابقة.",
                    color = RoyalGoldLight,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            Button(
                onClick = {
                    identityEnabled = true
                    logoEnabled = true
                    tickerEnabled = true
                    clockEnabled = true
                    liveEnabled = true
                    sportsMode = false

                    saveBoolean(
                        KEY_IDENTITY_ENABLED,
                        true
                    )

                    saveBoolean(
                        KEY_LOGO_ENABLED,
                        true
                    )

                    saveBoolean(
                        KEY_TICKER_ENABLED,
                        true
                    )

                    saveBoolean(
                        KEY_CLOCK_ENABLED,
                        true
                    )

                    saveBoolean(
                        KEY_LIVE_ENABLED,
                        true
                    )

                    saveBoolean(
                        KEY_SPORTS_MODE,
                        false
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalGold,
                    contentColor = RoyalBlack
                )
            ) {
                Text(
                    text = "▶ تفعيل هوية Royal Coffee",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )
        }
    }
}

/**
 * بطاقة مفتاح مستقلة لإعدادات هوية Royal TV.
 */
@Composable
private fun IdentitySwitchCard(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = RoyalPanel
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color =
                        if (enabled) {
                            RoyalGoldLight
                        } else {
                            RoyalCream.copy(
                                alpha = 0.45f
                            )
                        },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = description,
                    color =
                        if (enabled) {
                            RoyalCream
                        } else {
                            RoyalCream.copy(
                                alpha = 0.35f
                            )
                        },
                    fontSize = 13.sp
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
                            
