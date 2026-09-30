package com.almalaki.cafe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomerScreen(
    darkMode: Boolean,
    onTheme: () -> Unit,
    onOwner: () -> Unit
) {
    var products by remember {
        mutableStateOf<List<Product>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var message by remember {
        mutableStateOf("")
    }

    var cart by remember {
        mutableStateOf<Map<Int, Int>>(emptyMap())
    }

    var showCart by remember {
        mutableStateOf(false)
    }

    var showCheckout by remember {
        mutableStateOf(false)
    }

    var orderLoading by remember {
        mutableStateOf(false)
    }

    var orderMessage by remember {
        mutableStateOf("")
    }

    var orderSuccessNumber by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {
       
