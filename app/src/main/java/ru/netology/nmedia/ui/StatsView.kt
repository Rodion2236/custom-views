package ru.netology.nmedia.ui

import android.animation.ValueAnimator
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
    private var progress = 0F
    private var rotationAngle = 0F
    private enum class FillType { PARALLEL, SEQUENTIAL, BIDIRECTIONAL}

    private var fillType = FillType.PARALLEL

    init {
        context.withStyledAttributes(attrs, R.styleable.StatsView) {
            lineWidth = getDimension(R.styleable.StatsView_lineWidth, lineWidth)
            fontSize = getDimension(R.styleable.StatsView_fontSize, fontSize)
            val resId = getResourceId(R.styleable.StatsView_colors, 0)
            colors = resources.getIntArray(resId).toList()
            emptyColor = getColor(R.styleable.StatsView_emptyColor, emptyColor)
            fillType = when (getInt(R.styleable.StatsView_fillType, 1)) {
                0 -> FillType.PARALLEL
                2 -> FillType.BIDIRECTIONAL
                else -> FillType.SEQUENTIAL
            }
        }
    }

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

        val total = data.sum()
        if (total <= 0F) return

        val normalize = total > 1.0F

        if (fillType == FillType.PARALLEL) {
            canvas.save()
            canvas.rotate(360F * progress, center.x, center.y)
        }

        val sectorShares = mutableListOf<Float>()
        val sectorAngles = mutableListOf<Float>()

        for (value in data) {
            val share = if (normalize) value / total else value.coerceIn(0F, 1F)
            sectorShares.add(share)
            sectorAngles.add(360F * share)
        }

        var startAngle = -90F
        var firstSectorColor: Int? = null

        when (fillType) {
            FillType.PARALLEL -> {
                for ((index, fullAngle) in sectorAngles.withIndex()) {
                    val color = colors.getOrNull(index) ?: randomColor()
                    if (index == 0) firstSectorColor = color

                    paint.color = color
                    val currentAngle = fullAngle * progress
                    canvas.drawArc(oval, startAngle, currentAngle, false, paint)
                    startAngle += fullAngle
                }
            }

            FillType.SEQUENTIAL -> {
                var filledShare = 0F
                for ((index, fullAngle) in sectorAngles.withIndex()) {
                    val share = sectorShares[index]
                    val color = colors.getOrNull(index) ?: randomColor()
                    if (index == 0) firstSectorColor = color

                    paint.color = color

                    val currentAngle = when {
                        progress <= filledShare -> 0F
                        progress >= filledShare + share -> fullAngle
                        else -> fullAngle * ((progress - filledShare) / share)
                    }

                    canvas.drawArc(oval, startAngle, currentAngle, false, paint)
                    startAngle += fullAngle
                    filledShare += share
                }
            }

            FillType.BIDIRECTIONAL -> {
                for ((index, fullAngle) in sectorAngles.withIndex()) {
                    val color = colors.getOrNull(index) ?: randomColor()
                    if (index == 0) firstSectorColor = color

                    paint.color = color

                    val centerAngle = startAngle + fullAngle / 2F
                    val currentAngle = fullAngle * progress
                    val arcStart = centerAngle - currentAngle / 2F

                    canvas.drawArc(oval, arcStart, currentAngle, false, paint)
                    startAngle += fullAngle
                }
            }
        }

        if (progress >= 1.0F) {
            paint.apply {
                color = firstSectorColor!!
                style = Paint.Style.FILL
            }
            canvas.drawCircle(center.x, center.y - radius, lineWidth / 2 + 0.5F, paint)
        }

        if (fillType == FillType.PARALLEL) {
            canvas.restore()
        }

        val fillPercent = progress * 100
        val metrics = textPaint.fontMetrics
        val textY = center.y - (metrics.descent + metrics.ascent) / 2
        canvas.drawText("%.2f%%".format(fillPercent), center.x, textY, textPaint)
    }

    private fun randomColor() = 0xFF000000.toInt() or Random.nextInt(0x00FFFFFF)

    fun startAnimation(duration: Long = 1500) {
        ValueAnimator.ofFloat(0F, 1F).apply {
            addUpdateListener { animation ->
                progress = animation.animatedValue as Float
                rotationAngle = 360F * progress
                invalidate()
            }
            setDuration(duration)
            start()
        }
    }
}