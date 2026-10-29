package com.almalaki.cafe.royaltv

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import java.nio.ByteBuffer

/**
 * ROYAL TV — H.264 Video Encoder
 *
 * يجهز سطح إدخال للفيديو ويقرأ البيانات المشفرة.
 *
 * هذه المرحلة لا تنقل الفيديو عبر الشبكة.
 * يجب ربط inputSurface بخدمة MediaProjection
 * في خطوة مستقلة لاحقة.
 */
class RoyalTVVideoEncoder(
    private val width: Int,
    private val height: Int,
    private val bitRate: Int = 2_000_000,
    private val frameRate: Int = 20,
    private val iFrameIntervalSeconds: Int = 2,
    private val listener: Listener
) {

    interface Listener {
        fun onOutputFormatChanged(format: MediaFormat)

        fun onEncodedFrame(
            data: ByteArray,
            presentationTimeUs: Long,
            flags: Int
        )

        fun onEncoderError(error: Exception)
    }

    companion object {
        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val DRAIN_TIMEOUT_US = 10_000L
    }

    private var codec: MediaCodec? = null
    private var surface: Surface? = null
    private var drainThread: Thread? = null

    @Volatile
    private var running = false

    private var prepared = false

    /**
     * جهز المشفر وسطح إدخال الفيديو.
     * لا يبدأ الالتقاط بنفسه.
     */
    @Synchronized
    fun prepare(): Surface {
        check(!prepared) {
            "Video encoder is already prepared"
        }

        require(width >= 2 && width % 2 == 0) {
            "Video width must be a positive even number"
        }

        require(height >= 2 && height % 2 == 0) {
            "Video height must be a positive even number"
        }

        require(bitRate > 0) {
            "Video bitrate must be positive"
        }

        require(frameRate > 0) {
            "Video frame rate must be positive"
        }

        var createdCodec: MediaCodec? = null
        var createdSurface: Surface? = null

        try {
            val format = MediaFormat.createVideoFormat(
                MIME_TYPE,
                width,
                height
            ).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfoColorFormat.SURFACE
                )
                setInteger(
                    MediaFormat.KEY_BIT_RATE,
                    bitRate
                )
                setInteger(
                    MediaFormat.KEY_FRAME_RATE,
                    frameRate
                )
                setInteger(
                    MediaFormat.KEY_I_FRAME_INTERVAL,
                    iFrameIntervalSeconds
                )
            }

            createdCodec = MediaCodec.createEncoderByType(
                MIME_TYPE
            )

            createdCodec.configure(
                format,
                null,
                null,
                MediaCodec.CONFIGURE_FLAG_ENCODE
            )

            createdSurface = createdCodec.createInputSurface()

            codec = createdCodec
            surface = createdSurface
            prepared = true

            return createdSurface
        } catch (error: Exception) {
            try {
                createdSurface?.release()
            } catch (_: Exception) {
            }

            try {
                createdCodec?.release()
            } catch (_: Exception) {
            }

            codec = null
            surface = null
            prepared = false

            throw error
        }
    }

    /**
     * قيمة COLOR_FormatSurface الرسمية من Android.
     */
    private object MediaCodecInfoColorFormat {
        const val SURFACE =
            android.media.MediaCodecInfo.CodecCapabilities
                .COLOR_FormatSurface
    }

    /**
     * ابدأ قراءة مخرجات المشفر.
     * يجب استدعاء prepare() أولاً.
     */
    @Synchronized
    fun start() {
        check(prepared) {
            "Call prepare() before start()"
        }

        check(!running) {
            "Video encoder is already running"
        }

        val activeCodec = checkNotNull(codec) {
            "Video encoder is unavailable"
        }

        activeCodec.start()
        running = true

        drainThread = Thread(
            {
                drainOutput(activeCodec)
            },
            "RoyalTV-H264-Encoder"
        ).apply {
            isDaemon = true
            start()
        }
    }

    /**
     * اقرأ المخرجات المشفرة باستمرار حتى الإيقاف.
     */
    private fun drainOutput(activeCodec: MediaCodec) {
        val bufferInfo = MediaCodec.BufferInfo()

        try {
            while (running) {
                val outputIndex = activeCodec.dequeueOutputBuffer(
                    bufferInfo,
                    DRAIN_TIMEOUT_US
                )

                when {
                    outputIndex ==
                        MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // لا توجد بيانات جاهزة في هذه اللحظة.
                    }

                    outputIndex ==
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        listener.onOutputFormatChanged(
                            activeCodec.outputFormat
                        )
                    }

                    outputIndex >= 0 -> {
                        try {
                            if (bufferInfo.size > 0) {
                                val outputBuffer: ByteBuffer? =
                                    activeCodec.getOutputBuffer(
                                        outputIndex
                                    )

                                if (outputBuffer != null) {
                                    val packet = outputBuffer
                                        .duplicate()
                                        .apply {
                                            position(bufferInfo.offset)
                                            limit(
                                                bufferInfo.offset +
                                                    bufferInfo.size
                                            )
                                        }
                                        .slice()

                                    val bytes = ByteArray(
                                        packet.remaining()
                                    )

                                    packet.get(bytes)

                                    listener.onEncodedFrame(
                                        bytes,
                                        bufferInfo.presentationTimeUs,
                                        bufferInfo.flags
                                    )
                                }
                            }
                        } finally {
                            activeCodec.releaseOutputBuffer(
                                outputIndex,
                                false
                            )
                        }
                    }
                }
            }
        } catch (error: Exception) {
            if (running) {
                running = false
                listener.onEncoderError(error)
            }
        }
    }

    /**
     * اطلب إطار H.264 رئيسيًا جديدًا بعد اتصال المرسل.
     * هذا يقلل انتظار أول صورة قابلة للفك بعد إعادة الاتصال.
     */
    @Synchronized
    fun requestSyncFrame(): Boolean {
        val activeCodec = codec ?: return false
        if (!running) return false

        return try {
            activeCodec.setParameters(
                android.os.Bundle().apply {
                    putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
                }
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * أوقف المشفر وحرر موارده بأمان.
     */
    @Synchronized
    fun stop() {
        running = false

        val worker = drainThread
        drainThread = null

        if (worker != null && worker !== Thread.currentThread()) {
            worker.interrupt()

            try {
                worker.join(500)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        val activeCodec = codec
        codec = null

        try {
            activeCodec?.stop()
        } catch (_: Exception) {
        }

        try {
            activeCodec?.release()
        } catch (_: Exception) {
        }

        try {
            surface?.release()
        } catch (_: Exception) {
        }

        surface = null
        prepared = false
    }
}
