package com.almalaki.cafe

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val CUSTOMER_PROFILE_PREFS = "royal_customer_profile"
private const val CUSTOMER_ORDERS_PREFS = "royal_customer_orders"
private const val CUSTOMER_ORDERS_KEY = "orders_json"

private val CustomerRoyalGold = Color(0xFFD4AF37)
private val CustomerRoyalGoldLight = Color(0xFFFFE9A3)
private val CustomerRoyalBlack = Color(0xFF111111)
private val CustomerRoyalCream = Color(0xFFF5F0E5)
private val CustomerRoyalText = Color(0xFF171717)
private val CustomerRoyalMuted = Color(0xFF777777)

data class RoyalCustomerProfile(
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val fulfillmentType: String = "داخل المحل"
)

data class RoyalCustomerOrderRecord(
    val orderNumber: String,
    val customerName: String,
    val customerPhone: String,
    val address: String,
    val fulfillmentType: String,
    val totalAmount: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val items: List<RoyalInvoiceItem> = emptyList()
)

fun loadRoyalCustomerProfile(context: Context): RoyalCustomerProfile {
    val preferences = context.getSharedPreferences(
        CUSTOMER_PROFILE_PREFS,
        Context.MODE_PRIVATE
    )

    return RoyalCustomerProfile(
        name = preferences.getString("name", "").orEmpty(),
        phone = preferences.getString("phone", "").orEmpty(),
        address = preferences.getString("address", "").orEmpty(),
        fulfillmentType = preferences.getString(
            "fulfillment_type",
            "داخل المحل"
        ).orEmpty().ifBlank { "داخل المحل" }
    )
}

fun saveRoyalCustomerProfile(
    context: Context,
    profile: RoyalCustomerProfile
) {
    context.getSharedPreferences(
        CUSTOMER_PROFILE_PREFS,
        Context.MODE_PRIVATE
    ).edit()
        .putString("name", profile.name.trim())
        .putString("phone", profile.phone.trim())
        .putString("address", profile.address.trim())
        .putString("fulfillment_type", profile.fulfillmentType)
        .apply()
}

