package com.example.juegodepelotaconsensores.models

data class RawEntity(
    val type: String,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val checkpointIndex: Int? = null,
    val linkId: String? = null,
    val duration: Float? = null,
    val dx: Float? = null,
    val dy: Float? = null,
    val speed: Float? = null,
    // Phase 3 Logic Properties
    val switchMode: String? = null,
    val gateType: String? = null,
    val inputLinkIds: String? = null,
    val outputLinkId: String? = null,
    val portalId: Int? = null,
    // Phase 4 Boss Parameters
    val name: String? = null,
    val bossType: String? = null,
    val health: Int? = null,
    val phases: Int? = null,
    val attackDensity: Int? = null,
    val attackFrequency: Float? = null,
    val specialAttackFrequency: Int? = null
)
