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
    private var lastFrameTimeNanos = 0L
    
    // Entidades procesadas para la pantalla actual

    // Animación de muerte

    val gameState = GameState()

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
    private var physicalLevelWidth = 1920f
    private var physicalLevelHeight = 1080f
    
    // El Bus de Señales Global
    private var transitionProgress = 0f

    private var currentLevel: Level? = null

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
            
            var targetCamX = gameState.posX - width / 2f
            var targetCamY = gameState.posY - height / 2f
            
            gameState.activeBoss?.let { boss ->
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
            gameState.activeBoss?.let { boss ->
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
                canvas.scale(scale, scale, gameState.posX, gameState.posY)
            }
            EnvironmentRenderer.draw(
                canvas, theme, scaleX, scaleY, gameState.walls, gameState.hazards, gameState.movingEntities, gameState.spinningHazards,
                gameState.goal, gameState.gates, gameState.logicGates, gameState.switches, gameState.checkpoints, gameState.boxes, gameState.windZones, gameState.speedPads, gameState.portals, gameState.timers,
                gameState.activeCheckpointIndex, gameState.signalBus, wallPaint, hazardPaint, goalPaint, gatePaint, switchPaint, checkpointPaint
            )

            if (gameState.isExploding) {
                drawExplosion(canvas)
            } else if (!gameState.isLevelCompleted) {
                PlayerRenderer.draw(
                    canvas = canvas,
                    posX = gameState.posX,
                    posY = gameState.posY,
                    radius = gameState.radius,
                    trailPoints = gameState.trailPoints,
                    trailPaint = trailPaint,
                    ballPaint = ballPaint,
                    isLevelCompleted = gameState.isLevelCompleted
                )
            }
            
            // Dibujar Jefe (Boss) en la capa más alta (incluye proyectiles y onda expansiva)
            gameState.activeBoss?.let { boss ->
                BossRenderer.draw(canvas, boss, theme, gameState.switches, gameState.bossProjectiles)
            }
            
            // 5. Dibujar partículas activas (efectos de checkpoint/switch/boss)
            ParticleManager.updateAndDraw(canvas)
            
            canvas.restore()
            
            if (transitionProgress < 1f) {
                transitionProgress = Math.min(1f, transitionProgress + 0.03f)
            }
            UIRenderer.draw(canvas, width, height, gameState.activeBoss, theme, gameState.isLevelCompleted, transitionProgress)

            updatePhysics()
        }
        
        // Continuously invalidate since background renderer uses postInvalidateOnAnimation internally
        postInvalidateOnAnimation()
    }

    private fun drawExplosion(canvas: Canvas) {
        val elapsed = System.currentTimeMillis() - gameState.explosionTime
        val progress = elapsed.toFloat() / gameState.explosionDuration
        
        if (progress >= 1f) {
            gameState.isExploding = false
            if (gameState.activeCheckpointIndex == -1) {
                onLevelFailed?.invoke()
                if (gameState.currentLevelId == 999 && currentDebugPath != null) {
                    loadDebugLevel(currentDebugPath!!, showTransition = false)
                } else {
                    loadLevel(gameState.currentLevelId, showTransition = false)
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
        if (gameState.isExploding || gameState.isLevelCompleted) return

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
        gameState.activeBoss?.let { boss ->
            if (boss.entranceProgress < 1f) {
                boss.entranceProgress += 0.005f * dt
                if (boss.entranceProgress >= 1f) boss.entranceProgress = 1f
                return // Pausa las físicas mientras el jefe hace su entrada
            }
        }

        // --- 2. RESOLUCIÓN DE SEÑALES GLOBALES (UNIVERSAL SIGNAL BUS) ---
        SignalEvaluator.evaluate(
            dt, gameState.posX, gameState.posY, gameState.radius, gameState.switches, gameState.boxes, gameState.logicGates, gameState.timers, gameState.signalBus
        ) { cx, cy ->
            ParticleManager.triggerSwitchAnimation(cx, cy, currentLevel?.theme ?: "industrial")
        }

        // --- 3. INTEGRACIÓN FÍSICA VIA PHYSICS ENGINE ---
        val state = PhysicsState(
            posX = gameState.posX,
            posY = gameState.posY,
            velX = gameState.velX,
            velY = gameState.velY,
            radius = gameState.radius,
            tiltX = gameState.tiltX,
            tiltY = gameState.tiltY,
            physicalLevelWidth = physicalLevelWidth,
            physicalLevelHeight = physicalLevelHeight,
            EDITOR_WIDTH = EDITOR_WIDTH.toFloat(),
            EDITOR_HEIGHT = EDITOR_HEIGHT.toFloat(),
            activeCheckpointIndex = gameState.activeCheckpointIndex,
            activeCheckpointX = gameState.activeCheckpointX,
            activeCheckpointY = gameState.activeCheckpointY,
            themeName = currentLevel?.theme ?: "industrial",
            isExploding = gameState.isExploding,
            activeBoss = gameState.activeBoss,
            goal = gameState.goal,
            checkpoints = gameState.checkpoints,
            signalBus = gameState.signalBus,
            onTriggerDeath = { triggerDeath() },
            onLevelComplete = { 
                gameState.isLevelCompleted = true
                gameState.velX = 0f
                gameState.velY = 0f
                this@GameView.onLevelComplete?.invoke() 
            },
            onCheckpointActivated = { saveCheckpointState() },
            onTriggerCheckpointAnimation = { cx, cy ->
                ParticleManager.triggerCheckpointAnimation(cx, cy, currentLevel?.theme ?: "industrial")
            },
            onTriggerSwitchAnimation = { cx, cy ->
                ParticleManager.triggerSwitchAnimation(cx, cy, currentLevel?.theme ?: "industrial")
            },
            portalCooldown = gameState.portalCooldown
        )

        PhysicsEngine.update(
            state, dt, gameState.walls, gameState.hazards, gameState.gates, gameState.switches, gameState.boxes, gameState.logicGates,
            gameState.movingEntities, gameState.spinningHazards, gameState.windZones, gameState.speedPads, gameState.portals, gameState.bossProjectiles, gameState.trailPoints
        )

        // Copiar valores de vuelta
        gameState.posX = state.posX
        gameState.posY = state.posY
        gameState.velX = state.velX
        gameState.velY = state.velY
        gameState.activeCheckpointIndex = state.activeCheckpointIndex
        gameState.activeCheckpointX = state.activeCheckpointX
        gameState.activeCheckpointY = state.activeCheckpointY
        gameState.activeBoss = state.activeBoss
        gameState.portalCooldown = state.portalCooldown
    }

    private fun saveCheckpointState() {
        // Enfoque Tipo 1: Snapshot de elementos clave y de progreso permanente
        for (box in gameState.boxes) {
            box.checkpointRect.set(box.rect)
        }
        for (sw in gameState.switches) {
            if (sw.switchMode == "toggle") {
                sw.checkpointToggleActive = sw.isToggleActive
            }
        }
        gameState.activeBoss?.let { boss ->
            boss.checkpointHealth = boss.health
            boss.checkpointPhase = boss.currentPhase
        }
    }

    private fun triggerDeath() {
        gameState.isExploding = true
        gameState.explosionTime = System.currentTimeMillis()
        gameState.explosionX = gameState.posX
        gameState.explosionY = gameState.posY
        gameState.velX = 0f
        gameState.velY = 0f

        // Generar partículas de explosión basadas en el tema actual
        ParticleManager.triggerDeath(gameState.posX, gameState.posY, currentLevel?.theme ?: "industrial")
    }

    private fun resetBall(isDeathRespawn: Boolean = false) {
        gameState.posX = gameState.activeCheckpointX ?: gameState.startX
        gameState.posY = gameState.activeCheckpointY ?: gameState.startY
        gameState.velX = 0f
        gameState.velY = 0f
        gameState.trailPoints.clear()
        lastFrameTimeNanos = 0L // Restablecer temporizador de física

        // Reiniciar puzle al morir para mantener el desafío pero respetar gameState.checkpoints
        gameState.activeSwitches.clear()
        gameState.signalBus.clear()
        for ((index, sw) in gameState.switches.withIndex()) {
            if (sw.switchMode == "toggle") {
                sw.isToggleActive = sw.checkpointToggleActive
                if (sw.isToggleActive) {
                    gameState.activeSwitches.add(index)
                }
            } else {
                sw.isToggleActive = false
            }
            sw.wasTouchedLastFrame = false
            sw.isHoldActive = false
            sw.visualTimer = 0f
        }
        for (gate in gameState.gates) {
            gate.isGateOpen = false
            gate.openTimer = 0f
        }

        // Reiniciar posiciones de las cajas móviles
        // Reiniciar posiciones de las cajas móviles al último checkpoint
        for (box in gameState.boxes) {
            box.rect.set(box.checkpointRect)
            box.vx = 0f
            box.vy = 0f
        }

        // Reiniciar jefe desde el checkpoint
        gameState.activeBoss?.let { boss ->
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
        gameState.bossProjectiles.clear()
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
        
        gameState.radius = 20f * Math.min(scaleX, scaleY)

        gameState.walls.clear()
        gameState.hazards.clear()
        gameState.checkpoints.clear()
        gameState.switches.clear()
        gameState.gates.clear()
        gameState.movingEntities.clear()
        gameState.spinningHazards.clear()
        gameState.logicGates.clear()
        gameState.boxes.clear()
        gameState.windZones.clear()
        gameState.speedPads.clear()
        gameState.timers.clear()
        gameState.portals.clear()
        gameState.portalCooldown = 0f
        gameState.activeBoss = null
        gameState.bossProjectiles.clear()
        gameState.goal = RectF()
        gameState.startX = 0f
        gameState.startY = 0f

        for (raw in level.rawEntities) {
            val rx = raw.x * scaleX
            val ry = raw.y * scaleY
            val rw = raw.w * scaleX
            val rh = raw.h * scaleY

            when (raw.type) {
                "wall" -> gameState.walls.add(RectF(rx, ry, rx + rw, ry + rh))
                "hazard" -> gameState.hazards.add(RectF(rx, ry, rx + rw, ry + rh))
                "checkpoint" -> gameState.checkpoints.add(CheckpointData(RectF(rx, ry, rx + rw, ry + rh), raw.checkpointIndex ?: 0))
                "switch" -> {
                    val rawMode = raw.switchMode ?: "toggle"
                    val mode = when (rawMode) {
                        "pressure" -> "hold"
                        "trigger" -> "latch"
                        else -> rawMode
                    }
                    gameState.switches.add(
                        SwitchData(
                            RectF(rx, ry, rx + rw, ry + rh),
                            raw.linkId ?: raw.outputLinkId ?: "",
                            mode
                        )
                    )
                }
                "gate" -> gameState.gates.add(
                    GateData(
                        RectF(rx, ry, rx + rw, ry + rh),
                        raw.linkId ?: "",
                        false,
                        raw.duration
                    )
                )
                "box" -> gameState.boxes.add(
                    BoxData(
                        rect = RectF(rx, ry, rx + rw, ry + rh)
                    )
                )
                "logic_gate" -> {
                    val inputs = raw.inputLinkIds?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                    gameState.logicGates.add(
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
                    gameState.movingEntities.add(
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
                    gameState.movingEntities.add(
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
                    gameState.spinningHazards.add(
                        SpinningHazardData(cxVal, cyVal, radiusVal, raw.linkId?.takeIf { it.isNotBlank() })
                    )
                }
                "wind_zone" -> {
                    val fx = raw.dx ?: 0f
                    val fy = raw.dy ?: 0f
                    gameState.windZones.add(
                        WindZoneData(RectF(rx, ry, rx + rw, ry + rh), fx, fy)
                    )
                }
                "speed_pad" -> {
                    val bx = raw.dx ?: 0f
                    val by = raw.dy ?: 0f
                    gameState.speedPads.add(
                        SpeedPadData(RectF(rx, ry, rx + rw, ry + rh), bx, by)
                    )
                }
                "boss" -> {
                    gameState.activeBoss = BossData(
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
                    gameState.timers.add(
                        TimerData(
                            rect = RectF(rx, ry, rx + rw, ry + rh),
                            inputLinkId = raw.linkId ?: raw.inputLinkIds ?: "",
                            outputLinkId = raw.outputLinkId ?: "",
                            duration = raw.duration ?: 2f
                        )
                    )
                }
                "portal" -> {
                    gameState.portals.add(
                        PortalData(
                            rect = RectF(rx, ry, rx + rw, ry + rh),
                            portalId = raw.portalId ?: raw.checkpointIndex ?: 0
                        )
                    )
                }
                "goal" -> gameState.goal = RectF(rx, ry, rx + rw, ry + rh)
                "start" -> {
                    gameState.startX = rx + rw / 2f
                    gameState.startY = ry + rh / 2f
                }
            }
        }
        resetBall()
    }

    fun loadLevel(levelId: Int, showTransition: Boolean = true) {
        gameState.currentLevelId = levelId
        gameState.isExploding = false
        gameState.isLevelCompleted = false
        gameState.activeCheckpointIndex = -1
        gameState.activeCheckpointX = null
        gameState.activeCheckpointY = null
        gameState.activeSwitches.clear()
        gameState.activeBoss = null
        gameState.bossProjectiles.clear()

        val level = LevelManager.loadLevel(context, levelId)
        if (level != null) {
            currentLevel = level
            val theme = level.theme ?: "industrial"
            applyTheme(theme)
            processLevelScaling()
            gameState.activeBoss?.entranceProgress = if (showTransition) 0f else 1f
            bgRenderer.initBgParticles(theme)
            transitionProgress = if (showTransition) 0f else 1f
            invalidate()
        }
    }

    var currentDebugPath: String? = null

    fun loadDebugLevel(path: String, showTransition: Boolean = true) {
        currentDebugPath = path
        gameState.currentLevelId = 999
        gameState.isExploding = false
        gameState.isLevelCompleted = false
        gameState.activeCheckpointIndex = -1
        gameState.activeCheckpointX = null
        gameState.activeCheckpointY = null
        gameState.activeSwitches.clear()
        gameState.activeBoss = null
        gameState.bossProjectiles.clear()
        
        val level = LevelManager.loadLevelFromPath(context, path)
        if (level != null) {
            level.id = 999
            currentLevel = level
            val theme = level.theme ?: "industrial"
            applyTheme(theme)
            processLevelScaling()
            gameState.activeBoss?.entranceProgress = if (showTransition) 0f else 1f
            bgRenderer.initBgParticles(theme)
            transitionProgress = if (showTransition) 0f else 1f
            invalidate()
        }
    }
}
