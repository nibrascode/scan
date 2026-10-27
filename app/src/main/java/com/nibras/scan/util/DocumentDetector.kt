package com.nibras.scan.util

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.min

/**
 * Finds a document-like quadrilateral in a photo.
 *
 * Prefers OpenCV (Canny + 4-point contour). If the native library is missing or the
 * page has weak edges, falls back to a pure-Kotlin brightness blob so auto-crop still
 * moves the handles instead of always leaving the default inset.
 *
 * Returns 4 points (TL, TR, BR, BL — 8 floats) in [bitmap] pixel coordinates, or null.
 */
object DocumentDetector {

    @Volatile
    var openCvReady: Boolean = false

    fun detect(bitmap: Bitmap): FloatArray? {
        if (openCvReady) {
            try {
                detectOpenCv(bitmap)?.let { return it }
            } catch (_: Throwable) {
                // Native failure: keep going with the Kotlin detector.
            }
        }
        return try {
            detectByBrightness(bitmap)
        } catch (_: Throwable) {
            null
        }
    }

    private fun detectOpenCv(bitmap: Bitmap): FloatArray? {
        val src = Mat()
        val small = Mat()
        val gray = Mat()
        val edges = Mat()
        try {
            Utils.bitmapToMat(bitmap, src)
            val longSide: Double = max(src.width(), src.height()).toDouble()
            val scale: Double = if (longSide > 640.0) 640.0 / longSide else 1.0
            Imgproc.resize(src, small, Size(), scale, scale)

            Imgproc.cvtColor(small, gray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)
            Imgproc.Canny(gray, edges, 40.0, 120.0)
            Imgproc.dilate(edges, edges, Mat.ones(3, 3, CvType.CV_8U))

            val contours = mutableListOf<MatOfPoint>()
            Imgproc.findContours(
                edges, contours, Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE
            )

            val imageArea: Double = (small.width() * small.height()).toDouble()
            var bestQuad: Array<Point>? = null
            var bestArea = 0.0

            for (c in contours) {
                val c2f = MatOfPoint2f(*c.toArray())
                val peri: Double = Imgproc.arcLength(c2f, true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true)
                if (approx.total() == 4L) {
                    val area: Double = kotlin.math.abs(Imgproc.contourArea(approx))
                    if (area > bestArea && area > imageArea * 0.12 && area < imageArea * 0.98) {
                        bestArea = area
                        bestQuad = approx.toArray()
                    }
                }
                c2f.release()
                approx.release()
                c.release()
            }

            val scaleBack: Double = 1.0 / scale
            return bestQuad?.let { orderCorners(it, scaleBack) }
        } finally {
            src.release(); small.release(); gray.release(); edges.release()
        }
    }

    private fun orderCorners(pts: Array<Point>, scaleBack: Double): FloatArray {
        val bySum = pts.sortedBy { it.x + it.y }
        val tl = bySum.first()
        val br = bySum.last()
        val byDiff = pts.sortedBy { it.y - it.x }
        val tr = byDiff.first()
        val bl = byDiff.last()
        return floatArrayOf(
            (tl.x * scaleBack).toFloat(), (tl.y * scaleBack).toFloat(),
            (tr.x * scaleBack).toFloat(), (tr.y * scaleBack).toFloat(),
            (br.x * scaleBack).toFloat(), (br.y * scaleBack).toFloat(),
            (bl.x * scaleBack).toFloat(), (bl.y * scaleBack).toFloat()
        )
    }

