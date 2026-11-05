package com.almalaki.royaltv

import android.util.Log
import io.github.jan.supabase.SupabaseClient
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
    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

private const val ROYAL_TV_EVENT =
    "royal_tv_command"

/**
 * مستقبل أوامر تطبيق ROYAL TV المستقل.
 *
 * يستقبل الأوامر من قناة خاصة يحددها الخادم بعد تسجيل الجهاز.
 * لا يستخدم معرّف الشاشة المحلي كهوية قناة أو كوسيلة صلاحية.
 */
object RoyalTVRealtime {

    private var scope: CoroutineScope? = null
    private var channel: RealtimeChannel? = null
    private var listenJob: Job? = null

    @Synchronized
    fun start(
        supabase: SupabaseClient,
        channelTopic: String,
        onCommand: (RoyalTVCommand) -> Unit
    ) {
        if (!channelTopic.matches(
                Regex("^royal-tv:[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE)
            )
        ) {
            Log.e("RoyalTVRealtime", "موضوع قناة الجهاز غير صالح")
            return
        }

        stop()

        val receiverScope = CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        )

        scope = receiverScope

        receiverScope.launch {
            try {
                // The client carries the device's persisted Supabase Auth
                // session. Realtime resolves refreshed access tokens from Auth.
                val deviceChannel = supabase.channel(channelTopic) {
                    isPrivate = true
                }

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
                    "بدأ الاستماع للقناة الخاصة للجهاز: $channelTopic"
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
