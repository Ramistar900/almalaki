package com.almalaki.royaltv

import android.content.Context
import org.json.JSONArray

/**
 * Small persistent deduplication window for commands already accepted locally.
 * It is written before the server acknowledgement so a lost ACK can be retried
 * without replaying the local display action.
 */
object RoyalTVProcessedCommandStore {
    private const val PREFS_NAME = "royal_tv_processed_commands"
    private const val KEY_COMMAND_IDS = "processed_ids"
    private const val MAX_RETAINED_IDS = 1024
    private val lock = Any()
    private val commandIdPattern =
        Regex("^[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$")

    fun contains(context: Context, commandId: String): Boolean = synchronized(lock) {
        if (!commandIdPattern.matches(commandId)) return@synchronized false
        readIds(context).contains(commandId)
    }

    fun markProcessed(context: Context, commandId: String): Boolean = synchronized(lock) {
        if (!commandIdPattern.matches(commandId)) return@synchronized false

        val ids = readIds(context)
        ids.remove(commandId)
        ids.add(commandId)
        while (ids.size > MAX_RETAINED_IDS) {
            ids.removeAt(0)
        }

        val array = JSONArray()
        ids.forEach(array::put)
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_COMMAND_IDS, array.toString())
            .commit()
    }

    fun clear(context: Context): Boolean = synchronized(lock) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_COMMAND_IDS)
            .commit()
    }

    private fun readIds(context: Context): MutableList<String> {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_COMMAND_IDS, "[]")
            .orEmpty()

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val id = array.optString(index)
                    if (commandIdPattern.matches(id) && id !in this) add(id)
                }
            }.toMutableList()
        }.getOrDefault(mutableListOf())
    }
}
