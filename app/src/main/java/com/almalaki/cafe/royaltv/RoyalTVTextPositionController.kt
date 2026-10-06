package com.almalaki.cafe.royaltv

/**
 * يتحكم بموقع نص ROYAL TV على الشاشة.
 *
 * الموقع مخزن كنسبة من الشاشة:
 * X = 0.0 يسار → 1.0 يمين
 * Y = 0.0 أعلى → 1.0 أسفل
 *
 * لذلك يبقى مكان النص متوافقًا مع:
 * 720p / 1080p / 4K
 * Portrait / Landscape
 */
object RoyalTVTextPositionController {

    private const val MOVE_STEP = 0.02f

    fun moveLeft(id: String): Boolean {
        val text = RoyalTVTextManager.getText(id) ?: return false

        return RoyalTVTextManager.moveText(
            id = id,
            positionX = text.positionX - MOVE_STEP,
            positionY = text.positionY
        )
    }

    fun moveRight(id: String): Boolean {
        val text = RoyalTVTextManager.getText(id) ?: return false

        return RoyalTVTextManager.moveText(
            id = id,
            positionX = text.positionX + MOVE_STEP,
            positionY = text.positionY
        )
    }

    fun moveUp(id: String): Boolean {
        val text = RoyalTVTextManager.getText(id) ?: return false

        return RoyalTVTextManager.moveText(
            id = id,
            positionX = text.positionX,
            positionY = text.positionY - MOVE_STEP
        )
    }

    fun moveDown(id: String): Boolean {
        val text = RoyalTVTextManager.getText(id) ?: return false

        return RoyalTVTextManager.moveText(
            id = id,
            positionX = text.positionX,
            positionY = text.positionY + MOVE_STEP
        )
    }

    fun setPosition(
        id: String,
        positionX: Float,
        positionY: Float
    ): Boolean {
        return RoyalTVTextManager.moveText(
            id = id,
            positionX = positionX.coerceIn(0f, 1f),
            positionY = positionY.coerceIn(0f, 1f)
        )
    }

    fun center(id: String): Boolean {
        return setPosition(
            id = id,
            positionX = 0.5f,
            positionY = 0.5f
        )
    }

    fun resetPosition(id: String): Boolean {
        return center(id)
    }
}
