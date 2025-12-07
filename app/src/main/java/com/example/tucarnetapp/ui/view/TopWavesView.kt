package com.example.tucarnetapp.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.tucarnetapp.R

class TopWavesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ==== Parámetros ajustables ====
    // Qué tan alta es la vista en relación con el ancho (antes era 262/400 ≈ 0.65)
    private val HEIGHT_RATIO = 0.80f      // súbelo si quieres aún más alto

    // Offsets verticales (en proporción al alto de la elipse)
    // Valores menos negativos => las elipses bajan más
    private val PRIMARY_TOP_FACTOR = -70f / 262f   // antes -108/262
    private val SECONDARY_TOP_FACTOR = -100f / 262f// antes -136/262

    // Margen horizontal para que se salga un poco a los lados
    private val HORIZONTAL_MARGIN_FACTOR = 21f / 400f

    private val lightRedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // mismos colores que usas en el layer-list, pero tomados de R.color
        color = ContextCompat.getColor(context, R.color.carnet_red_main) // #E30613
        style = Paint.Style.FILL
    }

    private val darkRedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.carnet_red_dark) // #BC0017
        style = Paint.Style.FILL
    }

    private val lightOval = RectF()
    private val darkOval = RectF()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val desiredHeight = (width * HEIGHT_RATIO).toInt()

        val finalWidth = resolveSize(width, widthMeasureSpec)
        val finalHeight = resolveSize(desiredHeight, heightMeasureSpec)

        setMeasuredDimension(finalWidth, finalHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val widthF = w.toFloat()
        val heightF = h.toFloat()

        val left = -HORIZONTAL_MARGIN_FACTOR * widthF
        val right = widthF * (1f + HORIZONTAL_MARGIN_FACTOR)

        // La elipse mide todo el alto de la vista
        val ellipseHeight = heightF

        val lightTop = PRIMARY_TOP_FACTOR * ellipseHeight
        val darkTop = SECONDARY_TOP_FACTOR * ellipseHeight

        lightOval.set(left, lightTop, right, lightTop + ellipseHeight)
        darkOval.set(left, darkTop, right, darkTop + ellipseHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawOval(lightOval, lightRedPaint)   // primero clara
        canvas.drawOval(darkOval, darkRedPaint)     // luego oscura encima
    }
}