package com.ollie.chesscoach

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.view.Gravity
import android.view.View
import android.view.WindowManager

class OverlayView(private val ctx: Context) : View(ctx) {

    private var best: StockfishEngine.Best? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 80, 200, 120)
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
    }

    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    fun attach() { wm.addView(this, params) }
    fun detach() { runCatching { wm.removeView(this) } }
    fun update(b: StockfishEngine.Best) { best = b; postInvalidate() }
    fun clear() { best = null; postInvalidate() }

    override fun onDraw(canvas: Canvas) {
        val b = best ?: return
        val cellW = width / 8f
        val cellH = height / 8f
        val from = square(b.from, cellW, cellH)
        val to = square(b.to, cellW, cellH)
        canvas.drawLine(from.x, from.y, to.x, to.y, paint)
        // arrowhead
        val ang = Math.atan2((to.y - from.y).toDouble(), (to.x - from.x).toDouble())
        val size = 40f
        for (off in listOf(-0.5, 0.5)) {
            canvas.drawLine(
                to.x, to.y,
                to.x - size * Math.cos(ang + off),
                to.y - size * Math.sin(ang + off),
                paint
            )
        }
    }

    private fun square(sq: String, cw: Float, ch: Float): PointF {
        val file = sq[0] - 'a'
        val rank = 8 - (sq[1] - '1') - 1
        return PointF((file + 0.5f) * cw, (8 - rank - 0.5f) * ch)
    }
}