package com.example.juegodepelotaconsensores

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.juegodepelotaconsensores.engine.*
import com.example.juegodepelotaconsensores.models.*
import com.example.juegodepelotaconsensores.rendering.*
import java.util.LinkedList
import java.util.Queue

class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Constantes del Editor para Escalado Absoluto
    private val EDITOR_WIDTH = 1920f
    private val EDITOR_HEIGHT = 1080f

    private val bgRenderer = BackgroundRenderer(EDITOR_WIDTH, EDITOR_HEIGHT)

    init {
        ThemeManager.loadThemesFromJson(context)
    }

    private var activeBgColor = Color.parseColor("#121212")

    private var posX = 0f
    private var posY = 0f
    private var velX = 0f
    private var velY = 0f
    private var radius = 20f
    private var lastFrameTimeNanos = 0L
    
    // Entidades procesadas para la pantalla actual
    private val walls = mutableListOf<RectF>()
    private val hazards = mutableListOf<RectF>()
    private var goal = RectF()
    private var startX = 0f
    private var startY = 0f

    private val trailPoints = mutableListOf<TrailPoint>()

    var tiltX = 0f
    var tiltY = 0f

    // Animación de muerte
    private var isExploding = false
    private var explosionTime = 0L
    private val explosionDuration = 350L
    private var explosionX = 0f
    private var explosionY = 0f

    var onLevelComplete: (() -> Unit)? = null
    var onLevelFailed: (() -> Unit)? = null

    private val ballPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        setShadowLayer(8f, 0f, 0f, Color.argb(100, 255, 255, 255))
    }
    
    private val trailPaint = Paint().apply {
        color = Color.parseColor("#444444")
        isAntiAlias = true
        alpha = 100
    }

    private val wallPaint = Paint().apply {
        color = Color.parseColor("#333333")
        style = Paint.Style.FILL
    }

    private val hazardPaint = Paint().apply {
        color = Color.parseColor("#E57373") // Soft Red
        alpha = 255
    }

    private val goalPaint = Paint().apply {
        color = Color.parseColor("#81C784") // Soft Green
        style = Paint.Style.FILL
        setShadowLayer(15f, 0f, 0f, Color.parseColor("#81C784"))
    }

    private val checkpointPaint = Paint().apply {
        isAntiAlias = true
    }

    private val switchPaint = Paint().apply {
        isAntiAlias = true
    }

    private val gatePaint = Paint().apply {
        isAntiAlias = true
    }

    // Listas y estados de elementos interactivos
    private val checkpoints = mutableListOf<CheckpointData>()
    private var activeCheckpointIndex = -1
    private var activeCheckpointX: Float? = null
    private var activeCheckpointY: Float? = null
    private var physicalLevelWidth = 1920f
    private var physicalLevelHeight = 1080f

    private val switches = mutableListOf<SwitchData>()
    private val activeSwitches = mutableSetOf<Int>()

    private val gates = mutableListOf<GateData>()



    private val movingEntities = mutableListOf<MovingEntityData>()
    private val spinningHazards = mutableListOf<SpinningHazardData>()
    private val logicGates = mutableListOf<LogicGateData>()



    private val boxes = mutableListOf<BoxData>()
    
    // El Bus de Señales Global
    private val signalBus = mutableMapOf<String, Boolean>()

    private val windZones = mutableListOf<WindZoneData>()
    private val speedPads = mutableListOf<SpeedPadData>()
    private val timers = mutableListOf<TimerData>()
    private val portals = mutableListOf<PortalData>()
    private var portalCooldown = 0f
    private var transitionProgress = 0f
    private var activeBoss: BossData? = null
    private val bossProjectiles = mutableListOf<ProjectileData>()

    private var currentLevel: Level? = null
    private var currentLevelId: Int = 1
    private var isLevelCompleted = false

    private fun applyTheme(themeName: String) {
        val theme = ThemeManager.getTheme(themeName)
        activeBgColor = Color.parseColor(theme.bgColor)
        
        wallPaint.color = Color.parseColor(theme.wallColor)
        hazardPaint.color = Color.parseColor(theme.hazardColor)
        
        goalPaint.apply {
            color = Color.parseColor(theme.goalColor)
            setShadowLayer(15f, 0f, 0f, Color.parseColor(theme.goalColor))
        }
        
        ballPaint.apply {
            color = Color.parseColor(theme.ballColor)
            setShadowLayer(8f, 0f, 0f, Color.parseColor(theme.ballColor))
        }
        
        trailPaint.color = Color.parseColor(theme.trailColor)
        
        checkpointPaint.color = Color.parseColor(theme.checkpointColor)
        switchPaint.color = Color.parseColor(theme.switchColor)
        gatePaint.color = Color.parseColor(theme.gateColor)
    }

    private fun getProceduralColor(baseHex: String, linkId: String?): Int {
        val baseColor = Color.parseColor(baseHex)
        if (linkId.isNullOrEmpty()) return baseColor
        
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)
        
        val hash = Math.abs(linkId.hashCode())
        val hueShift = (hash % 12) * 30f
        hsv[0] = (hsv[0] + hueShift) % 360f
        
        return Color.HSVToColor(hsv)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val themeName = currentLevel?.theme ?: "industrial"
        val theme = ThemeManager.getTheme(themeName)
        bgRenderer.draw(canvas, width, height, themeName)

        currentLevel?.let { level ->
            val scaleX = width.toFloat() / EDITOR_WIDTH
            val scaleY = height.toFloat() / EDITOR_HEIGHT
            val pWidth = level.width * scaleX
            val pHeight = level.height * scaleY
            
            var targetCamX = posX - width / 2f
            var targetCamY = posY - height / 2f
            
            activeBoss?.let { boss ->
                if (boss.isDefeated) {
                    val now = System.currentTimeMillis()
                    val timeSinceDefeat = now - boss.lastDamageTime
                    val t = (timeSinceDefeat / 1500f).coerceIn(0f, 1f)
                    val smoothT = t * t * (3 - 2 * t)
                    
                    val bossCamX = boss.rect.centerX() - width / 2f
                    val bossCamY = boss.rect.centerY() - height / 2f
                    
                    targetCamX = targetCamX + (bossCamX - targetCamX) * smoothT
                    targetCamY = targetCamY + (bossCamY - targetCamY) * smoothT
                } else if (boss.entranceProgress < 1f) {
                    val p = boss.entranceProgress
                    val t = when {
                        p < 0.15f -> p / 0.15f // pan to boss
                        p < 0.85f -> 1f // stay on boss
                        else -> 1f - ((p - 0.85f) / 0.15f) // pan back
                    }
                    val smoothT = t * t * (3 - 2 * t)
                    
                    val bossCamX = boss.baseRect.centerX() - width / 2f
                    val bossCamY = boss.baseRect.centerY() - height / 2f
                    
                    targetCamX = targetCamX + (bossCamX - targetCamX) * smoothT
                    targetCamY = targetCamY + (bossCamY - targetCamY) * smoothT
                }
            }
            
            val camX = targetCamX.coerceIn(0f, Math.max(0f, pWidth - width))
            val camY = targetCamY.coerceIn(0f, Math.max(0f, pHeight - height))
            
            canvas.save()
            canvas.translate(-camX, -camY)
            
            // Screen shake dramático
            activeBoss?.let { boss ->
                if (boss.isDefeated) {
                    val now = System.currentTimeMillis()
                    val timeSinceDefeat = now - boss.lastDamageTime
                    if (timeSinceDefeat > 2000) {
                        val animTime = timeSinceDefeat - 2000
                        // Tiembla durante la explosión (3000 a 5500 ms)
                        if (animTime > 3000 && animTime < 5500) {
                            val intensity = ((5500 - animTime) / 2500f) * 60f
                            val shakeX = (Math.random().toFloat() - 0.5f) * intensity
                            val shakeY = (Math.random().toFloat() - 0.5f) * intensity
                            canvas.translate(shakeX, shakeY)
                        }
                    }
                } else if (boss.entranceProgress > 0.6f && boss.entranceProgress <= 0.85f) {
                    val roarProgress = (boss.entranceProgress - 0.6f) / 0.25f
                    // Shake is strongest at the beginning and dies down
                    val intensity = 50f * (1f - roarProgress)
                    val shakeX = (Math.random().toFloat() - 0.5f) * intensity
                    val shakeY = (Math.random().toFloat() - 0.5f) * intensity
                    canvas.translate(shakeX, shakeY)
                }
            }
            
            if (transitionProgress < 1f) {
                val scale = 0.95f + 0.05f * transitionProgress
                canvas.scale(scale, scale, posX, posY)
            }
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
                
                // Patrón de rayas interiores (para coherencia visual)
                val stripePaint = Paint().apply {
                    color = if (me.isHazard) Color.BLACK else Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f
                    alpha = if (me.isHazard) 50 else 40
                    isAntiAlias = true
                }
                val path = android.graphics.Path()
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
                    val tx = (saw.cx + Math.cos(angle) * dist).toFloat()
                    val ty = (saw.cy + Math.sin(angle) * dist).toFloat()
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

            // Dibujar la Meta (con su pulso llamativo que rompe con la lógica de color del tema)
            val goalPulse = 0.85f + 0.15f * Math.sin(System.currentTimeMillis() / 200.0).toFloat()
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
                    // Compuerta Abierta: Faint outline
                    gatePaint.style = Paint.Style.STROKE
                    gatePaint.strokeWidth = 3f
                    gatePaint.alpha = 60
                    canvas.drawRect(gate.rect, gatePaint)
                } else {
                    // Compuerta Cerrada: Solid warning design with procedural color
                    gatePaint.style = Paint.Style.FILL
                    gatePaint.alpha = 220
                    canvas.drawRect(gate.rect, gatePaint)
                    
                    gatePaint.style = Paint.Style.STROKE
                    gatePaint.strokeWidth = 2.5f
                    gatePaint.alpha = 150
                    canvas.drawRect(gate.rect, gatePaint)
                    
                    // Warning striped design
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
                    
                    // Si tiene inputLinkId, dibujar una chapa circular central con la etiqueta
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
                
                // Dibujar etiqueta en el centro (linkId o SW)
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
            val cpPulse = 0.8f + 0.2f * Math.sin(System.currentTimeMillis() / 250.0).toFloat()
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
                    
                    // Pulsing active border
                    checkpointPaint.style = Paint.Style.STROKE
                    checkpointPaint.strokeWidth = 5f * cpPulse
                    checkpointPaint.alpha = 255
                    canvas.drawRect(cp.rect, checkpointPaint)
                } else if (isDeactivated) {
                    // Estado Desactivado: Estático, plano y gris tenue
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
                
                // Dibujar etiqueta de checkmark ✓ sin el índice y sin círculo
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
                
                // Rellenar fondo del bloque móvil
                boxPaint.color = boxBg
                boxPaint.style = Paint.Style.FILL
                boxPaint.alpha = 240
                canvas.drawRect(box.rect, boxPaint)
                
                // Borde exterior temático
                boxPaint.color = boxColor
                boxPaint.style = Paint.Style.STROKE
                boxPaint.strokeWidth = 2.5f
                boxPaint.alpha = 255
                canvas.drawRect(box.rect, boxPaint)
                
                // Cruces de refuerzo interiores (Diseño en X)
                boxPaint.strokeWidth = 1.5f
                boxPaint.alpha = 150
                canvas.drawLine(box.rect.left + 5f, box.rect.top + 5f, box.rect.right - 5f, box.rect.bottom - 5f, boxPaint)
                canvas.drawLine(box.rect.right - 5f, box.rect.top + 5f, box.rect.left + 5f, box.rect.bottom - 5f, boxPaint)
                
                // Marco interior decorativo
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
                
                val forceLength = Math.sqrt((wz.forceX * wz.forceX + wz.forceY * wz.forceY).toDouble()).toFloat()
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
                            val alphaFade = (Math.sin(currentTravel * Math.PI) * 100).toInt().coerceIn(0, 255)
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
                
                val boostLen = Math.sqrt((sp.boostX * sp.boostX + sp.boostY * sp.boostY).toDouble()).toFloat()
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
                            val arrowAlpha = (Math.sin(progress * Math.PI) * 180).toInt().coerceIn(0, 255)
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
                
                val pulse = 1f + 0.1f * Math.sin(System.currentTimeMillis() / 200.0 + portal.portalId * 1.5).toFloat()
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

            if (isExploding) {
                drawExplosion(canvas)
            } else if (!isLevelCompleted) {
                // 3. Dibujar estela ultra-fluida (Cinta continua tapered)
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

                // 4. Dibujar pelota
                canvas.drawCircle(posX, posY, radius, ballPaint)
            }
            
            // Dibujar Proyectiles del Jefe
            val projPaint = Paint().apply {
                color = Color.parseColor(theme.bossLaserColor)
                style = Paint.Style.FILL
                isAntiAlias = true
                setShadowLayer(10f, 0f, 0f, Color.parseColor(theme.bossLaserColor))
            }
            for (proj in bossProjectiles) {
                canvas.drawCircle(proj.x, proj.y, proj.radius, projPaint)
                val innerPaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawCircle(proj.x, proj.y, proj.radius * 0.4f, innerPaint)
            }

            // Dibujar Jefe (Boss) en la capa más alta
            activeBoss?.let { boss ->
                BossRenderer.draw(canvas, boss, theme, switches)
            }
            
            // 5. Dibujar partículas activas (efectos de checkpoint/switch/boss)
            ParticleManager.updateAndDraw(canvas)
            
            // Onda expansiva en mundo
            activeBoss?.let { boss ->
                if (boss.isDefeated) {
                    val now = System.currentTimeMillis()
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
            
            canvas.restore()
            
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
                transitionProgress = Math.min(1f, transitionProgress + 0.03f)
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

            updatePhysics()
        }
        
        // Continuously invalidate since background renderer uses postInvalidateOnAnimation internally
        postInvalidateOnAnimation()
    }

    private fun drawExplosion(canvas: Canvas) {
        val elapsed = System.currentTimeMillis() - explosionTime
        val progress = elapsed.toFloat() / explosionDuration
        
        if (progress >= 1f) {
            isExploding = false
            if (activeCheckpointIndex == -1) {
                loadLevel(currentLevelId, showTransition = false)
            } else {
                resetBall(isDeathRespawn = true)
            }
            return
        }

        // Calcular dt para la animación de las partículas
        val currentTimeNanos = System.nanoTime()
        var dt = 1.0f
        if (lastFrameTimeNanos != 0L) {
            val elapsedTimeMs = (currentTimeNanos - lastFrameTimeNanos) / 1_000_000f
            dt = elapsedTimeMs / 16.666f
            if (dt > 2.0f) dt = 2.0f
            if (dt < 0.1f) dt = 0.1f
        }
        lastFrameTimeNanos = currentTimeNanos

        val explosionPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f * (1f - progress)
            isAntiAlias = true
        }
        
        val effectiveFriction = Math.pow(0.95, dt.toDouble()).toFloat()
        
        for (p in ParticleManager.particles) {
            p.x += p.vx * dt
            p.y += p.vy * dt
            
            p.vy += 0.2f * dt // Gravedad sutil
            p.vx *= effectiveFriction // Fricción
            
            explosionPaint.color = p.color
            explosionPaint.alpha = (255 * (1f - progress)).toInt()
            
            canvas.drawCircle(p.x, p.y, p.size * (1f - progress), explosionPaint)
        }
    }

    private fun updatePhysics() {
        if (isExploding || isLevelCompleted) return

        // 1. Calcular delta time para independencia de framerate
        val currentTimeNanos = System.nanoTime()
        if (lastFrameTimeNanos == 0L) {
            lastFrameTimeNanos = currentTimeNanos
            return
        }
        val elapsedTimeMs = (currentTimeNanos - lastFrameTimeNanos) / 1_000_000f
        lastFrameTimeNanos = currentTimeNanos

        // Delta normalizado a 60 FPS (16.666 ms)
        var dt = elapsedTimeMs / 16.666f
        // Clampeado para evitar saltos bruscos por caídas de frames o pausas
        if (dt > 2.0f) dt = 2.0f
        if (dt < 0.1f) dt = 0.1f

        // Animar la entrada del jefe
        activeBoss?.let { boss ->
            if (boss.entranceProgress < 1f) {
                boss.entranceProgress += 0.005f * dt
                if (boss.entranceProgress >= 1f) boss.entranceProgress = 1f
                return // Pausa las físicas mientras el jefe hace su entrada
            }
        }

        // --- 2. RESOLUCIÓN DE SEÑALES GLOBALES (UNIVERSAL SIGNAL BUS) ---
        SignalEvaluator.evaluate(
            dt, posX, posY, radius, switches, boxes, logicGates, timers, signalBus
        ) { cx, cy ->
            ParticleManager.triggerSwitchAnimation(cx, cy, currentLevel?.theme ?: "industrial")
        }

        // --- 3. INTEGRACIÓN FÍSICA VIA PHYSICS ENGINE ---
        val state = PhysicsState(
            posX = posX,
            posY = posY,
            velX = velX,
            velY = velY,
            radius = radius,
            tiltX = tiltX,
            tiltY = tiltY,
            physicalLevelWidth = physicalLevelWidth,
            physicalLevelHeight = physicalLevelHeight,
            EDITOR_WIDTH = EDITOR_WIDTH.toFloat(),
            EDITOR_HEIGHT = EDITOR_HEIGHT.toFloat(),
            activeCheckpointIndex = activeCheckpointIndex,
            activeCheckpointX = activeCheckpointX,
            activeCheckpointY = activeCheckpointY,
            themeName = currentLevel?.theme ?: "industrial",
            isExploding = isExploding,
            activeBoss = activeBoss,
            goal = goal,
            checkpoints = checkpoints,
            signalBus = signalBus,
            onTriggerDeath = { triggerDeath() },
            onLevelComplete = { 
                this@GameView.isLevelCompleted = true
                this@GameView.velX = 0f
                this@GameView.velY = 0f
                this@GameView.onLevelComplete?.invoke() 
            },
            onCheckpointActivated = { saveCheckpointState() },
            onTriggerCheckpointAnimation = { cx, cy ->
                ParticleManager.triggerCheckpointAnimation(cx, cy, currentLevel?.theme ?: "industrial")
            },
            onTriggerSwitchAnimation = { cx, cy ->
                ParticleManager.triggerSwitchAnimation(cx, cy, currentLevel?.theme ?: "industrial")
            },
            portalCooldown = portalCooldown
        )

        PhysicsEngine.update(
            state, dt, walls, hazards, gates, switches, boxes, logicGates,
            movingEntities, spinningHazards, windZones, speedPads, portals, bossProjectiles, trailPoints
        )

        // Copiar valores de vuelta
        posX = state.posX
        posY = state.posY
        velX = state.velX
        velY = state.velY
        activeCheckpointIndex = state.activeCheckpointIndex
        activeCheckpointX = state.activeCheckpointX
        activeCheckpointY = state.activeCheckpointY
        activeBoss = state.activeBoss
        portalCooldown = state.portalCooldown
    }

    private fun saveCheckpointState() {
        // Enfoque Tipo 1: Snapshot de elementos clave y de progreso permanente
        for (box in boxes) {
            box.checkpointRect.set(box.rect)
        }
        for (sw in switches) {
            if (sw.switchMode == "toggle") {
                sw.checkpointToggleActive = sw.isToggleActive
            }
        }
        activeBoss?.let { boss ->
            boss.checkpointHealth = boss.health
            boss.checkpointPhase = boss.currentPhase
        }
    }

    private fun triggerDeath() {
        isExploding = true
        explosionTime = System.currentTimeMillis()
        explosionX = posX
        explosionY = posY
        velX = 0f
        velY = 0f

        // Generar partículas de explosión basadas en el tema actual
        ParticleManager.triggerDeath(posX, posY, currentLevel?.theme ?: "industrial")
    }

    private fun resetBall(isDeathRespawn: Boolean = false) {
        posX = activeCheckpointX ?: startX
        posY = activeCheckpointY ?: startY
        velX = 0f
        velY = 0f
        trailPoints.clear()
        lastFrameTimeNanos = 0L // Restablecer temporizador de física

        // Reiniciar puzle al morir para mantener el desafío pero respetar checkpoints
        activeSwitches.clear()
        signalBus.clear()
        for ((index, sw) in switches.withIndex()) {
            if (sw.switchMode == "toggle") {
                sw.isToggleActive = sw.checkpointToggleActive
                if (sw.isToggleActive) {
                    activeSwitches.add(index)
                }
            } else {
                sw.isToggleActive = false
            }
            sw.wasTouchedLastFrame = false
            sw.isHoldActive = false
            sw.visualTimer = 0f
        }
        for (gate in gates) {
            gate.isGateOpen = false
            gate.openTimer = 0f
        }

        // Reiniciar posiciones de las cajas móviles
        // Reiniciar posiciones de las cajas móviles al último checkpoint
        for (box in boxes) {
            box.rect.set(box.checkpointRect)
            box.vx = 0f
            box.vy = 0f
        }

        // Reiniciar jefe desde el checkpoint
        activeBoss?.let { boss ->
            boss.health = boss.checkpointHealth
            boss.currentPhase = boss.checkpointPhase
            boss.currentSpeed = boss.baseSpeed
            boss.currentAttackDensity = boss.baseAttackDensity
            boss.currentAttackFrequency = boss.baseAttackFrequency
            boss.normalAttackCount = 0
            boss.isDefeated = false
            boss.hasExploded = false
            boss.hasStartedAnimation = false
            boss.floatOffset = 0f
            boss.rect.set(boss.baseRect)
            if (isDeathRespawn) {
                boss.entranceProgress = 1f
            }
            boss.visualHealth = boss.health.toFloat()
        }
        bossProjectiles.clear()
    }




    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        processLevelScaling()
    }

    private fun processLevelScaling() {
        val level = currentLevel ?: return
        if (width == 0 || height == 0) return

        val scaleX = width.toFloat() / EDITOR_WIDTH
        val scaleY = height.toFloat() / EDITOR_HEIGHT
        
        physicalLevelWidth = level.width * scaleX
        physicalLevelHeight = level.height * scaleY
        
        radius = 20f * Math.min(scaleX, scaleY)

        walls.clear()
        hazards.clear()
        checkpoints.clear()
        switches.clear()
        gates.clear()
        movingEntities.clear()
        spinningHazards.clear()
        logicGates.clear()
        boxes.clear()
        windZones.clear()
        speedPads.clear()
        timers.clear()
        portals.clear()
        portalCooldown = 0f
        activeBoss = null
        bossProjectiles.clear()
        goal = RectF()
        startX = 0f
        startY = 0f

        for (raw in level.rawEntities) {
            val rx = raw.x * scaleX
            val ry = raw.y * scaleY
            val rw = raw.w * scaleX
            val rh = raw.h * scaleY

            when (raw.type) {
                "wall" -> walls.add(RectF(rx, ry, rx + rw, ry + rh))
                "hazard" -> hazards.add(RectF(rx, ry, rx + rw, ry + rh))
                "checkpoint" -> checkpoints.add(CheckpointData(RectF(rx, ry, rx + rw, ry + rh), raw.checkpointIndex ?: 0))
                "switch" -> {
                    val rawMode = raw.switchMode ?: "toggle"
                    val mode = when (rawMode) {
                        "pressure" -> "hold"
                        "trigger" -> "latch"
                        else -> rawMode
                    }
                    switches.add(
                        SwitchData(
                            RectF(rx, ry, rx + rw, ry + rh),
                            raw.linkId ?: raw.outputLinkId ?: "",
                            mode
                        )
                    )
                }
                "gate" -> gates.add(
                    GateData(
                        RectF(rx, ry, rx + rw, ry + rh),
                        raw.linkId ?: "",
                        false,
                        raw.duration
                    )
                )
                "box" -> boxes.add(
                    BoxData(
                        rect = RectF(rx, ry, rx + rw, ry + rh)
                    )
                )
                "logic_gate" -> {
                    val inputs = raw.inputLinkIds?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                    logicGates.add(
                        LogicGateData(
                            RectF(rx, ry, rx + rw, ry + rh),
                            raw.gateType ?: "AND",
                            inputs,
                            raw.outputLinkId?.trim() ?: ""
                        )
                    )
                }
                "moving_wall" -> {
                    val dxScaled = (raw.dx ?: 0f) * scaleX
                    val dyScaled = (raw.dy ?: 0f) * scaleY
                    movingEntities.add(
                        MovingEntityData(
                            RectF(rx, ry, rx + rw, ry + rh),
                            dxScaled,
                            dyScaled,
                            raw.speed ?: 2f,
                            isHazard = false,
                            inputLinkId = raw.linkId?.takeIf { it.isNotBlank() }
                        )
                    )
                }
                "moving_hazard" -> {
                    val dxScaled = (raw.dx ?: 0f) * scaleX
                    val dyScaled = (raw.dy ?: 0f) * scaleY
                    movingEntities.add(
                        MovingEntityData(
                            RectF(rx, ry, rx + rw, ry + rh),
                            dxScaled,
                            dyScaled,
                            raw.speed ?: 2f,
                            isHazard = true,
                            inputLinkId = raw.linkId?.takeIf { it.isNotBlank() }
                        )
                    )
                }
                "spinning_hazard" -> {
                    val radiusVal = Math.min(rw, rh) / 2f
                    val cxVal = rx + rw / 2f
                    val cyVal = ry + rh / 2f
                    spinningHazards.add(
                        SpinningHazardData(cxVal, cyVal, radiusVal, raw.linkId?.takeIf { it.isNotBlank() })
                    )
                }
                "wind_zone" -> {
                    val fx = raw.dx ?: 0f
                    val fy = raw.dy ?: 0f
                    windZones.add(
                        WindZoneData(RectF(rx, ry, rx + rw, ry + rh), fx, fy)
                    )
                }
                "speed_pad" -> {
                    val bx = raw.dx ?: 0f
                    val by = raw.dy ?: 0f
                    speedPads.add(
                        SpeedPadData(RectF(rx, ry, rx + rw, ry + rh), bx, by)
                    )
                }
                "boss" -> {
                    activeBoss = BossData(
                        rect = RectF(rx, ry, rx + rw, ry + rh),
                        baseRect = RectF(rx, ry, rx + rw, ry + rh),
                        inputLinkId = raw.linkId ?: "",
                        name = raw.name ?: "Jefe Épico",
                        health = raw.health ?: 5,
                        maxHealth = raw.health ?: 5,
                        visualHealth = raw.health?.toFloat() ?: 5f,
                        phases = raw.phases ?: 2,
                        currentPhase = 1,
                        checkpointHealth = raw.health ?: 5,
                        checkpointPhase = 1,
                        bossType = raw.bossType ?: "scatter",
                        baseSpeed = raw.speed ?: 150f,
                        currentSpeed = raw.speed ?: 150f,
                        baseAttackDensity = raw.attackDensity ?: 3,
                        currentAttackDensity = raw.attackDensity ?: 3,
                        baseAttackFrequency = raw.attackFrequency ?: 2.0f,
                        currentAttackFrequency = raw.attackFrequency ?: 2.0f,
                        specialAttackFrequency = raw.specialAttackFrequency ?: 3,
                        entranceProgress = 0f
                    )
                }
                "timer" -> {
                    timers.add(
                        TimerData(
                            rect = RectF(rx, ry, rx + rw, ry + rh),
                            inputLinkId = raw.linkId ?: raw.inputLinkIds ?: "",
                            outputLinkId = raw.outputLinkId ?: "",
                            duration = raw.duration ?: 2f
                        )
                    )
                }
                "portal" -> {
                    portals.add(
                        PortalData(
                            rect = RectF(rx, ry, rx + rw, ry + rh),
                            portalId = raw.portalId ?: raw.checkpointIndex ?: 0
                        )
                    )
                }
                "goal" -> goal = RectF(rx, ry, rx + rw, ry + rh)
                "start" -> {
                    startX = rx + rw / 2f
                    startY = ry + rh / 2f
                }
            }
        }
        resetBall()
    }

    fun loadLevel(levelId: Int, showTransition: Boolean = true) {
        this.currentLevelId = levelId
        isExploding = false
        isLevelCompleted = false
        activeCheckpointIndex = -1
        activeCheckpointX = null
        activeCheckpointY = null
        activeSwitches.clear()
        activeBoss = null
        bossProjectiles.clear()

        val level = LevelManager.loadLevel(context, levelId)
        if (level != null) {
            currentLevel = level
            val theme = level.theme ?: "industrial"
            applyTheme(theme)
            processLevelScaling()
            activeBoss?.entranceProgress = if (showTransition) 0f else 1f
            bgRenderer.initBgParticles(theme)
            transitionProgress = if (showTransition) 0f else 1f
            invalidate()
        }
    }

}
