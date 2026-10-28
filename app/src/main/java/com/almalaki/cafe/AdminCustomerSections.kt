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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdminCustomerSections(
    accessToken: String,
    modifier: Modifier = Modifier
) {
    var sections by remember {
        mutableStateOf<List<CustomerSection>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var saving by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var infoMessage by remember {
        mutableStateOf("")
    }

    var reloadKey by remember {
        mutableStateOf(0)
    }

    var newTitle by remember {
        mutableStateOf("")
    }

    var newCategories by remember {
        mutableStateOf("")
    }

    LaunchedEffect(reloadKey) {
        loading = true
        errorMessage = ""

        Thread {
            try {
                val result = loadCustomerSections(accessToken = accessToken)
                sections = result
            } catch (e: Exception) {
                errorMessage = e.message
                    ?: "تعذر تحميل الأقسام."
            } finally {
                loading = false
            }
        }.start()
    }

    Column(
        modifier = modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "إدارة أقسام واجهة العميل",
            fontSize = 21.sp
        )

        Text(
            text = "إظهار الأقسام وإخفاؤها دون حذف المنتجات أو الصور.",
            fontSize = 14.sp
        )

        if (errorMessage.isNotBlank()) {
            Text(
                text = errorMessage,
                fontSize = 13.sp
            )
        }

        if (infoMessage.isNotBlank()) {
            Text(
                text = infoMessage,
                fontSize = 13.sp
            )
        }

        if (loading) {
            CircularProgressIndicator()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = sections,
                    key = { it.sectionKey }
                ) { section ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = section.title,
                                fontSize = 17.sp
                            )

                            Text(
                                text = if (section.enabled) {
                                    "الحالة: ظاهر للعملاء"
                                } else {
                                    "الحالة: مخفي عن العملاء"
                                },
                                fontSize = 13.sp
                            )

                            Text(
                                text = if (
                                    section.categoryNames.isEmpty()
                                ) {
                                    "التصنيفات: غير محددة"
                                } else {
                                    "التصنيفات: " +
                                        section.categoryNames.joinToString("، ")
                                },
                                fontSize = 12.sp
                            )

                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    enabled = !saving,
                                    onClick = {
                                        saving = true
                                        errorMessage = ""
                                        infoMessage = ""

                                        Thread {
                                            try {
                                                setCustomerSectionEnabled(
                                                    accessToken = accessToken,
                                                    sectionKey = section.sectionKey,
                                                    enabled = !section.enabled
                                                )

                                                infoMessage =
                                                    "تم تحديث إعداد القسم في قاعدة البيانات."

                                                reloadKey++
                                            } catch (e: Exception) {
                                                errorMessage =
                                                    e.message
                                                        ?: "تعذر تحديث القسم."
                                            } finally {
                                                saving = false
                                            }
                                        }.start()
                                    }
                                ) {
                                    Text(
                                        if (section.enabled) {
                                            "إخفاء القسم"
                                        } else {
                                            "إظهار القسم"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "إضافة قسم جديد",
            fontSize = 18.sp
        )

        OutlinedTextField(
            value = newTitle,
            onValueChange = { newTitle = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("اسم القسم") },
            singleLine = true
        )

        OutlinedTextField(
            value = newCategories,
            onValueChange = { newCategories = it },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("تصنيفات المنتجات، مفصولة بفواصل")
            },
            placeholder = {
                Text("مثال: المشروبات، مشروبات باردة")
            }
        )

        Text(
            text = "اكتب أسماء التصنيفات كما تظهر في المنتجات الحالية.",
            fontSize = 12.sp
        )

        Button(
            enabled = !saving &&
                newTitle.trim().isNotEmpty(),
            onClick = {
                val title = newTitle.trim()
                val categories = newCategories
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()

                saving = true
                errorMessage = ""
                infoMessage = ""

                Thread {
                    try {
                        val nextOrder =
                            (sections.maxOfOrNull {
                                it.sortOrder
                            } ?: 0) + 1

                        val key =
                            "custom_${System.currentTimeMillis()}"

                        saveCustomerSection(
                            accessToken = accessToken,
                            section = CustomerSection(
                                sectionKey = key,
                                title = title,
                                categoryNames = categories,
                                enabled = true,
                                sortOrder = nextOrder
                            )
                        )

                        newTitle = ""
                        newCategories = ""
                        infoMessage = "تم حفظ القسم الجديد."
                        reloadKey++
                    } catch (e: Exception) {
                        errorMessage = e.message
                            ?: "تعذر حفظ القسم."
                    } finally {
                        saving = false
                    }
                }.start()
            }
        ) {
            Text("حفظ القسم الجديد")
        }

        TextButton(
            enabled = !loading && !saving,
            onClick = {
                reloadKey++
            }
        ) {
            Text("تحديث قائمة الأقسام")
        }
    }
}
