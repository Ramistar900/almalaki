package com.almalaki.cafe.royaltv

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RoyalTVKeyboardFocus(
    val row: Int = 0,
    val column: Int = 0
)

object RoyalTVKeyboardNavigation {

    private val _focus =
        MutableStateFlow(
            RoyalTVKeyboardFocus()
        )

    val focus:
        StateFlow<RoyalTVKeyboardFocus> =
        _focus.asStateFlow()

    private var rows: List<List<String>> =
        emptyList()

    fun setRows(
        newRows: List<List<String>>
    ) {

        rows =
            newRows

        if (rows.isEmpty()) {
            _focus.value =
                RoyalTVKeyboardFocus()
            return
        }

        val firstRow =
            rows.firstOrNull()
                ?: emptyList()

        _focus.value =
            RoyalTVKeyboardFocus(
                row = 0,
                column =
                    if (firstRow.isEmpty()) {
                        0
                    } else {
                        0
                    }
            )
    }

    fun up() {

        if (rows.isEmpty()) {
            return
        }

        val current =
            _focus.value

        val newRow =
            (current.row - 1)
                .coerceAtLeast(0)

        val targetRow =
            rows.getOrNull(newRow)
                ?: return

        if (targetRow.isEmpty()) {
            return
        }

        val newColumn =
            current.column.coerceIn(
                0,
                targetRow.lastIndex
            )

        _focus.value =
            RoyalTVKeyboardFocus(
                row = newRow,
                column = newColumn
            )
    }

    fun down() {

        if (rows.isEmpty()) {
            return
        }

        val current =
            _focus.value

        val newRow =
            (current.row + 1)
                .coerceAtMost(
                    rows.lastIndex
                )

        val targetRow =
            rows.getOrNull(newRow)
                ?: return

        if (targetRow.isEmpty()) {
            return
        }

        val newColumn =
            current.column.coerceIn(
                0,
                targetRow.lastIndex
            )

        _focus.value =
            RoyalTVKeyboardFocus(
                row = newRow,
                column = newColumn
            )
    }

    fun left() {

        if (rows.isEmpty()) {
            return
        }

        val current =
            _focus.value

        val row =
            rows.getOrNull(
                current.row
            )
                ?: return

        if (row.isEmpty()) {
            return
        }

        val newColumn =
            (
                current.column - 1
            ).coerceAtLeast(0)

        _focus.value =
            current.copy(
                column = newColumn
            )
    }

    fun right() {

        if (rows.isEmpty()) {
            return
        }

        val current =
            _focus.value

        val row =
            rows.getOrNull(
                current.row
            )
                ?: return

        if (row.isEmpty()) {
            return
        }

        val newColumn =
            (
                current.column + 1
            ).coerceAtMost(
                row.lastIndex
            )

        _focus.value =
            current.copy(
                column = newColumn
            )
    }

    fun select() {

        if (rows.isEmpty()) {
            return
        }

        val current =
            _focus.value

        val row =
            rows.getOrNull(
                current.row
            )
                ?: return

        val key =
            row.getOrNull(
                current.column
            )
                ?: return

        when (key) {

            "🌐" -> {
                RoyalTVKeyboardManager
                    .switchLanguage()
            }

            "مسافة" -> {
                RoyalTVKeyboardManager
                    .space()
            }

            "⌫" -> {
                RoyalTVKeyboardManager
                    .delete()
            }

            "مسح" -> {
                RoyalTVKeyboardManager
                    .clear()
            }

            "بحث" -> {
                RoyalTVKeyboardManager
                    .enter()
            }

            else -> {
                RoyalTVKeyboardManager
                    .typeCharacter(
                        key
                    )
            }
        }
    }

    fun getFocusedKey(): String? {

        if (rows.isEmpty()) {
            return null
        }

        val current =
            _focus.value

        return rows
            .getOrNull(
                current.row
            )
            ?.getOrNull(
                current.column
            )
    }

    fun reset() {

        _focus.value =
            RoyalTVKeyboardFocus()
    }
}
