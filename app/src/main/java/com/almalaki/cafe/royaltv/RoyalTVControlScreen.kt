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
import com.almalaki.cafe.RoyalTVResponsiveLogo
import com.almalaki.cafe.royaltv.identity.loadRoyalTVLogoSizePercent
import com.almalaki.cafe.royaltv.identity.resetRoyalTVLogoSizePercent
import com.almalaki.cafe.royaltv.identity.saveRoyalTVLogoSizePercent

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
        RoyalTVIdentityControl(
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

/**
 * إعدادات شعار ROYAL TV من مركز التحكم.
 *
 * يستخدم نفس نظام حفظ الهوية الحالي.
 * لا ينشئ SharedPreferences جديدًا.
 */
@Composable
private fun RoyalTVIdentityControl(
    context: Context,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    var sizePercent by remember {
        mutableStateOf(
            loadRoyalTVLogoSizePercent(context)
        )
    }

    fun changeSize(
        increase: Boolean
    ) {
        val newValue =
            RoyalTVResponsiveLogo.applySizeStep(
                currentPercent = sizePercent,
                increase = increase
            )

        if (newValue != sizePercent) {
            sizePercent = newValue

            saveRoyalTVLogoSizePercent(
                context = context,
                percent = newValue
            )
        }
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
                text = "👑 هوية ROYAL TV",
                color = RoyalGold,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(
                20.dp
            ),
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
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text =
                        "يتكيف تلقائيًا مع أبعاد الشاشة مع الحفاظ على نسب الشعار وطبقاته وحركته.",
                    color = RoyalCream,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Text(
                    text = "$sizePercent%",
                    color = RoyalGold,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
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
                            changeSize(false)
                        },
                        enabled =
                            sizePercent >
                                RoyalTVResponsiveLogo.MIN_SIZE_PERCENT,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalPanel,
                            contentColor = RoyalGoldLight
                        )
                    ) {
                        Text("− 5%")
                    }

                    Button(
                        onClick = {
                            resetRoyalTVLogoSizePercent(
                                context
                            )

                            sizePercent =
                                RoyalTVResponsiveLogo.DEFAULT_SIZE_PERCENT
                        },
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalGold,
                            contentColor = RoyalBlack
                        )
                    ) {
                        Text("استعادة 100%")
                    }

                    Button(
                        onClick = {
                            changeSize(true)
                        },
                        enabled =
                            sizePercent <
                                RoyalTVResponsiveLogo.MAX_SIZE_PERCENT,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalPanel,
                            contentColor = RoyalGoldLight
                        )
                    ) {
                        Text("+ 5%")
                    }
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Text(
                    text =
                        "60% الحد الأدنى  •  100% الأساسي  •  160% الحد الأقصى",
                    color = RoyalCream,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

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
