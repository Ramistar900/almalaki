package com.almalaki.cafe.royaltv

import android.content.Context
import android.content.SharedPreferences

object RoyalTVDisplayIdAllocator {

private const val PREFS_NAME =
    "royal_tv_display_id_allocator"

private const val KEY_DISPLAY_ID =
    "display_id"

private const val KEY_NEXT_NUMBER =
    "next_number"

private const val FIRST_NUMBER =
    1

private const val DISPLAY_PREFIX =
    "ROYAL-TV-"

private var preferences: SharedPreferences? = null

fun initialize(
    context: Context
) {
    if (preferences != null) {
        return
    }

    preferences =
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
}

fun getDisplayId(
    context: Context
): String {

    initialize(context)

    val prefs =
        preferences
            ?: return createDisplayId(
                FIRST_NUMBER
            )

    val savedId =
        prefs.getString(
            KEY_DISPLAY_ID,
            null
        )

    if (!savedId.isNullOrBlank()) {
        return savedId
    }

    val nextNumber =
        getNextNumber(prefs)

    val displayId =
        createDisplayId(
            nextNumber
        )

    prefs.edit()
        .putString(
            KEY_DISPLAY_ID,
            displayId
        )
        .apply()

    saveNextNumber(
        prefs,
        nextNumber + 1
    )

    return displayId
}

fun hasDisplayId(
    context: Context
): Boolean {

    initialize(context)

    return preferences
        ?.contains(KEY_DISPLAY_ID)
        ?: false
}

fun getDisplayNumber(
    context: Context
): Int {

    val displayId =
        getDisplayId(context)

    return displayId
        .removePrefix(DISPLAY_PREFIX)
        .toIntOrNull()
        ?: FIRST_NUMBER
}

fun setDisplayId(
    context: Context,
    displayId: String
): Boolean {

    initialize(context)

    val cleanId =
        displayId
            .trim()
            .uppercase()

    if (!isValidDisplayId(cleanId)) {
        return false
    }

    preferences
        ?.edit()
        ?.putString(
            KEY_DISPLAY_ID,
            cleanId
        )
        ?.apply()

    val number =
        extractNumber(cleanId)

    if (number != null) {

        val currentNext =
            preferences
                ?.getInt(
                    KEY_NEXT_NUMBER,
                    FIRST_NUMBER
                )
                ?: FIRST_NUMBER

        if (number >= currentNext) {

            saveNextNumber(
                preferences,
                number + 1
            )
        }
    }

    return true
}

fun reset(
    context: Context
) {

    initialize(context)

    preferences
        ?.edit()
        ?.remove(KEY_DISPLAY_ID)
        ?.remove(KEY_NEXT_NUMBER)
        ?.apply()
}

private fun getNextNumber(
    prefs: SharedPreferences
): Int {

    return prefs.getInt(
        KEY_NEXT_NUMBER,
        FIRST_NUMBER
    ).coerceAtLeast(
        FIRST_NUMBER
    )
}

private fun saveNextNumber(
    prefs: SharedPreferences?,
    number: Int
) {

    prefs
        ?.edit()
        ?.putInt(
            KEY_NEXT_NUMBER,
            number.coerceAtLeast(
                FIRST_NUMBER
            )
        )
        ?.apply()
}

private fun createDisplayId(
    number: Int
): String {

    return DISPLAY_PREFIX +
        number
            .coerceAtLeast(
                FIRST_NUMBER
            )
            .toString()
            .padStart(
                3,
                '0'
            )
}

private fun isValidDisplayId(
    displayId: String
): Boolean {

    val number =
        extractNumber(displayId)

    return number != null &&
        number >= FIRST_NUMBER
}

private fun extractNumber(
    displayId: String
): Int? {

    if (
        !displayId.startsWith(
            DISPLAY_PREFIX
        )
    ) {
        return null
    }

    return displayId
        .removePrefix(
            DISPLAY_PREFIX
        )
        .toIntOrNull()
}

}
