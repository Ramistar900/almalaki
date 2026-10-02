package com.almalaki.cafe

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

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
                install(Realtime) {
                    jwtToken = accessToken
                }
            }

            val channel = supabase.realtime.channel("royal-orders")

            val changeFlow =
                channel.postgresChangeFlow<PostgresAction.Insert>(
                    schema = "public"
                ) {
                    table = "orders"
                }

            changeFlow.collect {
                withContextMain {
                    onNewOrder()
                }
            }

        } catch (_: Exception) {
            // الفحص الدوري الموجود في AdminScreen
            // يبقى كخطة احتياطية.
        }
    }
}

private suspend fun withContextMain(
    block: () -> Unit
) {
    kotlinx.coroutines.withContext(Dispatchers.Main) {
        block()
    }
}

fun stopRoyalOrderRealtime() {
    royalRealtimeJob?.cancel()
    royalRealtimeJob = null

    royalRealtimeScope?.cancel()
    royalRealtimeScope = null
}
