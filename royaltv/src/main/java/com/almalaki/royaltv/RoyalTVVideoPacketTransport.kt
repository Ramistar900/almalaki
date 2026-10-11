package com.almalaki.royaltv

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ROYAL TV - Video Packet Transport
 *
 * نقل حزم H.264 عبر TCP.
 * منفصل عن قناة الأوامر RoyalTVLocalTransport.
 *
 * جميع callbacks تُستدعى من خيوط العمل، وليس بالضرورة
 * من خيط واجهة المستخدم.
 */
class RoyalTVVideoPacketTransport(
    private val listener: Listener
) : Closeable {

    interface Listener {
        fun onConnected()
        fun onDisconnected(reason: String)
        fun onVideoPacket(
            data: ByteArray,
            presentationTimeUs: Long,
            flags: Int
        )
        fun onError(operation: String, reason: String)

        fun onServerStarted(port: Int) {}
    }

    companion object {
        const val DEFAULT_PORT = 39872
        const val MAX_PACKET_BYTES = 4 * 1024 * 1024
        const val CONNECT_TIMEOUT_MS = 8000
    }

    private val closed = AtomicBoolean(false)
    private val serverRunning = AtomicBoolean(false)
    private val outputLock = Any()

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var activeSocket: Socket? = null

    @Volatile
    private var activeOutput: DataOutputStream? = null

    @Volatile
    private var serverMode = false

    /**
     * تشغيل الخادم على جهاز العرض لاستقبال اتصال المرسل.
     * يبقى الخادم مستمعًا حتى بعد انقطاع العميل،
     * ويمكنه قبول اتصال جديد.
     */
    fun startServer(port: Int = DEFAULT_PORT) {
        if (closed.get()) {
            reportError("startServer", "تم إغلاق ناقل الفيديو")
            return
        }

        if (port !in 1..65535) {
            reportError("startServer", "رقم المنفذ غير صالح")
            return
        }

        if (!serverRunning.compareAndSet(false, true)) {
            reportError("startServer", "الخادم يعمل بالفعل")
            return
        }

        serverMode = true

        Thread({
            var localServer: ServerSocket? = null

            try {
                localServer = ServerSocket()
                localServer.reuseAddress = true
                localServer.bind(InetSocketAddress(port))

                synchronized(this) {
                    if (closed.get()) {
                        localServer.close()
                        return@Thread
                    }
                    serverSocket = localServer
                }

                listener.onServerStarted(port)

                while (
                    serverRunning.get() &&
                    !closed.get()
                ) {
                    var client: Socket? = null

                    try {
                        client = localServer.accept()
                        client.tcpNoDelay = true
                        client.keepAlive = true

                        if (
                            closed.get() ||
                            !serverRunning.get()
                        ) {
                            client.close()
                            break
                        }

                        installConnection(client)
                        client = null

                        readPackets(activeSocket)
                    } catch (e: SocketException) {
                        if (
                            serverRunning.get() &&
                            !closed.get()
                        ) {
                            reportError(
                                "accept",
                                e.message ?: "تعذر قبول الاتصال"
                            )
                        }
                    } catch (e: Exception) {
                        if (!closed.get()) {
                            reportError(
                                "server",
                                e.message ?: "حدث خطأ في الخادم"
                            )
                        }
                    } finally {
                        try {
                            client?.close()
                        } catch (_: Exception) {
                        }
                    }
                }
            } catch (e: Exception) {
                if (!closed.get()) {
                    reportError(
                        "startServer",
                        e.message ?: "تعذر تشغيل الخادم"
                    )
                }
            } finally {
                serverRunning.set(false)

                synchronized(this) {
                    if (serverSocket === localServer) {
                        serverSocket = null
                    }
                }

                try {
                    localServer?.close()
                } catch (_: Exception) {
                }
            }
        }, "RoyalTV-Video-Server").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * اتصال العميل بالخادم.
     * يُستخدم على جهاز إرسال الفيديو.
     */
    fun connect(
        host: String,
        port: Int = DEFAULT_PORT
    ) {
        if (closed.get()) {
            reportError("connect", "تم إغلاق ناقل الفيديو")
            return
        }

        if (host.isBlank() || port !in 1..65535) {
            reportError("connect", "عنوان الجهاز أو المنفذ غير صالح")
            return
        }

        Thread({
            var candidate: Socket? = null

            try {
                candidate = Socket()
                candidate.tcpNoDelay = true
                candidate.keepAlive = true
                candidate.connect(
                    InetSocketAddress(host, port),
                    CONNECT_TIMEOUT_MS
                )

                if (closed.get()) {
                    candidate.close()
                    return@Thread
                }

                installConnection(candidate)
                candidate = null

                readPackets(activeSocket)
            } catch (e: Exception) {
                if (!closed.get()) {
                    reportError(
                        "connect",
                        e.message ?: "تعذر الاتصال بجهاز العرض"
                    )
                }
            } finally {
                try {
                    candidate?.close()
                } catch (_: Exception) {
                }
            }
        }, "RoyalTV-Video-Client").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * إرسال حزمة فيديو مشفّرة.
     *
     * تنسيق الحزمة:
     * 1) طول البيانات: Int
     * 2) توقيت العرض بالميكروثانية: Long
     * 3) أعلام MediaCodec: Int
     * 4) بيانات H.264: ByteArray
     */
    fun sendVideoPacket(
        data: ByteArray,
        presentationTimeUs: Long,
        flags: Int
    ): Boolean {
        if (closed.get()) return false

        if (data.isEmpty() || data.size > MAX_PACKET_BYTES) {
            reportError(
                "sendVideoPacket",
                "حجم حزمة الفيديو غير صالح: ${data.size}"
            )
            return false
        }

        return try {
            synchronized(outputLock) {
                val socket = activeSocket
                val output = activeOutput

                if (
                    socket == null ||
                    output == null ||
                    socket.isClosed ||
                    !socket.isConnected
                ) {
                    return@synchronized false
                }

                output.writeInt(data.size)
                output.writeLong(presentationTimeUs)
                output.writeInt(flags)
                output.write(data)
                output.flush()

                true
            }
        } catch (e: Exception) {
            reportError(
                "sendVideoPacket",
                e.message ?: "تعذر إرسال حزمة الفيديو"
            )
            false
        }
    }

    private fun installConnection(socket: Socket) {
        val input = DataInputStream(
            BufferedInputStream(socket.getInputStream())
        )

        val output = DataOutputStream(
            BufferedOutputStream(socket.getOutputStream())
        )

        synchronized(outputLock) {
            if (closed.get()) {
                socket.close()
                return
            }

            val oldSocket = activeSocket

            activeSocket = socket
            activeOutput = output

            if (oldSocket != null && oldSocket !== socket) {
                try {
                    oldSocket.close()
                } catch (_: Exception) {
                }
            }
        }

        listener.onConnected()

        // يبدأ قارئ الحزم بعد تجهيز الاتصال.
        Thread({
            readPacketsFrom(socket, input)
        }, "RoyalTV-Video-Reader").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * يُبقي الخادم مستمعًا بعد انقطاع العميل.
     * في وضع العميل، ينتهي القارئ عند انقطاع الاتصال.
     */
    private fun readPackets(expectedSocket: Socket?) {
        if (expectedSocket == null) return

        try {
            val input = DataInputStream(
                BufferedInputStream(expectedSocket.getInputStream())
            )
            readPacketsFrom(expectedSocket, input)
        } catch (e: Exception) {
            if (!closed.get()) {
                reportError(
                    "readPackets",
                    e.message ?: "تعذرت قراءة الفيديو"
                )
            }
        }
    }

    private fun readPacketsFrom(
        socket: Socket,
        input: DataInputStream
    ) {
        var reason = "انقطع اتصال الفيديو"

        try {
            while (!closed.get() && !socket.isClosed) {
                val length = input.readInt()

                if (length !in 1..MAX_PACKET_BYTES) {
                    throw IOException(
                        "حجم حزمة فيديو غير صالح: $length"
                    )
                }

                val presentationTimeUs = input.readLong()
                val flags = input.readInt()
                val data = ByteArray(length)

                input.readFully(data)

                if (!closed.get()) {
                    listener.onVideoPacket(
                        data,
                        presentationTimeUs,
                        flags
                    )
                }
            }
        } catch (e: EOFException) {
            reason = "أغلق الطرف الآخر اتصال الفيديو"
        } catch (e: SocketException) {
            reason = e.message ?: "انقطع اتصال الشبكة"
        } catch (e: Exception) {
            reason = e.message ?: "تعذرت قراءة حزمة الفيديو"

            if (!closed.get()) {
                reportError("readPackets", reason)
            }
        } finally {
            val wasActive: Boolean

            synchronized(outputLock) {
                wasActive = activeSocket === socket

                if (wasActive) {
                    activeSocket = null
                    activeOutput = null
                }
            }

            try {
                socket.close()
            } catch (_: Exception) {
            }

            if (wasActive && !closed.get()) {
                listener.onDisconnected(reason)
            }
        }
    }

    private fun reportError(
        operation: String,
        reason: String
    ) {
        if (!closed.get()) {
            try {
                listener.onError(operation, reason)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * إيقاف الاتصال والخادم وتحرير الموارد.
     */
    override fun close() {
        if (!closed.compareAndSet(false, true)) return

        serverRunning.set(false)

        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }

        synchronized(outputLock) {
            try {
                activeSocket?.close()
            } catch (_: Exception) {
            }

            activeSocket = null
            activeOutput = null
        }

        serverSocket = null
    }
}
