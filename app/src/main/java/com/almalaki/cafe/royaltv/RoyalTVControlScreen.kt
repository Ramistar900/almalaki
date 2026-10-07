package com.almalaki.cafe.royaltv

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almalaki.cafe.ROYAL_TV_DEFAULT_DATE_SIZE_PERCENT
import com.almalaki.cafe.ROYAL_TV_DEFAULT_LIVE_OFFSET_X_PERCENT
import com.almalaki.cafe.ROYAL_TV_DEFAULT_LIVE_OFFSET_Y_PERCENT
import com.almalaki.cafe.ROYAL_TV_DEFAULT_LIVE_SIZE_PERCENT
import com.almalaki.cafe.ROYAL_TV_DEFAULT_LOGO_OFFSET_X_PERCENT
import com.almalaki.cafe.ROYAL_TV_DEFAULT_LOGO_OFFSET_Y_PERCENT
import com.almalaki.cafe.ROYAL_TV_DEFAULT_LOGO_SIZE_PERCENT
import com.almalaki.cafe.ROYAL_TV_DEFAULT_TICKER_SIZE_PERCENT
import com.almalaki.cafe.ROYAL_TV_DEFAULT_TIME_SIZE_PERCENT
import com.almalaki.cafe.RoyalTVLayoutSettings
import com.almalaki.cafe.changeRoyalTVOffsetXPercent
import com.almalaki.cafe.changeRoyalTVOffsetYPercent
import com.almalaki.cafe.changeRoyalTVSizePercent
import com.almalaki.cafe.loadRoyalTVLayoutSettings
import com.almalaki.cafe.resetRoyalTVLayoutSettings
import com.almalaki.cafe.saveRoyalTVLayoutSettings

private val RoyalGold = Color(0xFFD4AF37)
private val RoyalGoldLight = Color(0xFFFFE9A3)
private val RoyalBlack = Color(0xFF050505)
private val RoyalPanel = Color(0xFF111111)
private val RoyalCream = Color(0xFFF5F0E5)

data class RoyalTVControlItem(
    val icon: String,
    val title: String,
    val description: String
)

@Composable
fun RoyalTVControlScreen(
    modifier: Modifier = Modifier,
    onItemSelected: (RoyalTVControlItem) -> Unit = {}
) {
    val context = LocalContext.current

    var identityOpen by remember {
        mutableStateOf(false)
    }

    if (identityOpen) {
        RoyalTVLayoutControl(
            context = context,
            modifier = modifier,
            onBack = {
                identityOpen = false
            }
        )

        return
    }

    val items = listOf(
        RoyalTVControlItem(
            "▶️",
            "YouTube",
            "تشغيل محتوى YouTube على ROYAL TV"
        ),
        RoyalTVControlItem(
            "📺",
            "بث تطبيقات شاشة التلفزيون",
            "اختيار تطبيق مثبت على التلفزيون"
        ),
        RoyalTVControlItem(
            "🖼️",
            "عرض وسائط الجهاز",
            "عرض الصور والفيديوهات من جهاز المالك"
        ),
        RoyalTVControlItem(
            "📱",
            "بث تطبيقات الهاتف",
            "عرض محتوى تطبيقات الهاتف على التلفزيون"
        ),
        RoyalTVControlItem(
            "📡",
            "بث شاشة الهاتف",
            "عرض شاشة هاتف المالك على التلفزيون"
        ),
        RoyalTVControlItem(
            "🎬",
            "المحتوى والإعلانات",
            "إدارة المحتوى والإعلانات والبوسترات"
        ),
        RoyalTVControlItem(
            "👑",
            "هوية ROYAL TV",
            "الشعار والساعة وشريط الأخبار ومؤشر البث"
        ),
        RoyalTVControlItem(
            "⚽",
            "الوضع الرياضي",
            "إدارة وضع المباريات والمحتوى الرياضي"
        ),
        RoyalTVControlItem(
            "🔔",
            "الطلبات الجاهزة",
            "عرض طلب جاهز أو إعلان مؤقت على التلفزيون"
        ),
        RoyalTVControlItem(
            "🎮",
            "التحكم عن بُعد",
            "التحكم بالتلفزيون من شاشة المالك"
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RoyalBlack)
            .padding(18.dp)
    ) {
        Text(
            text = "📺 ROYAL TV",
            color = RoyalGold,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = "مركز التحكم الخاص بالمالك",
            color = RoyalGoldLight,
            fontSize = 15.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = items,
                key = { it.title }
            ) { item ->

                RoyalTVControlCard(
                    item = item,
                    onClick = {
                        if (item.title == "هوية ROYAL TV") {
                            identityOpen = true
                        }

                        onItemSelected(item)
                    }
                )
            }
        }
    }
}

