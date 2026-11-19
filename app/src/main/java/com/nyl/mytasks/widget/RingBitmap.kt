package com.nyl.mytasks.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/** Anneau de progression dessiné en bitmap (Glance ne sait pas dessiner d'arcs). */
object RingBitmap {
    fun draw(sizePx: Int, color: Int, ratio: Float, empty: Boolean): Bitmap {
        val size = sizePx.coerceAtLeast(8)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val stroke = size * 0.13f
        val rect = RectF(stroke / 2, stroke / 2, size - stroke / 2, size - stroke / 2)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
        }
        // Piste : la couleur du type, très atténuée.
        paint.color = (color and 0x00FFFFFF) or (0x38 shl 24)
        canvas.drawArc(rect, 0f, 360f, false, paint)
        if (!empty && ratio > 0f) {
            paint.color = color
            canvas.drawArc(rect, -90f, 360f * ratio.coerceIn(0f, 1f), false, paint)
        }
        return bitmap
    }
}
