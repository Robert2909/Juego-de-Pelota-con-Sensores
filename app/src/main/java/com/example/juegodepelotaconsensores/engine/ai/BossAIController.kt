package com.example.juegodepelotaconsensores.engine.ai

import android.graphics.RectF
import com.example.juegodepelotaconsensores.models.PhysicsState
import com.example.juegodepelotaconsensores.models.ProjectileData
import com.example.juegodepelotaconsensores.engine.ParticleManager

object BossAIController {
    
    fun checkDefeatedState(state: PhysicsState): Boolean {
        val boss = state.activeBoss ?: return false
        if (!boss.isDefeated) return false

        val now = System.currentTimeMillis()
        val timeSinceDefeat = now - boss.lastDamageTime
        
        if (timeSinceDefeat > 2000) {
            val animationTime = timeSinceDefeat - 2000
            
            if (!boss.hasStartedAnimation) {
                boss.hasStartedAnimation = true
                ParticleManager.triggerBossDamageParticles(boss.rect.centerX(), boss.rect.centerY())
            }
            
            val intensity = (animationTime / 3000f).coerceIn(0f, 1f) * 30f
            boss.floatOffset = (Math.random().toFloat() - 0.5f) * intensity
            
            if (animationTime > 3000 && !boss.hasExploded) {
                boss.hasExploded = true
                ParticleManager.triggerBossDefeatParticles(boss.rect.centerX(), boss.rect.centerY())
                ParticleManager.triggerBossDefeatParticles(boss.rect.centerX(), boss.rect.centerY())
            }
            
            if (animationTime > 3500 && animationTime < 4500 && Math.random() < 0.1) {
                val rx = boss.rect.centerX() + (Math.random() - 0.5f) * 150f
                val ry = boss.rect.centerY() + (Math.random() - 0.5f) * 150f
                ParticleManager.triggerBossDefeatParticles(rx.toFloat(), ry.toFloat())
            }
            
            if (animationTime > 5500) {
                state.onLevelComplete.invoke()
            }
        } else {
            boss.floatOffset = 0f
        }
        return true
    }

    fun update(state: PhysicsState, dt: Float, bossProjectiles: MutableList<ProjectileData>) {
        val boss = state.activeBoss ?: return
        if (boss.isDefeated) return

        val designScale = state.radius / 20f

        val phaseFactor = boss.health.toFloat() / boss.maxHealth.toFloat()
        val phaseSize = 1.0f / boss.phases.toFloat()
        var newPhase = boss.phases - Math.ceil((phaseFactor / phaseSize).toDouble()).toInt() + 1
        newPhase = newPhase.coerceIn(1, boss.phases)
        
        boss.currentPhase = newPhase
        
        val f = if (boss.phases > 1) (boss.currentPhase - 1).toFloat() / (boss.phases - 1).toFloat() else 0f
        val multiplier = 1f + f
        
        boss.currentSpeed = boss.baseSpeed * multiplier
        boss.currentAttackDensity = Math.round(boss.baseAttackDensity * multiplier)
        val currentAttackCooldown = (boss.baseAttackFrequency / multiplier) * 1000L
        val currentSpecialReq = Math.max(1, Math.round(boss.specialAttackFrequency / multiplier))

        boss.moveCycleTime += dt / 60f
        val bx = boss.rect.centerX()
        val by = boss.rect.centerY()
        
        when (boss.bossType) {
            "scatter" -> {
                if (Math.abs(bx - boss.targetX) < 10f && Math.abs(by - boss.targetY) < 10f || boss.targetX == 0f) {
                    boss.targetX = bx + (Math.random().toFloat() - 0.5f) * 600f
                    boss.targetY = by + (Math.random().toFloat() - 0.5f) * 600f
                    boss.targetX = boss.targetX.coerceIn(100f, state.physicalLevelWidth - 100f)
                    boss.targetY = boss.targetY.coerceIn(100f, state.physicalLevelHeight - 100f)
                }
            }
            "tracker" -> {
                boss.targetX = state.posX
                boss.targetY = state.posY
            }
            "spinner" -> {
                val cycle = boss.moveCycleTime * 0.5f
                boss.targetX = state.physicalLevelWidth / 2f + Math.cos(cycle.toDouble()).toFloat() * 400f
                boss.targetY = state.physicalLevelHeight / 2f + Math.sin((cycle * 1.5).toDouble()).toFloat() * 300f
            }
        }
        
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

        val floatY = 15f * Math.sin(System.currentTimeMillis() / 400.0).toFloat()
        boss.floatOffset = floatY
        val currentBossRect = RectF(boss.rect).apply { offset(0f, floatY) }
        
        if (boss.inputLinkId.isNotBlank() && state.signalBus[boss.inputLinkId] == true) {
            if (boss.takeDamage()) {
                ParticleManager.triggerBossDamageParticles(currentBossRect.centerX(), currentBossRect.centerY())
            }
        }
        
        val now = System.currentTimeMillis()
        if (now - boss.lastShootTime > currentAttackCooldown) {
            boss.lastShootTime = now
            boss.normalAttackCount++
            val isSpecial = boss.normalAttackCount >= currentSpecialReq
            
            if (isSpecial) {
                boss.normalAttackCount = 0
                when (boss.bossType) {
                    "scatter" -> {
                        val count = boss.currentAttackDensity * 3
                        for (k in 0 until count) {
                            val angle = Math.random() * Math.PI * 2
                            val speed = (3f + Math.random().toFloat() * 4f) * designScale
                            bossProjectiles.add(ProjectileData(currentBossRect.centerX(), currentBossRect.centerY(), Math.cos(angle).toFloat() * speed, Math.sin(angle).toFloat() * speed))
                        }
                    }
                    "tracker" -> {
                        val count = boss.currentAttackDensity * 2
                        val baseAngle = Math.atan2((state.posY - currentBossRect.centerY()).toDouble(), (state.posX - currentBossRect.centerX()).toDouble())
                        for (k in 0 until count) {
                            val spread = (k - count/2f) * 0.12f
                            val speed = 8f * designScale
                            bossProjectiles.add(ProjectileData(currentBossRect.centerX(), currentBossRect.centerY(), Math.cos(baseAngle + spread).toFloat() * speed, Math.sin(baseAngle + spread).toFloat() * speed))
                        }
                    }
                    "spinner" -> {
                        val count = boss.currentAttackDensity * 4
                        for (k in 0 until count) {
                            val angle = (k.toFloat() / count.toFloat()) * Math.PI * 2
                            val speed = 5f * designScale
                            bossProjectiles.add(ProjectileData(currentBossRect.centerX(), currentBossRect.centerY(), Math.cos(angle).toFloat() * speed, Math.sin(angle).toFloat() * speed))
                        }
                    }
                }
            } else {
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
                        val baseAngle = boss.moveCycleTime * 2.0
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
