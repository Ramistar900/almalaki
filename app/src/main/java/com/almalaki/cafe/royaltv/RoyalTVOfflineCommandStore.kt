package com.almalaki.cafe.royaltv

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * مخزن محلي دائم للأوامر التي لم يتم إرسالها بسبب Offline.
 *
 * لا علاقة له بحالة عرض الشاشة.
 *
 * RoyalTVLocalDisplayEngine
 * مسؤول عن حالة العرض المحلية.
 *
 * هذا الملف مسؤول فقط عن:
 *
 * Offline → حفظ الأمر
 * Online  → قراءة الأمر وإرساله
 *
 * مهم:
 * كل أمر محفوظ يحتوي أيضًا على
 * Target Device ID
 * حتى نعرف إلى أي شاشة يجب إرسال الأمر
 * عند عودة الاتصال.
 */
data class RoyalTVPendingCommand(
    val targetDeviceId: String,
    val command: RoyalTVCommand
)

object RoyalTVOfflineCommandStore {

    private const val PREFS_NAME =
        "royal_tv_offline_commands"

    private const val KEY_COMMANDS =
        "commands"

    private const val KEY_TARGET_DEVICE_ID =
        "targetDeviceId"

    private const val KEY_TYPE =
        "type"

    private const val KEY_PAYLOAD =
        "payload"

    private const val KEY_CREATED_AT =
        "createdAt"

    private var preferences:
        SharedPreferences? = null

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

    /**
     * إضافة أمر إلى قائمة الانتظار المحلية
     * مع حفظ الشاشة المستهدفة.
     */
    @Synchronized
    fun enqueue(
        targetDeviceId: String,
        command: RoyalTVCommand
    ) {

        val cleanTarget =
            targetDeviceId.trim()

        if (cleanTarget.isBlank()) {
            return
        }

        val current =
            readPendingCommands().toMutableList()

        current.add(
            RoyalTVPendingCommand(
                targetDeviceId = cleanTarget,
                command = command
            )
        )

        writePendingCommands(
            current
        )
    }

    /**
     * توافق مع الاستدعاءات القديمة.
     *
     * هذا الاستدعاء لا يستطيع تحديد الشاشة المستهدفة،
     * لذلك لا يستخدم للمزامنة الجديدة.
     */
    @Synchronized
    fun enqueue(
        command: RoyalTVCommand
    ) {

        val current =
            readCommands().toMutableList()

        current.add(command)

        writeCommands(
            current
        )
    }

    /**
     * قراءة جميع الأوامر المنتظرة
     * مع الشاشة المستهدفة.
     */
    @Synchronized
    fun getPendingCommands():
        List<RoyalTVPendingCommand> {

        return readPendingCommands()
    }

    /**
     * قراءة الأوامر فقط للتوافق القديم.
     */
    @Synchronized
    fun getCommands():
        List<RoyalTVCommand> {

        return readCommands()
    }

    /**
     * عدد الأوامر المنتظرة.
     */
    @Synchronized
    fun size(): Int {

        return readPendingCommands().size +
            readLegacyCommands().size
    }

    /**
     * هل توجد أوامر تنتظر الاتصال؟
     */
    @Synchronized
    fun hasPendingCommands(): Boolean {

        return readPendingCommands().isNotEmpty() ||
            readLegacyCommands().isNotEmpty()
    }

    /**
     * حذف أمر واحد بعد نجاح إرساله.
     */
    @Synchronized
    fun remove(
        pendingCommand:
            RoyalTVPendingCommand
    ) {

        val current =
            readPendingCommands()
                .toMutableList()

        val index =
            current.indexOfFirst {

                it.targetDeviceId ==
                    pendingCommand.targetDeviceId &&
                    it.command.createdAt ==
                    pendingCommand.command.createdAt &&
                    it.command.type ==
                    pendingCommand.command.type &&
                    it.command.payload ==
                    pendingCommand.command.payload
            }

        if (index >= 0) {

            current.removeAt(index)

            writePendingCommands(
                current
            )
        }
    }

