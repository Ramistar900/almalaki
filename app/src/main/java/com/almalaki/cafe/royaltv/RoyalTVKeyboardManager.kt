package com.almalaki.cafe.royaltv

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RoyalTVKeyboardLanguage {
    ARABIC,
    ENGLISH
}

enum class RoyalTVKeyboardAction {
    CHARACTER,
    SPACE,
    DELETE,
    CLEAR,
    ENTER,
    SWITCH_LANGUAGE
}

data class RoyalTVKeyboardState(
    val text: String = "",
    val language: RoyalTVKeyboardLanguage =
        RoyalTVKeyboardLanguage.ARABIC,
    val visible: Boolean = false,
    val focused: Boolean = true
)

data class RoyalTVKeyboardEvent(
    val action: RoyalTVKeyboardAction,
    val value: String = "",
    val text: String = "",
    val language: RoyalTVKeyboardLanguage =
        RoyalTVKeyboardLanguage.ARABIC,
    val createdAt: Long = System.currentTimeMillis()
)

object RoyalTVKeyboardManager {

    private val _state =
        MutableStateFlow(
            RoyalTVKeyboardState()
        )

    val state:
        StateFlow<RoyalTVKeyboardState> =
        _state.asStateFlow()

    private val _lastEvent =
        MutableStateFlow<RoyalTVKeyboardEvent?>(null)

    val lastEvent:
        StateFlow<RoyalTVKeyboardEvent?> =
        _lastEvent.asStateFlow()

    private var eventHandler:
        ((RoyalTVKeyboardEvent) -> Unit)? = null

    /*
     * الحروف العربية المستخدمة في كيبورد ROYAL TV.
     */
    val arabicKeys: List<String> =
        listOf(
            "ض", "ص", "ث", "ق", "ف",
            "غ", "ع", "ه", "خ", "ح",
            "ج", "د", "ش", "س", "ي",
            "ب", "ل", "ا", "ت", "ن",
            "م", "ك", "ط", "ئ", "ء",
            "ؤ", "ر", "لا", "ى", "ة",
            "و", "ز", "ظ"
        )

    /*
     * الأحرف الإنجليزية.
     */
    val englishKeys: List<String> =
        listOf(
            "Q", "W", "E", "R", "T",
            "Y", "U", "I", "O", "P",
            "A", "S", "D", "F", "G",
            "H", "J", "K", "L",
            "Z", "X", "C", "V", "B",
            "N", "M"
        )

    /*
     * الأرقام.
     */
    val numberKeys: List<String> =
        listOf(
            "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "0"
        )

    fun initialize(
        handler:
            ((RoyalTVKeyboardEvent) -> Unit)? = null
    ) {

        eventHandler =
            handler
    }

    fun show() {

        _state.value =
            _state.value.copy(
                visible = true,
                focused = true
            )
    }

    fun hide() {

        _state.value =
            _state.value.copy(
                visible = false,
                focused = false
            )
    }

    fun focus() {

        _state.value =
            _state.value.copy(
                focused = true
            )
    }

    fun unfocus() {

        _state.value =
            _state.value.copy(
                focused = false
            )
    }

    fun typeCharacter(
        character: String
    ) {

        if (character.isEmpty()) {
            return
        }

        val newText =
            _state.value.text +
                character

        _state.value =
            _state.value.copy(
                text = newText,
                visible = true,
                focused = true
            )

        emitEvent(
            action =
                RoyalTVKeyboardAction.CHARACTER,
            value = character,
            text = newText
        )
    }

    fun typeText(
        text: String
    ) {

        if (text.isEmpty()) {
            return
        }

        val newText =
            _state.value.text +
                text

        _state.value =
            _state.value.copy(
                text = newText,
                visible = true,
                focused = true
            )

        emitEvent(
            action =
                RoyalTVKeyboardAction.CHARACTER,
            value = text,
            text = newText
        )
    }

    fun space() {

        typeCharacter(" ")
    }

    fun delete() {

        val currentText =
            _state.value.text

        if (currentText.isEmpty()) {
            return
        }

        val newText =
            currentText.dropLast(1)

        _state.value =
            _state.value.copy(
                text = newText,
                visible = true,
                focused = true
            )

        emitEvent(
            action =
                RoyalTVKeyboardAction.DELETE,
            text = newText
        )
    }

    fun clear() {

        _state.value =
            _state.value.copy(
                text = "",
                visible = true,
                focused = true
            )

        emitEvent(
            action =
                RoyalTVKeyboardAction.CLEAR,
            text = ""
        )
    }

    fun enter() {

        val currentText =
            _state.value.text

        emitEvent(
            action =
                RoyalTVKeyboardAction.ENTER,
            text = currentText
        )
    }

    fun switchLanguage() {

        val newLanguage =
            when (
                _state.value.language
            ) {

                RoyalTVKeyboardLanguage.ARABIC ->
                    RoyalTVKeyboardLanguage.ENGLISH

                RoyalTVKeyboardLanguage.ENGLISH ->
                    RoyalTVKeyboardLanguage.ARABIC
            }

        _state.value =
            _state.value.copy(
                language = newLanguage,
                visible = true,
                focused = true
            )

        emitEvent(
            action =
                RoyalTVKeyboardAction.SWITCH_LANGUAGE,
            text = _state.value.text,
            language = newLanguage
        )
    }

    fun setLanguage(
        language: RoyalTVKeyboardLanguage
    ) {

        if (
            _state.value.language ==
                language
        ) {
            return
        }

        _state.value =
            _state.value.copy(
                language = language,
                visible = true,
                focused = true
            )

        emitEvent(
            action =
                RoyalTVKeyboardAction.SWITCH_LANGUAGE,
            text = _state.value.text,
            language = language
        )
    }

    fun setText(
        text: String
    ) {

        _state.value =
            _state.value.copy(
                text = text,
                visible = true,
                focused = true
            )
    }

    fun getCurrentText(): String {

        return _state.value.text
    }

    fun getCurrentLanguage():
        RoyalTVKeyboardLanguage {

        return _state.value.language
    }

    fun getCurrentKeys():
        List<String> {

        return when (
            _state.value.language
        ) {

            RoyalTVKeyboardLanguage.ARABIC ->
                arabicKeys

            RoyalTVKeyboardLanguage.ENGLISH ->
                englishKeys
        }
    }

    fun clearLastEvent() {

        _lastEvent.value =
            null
    }

    fun setEventHandler(
        handler:
            ((RoyalTVKeyboardEvent) -> Unit)?
    ) {

        eventHandler =
            handler
    }

    fun stop() {

        eventHandler =
            null

        _lastEvent.value =
            null

        _state.value =
            RoyalTVKeyboardState()
    }

    private fun emitEvent(
        action: RoyalTVKeyboardAction,
        value: String = "",
        text: String,
        language:
            RoyalTVKeyboardLanguage =
            _state.value.language
    ) {

        val event =
            RoyalTVKeyboardEvent(
                action = action,
                value = value,
                text = text,
                language = language
            )

        _lastEvent.value =
            event

        eventHandler?.invoke(
            event
        )
    }
}
