package com.example.juegodepelotaconsensores.models

import android.graphics.RectF

class GameState(
    // Identidad del nivel
    var currentLevelId: Int = 1,
    var themeName: String = "industrial",
    var physicalLevelWidth: Float = 1920f,
    var physicalLevelHeight: Float = 1080f,

    // Datos físicos del jugador
    var posX: Float = 0f,
    var posY: Float = 0f,
    var velX: Float = 0f,
    var velY: Float = 0f,
    var radius: Float = 20f,
    
    // Controles e input
    var tiltX: Float = 0f,
    var tiltY: Float = 0f,

    // Geometría Estática
    val walls: MutableList<RectF> = mutableListOf(),
    val hazards: MutableList<RectF> = mutableListOf(),
    var goal: RectF = RectF(),
    var startX: Float = 0f,
    var startY: Float = 0f,

    // Entidades Dinámicas
    val movingEntities: MutableList<MovingEntityData> = mutableListOf(),
    val spinningHazards: MutableList<SpinningHazardData> = mutableListOf(),
    val boxes: MutableList<BoxData> = mutableListOf(),
    val windZones: MutableList<WindZoneData> = mutableListOf(),
    val speedPads: MutableList<SpeedPadData> = mutableListOf(),
    val portals: MutableList<PortalData> = mutableListOf(),

    // Mecanismos Lógicos
    val switches: MutableList<SwitchData> = mutableListOf(),
    val activeSwitches: MutableSet<Int> = mutableSetOf(),
    val gates: MutableList<GateData> = mutableListOf(),
    val logicGates: MutableList<LogicGateData> = mutableListOf(),
    val timers: MutableList<TimerData> = mutableListOf(),
    
    // Checkpoints
    val checkpoints: MutableList<CheckpointData> = mutableListOf(),
    var activeCheckpointIndex: Int = -1,
    var activeCheckpointX: Float? = null,
    var activeCheckpointY: Float? = null,

    // Sistema de señales global
    val signalBus: MutableMap<String, Boolean> = mutableMapOf(),

    // Jefe (Boss) y Proyectiles
    var activeBoss: BossData? = null,
    val bossProjectiles: MutableList<ProjectileData> = mutableListOf(),

    // Rastros visuales
    val trailPoints: MutableList<TrailPoint> = mutableListOf(),

    // Variables de estado del flujo de juego
    var isExploding: Boolean = false,
    var explosionTime: Long = 0L,
    var explosionDuration: Long = 350L,
    var explosionX: Float = 0f,
    var explosionY: Float = 0f,
    var isLevelCompleted: Boolean = false,
    var portalCooldown: Float = 0f
) {
    fun resetForNewLevel(levelId: Int, newTheme: String, width: Float, height: Float) {
        currentLevelId = levelId
        themeName = newTheme
        physicalLevelWidth = width
        physicalLevelHeight = height
        
        isExploding = false
        isLevelCompleted = false
        activeCheckpointIndex = -1
        activeCheckpointX = null
        activeCheckpointY = null
        activeBoss = null
        portalCooldown = 0f
        
        walls.clear()
        hazards.clear()
        movingEntities.clear()
        spinningHazards.clear()
        boxes.clear()
        windZones.clear()
        speedPads.clear()
        portals.clear()
        
        switches.clear()
        activeSwitches.clear()
        gates.clear()
        logicGates.clear()
        timers.clear()
        checkpoints.clear()
        signalBus.clear()
        bossProjectiles.clear()
        trailPoints.clear()
    }
}
