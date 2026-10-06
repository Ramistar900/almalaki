package com.almalaki.cafe.royaltv

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/**
 * نموذج نص مستقل داخل قناة ROYAL TV.
 *
 * الإحداثيات x/y نسبية من 0.0 إلى 1.0:
 * 0.0 = بداية المساحة
 * 1.0 = نهايتها
 *
 * هذا يجعل موضع النص متكيفًا مع 720p و1080p و4K
 * ومع اختلاف أبعاد شاشة TV.
 */
data class RoyalTVText(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val enabled: Boolean = true,
    val fontSize: Float = 32f,
    val color: Long = 0xFFFFFFFF,
    val positionX: Float = 0.5f,
    val positionY: Float = 0.5f,
    val alpha: Float = 1f
)

/**
 * مدير نصوص ROYAL TV.
 *
 * المسؤوليات:
 *
 * 1. إضافة نصوص متعددة.
 * 2. تعديل النص والحجم واللون والموقع.
 * 3. حذف النصوص.
 * 4. إظهار/إخفاء النص.
 * 5. حفظ جميع النصوص محليًا على جهاز TV.
 * 6. استعادة النصوص بعد إعادة تشغيل التطبيق.
 * 7. نشر التغييرات فورًا عبر StateFlow.
 *
 * لا يحتوي هذا الملف على واجهة رسومية.
 * الشاشة تقرأ stateFlow وتعرض النصوص فعليًا.
 */
object RoyalTVTextManager {

    private const val PREFS_NAME =
        "royal_tv_texts"

    private const val KEY_TEXTS =
        "texts"

    private val _texts =
        MutableStateFlow<List<RoyalTVText>>(emptyList())

    val texts: StateFlow<List<RoyalTVText>> =
        _texts

    private var initialized =
        false

    private var appContext: Context? =
        null

    /**
     * تهيئة مدير النصوص واستعادة الحالة المحفوظة.
     */
    fun initialize(
        context: Context
    ) {
        appContext =
            context.applicationContext

        _texts.value =
            loadTexts()

        initialized = true
    }

    /**
     * إضافة نص جديد.
     */
    fun addText(
        text: String,
        fontSize: Float = 32f,
        color: Long = 0xFFFFFFFF,
        positionX: Float = 0.5f,
        positionY: Float = 0.5f,
        alpha: Float = 1f
    ): RoyalTVText? {

        val cleanText =
            text.trim()

        if (cleanText.isBlank()) {
            return null
        }

        val item =
            RoyalTVText(
                text = cleanText,
                fontSize = clampFontSize(
                    fontSize
                ),
                color = color,
                positionX = clampUnit(
                    positionX
                ),
                positionY = clampUnit(
                    positionY
                ),
                alpha = clampUnit(
                    alpha
                )
            )

        updateTexts(
            _texts.value + item
        )

        return item
    }

    /**
     * تعديل نص موجود.
     *
     * كل المعاملات اختيارية حتى نستطيع تغيير خاصية واحدة
     * دون لمس بقية خصائص النص.
     */
    fun updateText(
        id: String,
        text: String? = null,
        enabled: Boolean? = null,
        fontSize: Float? = null,
        color: Long? = null,
        positionX: Float? = null,
        positionY: Float? = null,
        alpha: Float? = null
    ): Boolean {

        var changed = false

        val updated =
            _texts.value.map { item ->

                if (item.id != id) {
                    return@map item
                }

                changed = true

                item.copy(
                    text =
                        text?.trim()?.takeIf {
                            it.isNotBlank()
                        } ?: item.text,

                    enabled =
                        enabled
                            ?: item.enabled,

                    fontSize =
                        fontSize?.let {
                            clampFontSize(it)
                        }
                            ?: item.fontSize,

                    color =
                        color ?: item.color,

                    positionX =
                        positionX?.let {
                            clampUnit(it)
                        }
                            ?: item.positionX,

                    positionY =
                        positionY?.let {
                            clampUnit(it)
                        }
                            ?: item.positionY,

                    alpha =
                        alpha?.let {
                            clampUnit(it)
                        }
                            ?: item.alpha
                )
            }

        if (changed) {
            updateTexts(updated)
        }

        return changed
    }

    /**
     * حذف نص.
     */
    fun deleteText(
        id: String
    ): Boolean {

        val old =
            _texts.value

        val updated =
            old.filterNot {
                it.id == id
            }

        if (updated.size == old.size) {
            return false
        }

        updateTexts(updated)
        return true
    }

