package com.example.juegodepelotaconsensores.rendering

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.example.juegodepelotaconsensores.models.BossData
import com.example.juegodepelotaconsensores.models.GameTheme

object UIRenderer {

    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        activeBoss: BossData?,
        theme: GameTheme,
        isLevelCompleted: Boolean,
        transitionProgress: Float
    ) {
        // Overlay cinemático de muerte de jefe
        activeBoss?.let { boss ->
            if (boss.isDefeated) {
                val now = System.currentTimeMillis()
                val animationTime = now - boss.lastDamageTime - 2000
                
                if (animationTime > 3000 || isLevelCompleted) {
                    val overlayPaint = Paint().apply { style = Paint.Style.FILL }
                    
                    if (isLevelCompleted || animationTime > 5500) {
                        // Se completó el nivel tras derrotar al jefe, mantener oscuro
                        overlayPaint.color = Color.BLACK
                        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
                    }
                    // Fading to white (3000 to 4000)
                    else if (animationTime <= 4000) {
                        val progress = (animationTime - 3000) / 1000f
                        overlayPaint.color = Color.WHITE
                        overlayPaint.alpha = (progress * 255).toInt().coerceIn(0, 255)
                        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
                    } 
                    // Hold white and fade to black (4000 to 5500)
                    else {
                        val progress = (animationTime - 4000) / 1500f
                        val r = (255 * (1f - progress)).toInt().coerceIn(0, 255)
                        overlayPaint.color = Color.rgb(r, r, r)
                        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
                    }
                }
            }
        }
        
        // Cortina de transición de nivel (fade-in desde negro)
        if (transitionProgress < 1f) {
            val fadePaint = Paint().apply {
                color = Color.BLACK
                alpha = ((1f - transitionProgress) * 255).toInt().coerceIn(0, 255)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fadePaint)
        }

        // UI Layer: Boss Health Bar
        activeBoss?.let { boss ->
            var uiAlpha = 255
            var uiScale = 1f
            var isVisible = true

            // Animación de aparición (Fade In + Scale) si está entrando
            if (boss.entranceProgress < 1f) {
                val p = boss.entranceProgress
                val animP = ((p - 0.15f) / 0.45f).coerceIn(0f, 1f)
                uiAlpha = (255 * animP).toInt().coerceIn(0, 255)
                uiScale = 0.5f + (0.5f * animP)
            }

            // Animación de muerte
            if (boss.isDefeated) {
                val now = System.currentTimeMillis()
                val animationTime = now - boss.lastDamageTime - 2000
                
                if (animationTime > 3000) {
                    isVisible = false // Desaparece con el flash blanco de la pantalla
                } else if (animationTime > 2700) {
                    // Colapso rápido en los últimos 300ms
                    val collapse = (animationTime - 2700) / 300f
                    uiScale = 1f - collapse
                    uiAlpha = (255 * (1f - collapse)).toInt().coerceIn(0, 255)
                } else if (animationTime > 0) {
                    // Parpadeo frenético
                    val speed = 100 - (animationTime / 2700f * 80).toLong()
                    val blink = (animationTime % Math.max(10L, speed)) / Math.max(10L, speed).toFloat()
                    uiAlpha = if (blink < 0.5f) 255 else 50
                }
            }
            
            val nowTime = System.currentTimeMillis()
            val timeSinceDamage = nowTime - boss.lastDamageTime
            val invulnerabilityDuration = 1000L

            // Animar visualHealth (vida animada) sincronizado con el invulnerability
            if (boss.visualHealth > boss.health) {
                if (timeSinceDamage >= invulnerabilityDuration) {
                    boss.visualHealth = boss.health.toFloat()
                } else {
                    val framesRemaining = Math.max(1f, (invulnerabilityDuration - timeSinceDamage) / 16f)
                    boss.visualHealth -= (boss.visualHealth - boss.health) / framesRemaining
                }
            } else if (boss.visualHealth < boss.health) {
                boss.visualHealth = boss.health.toFloat()
            }
            
            if (isVisible) {
                val baseBarWidth = width * 0.7f
                val barWidth = baseBarWidth * uiScale
                val barHeight = 35f * uiScale
                val barX = (width - barWidth) / 2f
                val barY = 120f + (35f - barHeight) / 2f // Mantener centrado verticalmente

                // Background bar
                val uiPaint = Paint().apply {
                    style = Paint.Style.FILL
                    color = Color.parseColor("#333333")
                    alpha = (200 * (uiAlpha / 255f)).toInt()
                    isAntiAlias = true
                }
                canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, uiPaint)
                
                // Fondo animado de daño (blanco)
                val damageRatio = boss.visualHealth / boss.maxHealth
                uiPaint.color = Color.WHITE
                uiPaint.alpha = (uiAlpha * 0.7f).toInt()
                canvas.drawRect(barX, barY, barX + (barWidth * damageRatio), barY + barHeight, uiPaint)
                
                // Health bar actual con parpadeo
                val healthRatio = boss.health.toFloat() / boss.maxHealth
                var currentHealthAlpha = uiAlpha
                if (timeSinceDamage < invulnerabilityDuration) {
                    if ((nowTime / 80) % 2 == 0L) {
                        uiPaint.color = Color.WHITE
                        currentHealthAlpha = (uiAlpha * 0.6f).toInt()
                    } else {
                        uiPaint.color = Color.parseColor(theme.bossLaserColor)
                    }
                } else {
                    uiPaint.color = Color.parseColor(theme.bossLaserColor)
                }
                uiPaint.alpha = currentHealthAlpha
                canvas.drawRect(barX, barY, barX + (barWidth * healthRatio), barY + barHeight, uiPaint)
                
                // Stroke bar
                uiPaint.style = Paint.Style.STROKE
                uiPaint.color = Color.WHITE
                uiPaint.strokeWidth = 4f * uiScale
                uiPaint.alpha = (180 * (uiAlpha / 255f)).toInt()
                canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, uiPaint)

                // Boss Name
                val textPaint = Paint().apply {
                    color = Color.parseColor(theme.bossLaserColor)
                    textSize = 65f * uiScale
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    alpha = uiAlpha
                    isAntiAlias = true
                    setShadowLayer(8f * uiScale, 0f, 0f, Color.BLACK)
                }
                canvas.drawText(boss.name.uppercase(), width / 2f, barY - (25f * uiScale), textPaint)
            }
        }
    }
}
