package com.nibras.scan.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

/** Shows a page and lets the user drag / pinch a signature on it. Rect is kept relative (0..1). */
class PlacementView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {

    var page: Bitmap? = null
        set(v) { field = v; invalidate() }
    var signature: Bitmap? = null
        set(v) { field = v; resetRect(); invalidate() }

    /** Signature rect relative to the page. */
    val rel = RectF(0.55f, 0.78f, 0.9f, 0.9f)
    private val pageRect = RectF()
    private val border = Paint().apply {
        style = Paint.Style.STROKE; color = Color.parseColor("#2FB0FF"); strokeWidth = 3f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }
    private val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private var lastX = 0f; private var lastY = 0f

    private fun resetRect() {
        val s = signature ?: return
        val pw = page?.width ?: 1; val ph = page?.height ?: 1
        val relW = 0.35f
        val relH = relW * pw * s.height / (s.width.toFloat() * ph)
        rel.set(0.58f, 0.85f - relH, 0.58f + relW, 0.85f)
    }

    private val scaler = ScaleGestureDetector(ctx, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector): Boolean {
            val f = d.scaleFactor
            val cx = rel.centerX(); val cy = rel.centerY()
            val w = (rel.width() * f).coerceIn(0.08f, 0.95f)
            val h = rel.height() * (w / rel.width())
            rel.set(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
            clamp(); invalidate(); return true
        }
    })

    private fun clamp() {
        if (rel.left < 0) rel.offset(-rel.left, 0f)
        if (rel.top < 0) rel.offset(0f, -rel.top)
        if (rel.right > 1) rel.offset(1 - rel.right, 0f)
        if (rel.bottom > 1) rel.offset(0f, 1 - rel.bottom)
    }

    override fun onDraw(canvas: Canvas) {
        val p = page ?: return
        val s = minOf(width.toFloat() / p.width, height.toFloat() / p.height)
        val w = p.width * s; val h = p.height * s
        pageRect.set((width - w) / 2, (height - h) / 2, (width + w) / 2, (height + h) / 2)
        canvas.drawBitmap(p, null, pageRect, bmpPaint)
        val sig = signature ?: return
        val r = RectF(pageRect.left + rel.left * w, pageRect.top + rel.top * h,
            pageRect.left + rel.right * w, pageRect.top + rel.bottom * h)
        canvas.drawBitmap(sig, null, r, bmpPaint)
        canvas.drawRect(r, border)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        scaler.onTouchEvent(e)
        if (pageRect.width() <= 0) return true
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { lastX = e.x; lastY = e.y; parent?.requestDisallowInterceptTouchEvent(true) }
            MotionEvent.ACTION_MOVE -> if (!scaler.isInProgress && e.pointerCount == 1) {
                rel.offset((e.x - lastX) / pageRect.width(), (e.y - lastY) / pageRect.height())
                clamp(); lastX = e.x; lastY = e.y; invalidate()
            }
            MotionEvent.ACTION_POINTER_UP -> { lastX = e.x; lastY = e.y }
        }
        return true
    }
}
