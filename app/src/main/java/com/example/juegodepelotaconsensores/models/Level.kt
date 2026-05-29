package com.example.juegodepelotaconsensores.models

data class Level(
    val id: Int,
    val theme: String?,
    val rawEntities: List<RawEntity>,
    val width: Float = 1920f,
    val height: Float = 1080f
)
