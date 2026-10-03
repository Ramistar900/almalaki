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
import java.io.File

private const val INTERFACE_PREFS = "royal_interface_settings"
private const val KEY_NEWS_TEXT = "news_text"
private const val KEY_TOP_LOGO_PATH = "top_logo_path"
private const val KEY_BOTTOM_LOGO_PATH = "bottom_logo_path"
private const val TOP_LOGO_FILE = "royal_top_logo.png"
private const val BOTTOM_LOGO_FILE = "royal_bottom_logo.png"
private const val DEFAULT_NEWS_TEXT = "أهلاً بكم في Royal Coffee ☕"

data class RoyalInterfaceSettings(
    val newsText: String = DEFAULT_NEWS_TEXT,
    val topLogoPath: String = "",
    val bottomLogoPath: String = ""
)

fun loadRoyalInterfaceSettings(context: Context): RoyalInterfaceSettings {
    val prefs = context.getSharedPreferences(INTERFACE_PREFS, Context.MODE_PRIVATE)
    return RoyalInterfaceSettings(
        newsText = prefs.getString(KEY_NEWS_TEXT, DEFAULT_NEWS_TEXT) ?: DEFAULT_NEWS_TEXT,
        topLogoPath = prefs.getString(KEY_TOP_LOGO_PATH, "") ?: "",
        bottomLogoPath = prefs.getString(KEY_BOTTOM_LOGO_PATH, "") ?: ""
    )
}

private fun savePngToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
    return try {
        val destination = File(context.filesDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        destination.absolutePath
    } catch (_: Exception) {
        null
    }
}

fun saveRoyalInterfaceSettings(
    context: Context,
    newsText: String,
    topLogoPath: String,
    bottomLogoPath: String
) {
    context.getSharedPreferences(INTERFACE_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_NEWS_TEXT, newsText.trim().ifEmpty { DEFAULT_NEWS_TEXT })
        .putString(KEY_TOP_LOGO_PATH, topLogoPath)
        .putString(KEY_BOTTOM_LOGO_PATH, bottomLogoPath)
        .apply()
}

@Composable
fun AdminInterfaceSettings(context: Context, modifier: Modifier = Modifier) {
    val saved = remember { loadRoyalInterfaceSettings(context) }
    var newsText by remember { mutableStateOf(saved.newsText) }
    var topLogoPath by remember { mutableStateOf(saved.topLogoPath) }
    var bottomLogoPath by remember { mutableStateOf(saved.bottomLogoPath) }
    var message by remember { mutableStateOf("") }

    val topLogoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = savePngToInternalStorage(context, uri, TOP_LOGO_FILE)
            message = if (savedPath != null) {
                topLogoPath = savedPath
                "تم اختيار اللوجو العلوي ✓"
            } else "تعذر حفظ اللوجو العلوي"
        }
    }

    val bottomLogoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = savePngToInternalStorage(context, uri, BOTTOM_LOGO_FILE)
            message = if (savedPath != null) {
                bottomLogoPath = savedPath
                "تم اختيار اللوجو السفلي ✓"
            } else "تعذر حفظ اللوجو السفلي"
        }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("إعدادات واجهة الكافيه", color = AdminGold, fontSize = 22.sp)
            Text("تعديل اللوجوهات ونص الشريط الإخباري", color = AdminCream, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { topLogoPicker.launch("image/png") },
                    modifier = Modifier.weight(1f)
                ) { Text("🖼️ اللوجو العلوي") }
                Button(
                    onClick = { bottomLogoPicker.launch("image/png") },
                    modifier = Modifier.weight(1f)
                ) { Text("🖼️ اللوجو السفلي") }
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
                    saveRoyalInterfaceSettings(context, newsText, topLogoPath, bottomLogoPath)
                    message = "تم حفظ إعدادات الواجهة بنجاح ✓"
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("💾 حفظ التعديلات") }
            if (message.isNotEmpty()) {
                Text(message, color = AdminGold, fontSize = 14.sp)
            }
        }
    }
}
