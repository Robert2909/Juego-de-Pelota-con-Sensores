package com.example.juegodepelotaconsensores.engine

import android.content.Context
import com.example.juegodepelotaconsensores.models.Level
import com.example.juegodepelotaconsensores.models.RawEntity
import org.json.JSONObject
import java.io.InputStream

object LevelManager {
    fun loadLevel(context: Context, levelId: Int): Level? {
        val fileName = if (levelId == 999) "levels/level_debug.json" else "levels/level_$levelId.json"
        return try {
            val inputStream: InputStream = context.assets.open(fileName)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            
            val theme = if (json.has("theme")) json.getString("theme") else "industrial"
            val levelWidth = if (json.has("width")) json.getDouble("width").toFloat() else 1920f
            val levelHeight = if (json.has("height")) json.getDouble("height").toFloat() else 1080f
            val entitiesArray = json.getJSONArray("entities")
            
            val rawEntities = mutableListOf<RawEntity>()
            for (i in 0 until entitiesArray.length()) {
                val obj = entitiesArray.getJSONObject(i)
                val type = obj.getString("type")
                val x = obj.getDouble("x").toFloat()
                val y = obj.getDouble("y").toFloat()
                val w = obj.getDouble("w").toFloat()
                val h = obj.getDouble("h").toFloat()
                
                val checkpointIndex = if (obj.has("checkpointIndex")) obj.getInt("checkpointIndex") else null
                val linkId = if (obj.has("linkId")) obj.getString("linkId") else null
                val duration = if (obj.has("duration")) obj.getDouble("duration").toFloat() else null
                val dx = if (obj.has("dx")) obj.getDouble("dx").toFloat() else null
                val dy = if (obj.has("dy")) obj.getDouble("dy").toFloat() else null
                val speed = if (obj.has("speed")) obj.getDouble("speed").toFloat() else null
                
                // Phase 3 Signal Bus & Logic properties
                val switchMode = if (obj.has("switchMode")) obj.getString("switchMode") else null
                val gateType = if (obj.has("gateType")) obj.getString("gateType") else null
                val inputLinkIds = if (obj.has("inputLinkIds")) obj.getString("inputLinkIds") else null
                val outputLinkId = if (obj.has("outputLinkId")) obj.getString("outputLinkId") else null
                val portalId = if (obj.has("portalId")) obj.getInt("portalId") else if (type == "portal" && obj.has("checkpointIndex")) obj.getInt("checkpointIndex") else null
                
                // Phase 4 Boss Parameters
                val name = if (obj.has("name")) obj.getString("name") else null
                val bossType = if (obj.has("bossType")) obj.getString("bossType") else null
                val health = if (obj.has("health")) obj.getInt("health") else null
                val phases = if (obj.has("phases")) obj.getInt("phases") else null
                val attackDensity = if (obj.has("attackDensity")) obj.getInt("attackDensity") else null
                val attackFrequency = if (obj.has("attackFrequency")) obj.getDouble("attackFrequency").toFloat() else null
                val specialAttackFrequency = if (obj.has("specialAttackFrequency")) obj.getInt("specialAttackFrequency") else null
                
                rawEntities.add(RawEntity(
                    type, x, y, w, h, checkpointIndex, linkId, duration, dx, dy, speed,
                    switchMode, gateType, inputLinkIds, outputLinkId, portalId,
                    name, bossType, health, phases, attackDensity, attackFrequency, specialAttackFrequency
                ))
            }
            Level(levelId, theme, rawEntities, levelWidth, levelHeight)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
