package com.example.juegodepelotaconsensores.models

data class BgParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var alpha: Int,
    var maxAlpha: Int,
    var fadeSpeed: Float,
    var oscSpeed: Float,
    var oscRadius: Float,
    var phase: Float
)
