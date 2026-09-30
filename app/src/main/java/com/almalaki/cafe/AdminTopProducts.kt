package com.almalaki.cafe

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TopProductRow(product: AdminTopProduct) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AdminPanel
        )
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${product.quantity}×",
                color = AdminGold,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    product.name,
                    color = AdminCream,
                    fontSize = 17.sp
                )

                Text(
                    "مبيعات: ${formatPrice(product.revenue)}",
                    color = AdminGold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
