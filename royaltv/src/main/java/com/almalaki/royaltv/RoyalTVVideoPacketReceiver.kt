package com.almalaki.royaltv

import android.util.Log
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.net.InetSocketAddress
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
        fun onServerStopped()
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
    private val lifecycleLock = Any()

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
                ServerSocket().use { server ->
                    val serverSocketForCleanup = server
                    server.reuseAddress = true
                    server.bind(InetSocketAddress(port))

                    // انشر المقبس فقط إذا لم يسبق إيقاف المستقبل عملية الربط.
                    // تتم عملية الفحص والنشر تحت القفل نفسه الذي يستخدمه close().
                    val serverReady = synchronized(lifecycleLock) {
                        if (!running.get()) {
                            false
                        } else {
                            serverSocket = server
                            true
                        }
                    }
                    if (!serverReady) return@use

                    listener.onServerStarted(port)

                    while (running.get()) {
                        try {
                            server.accept().use { client ->
                                if (!running.get()) {
                                    return@use
                                }

                                client.tcpNoDelay = true
                                client.keepAlive = true

                                // امنع نشر عميل جديد بعد close()، وتأكد أن close()
                                // يستطيع دائمًا رؤية المقبس الذي يحتاج إلى إغلاقه.
                                val clientReady = synchronized(lifecycleLock) {
                                    if (!running.get()) {
                                        false
                                    } else {
                                        clientSocket = client
                                        true
                                    }
                                }
                                if (!clientReady) return@use

                                try {
                                    listener.onClientConnected()
                                    receivePackets(client)
                                } catch (error: Exception) {
                                    if (running.get()) {
                                        reportError(
                                            error.message
                                                ?: "انقطع تدفق حزم الفيديو"
                                        )
                                    }
                                } finally {
                                    synchronized(lifecycleLock) {
                                        if (clientSocket === client) {
                                            clientSocket = null
                                        }
                                    }
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
                synchronized(lifecycleLock) {
                    if (serverSocket === serverSocketForCleanup) {
                        serverSocket = null
                    }
                }
                running.set(false)
                try {
                    listener.onServerStopped()
                } catch (error: Exception) {
                    Log.e(TAG, "Failed to notify server shutdown", error)
                }
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

        // التقط المراجع وامسحها ذريًا بالنسبة إلى نشرها في خيط الخادم.
        // بذلك لا يبقى مقبس يُنشأ بعد close() خارج نطاق عملية الإغلاق.
        val sockets = synchronized(lifecycleLock) {
            val client = clientSocket
            val server = serverSocket
            clientSocket = null
            serverSocket = null
            server to client
        }

        try {
            sockets.second?.close()
        } catch (_: Exception) {
        }

        try {
            sockets.first?.close()
        } catch (_: Exception) {
        }
    }
}
