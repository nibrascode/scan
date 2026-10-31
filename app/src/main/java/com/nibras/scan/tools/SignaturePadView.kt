package com.nibras.scan.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/** Finger signature pad. Exports a tightly cropped transparent PNG bitmap. */
class SignaturePadView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {

    private val paths = mutableListOf<Pair<Path, Int>>()
    private var current: Path? = null
    var inkColor: Int = Color.parseColor("#0D1B3E")
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        strokeWidth = 6f * resources.displayMetrics.density
    }
    private val guide = Paint().apply { color = Color.parseColor("#C9D1DC"); strokeWidth = 2f }
    var onChanged: (() -> Unit)? = null

    val isEmpty get() = paths.isEmpty()

    fun clear() { paths.clear(); invalidate(); onChanged?.invoke() }
    fun undo() { if (paths.isNotEmpty()) paths.removeAt(paths.size - 1); invalidate(); onChanged?.invoke() }

    override fun onDraw(canvas: Canvas) {
        val y = height * 0.75f
        canvas.drawLine(width * 0.08f, y, width * 0.92f, y, guide)
        for ((p, c) in paths) { paint.color = c; canvas.drawPath(p, paint) }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                current = Path().apply { moveTo(e.x, e.y) }.also { paths.add(it to inkColor) }
            }
            MotionEvent.ACTION_MOVE -> {
                for (h in 0 until e.historySize) current?.lineTo(e.getHistoricalX(h), e.getHistoricalY(h))
                current?.lineTo(e.x, e.y)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { current = null; onChanged?.invoke() }
        }
        invalidate()
        return true
    }

    fun export(): Bitmap? {
        if (paths.isEmpty()) return null
        val bounds = RectF(); val tmp = RectF()
        paths.forEachIndexed { i, (p, _) -> p.computeBounds(tmp, true); if (i == 0) bounds.set(tmp) else bounds.union(tmp) }
        val pad = paint.strokeWidth * 2
        bounds.inset(-pad, -pad)
        val w = bounds.width().toInt().coerceAtLeast(1); val h = bounds.height().toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp); c.translate(-bounds.left, -bounds.top)
        for ((p, col) in paths) { paint.color = col; c.drawPath(p, paint) }
        return bmp
    }
}
