package com.example.juegodepelotaconsensores.rendering

import android.graphics.*
import com.example.juegodepelotaconsensores.models.BossData
import com.example.juegodepelotaconsensores.models.SwitchData
import com.example.juegodepelotaconsensores.models.GameTheme
import com.example.juegodepelotaconsensores.models.ProjectileData
import com.example.juegodepelotaconsensores.engine.ParticleManager

object BossRenderer {

    fun draw(
        canvas: Canvas,
        boss: BossData,
        theme: GameTheme,
        switches: List<SwitchData>,
        projectiles: List<ProjectileData>
    ) {

        // Dibujar Proyectiles del Jefe
        val projPaint = Paint().apply {
            color = Color.parseColor(theme.bossLaserColor)
            style = Paint.Style.FILL
            isAntiAlias = true
            setShadowLayer(10f, 0f, 0f, Color.parseColor(theme.bossLaserColor))
        }
        val innerPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        for (proj in projectiles) {
            canvas.drawCircle(proj.x, proj.y, proj.radius, projPaint)
            canvas.drawCircle(proj.x, proj.y, proj.radius * 0.4f, innerPaint)
        }

        val now = System.currentTimeMillis()
        val floatY = boss.floatOffset
        val currentRect = RectF(boss.rect).apply { offset(0f, floatY) }
        
        val bossPaint = Paint().apply {
            isAntiAlias = true
        }
        
        val cx = currentRect.centerX()
        var cy = currentRect.centerY()
        var rx = currentRect.width() / 2f
        var ry = currentRect.height() / 2f
        var baseAlpha = 240
        
        if (boss.entranceProgress < 1f) {
            val progress = boss.entranceProgress
            // Boss should only animate between 0.15 and 0.60
            val animP = ((progress - 0.15f) / 0.45f).coerceIn(0f, 1f)
            // Caer desde arriba
            cy -= 600f * (1f - animP)
            // Hacerse sólido poco a poco
            baseAlpha = (240 * animP).toInt().coerceIn(0, 240)
            
            // Efecto visual de "Rugido" al aterrizar
            if (progress > 0.6f && progress <= 0.85f) {
                val roarProgress = (progress - 0.6f) / 0.25f
                val roarPaint = Paint().apply {
                    style = Paint.Style.STROKE
                    color = Color.parseColor(theme.bossLaserColor)
                    isAntiAlias = true
                }
                for (i in 0..2) {
                    val ringProgress = (roarProgress - (i * 0.1f)).coerceIn(0f, 1f)
                    if (ringProgress > 0f) {
                        roarPaint.strokeWidth = 20f * (1f - ringProgress)
                        roarPaint.alpha = (200 * (1f - ringProgress)).toInt().coerceIn(0, 255)
                        canvas.drawCircle(cx, cy, Math.max(rx, ry) * 5f * ringProgress, roarPaint)
                    }
                }
            }
        }
        
        // Onda expansiva gigante cuando recibe daño
        if (!boss.isDefeated) {
            val timeSinceDamage = now - boss.lastDamageTime
            if (timeSinceDamage < 1000) {
                val shockProgress = timeSinceDamage / 1000f
                val shockPaint = Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 30f * (1f - shockProgress)
                    color = Color.WHITE
                    alpha = (100 * (1f - shockProgress)).toInt().coerceIn(0, 255)
                    isAntiAlias = true
                }
                canvas.drawCircle(cx, cy, 1500f * shockProgress, shockPaint)
                
                val innerShockPaint = Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 50f * (1f - shockProgress)
                    color = Color.parseColor(theme.bossLaserColor)
                    alpha = (60 * (1f - shockProgress)).toInt().coerceIn(0, 255)
                    isAntiAlias = true
                }
                canvas.drawCircle(cx, cy, 1400f * shockProgress, innerShockPaint)
            }
        }
        
        // Draw Pre-Special Charging Glow
        val isCharging = boss.normalAttackCount >= boss.specialAttackFrequency
        if (!boss.isDefeated && isCharging) {
            val pulse = (Math.sin(now / 100.0) * 0.5 + 0.5).toFloat()
            val chargingPaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 10f + 10f * pulse
                color = Color.parseColor(theme.bossLaserColor)
                alpha = (150 * pulse).toInt()
                isAntiAlias = true
                setShadowLayer(30f, 0f, 0f, Color.parseColor(theme.bossLaserColor))
            }
            canvas.drawCircle(cx, cy, Math.max(rx, ry) * 1.5f, chargingPaint)
        }

        // Si está derrotado, dibujar animación de explosión inminente
        if (boss.isDefeated) {
            val timeSinceDefeat = now - boss.lastDamageTime
            
            if (timeSinceDefeat < 2000) {
                // Durante el paneo de cámara, dibujarlo normal sin animaciones
                val normalPath = Path()
                normalPath.moveTo(cx, cy - ry)
                normalPath.lineTo(cx + rx, cy)
                normalPath.lineTo(cx, cy + ry)
                normalPath.lineTo(cx - rx, cy)
                normalPath.close()
                
                bossPaint.style = Paint.Style.FILL
                bossPaint.color = Color.parseColor(theme.bossColor)
                bossPaint.alpha = baseAlpha
                canvas.drawPath(normalPath, bossPaint)
                return
            }
            
            val animationTime = timeSinceDefeat - 2000
            if (animationTime > 5500) return
            
            // Incremento progresivo de blanco
            val glow = (animationTime.toFloat() / 3000f).coerceIn(0f, 1f)
            val shakeX = (Math.random().toFloat() - 0.5f) * 15f * glow
            
            val defeatPath = Path()
            defeatPath.moveTo(cx + shakeX, cy - ry)
            defeatPath.lineTo(cx + rx + shakeX, cy)
            defeatPath.lineTo(cx + shakeX, cy + ry)
            defeatPath.lineTo(cx - rx + shakeX, cy)
            defeatPath.close()
            
            // Dibujar el boss normal pero más brillante
            val normalPaint = Paint().apply {
                style = Paint.Style.FILL
                color = Color.parseColor(theme.bossColor)
                alpha = baseAlpha
                isAntiAlias = true
            }
            canvas.drawPath(defeatPath, normalPaint)
            
            bossPaint.style = Paint.Style.FILL
            bossPaint.color = Color.WHITE
            bossPaint.alpha = (255 * glow).toInt()
            canvas.drawPath(defeatPath, bossPaint)
            
            return
        }

        val isInvulnerable = now - boss.lastDamageTime < 1000
        val isFlash = isInvulnerable && (now / 100) % 2 == 0L
        
        val path = Path()
        path.moveTo(cx, cy - ry)
        path.lineTo(cx + rx, cy)
        path.lineTo(cx, cy + ry)
        path.lineTo(cx - rx, cy)
        path.close()
        
        bossPaint.style = Paint.Style.FILL
        bossPaint.color = if (isFlash) Color.parseColor(theme.hazardColor) else Color.parseColor(theme.bossColor)
        bossPaint.alpha = baseAlpha
        canvas.drawPath(path, bossPaint)
        
        val phaseIntensity = if (boss.phases > 1) (boss.currentPhase - 1).toFloat() / (boss.phases - 1).toFloat() else 0f
        
        bossPaint.style = Paint.Style.STROKE
        bossPaint.strokeWidth = 5f + phaseIntensity * 10f
        bossPaint.color = Color.parseColor(theme.bossLaserColor)
        bossPaint.alpha = baseAlpha
        if (isFlash) {
            bossPaint.color = Color.WHITE
        }
        canvas.drawPath(path, bossPaint)
        
        bossPaint.style = Paint.Style.FILL
        bossPaint.color = Color.parseColor(theme.bossLaserColor)
        
        val pulseSpeed = 150.0 - (80.0 * phaseIntensity)
        val pulse = 0.8f + (0.2f + 0.2f * phaseIntensity) * Math.sin(now / pulseSpeed).toFloat()
        bossPaint.alpha = (baseAlpha * pulse * (180f / 240f)).toInt().coerceIn(0, 255)
        
        val maxRadiusScale = 0.35f + 0.15f * phaseIntensity
        canvas.drawCircle(cx, cy, rx * maxRadiusScale * pulse, bossPaint)
        


        // Rayo láser de daño
        if (isInvulnerable) {
            for (sw in switches) {
                if (sw.outputLinkId == boss.inputLinkId && (sw.isHoldActive || sw.isToggleActive)) {
                    val laserPaint = Paint().apply {
                        color = Color.parseColor(theme.bossLaserColor)
                        style = Paint.Style.STROKE
                        strokeWidth = 8f + 4f * Math.sin(now / 50.0).toFloat()
                        isAntiAlias = true
                        setShadowLayer(20f, 0f, 0f, Color.parseColor(theme.bossLaserColor))
                    }
                    canvas.drawLine(sw.rect.centerX(), sw.rect.centerY(), cx, cy, laserPaint)
                    
                    val laserInner = Paint().apply {
                        color = Color.WHITE
                        style = Paint.Style.STROKE
                        strokeWidth = 3f
                        isAntiAlias = true
                    }
                    canvas.drawLine(sw.rect.centerX(), sw.rect.centerY(), cx, cy, laserInner)
                }
            }
        }

        // Onda expansiva en mundo
        if (boss.isDefeated) {
            val animationTime = now - boss.lastDamageTime - 2000
            if (animationTime in 3001..5000) {
                val waveProgress = (animationTime - 3000) / 2000f
                val wavePaint = Paint().apply {
                    style = Paint.Style.STROKE
                    color = Color.WHITE
                    alpha = (255 * (1f - waveProgress)).toInt().coerceIn(0, 255)
                    strokeWidth = 40f * (1f - waveProgress)
                    isAntiAlias = true
                }
                canvas.drawCircle(boss.rect.centerX(), boss.rect.centerY(), waveProgress * 3000f, wavePaint)
            }
        }
    }
}
