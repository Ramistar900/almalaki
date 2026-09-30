package com.almalaki.cafe

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProductEditor(
    name: String,
    category: String,
    price: String,
    selectedImageUri: Uri?,
    editingProductId: Int?,
    loading: Boolean,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onSaveProduct: () -> Unit,
    onCancelEdit: () -> Unit
) {
    Column {
        SectionTitle(
            if (editingProductId == null)
                "إضافة منتج"
            else
                "تعديل المنتج"
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("اسم المنتج") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = category,
            onValueChange = onCategoryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("التصنيف") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = price,
            onValueChange = onPriceChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("السعر بالليرة السورية") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onPickImage,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (selectedImageUri == null)
                    "📷 اختيار صورة المنتج"
                else
                    "✅ تم اختيار صورة المنتج"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSaveProduct,
                enabled = !loading,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AdminGold,
                    contentColor = AdminBlack
                )
            ) {
                Text(
                    if (loading) "جاري..."
                    else if (editingProductId == null) "إضافة المنتج"
                    else "حفظ التعديل"
                )
            }

            if (editingProductId != null) {
                OutlinedButton(
                    onClick = onCancelEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إلغاء")
                }
            }
        }
    }
}

@Composable
fun ProductRow(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AdminPanel)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                product.name,
                color = AdminCream,
                fontSize = 18.sp
            )

            Text(
                "${product.category} • ${formatPrice(product.price)}",
                color = AdminGold
            )

            if (product.imageUrl.isNotEmpty()) {
                Text(
                    "📷 توجد صورة للمنتج",
                    color = AdminCream,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
               
