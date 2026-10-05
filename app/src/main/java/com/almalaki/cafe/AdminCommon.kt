package com.almalaki.cafe

import androidx.compose.ui.graphics.Color

const val ADMIN_SUPABASE_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

const val ADMIN_SUPABASE_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

val AdminGold = Color(0xFFD4AF37)
val AdminBlack = Color(0xFF050505)
val AdminCream = Color(0xFFF5F0E5)
val AdminPanel = Color(0xFF111111)
enum class AdminSection {
    HOME,
    PRODUCTS,
    ORDERS,
    ARCHIVE,
    ACCOUNT_SETTINGS,
    SALES,
    TOP_PRODUCTS,
    INTERFACE_SETTINGS,
    TV_SETTINGS,
    ROYAL_TV,
    ROYAL_TV_IDENTITY
}
