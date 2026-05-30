package com.example.juegodepelotaconsensores.rendering

import android.graphics.*
import com.example.juegodepelotaconsensores.models.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object EnvironmentRenderer {

    fun getProceduralColor(baseHex: String, linkId: String?): Int {
        val baseColor = Color.parseColor(baseHex)
        if (linkId.isNullOrEmpty()) return baseColor
        
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)
        
        val hash = Math.abs(linkId.hashCode())
        val hueShift = (hash % 12) * 30f
        hsv[0] = (hsv[0] + hueShift) % 360f
        
        return Color.HSVToColor(hsv)
    }

    fun draw(
        canvas: Canvas,
        theme: GameTheme,
        scaleX: Float,
        scaleY: Float,
        walls: List<RectF>,
        hazards: List<RectF>,
        movingEntities: List<MovingEntityData>,
        spinningHazards: List<SpinningHazardData>,
        goal: RectF,
        gates: List<GateData>,
        logicGates: List<LogicGateData>,
        switches: List<SwitchData>,
        checkpoints: List<CheckpointData>,
        boxes: List<BoxData>,
        windZones: List<WindZoneData>,
        speedPads: List<SpeedPadData>,
        portals: List<PortalData>,
        timers: List<TimerData>,
        activeCheckpointIndex: Int,
        signalBus: Map<String, Boolean>,
        wallPaint: Paint,
        hazardPaint: Paint,
        goalPaint: Paint,
        gatePaint: Paint,
        switchPaint: Paint,
        checkpointPaint: Paint
    ) {
        // 1. Dibujar el nivel (Muros y peligros planos y neutros para máxima lectura)
        for (wall in walls) canvas.drawRect(wall, wallPaint)
        for (hazard in hazards) canvas.drawRect(hazard, hazardPaint)

        // Dibujar Muros y Peligros Móviles
        for (me in movingEntities) {
            val basePaint = if (me.isHazard) hazardPaint else wallPaint
            canvas.drawRect(me.currentRect, basePaint)
            
            // Bordes
            val borderPaint = Paint().apply {
                color = if (me.isHazard) Color.parseColor("#FFA726") else Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 2f
                alpha = if (me.isHazard) 130 else 90
                isAntiAlias = true
            }
            canvas.drawRect(me.currentRect, borderPaint)
            
            // Patrón de rayas interiores
            val stripePaint = Paint().apply {
                color = if (me.isHazard) Color.BLACK else Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
                alpha = if (me.isHazard) 50 else 40
                isAntiAlias = true
            }
            val path = Path()
            val spacing = 12f
            val w = me.currentRect.width()
            val h = me.currentRect.height()
            val left = me.currentRect.left
            val top = me.currentRect.top
            
            var offset = -h
            while (offset < w) {
                val xStart = left + Math.max(0f, offset)
                val yStart = top + Math.max(0f, -offset)
                val xEnd = left + Math.min(w, offset + h)
                val yEnd = top + Math.min(h, -offset + w)
                if (xStart < xEnd && yStart < yEnd) {
                    path.moveTo(xStart, yStart)
                    path.lineTo(xEnd, yEnd)
                }
                offset += spacing
            }
            canvas.drawPath(path, stripePaint)
        }

        // Dibujar Sierras Giratorias
        val sawTimeAngle = (System.currentTimeMillis() / 150.0) % (Math.PI * 2)
        val teethCount = 12
        for (saw in spinningHazards) {
            val path = Path()
            for (j in 0 until teethCount * 2) {
                val angle = (j.toDouble() / teethCount) * Math.PI + sawTimeAngle
                val dist = if (j % 2 == 0) saw.radius else saw.radius * 0.72f
                val tx = (saw.cx + cos(angle) * dist).toFloat()
                val ty = (saw.cy + sin(angle) * dist).toFloat()
                if (j == 0) path.moveTo(tx, ty) else path.lineTo(tx, ty)
            }
            path.close()
            
            val sawPaint = Paint().apply {
                color = Color.parseColor(theme.hazardColor)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawPath(path, sawPaint)
            
            val centerPaint = Paint().apply {
                color = Color.parseColor("#121212")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(saw.cx, saw.cy, saw.radius * 0.25f, centerPaint)
            
            centerPaint.apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            canvas.drawCircle(saw.cx, saw.cy, saw.radius * 0.25f, centerPaint)
        }

        // Dibujar la Meta
        val goalPulse = 0.85f + 0.15f * sin(System.currentTimeMillis() / 200.0).toFloat()
        goalPaint.setShadowLayer(25f * goalPulse, 0f, 0f, Color.parseColor(theme.goalColor))
        canvas.drawRect(goal, goalPaint)
        
        val innerGoalPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * goalPulse
            alpha = (200 * goalPulse).toInt().coerceIn(0, 255)
        }
        val innerRect = RectF(goal.left + 6f, goal.top + 6f, goal.right - 6f, goal.bottom - 6f)
        canvas.drawRect(innerRect, innerGoalPaint)

        // Dibujar Compuertas/Gates
        for (gate in gates) {
            val gateColor = getProceduralColor(theme.gateColor, gate.inputLinkId)
            gatePaint.color = gateColor
            
            if (gate.isGateOpen) {
                gatePaint.style = Paint.Style.STROKE
                gatePaint.strokeWidth = 3f
                gatePaint.alpha = 60
                canvas.drawRect(gate.rect, gatePaint)
            } else {
                gatePaint.style = Paint.Style.FILL
                gatePaint.alpha = 220
                canvas.drawRect(gate.rect, gatePaint)
                
                gatePaint.style = Paint.Style.STROKE
                gatePaint.strokeWidth = 2.5f
                gatePaint.alpha = 150
                canvas.drawRect(gate.rect, gatePaint)
                
                val stripePaint = Paint().apply {
                    color = gateColor
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f
                    alpha = 100
                }
                val spacing = 15f
                var offset = -gate.rect.height()
                while (offset < gate.rect.width()) {
                    val xStart = gate.rect.left + Math.max(0f, offset)
                    val yStart = gate.rect.top + Math.max(0f, -offset)
                    val xEnd = gate.rect.left + Math.min(gate.rect.width(), offset + gate.rect.height())
                    val yEnd = gate.rect.top + Math.min(gate.rect.height(), -offset + gate.rect.width())
                    if (xStart < xEnd && yStart < yEnd) {
                        canvas.drawLine(xStart, yStart, xEnd, yEnd, stripePaint)
                    }
                    offset += spacing
                }
                
                if (gate.inputLinkId.isNotBlank()) {
                    val cx = gate.rect.centerX()
                    val cy = gate.rect.centerY()
                    val badgeRadius = 15f
                    
                    val badgePaint = Paint().apply {
                        color = Color.parseColor("#121212")
                        style = Paint.Style.FILL
                    }
                    canvas.drawCircle(cx, cy, badgeRadius, badgePaint)
                    
                    badgePaint.apply {
                        color = gateColor
                        style = Paint.Style.STROKE
                        strokeWidth = 2f
                    }
                    canvas.drawCircle(cx, cy, badgeRadius, badgePaint)
                    
                    val textPaint = Paint().apply {
                        color = gateColor
                        textSize = 14f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    val yPos = cy - (textPaint.descent() + textPaint.ascent()) / 2f
                    canvas.drawText(gate.inputLinkId, cx, yPos, textPaint)
                }
            }
        }

        // Dibujar Logic Gates (Microchips)
        for (lg in logicGates) {
            LogicGateRenderer.draw(canvas, lg, theme, signalBus)
        }

        // Dibujar Interruptores/Switches
        for (i in switches.indices) {
            val sw = switches[i]
            val isPressed = (signalBus[sw.outputLinkId] == true) || sw.isHoldActive || sw.visualTimer > 0f
            
            val switchColor = getProceduralColor(theme.switchColor, sw.outputLinkId)
            switchPaint.color = switchColor
            
            if (isPressed) {
                switchPaint.style = Paint.Style.FILL
                switchPaint.alpha = 255
                canvas.drawRect(sw.rect, switchPaint)
            } else {
                switchPaint.style = Paint.Style.FILL
                switchPaint.alpha = 60
                canvas.drawRect(sw.rect, switchPaint)
                
                switchPaint.style = Paint.Style.STROKE
                switchPaint.strokeWidth = 3f
                switchPaint.alpha = 200
                canvas.drawRect(sw.rect, switchPaint)
            }
            
            val cx = sw.rect.centerX()
            val cy = sw.rect.centerY()
            val labelText = if (sw.outputLinkId.isNotBlank()) sw.outputLinkId else "SW"
            
            val textPaint = Paint().apply {
                color = if (isPressed) Color.parseColor("#121212") else switchColor
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val yPos = cy - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(labelText, cx, yPos, textPaint)
        }

        // Dibujar Checkpoints
        val cpPulse = 0.8f + 0.2f * sin(System.currentTimeMillis() / 250.0).toFloat()
        val currentActiveCP = checkpoints.getOrNull(activeCheckpointIndex)
        val activeLogicalIndex = currentActiveCP?.index ?: -1
        
        for (i in checkpoints.indices) {
            val cp = checkpoints[i]
            val isActive = i == activeCheckpointIndex
            val isDeactivated = activeLogicalIndex != -1 && cp.index < activeLogicalIndex
            
            val baseColor = if (isDeactivated) {
                Color.parseColor("#424242")
            } else {
                Color.parseColor(theme.checkpointColor)
            }
            checkpointPaint.color = baseColor
            
            if (isActive) {
                checkpointPaint.style = Paint.Style.FILL
                checkpointPaint.alpha = (180 * cpPulse).toInt().coerceIn(0, 255)
                canvas.drawRect(cp.rect, checkpointPaint)
                
                checkpointPaint.style = Paint.Style.STROKE
                checkpointPaint.strokeWidth = 5f * cpPulse
                checkpointPaint.alpha = 255
                canvas.drawRect(cp.rect, checkpointPaint)
            } else if (isDeactivated) {
                checkpointPaint.style = Paint.Style.FILL
                checkpointPaint.alpha = 40
                canvas.drawRect(cp.rect, checkpointPaint)
                
                checkpointPaint.style = Paint.Style.STROKE
                checkpointPaint.strokeWidth = 2f
                checkpointPaint.alpha = 100
                canvas.drawRect(cp.rect, checkpointPaint)
            } else {
                checkpointPaint.style = Paint.Style.FILL
                checkpointPaint.alpha = (50 * cpPulse).toInt().coerceIn(0, 255)
                canvas.drawRect(cp.rect, checkpointPaint)
                
                checkpointPaint.style = Paint.Style.STROKE
                checkpointPaint.strokeWidth = 2.5f * cpPulse
                checkpointPaint.alpha = (120 * cpPulse).toInt().coerceIn(0, 255)
                canvas.drawRect(cp.rect, checkpointPaint)
            }

            val cx = cp.rect.centerX()
            val cy = cp.rect.centerY()
            
            val labelText = "✓"
            val textPaint = Paint().apply {
                color = baseColor
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                if (isDeactivated) {
                    alpha = 100
                }
            }
            val yPos = cy - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(labelText, cx, yPos, textPaint)
        }

        // Dibujar Cajas Móviles (Boxes)
        for (box in boxes) {
            val boxColor = Color.parseColor(theme.boxColor)
            val boxBg = Color.parseColor(theme.boxBg)
            
            val boxPaint = Paint().apply {
                isAntiAlias = true
            }
            
            boxPaint.color = boxBg
            boxPaint.style = Paint.Style.FILL
            boxPaint.alpha = 240
            canvas.drawRect(box.rect, boxPaint)
            
            boxPaint.color = boxColor
            boxPaint.style = Paint.Style.STROKE
            boxPaint.strokeWidth = 2.5f
            boxPaint.alpha = 255
            canvas.drawRect(box.rect, boxPaint)
            
            boxPaint.strokeWidth = 1.5f
            boxPaint.alpha = 150
            canvas.drawLine(box.rect.left + 5f, box.rect.top + 5f, box.rect.right - 5f, box.rect.bottom - 5f, boxPaint)
            canvas.drawLine(box.rect.right - 5f, box.rect.top + 5f, box.rect.left + 5f, box.rect.bottom - 5f, boxPaint)
            
            val innerSize = Math.min(box.rect.width(), box.rect.height()) * 0.4f
            val cx = box.rect.centerX()
            val cy = box.rect.centerY()
            boxPaint.style = Paint.Style.STROKE
            boxPaint.strokeWidth = 1f
            boxPaint.alpha = 120
            canvas.drawRect(cx - innerSize/2, cy - innerSize/2, cx + innerSize/2, cy + innerSize/2, boxPaint)
        }

        // Dibujar Zonas de Viento
        val windPaint = Paint().apply {
            color = Color.parseColor(theme.windColor)
            isAntiAlias = true
        }
        val timeOffset = (System.currentTimeMillis() % 2000) / 2000f
        for (wz in windZones) {
            windPaint.style = Paint.Style.FILL
            windPaint.alpha = 15
            canvas.drawRect(wz.rect, windPaint)
            
            windPaint.style = Paint.Style.STROKE
            windPaint.strokeWidth = 1.5f
            windPaint.alpha = 60
            val pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
            windPaint.pathEffect = pathEffect
            canvas.drawRect(wz.rect, windPaint)
            windPaint.pathEffect = null
            
            windPaint.style = Paint.Style.STROKE
            windPaint.strokeWidth = 2f
            
            val forceLength = sqrt((wz.forceX * wz.forceX + wz.forceY * wz.forceY).toDouble()).toFloat()
            if (forceLength > 0f) {
                val dirX = wz.forceX / forceLength
                val dirY = wz.forceY / forceLength
                val rectW = wz.rect.width()
                val rectH = wz.rect.height()
                val lineCount = ((rectW * rectH) / 15000f).toInt().coerceIn(3, 10)
                
                for (k in 0 until lineCount) {
                    val seedX = (wz.rect.left.toInt() * (k + 1)) % 100 / 100f
                    val seedY = (wz.rect.top.toInt() * (k + 3)) % 100 / 100f
                    val startPosX = wz.rect.left + seedX * rectW
                    val startPosY = wz.rect.top + seedY * rectH
                    val travelDistance = 80f
                    val currentTravel = (timeOffset + k / lineCount.toFloat()) % 1.0f
                    val offsetX = dirX * currentTravel * travelDistance
                    val offsetY = dirY * currentTravel * travelDistance
                    
                    val lx1 = startPosX + offsetX
                    val ly1 = startPosY + offsetY
                    val lx2 = lx1 + dirX * 25f
                    val ly2 = ly1 + dirY * 25f
                    
                    if (wz.rect.contains(lx1, ly1) && wz.rect.contains(lx2, ly2)) {
                        val alphaFade = (sin(currentTravel * Math.PI) * 100).toInt().coerceIn(0, 255)
                        windPaint.alpha = alphaFade
                        canvas.drawLine(lx1, ly1, lx2, ly2, windPaint)
                    }
                }
            }
        }

        // Dibujar Speed Pads
        val speedPadPaint = Paint().apply {
            color = Color.parseColor(theme.speedPadColor)
            isAntiAlias = true
        }
        val padTimeOffset = (System.currentTimeMillis() % 1000) / 1000f
        for (sp in speedPads) {
            speedPadPaint.style = Paint.Style.FILL
            speedPadPaint.alpha = 25
            canvas.drawRect(sp.rect, speedPadPaint)
            
            speedPadPaint.style = Paint.Style.STROKE
            speedPadPaint.strokeWidth = 2f
            speedPadPaint.alpha = 100
            canvas.drawRect(sp.rect, speedPadPaint)
            
            speedPadPaint.style = Paint.Style.STROKE
            speedPadPaint.strokeWidth = 3f
            speedPadPaint.strokeCap = Paint.Cap.ROUND
            
            val boostLen = sqrt((sp.boostX * sp.boostX + sp.boostY * sp.boostY).toDouble()).toFloat()
            if (boostLen > 0f) {
                val dirX = sp.boostX / boostLen
                val dirY = sp.boostY / boostLen
                val perpX = -dirY
                val perpY = dirX
                val rectW = sp.rect.width()
                val rectH = sp.rect.height()
                
                for (step in 0 until 3) {
                    val progress = (padTimeOffset + step / 3f) % 1.0f
                    val cx = sp.rect.left + rectW * 0.5f + dirX * (progress - 0.5f) * rectW * 0.8f
                    val cy = sp.rect.top + rectH * 0.5f + dirY * (progress - 0.5f) * rectH * 0.8f
                    val size = Math.min(rectW, rectH) * 0.25f
                    
                    val tipX = cx + dirX * size
                    val tipY = cy + dirY * size
                    val wing1X = cx - dirX * size * 0.5f + perpX * size
                    val wing1Y = cy - dirY * size * 0.5f + perpY * size
                    val wing2X = cx - dirX * size * 0.5f - perpX * size
                    val wing2Y = cy - dirY * size * 0.5f - perpY * size
                    
                    if (sp.rect.contains(tipX, tipY) && sp.rect.contains(wing1X, wing1Y) && sp.rect.contains(wing2X, wing2Y)) {
                        val arrowAlpha = (sin(progress * Math.PI) * 180).toInt().coerceIn(0, 255)
                        speedPadPaint.alpha = arrowAlpha
                        canvas.drawLine(wing1X, wing1Y, tipX, tipY, speedPadPaint)
                        canvas.drawLine(wing2X, wing2Y, tipX, tipY, speedPadPaint)
                    }
                }
            }
        }

        // 9. Dibujar Portales
        for (portal in portals) {
            val cx = portal.rect.centerX()
            val cy = portal.rect.centerY()
            val w = portal.rect.width()
            val h = portal.rect.height()
            val baseRadius = Math.min(w, h) / 2f
            
            val pulse = 1f + 0.1f * sin(System.currentTimeMillis() / 200.0 + portal.portalId * 1.5).toFloat()
            val radiusVal = baseRadius * pulse
            
            val portalPaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f * scaleX
                isAntiAlias = true
                color = Color.parseColor("#4facfe")
                setShadowLayer(15f, 0f, 0f, Color.parseColor("#4facfe"))
            }
            
            canvas.drawCircle(cx, cy, radiusVal, portalPaint)
            
            val innerPaint = Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = true
                color = Color.argb(80, 0, 242, 254)
            }
            canvas.drawCircle(cx, cy, radiusVal * 0.7f, innerPaint)

            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 14f * Math.min(scaleX, scaleY)
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(portal.portalId.toString(), cx, cy + 5f * scaleY, textPaint)
        }

        // 10. Dibujar Temporizadores
        for (timer in timers) {
            val bgPaint = Paint().apply {
                color = Color.parseColor("#424242")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRect(timer.rect, bgPaint)

            if (timer.isCounting && timer.duration > 0f) {
                val progress = timer.timeLeft / timer.duration
                val fillPaint = Paint().apply {
                    color = Color.parseColor("#FFB74D")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                val fillWidth = timer.rect.width() * progress
                canvas.drawRect(timer.rect.left, timer.rect.top, timer.rect.left + fillWidth, timer.rect.bottom, fillPaint)
            } else if (timer.hasFinished) {
                val finishPaint = Paint().apply {
                    color = Color.parseColor("#81C784")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawRect(timer.rect, finishPaint)
            }

            val borderPaint = Paint().apply {
                color = Color.parseColor("#FFFFFF")
                style = Paint.Style.STROKE
                strokeWidth = 2f * scaleX
                isAntiAlias = true
            }
            canvas.drawRect(timer.rect, borderPaint)

            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 12f * Math.min(scaleX, scaleY)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val label = if (timer.isCounting) String.format("%.1fs", timer.timeLeft) else if (timer.hasFinished) "ON" else "TIMER"
            canvas.drawText(label, timer.rect.centerX(), timer.rect.centerY() + 4f * scaleY, textPaint)
        }
    }
}