fun loadRoyalCustomerOrders(context: Context): List<RoyalCustomerOrderRecord> {
    val raw = context.getSharedPreferences(
        CUSTOMER_ORDERS_PREFS,
        Context.MODE_PRIVATE
    ).getString(CUSTOMER_ORDERS_KEY, "[]") ?: "[]"

    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val order = array.optJSONObject(index) ?: continue
                val itemArray = order.optJSONArray("items") ?: JSONArray()
                val items = buildList {
                    for (itemIndex in 0 until itemArray.length()) {
                        val item = itemArray.optJSONObject(itemIndex) ?: continue
                        add(
                            RoyalInvoiceItem(
                                name = item.optString("name"),
                                quantity = item.optInt("quantity"),
                                unitPrice = item.optDouble("unitPrice")
                            )
                        )
                    }
                }

                add(
                    RoyalCustomerOrderRecord(
                        orderNumber = order.optString("orderNumber"),
                        customerName = order.optString("customerName"),
                        customerPhone = order.optString("customerPhone"),
                        address = order.optString("address"),
                        fulfillmentType = order.optString("fulfillmentType"),
                        totalAmount = order.optDouble("totalAmount"),
                        createdAt = order.optLong("createdAt"),
                        items = items
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

fun saveRoyalCustomerOrder(
    context: Context,
    newOrder: RoyalCustomerOrderRecord
) {
    val savedOrders = loadRoyalCustomerOrders(context)
        .filterNot { it.orderNumber == newOrder.orderNumber }

    // يحتفظ التطبيق بآخر 30 طلبًا على الجهاز.
    val ordersToSave = (listOf(newOrder) + savedOrders).take(30)
    val array = JSONArray()

    ordersToSave.forEach { order ->
        val items = JSONArray()
        order.items.forEach { item ->
            items.put(
                JSONObject()
                    .put("name", item.name)
                    .put("quantity", item.quantity)
                    .put("unitPrice", item.unitPrice)
            )
        }

        array.put(
            JSONObject()
                .put("orderNumber", order.orderNumber)
                .put("customerName", order.customerName)
                .put("customerPhone", order.customerPhone)
                .put("address", order.address)
                .put("fulfillmentType", order.fulfillmentType)
                .put("totalAmount", order.totalAmount)
                .put("createdAt", order.createdAt)
                .put("items", items)
        )
    }

    context.getSharedPreferences(
        CUSTOMER_ORDERS_PREFS,
        Context.MODE_PRIVATE
    ).edit()
        .putString(CUSTOMER_ORDERS_KEY, array.toString())
        .apply()
}

@Composable
fun RoyalCustomerAccountDialog(
    isDarkMode: Boolean,
    profile: RoyalCustomerProfile,
    onDismiss: () -> Unit,
    onSave: (RoyalCustomerProfile) -> Unit,
    onManageRestaurants: () -> Unit
) {
    val panelColor = if (isDarkMode) CustomerRoyalBlack else Color.White
    val primaryText = if (isDarkMode) Color.White else CustomerRoyalText
    val secondaryText = if (isDarkMode) Color(0xFFBDBDBD) else CustomerRoyalMuted

    var name by remember(profile) { mutableStateOf(profile.name) }
    var phone by remember(profile) { mutableStateOf(profile.phone) }
    var address by remember(profile) { mutableStateOf(profile.address) }
    var fulfillmentType by remember(profile) {
        mutableStateOf(profile.fulfillmentType.ifBlank { "داخل المحل" })
    }
    var validationMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = panelColor,
        title = {
            Text(
                text = "حسابي",
                color = CustomerRoyalGold,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "احفظ بياناتك لتسهيل تجهيز طلباتك القادمة.",
                    color = secondaryText,
                    fontSize = 13.sp
                )

                TextButton(
                    onClick = onManageRestaurants,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "مطاعمي المرتبطة وتبديل المطعم",
                        color = CustomerRoyalGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; validationMessage = "" },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("اسم العميل") }
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; validationMessage = "" },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("رقم الهاتف") }
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("عنوان التوصيل (اختياري)") }
                )

                Text(
                    text = "طريقة الطلب الافتراضية",
                    color = primaryText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { fulfillmentType = "داخل المحل" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (fulfillmentType == "داخل المحل") "✓ داخل المحل" else "داخل المحل",
                            color = if (fulfillmentType == "داخل المحل") CustomerRoyalGold else primaryText
                        )
                    }

                    TextButton(
                        onClick = { fulfillmentType = "توصيل إلى المنزل" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (fulfillmentType == "توصيل إلى المنزل") "✓ توصيل" else "توصيل",
                            color = if (fulfillmentType == "توصيل إلى المنزل") CustomerRoyalGold else primaryText
                        )
                    }
                }

                if (validationMessage.isNotBlank()) {
                    Text(
                        text = validationMessage,
                        color = Color(0xFFD32F2F),
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "تُحفظ بيانات الحساب وسجل الطلبات على هذا الجهاز.",
                    color = secondaryText,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        validationMessage = "اكتب اسم العميل أولًا."
                    } else if (phone.isBlank()) {
                        validationMessage = "اكتب رقم الهاتف أولًا."
                    } else {
                        onSave(
                            RoyalCustomerProfile(
                                name = name.trim(),
                                phone = phone.trim(),
                                address = address.trim(),
                                fulfillmentType = fulfillmentType
                            )
                        )
                    }
                }
            ) {
                Text("حفظ البيانات", color = CustomerRoyalGold, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = secondaryText)
            }
        }
    )
}

