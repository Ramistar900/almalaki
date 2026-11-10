package com.almalaki.royaltv

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Centralized asset names for the original ROYAL DESIGN V16/V17 SVG artwork.
 * All SVG files are kept in src/main/assets and rendered as static vectors;
 * Compose supplies lightweight state-driven motion on Android 6+.
 */
object RoyalTVIconAssets {
    const val TV_SOURCE = "royal_icons/v16/tv/source.svg"
    const val TV_CHANNELS = "royal_icons/v16/tv/channels.svg"
    const val TV_LIVE = "royal_icons/v16/tv/live.svg"
    const val TV_CAST = "royal_icons/v16/tv/cast.svg"
    const val TV_REMOTE = "royal_icons/v16/tv/remote.svg"
    const val TV_SETTINGS = "royal_icons/v16/tv/settings.svg"
    const val TV_BACK = "royal_icons/v16/tv/back.svg"
    const val TV_FULLSCREEN = "royal_icons/v16/tv/fullscreen.svg"
    const val TV_REFRESH = "royal_icons/v16/tv/refresh.svg"
    const val TV_MUTE = "royal_icons/v16/tv/mute.svg"
    const val TV_VOLUME = "royal_icons/v16/tv/volume_up.svg"
    const val TV_FAVORITE = "royal_icons/v16/tv/heart.svg"
    const val TV_POWER = "royal_icons/v16/tv/power.svg"
    const val TV_WIFI = "royal_icons/v16/tv/wifi.svg"
    const val TV_LOCK = "royal_icons/v16/tv/lock.svg"
    const val TV_SHARE = "royal_icons/v16/tv/share.svg"
    const val TV_CLOSE = "royal_icons/v16/tv/close.svg"
    const val TV_GRID = "royal_icons/v16/tv/grid.svg"

    const val LAUNCHER_APPS = "royal_icons/v17/launcher/apps.svg"
    const val LAUNCHER_MUSIC = "royal_icons/v17/launcher/music.svg"
    const val LAUNCHER_MOVIES = "royal_icons/v17/launcher/movies.svg"
    const val LAUNCHER_FAVORITES = "royal_icons/v17/launcher/favorites.svg"
    const val LAUNCHER_GUIDE = "royal_icons/v17/launcher/guide.svg"
    const val LAUNCHER_LIVE = "royal_icons/v17/launcher/live_tv.svg"

    const val MUSIC_BACK = "royal_icons/v17/music/back.svg"
    const val MUSIC_DOWNLOAD = "royal_icons/v17/music/download.svg"
    const val MUSIC_EQUALIZER = "royal_icons/v17/music/equalizer.svg"
    const val MUSIC_FAVORITE = "royal_icons/v17/music/heart.svg"
    const val MUSIC_LYRICS = "royal_icons/v17/music/lyrics.svg"
    const val MUSIC_NOTE = "royal_icons/v17/music/music.svg"
    const val MUSIC_NEXT = "royal_icons/v17/music/next.svg"
    const val MUSIC_PAUSE = "royal_icons/v17/music/pause.svg"
    const val MUSIC_PLAY = "royal_icons/v17/music/play.svg"
    const val MUSIC_PLAYLIST = "royal_icons/v17/music/playlist_add.svg"
    const val MUSIC_PREVIOUS = "royal_icons/v17/music/prev.svg"
    const val MUSIC_QUEUE = "royal_icons/v17/music/queue.svg"
    const val MUSIC_REPEAT = "royal_icons/v17/music/repeat.svg"
    const val MUSIC_SEARCH = "royal_icons/v17/music/search.svg"
    const val MUSIC_SETTINGS = "royal_icons/v17/music/settings.svg"
    const val MUSIC_SHUFFLE = "royal_icons/v17/music/shuffle.svg"
    const val MUSIC_VOLUME = "royal_icons/v17/music/volume_up.svg"

    const val VIDEO_BACK = "royal_icons/v17/video/back.svg"
    const val VIDEO_BOOKMARK = "royal_icons/v17/video/bookmark.svg"
    const val VIDEO_FULLSCREEN = "royal_icons/v17/video/fullscreen.svg"
    const val VIDEO_MUTE = "royal_icons/v17/video/mute.svg"
    const val VIDEO_PAUSE = "royal_icons/v17/video/pause.svg"
    const val VIDEO_PLAY = "royal_icons/v17/video/play.svg"
    const val VIDEO_PREVIOUS = "royal_icons/v17/video/prev.svg"
    const val VIDEO_NEXT = "royal_icons/v17/video/next.svg"
    const val VIDEO_SEEK_BACK = "royal_icons/v17/video/seek_back.svg"
    const val VIDEO_SEEK_FORWARD = "royal_icons/v17/video/seek_forward.svg"
    const val VIDEO_SETTINGS = "royal_icons/v17/video/settings.svg"
    const val VIDEO_SUBTITLES = "royal_icons/v17/video/subtitles.svg"
    const val VIDEO_LIBRARY = "royal_icons/v17/video/video.svg"
    const val VIDEO_VOLUME = "royal_icons/v17/video/volume_up.svg"
}

private object RoyalTVSvgBitmapCache {
    private val cache = object : LruCache<String, Bitmap>(8 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.byteCount / 1024).coerceAtLeast(1)
    }

    fun get(path: String): Bitmap? = cache.get(path)

    fun put(path: String, bitmap: Bitmap) {
        cache.put(path, bitmap)
    }
}

private fun renderRoyalSvg(context: android.content.Context, path: String): Bitmap? {
    RoyalTVSvgBitmapCache.get(path)?.let { return it }

    return try {
        val svg = SVG.getFromAsset(context.assets, path)
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val picture = svg.renderToPicture(256, 256)
        Canvas(bitmap).drawPicture(picture)
        RoyalTVSvgBitmapCache.put(path, bitmap)
        bitmap
    } catch (_: Exception) {
        null
    }
}

/**
 * SVG artwork is rasterized once and cached. The scale animation is kept in
 * Compose so focus/playing state remains responsive on Android 6+ devices.
 */
@Composable
fun RoyalTVIcon(
    assetPath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val iconBitmap = produceState<Bitmap?>(
        initialValue = RoyalTVSvgBitmapCache.get(assetPath),
        key1 = assetPath
    ) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                renderRoyalSvg(context.applicationContext, assetPath)
            }
        }
    }

    val scale = animateFloatAsState(
        targetValue = if (active) 1.07f else 1f,
        animationSpec = spring(stiffness = 420f),
        label = "royal_svg_icon_scale"
    )

    val bitmap = iconBitmap.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier.scale(scale.value),
            contentScale = contentScale
        )
    } else {
        Box(modifier = modifier)
    }
}
