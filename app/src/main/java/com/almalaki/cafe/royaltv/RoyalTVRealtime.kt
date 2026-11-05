package com.almalaki.cafe.royaltv

import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val ROYAL_TV_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val ROYAL_TV_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

private val ROYAL_TV_PRIVATE_TOPIC = Regex(
    "^royal-tv:[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
    RegexOption.IGNORE_CASE
)

object RoyalTVRealtime {

    /**
     * Legacy API retained only so older call sites still compile.
     * Public display-ID subscriptions are deliberately disabled.
     */
    @Deprecated(
        message = "Use the registered device's private channel topic.",
        level = DeprecationLevel.WARNING
    )
    fun start(
        deviceId: String,
        onCommand: (RoyalTVCommand) -> Unit
    ) {
        Log.w(
            "RoyalTVRealtime",
            "Legacy public-channel listener disabled. Pair the TV and use its private server topic."
        )
    }

    /**
     * Legacy API fails closed. A display ID is not an authorization credential.
     */
    @Deprecated(
        message = "Use sendCommand(ownerAccessToken, channelTopic, command).",
        level = DeprecationLevel.WARNING
    )
    suspend fun sendCommand(
        targetDeviceId: String,
        command: RoyalTVCommand
    ): Boolean {
        Log.w(
            "RoyalTVRealtime",
            "Legacy public-channel send blocked for display ID: $targetDeviceId"
        )
        return false
    }

    /**
     * Sends only through a private topic returned by the server for a paired TV.
     * Database RLS checks both the current restaurant owner and device ownership.
     */
    suspend fun sendCommand(
        ownerAccessToken: String,
        channelTopic: String,
        command: RoyalTVCommand
    ): Boolean {
        if (ownerAccessToken.isBlank() ||
            !channelTopic.matches(ROYAL_TV_PRIVATE_TOPIC)
        ) {
            Log.e("RoyalTVRealtime", "Invalid owner token or private TV topic")
            return false
        }

        val supabase = createSupabaseClient(
            supabaseUrl = ROYAL_TV_URL,
            supabaseKey = ROYAL_TV_KEY
        ) {
            install(Realtime)
        }

        var targetChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null
        return try {
            supabase.realtime.setAuth(ownerAccessToken)
            targetChannel = supabase.channel(channelTopic) {
                isPrivate = true
            }
            targetChannel.subscribe(blockUntilSubscribed = true)
            targetChannel.broadcast(
                event = "royal_tv_command",
                message = buildJsonObject {
                    put("type", command.type.name)
                    put("payload", command.payload)
                    put("createdAt", command.createdAt)
                }
            )
            true
        } catch (error: Exception) {
            Log.e(
                "RoyalTVRealtime",
                "Private ROYAL TV command failed",
                error
            )
            false
        } finally {
            try {
                targetChannel?.unsubscribe()
            } catch (error: Exception) {
                Log.w(
                    "RoyalTVRealtime",
                    "Could not close private TV channel",
                    error
                )
            }
        }
    }

    fun stop() {
        // The owner now uses one private channel per send. No persistent
        // owner-side public subscription is kept.
    }
}
