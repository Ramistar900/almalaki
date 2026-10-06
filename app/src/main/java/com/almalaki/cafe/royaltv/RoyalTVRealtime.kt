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
 */
object RoyalTVRealtime {

    private var scope: CoroutineScope? = null
    private var channel: RealtimeChannel? = null
    private var listenJob: Job? = null

    /**
     * الاتصال بقناة جهاز ROYAL TV
     * والاستماع للأوامر.
     */
    fun start(
        deviceId: String,
        onCommand: (RoyalTVCommand) -> Unit
    ) {
        stop()

        val newScope =
            CoroutineScope(
                SupervisorJob() + Dispatchers.IO
            )

        scope = newScope

        newScope.launch {
            try {
                val supabase = createSupabaseClient(
                    supabaseUrl = ROYAL_TV_URL,
                    supabaseKey = ROYAL_TV_KEY
                ) {
                    install(Realtime)
                }

                val deviceChannel =
                    supabase.channel(
                        "royal-tv-$deviceId"
                    )

                channel = deviceChannel

                val commandFlow =
                    deviceChannel.broadcastFlow<JsonObject>(
                        event = ROYAL_TV_EVENT
                    )

                listenJob = launch {
                    commandFlow.collect { payload ->

                        val typeName =
                            payload["type"]
                                ?.toString()
                                ?.trim('"')
                                ?: return@collect

                        val commandType =
                            try {
                                RoyalTVCommandType.valueOf(
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
                                ?: System.currentTimeMillis()

                        onCommand(
                            RoyalTVCommand(
                                type = commandType,
                                payload = commandPayload,
                                createdAt = createdAt
                            )
                        )
                    }
                }

                deviceChannel.subscribe(
                    blockUntilSubscribed = true
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
     */
    suspend fun sendCommand(
        targetDeviceId: String,
        command: RoyalTVCommand
    ) {
        try {
            val supabase = createSupabaseClient(
                supabaseUrl = ROYAL_TV_URL,
                supabaseKey = ROYAL_TV_KEY
            ) {
                install(Realtime)
            }

            val targetChannel =
                supabase.channel(
                    "royal-tv-$targetDeviceId"
                )

            targetChannel.subscribe(
                blockUntilSubscribed = true
            )

            targetChannel.broadcast(
                event = ROYAL_TV_EVENT,
                message = buildJsonObject {
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

        } catch (e: Exception) {
            android.util.Log.e(
                "RoyalTVRealtime",
                "Send error: ${e.message}",
                e
            )
        }
    }

    /**
     * إيقاف قناة ROYAL TV الحالية.
     */
    fun stop() {
        listenJob?.cancel()
        listenJob = null

        val currentScope = scope
        val currentChannel = channel

        channel = null
        scope = null

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
                        "Disconnect error: ${e.message}",
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
