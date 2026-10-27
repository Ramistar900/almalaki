package com.almalaki.royaltv

import android.util.Log
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ROYAL TV — Video Packet Receiver
 *
 * يستقبل حزم H.264 عبر TCP.
 * تنسيق الحزمة متوافق مع ناقل الفيديو في تطبيق الملكي:
 * Int: حجم البيانات
 * Long: وقت العرض presentationTimeUs
 * Int: أعلام MediaCodec
 * ByteArray: بيانات الفيديو
 *
 * لا يعرض الفيديو بنفسه؛ تسلّم الحزم إلى Listener
 * لربطها لاحقًا بمفكك H.264 ونافذة البث.
 */
class RoyalTVVideoPacketReceiver(
    private val listener: Listener
) : AutoCloseable {

    interface Listener {
        fun onServerStarted(port: Int)
        fun onClientConnected()
        fun onVideoPacket(
            data: ByteArray,
            presentationTimeUs: Long,
            flags: Int
        )
        fun onClientDisconnected()
        fun onReceiverError(message: String)
    }

    companion object {
        const val DEFAULT_PORT = 39872
        const val MAX_PACKET_BYTES = 4 * 1024 * 1024
        private const val TAG = "RoyalTVVideoReceiver"
    }

    private val running = AtomicBoolean(false)

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var clientSocket: Socket? = null

    fun start(port: Int = DEFAULT_PORT) {
        if (port !in 1..65535) {
            listener.onReceiverError("رقم المنفذ غير صالح")
            return
        }

        if (!running.compareAndSet(false, true)) {
            return
        }

        Thread({
            try {
                ServerSocket(port).use { server ->
                    serverSocket = server
                    listener.onServerStarted(port)

                    while (running.get()) {
                        try {
                            server.accept().use { client ->
                                if (!running.get()) {
                                    return@use
                                }

                                clientSocket = client
                                client.tcpNoDelay = true
                                client.keepAlive = true

                                listener.onClientConnected()

                                try {
                                    receivePackets(client)
                                } catch (error: Exception) {
                                    if (running.get()) {
                                        reportError(
                                            error.message
                                                ?: "انقطع تدفق حزم الفيديو"
                                        )
                                    }
                                } finally {
                                    clientSocket = null
                                    listener.onClientDisconnected()
                                }
                            }
                        } catch (error: SocketException) {
                            if (running.get()) {
                                reportError(
                                    error.message
                                        ?: "تعذر قبول اتصال الفيديو"
                                )
                            }
                        }
                    }
                }
            } catch (error: Exception) {
                if (running.get()) {
                    reportError(
                        error.message
                            ?: "تعذر تشغيل مستقبل الفيديو"
                    )
                }
            } finally {
                serverSocket = null
                running.set(false)
            }
        }, "RoyalTV-Video-Receiver").apply {
            isDaemon = true
            start()
        }
    }

    private fun receivePackets(client: Socket) {
        val input = DataInputStream(
            BufferedInputStream(client.getInputStream())
        )

        while (running.get() && !client.isClosed) {
            val packetSize = try {
                input.readInt()
            } catch (_: EOFException) {
                break
            }

            if (packetSize !in 1..MAX_PACKET_BYTES) {
                throw IllegalArgumentException(
                    "حجم حزمة فيديو غير صالح: $packetSize"
                )
            }

            val presentationTimeUs = input.readLong()
            val flags = input.readInt()
            val packet = ByteArray(packetSize)

            input.readFully(packet)

            if (running.get()) {
                listener.onVideoPacket(
                    packet,
                    presentationTimeUs,
                    flags
                )
            }
        }
    }

    private fun reportError(message: String) {
        Log.e(TAG, message)
        try {
            listener.onReceiverError(message)
        } catch (_: Exception) {
            Log.e(TAG, "تعذر إرسال إشعار الخطأ")
        }
    }

    override fun close() {
        running.set(false)

        try {
            clientSocket?.close()
        } catch (_: Exception) {
        }

        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }

        clientSocket = null
        serverSocket = null
    }
}
