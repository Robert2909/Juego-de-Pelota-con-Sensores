package com.example.juegodepelotaconsensores.engine

import android.graphics.RectF
import com.example.juegodepelotaconsensores.models.*



object PhysicsEngine {

    fun update(
        state: PhysicsState,
        dt: Float,
        walls: MutableList<RectF>,
        hazards: MutableList<RectF>,
        gates: MutableList<GateData>,
        switches: MutableList<SwitchData>,
        boxes: MutableList<BoxData>,
        logicGates: MutableList<LogicGateData>,
        movingEntities: MutableList<MovingEntityData>,
        spinningHazards: MutableList<SpinningHazardData>,
        windZones: MutableList<WindZoneData>,
        speedPads: MutableList<SpeedPadData>,
        portals: MutableList<PortalData>,
        bossProjectiles: MutableList<ProjectileData>,
        trailPoints: MutableList<TrailPoint>
    ) {
        if (state.isExploding) return

        if (com.example.juegodepelotaconsensores.engine.ai.BossAIController.checkDefeatedState(state)) {
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
        com.example.juegodepelotaconsensores.engine.ai.BossAIController.update(state, dt, bossProjectiles)

        // Actualizar proyectiles del jefe y verificar colisiones con la pelota
        com.example.juegodepelotaconsensores.engine.ai.ProjectileManager.update(state, dt, bossProjectiles)

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
                state.onTriggerDeath.invoke()
                return
            }
        }

        for (me in movingEntities) {
            if (me.isHazard && RectF.intersects(me.currentRect, finalRect)) {
                state.onTriggerDeath.invoke()
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
                state.onTriggerDeath.invoke()
                return
            }
        }
        
        // Chequeo de colisión con el Jefe (mata al jugador al tocar)
        state.activeBoss?.let { boss ->
            if (!boss.isDefeated && boss.entranceProgress == 1f && RectF.intersects(boss.rect, finalRect)) {
                state.onTriggerDeath.invoke()
                return
            }
        }



        if (RectF.intersects(state.goal, finalRect)) {
            state.onLevelComplete.invoke()
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
                        state.onCheckpointActivated.invoke()
                        state.onTriggerCheckpointAnimation.invoke(cp.rect.centerX(), cp.rect.centerY())
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
        walls: MutableList<RectF>,
        gates: MutableList<GateData>,
        boxes: MutableList<BoxData>,
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
                
                if (normX < 0f && pushVelX != 0f) state.velX = pushVelX
                if (normX > 0f && pushVelX != 0f) state.velX = pushVelX
                if (normY < 0f && pushVelY != 0f) state.velY = pushVelY
                if (normY > 0f && pushVelY != 0f) state.velY = pushVelY
            }
            return true
        }
        return false
    }
}
