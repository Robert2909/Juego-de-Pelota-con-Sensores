package com.example.juegodepelotaconsensores.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.juegodepelotaconsensores.models.BgParticle

class BackgroundRenderer(private val editorWidth: Float, private val editorHeight: Float) {
    private val bgParticles = mutableListOf<BgParticle>()
    private val gradPaint = Paint()
    private var lastFrameTimeNanos = 0L

    fun initBgParticles(theme: String) {
        bgParticles.clear()
        val random = java.util.Random()
        val numParticles = 25
        for (i in 0 until numParticles) {
            bgParticles.add(createRandomBgParticle(random, theme, randomY = true))
        }
    }

    private fun createRandomBgParticle(random: java.util.Random, theme: String, randomY: Boolean): BgParticle {
        val px = random.nextFloat() * editorWidth
        val py = if (randomY) random.nextFloat() * editorHeight else editorHeight + 20f
        
        var vx = (random.nextFloat() - 0.5f) * 0.3f
        var vy = -0.1f - random.nextFloat() * 0.2f
        var size = 3f + random.nextFloat() * 6f
        var maxAlpha = 15 + random.nextInt(25)
        
        when (theme) {
            "volcano" -> {
                vx = (random.nextFloat() - 0.5f) * 0.4f
                vy = -0.3f - random.nextFloat() * 0.5f
                size = 2f + random.nextFloat() * 5f
                maxAlpha = 20 + random.nextInt(30)
            }
            "neon" -> {
                vx = -0.4f - random.nextFloat() * 0.4f
                vy = (random.nextFloat() - 0.5f) * 0.1f
                size = 2f + random.nextFloat() * 4f
                maxAlpha = 12 + random.nextInt(20)
            }
            "forest" -> {
                vx = (random.nextFloat() - 0.5f) * 0.4f
                vy = (random.nextFloat() - 0.5f) * 0.4f
                size = 3f + random.nextFloat() * 6f
                maxAlpha = 25 + random.nextInt(30)
            }
            "industrial" -> {
                vx = (random.nextFloat() - 0.5f) * 0.15f
                vy = 0.1f + random.nextFloat() * 0.2f
                size = 2f + random.nextFloat() * 5f
                maxAlpha = 10 + random.nextInt(15)
            }
        }
        
        return BgParticle(
            x = px,
            y = py,
            vx = vx,
            vy = vy,
            size = size,
            alpha = if (randomY) random.nextInt(maxAlpha) else 0,
            maxAlpha = maxAlpha,
            fadeSpeed = 0.2f + random.nextFloat() * 0.8f,
            oscSpeed = 0.001f + random.nextFloat() * 0.002f,
            oscRadius = 4f + random.nextFloat() * 12f,
            phase = random.nextFloat() * 3.14f * 2f
        )
    }

    fun draw(canvas: Canvas, width: Int, height: Int, themeName: String) {
        val activeTheme = ThemeManager.getTheme(themeName)
        
        val time = System.currentTimeMillis()
        val angle = time * 0.00005
        
        val gcx = width / 2f + Math.cos(angle).toFloat() * (width * 0.25f)
        val gcy = height / 2f + Math.sin(angle).toFloat() * (height * 0.25f)
        val radius = Math.max(width, height) * 0.8f
        
        val centerColor = Color.parseColor(activeTheme.blob1Color)
        val outerColor = Color.parseColor(activeTheme.bgColor)
        
        val shader = RadialGradient(gcx, gcy, radius, centerColor, outerColor, Shader.TileMode.CLAMP)
        gradPaint.shader = shader
        
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradPaint)
        
        // Calcular dt
        val currentTimeNanos = System.nanoTime()
        var dt = 1.0f
        if (lastFrameTimeNanos != 0L) {
            val elapsedTimeMs = (currentTimeNanos - lastFrameTimeNanos) / 1_000_000f
            dt = elapsedTimeMs / 16.666f
            if (dt > 2.0f) dt = 2.0f
            if (dt < 0.1f) dt = 0.1f
        }
        lastFrameTimeNanos = currentTimeNanos

        drawAndUpdateBgParticles(canvas, width, height, themeName, dt)
    }

    private fun drawAndUpdateBgParticles(canvas: Canvas, width: Int, height: Int, theme: String, dt: Float) {
        val pColor = when (theme) {
            "volcano" -> Color.parseColor("#FF5722")
            "neon" -> Color.parseColor("#00F2FF")
            "forest" -> Color.parseColor("#C5E1A5")
            else -> Color.parseColor("#22FFFFFF")
        }
        
        val random = java.util.Random()
        val scaleX = width.toFloat() / editorWidth
        val scaleY = height.toFloat() / editorHeight
        val baseScale = Math.min(scaleX, scaleY)
        
        val pPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val iterator = bgParticles.listIterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            
            p.x += p.vx * dt
            p.y += p.vy * dt
            
            val time = System.currentTimeMillis()
            val osc = Math.sin(time * p.oscSpeed.toDouble() + p.phase).toFloat() * p.oscRadius
            
            val currentAlpha = (p.maxAlpha * (0.5f + 0.5f * osc)).toInt().coerceIn(0, 255)
            
            pPaint.color = pColor
            pPaint.alpha = currentAlpha
            
            val drawX = (p.x + osc) * scaleX
            val drawY = p.y * scaleY
            val drawSize = p.size * baseScale
            
            canvas.drawCircle(drawX, drawY, drawSize, pPaint)
            
            val isOutOfBound = when (theme) {
                "volcano" -> p.y < -20f || p.x < -20f || p.x > editorWidth + 20f
                "industrial" -> p.y > editorHeight + 20f || p.x < -20f || p.x > editorWidth + 20f
                "neon" -> p.x < -20f || p.y < -20f || p.y > editorHeight + 20f
                else -> p.y < -20f || p.y > editorHeight + 20f || p.x < -20f || p.x > editorWidth + 20f
            }
            
            if (isOutOfBound) {
                iterator.set(createRandomBgParticle(random, theme, randomY = false))
            }
        }
    }
}
