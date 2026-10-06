package com.almalaki.cafe.royaltv

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.ArrayDeque

object RoyalTVCommandQueue {

    private const val COMMAND_DELAY_MS = 50L

    private val mutex = Mutex()

    private val queue =
        ArrayDeque<RoyalTVCommand>()

    private var scope:
        CoroutineScope? = null

    private var workerJob:
        Job? = null

    private var processing =
        false

    private var commandHandler:
        ((RoyalTVCommand) -> Unit)? = null

    fun initialize(
        handler: (RoyalTVCommand) -> Unit
    ) {

        commandHandler = handler

        if (scope == null) {
            scope =
                CoroutineScope(
                    SupervisorJob() +
                        Dispatchers.Main.immediate
                )
        }

        startWorker()
    }

    fun enqueue(
        command: RoyalTVCommand
    ) {

        scope
            ?: initialize {
                RoyalTVManager.executeCommand(
                    it
                )
            }

        scope
            ?.launch {
                mutex.withLock {

                    queue.addLast(
                        command
                    )
                }

                startWorker()
            }
    }

    fun enqueueAll(
        commands: List<RoyalTVCommand>
    ) {

        if (commands.isEmpty()) {
            return
        }

        scope
            ?: initialize {
                RoyalTVManager.executeCommand(
                    it
                )
            }

        scope
            ?.launch {

                mutex.withLock {

                    commands.forEach { command ->
                        queue.addLast(
                            command
                        )
                    }
                }

                startWorker()
            }
    }

    fun clear() {

        scope
            ?.launch {

                mutex.withLock {
                    queue.clear()
                }
            }
    }

    fun size(): Int {
        return synchronized(this) {
            queue.size
        }
    }

    fun isEmpty(): Boolean {
        return synchronized(this) {
            queue.isEmpty()
        }
    }

    fun isProcessing(): Boolean {
        return processing
    }

    private fun startWorker() {

        if (workerJob?.isActive == true) {
            return
        }

        val currentScope =
            scope
                ?: return

        workerJob =
            currentScope.launch {

                processing = true

                try {

                    while (true) {

                        val command =
                            mutex.withLock {

                                if (
                                    queue.isEmpty()
                                ) {
                                    null
                                } else {
                                    queue.removeFirst()
                                }
                            }

                        if (command == null) {
                            break
                        }

                        try {

                            commandHandler
                                ?.invoke(
                                    command
                                )

                        } catch (e: Exception) {

                            android.util.Log.e(
                                "RoyalTVCommandQueue",
                                "Command execution error: ${e.message}",
                                e
                            )
                        }

                        delay(
                            COMMAND_DELAY_MS
                        )
                    }

                } finally {

                    processing = false
                    workerJob = null
                }
            }
    }

    fun stop() {

        workerJob?.cancel()
        workerJob = null

        scope?.cancel()
        scope = null

        synchronized(this) {
            queue.clear()
        }

        processing = false
        commandHandler = null
    }
}
