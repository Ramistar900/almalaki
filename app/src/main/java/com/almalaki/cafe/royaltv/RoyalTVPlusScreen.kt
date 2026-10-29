package com.almalaki.cafe.royaltv

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val PLUS_SUPABASE_URL = "https://duvxxskgdmgrtaleedqu.supabase.co"
private const val PLUS_SUPABASE_KEY = "sb_publishable_L-BfALjb0TkdTlyLTWbFBIg_aez3fjQd"
private const val ROYAL_TV_PACKAGE = "com.almalaki.royaltv"
private const val ROYAL_TV_DOWNLOAD_URL = "https://www.mediafire.com/file/d8pp2tvwpatukzt/royaltv-release.apk/file"

private val PlusGold = Color(0xFFD4AF37)
private val PlusCream = Color(0xFFFFE9A3)
private val PlusBlack = Color(0xFF050505)
private val PlusPanel = Color(0xFF151515)

data class RoyalTVPlusRestaurant(
    val id: String,
    val name: String
)

private fun requestPlusApi(
    accessToken: String,
    path: String,
    method: String = "GET",
    body: JSONObject? = null
): String {
    val connection = (URL("$PLUS_SUPABASE_URL/rest/v1/$path")
        .openConnection() as HttpURLConnection)
    try {
        connection.requestMethod = method
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("apikey", PLUS_SUPABASE_KEY)
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Accept", "application/json")
        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            throw IllegalStateException("تعذر التحقق من Plus (HTTP $code). قد تحتاج قاعدة البيانات إلى تفعيل ميزة الاشتراك أولًا.")
        }
        return response
    } finally {
        connection.disconnect()
    }
}

private fun loadOwnedPlusRestaurants(accessToken: String): List<RoyalTVPlusRestaurant> {
    val response = requestPlusApi(
        accessToken = accessToken,
        path = "restaurants?select=id,name,status&status=eq.active&order=name.asc"
    )
    val json = JSONArray(response)
    return (0 until json.length()).mapNotNull { index ->
        val item = json.optJSONObject(index) ?: return@mapNotNull null
        val id = item.optString("id").trim()
        val name = item.optString("name").trim()
        if (id.isBlank() || name.isBlank()) null else RoyalTVPlusRestaurant(id, name)
    }
}

private fun checkRoyalTVPlus(accessToken: String, restaurantId: String): Boolean {
    val response = requestPlusApi(
        accessToken = accessToken,
        path = "rpc/has_royal_tv_plus",
        method = "POST",
        body = JSONObject().put("p_restaurant_id", restaurantId)
    ).trim()
    return when (response) {
        "true" -> true
        "false" -> false
        else -> throw IllegalStateException("أعاد الخادم نتيجة غير متوقعة للتحقق من Plus.")
    }
}

private fun openOrDownloadRoyalTV(context: Context): Boolean {
    val launchIntent = try {
        context.packageManager.getLaunchIntentForPackage(ROYAL_TV_PACKAGE)
            ?: context.packageManager.getLeanbackLaunchIntentForPackage(ROYAL_TV_PACKAGE)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    if (launchIntent != null) {
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return true
    }

    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ROYAL_TV_DOWNLOAD_URL))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(browserIntent)
    return false
}

@Composable
fun RoyalTVPlusScreen(
    accessToken: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var restaurants by remember { mutableStateOf<List<RoyalTVPlusRestaurant>>(emptyList()) }
    var selectedRestaurantId by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("جارٍ تحميل المطاعم التي يملكها هذا الحساب...") }

    fun refreshRestaurants() {
        if (loading) return
        loading = true
        status = "جارٍ تحميل المطاعم..."
        scope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    loadOwnedPlusRestaurants(accessToken)
                }
                restaurants = loaded
                if (selectedRestaurantId !in loaded.map { it.id }) {
                    selectedRestaurantId = loaded.firstOrNull()?.id.orEmpty()
                }
                status = if (loaded.isEmpty()) {
                    "لم يعثر الخادم على مطعم نشط مرتبط بحساب المالك الحالي."
                } else {
                    "اختر المطعم الذي تريد فحص اشتراكه."
                }
            } catch (error: Exception) {
                status = error.message ?: "تعذر تحميل المطاعم."
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(accessToken) {
        refreshRestaurants()
    }

    Column(
        modifier = modifier.fillMaxSize().background(PlusBlack).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ROYAL TV PLUS",
            color = PlusGold,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "إدارة صلاحية الميزات المتقدمة",
            color = PlusCream,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "المشاهدة والتشغيل الأساسي دون اتصال يبقيان متاحين. انتهاء Plus لا يحذف الوسائط أو إعداداتك.",
            color = PlusCream,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))

        if (loading) {
            CircularProgressIndicator(color = PlusGold)
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(restaurants, key = { it.id }) { restaurant ->
                val selected = selectedRestaurantId == restaurant.id
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) Color(0xFF29220E) else PlusPanel
                    )
                ) {
                    Button(
                        onClick = {
                            selectedRestaurantId = restaurant.id
                            status = "المطعم المحدد: ${restaurant.name}"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = if (selected) PlusGold else PlusCream
                        )
                    ) {
                        Text(
                            text = (if (selected) "✓  " else "") + restaurant.name,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Text(
            text = status,
            color = PlusCream,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
        )

        Button(
            onClick = {
                if (selectedRestaurantId.isBlank() || checking) {
                    status = "اختر مطعمًا نشطًا أولًا."
                } else {
                    checking = true
                    status = "جارٍ التحقق من الاشتراك من الخادم..."
                    scope.launch {
                        try {
                            val active = withContext(Dispatchers.IO) {
                                checkRoyalTVPlus(accessToken, selectedRestaurantId)
                            }
                            if (active) {
                                status = "الاشتراك فعال. جارٍ فتح ROYAL TV أو صفحة التنزيل..."
                                val openedApp = openOrDownloadRoyalTV(context)
                                if (!openedApp) {
                                    status = "ROYAL TV غير مثبت. فتحنا رابط التنزيل الاحتياطي على MediaFire؛ بعد التثبيت ارجع إلى هنا وأعد فحص الاشتراك لمتابعة الربط."
                                }
                            } else {
                                status = "اشتراك Plus غير فعال لهذا المطعم. لم يتم تنفيذ أي عملية دفع؛ ربط بوابة الدفع سيُضاف بعد اختيارها."
                            }
                        } catch (error: Exception) {
                            status = error.message ?: "تعذر التحقق من الاشتراك."
                        } finally {
                            checking = false
                        }
                    }
                }
            },
            enabled = !checking && !loading && selectedRestaurantId.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PlusGold,
                contentColor = PlusBlack
            )
        ) {
            Text(if (checking) "جارٍ التحقق..." else "التحقق والمتابعة إلى ROYAL TV")
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { refreshRestaurants() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = PlusPanel,
                contentColor = PlusCream
            )
        ) {
            Text("تحديث قائمة المطاعم")
        }
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = PlusPanel,
                contentColor = PlusGold
            )
        ) {
            Text("رجوع")
        }
    }
}
