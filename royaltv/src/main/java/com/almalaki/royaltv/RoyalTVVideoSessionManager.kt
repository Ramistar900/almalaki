package com.almalaki.royaltv

import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * يدير جلسة استقبال فيديو ROYAL TV بصورة مستقلة عن نافذة العرض.
 * يحتفظ بمستقبل TCP أثناء عمل عملية التطبيق ويحفظ آخر تهيئة H.264.
 */
object RoyalTVVideoSessionManager {

    private const val TAG = "RoyalTVVideoSession"
    private const val STREAM_CONFIG_FLAG = Int.MIN_VALUE

    interface Listener {
        fun onVideoPacket(
            data: ByteArray,
            presentationTimeUs: Long,
            flags: Int
        )

        fun onSenderConnected()
        fun onSenderDisconnected()
        fun onReceiverError(message: String)
    }

    private val listeners = CopyOnWriteArrayList<Listener>()
    private val lock = Any()
    private val restartScheduled = AtomicBoolean(false)

    @Volatile
    private var receiver: RoyalTVVideoPacketReceiver? = null

    @Volatile
    private var started = false

    @Volatile
    private var senderConnected = false

    private var latestConfigPacket: ByteArray? = null

    /**
     * تسجيل مستمع العرض وإعادة إرسال تهيئة الترميز المحفوظة عند توفرها.
     */
    fun attach(listener: Listener) {
        synchronized(lock) {
            listeners.addIfAbsent(listener)

            // سلّم الحالة الحالية ضمن القفل نفسه الذي يحدّث حالة الاتصال،
            // حتى لا يصل إشعار اتصال قديم بعد إشعار الانقطاع.
            if (senderConnected) {
                safely { listener.onSenderConnected() }

                latestConfigPacket?.copyOf()?.let { config ->
                    safely {
                        listener.onVideoPacket(
                            config,
                            0L,
                            STREAM_CONFIG_FLAG
                        )
                    }
                }
            }
        }

        startIfNeeded()
    }

    /**
     * فصل مستمع العرض فقط؛ لا يغلق مستقبل TCP.
     */
    fun detach(listener: Listener) {
        listeners.remove(listener)
    }

    private fun startIfNeeded() {
        synchronized(lock) {
            if (started) return

            started = true

            val created = RoyalTVVideoPacketReceiver(
                object : RoyalTVVideoPacketReceiver.Listener {

                    override fun onServerStarted(port: Int) {
                        Log.i(TAG, "Video session listening on $port")
                    }

                    override fun onServerStopped() {
                        synchronized(lock) {
                            // اسمح بإنشاء مستقبل جديد إذا انتهى هذا الخادم.
                            started = false
                            receiver = null
                        }
                        Log.w(TAG, "Video receiver stopped; scheduling a safe restart")
                        scheduleRestartIfNeeded()
                    }

                    override fun onClientConnected() {
                        synchronized(lock) {
                            senderConnected = true
                            listeners.forEach { listener ->
                                safely { listener.onSenderConnected() }
                            }
                        }
                    }

                    override fun onVideoPacket(
                        data: ByteArray,
                        presentationTimeUs: Long,
                        flags: Int
                    ) {
                        if (flags == STREAM_CONFIG_FLAG) {
                            synchronized(lock) {
                                latestConfigPacket = data.copyOf()
                            }
                        }

                        listeners.forEach { listener ->
                            safely {
                                listener.onVideoPacket(
                                    data,
                                    presentationTimeUs,
                                    flags
                                )
                            }
                        }
                    }

                    override fun onClientDisconnected() {
                        synchronized(lock) {
                            senderConnected = false
                            latestConfigPacket = null
                            listeners.forEach { listener ->
                                safely { listener.onSenderDisconnected() }
                            }
                        }
                    }

                    override fun onReceiverError(message: String) {
                        Log.e(TAG, message)
                        listeners.forEach { listener ->
                            safely { listener.onReceiverError(message) }
                        }
                    }
                }
            )

            receiver = created
            created.start()
        }
    }

    /**
     * يعيد تشغيل مستقبل TCP إذا توقف بينما ما زالت نافذة العرض مسجلة.
     * تأخير قصير يمنح الخادم السابق وقتًا لإغلاق المنفذ، مع منع تكرار مهام الإعادة.
     */
    private fun scheduleRestartIfNeeded() {
        if (listeners.isEmpty() || !restartScheduled.compareAndSet(false, true)) {
            return
        }

        Thread({
            try {
                Thread.sleep(1500)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } finally {
                restartScheduled.set(false)
            }

            if (listeners.isNotEmpty()) {
                startIfNeeded()
            }
        }, "RoyalTV-Video-Restart").apply {
            isDaemon = true
            start()
        }
    }

    private inline fun safely(action: () -> Unit) {
        try {
            action()
        } catch (error: Exception) {
            Log.e(TAG, "Session listener failed", error)
        }
    }
}
