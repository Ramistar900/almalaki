package com.almalaki.cafe.royaltv

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val ROYAL_TV_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val ROYAL_TV_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFBIg_aez3fjQd"

private const val ROYAL_TV_EVENT =
    "royal_tv_command"

/**
 * قناة الاتصال المشتركة لنظام ROYAL TV.
 *
 * مستقلة عن SupabaseRealtime.kt الحالي
 * الخاص بإشعارات الطلبات.
 *
 * مسؤوليتها:
 *
 * 1. استقبال أوامر ROYAL TV.
 * 2. إرسال أوامر إلى جهاز محدد.
 * 3. إرجاع نتيجة حقيقية لعملية الإرسال:
 *
 * true  = تم الإرسال بنجاح.
 * false = فشل الإرسال.
 *
 * هذا مهم جدًا للمزامنة Offline → Online،
 * لأن الأمر لا يجب أن يُحذف من المخزن المحلي
 * إلا بعد نجاح الإرسال فعليًا.
 */
object RoyalTVRealtime {

    private var scope:
        CoroutineScope? = null

    private var channel:
        RealtimeChannel? = null

    private var listenJob:
        Job? = null

    /**
     * الاتصال بقناة جهاز ROYAL TV
     * والاستماع للأوامر.
     */
    fun start(
        deviceId: String,
        onCommand:
            (RoyalTVCommand) -> Unit
    ) {

        stop()

        val newScope =
            CoroutineScope(
                SupervisorJob() +
                    Dispatchers.IO
            )

        scope =
            newScope

        newScope.launch {

            try {

                val supabase =
                    createSupabaseClient(
                        supabaseUrl =
                            ROYAL_TV_URL,
                        supabaseKey =
                            ROYAL_TV_KEY
                    ) {

                        install(
                            Realtime
                        )
                    }

                val deviceChannel =
                    supabase.channel(
                        "royal-tv-$deviceId"
                    )

                channel =
                    deviceChannel

                val commandFlow =
                    deviceChannel
                        .broadcastFlow<JsonObject>(
                            event =
                                ROYAL_TV_EVENT
                        )

                listenJob =
                    launch {

                        commandFlow.collect {
                            payload ->

                            val typeName =
                                payload["type"]
                                    ?.toString()
                                    ?.trim('"')
                                    ?: return@collect

                            val commandType =
                                try {

                                    RoyalTVCommandType
                                        .valueOf(
                                            typeName
                                        )

                                } catch (_: Exception) {

                                    return@collect
                                }

                            val commandPayload =
                                payload["payload"]
                                    ?.toString()
                                    ?.trim('"')
                                    ?: ""

                            val createdAt =
                                payload["createdAt"]
                                    ?.toString()
                                    ?.trim('"')
                                    ?.toLongOrNull()
                                    ?: System
                                        .currentTimeMillis()

                            onCommand(
                                RoyalTVCommand(
                                    type =
                                        commandType,
                                    payload =
                                        commandPayload,
                                    createdAt =
                                        createdAt
                                )
                            )
                        }
                    }

                deviceChannel.subscribe(
                    blockUntilSubscribed =
                        true
                )

            } catch (e: Exception) {

                android.util.Log.e(
                    "RoyalTVRealtime",
                    "Connection error: ${e.message}",
                    e
                )
            }
        }
    }

    /**
     * إرسال أمر إلى جهاز ROYAL TV محدد.
     *
     * النتيجة:
     *
     * true
     * = تم إنشاء القناة والاشتراك
     *   وإرسال الأمر بنجاح.
     *
     * false
     * = حدث خطأ أثناء الإرسال.
     *
     * مهم:
     * لا نقوم هنا بحذف الأمر من Offline Store.
     *
     * ConnectionManager هو المسؤول
     * عن حذف الأمر بعد نجاح الإرسال.
     */
    suspend fun sendCommand(
        targetDeviceId: String,
        command: RoyalTVCommand
    ): Boolean {

        if (
            targetDeviceId
                .trim()
                .isBlank()
        ) {

            android.util.Log.e(
                "RoyalTVRealtime",
                "Send error: empty target device ID"
            )

            return false
        }

        return try {

            val supabase =
                createSupabaseClient(
                    supabaseUrl =
                        ROYAL_TV_URL,
                    supabaseKey =
                        ROYAL_TV_KEY
                ) {

                    install(
                        Realtime
                    )
                }

            val targetChannel =
                supabase.channel(
                    "royal-tv-$targetDeviceId"
                )

            targetChannel.subscribe(
                blockUntilSubscribed =
                    true
            )

            targetChannel.broadcast(
                event =
                    ROYAL_TV_EVENT,
                message =
                    buildJsonObject {

                        put(
                            "type",
                            command.type.name
                        )

                        put(
                            "payload",
                            command.payload
                        )

                        put(
                            "createdAt",
                            command.createdAt
                        )
                    }
            )

            targetChannel.unsubscribe()

            android.util.Log.d(
                "RoyalTVRealtime",
                "Command sent successfully: " +
                    "${command.type} → " +
                    targetDeviceId
            )

            true

        } catch (e: Exception) {

            android.util.Log.e(
                "RoyalTVRealtime",
                "Send error: ${e.message}",
                e
            )

            false
        }
    }

    /**
     * إيقاف قناة ROYAL TV الحالية.
     *
     * هذا يوقف اتصال Realtime فقط.
     *
     * لا يمسح:
     * - حالة العرض المحلية.
     * - الأوامر Offline.
     * - Resume State.
     */
    fun stop() {

        listenJob?.cancel()

        listenJob =
            null

        val currentScope =
            scope

        val currentChannel =
            channel

        channel =
            null

        scope =
            null

        if (
            currentScope != null &&
            currentChannel != null
        ) {

            currentScope.launch {

                try {

                    currentChannel.unsubscribe()

                } catch (e: Exception) {

                    android.util.Log.e(
                        "RoyalTVRealtime",
                        "Disconnect error: " +
                            "${e.message}",
                        e
                    )

                } finally {

                    currentScope.cancel()
                }
            }

        } else {

            currentScope?.cancel()
        }
    }
}
