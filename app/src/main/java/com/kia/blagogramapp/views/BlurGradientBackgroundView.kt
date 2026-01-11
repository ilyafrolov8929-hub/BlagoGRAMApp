package com.kia.blagogramapp.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin

class BlurGradientBackgroundView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase = 0f
    private val animator = ValueAnimator.ofFloat(0f, Math.PI.toFloat() * 2f).apply {
        duration = 10000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        // Рисуем основной темный фон
        canvas.drawColor(Color.parseColor("#0A0A0A"))

        // Рисуем несколько размытых пятен, которые движутся
        drawSpot(canvas, w * 0.2f + sin(phase) * 100, h * 0.3f + cos(phase) * 100, w * 0.6f, Color.parseColor("#4400FF77"))
        drawSpot(canvas, w * 0.8f + cos(phase * 0.8f) * 150, h * 0.7f + sin(phase * 0.8f) * 150, w * 0.7f, Color.parseColor("#447700FF"))
        drawSpot(canvas, w * 0.5f + sin(phase * 1.2f) * 120, h * 0.5f, w * 0.5f, Color.parseColor("#33FF00CC"))
    }

    private fun drawSpot(canvas: Canvas, x: Float, y: Float, radius: Float, color: Int) {
        paint.shader = RadialGradient(
            x, y, radius,
            intArrayOf(color, Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x, y, radius, paint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }
}
