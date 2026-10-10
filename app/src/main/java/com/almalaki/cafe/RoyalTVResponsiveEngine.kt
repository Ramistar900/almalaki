
package com.almalaki.cafe

import android.util.DisplayMetrics
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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
 * المرحلة 5.11 — TV Resolution
 *
 * مدمج مباشرة في محرك العرض الحالي.
 * لا يحتاج إلى ملف دقة منفصل.
 *
 * ملاحظة:
 * أبعاد Compose تمثل مساحة العرض الحالية،
 * وأبعاد Android تمثل أبعاد الشاشة التي يبلّغ عنها النظام.
 * قد تختلف أبعاد النظام عن الدقة الأصلية للوحة التلفزيون.
 * ============================================================
 */

enum class RoyalTVResolutionClass(
    val label: String
) {
    SD("SD"),
    HD("HD"),
    FULL_HD("Full HD"),
    QHD("QHD"),
    UHD_4K("4K"),
    UHD_8K("8K"),
    UNKNOWN("Unknown")
}

data class RoyalTVDisplayMetrics(
    val widthDp: Dp,
    val heightDp: Dp,
    val widthPx: Int,
    val heightPx: Int,
    val density: Float,
    val aspectRatio: Float,
    val isLandscape: Boolean,
    val isPortrait: Boolean,
    val screenResolutionWidthPx: Int = 0,
    val screenResolutionHeightPx: Int = 0
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

    /**
     * تصنيف الدقة المبلّغ عنها من Android.
     * يستخدم أطول ضلع حتى لا يؤثر اتجاه الشاشة.
     */
    val resolutionClass: RoyalTVResolutionClass
        get() {
            val longestSide = max(
                screenResolutionWidthPx,
                screenResolutionHeightPx
            )

            return when {
                longestSide <= 0 ->
                    RoyalTVResolutionClass.UNKNOWN

                longestSide < 1280 ->
                    RoyalTVResolutionClass.SD

                longestSide < 1920 ->
                    RoyalTVResolutionClass.HD

                longestSide < 2560 ->
                    RoyalTVResolutionClass.FULL_HD

                longestSide < 3840 ->
                    RoyalTVResolutionClass.QHD

                longestSide < 7680 ->
                    RoyalTVResolutionClass.UHD_4K

                else ->
                    RoyalTVResolutionClass.UHD_8K
            }
        }

    val reportedScreenResolution: String
        get() =
            if (
                screenResolutionWidthPx > 0 &&
                screenResolutionHeightPx > 0
            ) {
                "${screenResolutionWidthPx} × ${screenResolutionHeightPx}"
            } else {
                "Unknown"
            }

    /**
     * نسبة أبعاد الشاشة المبلّغ عنها من Android.
     */
    val screenAspectRatio: Float
        get() {
            val width = max(
                screenResolutionWidthPx,
                0
            )

            val height = max(
                screenResolutionHeightPx,
                0
            )

            return if (width > 0 && height > 0) {
                width.toFloat() / height.toFloat()
            } else {
                aspectRatio
            }
        }
}

/**
 * ============================================================
 * ROYAL TV RESPONSIVE ENGINE
 * ============================================================
 *
 * 1280 × 720 مرجع حسابي فقط.
 * لا يتم فرض دقة ثابتة على الشاشة.
 * ============================================================
 */
object RoyalTVResponsiveEngine {

    const val REFERENCE_WIDTH_DP = 1280f
    const val REFERENCE_HEIGHT_DP = 720f

    /**
     * مقياس متوازن يعتمد على العرض والارتفاع.
     * نحافظ على السلوك السابق لتجنب تغيير التصميم فجأة.
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
     * مقياس يعتمد على عرض مساحة العرض.
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
     * مقياس يعتمد على ارتفاع مساحة العرض.
     */
    fun calculateHeightScale(
        metrics: RoyalTVDisplayMetrics
    ): Float {

        return (
            metrics.heightDp.value /
                REFERENCE_HEIGHT_DP
        ).coerceAtLeast(0.1f)
    }

    /**
     * الوصول إلى تصنيف الدقة من المحرك الموحد.
     */
    fun resolutionClass(
        metrics: RoyalTVDisplayMetrics
    ): RoyalTVResolutionClass {
        return metrics.resolutionClass
    }
}

/**
 * ============================================================
 * CREATE DISPLAY METRICS
 * ============================================================
 *
 * يحسب أبعاد مساحة العرض داخل Compose،
 * ويستقبل أبعاد الشاشة التي يبلّغ عنها Android.
 *
 * الوسيطان الأخيران اختياريان للمحافظة على
 * توافق الاستدعاءات القديمة.
 * ============================================================
 */
fun createRoyalTVDisplayMetrics(
    widthDp: Dp,
    heightDp: Dp,
    density: Float,
    screenResolutionWidthPx: Int = 0,
    screenResolutionHeightPx: Int = 0
): RoyalTVDisplayMetrics {

    val safeWidthDp =
        widthDp.value.coerceAtLeast(0f)

    val safeHeightDp =
        heightDp.value.coerceAtLeast(0f)

    val safeDensity =
        density.coerceAtLeast(0.1f)

    // أبعاد مساحة العرض الحالية، وليست بالضرورة
    // الدقة الأصلية للوحة التلفزيون.
    val widthPx =
        (safeWidthDp * safeDensity).toInt()

    val heightPx =
        (safeHeightDp * safeDensity).toInt()

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
        density = safeDensity,
        aspectRatio = aspectRatio,
        isLandscape = safeWidthDp >= safeHeightDp,
        isPortrait = safeHeightDp > safeWidthDp,
        screenResolutionWidthPx =
            screenResolutionWidthPx.coerceAtLeast(0),
        screenResolutionHeightPx =
            screenResolutionHeightPx.coerceAtLeast(0)
    )
}

/**
 * ============================================================
 * COMPOSE DISPLAY METRICS
 * ============================================================
 *
 * تُستخدم مباشرة من شاشة Royal TV الحالية.
 * لا حاجة إلى استدعاء جديد داخل RoyalTVScreen.kt.
 * ============================================================
 */
@Composable
fun rememberRoyalTVDisplayMetrics(
    widthDp: Dp,
    heightDp: Dp
): RoyalTVDisplayMetrics {

    val density =
        LocalDensity.current.density

    val context =
        LocalContext.current

    val androidDisplayMetrics: DisplayMetrics =
        context.resources.displayMetrics

    return createRoyalTVDisplayMetrics(
        widthDp = widthDp,
        heightDp = heightDp,
        density = density,
        screenResolutionWidthPx =
            androidDisplayMetrics.widthPixels,
        screenResolutionHeightPx =
            androidDisplayMetrics.heightPixels
    )
}