    /**
     * حذف أمر واحد بالطريقة القديمة.
     */
    @Synchronized
    fun remove(
        command: RoyalTVCommand
    ) {

        val current =
            readCommands()
                .toMutableList()

        val index =
            current.indexOfFirst {

                it.createdAt ==
                    command.createdAt &&
                    it.type ==
                    command.type &&
                    it.payload ==
                    command.payload
            }

        if (index >= 0) {

            current.removeAt(index)

            writeCommands(
                current
            )
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

    /**
     * قراءة الأوامر الجديدة التي تحتوي
     * على Target Device ID.
     */
    private fun readPendingCommands():
        List<RoyalTVPendingCommand> {

        val prefs =
            preferences
                ?: return emptyList()

        val raw =
            prefs.getString(
                KEY_COMMANDS,
                null
            )
                ?: return emptyList()

        return try {

            val array =
                JSONArray(raw)

            val commands =
                mutableListOf<RoyalTVPendingCommand>()

            for (
                index in
                    0 until array.length()
            ) {

                val item =
                    array.optJSONObject(index)
                        ?: continue

                val targetDeviceId =
                    item.optString(
                        KEY_TARGET_DEVICE_ID,
                        ""
                    ).trim()

                if (
                    targetDeviceId.isBlank()
                ) {
                    continue
                }

                val command =
                    parseCommand(item)
                        ?: continue

                commands.add(
                    RoyalTVPendingCommand(
                        targetDeviceId =
                            targetDeviceId,
                        command = command
                    )
                )
            }

            commands

        } catch (_: Exception) {

            emptyList()
        }
    }

    /**
     * قراءة الأوامر القديمة التي لا تحتوي
     * على Target Device ID.
     */
    private fun readLegacyCommands():
        List<RoyalTVCommand> {

        val prefs =
            preferences
                ?: return emptyList()

        val raw =
            prefs.getString(
                KEY_COMMANDS,
                null
            )
                ?: return emptyList()

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

                val targetDeviceId =
                    item.optString(
                        KEY_TARGET_DEVICE_ID,
                        ""
                    )

                if (
                    targetDeviceId.isNotBlank()
                ) {
                    continue
                }

                val command =
                    parseCommand(item)
                        ?: continue

                commands.add(command)
            }

            commands

        } catch (_: Exception) {

            emptyList()
        }
    }

    /**
     * قراءة جميع الأوامر القديمة
     * للتوافق مع الواجهة السابقة.
     */
    private fun readCommands():
        List<RoyalTVCommand> {

        return readLegacyCommands()
    }

    /**
     * تحويل JSON إلى RoyalTVCommand.
     */
    private fun parseCommand(
        item: JSONObject
    ): RoyalTVCommand? {

        val typeName =
            item.optString(
                KEY_TYPE,
                ""
            )

        val type =
            try {

                RoyalTVCommandType.valueOf(
                    typeName
                )

            } catch (_: Exception) {

                return null
            }

        val payload =
            item.optString(
                KEY_PAYLOAD,
                ""
            )

        val createdAt =
            item.optLong(
                KEY_CREATED_AT,
                System.currentTimeMillis()
            )

        return RoyalTVCommand(
            type = type,
            payload = payload,
            createdAt = createdAt
        )
    }

    /**
     * كتابة الأوامر الجديدة.
     */
    private fun writePendingCommands(
        commands:
            List<RoyalTVPendingCommand>
    ) {

        val prefs =
            preferences
                ?: return

        val array =
            JSONArray()

        commands.forEach {
            pending ->

            val item =
                JSONObject()

            item.put(
                KEY_TARGET_DEVICE_ID,
                pending.targetDeviceId
            )

            item.put(
                KEY_TYPE,
                pending.command.type.name
            )

            item.put(
                KEY_PAYLOAD,
                pending.command.payload
            )

            item.put(
                KEY_CREATED_AT,
                pending.command.createdAt
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

    /**
     * كتابة الأوامر القديمة.
     */
    private fun writeCommands(
        commands:
            List<RoyalTVCommand>
    ) {

        val prefs =
            preferences
                ?: return

        val array =
            JSONArray()

        commands.forEach { command ->

            val item =
                JSONObject()

            item.put(
                KEY_TYPE,
                command.type.name
            )

            item.put(
                KEY_PAYLOAD,
                command.payload
            )

            item.put(
                KEY_CREATED_AT,
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
