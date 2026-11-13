package com.wwdcqxt.wordmemory.dashboard.chart

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.wwdcqxt.wordmemory.dashboard.R

/**
 * 自绘环形进度图：展示总体正确率。
 * 背景圆环 + 前景弧 + 中心百分比文字，全部用 Canvas 绘制。
 */
class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var percent: Float = 0f

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.dash_grid)
    }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.dash_accent)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.dash_text)
        textAlign = Paint.Align.CENTER
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.dash_text_secondary)
        textAlign = Paint.Align.CENTER
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            11f,
            resources.displayMetrics
        )
    }

    private val oval = RectF()

    fun setPercent(value: Float) {
        percent = value.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val stroke = 14f * density()
        val size = minOf(width, height).toFloat() - stroke
        val left = (width - size) / 2
        val top = (height - size) / 2
        oval.set(left, top, left + size, top + size)

        trackPaint.strokeWidth = stroke
        arcPaint.strokeWidth = stroke

        canvas.drawArc(oval, 0f, 360f, false, trackPaint)
        if (percent > 0f) {
            // 从 12 点方向顺时针画
            canvas.drawArc(oval, -90f, 360f * percent, false, arcPaint)
        }

        textPaint.textSize = size * 0.22f
        canvas.drawText(
            "${(percent * 100).toInt()}%",
            width / 2f,
            height / 2f + textPaint.textSize * 0.35f,
            textPaint
        )
        canvas.drawText("正确率", width / 2f, height / 2f - textPaint.textSize * 0.9f, hintPaint)
    }

    private fun density(): Float = resources.displayMetrics.density
}
