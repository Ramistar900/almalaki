package com.almalaki.cafe.royaltv

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "مركز التحكم الخاص بالمالك",
            color = RoyalGoldLight,
            fontSize = 15.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = items,
                key = { it.title }
            ) { item ->
                RoyalTVControlCard(
                    item = item,
                    onClick = {
                        onItemSelected(item)
                    }
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.icon,
                fontSize = 30.sp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    color = RoyalGoldLight,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.description,
                    color = RoyalCream,
                    fontSize = 13.sp
                )
            }
        }
    }
}
