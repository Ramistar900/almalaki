
package com.almalaki.royaltv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AppsGold = Color(0xFFD4AF37)
private val AppsBlack = Color(0xFF080C12)
private val AppsCard = Color(0xFF171D27)
private val AppsText = Color(0xFFE8EDF3)
private val AppsGreen = Color(0xFF176B60)

@Composable
fun RoyalTVInstalledAppsPanel(
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val apps = remember {
        mutableStateListOf<RoyalTVInstalledApp>()
    }

    var message by remember {
        mutableStateOf("جارٍ تحميل التطبيقات...")
    }

    fun refreshApps() {
        apps.clear()
        apps.addAll(
            RoyalTVInstalledAppsManager.getLaunchableApps(context)
        )
        message = if (apps.isEmpty()) {
            "لم يتم العثور على تطبيقات قابلة للتشغيل."
        } else {
            "اختر تطبيقًا لفتحه على هذا الجهاز."
        }
    }

    LaunchedEffect(Unit) {
        refreshApps()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppsBlack, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "تطبيقات ROYAL TV",
                    color = AppsGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "التطبيقات المثبتة على جهاز العرض الحالي",
                    color = AppsText.copy(alpha = 0.78f),
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = { refreshApps() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppsCard,
                    contentColor = AppsGold
                )
            ) {
                Text("تحديث")
            }
        }

        Text(
            message,
            color = AppsText.copy(alpha = 0.8f),
            fontSize = 13.sp
        )

        if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "ستظهر هنا التطبيقات التي يسمح النظام باكتشافها.",
                    color = AppsText.copy(alpha = 0.65f),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = apps,
                    key = { it.packageName }
                ) { app ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val opened =
                                    RoyalTVInstalledAppsManager.launchApp(
                                        context,
                                        app.packageName
                                    )

                                message = if (opened) {
                                    "أُرسل أمر فتح ${app.label}."
                                } else {
                                    "تعذر فتح ${app.label}."
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AppsCard
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 14.dp,
                                    vertical = 12.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        AppsGold.copy(alpha = 0.14f),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "▶",
                                    color = AppsGold,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    app.label,
                                    color = AppsText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    app.packageName,
                                    color = AppsText.copy(alpha = 0.58f),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                "فتح",
                                color = AppsGold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (onClose != null) {
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppsGreen,
                    contentColor = Color.White
                )
            ) {
                Text("العودة إلى ROYAL TV")
            }
        }
    }
}
