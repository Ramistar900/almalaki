package com.almalaki.cafe

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/**
 * ============================================================
 * ROYAL TV — RESPONSIVE DISPLAY ENGINE
 * ============================================================
 *
 * المرحلة 5 — الخطوة 1
 * Display Metrics
 *
 * طبقة مستقلة لقراءة وتحليل أبعاد شاشة Royal TV.
 *
 * لا تعتمد على دقة تلفزيون ثابتة.
 * ============================================================
 */

data class RoyalTVDisplayMetrics(
    val widthDp: Dp,
    val heightDp: Dp,
    val widthPx: Int,
    val heightPx: Int,
    val density: Float,
    val aspectRatio: Float,
    val isLandscape: Boolean,
    val isPortrait: Boolean
) {

    val shortestSideDp: Dp
        get() = min(
            widthDp.value,
            heightDp.value
        ).dp

    val longestSideDp: Dp
        get() = max(
            widthDp.value,
            heightDp.value
        ).dp
}

/**
 * ============================================================
 * ROYAL TV RESPONSIVE ENGINE
 * ============================================================
 *
 * 1280 × 720 هو مرجع حسابي فقط.
 * لا يتم فرض هذه الدقة على شاشة التلفزيون.
 * ============================================================
 */
object RoyalTVResponsiveEngine {

    const val REFERENCE_WIDTH_DP = 1280f
    const val REFERENCE_HEIGHT_DP = 720f

    /**
     * مقياس متوازن يعتمد على العرض والارتفاع معًا.
     */
    fun calculateScale(
        metrics: RoyalTVDisplayMetrics
    ): Float {

        val widthScale =
            metrics.widthDp.value /
                REFERENCE_WIDTH_DP

        val heightScale =
            metrics.heightDp.value /
                REFERENCE_HEIGHT_DP

        return min(
            widthScale,
            heightScale
        ).coerceAtLeast(0.1f)
    }

    /**
     * مقياس يعتمد على عرض الشاشة.
     */
    fun calculateWidthScale(
        metrics: RoyalTVDisplayMetrics
    ): Float {

        return (
            metrics.widthDp.value /
                REFERENCE_WIDTH_DP
            ).coerceAtLeast(0.1f)
    }

    /**
     * مقياس يعتمد على ارتفاع الشاشة.
     */
    fun calculateHeightScale(
        metrics: RoyalTVDisplayMetrics
    ): Float {

        return (
            metrics.heightDp.value /
                REFERENCE_HEIGHT_DP
            ).coerceAtLeast(0.1f)
    }
}

/**
 * ============================================================
 * CREATE DISPLAY METRICS
 * ============================================================
 *
 * يحول أبعاد Compose إلى بيانات كاملة
 * يمكن استخدامها في جميع واجهات Royal TV.
 * ============================================================
 */
fun createRoyalTVDisplayMetrics(
    widthDp: Dp,
    heightDp: Dp,
    density: Float
): RoyalTVDisplayMetrics {

    val safeWidthDp =
        widthDp.value.coerceAtLeast(0f)

    val safeHeightDp =
        heightDp.value.coerceAtLeast(0f)

    val widthPx =
        (safeWidthDp * density)
            .toInt()

    val heightPx =
        (safeHeightDp * density)
            .toInt()

    val aspectRatio =
        if (safeHeightDp > 0f) {
            safeWidthDp / safeHeightDp
        } else {
            1f
        }

    return RoyalTVDisplayMetrics(
        widthDp = safeWidthDp.dp,
        heightDp = safeHeightDp.dp,
        widthPx = widthPx,
        heightPx = heightPx,
        density = density,
        aspectRatio = aspectRatio,
        isLandscape = safeWidthDp >= safeHeightDp,
        isPortrait = safeHeightDp > safeWidthDp
    )
}

/**
 * ============================================================
 * COMPOSE DISPLAY METRICS
 * ============================================================
 *
 * نسخة جاهزة للاستخدام داخل شاشات Compose.
 * ============================================================
 */
@Composable
fun rememberRoyalTVDisplayMetrics(
    widthDp: Dp,
    heightDp: Dp
): RoyalTVDisplayMetrics {

    val density =
        LocalDensity.current.density

    return createRoyalTVDisplayMetrics(
        widthDp = widthDp,
        heightDp = heightDp,
        density = density
    )
}
