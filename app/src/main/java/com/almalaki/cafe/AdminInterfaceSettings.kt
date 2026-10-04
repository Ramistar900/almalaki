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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

private const val INTERFACE_PREFS = "royal_interface_settings"

private const val KEY_NEWS_TEXT = "news_text"
private const val KEY_TOP_LOGO_PATH = "top_logo_path"
private const val KEY_BOTTOM_LOGO_PATH = "bottom_logo_path"

private const val KEY_LOGO_LIBRARY = "logo_library"

private const val DEFAULT_NEWS_TEXT =
    "أهلاً بكم في Royal Coffee ☕"

/*
 * الشعار الأساسي للتطبيق.
 *
 * هذا المعرّف ثابت ولن يتم حذفه.
 */
const val ROYAL_DEFAULT_LOGO_ID = "default_royal"

/*
 * نوع حركة الشعار.
 *
 * layered:
 * الشعار الأساسي المكوّن من:
 * - التاج والدرع
 * - RC
 *
 * generic_3d:
 * أي شعار PNG إضافي يرفع من المالك.
 */
const val ROYAL_ANIMATION_LAYERED = "layered"
const val ROYAL_ANIMATION_GENERIC_3D = "generic_3d"

/*
 * بيانات شعار واحد داخل مكتبة الشعارات.
 */
data class RoyalLogoEntry(
    val id: String,
    val name: String,
    val filePath: String,
    val animationType: String
)

/*
 * إعدادات واجهة التطبيق الحالية.
 *
 * topLogoPath و bottomLogoPath محفوظان كما هما
 * حتى لا نكسر الربط الحالي قبل الانتقال إلى
 * نظام الشعار النشط في الخطوة التالية.
 */
data class RoyalInterfaceSettings(
    val newsText: String = DEFAULT_NEWS_TEXT,
    val topLogoPath: String = "",
    val bottomLogoPath: String = ""
)

/*
 * إنشاء تعريف الشعار الأساسي.
 *
 * مهم:
 * filePath فارغ لأن الشعار الأساسي ليس صورة PNG واحدة.
 * هو مكوّن من:
 *
 * royal_crest_layer.png
 * royal_rc_layer.png
 */
fun royalDefaultLogoEntry(): RoyalLogoEntry {
    return RoyalLogoEntry(
        id = ROYAL_DEFAULT_LOGO_ID,
        name = "الشعار الأساسي",
        filePath = "",
        animationType = ROYAL_ANIMATION_LAYERED
    )
}

/*
 * تحويل مكتبة الشعارات إلى JSON.
 */
private fun logoLibraryToJson(
    logos: List<RoyalLogoEntry>
): String {

    val array = JSONArray()

    logos.forEach { logo ->

        val item = JSONObject()

        item.put("id", logo.id)
        item.put("name", logo.name)
        item.put("filePath", logo.filePath)
        item.put("animationType", logo.animationType)

        array.put(item)
    }

    return array.toString()
}

/*
 * قراءة مكتبة الشعارات من SharedPreferences.
 */
private fun logoLibraryFromJson(
    json: String?
): MutableList<RoyalLogoEntry> {

    val result = mutableListOf<RoyalLogoEntry>()

    if (json.isNullOrBlank()) {
        return result
    }

    return try {

        val array = JSONArray(json)

        for (index in 0 until array.length()) {

            val item = array.optJSONObject(index)
                ?: continue

            val id = item.optString("id")
            val name = item.optString("name")
            val filePath = item.optString("filePath")
            val animationType =
                item.optString(
                    "animationType",
                    ROYAL_ANIMATION_GENERIC_3D
                )

            if (id.isNotBlank()) {

                result.add(
                    RoyalLogoEntry(
                        id = id,
                        name = name.ifBlank {
                            "شعار إضافي"
                        },
                        filePath = filePath,
                        animationType = animationType
                    )
                )
            }
        }

        result

    } catch (_: Exception) {

        mutableListOf()
    }
}

/*
 * حفظ مكتبة الشعارات.
 */
private fun saveRoyalLogoLibrary(
    context: Context,
    logos: List<RoyalLogoEntry>
) {

    context
        .getSharedPreferences(
            INTERFACE_PREFS,
            Context.MODE_PRIVATE
        )
        .edit()
        .putString(
            KEY_LOGO_LIBRARY,
            logoLibraryToJson(logos)
        )
        .apply()
}

/*
 * تحميل مكتبة الشعارات.
 *
 * يضمن وجود الشعار الأساسي دائمًا.
 *
 * كما يقوم بترحيل الشعار القديم الموجود في
 * topLogoPath / bottomLogoPath إلى المكتبة
 * إذا كان موجودًا ولم يكن مسجلًا فيها.
 */
