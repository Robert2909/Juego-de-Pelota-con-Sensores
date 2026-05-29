package com.example.juegodepelotaconsensores.models

data class GameTheme(
    val bgColor: String,
    val wallColor: String,
    val hazardColor: String,
    val goalColor: String,
    val ballColor: String,
    val trailColor: String,
    val blob1Color: String,
    val blob2Color: String,
    val blob3Color: String,
    val checkpointColor: String = "#64B5F6",
    val switchColor: String = "#FFD54F",
    val gateColor: String = "#90A4AE",
    val boxColor: String = "#4FC3F7",
    val boxBg: String = "#152238",
    val logicGateColor: String = "#9575CD",
    val logicGateBg: String = "#1A1A1A",
    val windColor: String = "#00E5FF",
    val speedPadColor: String = "#FFEA00",
    val bossColor: String = "#1B1A24",
    val bossLaserColor: String = "#FF1744"
)
