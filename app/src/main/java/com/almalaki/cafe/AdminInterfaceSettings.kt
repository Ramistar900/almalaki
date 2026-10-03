package com.almalaki.cafe

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val INTERFACE_PREFS = "royal_interface_settings"
private const val KEY_NEWS_TEXT = "news_text"
private const val KEY_TOP_LOGO_URI = "top_logo_uri"
private const val KEY_BOTTOM_LOGO_URI = "bottom_logo_uri"

private const val DEFAULT_NEWS_TEXT = "أهلاً بكم في Royal Coffee ☕"

data class RoyalInterfaceSettings(
    val newsText: String = DEFAULT_NEWS_TEXT,
    val topLogoUri: String = "",
    val bottomLogoUri: String = ""
)

fun loadRoyalInterfaceSettings(context: Context): RoyalInterfaceSettings {
    val prefs = context.getSharedPreferences(INTERFACE_PREFS, Context.MODE_PRIVATE)

    return RoyalInterfaceSettings(
        newsText = prefs.getString(KEY_NEWS_TEXT, DEFAULT_NEWS_TEXT)
            ?: DEFAULT_NEWS_TEXT,
        topLogoUri = prefs.getString(KEY_TOP_LOGO_URI, "") ?: "",
        bottomLogoUri = prefs.getString(KEY_BOTTOM_LOGO_URI, "") ?: ""
    )
}

fun saveRoyalInterfaceSettings(
    context: Context,
    newsText: String,
    topLogoUri: String,
    bottomLogoUri: String
) {
    context.getSharedPreferences(INTERFACE_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_NEWS_TEXT, newsText)
        .putString(KEY_TOP_LOGO_URI, topLogoUri)
        .putString(KEY_BOTTOM_LOGO_URI, bottomLogoUri)
        .apply()
}

@Composable
fun AdminInterfaceSettings(
    context: Context,
    modifier: Modifier = Modifier
) {
    val saved = remember { loadRoyalInterfaceSettings(context) }

    var newsText by remember { mutableStateOf(saved.newsText) }
    var topLogoUri by remember { mutableStateOf(saved.topLogoUri) }
    var bottomLogoUri by remember { mutableStateOf(saved.bottomLogoUri) }
    var message by remember { mutableStateOf("") }

    val topLogoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) topLogoUri = uri.toString()
    }

    val bottomLogoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) bottomLogoUri = uri.toString()
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "إعدادات واجهة الكافيه",
                color = AdminGold,
                fontSize = 22.sp
            )

            Text(
                text = "تعديل الشعارات ونص الشريط الإخباري",
                color = AdminCream,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { topLogoPicker.launch("image/png") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🖼️ اللوجو العلوي")
                }

                Button(
                    onClick = { bottomLogoPicker.launch("image/png") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🖼️ اللوجو السفلي")
                }
            }

            OutlinedTextField(
                value = newsText,
                onValueChange = { newsText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("نص الشريط الإخباري") },
                singleLine = true
            )

            Button(
                onClick = {
                    saveRoyalInterfaceSettings(
                        context = context,
                        newsText = newsText.trim().ifEmpty { DEFAULT_NEWS_TEXT },
                        topLogoUri = topLogoUri,
                        bottomLogoUri = bottomLogoUri
                    )
                    message = "تم حفظ إعدادات الواجهة بنجاح ✓"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("💾 حفظ التعديلات")
            }

            if (message.isNotEmpty()) {
                Text(
                    text = message,
                    color = AdminGold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
