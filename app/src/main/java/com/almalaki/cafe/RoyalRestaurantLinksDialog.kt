package com.almalaki.cafe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoyalRestaurantLinksDialog(
    isDarkMode: Boolean,
    activeRestaurant: RoyalLinkedRestaurant,
    restaurants: List<RoyalLinkedRestaurant>,
    loading: Boolean,
    message: String,
    onLink: (String) -> Unit,
    onSelect: (RoyalLinkedRestaurant) -> Unit,
    onDismiss: () -> Unit
) {
    val panelColor = if (isDarkMode) Color(0xFF111111) else Color.White
    val primaryText = if (isDarkMode) Color.White else Color(0xFF171717)
    val secondaryText = if (isDarkMode) Color(0xFFBDBDBD) else Color(0xFF777777)
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        containerColor = panelColor,
        title = {
            Text("مطاعمي المرتبطة", color = Color(0xFFD4AF37), fontSize = 21.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "اختر مطعمًا لعرض قائمته، أو أدخل رمزًا دائمًا لربط مطعم جديد.",
                    color = secondaryText,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase(); },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("رمز المطعم، مثل ROYAL-…") }
                )
                TextButton(
                    enabled = !loading && code.isNotBlank(),
                    onClick = { onLink(code.trim()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            color = Color(0xFFD4AF37),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("التحقق والربط", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                    }
                }
                if (message.isNotBlank()) {
                    Text(message, color = if (message.contains("نجاح")) Color(0xFF2E9D65) else Color(0xFFD32F2F), fontSize = 12.sp)
                }
                Text("اختر المطعم الحالي", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                restaurants.forEach { restaurant ->
                    val selected = restaurant.id == activeRestaurant.id
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) Color(0xFFD4AF37) else Color(0xFF777777).copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(
                                if (selected) Color(0xFFD4AF37).copy(alpha = 0.10f) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = !loading && !selected) { onSelect(restaurant) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(restaurant.name, color = if (selected) Color(0xFFD4AF37) else primaryText, fontWeight = FontWeight.SemiBold)
                            if (selected) Text("المطعم المعروض الآن", color = secondaryText, fontSize = 11.sp)
                        }
                        if (selected) Text("✓", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                        else Text("عرض", color = Color(0xFFD4AF37), fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !loading) {
                Text("إغلاق", color = Color(0xFFD4AF37))
            }
        }
    )
}
