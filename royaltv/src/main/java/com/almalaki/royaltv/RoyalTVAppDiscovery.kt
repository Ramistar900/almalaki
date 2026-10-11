package com.almalaki.royaltv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun RoyalTVAppDiscoveryPanel(
    onAppSelected: ((RoyalTVInstalledApp) -> Unit)? = null
) {
    val context = LocalContext.current

    var apps by remember {
        mutableStateOf<List<RoyalTVInstalledApp>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var selectedApp by remember {
        mutableStateOf<RoyalTVInstalledApp?>(null)
    }

    fun refreshApps() {
        loading = true
        apps = RoyalTVInstalledAppsManager.getLaunchableApps(context)
        loading = false
    }

    LaunchedEffect(Unit) {
        refreshApps()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "تطبيقات ROYAL TV",
            color = Color(0xFFD4AF37),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = "اختر التطبيق الذي تريد عرضه داخل نافذة البث.",
            color = Color(0xFFDCE5EE),
            fontSize = 12.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (loading) {
                    "جارٍ الفحص..."
                } else {
                    "عدد التطبيقات: ${apps.size}"
                },
                color = Color(0xFFD4AF37),
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = { refreshApps() },
                enabled = !loading
            ) {
                Text("تحديث")
            }
        }

        if (selectedApp != null && onAppSelected == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF182333)
                ),
                border = BorderStroke(
                    1.dp,
                    Color(0xFFD4AF37)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "تم اختيار: ${selectedApp!!.label}",
                        color = Color(0xFFD4AF37),
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "سيتم ربط هذا الاختيار بنافذة البث في الخطوة التالية. لم يتم فتح التطبيق خارج ROYAL TV.",
                        color = Color(0xFFDCE5EE),
                        fontSize = 12.sp
                    )

                    Button(
                        onClick = { selectedApp = null }
                    ) {
                        Text("العودة إلى القائمة")
                    }
                }
            }
        }

        if (loading) {
            Text(
                text = "جارٍ البحث عن التطبيقات...",
                color = Color(0xFFDCE5EE),
                fontSize = 13.sp
            )
        } else if (apps.isEmpty()) {
            Text(
                text = "لم يعثر النظام على تطبيقات مثبتة.",
                color = Color(0xFFDCE5EE),
                fontSize = 13.sp
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    items = apps,
                    key = { it.packageName }
                ) { app ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (onAppSelected != null) {
                                    onAppSelected(app)
                                } else {
                                    selectedApp = app
                                }
                            }
                            .focusable(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            Color(0xFFB99A52)
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF111722)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 9.dp
                                ),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = app.label,
                                color = Color(0xFFD4AF37),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = app.packageName,
                                color = Color(0xFFDCE5EE)
                                    .copy(alpha = 0.72f),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
