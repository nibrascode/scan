package com.nibras.scan.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

object PerspectiveUtils {

    private const val MAX_OUTPUT_SIDE = 3000

    /**
     * Straightens [corners] (TL, TR, BR, BL) with Skia's native 4-point transform.
     * This replaces the old per-pixel Kotlin warp, which was the slowest step in a scan.
     */
    fun warpToRectangle(src: Bitmap, corners: FloatArray): Bitmap {
        require(corners.size == 8) { "corners must contain 4 points (8 floats)" }

        var outW = max(dist(corners, 0, 1), dist(corners, 3, 2))
        var outH = max(dist(corners, 0, 3), dist(corners, 1, 2))
        val longest = max(outW, outH)
        if (longest > MAX_OUTPUT_SIDE) {
            val k = MAX_OUTPUT_SIDE / longest
            outW *= k
            outH *= k
        }
        val w = outW.roundToInt().coerceIn(1, MAX_OUTPUT_SIDE)
        val h = outH.roundToInt().coerceIn(1, MAX_OUTPUT_SIDE)

        val matrix = Matrix()
        val mapped = matrix.setPolyToPoly(
            floatArrayOf(
                corners[0], corners[1], corners[2], corners[3],
                corners[4], corners[5], corners[6], corners[7]
            ),
            0,
            floatArrayOf(0f, 0f, w.toFloat(), 0f, w.toFloat(), h.toFloat(), 0f, h.toFloat()),
            0,
            4
        )

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.WHITE)
        if (mapped) {
            canvas.drawBitmap(src, matrix, paint)
        } else {
            canvas.drawBitmap(src, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), paint)
        }
        return out
    }

    private fun dist(c: FloatArray, a: Int, b: Int): Double =
        hypot((c[a * 2] - c[b * 2]).toDouble(), (c[a * 2 + 1] - c[b * 2 + 1]).toDouble())
}
