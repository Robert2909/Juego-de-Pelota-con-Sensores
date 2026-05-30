package com.example.juegodepelotaconsensores

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.juegodepelotaconsensores.engine.*
import com.example.juegodepelotaconsensores.models.*
import com.example.juegodepelotaconsensores.rendering.*
import com.example.juegodepelotaconsensores.rendering.PlayerRenderer
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
            EnvironmentRenderer.draw(
                canvas, theme, scaleX, scaleY, walls, hazards, movingEntities, spinningHazards,
                goal, gates, logicGates, switches, checkpoints, boxes, windZones, speedPads, portals, timers,
                activeCheckpointIndex, signalBus, wallPaint, hazardPaint, goalPaint, gatePaint, switchPaint, checkpointPaint
            )

            if (isExploding) {
                drawExplosion(canvas)
            } else if (!isLevelCompleted) {
                PlayerRenderer.draw(
                    canvas = canvas,
                    posX = posX,
                    posY = posY,
                    radius = radius,
                    trailPoints = trailPoints,
                    trailPaint = trailPaint,
                    ballPaint = ballPaint,
                    isLevelCompleted = isLevelCompleted
                )
            }
            
            // Dibujar Jefe (Boss) en la capa más alta (incluye proyectiles y onda expansiva)
            activeBoss?.let { boss ->
                BossRenderer.draw(canvas, boss, theme, switches, bossProjectiles)
            }
            
            // 5. Dibujar partículas activas (efectos de checkpoint/switch/boss)
            ParticleManager.updateAndDraw(canvas)
            
            canvas.restore()
            
            if (transitionProgress < 1f) {
                transitionProgress = Math.min(1f, transitionProgress + 0.03f)
            }
            UIRenderer.draw(canvas, width, height, activeBoss, theme, isLevelCompleted, transitionProgress)

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
                onLevelFailed?.invoke()
                if (currentLevelId == 999 && currentDebugPath != null) {
                    loadDebugLevel(currentDebugPath!!, showTransition = false)
                } else {
                    loadLevel(currentLevelId, showTransition = false)
                }
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

    var currentDebugPath: String? = null

    fun loadDebugLevel(path: String, showTransition: Boolean = true) {
        currentDebugPath = path
        this.currentLevelId = 999
        isExploding = false
        isLevelCompleted = false
        activeCheckpointIndex = -1
        activeCheckpointX = null
        activeCheckpointY = null
        activeSwitches.clear()
        activeBoss = null
        bossProjectiles.clear()
        
        val level = LevelManager.loadLevelFromPath(context, path)
        if (level != null) {
            level.id = 999
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
