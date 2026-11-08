package com.almalaki.royaltv

import android.content.Context
import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

private const val ROYAL_TV_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val ROYAL_TV_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

private const val ROYAL_TV_EVENT =
    "royal_tv_command"

/**
 * Authenticated private receiver for the standalone ROYAL TV app.
 *
 * The legacy channel topic is preserved so existing device identifiers remain
 * compatible. Only a server-registered TV session can join this private topic.
 */
object RoyalTVRealtime {

    private var scope: CoroutineScope? = null
    private var channel: RealtimeChannel? = null
    private var listenJob: Job? = null
    private var authRefreshJob: Job? = null

    @Synchronized
    fun start(
        context: Context,
        deviceId: String,
        onCommand: (RoyalTVCommand) -> Unit
    ) {
        if (deviceId.isBlank()) {
            Log.e("RoyalTVRealtime", "معرّف الجهاز فارغ")
            return
        }

        stop()

        val receiverScope = CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        )
        scope = receiverScope

        receiverScope.launch {
            try {
                val accessToken = RoyalTVDeviceAuthManager.validAccessToken(context)
                    ?: throw IllegalStateException(
                        "هذا التلفاز غير مسجل أو انتهت جلسته. أعد ربطه برمز تفعيل جديد."
                    )

                val supabase = createSupabaseClient(
                    supabaseUrl = ROYAL_TV_URL,
                    supabaseKey = ROYAL_TV_KEY
                ) {
                    install(Realtime)
                }

                // Set the TV's own JWT before creating or joining its channel.
                supabase.realtime.setAuth(accessToken)

                val deviceChannel = supabase.channel(
                    "royal-tv-$deviceId"
                ) {
                    isPrivate = true
                }
                channel = deviceChannel

                val commandFlow = deviceChannel.broadcastFlow<JsonObject>(
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
                            Log.w("RoyalTVRealtime", "نوع أمر غير معروف: $typeName")
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

                deviceChannel.subscribe(blockUntilSubscribed = true)

                Log.i(
                    "RoyalTVRealtime",
                    "تم الاتصال بالقناة الخاصة لجهاز ROYAL TV: $deviceId"
                )

                // Refresh the device JWT before expiry and update the live socket.
                authRefreshJob = launch {
                    while (isActive) {
                        delay(5 * 60 * 1000L)
                        val renewedToken =
                            RoyalTVDeviceAuthManager.validAccessToken(context)

                        if (renewedToken.isNullOrBlank()) {
                            Log.e(
                                "RoyalTVRealtime",
                                "تعذر تجديد جلسة التلفاز؛ سيتم إغلاق قناة الاستقبال."
                            )
                            deviceChannel.unsubscribe()
                            break
                        }

                        supabase.realtime.setAuth(renewedToken)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(
                    "RoyalTVRealtime",
                    "فشل الاتصال الآمن بقناة ROYAL TV: ${error.message}",
                    error
                )
            }
        }
    }

    fun stop() {
        listenJob?.cancel()
        listenJob = null
        authRefreshJob?.cancel()
        authRefreshJob = null

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