    /**
     * Largest bright region (paper) → its four extreme corners.
     * Works without OpenCV when the page is lighter than the table.
     */
    private fun detectByBrightness(bitmap: Bitmap): FloatArray? {
        val longSide = max(bitmap.width, bitmap.height)
        val scale = if (longSide > 320) 320f / longSide else 1f
        val sw = max(8, (bitmap.width * scale).toInt())
        val sh = max(8, (bitmap.height * scale).toInt())
        val small = if (sw == bitmap.width && sh == bitmap.height) bitmap
        else Bitmap.createScaledBitmap(bitmap, sw, sh, true)
        val pixels = IntArray(sw * sh)
        small.getPixels(pixels, 0, sw, 0, 0, sw, sh)
        if (small !== bitmap) small.recycle()

        val gray = IntArray(sw * sh)
        val hist = IntArray(256)
        for (i in pixels.indices) {
            val p = pixels[i]
            val l = (((p shr 16) and 0xFF) * 299 + ((p shr 8) and 0xFF) * 587 + (p and 0xFF) * 114) / 1000
            gray[i] = l
            hist[l]++
        }
        val threshold = otsu(hist, sw * sh)
        val bright = BooleanArray(sw * sh) { gray[it] >= threshold }

        val label = IntArray(sw * sh)
        val qx = IntArray(sw * sh)
        val qy = IntArray(sw * sh)
        var bestArea = 0
        var tlX = 0; var tlY = 0; var trX = 0; var trY = 0
        var brX = 0; var brY = 0; var blX = 0; var blY = 0
        var id = 0

        for (sy in 0 until sh) {
            for (sx0 in 0 until sw) {
                val start = sy * sw + sx0
                if (!bright[start] || label[start] != 0) continue
                id++
                var head = 0
                var tail = 0
                qx[tail] = sx0
                qy[tail] = sy
                tail++
                label[start] = id
                var area = 0
                var minSum = Int.MAX_VALUE
                var maxSum = Int.MIN_VALUE
                var maxDiff = Int.MIN_VALUE
                var minDiff = Int.MAX_VALUE
                var cTlX = sx0; var cTlY = sy; var cTrX = sx0; var cTrY = sy
                var cBrX = sx0; var cBrY = sy; var cBlX = sx0; var cBlY = sy
                while (head < tail) {
                    val cx = qx[head]
                    val cy = qy[head]
                    head++
                    area++
                    val sum = cx + cy
                    val diff = cx - cy
                    if (sum < minSum) { minSum = sum; cTlX = cx; cTlY = cy }
                    if (sum > maxSum) { maxSum = sum; cBrX = cx; cBrY = cy }
                    if (diff > maxDiff) { maxDiff = diff; cTrX = cx; cTrY = cy }
                    if (diff < minDiff) { minDiff = diff; cBlX = cx; cBlY = cy }
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            val nx = cx + dx
                            val ny = cy + dy
                            if (nx < 0 || ny < 0 || nx >= sw || ny >= sh) continue
                            val ni = ny * sw + nx
                            if (!bright[ni] || label[ni] != 0) continue
                            label[ni] = id
                            qx[tail] = nx
                            qy[tail] = ny
                            tail++
                        }
                    }
                }
                if (area > bestArea) {
                    bestArea = area
                    tlX = cTlX; tlY = cTlY; trX = cTrX; trY = cTrY
                    brX = cBrX; brY = cBrY; blX = cBlX; blY = cBlY
                }
            }
        }

        val imageArea = sw * sh
        if (bestArea < imageArea * 0.12 || bestArea > imageArea * 0.98) return null

        val span = max(max(trX, brX) - min(tlX, blX), max(blY, brY) - min(tlY, trY))
        if (span < min(sw, sh) * 0.25f) return null

        val sx = bitmap.width.toFloat() / sw
        val sy = bitmap.height.toFloat() / sh
        return floatArrayOf(
            tlX * sx, tlY * sy,
            trX * sx, trY * sy,
            brX * sx, brY * sy,
            blX * sx, blY * sy
        )
    }

    private fun otsu(hist: IntArray, total: Int): Int {
        var sum = 0.0
        for (i in 0..255) sum += i * hist[i]
        var sumB = 0.0
        var wB = 0
        var maxVar = -1.0
        var threshold = 127
        for (i in 0..255) {
            wB += hist[i]
            if (wB == 0) continue
            val wF = total - wB
            if (wF == 0) break
            sumB += i * hist[i]
            val mB = sumB / wB
            val mF = (sum - sumB) / wF
            val between = wB.toDouble() * wF * (mB - mF) * (mB - mF)
            if (between > maxVar) {
                maxVar = between
                threshold = i
            }
        }
        return threshold
    }
}
