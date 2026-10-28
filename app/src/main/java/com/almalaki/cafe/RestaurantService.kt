package com.almalaki.cafe

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private const val RESTAURANT_SUPABASE_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val RESTAURANT_SUPABASE_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

data class RestaurantStore(
    val id: String,
    val name: String,
    val joinCode: String
)

/**
 * Resolve a customer-facing permanent code without granting customer access
 * to owner-only data. The RPC returns only the active restaurant identity.
 */
fun resolveRestaurantCode(code: String): RestaurantStore {
    val normalizedCode = code.trim().uppercase(Locale.ROOT)

    if (!Regex("^ROYAL-[A-F0-9]{12}$").matches(normalizedCode)) {
        throw Exception("صيغة رمز المطعم غير صحيحة.")
    }

    val connection = URL(
        "$RESTAURANT_SUPABASE_URL/rest/v1/rpc/resolve_restaurant_join_code"
    ).openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.doOutput = true
        connection.setRequestProperty("apikey", RESTAURANT_SUPABASE_KEY)
        connection.setRequestProperty(
            "Authorization",
            "Bearer $RESTAURANT_SUPABASE_KEY"
        )
        connection.setRequestProperty("Content-Type", "application/json")

        val body = JSONObject()
            .put("p_code", normalizedCode)
            .toString()

        connection.outputStream.use {
            it.write(body.toByteArray(Charsets.UTF_8))
        }

        val responseCode = connection.responseCode
        val response = (
            if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
        )?.bufferedReader()?.use { it.readText() }.orEmpty()

        if (responseCode !in 200..299) {
            throw Exception(
                "تعذر التحقق من رمز المطعم (HTTP $responseCode): " +
                    response.ifBlank { "حاول مرة أخرى." }
            )
        }

        val rows = JSONArray(response)
        if (rows.length() == 0) {
            throw Exception("رمز المطعم غير صحيح أو أن المطعم غير متاح.")
        }

        val row = rows.getJSONObject(0)
        return RestaurantStore(
            id = row.getString("restaurant_id"),
            name = row.getString("restaurant_name"),
            joinCode = normalizedCode
        )
    } finally {
        connection.disconnect()
    }
}

fun loadSelectedRestaurant(context: Context): RestaurantStore? {
    val prefs = context.getSharedPreferences(
        "royal_customer_store",
        Context.MODE_PRIVATE
    )

    val id = prefs.getString("restaurant_id", null)?.takeIf { it.isNotBlank() }
        ?: return null
    val name = prefs.getString("restaurant_name", null)?.takeIf { it.isNotBlank() }
        ?: return null
    val code = prefs.getString("join_code", null)?.takeIf { it.isNotBlank() }
        ?: return null

    return RestaurantStore(id = id, name = name, joinCode = code)
}

fun saveSelectedRestaurant(context: Context, store: RestaurantStore) {
    context.getSharedPreferences(
        "royal_customer_store",
        Context.MODE_PRIVATE
    ).edit()
        .putString("restaurant_id", store.id)
        .putString("restaurant_name", store.name)
        .putString("join_code", store.joinCode)
        .apply()
}

fun clearSelectedRestaurant(context: Context) {
    context.getSharedPreferences(
        "royal_customer_store",
        Context.MODE_PRIVATE
    ).edit()
        .clear()
        .apply()
}

data class OwnerRestaurantDetails(
    val id: String,
    val name: String,
    val status: String,
    val joinCode: String
)

private fun ownerRestaurantRequest(
    path: String,
    accessToken: String,
    method: String = "GET",
    body: String? = null,
    authEndpoint: Boolean = false
): String {
    require(accessToken.isNotBlank()) {
        "جلسة المالك غير متوفرة."
    }

    val endpoint = if (authEndpoint) {
        "$RESTAURANT_SUPABASE_URL/$path"
    } else {
        "$RESTAURANT_SUPABASE_URL/rest/v1/$path"
    }

    val connection = URL(endpoint).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = method
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.doOutput = body != null
        connection.setRequestProperty("apikey", RESTAURANT_SUPABASE_KEY)
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", "application/json")

        if (body != null) {
            connection.outputStream.use {
                it.write(body.toByteArray(Charsets.UTF_8))
            }
        }

        val statusCode = connection.responseCode
        val response = (
            if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
        )?.bufferedReader()?.use { it.readText() }.orEmpty()

        if (statusCode !in 200..299) {
            throw Exception(
                "تعذر تحميل بيانات المطعم (HTTP $statusCode): " +
                    response.ifBlank { "حاول مرة أخرى." }
            )
        }

        return response
    } finally {
        connection.disconnect()
    }
}

