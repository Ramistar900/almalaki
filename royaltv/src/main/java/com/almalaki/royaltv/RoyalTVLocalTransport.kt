package com.almalaki.royaltv

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ROYAL TV — Local TCP Transport
 *
 * النقل المحلي بين أجهزة ROYAL TV.
 * يميّز بين جاهزية الخادم واتصال العميل،
 * ويتيح للخادم استقبال عميل جديد بعد انقطاع السابق.
 */
class RoyalTVLocalTransport(
    private val listener: Listener
) {

    companion object {
        const val DEFAULT_PORT = 39871
        const val MAX_MESSAGE_BYTES = 1024 * 1024
        const val CONNECT_TIMEOUT_MS = 8000
    }

    interface Listener {
        fun onConnected()
        fun onDisconnected(reason: String)
        fun onMessageReceived(message: String)
        fun onError(operation: String, reason: String)

        /** يُستدعى بعد نجاح فتح منفذ الخادم فعلياً. */
        fun onServerStarted(port: Int) {}

        /** يُستدعى عند توقف الخادم الذي كان يعمل. */
        fun onServerStopped(reason: String) {}
    }

    private val executor = Executors.newCachedThreadPool()
    private val active = AtomicBoolean(false)
    private val released = AtomicBoolean(false)
    private val outputLock = Any()

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var socket: Socket? = null

    @Volatile
    private var output: DataOutputStream? = null

    @Volatile
    private var serverListening = false

    /**
     * هل منفذ TCP مفتوح فعلياً ويستقبل الاتصالات؟
     */
    fun isServerListening(): Boolean {
        val server = serverSocket
        return serverListening &&
            server != null &&
            server.isBound &&
            !server.isClosed
    }

    fun isConnected(): Boolean {
        val current = socket
        return active.get() &&
            current != null &&
            current.isConnected &&
            !current.isClosed &&
            current.isBound
    }

    /**
     * يبدأ خادم TCP بصورة غير متزامنة.
     * true تعني قبول بدء المهمة فقط؛
     * تأكيد الجاهزية يصل عبر onServerStarted.
     */
    @Synchronized
    fun startServer(port: Int = DEFAULT_PORT): Boolean {
        if (released.get()) return false
        if (port !in 1..65535) return false
        if (isServerListening()) return true
        if (!active.compareAndSet(false, true)) return false

        try {
            executor.execute {
                runServer(port)
            }
            return true
        } catch (exception: Exception) {
            active.set(false)
            listener.onError(
                "startServer",
                exception.message ?: "تعذر تشغيل مهمة خادم TCP"
            )
            return false
        }
    }

    private fun runServer(port: Int) {
        var localServer: ServerSocket? = null
        var stopReason = "توقف خادم TCP"

        try {
            // إنشاء ServerSocket على هذا المنفذ يفتح المنفذ فعلياً.
            localServer = ServerSocket(port)

            if (!active.get() || released.get()) {
                localServer.close()
                return
            }

            serverSocket = localServer
            serverListening = true

            listener.onServerStarted(port)

            while (active.get() && !released.get()) {
                val accepted = try {
                    localServer.accept()
                } catch (exception: SocketException) {
                    if (active.get() && !released.get()) {
                        stopReason = exception.message
                            ?: "توقف قبول اتصالات TCP"
                        listener.onError("accept", stopReason)
                    }
                    break
                }

                if (!active.get() || released.get()) {
                    closeQuietly(accepted)
                    break
                }

                configureSocket(accepted)

                socket = accepted
                output = DataOutputStream(
                    BufferedOutputStream(accepted.getOutputStream())
                )

                listener.onConnected()

                // عند انقطاع العميل تعود الدالة ويستمر الخادم
                // في حلقة accept لاستقبال اتصال جديد.
                readMessages(accepted)
            }
        } catch (exception: Exception) {
            stopReason = exception.message ?: "تعذر تشغيل خادم TCP"
            if (active.get() && !released.get()) {
                listener.onError("startServer", stopReason)
            }
        } finally {
            serverListening = false

            if (serverSocket === localServer) {
                serverSocket = null
            }

            closeQuietly(localServer)

            // لا نغلق اتصالاً أحدث أنشأته مهمة أخرى.
            if (socket == null || !active.get() || released.get()) {
                active.set(false)
            }

            if (localServer != null) {
                listener.onServerStopped(stopReason)
            }
        }
    }

    /**
     * يتصل هذا الجهاز بخادم TCP بعيد.
     */
    @Synchronized
    fun connect(host: String, port: Int = DEFAULT_PORT): Boolean {
        if (released.get()) return false
        if (host.isBlank() || port !in 1..65535) return false
        if (!active.compareAndSet(false, true)) return false

        try {
            executor.execute {
                var client: Socket? = null

                try {
                    client = Socket()
                    client.connect(
                        java.net.InetSocketAddress(host, port),
                        CONNECT_TIMEOUT_MS
                    )

                    if (!active.get() || released.get()) {
                        closeQuietly(client)
                        return@execute
                    }

                    configureSocket(client)
                    socket = client
                    output = DataOutputStream(
                        BufferedOutputStream(client.getOutputStream())
                    )

                    listener.onConnected()
                    readMessages(client)
                } catch (exception: Exception) {
                    if (active.get() && !released.get()) {
                        listener.onError(
                            "connect",
                            exception.message ?: "تعذر الاتصال بخادم TCP"
                        )
                    }
                } finally {
                    active.set(false)
                    closeConnection("انتهى اتصال TCP")
                }
            }
            return true
        } catch (exception: Exception) {
            active.set(false)
            listener.onError(
                "connect",
                exception.message ?: "تعذر بدء مهمة الاتصال"
            )
            return false
        }
    }

    private fun configureSocket(target: Socket) {
        target.tcpNoDelay = true
        target.keepAlive = true
    }

    /**
     * صيغة الرسائل:
     * طول الرسالة بالبايت كعدد صحيح، ثم محتوى UTF-8.
     */
    fun sendMessage(message: String): Boolean {
        if (released.get() || !isConnected()) return false

        val bytes = message.toByteArray(Charsets.UTF_8)

        if (bytes.isEmpty() || bytes.size > MAX_MESSAGE_BYTES) {
            listener.onError(
                "sendMessage",
                "حجم الرسالة غير صالح"
            )
            return false
        }

        return try {
            synchronized(outputLock) {
                val currentOutput = output ?: return false

                if (!isConnected()) return false

                currentOutput.writeInt(bytes.size)
                currentOutput.write(bytes)
                currentOutput.flush()
            }
            true
        } catch (exception: IOException) {
            listener.onError(
                "sendMessage",
                exception.message ?: "تعذر إرسال الرسالة"
            )
            closeConnection("فشل إرسال الرسالة")
            false
        }
    }

    private fun readMessages(source: Socket) {
        try {
            val input = DataInputStream(
                BufferedInputStream(source.getInputStream())
            )

            while (active.get() && !released.get() && !source.isClosed) {
                val length = try {
                    input.readInt()
                } catch (exception: EOFException) {
                    break
                }

                if (length !in 1..MAX_MESSAGE_BYTES) {
                    listener.onError(
                        "readMessages",
                        "حجم الرسالة الواردة غير صالح"
                    )
                    break
                }

                val bytes = ByteArray(length)
                input.readFully(bytes)

                listener.onMessageReceived(
                    String(bytes, Charsets.UTF_8)
                )
            }
        } catch (exception: SocketException) {
            if (active.get() && !released.get()) {
                listener.onError(
                    "readMessages",
                    exception.message ?: "انقطع اتصال TCP"
                )
            }
        } catch (exception: IOException) {
            if (active.get() && !released.get()) {
                listener.onError(
                    "readMessages",
                    exception.message ?: "تعذرت قراءة الرسالة"
                )
            }
        } finally {
            // يغلق اتصال العميل الحالي فقط.
            // لا يوقف حلقة accept الخاصة بالخادم.
            closeConnection("انتهى اتصال الطرف الآخر", source)
        }
    }

    private fun closeConnection(
        reason: String,
        expectedSocket: Socket? = null
    ) {
        val oldSocket: Socket?

        synchronized(outputLock) {
            val current = socket

            // يمنع اتصال قديم من إغلاق اتصال أحدث.
            if (expectedSocket != null && current !== expectedSocket) {
                closeQuietly(expectedSocket)
                return
            }

            oldSocket = current
            socket = null
            output = null
        }

        closeQuietly(oldSocket)

        if (oldSocket != null) {
            listener.onDisconnected(reason)
        }
    }

    /**
     * يوقف الخادم والاتصال الحالي.
     */
    @Synchronized
    fun disconnect() {
        active.set(false)

        val oldServer = serverSocket
        serverListening = false

        serverSocket = null
        closeQuietly(oldServer)

        closeConnection("تم قطع اتصال TCP")
    }

    /**
     * تحرير نهائي لموارد النقل.
     */
    @Synchronized
    fun release() {
        if (!released.compareAndSet(false, true)) return

        disconnect()
        executor.shutdownNow()
    }

    private fun closeQuietly(closeable: AutoCloseable?) {
        try {
            closeable?.close()
        } catch (_: Exception) {
            // لا نوقف التنظيف بسبب فشل الإغلاق.
        }
    }
}
