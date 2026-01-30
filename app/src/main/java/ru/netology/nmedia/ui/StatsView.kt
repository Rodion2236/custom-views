package ru.netology.nmedia.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import ru.netology.nmedia.R
import ru.netology.nmedia.util.AndroidUtils
import kotlin.math.min
import kotlin.random.Random

class StatsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : View(context, attrs, defStyleAttr, defStyleRes) {
    private var radius = 0F
    private var center = PointF(0F, 0F)
    private var oval = RectF(0F, 0F, 0F, 0F)

    private var lineWidth = AndroidUtils.dp(context, 5F).toFloat()
    private var fontSize = AndroidUtils.dp(context, 40F).toFloat()
    private var colors = emptyList<Int>()
    private var emptyColor = context.getColor(R.color.stats_empty)

    init {
        context.withStyledAttributes(attrs, R.styleable.StatsView) {
            lineWidth = getDimension(R.styleable.StatsView_lineWidth, lineWidth)
            fontSize = getDimension(R.styleable.StatsView_fontSize, fontSize)
            val resId = getResourceId(R.styleable.StatsView_colors, 0)
            colors = resources.getIntArray(resId).toList()
        }
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = lineWidth
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = fontSize
    }

    var data: List<Float> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val padding = lineWidth / 2
        radius = min(w, h) / 2F - padding
        center = PointF(w / 2F, h / 2F)
        oval = RectF(
            center.x - radius, center.y - radius,
            center.x + radius, center.y + radius,
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (data.isEmpty()) return

        paint.color = emptyColor
        canvas.drawArc(oval, -90F, 360F, false, paint)

        val total = data.sum()
        if (total <= 0F) return

        val normalize = total > 1.0F

        var startAngle = -90F
        var firstSectorColor: Int? = null

        for ((index, value) in data.withIndex()) {
            val sectorValue = if (normalize) value / total else value
            val sweepAngle = 360F * sectorValue.coerceIn(0F, 1F)

            val color = colors.getOrNull(index) ?: randomColor()
            if (index == 0) firstSectorColor = color

            paint.color = color
            canvas.drawArc(oval, startAngle, sweepAngle, false, paint)
            startAngle += sweepAngle
        }

        paint.apply {
            color = firstSectorColor!!
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center.x, center.y - radius, lineWidth / 2 + 0.5F, paint)

        val fillPercent = if (normalize) 1.0F else total.coerceIn(0F, 1F)
        val metrics = textPaint.fontMetrics
        val textY = center.y - (metrics.descent + metrics.ascent) / 2
        canvas.drawText("%.2f%%".format(fillPercent * 100), center.x, textY, textPaint)
    }

    private fun randomColor() = 0xFF000000.toInt() or Random.nextInt(0x00FFFFFF)
}