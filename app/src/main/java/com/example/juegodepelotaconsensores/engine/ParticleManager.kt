package com.example.juegodepelotaconsensores.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.juegodepelotaconsensores.models.Particle

object ParticleManager {
    val particles = mutableListOf<Particle>()

    fun clear() {
        particles.clear()
    }

    fun updateAndDraw(canvas: Canvas) {
        val iterator = particles.iterator()
        val particlePaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.1f // Gravedad muy suave
            p.vx *= 0.96f // Fricción suave
            
            p.size -= 0.15f
            if (p.size <= 0f) {
                iterator.remove()
            } else {
                particlePaint.color = p.color
                canvas.drawCircle(p.x, p.y, p.size, particlePaint)
            }
        }
    }

    fun triggerDeath(x: Float, y: Float, themeName: String) {
        particles.clear()
        val theme = ThemeManager.getTheme(themeName)
        val pColor = Color.parseColor(theme.ballColor)
        
        for (i in 0 until 15) {
            val angle = Math.random() * 2 * Math.PI
            val speed = (2f + Math.random() * 6f).toFloat()
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (Math.cos(angle) * speed).toFloat(),
                    vy = (Math.sin(angle) * speed).toFloat(),
                    size = (4f + Math.random() * 6f).toFloat(),
                    color = pColor
                )
            )
        }
    }

    fun triggerCheckpointAnimation(x: Float, y: Float, themeName: String) {
        val theme = ThemeManager.getTheme(themeName)
        val pColor = Color.parseColor(theme.checkpointColor)
        for (i in 0 until 12) {
            val angle = Math.random() * 2 * Math.PI
            val speed = (1f + Math.random() * 3f).toFloat()
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (Math.cos(angle) * speed).toFloat(),
                    vy = (Math.sin(angle) * speed).toFloat(),
                    size = (4f + Math.random() * 4f).toFloat(),
                    color = pColor
                )
            )
        }
    }

    fun triggerSwitchAnimation(x: Float, y: Float, themeName: String) {
        val theme = ThemeManager.getTheme(themeName)
        val pColor = Color.parseColor(theme.switchColor)
        for (i in 0 until 8) {
            val angle = Math.random() * 2 * Math.PI
            val speed = (1f + Math.random() * 2.5f).toFloat()
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (Math.cos(angle) * speed).toFloat(),
                    vy = (Math.sin(angle) * speed).toFloat(),
                    size = (3f + Math.random() * 3f).toFloat(),
                    color = pColor
                )
            )
        }
    }

    fun triggerBossDamageParticles(x: Float, y: Float) {
        val pColor = Color.parseColor("#FF1744")
        for (i in 0 until 15) {
            val angle = Math.random() * 2 * Math.PI
            val speed = (2f + Math.random() * 5f).toFloat()
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (Math.cos(angle) * speed).toFloat(),
                    vy = (Math.sin(angle) * speed).toFloat(),
                    size = (4f + Math.random() * 5f).toFloat(),
                    color = pColor
                )
            )
        }
    }

    fun triggerBossDefeatParticles(x: Float, y: Float) {
        val colors = listOf(Color.parseColor("#FF1744"), Color.parseColor("#FFD600"), Color.WHITE)
        for (i in 0 until 40) {
            val angle = Math.random() * 2 * Math.PI
            val speed = (3f + Math.random() * 8f).toFloat()
            val pColor = colors.random()
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (Math.cos(angle) * speed).toFloat(),
                    vy = (Math.sin(angle) * speed).toFloat(),
                    size = (5f + Math.random() * 8f).toFloat(),
                    color = pColor
                )
            )
        }
    }

    fun triggerBossTrailParticles(x: Float, y: Float, phaseIntensity: Float, width: Float, height: Float) {
        val count = 1 + (phaseIntensity * 4).toInt()
        val color = Color.parseColor("#FF1744")
        for (i in 0 until count) {
            val angle = Math.random() * 2 * Math.PI
            val speed = (0.5f + Math.random() * 1.5f).toFloat()
            val offsetX = (Math.random() - 0.5) * width * 0.8
            val offsetY = (Math.random() - 0.5) * height * 0.8
            particles.add(
                Particle(
                    x = x + offsetX.toFloat(),
                    y = y + offsetY.toFloat(),
                    vx = (Math.cos(angle) * speed).toFloat(),
                    vy = (Math.sin(angle) * speed).toFloat(),
                    size = (3f + Math.random() * 3f).toFloat() + (phaseIntensity * 4f),
                    color = color
                )
            )
        }
    }

    fun triggerPortalTeleportParticles(x: Float, y: Float, themeName: String) {
        val pColor = Color.parseColor("#4facfe")
        for (i in 0 until 15) {
            val angle = Math.random() * 2 * Math.PI
            val speed = (2f + Math.random() * 5f).toFloat()
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (Math.cos(angle) * speed).toFloat(),
                    vy = (Math.sin(angle) * speed).toFloat(),
                    size = (4f + Math.random() * 5f).toFloat(),
                    color = pColor
                )
            )
        }
    }
}
