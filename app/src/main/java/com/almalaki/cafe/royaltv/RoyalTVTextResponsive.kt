package com.almalaki.cafe.royaltv

import kotlin.math.max
import kotlin.math.min

/**
 * القيم الفعلية التي تحتاجها واجهة ROYAL TV
 * بعد تحويل القيم النسبية المحفوظة في RoyalTVText.
 */
data class RoyalTVTextLayout(
    val x: Float,
    val y: Float,
    val fontSizePx: Float,
    val alpha: Float
)

/**
 * محرك Responsive لنصوص ROYAL TV.
 *
 * لا يعتمد على مقاس تلفزيون محدد مثل 43 أو 55 أو 65 بوصة.
 *
 * يعتمد على:
 *
 * - العرض الفعلي المتاح.
 * - الارتفاع الفعلي المتاح.
 * - دقة المساحة التي ترسم عليها ROYAL TV.
 *
 * لذلك يمكن استخدامه مع:
 *
 * 720p
 * 1080p
 * 4K
 * Portrait
 * Landscape
 */
object RoyalTVTextResponsive {

    /*
     * الحد الأدنى للبعد المستخدم في حساب مقياس
     * حجم النص.
     *
     * 720 يمثل مرجعًا هندسيًا فقط وليس مقاس شاشة
     * أو حجم تلفزيون.
     */
    private const val BASE_SHORT_DIMENSION =
        720f

    /*
     * أصغر وأكبر معامل مسموح به حتى لا يصبح
     * النص صغيرًا جدًا أو ضخمًا جدًا عند وجود
     * قيم شاشة غير معتادة.
     */
    private const val MIN_SCALE =
        0.75f

    private const val MAX_SCALE =
        3.0f

    /**
     * يحسب Layout النهائي للنص.
     *
     * positionX / positionY:
     *
     * 0.0 = البداية
     * 0.5 = المنتصف
     * 1.0 = النهاية
     *
     * fontSize:
     * القيمة الأساسية المحفوظة في RoyalTVText.
     */
    fun calculate(
        text: RoyalTVText,
        availableWidthPx: Float,
        availableHeightPx: Float
    ): RoyalTVTextLayout {

        val width =
            max(
                1f,
                availableWidthPx
            )

        val height =
            max(
                1f,
                availableHeightPx
            )

        /*
         * نستخدم أصغر بعد فعلي للشاشة.
         *
         * هذا يجعل المقياس متوازنًا في:
         *
         * 16:9
         * 16:10
         * 4:3
         * Portrait
         */
        val shortDimension =
            min(
                width,
                height
            )

        val rawScale =
            shortDimension /
                BASE_SHORT_DIMENSION

        val scale =
            rawScale.coerceIn(
                MIN_SCALE,
                MAX_SCALE
            )

        /*
         * الموضع محفوظ كنسبة من المساحة.
         *
         * بهذه الطريقة لا نربط النص
         * ببكسلات شاشة محددة.
         */
        val x =
            text.positionX
                .coerceIn(
                    0f,
                    1f
                ) * width

        val y =
            text.positionY
                .coerceIn(
                    0f,
                    1f
                ) * height

        /*
         * حجم الخط النهائي بالبكسل.
         */
        val fontSizePx =
            text.fontSize
                .coerceAtLeast(8f) *
                scale

        return RoyalTVTextLayout(
            x = x,
            y = y,
            fontSizePx = fontSizePx,
            alpha =
                text.alpha.coerceIn(
                    0f,
                    1f
                )
        )
    }

    /**
     * تحويل موضع X النسبي إلى Pixel.
     */
    fun positionXPx(
        positionX: Float,
        availableWidthPx: Float
    ): Float {

        return positionX
            .coerceIn(
                0f,
                1f
            ) *
            max(
                1f,
                availableWidthPx
            )
    }

    /**
     * تحويل موضع Y النسبي إلى Pixel.
     */
    fun positionYPx(
        positionY: Float,
        availableHeightPx: Float
    ): Float {

        return positionY
            .coerceIn(
                0f,
                1f
            ) *
            max(
                1f,
                availableHeightPx
            )
    }

    /**
     * حساب حجم الخط الفعلي.
     */
    fun fontSizePx(
        baseFontSize: Float,
        availableWidthPx: Float,
        availableHeightPx: Float
    ): Float {

        val width =
            max(
                1f,
                availableWidthPx
            )

        val height =
            max(
                1f,
                availableHeightPx
            )

        val shortDimension =
            min(
                width,
                height
            )

        val scale =
            (
                shortDimension /
                    BASE_SHORT_DIMENSION
                ).coerceIn(
                    MIN_SCALE,
                    MAX_SCALE
                )

        return baseFontSize
            .coerceAtLeast(8f) *
            scale
    }

    /**
     * تحويل Pixel إلى موضع X نسبي.
     *
     * مفيد لاحقًا عندما يحرك المالك النص
     * ثم نريد حفظ المكان كنسبة بدل Pixel.
     */
    fun positionXNormalized(
        xPx: Float,
        availableWidthPx: Float
    ): Float {

        val width =
            max(
                1f,
                availableWidthPx
            )

        return (
            xPx / width
            ).coerceIn(
                0f,
                1f
            )
    }

    /**
     * تحويل Pixel إلى موضع Y نسبي.
     */
    fun positionYNormalized(
        yPx: Float,
        availableHeightPx: Float
    ): Float {

        val height =
            max(
                1f,
                availableHeightPx
            )

        return (
            yPx / height
            ).coerceIn(
                0f,
                1f
            )
    }

    /**
     * التأكد من أن أبعاد الشاشة صالحة.
     */
    fun hasValidDisplaySize(
        availableWidthPx: Float,
        availableHeightPx: Float
    ): Boolean {

        return availableWidthPx > 0f &&
            availableHeightPx > 0f
    }
}
