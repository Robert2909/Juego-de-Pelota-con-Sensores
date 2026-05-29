package com.example.juegodepelotaconsensores.engine

import android.graphics.RectF
import com.example.juegodepelotaconsensores.models.*

class PhysicsState(
    var posX: Float,
    var posY: Float,
    var velX: Float,
    var velY: Float,
    val radius: Float,
    val tiltX: Float,
    val tiltY: Float,
    val physicalLevelWidth: Float,
    val physicalLevelHeight: Float,
    val EDITOR_WIDTH: Float,
    val EDITOR_HEIGHT: Float,
    var activeCheckpointIndex: Int,
    var activeCheckpointX: Float?,
    var activeCheckpointY: Float?,
    val themeName: String,
    val isExploding: Boolean,
    var activeBoss: BossData?,
    val goal: RectF,
    val checkpoints: List<CheckpointData>,
    val signalBus: Map<String, Boolean>,
    val onTriggerDeath: () -> Unit,
    val onLevelComplete: () -> Unit,
    val onCheckpointActivated: () -> Unit,
    val onTriggerCheckpointAnimation: (x: Float, y: Float) -> Unit,
    val onTriggerSwitchAnimation: (x: Float, y: Float) -> Unit,
    var portalCooldown: Float = 0f
)

object PhysicsEngine {

