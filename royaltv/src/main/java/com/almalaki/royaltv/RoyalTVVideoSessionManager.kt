package com.almalaki.royaltv

import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList

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
        listeners.addIfAbsent(listener)

        val config: ByteArray?
        val connected: Boolean

        synchronized(lock) {
            config = latestConfigPacket?.copyOf()
            connected = senderConnected
        }

        if (connected) {
            safely { listener.onSenderConnected() }
        }

        if (config != null) {
            safely {
                listener.onVideoPacket(
                    config,
                    0L,
                    STREAM_CONFIG_FLAG
                )
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

                    override fun onClientConnected() {
                        senderConnected = true
                        listeners.forEach { listener ->
                            safely { listener.onSenderConnected() }
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
                        senderConnected = false
                        listeners.forEach { listener ->
                            safely { listener.onSenderDisconnected() }
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

    private inline fun safely(action: () -> Unit) {
        try {
            action()
        } catch (error: Exception) {
            Log.e(TAG, "Session listener failed", error)
        }
    }
}
