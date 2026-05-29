package com.example.juegodepelotaconsensores.rendering

import android.graphics.*
import com.example.juegodepelotaconsensores.models.LogicGateData
import com.example.juegodepelotaconsensores.models.GameTheme

object LogicGateRenderer {

    fun draw(
        canvas: Canvas,
        lg: LogicGateData,
        theme: GameTheme,
        signalBus: Map<String, Boolean>
    ) {
        val chipColor = Color.parseColor(theme.logicGateColor)
        val chipBg = Color.parseColor(theme.logicGateBg)
        val isOutputActive = lg.outputLinkId.isNotBlank() && signalBus[lg.outputLinkId] == true
        
        val gatePaint = Paint().apply {
            isAntiAlias = true
        }

        // Cuerpo del chip (Fondo)
        gatePaint.color = chipBg
        gatePaint.style = Paint.Style.FILL
        gatePaint.alpha = 240
        canvas.drawRect(lg.rect, gatePaint)
        
        // Brillo interno cuando la salida está activa
        if (isOutputActive) {
            gatePaint.color = chipColor
            gatePaint.style = Paint.Style.FILL
            gatePaint.alpha = 50
            canvas.drawRect(lg.rect, gatePaint)
        }
        
        // Borde del microchip (Grueso y brillante si está activo)
        gatePaint.color = chipColor
        gatePaint.style = Paint.Style.STROKE
        gatePaint.strokeWidth = if (isOutputActive) 3.5f else 2f
        gatePaint.alpha = 255
        canvas.drawRect(lg.rect, gatePaint)
        
        val cx = lg.rect.centerX()
        val cy = lg.rect.centerY()
        
        // Dibujar terminales de entrada (Pins en la parte superior)
        val inputs = lg.inputLinkIds
        val pinRadius = 4f
        val pinPaint = Paint().apply {
            isAntiAlias = true
        }
        
        if (inputs.isNotEmpty()) {
            for (i in inputs.indices) {
                val inputId = inputs[i]
                val isInputActive = signalBus[inputId] == true
                
                // Verde brillante para activo, gris apagado para inactivo
                pinPaint.color = if (isInputActive) Color.parseColor("#00E676") else Color.parseColor("#757575")
                
                val ratio = if (inputs.size == 1) 0.5f else (0.25f + 0.5f * (i.toFloat() / (inputs.size - 1)))
                val px = lg.rect.left + lg.rect.width() * ratio
                val py = lg.rect.top
                
                // Línea conectora
                pinPaint.style = Paint.Style.STROKE
                pinPaint.strokeWidth = 1.5f
                canvas.drawLine(px, py - 6f, px, py, pinPaint)
                
                // Pin redondo
                pinPaint.style = Paint.Style.FILL
                canvas.drawCircle(px, py - 6f, pinRadius, pinPaint)
                
                // Aura de brillo si la señal de entrada está viva
                if (isInputActive) {
                    pinPaint.alpha = 80
                    canvas.drawCircle(px, py - 6f, pinRadius + 3f, pinPaint)
                    pinPaint.alpha = 255
                }
            }
        }
        
        // Dibujar terminal de salida (Pin en la parte inferior)
        if (lg.outputLinkId.isNotBlank()) {
            pinPaint.color = if (isOutputActive) Color.parseColor("#00E676") else Color.parseColor("#757575")
            val px = lg.rect.left + lg.rect.width() * 0.5f
            val py = lg.rect.bottom
            
            // Aura de brillo de fondo (mas grande para la salida)
            if (isOutputActive) {
                pinPaint.style = Paint.Style.FILL
                pinPaint.color = Color.parseColor("#00E676")
                pinPaint.alpha = 100
                canvas.drawCircle(px, py + 6f, pinRadius + 6f, pinPaint)
                pinPaint.alpha = 255
            }
            
            // Línea conectora
            pinPaint.color = if (isOutputActive) Color.parseColor("#00E676") else Color.parseColor("#757575")
            pinPaint.style = Paint.Style.STROKE
            pinPaint.strokeWidth = if (isOutputActive) 3f else 1.5f
            canvas.drawLine(px, py, px, py + 6f, pinPaint)
            
            // Pin redondo
            pinPaint.style = Paint.Style.FILL
            canvas.drawCircle(px, py + 6f, pinRadius + (if (isOutputActive) 1f else 0f), pinPaint)
            
            // Brillo blanco en el centro para resaltar intensidad
            if (isOutputActive) {
                pinPaint.color = Color.WHITE
                canvas.drawCircle(px, py + 6f, pinRadius * 0.5f, pinPaint)
            }
        }
        
        // Texto de la compuerta (Brilla en blanco al activarse)
        val textPaint = Paint().apply {
            color = if (isOutputActive) Color.WHITE else chipColor
            textSize = 14f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val yPos = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(lg.type, cx, yPos, textPaint)
    }
}
