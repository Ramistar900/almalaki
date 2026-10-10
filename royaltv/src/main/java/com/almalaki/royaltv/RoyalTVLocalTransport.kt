
package com.almalaki.royaltv

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ROYAL TV — Local TCP Transport
 *
 * طبقة نقل الرسائل عبر شبكة محلية.
 * يمكن تشغيلها كمستقبل أو كعميل.
 *
 * بروتوكول الرسائل:
 * 4 bytes: طول الرسالة
 * N bytes: محتوى الرسالة بصيغة UTF-8
 *
 * لا تنفذ هذه الطبقة أوامر الرسائل؛
 * تمررها إلى Listener ليتولى النظام معالجتها.
 */
class RoyalTVLocalTransport(
    private val listener: Listener
) {

    companion object {
        const val DEFAULT_PORT = 39871
        const val MAX_MESSAGE_BYTES = 1024 * 1024
        private const val CONNECT_TIMEOUT_MS = 8000
    }

    interface Listener {
        fun onConnected() {}
        fun onDisconnected(reason: String) {}
        fun onMessageReceived(message: String) {}
        fun onError(operation: String, reason: String) {}
    }

    private val executor: ExecutorService =
        Executors.newCachedThreadPool()

    private val active = AtomicBoolean(false)
    private val released = AtomicBoolean(false)
    private val outputLock = Any()

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var socket: Socket? = null

    @Volatile
    private var output: DataOutputStream? = null

    /**
     * تشغيل المستقبِل على جهاز ROYAL TV.
     * أعطِ الهاتف عنوان IP المحلي لمجموعة Wi-Fi Direct.
     */
    fun startServer(port: Int = DEFAULT_PORT): Boolean {
        if (released.get()) {
            reportError("startServer", "تم تحرير مدير الاتصال")
            return false
        }

        if (port !in 1..65535) {
            reportError("startServer", "رقم المنفذ غير صالح")
            return false
        }

        if (!active.compareAndSet(false, true)) {
            reportError("startServer", "الاتصال يعمل بالفعل")
            return false
        }

        executor.execute {
            var localServer: ServerSocket? = null

            try {
                localServer = ServerSocket()
                localServer.reuseAddress = true
                localServer.bind(InetSocketAddress(port))

                serverSocket = localServer

                while (active.get() && !released.get()) {
                    val accepted = localServer.accept()

                    if (!active.get() || released.get()) {
                        accepted.close()
                        break
                    }

                    if (socket != null) {
                        accepted.close()
                        continue
                    }

                    configureSocket(accepted)
                    socket = accepted
                    output = DataOutputStream(
                        BufferedOutputStream(accepted.getOutputStream())
                    )

                    listener.onConnected()
                    readMessages(accepted)
                }
            } catch (error: IOException) {
                if (active.get() && !released.get()) {
                    reportError(
                        "startServer",
                        error.message ?: "تعذّر تشغيل المستقبِل"
                    )
                }
            } finally {
                try {
                    localServer?.close()
                } catch (_: IOException) {
                    // تجاهل الإغلاق المتكرر.
                }

                if (serverSocket === localServer) {
                    serverSocket = null
                }

                active.set(false)
                closeConnection("توقف المستقبِل")
            }
        }

        return true
    }

    /**
     * اتصال العميل بالمستقبِل باستخدام عنوان IP والمنفذ.
     * مثال عنوان IP: عنوان المجموعة الذي يوفره Wi-Fi Direct.
     */
    fun connect(host: String, port: Int = DEFAULT_PORT): Boolean {
        if (released.get()) {
            reportError("connect", "تم تحرير مدير الاتصال")
            return false
        }

        if (host.isBlank() || port !in 1..65535) {
            reportError("connect", "العنوان أو المنفذ غير صالح")
            return false
        }

        if (!active.compareAndSet(false, true)) {
            reportError("connect", "الاتصال يعمل بالفعل")
            return false
        }

        executor.execute {
            var client: Socket? = null

            try {
                client = Socket()
                client.connect(
                    InetSocketAddress(host.trim(), port),
                    CONNECT_TIMEOUT_MS
                )

                if (!active.get() || released.get()) {
                    client.close()
                    return@execute
                }

                configureSocket(client)
                socket = client
                output = DataOutputStream(
                    BufferedOutputStream(client.getOutputStream())
                )

                listener.onConnected()
                readMessages(client)
            } catch (error: IOException) {
                reportError(
                    "connect",
                    error.message ?: "تعذّر الاتصال بالجهاز"
                )
            } finally {
                active.set(false)
                closeConnection("انتهى اتصال العميل")
            }
        }

        return true
    }

    /**
     * إرسال رسالة UTF-8 إلى الطرف المتصل.
     * يجب على الطرفين استخدام بروتوكول الطول نفسه.
     */
    fun sendMessage(message: String): Boolean {
        if (released.get() || !active.get()) {
            reportError("sendMessage", "لا يوجد اتصال نشط")
            return false
        }

        val bytes = message.toByteArray(StandardCharsets.UTF_8)

        if (bytes.isEmpty() || bytes.size > MAX_MESSAGE_BYTES) {
            reportError(
                "sendMessage",
                "حجم الرسالة غير صالح أو يتجاوز الحد المسموح"
            )
            return false
        }

        return try {
            synchronized(outputLock) {
                val stream = output
                    ?: throw IOException("قناة الإرسال غير جاهزة")

                stream.writeInt(bytes.size)
                stream.write(bytes)
                stream.flush()
            }

            true
        } catch (error: IOException) {
            reportError(
                "sendMessage",
                error.message ?: "فشل إرسال الرسالة"
            )
            closeConnection("تعذّر الإرسال")
            false
        }
    }

    fun isConnected(): Boolean =
        active.get() &&
            socket?.isConnected == true &&
            socket?.isClosed == false

    /**
     * إغلاق الاتصال الحالي وإيقاف المستقبِل إن كان يعمل.
     */
    fun disconnect() {
        active.set(false)

        try {
            serverSocket?.close()
        } catch (_: IOException) {
            // تجاهل أخطاء الإغلاق.
        }

        closeConnection("تم قطع الاتصال")
    }

    /**
     * استدعِ هذه الدالة عند انتهاء عمر مدير الاتصال.
     * بعد release يجب إنشاء كائن جديد لإعادة استخدام النقل.
     */
    fun release() {
        if (!released.compareAndSet(false, true)) return

        disconnect()
        executor.shutdownNow()
    }

    private fun readMessages(connection: Socket) {
        try {
            val input = DataInputStream(
                BufferedInputStream(connection.getInputStream())
            )

            while (
                active.get() &&
                !released.get() &&
                !connection.isClosed
            ) {
                val length = try {
                    input.readInt()
                } catch (_: EOFException) {
                    break
                }

                if (length <= 0 || length > MAX_MESSAGE_BYTES) {
                    throw IOException("حجم الرسالة المستلمة غير صالح")
                }

                val bytes = ByteArray(length)
                input.readFully(bytes)

                val message = String(bytes, StandardCharsets.UTF_8)
                listener.onMessageReceived(message)
            }
        } catch (error: SocketException) {
            if (active.get() && !released.get()) {
                reportError(
                    "receive",
                    error.message ?: "انقطع اتصال الشبكة"
                )
            }
        } catch (error: IOException) {
            if (active.get() && !released.get()) {
                reportError(
                    "receive",
                    error.message ?: "تعذّر استقبال الرسالة"
                )
            }
        } finally {
            closeConnection("انتهى اتصال الطرف الآخر")
        }
    }

    private fun configureSocket(connection: Socket) {
        connection.tcpNoDelay = true
        connection.keepAlive = true
    }

    private fun closeConnection(reason: String) {
        val previous = socket
        socket = null
        output = null

        try {
            previous?.close()
        } catch (_: IOException) {
            // تجاهل أخطاء الإغلاق.
        }

        if (previous != null) {
            listener.onDisconnected(reason)
        }
    }

    private fun reportError(operation: String, reason: String) {
        listener.onError(operation, reason)
    }
}