    fun update(
        state: PhysicsState,
        dt: Float,
        walls: List<RectF>,
        hazards: List<RectF>,
        gates: List<GateData>,
        switches: List<SwitchData>,
        boxes: List<BoxData>,
        logicGates: List<LogicGateData>,
        movingEntities: List<MovingEntityData>,
        spinningHazards: List<SpinningHazardData>,
        windZones: List<WindZoneData>,
        speedPads: List<SpeedPadData>,
        portals: List<PortalData>,
        bossProjectiles: MutableList<ProjectileData>,
        trailPoints: MutableList<TrailPoint>
    ) {
        if (state.isExploding) return

        val boss = state.activeBoss
        if (boss != null && boss.isDefeated) {
            val now = System.currentTimeMillis()
            val timeSinceDefeat = now - boss.lastDamageTime
            
            if (timeSinceDefeat > 2000) {
                val animationTime = timeSinceDefeat - 2000
                
                if (!boss.hasStartedAnimation) {
                    boss.hasStartedAnimation = true
                    ParticleManager.triggerBossDamageParticles(boss.rect.centerX(), boss.rect.centerY())
                }
                
                // Efecto de temblor dinámico e intenso después de que la cámara llega (y se estabiliza)
                val intensity = (animationTime / 3000f).coerceIn(0f, 1f) * 30f
                boss.floatOffset = (Math.random().toFloat() - 0.5f) * intensity
                
                if (animationTime > 3000 && !boss.hasExploded) {
                    boss.hasExploded = true
                    ParticleManager.triggerBossDefeatParticles(boss.rect.centerX(), boss.rect.centerY())
                    // Onda expansiva inicial
                    ParticleManager.triggerBossDefeatParticles(boss.rect.centerX(), boss.rect.centerY())
                }
                
                // Explosiones secundarias durante el destello blanco
                if (animationTime > 3500 && animationTime < 4500 && Math.random() < 0.1) {
                    val rx = boss.rect.centerX() + (Math.random() - 0.5f) * 150f
                    val ry = boss.rect.centerY() + (Math.random() - 0.5f) * 150f
                    ParticleManager.triggerBossDefeatParticles(rx.toFloat(), ry.toFloat())
                }
                
                if (animationTime > 5500) {
                    state.onLevelComplete()
                    return
                }
            } else {
                boss.floatOffset = 0f
            }
            
            // Durante la animación, evitamos chequeos adicionales de victoria por meta normal
            return
        }

        // C. Actuadores: Actualizar temporizadores de puertas
        for (gate in gates) {
            if (gate.inputLinkId.isNotBlank()) {
                val signalActive = state.signalBus[gate.inputLinkId] == true
                
                // Determinar si la señal es de tipo pulso (proviene de un interruptor en modo pulse)
                var isPulseSignal = false
                for (sw in switches) {
                    if (sw.outputLinkId == gate.inputLinkId && sw.switchMode == "pulse") {
                        isPulseSignal = true
                        break
                    }
                }
                
                if (isPulseSignal) {
                    val targetDuration = if (gate.duration != null && gate.duration > 0f) gate.duration else 2.0f
                    if (signalActive) {
                        gate.openTimer = targetDuration
                        gate.isGateOpen = true
                    } else {
                        if (gate.openTimer > 0f) {
                            gate.openTimer -= dt / 60f
                            gate.isGateOpen = gate.openTimer > 0f
                        } else {
                            gate.isGateOpen = false
                        }
                    }
                } else {
                    gate.isGateOpen = signalActive
                    gate.openTimer = 0f
                }
            }
        }

        // 3. Actualizar posiciones de obstáculos móviles en tiempo real (con pausa vía SignalBus)
        for (me in movingEntities) {
            val oldLeft = me.currentRect.left
            val oldTop = me.currentRect.top

            // Avanza el tiempo interno solo si no tiene señal asignada o si su señal está activa
            if (me.inputLinkId.isNullOrBlank() || state.signalBus[me.inputLinkId] == true) {
                me.internalTimeMs += dt * 16.666f
            }

            val period = if (me.speed != null && me.speed > 0f) me.speed else 2f
            val cycleMs = period * 1000.0
            val angle = ((me.internalTimeMs % cycleMs) / cycleMs) * 2.0 * Math.PI
            val factor = (0.5f - 0.5f * Math.cos(angle)).toFloat()
            val dxMove = me.dx * factor
            val dyMove = me.dy * factor
            me.currentRect.set(
                me.baseRect.left + dxMove,
                me.baseRect.top + dyMove,
                me.baseRect.right + dxMove,
                me.baseRect.bottom + dyMove
            )

            me.vx = me.currentRect.left - oldLeft
            me.vy = me.currentRect.top - oldTop
        }

        // Obstáculos móviles empujan cajas
        for (me in movingEntities) {
            for (box in boxes) {
                if (RectF.intersects(me.currentRect, box.rect)) {
                    val testRect = RectF(box.rect)
                    testRect.offset(me.vx, me.vy)
                    if (!isBoxColliding(box, testRect, walls, gates, boxes,  state.physicalLevelWidth, state.physicalLevelHeight)) {
                        box.rect.set(testRect)
                    }
                }
             }
        }

        // 4. Escalar fuerza de inclinación por escala de diseño y dt
        // Se usa el radio de la bola para obtener la escala pura de la pantalla (independiente del tamaño del nivel)
        val designScale = state.radius / 20f
        val baseSpeed = 0.8f
        val speedMultiplier = baseSpeed * designScale

        var appliedForceX = state.tiltX * speedMultiplier
        var appliedForceY = state.tiltY * speedMultiplier
        
        // Si el jefe está derrotado, el jugador pierde el control y se detiene completamente
        if (state.activeBoss?.isDefeated == true) {
            appliedForceX = 0f
            appliedForceY = 0f
            state.velX = 0f
            state.velY = 0f
        }

        // Zonas de Viento / Gravedad
        val currentBallRect = RectF(
            state.posX - state.radius,
            state.posY - state.radius,
            state.posX + state.radius,
            state.posY + state.radius
        )
        for (wz in windZones) {
            if (RectF.intersects(wz.rect, currentBallRect)) {
                appliedForceX += wz.forceX * designScale
                appliedForceY += wz.forceY * designScale
            }
        }

        state.velX += appliedForceX * dt
        state.velY += appliedForceY * dt

        // Speed Pads logic
        for (sp in speedPads) {
            if (RectF.intersects(sp.rect, currentBallRect)) {
                if (sp.boostX != 0f) {
                    state.velX = sp.boostX * designScale
                }
                if (sp.boostY != 0f) {
                    state.velY = sp.boostY * designScale
                }
            }
        }

        // Lógica del Jefe (Boss)
        state.activeBoss?.let { boss ->
            if (!boss.isDefeated) {
                // 1. Calcular Fase y Multiplicadores Absolutos
                val phaseFactor = boss.health.toFloat() / boss.maxHealth.toFloat() // 1.0 to 0.0
                val phaseSize = 1.0f / boss.phases.toFloat()
                var newPhase = boss.phases - Math.ceil((phaseFactor / phaseSize).toDouble()).toInt() + 1
                newPhase = newPhase.coerceIn(1, boss.phases)
                
                boss.currentPhase = newPhase
                
                val f = if (boss.phases > 1) (boss.currentPhase - 1).toFloat() / (boss.phases - 1).toFloat() else 0f
                val multiplier = 1f + f // De 1x (fase inicial) a 2x (fase final)
                
                boss.currentSpeed = boss.baseSpeed * multiplier
                boss.currentAttackDensity = Math.round(boss.baseAttackDensity * multiplier)
                val currentAttackCooldown = (boss.baseAttackFrequency / multiplier) * 1000L
                val currentSpecialReq = Math.max(1, Math.round(boss.specialAttackFrequency / multiplier))

                // 2. Lógica de Movimiento
                boss.moveCycleTime += dt / 60f
                val bx = boss.rect.centerX()
                val by = boss.rect.centerY()
                
                when (boss.bossType) {
                    "scatter" -> {
                        // Dispersor Caótico: se mueve erráticamente a puntos aleatorios
                        if (Math.abs(bx - boss.targetX) < 10f && Math.abs(by - boss.targetY) < 10f || boss.targetX == 0f) {
                            boss.targetX = bx + (Math.random().toFloat() - 0.5f) * 600f
                            boss.targetY = by + (Math.random().toFloat() - 0.5f) * 600f
                            boss.targetX = boss.targetX.coerceIn(100f, state.physicalLevelWidth - 100f)
                            boss.targetY = boss.targetY.coerceIn(100f, state.physicalLevelHeight - 100f)
                        }
                    }
                    "tracker" -> {
                        // Cazador Preciso: persigue al jugador constantemente
                        boss.targetX = state.posX
                        boss.targetY = state.posY
                    }
                    "spinner" -> {
                        // Hélice Mortal: se mueve en un patrón circular suavizado alrededor del centro (o persiguiendo suavemente)
                        val cycle = boss.moveCycleTime * 0.5f
                        boss.targetX = state.physicalLevelWidth / 2f + Math.cos(cycle.toDouble()).toFloat() * 400f
                        boss.targetY = state.physicalLevelHeight / 2f + Math.sin((cycle * 1.5).toDouble()).toFloat() * 300f
                    }
                }
                
                // Aplicar movimiento
                val mdx = boss.targetX - bx
                val mdy = boss.targetY - by
                val mdist = Math.max(1f, Math.sqrt((mdx*mdx + mdy*mdy).toDouble()).toFloat())
                val moveAmount = boss.currentSpeed * designScale * (dt / 60f)
                
                if (mdist > moveAmount) {
                    boss.rect.offset((mdx / mdist) * moveAmount, (mdy / mdist) * moveAmount)
                } else {
                    boss.rect.offset(mdx, mdy)
                }

                val phaseIntensity = if (boss.phases > 1) (boss.currentPhase - 1).toFloat() / (boss.phases - 1).toFloat() else 0f
                if (boss.entranceProgress == 1f && Math.random() < 0.2 + (0.3 * phaseIntensity)) {
                    ParticleManager.triggerBossTrailParticles(
                        boss.rect.centerX(), 
                        boss.rect.centerY(), 
                        phaseIntensity, 
                        boss.rect.width(), 
                        boss.rect.height()
                    )
                }

                // Efecto visual flotante
                val floatY = 15f * Math.sin(System.currentTimeMillis() / 400.0).toFloat()
                boss.floatOffset = floatY
                val currentBossRect = RectF(boss.rect).apply { offset(0f, floatY) }
                
                // 3. Lógica de Daño
                if (boss.inputLinkId.isNotBlank() && state.signalBus[boss.inputLinkId] == true) {
                    val now = System.currentTimeMillis()
                    if (now - boss.lastDamageTime > 1000) {
                        boss.health -= 1
                        boss.lastDamageTime = now
                        ParticleManager.triggerBossDamageParticles(currentBossRect.centerX(), currentBossRect.centerY())
                        if (boss.health <= 0) {
                            boss.isDefeated = true
                        }
                    }
                }
                
                // 4. Lógica de Disparo (Ataque Normal y Especial)
                val now = System.currentTimeMillis()
                if (now - boss.lastShootTime > currentAttackCooldown) {
                    boss.lastShootTime = now
                    boss.normalAttackCount++
                    val isSpecial = boss.normalAttackCount >= currentSpecialReq
                    
                    if (isSpecial) {
                        boss.normalAttackCount = 0
                        // --- ATAQUE ESPECIAL ---
                        when (boss.bossType) {
                            "scatter" -> {
                                val count = boss.currentAttackDensity * 3 // Explosión masiva en todas direcciones
                                for (k in 0 until count) {
                                    val angle = Math.random() * Math.PI * 2
                                    val speed = (3f + Math.random().toFloat() * 4f) * designScale
                                    bossProjectiles.add(ProjectileData(currentBossRect.centerX(), currentBossRect.centerY(), Math.cos(angle).toFloat() * speed, Math.sin(angle).toFloat() * speed))
                                }
                            }
                            "tracker" -> {
                                val count = boss.currentAttackDensity * 2 // Arco concentrado letal hacia el jugador
                                val baseAngle = Math.atan2((state.posY - currentBossRect.centerY()).toDouble(), (state.posX - currentBossRect.centerX()).toDouble())
                                for (k in 0 until count) {
                                    val spread = (k - count/2f) * 0.12f
                                    val speed = 8f * designScale
                                    bossProjectiles.add(ProjectileData(currentBossRect.centerX(), currentBossRect.centerY(), Math.cos(baseAngle + spread).toFloat() * speed, Math.sin(baseAngle + spread).toFloat() * speed))
                                }
                            }
                            "spinner" -> {
                                val count = boss.currentAttackDensity * 4 // Anillo explosivo simétrico
                                for (k in 0 until count) {
                                    val angle = (k.toFloat() / count.toFloat()) * Math.PI * 2
                                    val speed = 5f * designScale
                                    bossProjectiles.add(ProjectileData(currentBossRect.centerX(), currentBossRect.centerY(), Math.cos(angle).toFloat() * speed, Math.sin(angle).toFloat() * speed))
                                }
                            }
                        }
                    } else {
                        // --- ATAQUE NORMAL ---
                        when (boss.bossType) {
                            "scatter" -> {
                                val count = boss.currentAttackDensity
                                for (k in 0 until count) {
                                    val angle = Math.random() * Math.PI * 2
                                    val speed = 4f * designScale
                                    bossProjectiles.add(ProjectileData(currentBossRect.centerX(), currentBossRect.centerY(), Math.cos(angle).toFloat() * speed, Math.sin(angle).toFloat() * speed))
                                }
                            }
                            "tracker" -> {
                                val count = boss.currentAttackDensity
                                val baseAngle = Math.atan2((state.posY - currentBossRect.centerY()).toDouble(), (state.posX - currentBossRect.centerX()).toDouble())
                                for (k in 0 until count) {
                                    val spread = if (count > 1) (k - (count-1)/2f) * 0.15f else 0f
                                    val speed = 6f * designScale
                                    bossProjectiles.add(ProjectileData(currentBossRect.centerX(), currentBossRect.centerY(), Math.cos(baseAngle + spread).toFloat() * speed, Math.sin(baseAngle + spread).toFloat() * speed))
                                }
                            }
                            "spinner" -> {
                                val count = boss.currentAttackDensity
                                val baseAngle = boss.moveCycleTime * 2.0 // Gira con el tiempo
                                for (k in 0 until count) {
                                    val angle = baseAngle + (k.toFloat() / count.toFloat()) * Math.PI * 2
                                    val speed = 5f * designScale
                                    bossProjectiles.add(ProjectileData(currentBossRect.centerX(), currentBossRect.centerY(), Math.cos(angle).toFloat() * speed, Math.sin(angle).toFloat() * speed))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Actualizar proyectiles del jefe y verificar colisiones con la pelota
        val projIterator = bossProjectiles.iterator()
        while (projIterator.hasNext()) {
            val proj = projIterator.next()
            proj.x += proj.vx * dt
            proj.y += proj.vy * dt
            
            val dx = state.posX - proj.x
            val dy = state.posY - proj.y
            val distSq = dx * dx + dy * dy
            val colDist = state.radius + proj.radius
            if (distSq < colDist * colDist) {
                state.onTriggerDeath()
                return
            }
            
            if (proj.x < -50f || proj.x > state.physicalLevelWidth + 50f || proj.y < -50f || proj.y > state.physicalLevelHeight + 50f) {
                projIterator.remove()
            }
        }

        // 3. Fricción/Arrastre (Drag) ajustado exponencialmente por dt
        val baseDrag = 0.95f
        val effectiveDrag = Math.pow(baseDrag.toDouble(), dt.toDouble()).toFloat()
        state.velX *= effectiveDrag
        state.velY *= effectiveDrag

        // Calcular desplazamiento total esperado en este frame
        val expectedDisplacement = Math.sqrt((state.velX * state.velX + state.velY * state.velY).toDouble()).toFloat() * dt
        val maxSafeStep = state.radius * 0.4f
        val substeps = if (expectedDisplacement > maxSafeStep) {
            Math.ceil((expectedDisplacement / maxSafeStep).toDouble()).toInt().coerceIn(1, 8)
        } else {
            1
        }
        val subDt = dt / substeps

        for (step in 0 until substeps) {
            val nextX = state.posX + state.velX * subDt
            val nextY = state.posY + state.velY * subDt

            // --- COLISIONES CON MUROS Y COMPUERTAS CERRADAS ---
            val boundsX = RectF(nextX - state.radius, state.posY - state.radius, nextX + state.radius, state.posY + state.radius)
            var collisionX = false

            // Colisiones con Cajas Móviles (Empuje en X)
            for (box in boxes) {
                if (RectF.intersects(box.rect, boundsX)) {
                    val pushDist = if (state.velX > 0) {
                        (nextX + state.radius) - box.rect.left
                    } else {
                        (nextX - state.radius) - box.rect.right
                    }
                    val testRect = RectF(box.rect)
                    testRect.offset(pushDist, 0f)
                    if (!isBoxColliding(box, testRect, walls, gates, boxes, state.physicalLevelWidth, state.physicalLevelHeight)) {
                        box.rect.set(testRect)
                    } else {
                        collisionX = true
                    }
                }
            }

            if (!collisionX) {
                for (wall in walls) {
                    if (RectF.intersects(wall, boundsX)) {
                        collisionX = true
                        break
                    }
                }
            }
            if (!collisionX) {
                for (gate in gates) {
                    if (!gate.isGateOpen && RectF.intersects(gate.rect, boundsX)) {
                        collisionX = true
                        break
                    }
                }
            }
            if (!collisionX) {
                for (me in movingEntities) {
                    if (!me.isHazard && RectF.intersects(me.currentRect, boundsX)) {
                        collisionX = true
                        break
                    }
                }
            }
            if (!collisionX && nextX - state.radius >= 0 && nextX + state.radius <= state.physicalLevelWidth) {
                state.posX = nextX
            } else {
                state.velX = 0f
            }

            val boundsY = RectF(state.posX - state.radius, nextY - state.radius, state.posX + state.radius, nextY + state.radius)
            var collisionY = false

            // Colisiones con Cajas Móviles (Empuje en Y)
            for (box in boxes) {
                if (RectF.intersects(box.rect, boundsY)) {
                    val pushDist = if (state.velY > 0) {
                        (nextY + state.radius) - box.rect.top
                    } else {
                        (nextY - state.radius) - box.rect.bottom
                    }
                    val testRect = RectF(box.rect)
                    testRect.offset(0f, pushDist)
                    if (!isBoxColliding(box, testRect, walls, gates, boxes, state.physicalLevelWidth, state.physicalLevelHeight)) {
                        box.rect.set(testRect)
                    } else {
                        collisionY = true
                    }
                }
            }

            if (!collisionY) {
                for (wall in walls) {
                    if (RectF.intersects(wall, boundsY)) {
                        collisionY = true
                        break
                    }
                }
            }
            if (!collisionY) {
                for (gate in gates) {
                    if (!gate.isGateOpen && RectF.intersects(gate.rect, boundsY)) {
                        collisionY = true
                        break
                    }
                }
            }
            if (!collisionY) {
                for (me in movingEntities) {
                    if (!me.isHazard && RectF.intersects(me.currentRect, boundsY)) {
                        collisionY = true
                        break
                    }
                }
            }
            if (!collisionY && nextY - state.radius >= 0 && nextY + state.radius <= state.physicalLevelHeight) {
                state.posY = nextY
            } else {
                state.velY = 0f
            }

            // --- RESOLUCIÓN DE SOBREPOSICIÓN Y EMPUJE EN TIEMPO REAL ---
            var hasOverlap = true
            var overlapAttempts = 0
            while (hasOverlap && overlapAttempts < 3) {
                hasOverlap = false
                overlapAttempts++
                
                // 1. Muros estáticos
                for (wall in walls) {
                    if (resolveOverlap(wall, 0f, 0f, state)) {
                        hasOverlap = true
                    }
                }
                // 2. Compuertas cerradas
                for (gate in gates) {
                    if (!gate.isGateOpen) {
                        if (resolveOverlap(gate.rect, 0f, 0f, state)) {
                            hasOverlap = true
                        }
                    }
                }
                // 3. Muros móviles (empujan transmitiendo velocidad real)
                for (me in movingEntities) {
                    if (!me.isHazard) {
                        val pVelX = if (dt > 0f) me.vx / dt else 0f
                        val pVelY = if (dt > 0f) me.vy / dt else 0f
                        if (resolveOverlap(me.currentRect, pVelX, pVelY, state)) {
                            hasOverlap = true
                        }
                    }
                }
            }

            state.posX = state.posX.coerceIn(state.radius, state.physicalLevelWidth - state.radius)
            state.posY = state.posY.coerceIn(state.radius, state.physicalLevelHeight - state.radius)
        }

        // --- CHEQUEO DE PELIGROS Y META ---
        val finalRect = RectF(
            state.posX - state.radius,
            state.posY - state.radius,
            state.posX + state.radius,
            state.posY + state.radius
        )
        
        for (hazard in hazards) {
            if (RectF.intersects(hazard, finalRect)) {
                state.onTriggerDeath()
                return
            }
        }

        for (me in movingEntities) {
            if (me.isHazard && RectF.intersects(me.currentRect, finalRect)) {
                state.onTriggerDeath()
                return
            }
        }

        // Chequeo de colisión circular perfecta con Sierras Giratorias
        for (saw in spinningHazards) {
            val dx = state.posX - saw.cx
            val dy = state.posY - saw.cy
            val distanceSq = dx * dx + dy * dy
            val collisionDist = state.radius + saw.radius
            if (distanceSq < collisionDist * collisionDist) {
                state.onTriggerDeath()
                return
            }
        }
        
        // Chequeo de colisión con el Jefe (mata al jugador al tocar)
        state.activeBoss?.let { boss ->
            if (!boss.isDefeated && boss.entranceProgress == 1f && RectF.intersects(boss.rect, finalRect)) {
                state.onTriggerDeath()
                return
            }
        }



        if (RectF.intersects(state.goal, finalRect)) {
            state.onLevelComplete()
            return
        }

        // --- CHEQUEO DE CHECKPOINTS ---
        for (i in state.checkpoints.indices) {
            val cp = state.checkpoints[i]
            if (RectF.intersects(cp.rect, finalRect)) {
                val currentActiveCP = state.checkpoints.getOrNull(state.activeCheckpointIndex)
                val activeLogicalIndex = currentActiveCP?.index ?: -1
                
                if (cp.index >= activeLogicalIndex) {
                    if (state.activeCheckpointIndex != i) {
                        state.activeCheckpointIndex = i
                        state.activeCheckpointX = cp.rect.centerX()
                        state.activeCheckpointY = cp.rect.centerY()
                        state.onCheckpointActivated()
                        state.onTriggerCheckpointAnimation(cp.rect.centerX(), cp.rect.centerY())
                    }
                }
            }
        }

        // --- CHEQUEO DE PORTALES ---
        if (state.portalCooldown > 0f) {
            state.portalCooldown -= dt / 60f
        } else {
            for (portalA in portals) {
                if (RectF.intersects(portalA.rect, finalRect)) {
                    val portalB = portals.firstOrNull { it !== portalA && it.portalId == portalA.portalId }
                    if (portalB != null) {
                        state.posX = portalB.rect.centerX()
                        state.posY = portalB.rect.centerY()
                        state.portalCooldown = 0.5f
                        ParticleManager.triggerPortalTeleportParticles(portalA.rect.centerX(), portalA.rect.centerY(), state.themeName)
                        ParticleManager.triggerPortalTeleportParticles(portalB.rect.centerX(), portalB.rect.centerY(), state.themeName)
                        break
                    }
                }
            }
        }

        // Actualizar estela con decaimiento por delta time
        val decayRate = 0.08f * dt
        val iterator = trailPoints.iterator()
        while (iterator.hasNext()) {
            val tp = iterator.next()
            tp.alpha -= decayRate
            if (tp.alpha <= 0f) {
                iterator.remove()
            }
        }

        // Agregar nuevo punto si la bola se ha movido suficiente (evita acumulación al detenerse)
        val lastPoint = trailPoints.lastOrNull()
        val distSq = if (lastPoint != null) {
            val dx = state.posX - lastPoint.x
            val dy = state.posY - lastPoint.y
            dx * dx + dy * dy
        } else {
            Float.MAX_VALUE
        }

        if (distSq > 36f) { // 6 píxeles de distancia al cuadrado (36f)
            trailPoints.add(TrailPoint(state.posX, state.posY))
        }
    }

    private fun isBoxColliding(
        box: BoxData,
        testRect: RectF,
        walls: List<RectF>,
        gates: List<GateData>,
        boxes: List<BoxData>,
        physicalLevelWidth: Float,
        physicalLevelHeight: Float
    ): Boolean {
        for (wall in walls) {
            if (RectF.intersects(wall, testRect)) return true
        }
        for (gate in gates) {
            if (!gate.isGateOpen && RectF.intersects(gate.rect, testRect)) return true
        }
        if (testRect.left < 0f || testRect.right > physicalLevelWidth || testRect.top < 0f || testRect.bottom > physicalLevelHeight) return true
        for (other in boxes) {
            if (other !== box && RectF.intersects(other.rect, testRect)) return true
        }
        return false
    }

    private fun resolveOverlap(rect: RectF, pushVelX: Float = 0f, pushVelY: Float = 0f, state: PhysicsState): Boolean {
        val closestX = state.posX.coerceIn(rect.left, rect.right)
        val closestY = state.posY.coerceIn(rect.top, rect.bottom)
        
        val dx = state.posX - closestX
        val dy = state.posY - closestY
        val distanceSq = dx * dx + dy * dy
        
        if (distanceSq < state.radius * state.radius || (state.posX >= rect.left && state.posX <= rect.right && state.posY >= rect.top && state.posY <= rect.bottom)) {
            val distance = Math.sqrt(distanceSq.toDouble()).toFloat()
            
            if (distance == 0f || (state.posX >= rect.left && state.posX <= rect.right && state.posY >= rect.top && state.posY <= rect.bottom)) {
                val dl = state.posX - rect.left + state.radius
                val dr = rect.right - state.posX + state.radius
                val dt = state.posY - rect.top + state.radius
                val db = rect.bottom - state.posY + state.radius
                
                val minDepth = Math.min(Math.min(dl, dr), Math.min(dt, db))
                when (minDepth) {
                    dl -> {
                        state.posX = rect.left - state.radius
                        if (pushVelX != 0f) state.velX = pushVelX
                    }
                    dr -> {
                        state.posX = rect.right + state.radius
                        if (pushVelX != 0f) state.velX = pushVelX
                    }
                    dt -> {
                        state.posY = rect.top - state.radius
                        if (pushVelY != 0f) state.velY = pushVelY
                    }
                    db -> {
                        state.posY = rect.bottom + state.radius
                        if (pushVelY != 0f) state.velY = pushVelY
                    }
                }
            } else {
                val overlap = state.radius - distance
                val normX = dx / distance
                val normY = dy / distance
                state.posX += normX * overlap
                state.posY += normY * overlap
                
                if (normX < 0 && pushVelX != 0f) state.velX = pushVelX
                if (normX > 0 && pushVelX != 0f) state.velX = pushVelX
                if (normY < 0 && pushVelY != 0f) state.velY = pushVelY
                if (normY > 0 && pushVelY != 0f) state.velY = pushVelY
            }
            return true
        }
        return false
    }
}
