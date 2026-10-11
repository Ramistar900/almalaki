package com.almalaki.royaltv

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
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

internal data class RoyalTVInstalledApp(
    val label: String,
    val packageName: String
)

internal object RoyalTVAppDiscovery {

    @Suppress("DEPRECATION")
    fun findLaunchableApps(
        context: Context
    ): List<RoyalTVInstalledApp> {

        val packageManager = context.packageManager
        val found =
            LinkedHashMap<String, RoyalTVInstalledApp>()

        val queries = listOf(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
        )

        for (query in queries) {
            val resolved =
                packageManager.queryIntentActivities(query, 0)

            for (info: ResolveInfo in resolved) {
                val activityInfo = info.activityInfo
                    ?: continue

                val packageName = activityInfo.packageName

                if (packageName == context.packageName) {
                    continue
                }

                val label = info.loadLabel(packageManager)
                    ?.toString()
                    ?.trim()
                    .orEmpty()
                    .ifBlank { packageName }

                if (!found.containsKey(packageName)) {
                    found[packageName] =
                        RoyalTVInstalledApp(
                            label = label,
                            packageName = packageName
                        )
                }
            }
        }

        return found.values.sortedBy {
            it.label.lowercase()
        }
    }

    fun launch(
        context: Context,
        app: RoyalTVInstalledApp
    ) {
        val packageManager = context.packageManager

        val intent =
            packageManager.getLeanbackLaunchIntentForPackage(
                app.packageName
            ) ?: packageManager.getLaunchIntentForPackage(
                app.packageName
            )

        if (intent == null) {
            Toast.makeText(
                context,
                "لا يوجد مدخل تشغيل متاح لهذا التطبيق",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (error: Exception) {
            Toast.makeText(
                context,
                "تعذّر فتح التطبيق: ${app.label}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

@Composable
internal fun RoyalTVAppDiscoveryPanel() {

    val context = LocalContext.current

    var apps by remember {
        mutableStateOf<List<RoyalTVInstalledApp>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    fun refreshApps() {
        loading = true
        apps = RoyalTVAppDiscovery.findLaunchableApps(context)
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
            text = "التطبيقات المتاحة",
            color = Color(0xFFD4AF37),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = "اختر تطبيقًا لاختبار اكتشافه وتشغيله.",
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
                onClick = { refreshApps() }
            ) {
                Text("تحديث")
            }
        }

        if (!loading && apps.isEmpty()) {
            Text(
                text = "لم يعثر النظام على تطبيقات قابلة للتشغيل.",
                color = Color(0xFFDCE5EE),
                fontSize = 13.sp
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                items(
                    items = apps,
                    key = { it.packageName }
                ) { app ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                RoyalTVAppDiscovery.launch(
                                    context,
                                    app
                                )
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
                            verticalArrangement =
                                Arrangement.spacedBy(3.dp)
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
