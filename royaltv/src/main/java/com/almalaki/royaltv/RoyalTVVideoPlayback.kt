package com.almalaki.royaltv

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun RoyalTVVideoPlaybackLayer(
    modifier: Modifier = Modifier,
    onStreamingChanged: (Boolean) -> Unit
) {
    val callback = remember(onStreamingChanged) {
        onStreamingChanged
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextureView(context).apply {
                isOpaque = false

                surfaceTextureListener =
                    object : TextureView.SurfaceTextureListener {

                        private var playback:
                            RoyalTVVideoPlaybackController? = null

                        override fun onSurfaceTextureAvailable(
                            surfaceTexture: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {
                            val surface = Surface(surfaceTexture)

                            playback =
                                RoyalTVVideoPlaybackController(
                                    surface = surface,
                                    onStreamingChanged = callback
                                ).also {
                                    it.start()
                                }
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surfaceTexture: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) = Unit

                        override fun onSurfaceTextureDestroyed(
                            surfaceTexture: SurfaceTexture
                        ): Boolean {
                            playback?.close()
                            playback = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(
                            surfaceTexture: SurfaceTexture
                        ) = Unit
                    }
            }
        },
        update = {}
    )
}

private class RoyalTVVideoPlaybackController(
    private val surface: Surface,
    private val onStreamingChanged: (Boolean) -> Unit
) : AutoCloseable, RoyalTVVideoSessionManager.Listener {

    companion object {
        private const val TAG = "RoyalTVVideoPlayback"
        private const val STREAM_CONFIG_FLAG = Int.MIN_VALUE
        private const val CONFIG_MAGIC = 0x52545643
        private const val MIME_TYPE =
            MediaFormat.MIMETYPE_VIDEO_AVC
    }

    private val closed = AtomicBoolean(false)

    @Volatile
    private var decoder: MediaCodec? = null

    @Volatile
    private var streaming = false

    fun start() {
        if (!closed.get()) {
            // يسجّل نافذة العرض فقط؛ لا يملك مستقبل TCP ولا يغلقه.
            RoyalTVVideoSessionManager.attach(this)
        }
    }

    @Synchronized
    override fun onVideoPacket(
        data: ByteArray,
        presentationTimeUs: Long,
        flags: Int
    ) {
        if (closed.get()) return

        try {
            if (flags == STREAM_CONFIG_FLAG) {
                configureDecoder(data)
                return
            }

            val activeDecoder = decoder ?: return

            if (
                (flags and
                    MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
            ) {
                return
            }

            val inputIndex =
                activeDecoder.dequeueInputBuffer(10_000)

            if (inputIndex < 0) return

            val inputBuffer =
                activeDecoder.getInputBuffer(inputIndex)

            if (inputBuffer == null) {
                // أعد خانة الإدخال إلى MediaCodec بدل تركها محجوزة.
                activeDecoder.queueInputBuffer(
                    inputIndex,
                    0,
                    0,
                    presentationTimeUs,
                    0
                )
                Log.w(TAG, "Decoder input buffer unavailable")
                return
            }

            inputBuffer.clear()

            if (data.size > inputBuffer.remaining()) {
                // إسقاط هذه الحزمة فقط؛ لا ننهي جلسة فك الترميز كلها.
                activeDecoder.queueInputBuffer(
                    inputIndex,
                    0,
                    0,
                    presentationTimeUs,
                    0
                )

                Log.e(
                    TAG,
                    "Video packet exceeds decoder input capacity"
                )
                return
            }

            inputBuffer.put(data)

            activeDecoder.queueInputBuffer(
                inputIndex,
                0,
                data.size,
                presentationTimeUs,
                flags and MediaCodec.BUFFER_FLAG_KEY_FRAME
            )

            drainDecoder(activeDecoder)

        } catch (error: Exception) {
            Log.e(TAG, "Failed to decode video packet", error)
        }
    }

    private fun configureDecoder(packet: ByteArray) {
        val input = DataInputStream(
            ByteArrayInputStream(packet)
        )

        require(input.readInt() == CONFIG_MAGIC) {
            "Invalid video configuration packet"
        }

        val width = input.readInt()
        val height = input.readInt()

        require(width >= 2 && height >= 2) {
            "Invalid video dimensions"
        }

        val csd0 = readConfigBuffer(input)
        val csd1 = readConfigBuffer(input)

        releaseDecoder()

        val format = MediaFormat.createVideoFormat(
            MIME_TYPE,
            width,
            height
        ).apply {
            setByteBuffer("csd-0", ByteBuffer.wrap(csd0))
            setByteBuffer("csd-1", ByteBuffer.wrap(csd1))
            setInteger(
                MediaFormat.KEY_MAX_INPUT_SIZE,
                4 * 1024 * 1024
            )
        }

        val created =
            MediaCodec.createDecoderByType(MIME_TYPE)

        try {
            created.configure(format, surface, null, 0)
            created.start()

            decoder = created
            streaming = true
            onStreamingChanged(true)

            Log.i(
                TAG,
                "H.264 decoder started at ${width}x${height}"
            )

        } catch (error: Exception) {
            try {
                created.release()
            } catch (_: Exception) {
            }

            throw error
        }
    }

    private fun readConfigBuffer(
        input: DataInputStream
    ): ByteArray {
        val length = input.readInt()

        require(length in 1..1_048_576) {
            "Invalid codec configuration size"
        }

        return ByteArray(length).also {
            input.readFully(it)
        }
    }

    private fun drainDecoder(activeDecoder: MediaCodec) {
        val info = MediaCodec.BufferInfo()

        repeat(8) {
            val index =
                activeDecoder.dequeueOutputBuffer(info, 0)

            if (index >= 0) {
                activeDecoder.releaseOutputBuffer(
                    index,
                    true
                )
            } else {
                return
            }
        }
    }

    override fun onSenderConnected() {
        Log.i(TAG, "Sender connected")
    }

    @Synchronized
    override fun onSenderDisconnected() {
        releaseDecoder()

        if (streaming) {
            streaming = false
            onStreamingChanged(false)
        }

        Log.i(TAG, "Sender disconnected")
    }

    override fun onReceiverError(message: String) {
        Log.e(TAG, message)
    }

    private fun releaseDecoder() {
        val old = decoder
        decoder = null

        if (old != null) {
            try {
                old.stop()
            } catch (_: Exception) {
            }

            try {
                old.release()
            } catch (_: Exception) {
            }
        }
    }

    @Synchronized
    override fun close() {
        if (!closed.compareAndSet(false, true)) return

        // فصل العرض فقط؛ تظل جلسة TCP جاهزة للاتصال التالي.
        RoyalTVVideoSessionManager.detach(this)
        releaseDecoder()

        if (streaming) {
            streaming = false
            onStreamingChanged(false)
        }

        try {
            surface.release()
        } catch (_: Exception) {
        }
    }
}
