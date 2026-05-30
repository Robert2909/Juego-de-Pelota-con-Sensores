package com.example.juegodepelotaconsensores.models

import android.graphics.RectF

data class PhysicsState(
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
    val checkpoints: MutableList<CheckpointData>,
    val signalBus: MutableMap<String, Boolean>,
    val onTriggerDeath: () -> Unit,
    val onLevelComplete: () -> Unit,
    val onCheckpointActivated: () -> Unit,
    val onTriggerCheckpointAnimation: (Float, Float) -> Unit,
    val onTriggerSwitchAnimation: (Float, Float) -> Unit,
    var portalCooldown: Float
)
