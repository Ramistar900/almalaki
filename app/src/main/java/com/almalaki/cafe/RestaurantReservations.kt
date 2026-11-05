package com.almalaki.cafe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class RoyalReservation(
    val id: Long,
    val customerName: String,
    val customerPhone: String,
    val reservationAt: String,
    val partySize: Int,
    val customerNote: String,
    val status: String,
    val ownerNote: String
)

object RoyalReservationCloud {
    private fun request(token: String, method: String, path: String, body: String? = null, prefer: String? = null): String {
        val connection = URL("${RoyalSupabaseAuth.PROJECT_URL}$path").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("apikey", RoyalSupabaseAuth.PUBLISHABLE_KEY)
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            if (prefer != null) connection.setRequestProperty("Prefer", prefer)
            val code = connection.responseCode
            val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val detail = runCatching { JSONObject(response).optString("message") }.getOrDefault("")
                throw Exception(detail.ifBlank { "تعذر تنفيذ طلب الحجز (HTTP $code)." })
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    fun create(restaurantId: String, name: String, phone: String, date: String, time: String, partySize: Int, note: String) {
        val token = RoyalSupabaseAuth.accessTokenOrNull() ?: throw Exception("سجّل الدخول أولاً.")
        val parsed = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = false }.parse("$date $time")
            ?: throw Exception("أدخل التاريخ والوقت بصيغة صحيحة.")
        if (parsed.time <= System.currentTimeMillis()) throw Exception("اختر موعداً مستقبلياً.")
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(parsed)
        val body = JSONObject()
            .put("p_restaurant_id", restaurantId)
            .put("p_customer_name", name.trim())
            .put("p_customer_phone", phone.trim())
            .put("p_reservation_at", iso)
            .put("p_party_size", partySize)
            .put("p_customer_note", note.trim())
            .toString()
        request(token, "POST", "/rest/v1/rpc/create_restaurant_reservation", body)
    }

    fun loadOwner(token: String, restaurantId: String): List<RoyalReservation> {
        val rows = JSONArray(request(
            token, "GET",
            "/rest/v1/restaurant_reservations?select=id,customer_name,customer_phone,reservation_at,party_size,customer_note,status,owner_note&restaurant_id=eq.$restaurantId&order=reservation_at.asc&limit=200"
        ))
        return buildList {
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                add(RoyalReservation(
                    id = row.optLong("id"),
                    customerName = row.optString("customer_name"),
                    customerPhone = row.optString("customer_phone"),
                    reservationAt = row.optString("reservation_at"),
                    partySize = row.optInt("party_size"),
                    customerNote = row.optString("customer_note"),
                    status = row.optString("status"),
                    ownerNote = row.optString("owner_note")
                ))
            }
        }
    }

    fun updateStatus(token: String, restaurantId: String, reservationId: Long, status: String) {
        val body = JSONObject()
            .put("p_reservation_id", reservationId)
            .put("p_restaurant_id", restaurantId)
            .put("p_status", status)
            .put("p_owner_note", "")
            .toString()
        request(token, "POST", "/rest/v1/rpc/update_restaurant_reservation_status", body)
    }
}

@Composable
fun CustomerReservationDialog(
    restaurantId: String,
    profile: RoyalCustomerProfile,
    onDismiss: () -> Unit,
    onSubmitted: () -> Unit
) {
    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var phone by remember(profile.phone) { mutableStateOf(profile.phone) }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("19:00") }
    var party by remember { mutableStateOf("2") }
    var note by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        containerColor = Color(0xFF111111),
        title = { Text("حجز طاولة", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("أرسل طلب الحجز إلى المطعم، وسيؤكد المالك الموعد.", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                OutlinedTextField(name, { name = it }, label = { Text("الاسم") }, singleLine = true)
                OutlinedTextField(phone, { phone = it }, label = { Text("رقم الهاتف") }, singleLine = true)
                OutlinedTextField(date, { date = it }, label = { Text("التاريخ (YYYY-MM-DD)") }, singleLine = true)
                OutlinedTextField(time, { time = it }, label = { Text("الوقت (HH:MM)") }, singleLine = true)
                OutlinedTextField(party, { party = it.filter(Char::isDigit).take(2) }, label = { Text("عدد الأشخاص (1–20)") }, singleLine = true)
                OutlinedTextField(note, { note = it.take(500) }, label = { Text("ملاحظة (اختياري)") }, minLines = 2)
                if (message.isNotBlank()) Text(message, color = Color(0xFFFFD36A), fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                enabled = !loading && name.isNotBlank() && phone.isNotBlank() && date.isNotBlank() && party.toIntOrNull()?.let { it in 1..20 } == true,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37), contentColor = Color.Black),
                onClick = {
                    loading = true
                    message = ""
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                RoyalReservationCloud.create(restaurantId, name, phone, date, time, party.toInt(), note)
                            }
                            onSubmitted()
                        } catch (error: Exception) {
                            message = error.message ?: "تعذر إرسال الحجز."
                        } finally {
                            loading = false
                        }
                    }
                }
            ) { Text(if (loading) "جارٍ الإرسال..." else "إرسال طلب الحجز") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) { Text("إلغاء", color = Color.White) }
        }
    )
}

