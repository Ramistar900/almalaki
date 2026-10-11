package com.almalaki.royaltv

import android.util.Log
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

private const val ROYAL_TV_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val ROYAL_TV_KEY =
    sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd

private const val ROYAL_TV_EVENT =
    "royal_tv_command"

/**
 * مستقبل أوامر تطبيق ROYAL TV المستقل.
 *
 * يستخدم القناة نفسها التي يستخدمها تطبيق المالك:
 * royal-tv-{deviceId}
 *
 * هذه المرحلة تستقبل الأوامر فقط؛
 * تنفيذ كل أمر على الشاشة سيتم ربطه في المرحلة التالية.
 */
object RoyalTVRealtime {

    private var scope: CoroutineScope? = null
    private var channel: RealtimeChannel? = null
    private var listenJob: Job? = null

    @Synchronized
    fun start(
        deviceId: String,
        onCommand: (RoyalTVCommand) -> Unit
    ) {
        if (deviceId.isBlank()) {
            Log.e("RoyalTVRealtime", "معرّف الجهاز فارغ")
            return
        }

        if (ROYAL_TV_KEY ==
            "PASTE_EXISTING_SUPABASE_PUBLISHABLE_KEY_HERE"
        ) {
            Log.e(
                "RoyalTVRealtime",
                "يجب إدخال مفتاح Supabase publishable الموجود في تطبيق المالك"
            )
            return
        }

        stop()

        val receiverScope = CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        )

        scope = receiverScope

        receiverScope.launch {
            try {
                val supabase = createSupabaseClient(
                    supabaseUrl = ROYAL_TV_URL,
                    supabaseKey = ROYAL_TV_KEY
                ) {
                    install(Realtime)
                }

                val deviceChannel = supabase.channel(
                    "royal-tv-$deviceId"
                )

                channel = deviceChannel

                val commandFlow =
                    deviceChannel.broadcastFlow<JsonObject>(
                        event = ROYAL_TV_EVENT
                    )

                listenJob = launch {
                    commandFlow.collect { payload ->
                        val typeName = payload["type"]
                            ?.toString()
                            ?.trim('"')
                            ?: return@collect

                        val commandType = try {
                            RoyalTVCommandType.valueOf(typeName)
                        } catch (error: IllegalArgumentException) {
                            Log.w(
                                "RoyalTVRealtime",
                                "نوع أمر غير معروف: $typeName"
                            )
                            return@collect
                        }

                        val commandPayload = payload["payload"]
                            ?.toString()
                            ?.trim('"')
                            ?: ""

                        val createdAt = payload["createdAt"]
                            ?.toString()
                            ?.trim('"')
                            ?.toLongOrNull()
                            ?: System.currentTimeMillis()

                        try {
                            onCommand(
                                RoyalTVCommand(
                                    type = commandType,
                                    payload = commandPayload,
                                    createdAt = createdAt
                                )
                            )
                        } catch (error: Exception) {
                            Log.e(
                                "RoyalTVRealtime",
                                "تعذر تسليم الأمر للمستقبل",
                                error
                            )
                        }
                    }
                }

                deviceChannel.subscribe(
                    blockUntilSubscribed = true
                )

                Log.i(
                    "RoyalTVRealtime",
                    "بدأ الاستماع لقناة الجهاز: $deviceId"
                )
            } catch (error: Exception) {
                Log.e(
                    "RoyalTVRealtime",
                    "فشل الاتصال بقناة ROYAL TV",
                    error
                )
            }
        }
    }

    fun stop() {
        listenJob?.cancel()
        listenJob = null

        val oldScope = scope
        val oldChannel = channel

        scope = null
        channel = null

        if (oldScope != null) {
            oldScope.launch {
                try {
                    oldChannel?.unsubscribe()
                } catch (error: Exception) {
                    Log.w(
                        "RoyalTVRealtime",
                        "تعذر إغلاق القناة بصورة طبيعية",
                        error
                    )
                } finally {
                    oldScope.cancel()
                }
            }
        } else {
            oldScope?.cancel()
        }
    }
}
