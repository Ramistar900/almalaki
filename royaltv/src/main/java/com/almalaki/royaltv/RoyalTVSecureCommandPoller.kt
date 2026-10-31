package com.almalaki.royaltv

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Polls the server-owned queue. A command is acknowledged only after the
 * application callback confirms that the command was accepted locally.
 *
 * The activity starts this worker only when a device credential has already
 * been configured by successful pairing. Owner-side challenge creation and
 * production deployment remain separate prerequisites.
 */
object RoyalTVSecureCommandPoller {
    private const val TAG = "RoyalTVSecurePoller"
    private const val POLL_INTERVAL_MS = 5000L

    private var scope: CoroutineScope? = null
    private var pollJob: Job? = null

    @Synchronized
    fun start(
        context: Context,
        onCommand: suspend (RoyalTVServerCommand) -> Boolean
    ) {
        stop()
        val client = RoyalTVAuthorizationClient(context.applicationContext)
        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = workerScope
        pollJob = workerScope.launch {
            while (isActive) {
                try {
                    val commands = client.pollCommands()
                    for (command in commands) {
                        if (!isActive) break
                        val accepted = withContext(Dispatchers.Main) {
                            onCommand(command)
                        }
                        if (accepted) {
                            // The server validates device ownership, claim status,
                            // expiry and one-time acknowledgement before accepting this.
                            client.acknowledge(command.id)
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.w(TAG, "Secure command poll failed; retrying", error)
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    @Synchronized
    fun stop() {
        pollJob?.cancel()
        pollJob = null
        scope?.cancel()
        scope = null
    }
}
