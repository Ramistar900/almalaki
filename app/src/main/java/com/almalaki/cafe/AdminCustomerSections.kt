package com.almalaki.cafe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AdminCustomerSections(
    accessToken: String,
    restaurantId: String,
    modifier: Modifier = Modifier
) {
    var sections by remember(restaurantId) {
        mutableStateOf<List<CustomerSection>>(emptyList())
    }
    var loading by remember(restaurantId) { mutableStateOf(true) }
    var saving by remember(restaurantId) { mutableStateOf(false) }
    var errorMessage by remember(restaurantId) { mutableStateOf("") }
    var infoMessage by remember(restaurantId) { mutableStateOf("") }
    var reloadKey by remember(restaurantId) { mutableStateOf(0) }
    var newTitle by remember(restaurantId) { mutableStateOf("") }
    var newCategories by remember(restaurantId) { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(accessToken, restaurantId, reloadKey) {
        if (restaurantId.isBlank()) {
            sections = emptyList()
            loading = false
            errorMessage = "اختر مطعمًا قبل إدارة أقسام واجهة العميل."
            return@LaunchedEffect
        }

        loading = true
        errorMessage = ""
        try {
            sections = withContext(Dispatchers.IO) {
                loadCustomerSections(accessToken, restaurantId)
            }
        } catch (error: Exception) {
            sections = emptyList()
            errorMessage = error.message ?: "تعذر تحميل أقسام هذا المطعم."
        } finally {
            loading = false
        }
    }

    Column(
        modifier = modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("إدارة أقسام واجهة العميل", fontSize = 21.sp)
        Text("يتم حفظ الأقسام وإظهارها لهذا المطعم المحدد فقط.", fontSize = 14.sp)

        if (errorMessage.isNotBlank()) Text(errorMessage, fontSize = 13.sp)
        if (infoMessage.isNotBlank()) Text(infoMessage, fontSize = 13.sp)

        if (loading) {
            CircularProgressIndicator()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = sections,
                    key = { "$restaurantId:${it.sectionKey}" }
                ) { section ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(section.title, fontSize = 17.sp)
                            Text(
                                if (section.enabled) "الحالة: ظاهر للعملاء"
                                else "الحالة: مخفي عن العملاء",
                                fontSize = 13.sp
                            )
                            Text(
                                if (section.categoryNames.isEmpty()) "التصنيفات: غير محددة"
                                else "التصنيفات: " + section.categoryNames.joinToString("، "),
                                fontSize = 12.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    enabled = !saving,
                                    onClick = {
                                        scope.launch {
                                            saving = true
                                            errorMessage = ""
                                            infoMessage = ""
                                            try {
                                                withContext(Dispatchers.IO) {
                                                    setCustomerSectionEnabled(
                                                        accessToken = accessToken,
                                                        sectionKey = section.sectionKey,
                                                        enabled = !section.enabled,
                                                        restaurantId = restaurantId
                                                    )
                                                }
                                                infoMessage = "تم تحديث القسم في المطعم المحدد."
                                                reloadKey++
                                            } catch (error: Exception) {
                                                errorMessage = error.message ?: "تعذر تحديث القسم."
                                            } finally {
                                                saving = false
                                            }
                                        }
                                    }
                                ) {
                                    Text(if (section.enabled) "إخفاء القسم" else "إظهار القسم")
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("إضافة قسم جديد", fontSize = 18.sp)

        OutlinedTextField(
            value = newTitle,
            onValueChange = { newTitle = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("اسم القسم") },
            singleLine = true,
            enabled = !saving && restaurantId.isNotBlank()
        )
        OutlinedTextField(
            value = newCategories,
            onValueChange = { newCategories = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("تصنيفات المنتجات، مفصولة بفواصل") },
            placeholder = { Text("مثال: المشروبات، مشروبات باردة") },
            enabled = !saving && restaurantId.isNotBlank()
        )
        Text("اكتب أسماء التصنيفات كما تظهر في منتجات هذا المطعم.", fontSize = 12.sp)

        Button(
            enabled = !saving && !loading && restaurantId.isNotBlank() && newTitle.trim().isNotEmpty(),
            onClick = {
                val title = newTitle.trim()
                val categories = newCategories.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()

                scope.launch {
                    saving = true
                    errorMessage = ""
                    infoMessage = ""
                    try {
                        val nextOrder = (sections.maxOfOrNull { it.sortOrder } ?: 0) + 1
                        // Namespace keys by tenant as well as time to avoid cross-store collisions.
                        val tenantKey = restaurantId.replace("-", "").take(12)
                        val key = "custom_${tenantKey}_${System.currentTimeMillis()}"
                        withContext(Dispatchers.IO) {
                            saveCustomerSection(
                                accessToken = accessToken,
                                section = CustomerSection(
                                    sectionKey = key,
                                    title = title,
                                    categoryNames = categories,
                                    enabled = true,
                                    sortOrder = nextOrder
                                ),
                                restaurantId = restaurantId
                            )
                        }
                        newTitle = ""
                        newCategories = ""
                        infoMessage = "تم حفظ القسم الجديد لهذا المطعم."
                        reloadKey++
                    } catch (error: Exception) {
                        errorMessage = error.message ?: "تعذر حفظ القسم."
                    } finally {
                        saving = false
                    }
                }
            }
        ) {
            Text("حفظ القسم الجديد")
        }

        TextButton(
            enabled = !loading && !saving,
            onClick = { reloadKey++ }
        ) {
            Text("تحديث قائمة الأقسام")
        }
    }
}