@Composable
fun RoyalCustomerOrdersDialog(
    isDarkMode: Boolean,
    orders: List<RoyalCustomerOrderRecord>,
    onDismiss: () -> Unit
) {
    val panelColor = if (isDarkMode) CustomerRoyalBlack else Color.White
    val primaryText = if (isDarkMode) Color.White else CustomerRoyalText
    val secondaryText = if (isDarkMode) Color(0xFFBDBDBD) else CustomerRoyalMuted

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = panelColor,
        title = {
            Text(
                text = "طلباتي",
                color = CustomerRoyalGold,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (orders.isEmpty()) {
                Text(
                    text = "لا توجد طلبات محفوظة بعد. ستظهر هنا الطلبات التي ترسلها من هذا الجهاز.",
                    color = secondaryText,
                    fontSize = 14.sp
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 470.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    orders.forEach { order ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = CustomerRoyalGold.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(
                                    color = if (isDarkMode) Color(0xFF19151D) else CustomerRoyalCream,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "رقم الطلب: ${order.orderNumber}",
                                color = CustomerRoyalGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            Text(
                                text = "التاريخ: ${SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.getDefault()).format(Date(order.createdAt))}",
                                color = secondaryText,
                                fontSize = 12.sp
                            )

                            Text(
                                text = "النوع: ${order.fulfillmentType}",
                                color = primaryText,
                                fontSize = 12.sp
                            )

                            Text(
                                text = "الإجمالي: ${String.format(Locale.US, "%.2f", order.totalAmount)}",
                                color = primaryText,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )

                            order.items.forEach { item ->
                                Text(
                                    text = "• ${item.name} × ${item.quantity}",
                                    color = secondaryText,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = CustomerRoyalGold)
            }
        }
    )
}


private enum class RoyalAccountPage {
    HOME, REGISTER_METHODS, SIGN_IN, EMAIL_REGISTER, FACEBOOK, WHATSAPP
}

/**
 * بوابة الحساب المستقلة. لا تدّعي نجاح المصادقة قبل تهيئة مزوّد تسجيل الدخول.
 */
@Composable
fun RoyalCustomerAccountPortal(
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onCustomerDetails: () -> Unit
) {
    var page by remember { mutableStateOf(RoyalAccountPage.HOME) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val panelColor = if (isDarkMode) CustomerRoyalBlack else Color.White
    val primaryText = if (isDarkMode) Color.White else CustomerRoyalText
    val secondaryText = if (isDarkMode) Color(0xFFBDBDBD) else CustomerRoyalMuted

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = panelColor,
        title = {
            Text(
                text = when (page) {
                    RoyalAccountPage.HOME -> "حسابي"
                    RoyalAccountPage.REGISTER_METHODS -> "إنشاء حساب"
                    RoyalAccountPage.SIGN_IN -> "الدخول إلى حسابي"
                    RoyalAccountPage.EMAIL_REGISTER -> "التسجيل بالبريد الإلكتروني"
                    RoyalAccountPage.FACEBOOK -> "الربط عبر Facebook"
                    RoyalAccountPage.WHATSAPP -> "الربط عبر WhatsApp"
                },
                color = CustomerRoyalGold,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (page) {
                    RoyalAccountPage.HOME -> {
                        Text(
                            "مرحبًا بك في حساب الملكي. اختر ما تريد دون ازدحام الخيارات.",
                            color = secondaryText,
                            fontSize = 13.sp
                        )
                        AccountPortalButton("إنشاء حساب", primaryText) {
                            statusMessage = ""
                            page = RoyalAccountPage.REGISTER_METHODS
                        }
                        AccountPortalButton("الدخول إلى حسابي", primaryText) {
                            statusMessage = ""
                            page = RoyalAccountPage.SIGN_IN
                        }
                        AccountPortalButton("بيانات العميل والمطاعم المرتبطة", primaryText) {
                            onCustomerDetails()
                        }
                    }
                    RoyalAccountPage.REGISTER_METHODS -> {
                        Text("اختر وسيلة التسجيل؛ ستظهر تفاصيل الوسيلة في صفحة مستقلة.", color = secondaryText, fontSize = 13.sp)
                        AccountPortalButton("البريد الإلكتروني", primaryText) {
                            email = ""; password = ""; statusMessage = ""
                            page = RoyalAccountPage.EMAIL_REGISTER
                        }
                        AccountPortalButton("Facebook", primaryText) {
                            statusMessage = "تسجيل Facebook غير مفعّل بعد. يلزم إعداد مزوّد OAuth والتحقق من إعداداته قبل استخدامه."
                            page = RoyalAccountPage.FACEBOOK
                        }
                        AccountPortalButton("WhatsApp", primaryText) {
                            statusMessage = "ربط WhatsApp غير مفعّل بعد. يلزم إعداد تحقق رسمي لرقم الهاتف؛ لن نعتبر مجرد إدخال الرقم تسجيلًا موثّقًا."
                            page = RoyalAccountPage.WHATSAPP
                        }
                    }
                    RoyalAccountPage.SIGN_IN -> {
                        Text("تسجيل الدخول يتم عبر خادم Supabase. إذا كان تأكيد البريد مطلوبًا، يجب تأكيده أولًا.", color = secondaryText, fontSize = 13.sp)
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; statusMessage = "" },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("البريد الإلكتروني") }
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; statusMessage = "" },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("كلمة المرور") }
                        )
                        TextButton(
                            enabled = !isBusy,
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    statusMessage = "أدخل البريد الإلكتروني وكلمة المرور."
                                } else {
                                    isBusy = true
                                    statusMessage = ""
                                    coroutineScope.launch {
                                        try {
                                            RoyalSupabaseAuth.signIn(email, password)
                                            statusMessage = "تم قبول تسجيل الدخول من الخادم."
                                        } catch (error: Exception) {
                                            statusMessage = error.message?.take(220)
                                                ?: "تعذر تسجيل الدخول. تحقق من البيانات والاتصال."
                                        } finally {
                                            isBusy = false
                                        }
                                    }
                                }
                            }
                        ) { Text(if (isBusy) "جارٍ الاتصال..." else "تسجيل الدخول", color = CustomerRoyalGold) }
                        TextButton(
                            enabled = !isBusy,
                            onClick = {
                                if (email.isBlank()) {
                                    statusMessage = "أدخل بريدك الإلكتروني لاستعادة كلمة المرور."
                                } else {
                                    isBusy = true
                                    statusMessage = ""
                                    coroutineScope.launch {
                                        try {
                                            RoyalSupabaseAuth.sendPasswordReset(email)
                                            statusMessage = "استجاب الخادم لطلب الاستعادة. تحقق من بريدك إذا كان إرسال الرسائل مفعّلًا."
                                        } catch (error: Exception) {
                                            statusMessage = error.message?.take(220)
                                                ?: "تعذر إرسال طلب الاستعادة."
                                        } finally {
                                            isBusy = false
                                        }
                                    }
                                }
                            }
                        ) { Text("نسيت كلمة المرور؟", color = CustomerRoyalGold) }
                        if (statusMessage.isNotBlank()) {
                            Text(statusMessage, color = secondaryText, fontSize = 12.sp)
                        }
                    }
                    RoyalAccountPage.EMAIL_REGISTER -> {
                        Text("أنشئ حسابًا على خادم Supabase باستخدام بريدك وكلمة مرور قوية.", color = secondaryText, fontSize = 13.sp)
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; statusMessage = "" },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("البريد الإلكتروني") }
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; statusMessage = "" },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("كلمة المرور") }
                        )
                        TextButton(
                            enabled = !isBusy,
                            onClick = {
                                if (email.isBlank() || password.length < 8) {
                                    statusMessage = "أدخل بريدًا صحيحًا وكلمة مرور لا تقل عن 8 أحرف."
                                } else {
                                    isBusy = true
                                    statusMessage = ""
                                    coroutineScope.launch {
                                        try {
                                            RoyalSupabaseAuth.register(email, password)
                                            statusMessage = "استجاب الخادم لطلب التسجيل. إذا كان تأكيد البريد مفعّلًا، افتح رسالة التأكيد قبل تسجيل الدخول."
                                        } catch (error: Exception) {
                                            statusMessage = error.message?.take(220)
                                                ?: "تعذر إنشاء الحساب. حاول مجددًا."
                                        } finally {
                                            isBusy = false
                                        }
                                    }
                                }
                            }
                        ) { Text(if (isBusy) "جارٍ التسجيل..." else "إنشاء الحساب", color = CustomerRoyalGold) }
                        if (statusMessage.isNotBlank()) {
                            Text(statusMessage, color = secondaryText, fontSize = 12.sp)
                        }
                    }
                    RoyalAccountPage.FACEBOOK,
                    RoyalAccountPage.WHATSAPP -> {
                        Text(statusMessage, color = secondaryText, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when (page) {
                    RoyalAccountPage.HOME -> onDismiss()
                    RoyalAccountPage.REGISTER_METHODS,
                    RoyalAccountPage.SIGN_IN -> page = RoyalAccountPage.HOME
                    RoyalAccountPage.EMAIL_REGISTER,
                    RoyalAccountPage.FACEBOOK,
                    RoyalAccountPage.WHATSAPP -> page = RoyalAccountPage.REGISTER_METHODS
                }
            }) {
                Text(
                    if (page == RoyalAccountPage.HOME) "إغلاق" else "رجوع",
                    color = CustomerRoyalGold,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            if (page != RoyalAccountPage.HOME) {
                TextButton(onClick = onDismiss) {
                    Text("إغلاق", color = secondaryText)
                }
            }
        }
    )
}

@Composable
private fun AccountPortalButton(
    label: String,
    textColor: Color,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = CustomerRoyalGold.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}
