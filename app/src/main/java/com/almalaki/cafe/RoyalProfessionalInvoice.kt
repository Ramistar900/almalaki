package com.almalaki.cafe
import android.content.Intent
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val InvoiceBlack = Color(0xFF050505)
private val InvoicePanel = Color(0xFF111111)
private val InvoicePurple = Color(0xFF1B1024)
private val InvoiceGold = Color(0xFFD4AF37)
private val InvoiceGoldLight = Color(0xFFFFE9A3)
private val InvoiceBronze = Color(0xFF8C6B16)

data class RoyalInvoiceItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Double
) {
    val total: Double
        get() = unitPrice * quantity
}

@Composable
fun RoyalProfessionalInvoice(
    orderNumber: String,
    customerName: String,
    customerPhone: String,
    fulfillmentType: String,
    deliveryAddress: String,
    items: List<RoyalInvoiceItem>,
    totalAmount: Double,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            InvoicePurple,
                            InvoiceBlack
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 2.dp,
                    color = InvoiceGold.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(16.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {

                // =========================
                // رأس الفاتورة
                // =========================

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.Top
                ) {

                    Column {

                        Text(
                            text = "ROYAL COFFEE",
                            color = InvoiceGold,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "فاتورة الطلب",
                            color =
                                InvoiceGoldLight.copy(
                                    alpha = 0.72f
                                ),
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(4.dp))

RoyalDateTime(
    language = "ar",
    style = androidx.compose.ui.text.TextStyle(
        color = InvoiceGoldLight.copy(alpha = 0.70f),
        fontSize = 10.sp
    )
)
                    }

                    Column(
                        horizontalAlignment =
                            Alignment.End
                    ) {

                        Text(
                            text = "رقم الطلب",
                            color =
                                InvoiceGoldLight.copy(
                                    alpha = 0.65f
                                ),
                            fontSize = 10.sp
                        )

                        Text(
                            text = orderNumber,
                            color = InvoiceGoldLight,
                            fontSize = 14.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }

                Spacer(
                    Modifier.height(14.dp)
                )

                InvoiceDivider()

                Spacer(
                    Modifier.height(12.dp)
                )

                // =========================
                // معلومات العميل
                // =========================

                InvoiceInfoRow(
                    label = "العميل",
                    value = customerName
                )

                if (
                    customerPhone
                        .trim()
                        .isNotEmpty()
                ) {

                    InvoiceInfoRow(
                        label = "الهاتف",
                        value = customerPhone
                    )
                }

                InvoiceInfoRow(
                    label = "نوع الطلب",
                    value = fulfillmentType
                )

                if (
                    fulfillmentType ==
                    "توصيل إلى المنزل" &&
                    deliveryAddress
                        .trim()
                        .isNotEmpty()
                ) {

                    InvoiceInfoRow(
                        label = "العنوان",
                        value = deliveryAddress
                    )
                }

                Spacer(
                    Modifier.height(14.dp)
                )

                // =========================
                // تفاصيل المنتجات
                // =========================

                Text(
                    text = "تفاصيل الطلب",
                    color = InvoiceGold,
                    fontSize = 15.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                items.forEach { item ->

                    InvoiceItemRow(
                        item = item
                    )

                    Spacer(
                        Modifier.height(7.dp)
                    )
                }

                Spacer(
                    Modifier.height(5.dp)
                )

                InvoiceDivider()

                Spacer(
                    Modifier.height(12.dp)
                )

                // =========================
                // الإجمالي
                // =========================

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color =
                                InvoiceGold.copy(
                                    alpha = 0.10f
                                ),
                            shape =
                                RoundedCornerShape(
                                    14.dp
                                )
                        )
                        .border(
                            width = 1.dp,
                            color =
                                InvoiceGold.copy(
                                    alpha = 0.35f
                                ),
                            shape =
                                RoundedCornerShape(
                                    14.dp
                                )
                        )
                        .padding(14.dp),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text = "الإجمالي",
                        color = InvoiceGoldLight,
                        fontSize = 15.sp,
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Text(
                        text =
                            formatPrice(
                                totalAmount
                            ),
                        color = InvoiceGold,
                        fontSize = 21.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                Spacer(
                    Modifier.height(10.dp)
                )

                Text(
                    text =
                        "شكرًا لاختيارك Royal Coffee ♛",
                    modifier =
                        Modifier.fillMaxWidth(),
                    color =
                        InvoiceGoldLight.copy(
                            alpha = 0.72f
                        ),
                    fontSize = 11.sp,
                    fontWeight =
                        FontWeight.Medium
                )

                Spacer(
                    Modifier.height(14.dp)
                )

                // =========================
                // الأزرار
                // =========================

                Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    TextButton(
        onClick = onClose,
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = "إغلاق",
            color = InvoiceGold,
            fontSize = 14.sp
        )
    }

    Button(
        onClick = onClose,
        modifier = Modifier.weight(1.4f),
        shape = RoundedCornerShape(13.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = InvoiceGold,
            contentColor = InvoiceBlack
        )
    ) {
        Text(
            text = "تم",
            fontWeight = FontWeight.Bold
        )
    }
                }

                    TextButton(
                        onClick = onClose,
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text = "إغلاق",
                            color = InvoiceGold,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = onClose,
                        modifier =
                            Modifier.weight(1.4f),

                        shape =
                            RoundedCornerShape(13.dp),

                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        InvoiceGold,
                                    contentColor =
                                        InvoiceBlack
                                )
                    ) {

                        Text(
                            text = "تم",
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }
        }
    }


// =====================================================
// معلومات العميل
// =====================================================

@Composable
private fun InvoiceInfoRow(
    label: String,
    value: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),

        verticalAlignment =
            Alignment.Top
    ) {

        Text(
            text = label,
            color =
                InvoiceGoldLight.copy(
                    alpha = 0.58f
                ),
            fontSize = 11.sp,
            modifier =
                Modifier.width(70.dp)
        )

        Text(
            text = value,
            color =
                Color.White.copy(
                    alpha = 0.90f
                ),
            fontSize = 12.sp,
            fontWeight =
                FontWeight.Medium,
            modifier =
                Modifier.weight(1f)
        )
    }
}

// =====================================================
// منتج داخل الفاتورة
// =====================================================

@Composable
private fun InvoiceItemRow(
    item: RoyalInvoiceItem
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = InvoicePanel,
                shape =
                    RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color =
                    InvoiceGold.copy(
                        alpha = 0.18f
                    ),
                shape =
                    RoundedCornerShape(12.dp)
            )
            .padding(10.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text = item.name,
                color =
                    Color.White.copy(
                        alpha = 0.94f
                    ),
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                Modifier.height(2.dp)
            )

            Text(
                text =
                    item.quantity
                        .toString() +
                        " × " +
                        formatPrice(
                            item.unitPrice
                        ),

                color =
                    InvoiceGoldLight.copy(
                        alpha = 0.62f
                    ),

                fontSize = 10.sp
            )
        }

        Text(
            text =
                formatPrice(
                    item.total
                ),

            color = InvoiceGold,
            fontSize = 13.sp,
            fontWeight =
                FontWeight.Bold
        )
    }
}

// =====================================================
// خط فاصل ذهبي
// =====================================================

@Composable
private fun InvoiceDivider() {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        InvoiceBronze,
                        InvoiceGold,
                        InvoiceBronze,
                        Color.Transparent
                    )
                )
            )
    )
}
