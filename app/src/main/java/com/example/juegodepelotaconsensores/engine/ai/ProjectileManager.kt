package com.example.juegodepelotaconsensores.engine.ai

import com.example.juegodepelotaconsensores.models.PhysicsState
import com.example.juegodepelotaconsensores.models.ProjectileData

object ProjectileManager {

    fun update(state: PhysicsState, dt: Float, bossProjectiles: MutableList<ProjectileData>) {
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
                state.onTriggerDeath.invoke()
                return
            }
            
            if (proj.x < -50f || proj.x > state.physicalLevelWidth + 50f || proj.y < -50f || proj.y > state.physicalLevelHeight + 50f) {
                projIterator.remove()
            }
        }
    }
}
