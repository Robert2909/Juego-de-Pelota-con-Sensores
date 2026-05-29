package com.example.juegodepelotaconsensores.engine

import android.content.Context
import com.example.juegodepelotaconsensores.models.GameTheme
import org.json.JSONObject
import java.io.InputStream

object ThemeManager {
    private val themes = mutableMapOf<String, GameTheme>()

    fun loadThemesFromJson(context: Context) {
        try {
            val inputStream: InputStream = context.assets.open("themes.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val themeObj = json.getJSONObject(key)
                
                val bgColor = themeObj.optString("bgColor", "#121212")
                val wallColor = themeObj.optString("wall", "#333333")
                val hazardColor = themeObj.optString("hazard", "#E57373")
                val goalColor = themeObj.optString("goal", "#81C784")
                val ballColor = themeObj.optString("ball", "#FFFFFF")
                val trailColor = themeObj.optString("trail", "#44FFFFFF")
                val blob1Color = themeObj.optString("blob1", "#1A1A1A")
                val blob2Color = themeObj.optString("blob2", "#161616")
                val blob3Color = themeObj.optString("blob3", "#151515")
                val checkpointColor = themeObj.optString("checkpoint", "#64B5F6")
                val switchColor = themeObj.optString("switch", "#FFD54F")
                val gateColor = themeObj.optString("gate", "#90A4AE")
                val boxColor = themeObj.optString("boxColor", "#4FC3F7")
                val boxBg = themeObj.optString("boxBg", "#152238")
                val logicGateColor = themeObj.optString("logicGateColor", "#9575CD")
                val logicGateBg = themeObj.optString("logicGateBg", "#1A1A1A")
                val windColor = themeObj.optString("windColor", "#00E5FF")
                val speedPadColor = themeObj.optString("speedPadColor", "#FFEA00")
                val bossColor = themeObj.optString("bossColor", "#1B1A24")
                val bossLaserColor = themeObj.optString("bossLaserColor", "#FF1744")
                
                themes[key] = GameTheme(
                    bgColor, wallColor, hazardColor, goalColor, ballColor, trailColor,
                    blob1Color, blob2Color, blob3Color, checkpointColor, switchColor, gateColor,
                    boxColor, boxBg, logicGateColor, logicGateBg, windColor, speedPadColor, bossColor, bossLaserColor
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback de seguridad en memoria
            themes["industrial"] = GameTheme("#121212", "#333333", "#E57373", "#81C784", "#FFFFFF", "#44FFFFFF", "#1A1A1A", "#161616", "#151515", "#64B5F6", "#FFD54F", "#90A4AE", "#FFB74D", "#2A2015", "#9575CD", "#1E1A2A", "#00E5FF", "#FFEA00", "#1B1A24", "#FF1744")
            themes["volcano"] = GameTheme("#1A0905", "#2C1610", "#FF5722", "#FFE082", "#FFEB3B", "#33FF5722", "#2E110A", "#230C07", "#1F0804", "#00E676", "#FFEB3B", "#A1887F", "#FF7043", "#21100B", "#AB47BC", "#1D0A1C", "#FF8A65", "#FF9800", "#3E2723", "#D84315")
            themes["neon"] = GameTheme("#0A0915", "#1B1947", "#FF007F", "#00FF66", "#00F2FF", "#3300F2FF", "#15112E", "#0B1D2A", "#1E0920", "#E040FB", "#FFFF00", "#3D5AFE", "#00F2FF", "#08162B", "#E040FB", "#180826", "#B388FF", "#76FF03", "#1A237E", "#FF00FF")
            themes["forest"] = GameTheme("#0D1611", "#243328", "#8D6E63", "#AED581", "#E8F5E9", "#22E8F5E9", "#13231B", "#171F14", "#0A1D13", "#4DB6AC", "#FFB74D", "#795548", "#8D6E63", "#211510", "#26A69A", "#0B1E1B", "#81C784", "#CDDC39", "#1B5E20", "#FF9800")
        }
    }

    fun getTheme(themeName: String?): GameTheme {
        return themes[themeName ?: "industrial"] ?: themes["industrial"]!!
    }
}