    /**
     * حذف جميع النصوص.
     */
    fun clearAll() {
        updateTexts(emptyList())
    }

    /**
     * تغيير حالة إظهار نص.
     */
    fun setEnabled(
        id: String,
        enabled: Boolean
    ): Boolean {
        return updateText(
            id = id,
            enabled = enabled
        )
    }

    /**
     * تحريك النص.
     *
     * x/y قيم نسبية من 0 إلى 1.
     */
    fun moveText(
        id: String,
        positionX: Float,
        positionY: Float
    ): Boolean {
        return updateText(
            id = id,
            positionX = positionX,
            positionY = positionY
        )
    }

    /**
     * تغيير حجم النص.
     */
    fun setFontSize(
        id: String,
        fontSize: Float
    ): Boolean {
        return updateText(
            id = id,
            fontSize = fontSize
        )
    }

    /**
     * تغيير لون النص.
     */
    fun setColor(
        id: String,
        color: Long
    ): Boolean {
        return updateText(
            id = id,
            color = color
        )
    }

    /**
     * تغيير شفافية النص.
     */
    fun setAlpha(
        id: String,
        alpha: Float
    ): Boolean {
        return updateText(
            id = id,
            alpha = alpha
        )
    }

    /**
     * الحصول على نص محدد.
     */
    fun getText(
        id: String
    ): RoyalTVText? {
        return _texts.value.firstOrNull {
            it.id == id
        }
    }

    /**
     * إعادة ضبط مدير النصوص.
     */
    fun reset() {
        updateTexts(emptyList())
    }

    private fun updateTexts(
        value: List<RoyalTVText>
    ) {
        _texts.value =
            value

        saveTexts(
            value
        )
    }

    private fun saveTexts(
        value: List<RoyalTVText>
    ) {

        val context =
            appContext
                ?: return

        val array =
            JSONArray()

        value.forEach { item ->

            val objectValue =
                JSONObject()
                    .apply {
                        put(
                            "id",
                            item.id
                        )
                        put(
                            "text",
                            item.text
                        )
                        put(
                            "enabled",
                            item.enabled
                        )
                        put(
                            "fontSize",
                            item.fontSize
                        )
                        put(
                            "color",
                            item.color
                        )
                        put(
                            "positionX",
                            item.positionX
                        )
                        put(
                            "positionY",
                            item.positionY
                        )
                        put(
                            "alpha",
                            item.alpha
                        )
                    }

            array.put(
                objectValue
            )
        }

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_TEXTS,
                array.toString()
            )
            .apply()
    }

    private fun loadTexts():
        List<RoyalTVText> {

        val context =
            appContext
                ?: return emptyList()

        val raw =
            context
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                .getString(
                    KEY_TEXTS,
                    null
                )
                ?: return emptyList()

        return try {

            val array =
                JSONArray(raw)

            buildList {

                for (index in 0 until array.length()) {

                    val objectValue =
                        array.getJSONObject(
                            index
                        )

                    val text =
                        objectValue.optString(
                            "text",
                            ""
                        ).trim()

                    if (text.isBlank()) {
                        continue
                    }

                    add(
                        RoyalTVText(
                            id =
                                objectValue.optString(
                                    "id",
                                    UUID.randomUUID().toString()
                                ),
                            text = text,
                            enabled =
                                objectValue.optBoolean(
                                    "enabled",
                                    true
                                ),
                            fontSize =
                                clampFontSize(
                                    objectValue.optDouble(
                                        "fontSize",
                                        32.0
                                    ).toFloat()
                                ),
                            color =
                                objectValue.optLong(
                                    "color",
                                    0xFFFFFFFF
                                ),
                            positionX =
                                clampUnit(
                                    objectValue.optDouble(
                                        "positionX",
                                        0.5
                                    ).toFloat()
                                ),
                            positionY =
                                clampUnit(
                                    objectValue.optDouble(
                                        "positionY",
                                        0.5
                                    ).toFloat()
                                ),
                            alpha =
                                clampUnit(
                                    objectValue.optDouble(
                                        "alpha",
                                        1.0
                                    ).toFloat()
                                )
                        )
                    )
                }
            }

        } catch (
            exception: Exception
        ) {
            emptyList()
        }
    }

    private fun clampUnit(
        value: Float
    ): Float {
        return min(
            1f,
            max(
                0f,
                value
            )
        )
    }

    private fun clampFontSize(
        value: Float
    ): Float {
        return min(
            200f,
            max(
                8f,
                value
            )
        )
    }
}
