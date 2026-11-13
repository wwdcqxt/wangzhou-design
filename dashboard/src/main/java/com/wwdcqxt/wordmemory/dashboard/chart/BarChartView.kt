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
 * 自绘柱状图（不使用第三方图表库）。
 *
 * 绘制内容：三条水平网格线、圆角柱子、首/中/末三个日期标签。
 * 数据为空或全为 0 时绘制“暂无数据”占位，符合空态要求。
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var points: List<Pair<String, Int>> = emptyList()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.dash_bar)
    }
    private val barDimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.dash_bar_dim)
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.dash_grid)
        strokeWidth = density()
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.dash_text_secondary)
        textSize = sp(10f)
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.dash_text)
        textSize = sp(10f)
        textAlign = Paint.Align.CENTER
    }

    private val barRect = RectF()

    fun setData(data: List<Pair<String, Int>>) {
        points = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val paddingLeft = 8f * density()
        val paddingRight = 8f * density()
        val paddingTop = 18f * density()
        val paddingBottom = 22f * density()
        val chartLeft = paddingLeft
        val chartRight = width - paddingRight
        val chartTop = paddingTop
        val chartBottom = height - paddingBottom

        // 网格线
        val gridCount = 3
        for (i in 0..gridCount) {
            val y = chartTop + (chartBottom - chartTop) * i / gridCount
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
        }

        if (points.isEmpty() || points.all { it.second == 0 }) {
            labelPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                "暂无复习数据",
                (chartLeft + chartRight) / 2,
                (chartTop + chartBottom) / 2,
                labelPaint
            )
            labelPaint.textAlign = Paint.Align.LEFT
            return
        }

        val maxValue = points.maxOf { it.second }.coerceAtLeast(1)
        val slotWidth = (chartRight - chartLeft) / points.size
        // 只有一两天数据时柱子会占满整屏，限制最大宽度保证观感
        val barWidth = minOf(slotWidth * 0.55f, 46f * density())
        val radius = 4f * density()

        points.forEachIndexed { index, (label, value) ->
            val centerX = chartLeft + slotWidth * index + slotWidth / 2
            val barHeight = (chartBottom - chartTop) * (value.toFloat() / maxValue)
            val top = chartBottom - barHeight
            barRect.set(centerX - barWidth / 2, top, centerX + barWidth / 2, chartBottom)

            barPaint.color = ContextCompat.getColor(
                context,
                if (index == points.lastIndex) R.color.dash_bar else R.color.dash_primary
            )
            if (barHeight > 0f) {
                canvas.drawRoundRect(barRect, radius, radius, barPaint)
            }

            if (value > 0) {
                canvas.drawText(value.toString(), centerX, top - 4f * density(), valuePaint)
            }

            // 只在首、中、末标日期，避免 14 个标签挤在一起；首末贴边对齐防止被裁切
            if (index == 0 || index == points.size / 2 || index == points.lastIndex) {
                val text = label.takeLast(5)
                when (index) {
                    0 -> {
                        labelPaint.textAlign = Paint.Align.LEFT
                        canvas.drawText(text, chartLeft, height - 6f * density(), labelPaint)
                    }
                    points.lastIndex -> {
                        labelPaint.textAlign = Paint.Align.RIGHT
                        canvas.drawText(text, chartRight, height - 6f * density(), labelPaint)
                    }
                    else -> {
                        labelPaint.textAlign = Paint.Align.CENTER
                        canvas.drawText(text, centerX, height - 6f * density(), labelPaint)
                    }
                }
            }
        }
    }

    private fun density(): Float = resources.displayMetrics.density

    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
}
