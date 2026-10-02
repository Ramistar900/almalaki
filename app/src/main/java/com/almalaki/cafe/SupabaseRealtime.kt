package com.almalaki.cafe

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ROYAL_REALTIME_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val ROYAL_REALTIME_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFBIg_aez3fjQd"

private var royalRealtimeJob: Job? = null

private var royalRealtimeScope: CoroutineScope? = null

fun startRoyalOrderRealtime(
    accessToken: String,
    onNewOrder: () -> Unit
) {
    stopRoyalOrderRealtime()

    val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    royalRealtimeScope = scope

    royalRealtimeJob = scope.launch {
        try {
            val supabase = createSupabaseClient(
                supabaseUrl = ROYAL_REALTIME_URL,
                supabaseKey = ROYAL_REALTIME_KEY
            ) {
                install(Realtime)
            }

            supabase.realtime.setAuth(accessToken)

            val channel = supabase.channel("royal-orders")

            val changeFlow =
                channel.postgresChangeFlow<PostgresAction.Insert>(
                    schema = "public"
                ) {
                    table = "orders"
                }

            changeFlow
                .onEach {
                    withContext(Dispatchers.Main) {
                        onNewOrder()
                    }
                }
                .launchIn(this)

            channel.subscribe()

        } catch (_: Exception) {
            // يبقى الفحص الدوري الموجود في AdminScreen
            // كخطة احتياطية إذا تعذر اتصال Realtime.
        }
    }
}

fun stopRoyalOrderRealtime() {
    royalRealtimeJob?.cancel()
    royalRealtimeJob = null

    royalRealtimeScope?.cancel()
    royalRealtimeScope = null
}
