package com.almalaki.cafe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.onFocusChanged

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

        AdminMenuItem(
            RoyalIcon.HOME,
            "الرئيسية",
            section == AdminSection.HOME
        ) {
            onSectionSelected(AdminSection.HOME)
        }

        AdminMenuItem(
            RoyalIcon.PRODUCTS,
            "تعديل المنتجات",
            section == AdminSection.PRODUCTS
        ) {
            onSectionSelected(AdminSection.PRODUCTS)
        }

        AdminMenuItem(
            RoyalIcon.ORDERS,
            "الطلبات",
            section == AdminSection.ORDERS
        ) {
            onSectionSelected(AdminSection.ORDERS)
        }

        AdminMenuItem(
            RoyalIcon.RESERVATIONS,
            "الحجوزات",
            section == AdminSection.RESERVATIONS
        ) {
            onSectionSelected(AdminSection.RESERVATIONS)
        }

        AdminMenuItem(
            RoyalIcon.ARCHIVE,
            "الأرشيف",
            section == AdminSection.ARCHIVE
        ) {
            onSectionSelected(AdminSection.ARCHIVE)
        }

        AdminMenuItem(
            RoyalIcon.SALES,
            "المبيعات",
            section == AdminSection.SALES
        ) {
            onSectionSelected(AdminSection.SALES)
        }

        AdminMenuItem(
            RoyalIcon.TOP_PRODUCTS,
            "الأكثر طلبًا",
            section == AdminSection.TOP_PRODUCTS
        ) {
            onSectionSelected(AdminSection.TOP_PRODUCTS)
        }

        AdminMenuItem(
            RoyalIcon.SETTINGS,
            "إعدادات الحساب",
            section == AdminSection.ACCOUNT_SETTINGS
        ) {
            onSectionSelected(AdminSection.ACCOUNT_SETTINGS)
        }
        
        AdminMenuItem(
            RoyalIcon.TV,
            "مركز تحكم ROYAL TV",
            section == AdminSection.ROYAL_TV_CONTROL
        ) {
            onSectionSelected(AdminSection.ROYAL_TV_CONTROL)
        }
        

        AdminMenuItem(
            RoyalIcon.PALETTE,
            "تحرير الواجهة",
            section == AdminSection.INTERFACE_SETTINGS
        ) {
            onSectionSelected(AdminSection.INTERFACE_SETTINGS)
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoyalIconButton(icon = RoyalIcon.LOGOUT, contentDescription = "تسجيل الخروج",
                    size = 22.dp, onClick = {}, interactive = false, tint = AdminGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل الخروج")
            }
        }
    }
}

@Composable
fun AdminMenuItem(
    icon: RoyalIcon,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val focused = remember { mutableStateOf(false) }

    val container =
        if (selected) AdminGold
        else Color.Transparent

    val textColor =
        if (selected) AdminBlack
        else AdminCream

    val focusBorder =
        if (focused.value) AdminGold
        else Color.Transparent

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .onFocusChanged {
                focused.value = it.isFocused
            }
            .border(
                width = if (focused.value) 2.dp else 0.dp,
                color = focusBorder,
                shape = MaterialTheme.shapes.medium
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = textColor
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RoyalIconButton(
                icon = icon,
                contentDescription = title,
                size = 27.dp,
                onClick = {},
                interactive = false,
                tint = if (selected) AdminBlack else AdminGold
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(title, modifier = Modifier.weight(1f))
        }
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
            .padding(
                horizontal = 10.dp,
                vertical = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RoyalIconButton(
            icon = if (menuOpen) RoyalIcon.CLOSE else RoyalIcon.MENU,
            contentDescription = if (menuOpen) "إغلاق القائمة" else "فتح القائمة",
            size = 28.dp,
            onClick = onMenuClick,
            tint = AdminGold
        )

        Text(
            text = title,
            color = AdminGold,
            fontSize = 21.sp,
            modifier = Modifier.weight(1f)
        )

        RoyalDateTime(
            language = "ar",
            style = androidx.compose.ui.text.TextStyle(
                color = AdminGold,
                fontSize = 11.sp
            )
        )

        TextButton(onClick = onLogout) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoyalIconButton(icon = RoyalIcon.LOGOUT, contentDescription = "خروج",
                    size = 20.dp, onClick = {}, interactive = false, tint = AdminGold)
                Spacer(modifier = Modifier.width(6.dp))
                Text("خروج", color = AdminGold)
            }
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
            AdminMenuItem(
                RoyalIcon.HOME,
                "الرئيسية",
                section == AdminSection.HOME
            ) {
                onSectionSelected(AdminSection.HOME)
            }
        }

        item {
            AdminMenuItem(
                RoyalIcon.PRODUCTS,
                "تعديل المنتجات",
                section == AdminSection.PRODUCTS
            ) {
                onSectionSelected(AdminSection.PRODUCTS)
            }
        }

        item {
            AdminMenuItem(
                RoyalIcon.ORDERS,
                "الطلبات",
                section == AdminSection.ORDERS
            ) {
                onSectionSelected(AdminSection.ORDERS)
            }
        }

        item {
            AdminMenuItem(
                RoyalIcon.RESERVATIONS,
                "الحجوزات",
                section == AdminSection.RESERVATIONS
            ) {
                onSectionSelected(AdminSection.RESERVATIONS)
            }
        }

        item {
            AdminMenuItem(
                RoyalIcon.ARCHIVE,
                "الأرشيف",
                section == AdminSection.ARCHIVE
            ) {
                onSectionSelected(AdminSection.ARCHIVE)
            }
        }

        item {
            AdminMenuItem(
                RoyalIcon.SALES,
                "المبيعات",
                section == AdminSection.SALES
            ) {
                onSectionSelected(AdminSection.SALES)
            }
        }

        item {
            AdminMenuItem(
                RoyalIcon.PALETTE,
                "تحرير الواجهة",
                section == AdminSection.INTERFACE_SETTINGS
            ) {
                onSectionSelected(
                    AdminSection.INTERFACE_SETTINGS
                )
            }
        }

        item {
            AdminMenuItem(
                RoyalIcon.TOP_PRODUCTS,
                "الأكثر طلبًا",
                section == AdminSection.TOP_PRODUCTS
            ) {
                onSectionSelected(AdminSection.TOP_PRODUCTS)
            }
        }
        
        item {
            AdminMenuItem(
                RoyalIcon.SETTINGS,
                "إعدادات الحساب",
                section == AdminSection.ACCOUNT_SETTINGS
            ) {
                onSectionSelected(
                    AdminSection.ACCOUNT_SETTINGS
                )
            }
        }

        item {
            AdminMenuItem(
                RoyalIcon.TV,
                "مركز تحكم ROYAL TV",
                section == AdminSection.ROYAL_TV_CONTROL
            ) {
                onSectionSelected(
                    AdminSection.ROYAL_TV_CONTROL
                )
            }
        }
    }
}
