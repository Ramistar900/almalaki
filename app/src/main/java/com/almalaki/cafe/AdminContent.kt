package com.almalaki.cafe

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdminContent(
    section: AdminSection,
    products: List<Product>,
    orders: List<AdminOrder>,
    archivedOrders: List<AdminArchivedOrder>,
    salesStats: AdminSalesStats,
    topProducts: List<AdminTopProduct>,
    loadingDashboard: Boolean,
    loadingArchive: Boolean,
    message: String,
    name: String,
    category: String,
    price: String,
    selectedImageUri: Uri?,
    editingProductId: Int?,
    loading: Boolean,
    accessToken: String,
    context: android.content.Context,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onSaveProduct: () -> Unit,
    onCancelEdit: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshArchive: () -> Unit,
    onOrderStatus: (Long, String) -> Unit,
    onDeleteOrder: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (section) {

            AdminSection.HOME -> {
                item {
                    DashboardHeader(
                        loading = loadingDashboard,
                        onRefresh = onRefresh
                    )
                }

                item {
                    SalesCards(salesStats)
                }

                item {
                    QuickStats(
                        ordersCount = orders.size,
                        productsCount = products.size
                    )
                }

                item {
                    Text(
                        text = "الأكثر طلبًا 🔥",
                        color = AdminGold,
                        fontSize = 21.sp
                    )
                }

                items(topProducts.take(5)) { product ->
                    TopProductRow(product)
                }
            }

            AdminSection.PRODUCTS -> {
                item {
                    ProductEditor(
                        name = name,
                        category = category,
                        price = price,
                        selectedImageUri = selectedImageUri,
                        editingProductId = editingProductId,
                        loading = loading,
                        onNameChange = onNameChange,
                        onCategoryChange = onCategoryChange,
                        onPriceChange = onPriceChange,
                        onPickImage = onPickImage,
                        onSaveProduct = onSaveProduct,
                        onCancelEdit = onCancelEdit
                    )
                }

                item {
                    SectionTitle("المنتجات الحالية")
                }

                items(
                    products,
                    key = { it.id }
                ) { product ->
                    ProductRow(
                        product = product,
                        onEdit = {
                            onEditProduct(product)
                        },
                        onDelete = {
                            onDeleteProduct(product)
                        }
                    )
                }
            }

            AdminSection.ORDERS -> {
                item {
                    SectionTitle("الطلبات")
                }

                if (orders.isEmpty()) {
                    item {
                        Text(
                            "لا توجد طلبات حاليًا.",
                            color = AdminCream
                        )
                    }
                } else {
                    items(
                        orders,
                        key = { it.id }
                    ) { order ->
                        AdminOrderCard(
                            order = order,
                            accessToken = accessToken,
                            onStatus = onOrderStatus,
                            onDelete = onDeleteOrder
                        )
                    }
                }
            }

            AdminSection.ARCHIVE -> {
                item {
                    ArchiveScreen(
                        archivedOrders = archivedOrders,
                        loading = loadingArchive,
                        onRefresh = onRefreshArchive
                    )
                }
            }

            AdminSection.SALES -> {
                item {
                    DashboardHeader(
                        loading = loadingDashboard,
                        onRefresh = onRefresh
                    )
                }

                item {
                    SalesCards(salesStats)
                }

                item {
                    SalesInfo(
                        todayOrders = salesStats.todayOrders,
                        weekOrders = salesStats.weekOrders,
                        monthOrders = salesStats.monthOrders
                    )
                }
            }

            AdminSection.TOP_PRODUCTS -> {
                item {
                    DashboardHeader(
                        loading = loadingDashboard,
                        onRefresh = onRefresh
                    )
                }

                item {
                    SectionTitle("الأكثر طلبًا 🔥")
                }

                if (topProducts.isEmpty()) {
                    item {
                        Text(
                            "لا توجد مبيعات مكتملة لعرض الترتيب بعد.",
                            color = AdminCream
                        )
                    }
                } else {
                    items(
                        topProducts,
                        key = { it.productId }
                    ) { product ->
                        TopProductRow(product)
                    }
                }
            }
        }

        if (message.isNotEmpty()) {
            item {
                Text(
                    text = message,
                    color = AdminCream,
                    modifier = Modifier.padding(
                        vertical = 8.dp
                    )
                )
            }
        }
    }
}

@Composable
fun DashboardHeader(
    loading: Boolean,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "لوحة المالك 👑",
                color = AdminGold,
                fontSize = 25.sp
            )

            Text(
                text = "ملخص نشاط الكافيه",
                color = AdminCream,
                fontSize = 14.sp
            )
        }

        OutlinedButton(
            onClick = onRefresh,
            enabled = !loading
        ) {
            Text(
                if (loading) "جاري..."
                else "تحديث"
            )
        }
    }
}

@Composable
fun SalesCards(
    stats: AdminSalesStats
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SalesCard(
            "إجمالي مبيعات اليوم",
            stats.today
        )

        SalesCard(
            "إجمالي مبيعات هذا الأسبوع",
            stats.week
        )

        SalesCard(
            "إجمالي مبيعات هذا الشهر",
            stats.month
        )
    }
}

@Composable
fun SalesCard(
    title: String,
    amount: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AdminPanel
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                title,
                color = AdminCream,
                fontSize = 15.sp
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                formatPrice(amount),
                color = AdminGold,
                fontSize = 25.sp
            )
        }
    }
}

@Composable
fun QuickStats(
    ordersCount: Int,
    productsCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SmallStat(
            "الطلبات",
            ordersCount.toString(),
            Modifier.weight(1f)
        )

        SmallStat(
            "المنتجات",
            productsCount.toString(),
            Modifier.weight(1f)
        )
    }
}

@Composable
fun SmallStat(
    title: String,
    value: String,
    modifier: Modifier
) {
   
