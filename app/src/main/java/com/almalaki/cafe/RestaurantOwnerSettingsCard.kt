package com.almalaki.cafe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RestaurantOwnerSettingsCard(
    restaurant: OwnerRestaurantDetails?,
    loading: Boolean,
    saving: Boolean,
    message: String,
    onRefresh: () -> Unit,
    onCreate: (String) -> Unit
) {
    val context = LocalContext.current
    var restaurantName by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AdminPanel)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "هوية المطعم ورمزه الدائم",
                color = AdminGold,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "هذا الرمز يربط أجهزة العملاء بقائمة مطعمك. يبقى ثابتًا ما لم تُنفّذ عملية تغيير مخصصة له.",
                color = AdminCream,
                fontSize = 13.sp
            )

            if (loading) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(22.dp),
                        color = AdminGold,
                        strokeWidth = 2.dp
                    )
                    Text("جاري تحميل بيانات المطعم...", color = AdminCream)
                }
            } else if (restaurant == null) {
                Text(
                    text = "لا يوجد مطعم مرتبط بهذا الحساب حتى الآن.",
                    color = AdminCream,
                    fontSize = 14.sp
                )

                OutlinedTextField(
                    value = restaurantName,
                    onValueChange = { restaurantName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("اسم المطعم") },
                    placeholder = { Text("اكتب اسم المطعم") },
                    singleLine = true,
                    enabled = !saving
                )

                Button(
                    onClick = {
                        onCreate(restaurantName.trim())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !saving && restaurantName.trim().length in 2..100,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AdminGold,
                        contentColor = AdminBlack
                    )
                ) {
                    Text(
                        if (saving) "جاري إنشاء المطعم..." else "إنشاء مطعمي ورمز العميل",
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "اسم المطعم: \${restaurant.name}",
                    color = AdminCream,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = if (restaurant.status == "active") {
                        "الحالة: نشط"
                    } else {
                        "الحالة: موقوف مؤقتًا"
                    },
                    color = if (restaurant.status == "active") {
                        Color(0xFF80D8A5)
                    } else {
                        Color(0xFFE9A23B)
                    },
                    fontSize = 13.sp
                )

                Text(
                    text = restaurant.joinCode,
                    color = AdminGold,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        val clipboard = context.getSystemService(
                            Context.CLIPBOARD_SERVICE
                        ) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                "Royal restaurant code",
                                restaurant.joinCode
                            )
                        )
                        copied = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AdminGold,
                        contentColor = AdminBlack
                    )
                ) {
                    Text(if (copied) "تم نسخ الرمز ✓" else "نسخ رمز المطعم")
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "أرسل هذا الرمز إلى عملائك؛ يُدخلونه عند فتح التطبيق لعرض قائمة مطعمك.",
                    color = AdminCream,
                    fontSize = 12.sp
                )
            }

            if (message.isNotBlank()) {
                Text(
                    text = message,
                    color = if (message.contains("تعذر") || message.contains("خطأ")) {
                        Color(0xFFFF8A80)
                    } else {
                        AdminGold
                    },
                    fontSize = 13.sp
                )
            }

            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading && !saving
            ) {
                Text("تحديث بيانات المطعم")
            }
        }
    }
}
