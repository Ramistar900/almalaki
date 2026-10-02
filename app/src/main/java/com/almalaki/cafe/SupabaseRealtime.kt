package com.almalaki.cafe

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    context: android.content.Context,
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

            try {
    channel.subscribe(blockUntilSubscribed = true)

    android.os.Handler(
        android.os.Looper.getMainLooper()
    ).post {
        android.widget.Toast.makeText(
            context,
            "Realtime: تم الاتصال بنجاح",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
} catch (e: Exception) {
    throw e
            }

            changeFlow.collect {
                withContext(Dispatchers.Main) {
                    onNewOrder()
                }
            }

        } catch (e: Exception) {
    android.util.Log.e(
        "RoyalRealtime",
        "Realtime error: ${e.message}",
        e
    )

    android.os.Handler(
        android.os.Looper.getMainLooper()
    ).post {
        android.widget.Toast.makeText(
            context,
            "Realtime: ${e.message ?: "خطأ غير معروف"}",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
        }
    }
}

fun stopRoyalOrderRealtime() {
    royalRealtimeJob?.cancel()
    royalRealtimeJob = null

    royalRealtimeScope?.cancel()
    royalRealtimeScope = null
}