/*
 * ============================================================
 * ROYAL TV LAYOUT CONTROL
 * ============================================================
 *
 * مركز التحكم الحقيقي لتخطيط شاشة Royal TV.
 *
 * جميع القيم تأتي من:
 *
 * RoyalTVLayoutSettings.kt
 *
 * ولا يتم إنشاء SharedPreferences جديد هنا.
 */
@Composable
private fun RoyalTVLayoutControl(
    context: Context,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    var settings by remember {
        mutableStateOf(
            loadRoyalTVLayoutSettings(context)
        )
    }
    var directEditorOpen by remember {
    mutableStateOf(false)
    }
    if (directEditorOpen) {
    RoyalTVDirectEditor(
        originalSettings = settings,
        target = RoyalTVEditTarget.LOGO,
        onSave = { newSettings ->
            settings = newSettings

            saveRoyalTVLayoutSettings(
                context = context,
                settings = newSettings
            )

            directEditorOpen = false
        },
        onCancel = {
            directEditorOpen = false
        }
    ) { draftSettings ->

        RoyalTVScreen(
            modifier = Modifier.fillMaxSize(),
            layoutSettingsOverride = draftSettings
        )
    }

    return
    }

    fun save(
        newSettings: RoyalTVLayoutSettings
    ) {
        settings = newSettings

        saveRoyalTVLayoutSettings(
            context = context,
            settings = newSettings
        )
    }

    fun changeLogoSize(
        increase: Boolean
    ) {
        save(
            settings.copy(
                logoSizePercent =
                    changeRoyalTVSizePercent(
                        currentPercent =
                            settings.logoSizePercent,
                        increase = increase
                    )
            )
        )
    }

    fun changeLogoX(
        increase: Boolean
    ) {
        save(
            settings.copy(
                logoOffsetXPercent =
                    changeRoyalTVOffsetXPercent(
                        currentPercent =
                            settings.logoOffsetXPercent,
                        increase = increase
                    )
            )
        )
    }

    fun changeLogoY(
        increase: Boolean
    ) {
        save(
            settings.copy(
                logoOffsetYPercent =
                    changeRoyalTVOffsetYPercent(
                        currentPercent =
                            settings.logoOffsetYPercent,
                        increase = increase
                    )
            )
        )
    }

    fun changeTickerSize(
        increase: Boolean
    ) {
        save(
            settings.copy(
                tickerSizePercent =
                    changeRoyalTVSizePercent(
                        currentPercent =
                            settings.tickerSizePercent,
                        increase = increase
                    )
            )
        )
    }

    fun changeTimeSize(
        increase: Boolean
    ) {
        save(
            settings.copy(
                timeSizePercent =
                    changeRoyalTVSizePercent(
                        currentPercent =
                            settings.timeSizePercent,
                        increase = increase
                    )
            )
        )
    }

    fun changeDateSize(
        increase: Boolean
    ) {
        save(
            settings.copy(
                dateSizePercent =
                    changeRoyalTVSizePercent(
                        currentPercent =
                            settings.dateSizePercent,
                        increase = increase
                    )
            )
        )
    }

    fun changeLiveSize(
        increase: Boolean
    ) {
        save(
            settings.copy(
                liveSizePercent =
                    changeRoyalTVSizePercent(
                        currentPercent =
                            settings.liveSizePercent,
                        increase = increase
                    )
            )
        )
    }

    fun changeLiveX(
        increase: Boolean
    ) {
        save(
            settings.copy(
                liveOffsetXPercent =
                    changeRoyalTVOffsetXPercent(
                        currentPercent =
                            settings.liveOffsetXPercent,
                        increase = increase
                    )
            )
        )
    }

    fun changeLiveY(
        increase: Boolean
    ) {
        save(
            settings.copy(
                liveOffsetYPercent =
                    changeRoyalTVOffsetYPercent(
                        currentPercent =
                            settings.liveOffsetYPercent,
                        increase = increase
                    )
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RoyalBlack)
            .padding(18.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalPanel,
                    contentColor = RoyalGoldLight
                )
            ) {
                Text("← رجوع")
            }

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Text(
                text = "👑 تخطيط ROYAL TV",
                color = RoyalGold,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
        Button(
    onClick = {
        directEditorOpen = true
    },
    modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
    colors = ButtonDefaults.buttonColors(
        containerColor = RoyalGold,
        contentColor = RoyalBlack
    )
) {
    Text(
        text = "✏️ تعديل مباشر",
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold
    )
        }

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            item {
                RoyalTVLayoutSectionTitle(
                    title = "👑 الشعار",
                    description =
                        "الحجم والموقع. لا يتم تغيير طبقات PNG أو حركة الشعار."
                )
            }

            item {
                RoyalTVSizeControlCard(
                    title = "حجم الشعار",
                    value = settings.logoSizePercent,
                    onDecrease = {
                        changeLogoSize(false)
                    },
                    onReset = {
                        save(
                            settings.copy(
                                logoSizePercent =
                                    ROYAL_TV_DEFAULT_LOGO_SIZE_PERCENT
                            )
                        )
                    },
                    onIncrease = {
                        changeLogoSize(true)
                    }
                )
            }

            item {
                RoyalTVPositionControlCard(
                    title = "الموضع الأفقي للشعار",
                    value = settings.logoOffsetXPercent,
                    negativeLabel = "← يسار",
                    positiveLabel = "يمين →",
                    onNegative = {
                        changeLogoX(false)
                    },
                    onCenter = {
                        save(
                            settings.copy(
                                logoOffsetXPercent =
                                    ROYAL_TV_DEFAULT_LOGO_OFFSET_X_PERCENT
                            )
                        )
                    },
                    onPositive = {
                        changeLogoX(true)
                    }
                )
            }

            item {
                RoyalTVPositionControlCard(
                    title = "الموضع العمودي للشعار",
                    value = settings.logoOffsetYPercent,
                    negativeLabel = "↑ أعلى",
                    positiveLabel = "أسفل ↓",
                    onNegative = {
                        changeLogoY(false)
                    },
                    onCenter = {
                        save(
                            settings.copy(
                                logoOffsetYPercent =
                                    ROYAL_TV_DEFAULT_LOGO_OFFSET_Y_PERCENT
                            )
                        )
                    },
                    onPositive = {
                        changeLogoY(true)
                    }
                )
            }

            item {
                RoyalTVLayoutSectionTitle(
                    title = "📰 شريط الأخبار",
                    description =
                        "الحجم فقط. النص الطويل يبقى داخل الشريط ويتحرك باستمرار."
                )
            }

            item {
                RoyalTVSizeControlCard(
                    title = "حجم شريط الأخبار",
                    value = settings.tickerSizePercent,
                    onDecrease = {
                        changeTickerSize(false)
                    },
                    onReset = {
                        save(
                            settings.copy(
                                tickerSizePercent =
                                    ROYAL_TV_DEFAULT_TICKER_SIZE_PERCENT
                            )
                        )
                    },
                    onIncrease = {
                        changeTickerSize(true)
                    }
                )
            }

            item {
                RoyalTVLayoutSectionTitle(
                    title = "🕐 الوقت والتاريخ",
                    description =
                        "يمكن ضبط حجم الوقت والتاريخ كلٌ على حدة."
                )
            }

            item {
                RoyalTVSizeControlCard(
                    title = "حجم الوقت",
                    value = settings.timeSizePercent,
                    onDecrease = {
                        changeTimeSize(false)
                    },
                    onReset = {
                        save(
                            settings.copy(
                                timeSizePercent =
                                    ROYAL_TV_DEFAULT_TIME_SIZE_PERCENT
                            )
                        )
                    },
                    onIncrease = {
                        changeTimeSize(true)
                    }
                )
            }

            item {
                RoyalTVSizeControlCard(
                    title = "حجم التاريخ",
                    value = settings.dateSizePercent,
                    onDecrease = {
                        changeDateSize(false)
                    },
                    onReset = {
                        save(
                            settings.copy(
                                dateSizePercent =
                                    ROYAL_TV_DEFAULT_DATE_SIZE_PERCENT
                            )
                        )
                    },
                    onIncrease = {
                        changeDateSize(true)
                    }
                )
            }

            /*
             * ----------------------------------------------------
             * إظهار / إخفاء التاريخ
             * ----------------------------------------------------
             *
             * لا يغير حجم التاريخ.
             * فقط يبدل ظهوره على شاشة Royal TV.
             *
             * الحالة محفوظة داخل RoyalTVLayoutSettings.
             */
            item {
                Button(
                    onClick = {
                        save(
                            settings.copy(
                                showDate = !settings.showDate
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            if (settings.showDate) {
                                RoyalGold
                            } else {
                                RoyalPanel
                            },
                        contentColor =
                            if (settings.showDate) {
                                RoyalBlack
                            } else {
                                RoyalGoldLight
                            }
                    )
                ) {
                    Text(
                        text = "📅 إظهار التاريخ / إخفاء التاريخ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                RoyalTVLayoutSectionTitle(
                    title = "🔴 مباشر",
                    description =
                        "الحجم والموقع لمؤشر البث."
                )
            }

            item {
                RoyalTVSizeControlCard(
                    title = "حجم مباشر",
                    value = settings.liveSizePercent,
                    onDecrease = {
                        changeLiveSize(false)
                    },
                    onReset = {
                        save(
                            settings.copy(
                                liveSizePercent =
                                    ROYAL_TV_DEFAULT_LIVE_SIZE_PERCENT
                            )
                        )
                    },
                    onIncrease = {
                        changeLiveSize(true)
                    }
                )
            }

            item {
                RoyalTVPositionControlCard(
                    title = "الموضع الأفقي لمباشر",
                    value = settings.liveOffsetXPercent,
                    negativeLabel = "← يسار",
                    positiveLabel = "يمين →",
                    onNegative = {
                        changeLiveX(false)
                    },
                    onCenter = {
                        save(
                            settings.copy(
                                liveOffsetXPercent =
                                    ROYAL_TV_DEFAULT_LIVE_OFFSET_X_PERCENT
                            )
                        )
                    },
                    onPositive = {
                        changeLiveX(true)
                    }
                )
            }

            item {
                RoyalTVPositionControlCard(
                    title = "الموضع العمودي لمباشر",
                    value = settings.liveOffsetYPercent,
                    negativeLabel = "↑ أعلى",
                    positiveLabel = "أسفل ↓",
                    onNegative = {
                        changeLiveY(false)
                    },
                    onCenter = {
                        save(
                            settings.copy(
                                liveOffsetYPercent =
                                    ROYAL_TV_DEFAULT_LIVE_OFFSET_Y_PERCENT
                            )
                        )
                    },
                    onPositive = {
                        changeLiveY(true)
                    }
                )
            }

            item {
                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Button(
                    onClick = {
                        resetRoyalTVLayoutSettings(
                            context
                        )

                        settings =
                            loadRoyalTVLayoutSettings(
                                context
                            )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalGold,
                        contentColor = RoyalBlack
                    )
                ) {
                    Text(
                        text = "♻️ استعادة كل التخطيط الأساسي",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.height(10.dp)
                )
            }
        }
    }
}

/**
 * عنوان قسم داخل مركز التحكم.
 */
@Composable
private fun RoyalTVLayoutSectionTitle(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = RoyalGold.copy(
                alpha = 0.08f
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = title,
                color = RoyalGold,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = description,
                color = RoyalCream,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * بطاقة تحكم بالحجم.
 */
@Composable
private fun RoyalTVSizeControlCard(
    title: String,
    value: Int,
    onDecrease: () -> Unit,
    onReset: () -> Unit,
    onIncrease: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = RoyalPanel
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = title,
                color = RoyalGoldLight,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "$value%",
                color = RoyalGold,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Button(
                    onClick = onDecrease,
                    enabled = value > 60,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalPanel,
                        contentColor = RoyalGoldLight
                    )
                ) {
                    Text("− 5%")
                }

                Button(
                    onClick = onReset,
                    modifier = Modifier.weight(1.35f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalGold,
                        contentColor = RoyalBlack
                    )
                ) {
                    Text("100%")
                }

                Button(
                    onClick = onIncrease,
                    enabled = value < 160,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalPanel,
                        contentColor = RoyalGoldLight
                    )
                ) {
                    Text("+ 5%")
                }
            }
        }
    }
}

/**
 * بطاقة تحكم بالموقع.
 *
 * القيمة:
 *
 * -100 ← يسار / أعلى
 * 0    ← الوضع الأساسي
 * +100 → يمين / أسفل
 */
@Composable
private fun RoyalTVPositionControlCard(
    title: String,
    value: Int,
    negativeLabel: String,
    positiveLabel: String,
    onNegative: () -> Unit,
    onCenter: () -> Unit,
    onPositive: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = RoyalPanel
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = title,
                color = RoyalGoldLight,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text =
                    if (value > 0) {
                        "+$value%"
                    } else {
                        "$value%"
                    },
                color = RoyalGold,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Button(
                    onClick = onNegative,
                    enabled = value > -100,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalPanel,
                        contentColor = RoyalGoldLight
                    )
                ) {
                    Text(negativeLabel)
                }

                Button(
                    onClick = onCenter,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalGold,
                        contentColor = RoyalBlack
                    )
                ) {
                    Text("0")
                }

                Button(
                    onClick = onPositive,
                    enabled = value < 100,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalPanel,
                        contentColor = RoyalGoldLight
                    )
                ) {
                    Text(positiveLabel)
                }
            }
        }
    }
}

/**
 * بطاقة عنصر من القائمة الرئيسية.
 */
@Composable
private fun RoyalTVControlCard(
    item: RoyalTVControlItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = RoyalPanel
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 15.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = item.icon,
                fontSize = 30.sp
            )

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = item.title,
                    color = RoyalGoldLight,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = item.description,
                    color = RoyalCream,
                    fontSize = 13.sp
                )
            }
        }
    }
}
