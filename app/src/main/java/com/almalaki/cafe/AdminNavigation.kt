package com.almalaki.cafe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdminSidebar(
    section: AdminSection,
    onSectionSelected: (AdminSection) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(AdminPanel)
            .padding(12.dp)
    ) {
        Text(
            text = "الملكي 👑",
            color = AdminGold,
            fontSize = 25.sp,
            modifier = Modifier.padding(8.dp)
        )

        Text(
            text = "لوحة المالك",
            color = AdminCream,
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        AdminMenuItem("⌂", "الرئيسية", section == AdminSection.HOME) {
            onSectionSelected(AdminSection.HOME)
        }

        AdminMenuItem("▣", "تعديل المنتجات", section == AdminSection.PRODUCTS) {
            onSectionSelected(AdminSection.PRODUCTS)
        }

        AdminMenuItem("▤", "الطلبات", section == AdminSection.ORDERS) {
            onSectionSelected(AdminSection.ORDERS)
        }

        AdminMenuItem("📦", "الأرشيف", section == AdminSection.ARCHIVE) {
            onSectionSelected(AdminSection.ARCHIVE)
        }

        AdminMenuItem("◈", "المبيعات", section == AdminSection.SALES) {
            onSectionSelected(AdminSection.SALES)
        }

        AdminMenuItem("★", "الأكثر طلبًا", section == AdminSection.TOP_PRODUCTS) {
            onSectionSelected(AdminSection.TOP_PRODUCTS)
        }

        AdminMenuItem("⚙", "إعدادات الحساب", section == AdminSection.ACCOUNT_SETTINGS) {
            onSectionSelected(AdminSection.ACCOUNT_SETTINGS)
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تسجيل الخروج")
        }
    }
}

@Composable
fun AdminMenuItem(
    icon: String,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container = if (selected) AdminGold else Color.Transparent
    val textColor = if (selected) AdminBlack else AdminCream

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = textColor
        )
    ) {
        Text("$icon  $title")
    }
}

@Composable
fun AdminTopBar(
    title: String,
    menuOpen: Boolean,
    onMenuClick: () -> Unit,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AdminPanel)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onMenuClick) {
            Text(
                text = if (menuOpen) "×" else "☰",
                color = AdminGold,
                fontSize = 27.sp
            )
        }

        Text(
            text = title,
            color = AdminGold,
            fontSize = 21.sp,
            modifier = Modifier.weight(1f)
        )

        TextButton(onClick = onLogout) {
            Text("خروج", color = AdminGold)
        }
    }
}

@Composable
fun AdminHorizontalMenu(
    section: AdminSection,
    onSectionSelected: (AdminSection) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(AdminPanel)
            .heightIn(max = 350.dp)
            .padding(horizontal = 8.dp)
    ) {
        item {
            AdminMenuItem("⌂", "الرئيسية", section == AdminSection.HOME) {
                onSectionSelected(AdminSection.HOME)
            }
        }

        item {
            AdminMenuItem("▣", "تعديل المنتجات", section == AdminSection.PRODUCTS) {
                onSectionSelected(AdminSection.PRODUCTS)
            }
        }

        item {
            AdminMenuItem("▤", "الطلبات", section == AdminSection.ORDERS) {
                onSectionSelected(AdminSection.ORDERS)
            }
        }

        item {
            AdminMenuItem("📦", "الأرشيف", section == AdminSection.ARCHIVE) {
                onSectionSelected(AdminSection.ARCHIVE)
            }
        }

        item {
            AdminMenuItem("◈", "المبيعات", section == AdminSection.SALES) {
                onSectionSelected(AdminSection.SALES)
            }
        }

        item {
            AdminMenuItem("★", "الأكثر طلبًا", section == AdminSection.TOP_PRODUCTS) {
                onSectionSelected(AdminSection.TOP_PRODUCTS)
            }
        }

        item {
            AdminMenuItem(
                "⚙",
                "إعدادات الحساب",
                section == AdminSection.ACCOUNT_SETTINGS
            ) {
                onSectionSelected(AdminSection.ACCOUNT_SETTINGS)
            }
        }
    }
}