@Composable
fun OwnerReservationsSection(
    accessToken: String,
    restaurantId: String,
    modifier: Modifier = Modifier
) {
    var reservations by remember(restaurantId) { mutableStateOf<List<RoyalReservation>>(emptyList()) }
    var loading by remember(restaurantId) { mutableStateOf(false) }
    var message by remember(restaurantId) { mutableStateOf("") }

    fun refresh() {
        if (restaurantId.isBlank()) return
        loading = true
        Thread {
            try {
                val loaded = RoyalReservationCloud.loadOwner(accessToken, restaurantId)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    reservations = loaded
                    loading = false
                    message = ""
                }
            } catch (error: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    loading = false
                    message = error.message ?: "تعذر تحميل الحجوزات."
                }
            }
        }.start()
    }

    androidx.compose.runtime.LaunchedEffect(restaurantId) {
        refresh()
        while (true) {
            kotlinx.coroutines.delay(15000)
            refresh()
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row {
            Text("حجوزات المطعم", color = Color(0xFFD4AF37), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = ::refresh, enabled = !loading) { Text(if (loading) "..." else "تحديث") }
        }
        if (message.isNotBlank()) Text(message, color = Color(0xFFFFD36A))
        if (reservations.isEmpty() && !loading) Text("لا توجد حجوزات حالياً.", color = Color.White)
        reservations.forEach { reservation ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(reservation.customerName, color = Color(0xFFFFE9A3), fontWeight = FontWeight.Bold)
                    Text("الهاتف: ${reservation.customerPhone}", color = Color.White, fontSize = 13.sp)
                    Text("الموعد: ${reservation.reservationAt.replace("T", " ").take(16)}", color = Color.White, fontSize = 13.sp)
                    Text("عدد الأشخاص: ${reservation.partySize}", color = Color.White, fontSize = 13.sp)
                    if (reservation.customerNote.isNotBlank()) Text("ملاحظة العميل: ${reservation.customerNote}", color = Color.LightGray, fontSize = 12.sp)
                    Text("الحالة: ${when (reservation.status) { "pending" -> "بانتظار التأكيد"; "confirmed" -> "مؤكد"; "rejected" -> "مرفوض"; else -> reservation.status }}", color = Color(0xFFD4AF37), fontSize = 13.sp)
                    if (reservation.status == "pending") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    Thread {
                                        try {
                                            RoyalReservationCloud.updateStatus(accessToken, restaurantId, reservation.id, "confirmed")
                                            android.os.Handler(android.os.Looper.getMainLooper()).post { refresh() }
                                        } catch (error: Exception) {
                                            android.os.Handler(android.os.Looper.getMainLooper()).post { message = error.message ?: "تعذر تأكيد الحجز." }
                                        }
                                    }.start()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF146B4A))
                            ) { Text("تأكيد") }
                            OutlinedButton(
                                onClick = {
                                    Thread {
                                        try {
                                            RoyalReservationCloud.updateStatus(accessToken, restaurantId, reservation.id, "rejected")
                                            refresh()
                                        } catch (error: Exception) {
                                            android.os.Handler(android.os.Looper.getMainLooper()).post { message = error.message ?: "تعذر رفض الحجز." }
                                        }
                                    }.start()
                                }
                            ) { Text("رفض") }
                        }
                    }
                }
            }
        }
    }
}