fun loadRoyalLogoLibrary(
    context: Context
): List<RoyalLogoEntry> {

    val prefs =
        context.getSharedPreferences(
            INTERFACE_PREFS,
            Context.MODE_PRIVATE
        )

    val logos =
        logoLibraryFromJson(
            prefs.getString(
                KEY_LOGO_LIBRARY,
                null
            )
        )

    /*
     * التأكد من وجود الشعار الأساسي دائمًا.
     */
    if (
        logos.none {
            it.id == ROYAL_DEFAULT_LOGO_ID
        }
    ) {

        logos.add(
            0,
            royalDefaultLogoEntry()
        )
    }

    /*
     * ترحيل اللوجو العلوي القديم.
     */
    val oldTopPath =
        prefs.getString(
            KEY_TOP_LOGO_PATH,
            ""
        ) ?: ""

    if (
        oldTopPath.isNotBlank() &&
        File(oldTopPath).exists() &&
        logos.none {
            it.filePath == oldTopPath
        }
    ) {

        logos.add(
            RoyalLogoEntry(
                id = "legacy_top_${UUID.randomUUID()}",
                name = "الشعار السابق - علوي",
                filePath = oldTopPath,
                animationType = ROYAL_ANIMATION_GENERIC_3D
            )
        )
    }

    /*
     * ترحيل اللوجو السفلي القديم.
     */
    val oldBottomPath =
        prefs.getString(
            KEY_BOTTOM_LOGO_PATH,
            ""
        ) ?: ""

    if (
        oldBottomPath.isNotBlank() &&
        File(oldBottomPath).exists() &&
        logos.none {
            it.filePath == oldBottomPath
        }
    ) {

        logos.add(
            RoyalLogoEntry(
                id = "legacy_bottom_${UUID.randomUUID()}",
                name = "الشعار السابق - سفلي",
                filePath = oldBottomPath,
                animationType = ROYAL_ANIMATION_GENERIC_3D
            )
        )
    }

    /*
     * حفظ المكتبة بعد التأكد من اكتمالها.
     */
    saveRoyalLogoLibrary(
        context = context,
        logos = logos
    )

    return logos
}

/*
 * إضافة شعار PNG جديد إلى مكتبة الشعارات.
 *
 * كل شعار يحصل على ملف مستقل.
 *
 * لن يتم استبدال شعار سابق.
 */
private fun addLogoToLibrary(
    context: Context,
    uri: Uri,
    displayName: String
): RoyalLogoEntry? {

    return try {

        val id =
            "logo_${UUID.randomUUID()}"

        val fileName =
            "royal_logo_${id}.png"

        val destination =
            File(
                context.filesDir,
                fileName
            )

        context.contentResolver
            .openInputStream(uri)
            ?.use { input ->

                destination.outputStream()
                    .use { output ->

                        input.copyTo(output)
                    }
            }
            ?: return null

        val logo =
            RoyalLogoEntry(
                id = id,
                name = displayName.ifBlank {
                    "شعار إضافي"
                },
                filePath = destination.absolutePath,
                animationType = ROYAL_ANIMATION_GENERIC_3D
            )

        val library =
            loadRoyalLogoLibrary(
                context
            ).toMutableList()

        library.add(logo)

        saveRoyalLogoLibrary(
            context,
            library
        )

        logo

    } catch (_: Exception) {

        null
    }
}

/*
 * إعدادات الواجهة الحالية.
 */
fun loadRoyalInterfaceSettings(
    context: Context
): RoyalInterfaceSettings {

    val prefs =
        context.getSharedPreferences(
            INTERFACE_PREFS,
            Context.MODE_PRIVATE
        )

    /*
     * التأكد من إنشاء مكتبة الشعارات.
     */
    loadRoyalLogoLibrary(context)

    return RoyalInterfaceSettings(

        newsText =
            prefs.getString(
                KEY_NEWS_TEXT,
                DEFAULT_NEWS_TEXT
            ) ?: DEFAULT_NEWS_TEXT,

        topLogoPath =
            prefs.getString(
                KEY_TOP_LOGO_PATH,
                ""
            ) ?: "",

        bottomLogoPath =
            prefs.getString(
                KEY_BOTTOM_LOGO_PATH,
                ""
            ) ?: ""
    )
}

/*
 * حفظ إعدادات الواجهة الحالية.
 */
fun saveRoyalInterfaceSettings(
    context: Context,
    newsText: String,
    topLogoPath: String,
    bottomLogoPath: String
) {

    context
        .getSharedPreferences(
            INTERFACE_PREFS,
            Context.MODE_PRIVATE
        )
        .edit()

        .putString(
            KEY_NEWS_TEXT,
            newsText
                .trim()
                .ifEmpty {
                    DEFAULT_NEWS_TEXT
                }
        )

        .putString(
            KEY_TOP_LOGO_PATH,
            topLogoPath
        )

        .putString(
            KEY_BOTTOM_LOGO_PATH,
            bottomLogoPath
        )

        .apply()

    /*
     * التأكد من أن الشعار الأساسي
     * ما زال موجودًا في المكتبة.
     */
    loadRoyalLogoLibrary(context)
}

