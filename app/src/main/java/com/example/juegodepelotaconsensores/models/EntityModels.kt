package com.example.juegodepelotaconsensores.models

import android.graphics.RectF

data class CheckpointData(
    val rect: RectF,
    val index: Int
)

data class SwitchData(
    val rect: RectF,
    val outputLinkId: String,
    val switchMode: String,
    var isToggleActive: Boolean = false,
    var wasTouchedLastFrame: Boolean = false,
    var isHoldActive: Boolean = false,
    var visualTimer: Float = 0f,
    var checkpointToggleActive: Boolean = false
)

data class GateData(
    val rect: RectF,
    val inputLinkId: String,
    var isGateOpen: Boolean = false,
    val duration: Float?,
    var openTimer: Float = 0f
)

data class LogicGateData(
    val rect: RectF,
    val type: String,
    val inputLinkIds: List<String>,
    val outputLinkId: String
)

data class WindZoneData(
    val rect: RectF,
    val forceX: Float,
    val forceY: Float
)

data class SpeedPadData(
    val rect: RectF,
    val boostX: Float,
    val boostY: Float
)

data class BossData(
    val rect: RectF,
    val baseRect: RectF,
    val inputLinkId: String,
    var name: String = "Jefe Épico",
    var health: Int = 3,
    var maxHealth: Int = 3,
    var visualHealth: Float = 3f,
    var phases: Int = 2,
    var currentPhase: Int = 1,
    var bossType: String = "scatter",
    var baseSpeed: Float = 150f,
    var currentSpeed: Float = 150f,
    var baseAttackDensity: Int = 3,
    var currentAttackDensity: Int = 3,
    var baseAttackFrequency: Float = 2.0f,
    var currentAttackFrequency: Float = 2.0f,
    var specialAttackFrequency: Int = 3,
    var normalAttackCount: Int = 0,
    var moveCycleTime: Float = 0f,
    var targetX: Float = 0f,
    var targetY: Float = 0f,
    var lastDamageTime: Long = 0L,
    var lastShootTime: Long = 0L,
    var isDefeated: Boolean = false,
    var floatOffset: Float = 0f,
    var hasExploded: Boolean = false,
    var hasStartedAnimation: Boolean = false,
    var checkpointHealth: Int = 3,
    var checkpointPhase: Int = 1,
    var entranceProgress: Float = 0f
)

data class ProjectileData(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float = 12f
)

data class MovingEntityData(
    val baseRect: RectF,
    val dx: Float,
    val dy: Float,
    val speed: Float?,
    val isHazard: Boolean,
    val inputLinkId: String?,
    val currentRect: RectF = RectF(baseRect),
    var vx: Float = 0f,
    var vy: Float = 0f,
    var internalTimeMs: Float = 0f
)

data class SpinningHazardData(
    val cx: Float,
    val cy: Float,
    val radius: Float,
    val inputLinkId: String?
)

data class BoxData(
    val rect: RectF,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val baseRect: RectF = RectF(rect),
    val checkpointRect: RectF = RectF(rect)
)

data class TrailPoint(
    val x: Float,
    val y: Float,
    var alpha: Float = 1.0f
)

data class TimerData(
    val rect: RectF,
    val inputLinkId: String,
    val outputLinkId: String,
    val duration: Float,
    var timeLeft: Float = 0f,
    var isCounting: Boolean = false,
    var hasFinished: Boolean = false,
    var wasInputActiveLastFrame: Boolean = false
)

data class PortalData(
    val rect: RectF,
    val portalId: Int
)
