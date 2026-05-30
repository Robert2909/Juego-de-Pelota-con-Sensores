package com.example.juegodepelotaconsensores.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import com.example.juegodepelotaconsensores.models.TrailPoint

object PlayerRenderer {

    fun draw(
        canvas: Canvas,
        posX: Float,
        posY: Float,
        radius: Float,
        trailPoints: List<TrailPoint>,
        trailPaint: Paint,
        ballPaint: Paint,
        isLevelCompleted: Boolean
    ) {
        if (!isLevelCompleted) {
            // 1. Dibujar estela ultra-fluida (Cinta continua tapered)
            val size = trailPoints.size
            if (size > 0) {
                trailPaint.strokeCap = Paint.Cap.ROUND
                trailPaint.style = Paint.Style.STROKE
                
                for (i in 0 until size) {
                    val p1 = trailPoints[i]
                    val p2 = if (i == size - 1) {
                        PointF(posX, posY) // Conectar directamente al centro de la bola
                    } else {
                        PointF(trailPoints[i + 1].x, trailPoints[i + 1].y)
                    }
                    
                    val progress = i.toFloat() / size
                    trailPaint.alpha = (p1.alpha * progress * 160).toInt().coerceIn(0, 255)
                    trailPaint.strokeWidth = radius * 2f * progress * p1.alpha
                    
                    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, trailPaint)
                }
            }

            // 2. Dibujar pelota
            canvas.drawCircle(posX, posY, radius, ballPaint)
        }
    }
}