/** Returns the first active restaurant owned by the signed-in account. */
fun loadOwnedRestaurant(accessToken: String): OwnerRestaurantDetails? {
    val userJson = JSONObject(
        ownerRestaurantRequest(
            path = "auth/v1/user",
            accessToken = accessToken,
            authEndpoint = true
        )
    )
    val userId = userJson.optString("id")
        .takeIf { it.isNotBlank() }
        ?: throw Exception("تعذر تحديد حساب المالك الحالي.")

    val encodedUserId = java.net.URLEncoder.encode(userId, "UTF-8")
    val membershipRows = JSONArray(
        ownerRestaurantRequest(
            path = "restaurant_members?select=restaurant_id" +
                "&user_id=eq.$encodedUserId&role=eq.owner&is_active=eq.true" +
                "&order=created_at.asc&limit=1",
            accessToken = accessToken
        )
    )

    if (membershipRows.length() == 0) return null

    val restaurantId = membershipRows.getJSONObject(0)
        .getString("restaurant_id")
    val encodedRestaurantId =
        java.net.URLEncoder.encode(restaurantId, "UTF-8")

    val restaurantRows = JSONArray(
        ownerRestaurantRequest(
            path = "restaurants?id=eq.$encodedRestaurantId&select=id,name,status&limit=1",
            accessToken = accessToken
        )
    )
    if (restaurantRows.length() == 0) return null

    val codeRows = JSONArray(
        ownerRestaurantRequest(
            path = "restaurant_join_codes?restaurant_id=eq.$encodedRestaurantId" +
                "&select=join_code&limit=1",
            accessToken = accessToken
        )
    )
    if (codeRows.length() == 0) {
        throw Exception("لم يتم العثور على الرمز الدائم لهذا المطعم.")
    }

    val restaurant = restaurantRows.getJSONObject(0)
    return OwnerRestaurantDetails(
        id = restaurant.getString("id"),
        name = restaurant.getString("name"),
        status = restaurant.optString("status", "active"),
        joinCode = codeRows.getJSONObject(0).getString("join_code")
    )
}

/** Creates one restaurant for the authenticated owner account. */
fun createOwnedRestaurant(
    accessToken: String,
    name: String
): OwnerRestaurantDetails {
    val restaurantName = name.trim()
    if (restaurantName.length !in 2..100) {
        throw Exception("اسم المطعم يجب أن يكون بين حرفين و100 حرف.")
    }

    val response = JSONArray(
        ownerRestaurantRequest(
            path = "rpc/create_restaurant",
            accessToken = accessToken,
            method = "POST",
            body = JSONObject()
                .put("p_name", restaurantName)
                .toString()
        )
    )

    if (response.length() == 0) {
        throw Exception("لم تُرجع قاعدة البيانات بيانات المطعم الجديد.")
    }

    val row = response.getJSONObject(0)
    return OwnerRestaurantDetails(
        id = row.getString("restaurant_id"),
        name = row.getString("restaurant_name"),
        status = "active",
        joinCode = row.getString("permanent_code")
    )
}


@Composable
fun RestaurantJoinScreen(
    darkMode: Boolean,
    onRestaurantSelected: (RestaurantStore) -> Unit,
    onOwnerLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val background = if (darkMode) Color(0xFF050505) else Color(0xFFF7F2E8)
    val panel = if (darkMode) Color(0xFF111111) else Color.White
    val primaryText = if (darkMode) Color.White else Color(0xFF222222)
    val secondaryText = if (darkMode) Color(0xFFBDBDBD) else Color(0xFF666666)
    val gold = Color(0xFFD4AF37)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(panel, RoundedCornerShape(22.dp))
                .border(1.dp, gold.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "الملكي",
                color = gold,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "الارتباط بمطعم",
                color = primaryText,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "أدخل الرمز الدائم الذي أعطاك إياه صاحب المطعم لعرض قائمته وإرسال طلباتك إليه.",
                color = secondaryText,
                fontSize = 14.sp
            )
            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it.uppercase(Locale.ROOT).filter { ch ->
                        ch.isLetterOrDigit() || ch == '-'
                    }.take(18)
                    errorMessage = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("رمز المطعم") },
                placeholder = { Text("ROYAL-XXXXXXXXXXXX") },
                singleLine = true,
                enabled = !loading
            )
            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFE05252),
                    fontSize = 13.sp
                )
            }
            Button(
                onClick = onOwnerLogin,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = gold
                )
            ) {
                Text("دخول المالك")
            }

            Button(
                onClick = {
                    loading = true
                    errorMessage = ""
                    val submittedCode = code
                    Thread {
                        try {
                            val store = resolveRestaurantCode(submittedCode)
                            Handler(Looper.getMainLooper()).post {
                                saveSelectedRestaurant(context, store)
                                loading = false
                                onRestaurantSelected(store)
                            }
                        } catch (error: Exception) {
                            Handler(Looper.getMainLooper()).post {
                                errorMessage = error.message ?: "تعذر التحقق من الرمز."
                                loading = false
                            }
                        }
                    }.start()
                },
                enabled = !loading && code.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = gold,
                    contentColor = Color.Black
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(22.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("متابعة إلى القائمة", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
