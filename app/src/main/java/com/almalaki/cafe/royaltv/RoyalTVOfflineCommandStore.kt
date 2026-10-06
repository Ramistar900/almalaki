package com.almalaki.cafe.royaltv

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * مخزن محلي دائم للأوامر التي لم يتم إرسالها بسبب Offline.
 *
 * لا علاقة له بحالة عرض الشاشة.
 * RoyalTVLocalDisplayEngine مسؤول عن حالة العرض.
 *
 * هذا الملف مسؤول فقط عن:
 * Offline → حفظ الأمر
 * Online  → قراءة الأمر وإرساله
 */
object RoyalTVOfflineCommandStore {

    private const val PREFS_NAME =
        "royal_tv_offline_commands"

    private const val KEY_COMMANDS =
        "commands"

    private var preferences:
        SharedPreferences? = null

    fun initialize(context: Context) {

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

    /**
     * إضافة أمر إلى قائمة الانتظار المحلية.
     */
    @Synchronized
    fun enqueue(
        command: RoyalTVCommand
    ) {

        val current =
            readCommands().toMutableList()

        current.add(command)

        writeCommands(current)
    }

    /**
     * قراءة جميع الأوامر المنتظرة.
     */
    @Synchronized
    fun getPendingCommands():
        List<RoyalTVCommand> {

        return readCommands()
    }

    /**
     * عدد الأوامر المنتظرة.
     */
    @Synchronized
    fun size(): Int {

        return readCommands().size
    }

    /**
     * هل توجد أوامر تنتظر الاتصال؟
     */
    @Synchronized
    fun hasPendingCommands(): Boolean {

        return readCommands().isNotEmpty()
    }

    /**
     * حذف أمر واحد بعد نجاح إرساله.
     */
    @Synchronized
    fun remove(
        command: RoyalTVCommand
    ) {

        val current =
            readCommands().toMutableList()

        val index =
            current.indexOfFirst {
                it.createdAt == command.createdAt &&
                    it.type == command.type &&
                    it.payload == command.payload
            }

        if (index >= 0) {
            current.removeAt(index)
            writeCommands(current)
        }
    }

    /**
     * حذف جميع الأوامر المنتظرة.
     */
    @Synchronized
    fun clear() {

        preferences
            ?.edit()
            ?.remove(KEY_COMMANDS)
            ?.apply()
    }

    private fun readCommands():
        List<RoyalTVCommand> {

        val prefs =
            preferences ?: return emptyList()

        val raw =
            prefs.getString(
                KEY_COMMANDS,
                null
            ) ?: return emptyList()

        return try {

            val array =
                JSONArray(raw)

            val commands =
                mutableListOf<RoyalTVCommand>()

            for (
                index in
                    0 until array.length()
            ) {

                val item =
                    array.optJSONObject(index)
                        ?: continue

                val typeName =
                    item.optString(
                        "type",
                        ""
                    )

                val type =
                    try {
                        RoyalTVCommandType.valueOf(
                            typeName
                        )
                    } catch (_: Exception) {
                        continue
                    }

                val payload =
                    item.optString(
                        "payload",
                        ""
                    )

                val createdAt =
                    item.optLong(
                        "createdAt",
                        System.currentTimeMillis()
                    )

                commands.add(
                    RoyalTVCommand(
                        type = type,
                        payload = payload,
                        createdAt = createdAt
                    )
                )
            }

            commands

        } catch (_: Exception) {

            emptyList()
        }
    }

    private fun writeCommands(
        commands:
            List<RoyalTVCommand>
    ) {

        val prefs =
            preferences ?: return

        val array =
            JSONArray()

        commands.forEach { command ->

            val item =
                JSONObject()

            item.put(
                "type",
                command.type.name
            )

            item.put(
                "payload",
                command.payload
            )

            item.put(
                "createdAt",
                command.createdAt
            )

            array.put(item)
        }

        prefs.edit()
            .putString(
                KEY_COMMANDS,
                array.toString()
            )
            .apply()
    }
}
