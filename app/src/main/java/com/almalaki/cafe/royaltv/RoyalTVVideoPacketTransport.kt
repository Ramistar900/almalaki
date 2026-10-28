package com.almalaki.cafe.royaltv

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
 * منفصل عن قناة أوامر RoyalTVLocalTransport.
 *
 * تنسيق كل حزمة:
 * Int    = حجم البيانات
 * Long   = presentationTimeUs
 * Int    = MediaCodec flags
 * Byte[] = بيانات H.264
 *
 * تُستدعى callbacks من خيوط العمل.
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

        fun onError(
            operation: String,
            reason: String
        )

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

    /**
     * تشغيل خادم الفيديو على جهاز العرض.
     * يقبل اتصالًا جديدًا بعد انقطاع الاتصال السابق.
     */
    fun startServer(port: Int = DEFAULT_PORT) {
        if (closed.get()) {
            reportError("startServer", "ناقل الفيديو مغلق")
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

                        val connectedClient = client

                        if (!installConnection(connectedClient)) {
                            connectedClient.close()
                            client = null
                            continue
                        }

                        client = null

                        // قارئ واحد فقط لهذا الاتصال.
                        // لا ننتقل لقبول عميل جديد حتى ينتهي هذا الاتصال.
                        readPackets(connectedClient)

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
     * الاتصال بخادم الفيديو على جهاز العرض.
     * يُستخدم على جهاز إرسال الفيديو.
     */
    fun connect(
        host: String,
        port: Int = DEFAULT_PORT
    ) {
        if (closed.get()) {
            reportError("connect", "ناقل الفيديو مغلق")
            return
        }

        if (host.isBlank() || port !in 1..65535) {
            reportError(
                "connect",
                "عنوان الجهاز أو رقم المنفذ غير صالح"
            )
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

                val connectedSocket = candidate

                if (!installConnection(connectedSocket)) {
                    connectedSocket.close()
                    return@Thread
                }

                candidate = null

                // قارئ واحد فقط للاتصال.
                readPackets(connectedSocket)

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
     * تجهيز الاتصال وتسجيل مخرج الإرسال فقط.
     *
     * لا يبدأ قارئًا هنا؛ القراءة تتم في readPackets
     * مرة واحدة من مسار الخادم أو العميل.
     */
    private fun installConnection(socket: Socket): Boolean {
        val output = try {
            DataOutputStream(
                BufferedOutputStream(socket.getOutputStream())
            )
        } catch (e: Exception) {
            reportError(
                "installConnection",
                e.message ?: "تعذر تجهيز اتصال الفيديو"
            )
            return false
        }

        synchronized(outputLock) {
            if (
                closed.get() ||
                socket.isClosed ||
                !socket.isConnected
            ) {
                return false
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

        try {
            listener.onConnected()
        } catch (e: Exception) {
            reportError(
                "onConnected",
                e.message ?: "حدث خطأ في إشعار الاتصال"
            )
        }

        return true
    }

    /**
     * إرسال حزمة فيديو مشفّرة.
     * يعيد false عند عدم وجود اتصال أو فشل الإرسال.
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

        var failedSocket: Socket? = null

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

                failedSocket = socket

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

            // أغلق الاتصال المعطوب لإيقاظ قارئ الحزم وإطلاق onDisconnected،
            // حتى يتمكن المرسل من جدولة إعادة الاتصال تلقائيًا.
            failedSocket?.let { disconnectSocket(it) }

            false
        }
    }

    /**
     * قارئ الحزم الوحيد لكل اتصال.
     */
    private fun readPackets(socket: Socket) {
        try {
            val input = DataInputStream(
                BufferedInputStream(socket.getInputStream())
            )

            while (!closed.get() && !socket.isClosed) {
                val length = try {
                    input.readInt()
                } catch (e: EOFException) {
                    break
                }

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
        } catch (e: SocketException) {
            if (!closed.get()) {
                reportError(
                    "readPackets",
                    e.message ?: "انقطع اتصال الشبكة"
                )
            }
        } catch (e: Exception) {
            if (!closed.get()) {
                reportError(
                    "readPackets",
                    e.message ?: "تعذرت قراءة حزمة الفيديو"
                )
            }
        } finally {
            disconnectSocket(socket)
        }
    }

    /**
     * تحرير الاتصال الحالي دون إيقاف الخادم.
     */
    private fun disconnectSocket(socket: Socket) {
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
            try {
                listener.onDisconnected("انقطع اتصال الفيديو")
            } catch (_: Exception) {
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
     * إيقاف الخادم والاتصال وتحرير الموارد.
     * هذا الكائن لا يُعاد استخدامه بعد close().
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