@Composable
fun AdminInterfaceSettings(
    context: Context,
    modifier: Modifier = Modifier
) {

    val saved =
        remember {
            loadRoyalInterfaceSettings(context)
        }

    var newsText by remember {
        mutableStateOf(
            saved.newsText
        )
    }

    var topLogoPath by remember {
        mutableStateOf(
            saved.topLogoPath
        )
    }

    var bottomLogoPath by remember {
        mutableStateOf(
            saved.bottomLogoPath
        )
    }

    var logoLibrary by remember {
        mutableStateOf(
            loadRoyalLogoLibrary(context)
        )
    }

    var message by remember {
        mutableStateOf("")
    }

    /*
     * اختيار شعار علوي جديد.
     *
     * لا يستبدل الشعار القديم داخل المكتبة.
     * ينشئ ملفًا جديدًا مستقلًا.
     */
    val topLogoPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (uri != null) {

                val newLogo =
                    addLogoToLibrary(
                        context = context,
                        uri = uri,
                        displayName = "شعار إضافي ${logoLibrary.size}"
                    )

                message =
                    if (newLogo != null) {

                        topLogoPath =
                            newLogo.filePath

                        logoLibrary =
                            loadRoyalLogoLibrary(
                                context
                            )

                        "تمت إضافة الشعار العلوي إلى مكتبة الشعارات ✓"

                    } else {

                        "تعذر حفظ الشعار العلوي"
                    }
            }
        }

    /*
     * اختيار شعار سفلي جديد.
     *
     * أيضًا يحفظ نسخة مستقلة.
     */
    val bottomLogoPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (uri != null) {

                val newLogo =
                    addLogoToLibrary(
                        context = context,
                        uri = uri,
                        displayName = "شعار إضافي ${logoLibrary.size}"
                    )

                message =
                    if (newLogo != null) {

                        bottomLogoPath =
                            newLogo.filePath

                        logoLibrary =
                            loadRoyalLogoLibrary(
                                context
                            )

                        "تمت إضافة الشعار السفلي إلى مكتبة الشعارات ✓"

                    } else {

                        "تعذر حفظ الشعار السفلي"
                    }
            }
        }

    Card(
        modifier =
            modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            Text(
                text = "إعدادات واجهة الكافيه",
                color = AdminGold,
                fontSize = 22.sp
            )

            Text(
                text =
                    "إدارة الشعارات ونص الشريط الإخباري",
                color = AdminCream,
                fontSize = 14.sp
            )

            Spacer(
                Modifier.height(4.dp)
            )

            Text(
                text =
                    "👑 الشعار الأساسي محفوظ دائمًا ولا يتم حذفه",
                color = AdminGold,
                fontSize = 14.sp
            )

            Text(
                text =
                    "عدد الشعارات المحفوظة: ${logoLibrary.size}",
                color = AdminCream,
                fontSize = 13.sp
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Button(
                    onClick = {
                        topLogoPicker.launch(
                            "image/png"
                        )
                    },

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        "🖼️ إضافة شعار"
                    )
                }

                Button(
                    onClick = {
                        bottomLogoPicker.launch(
                            "image/png"
                        )
                    },

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        "🖼️ إضافة شعار آخر"
                    )
                }
            }

            /*
             * عرض مكتبة الشعارات الحالية.
             *
             * لا يوجد حذف هنا في هذه المرحلة.
             * خصوصًا أن الشعار الأساسي يجب أن يبقى دائمًا.
             */
            Text(
                text = "📚 مكتبة الشعارات",
                color = AdminGold,
                fontSize = 17.sp
            )

            logoLibrary.forEach { logo ->

                val animationText =
                    if (
                        logo.animationType ==
                        ROYAL_ANIMATION_LAYERED
                    ) {
                        "حركة الشعار الأساسي"
                    } else {
                        "3D + توقف 5 ثوانٍ"
                    }

                Text(
                    text =
                        if (
                            logo.id ==
                            ROYAL_DEFAULT_LOGO_ID
                        ) {
                            "👑 ${logo.name} — $animationText"
                        } else {
                            "🖼️ ${logo.name} — $animationText"
                        },

                    color = AdminCream,
                    fontSize = 13.sp
                )
            }

            OutlinedTextField(
                value = newsText,

                onValueChange = {
                    newsText = it
                },

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text(
                        "نص الشريط الإخباري"
                    )
                },

                singleLine = true
            )

            Button(
                onClick = {

                    saveRoyalInterfaceSettings(
                        context = context,
                        newsText = newsText,
                        topLogoPath = topLogoPath,
                        bottomLogoPath = bottomLogoPath
                    )

                    logoLibrary =
                        loadRoyalLogoLibrary(
                            context
                        )

                    message =
                        "تم حفظ إعدادات الواجهة ومكتبة الشعارات بنجاح ✓"
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    "📌 حفظ التعديلات"
                )
            }

            if (
                message.isNotEmpty()
            ) {

                Text(
                    text = message,
                    color = AdminGold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
