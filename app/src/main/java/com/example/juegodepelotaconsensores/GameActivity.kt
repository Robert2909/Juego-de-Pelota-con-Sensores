package com.example.juegodepelotaconsensores

import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.juegodepelotaconsensores.ui.theme.JuegoDePelotaConSensoresTheme

import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.BorderStroke
import com.example.juegodepelotaconsensores.ui.theme.*

class GameActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    
    // Estados para la inclinación (normales, sin recomposición de Compose para evitar lag)
    private var tiltX = 0f
    private var tiltY = 0f

    private var gameStatus by mutableStateOf("PLAYING") // "PLAYING", "WON", "FAILED"
    private var currentLevelId by mutableIntStateOf(1)
    private var gameView: GameView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Immersive Mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        currentLevelId = intent.getIntExtra("LEVEL_ID", 1)
        val debugLevelPath = intent.getStringExtra("DEBUG_LEVEL_PATH")
        
        // Bloquear la orientación actual (fijar si es landscape o reverse landscape)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val rememberedGameView = androidx.compose.runtime.remember {
                GameView(context).apply {
                    if (debugLevelPath != null) {
                        loadDebugLevel(debugLevelPath)
                    } else {
                        loadLevel(currentLevelId)
                    }
                    onLevelComplete = { gameStatus = "WON" }
                    onLevelFailed = { gameStatus = "FAILED" }
                    gameView = this
                }
            }
            
            JuegoDePelotaConSensoresTheme {
                Box(modifier = Modifier.fillMaxSize().background(ModernDarkBg)) {
                    AndroidView(
                        factory = { rememberedGameView },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Overlay de Victoria Moderno (Solo para WON)
                    if (gameStatus == "WON") {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Text(
                                    text = "COMPLETADO",
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = 12.sp,
                                    color = ModernSuccess
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                     text = if (debugLevelPath != null || currentLevelId == 999) "NIVEL DEBUG" else "NIVEL $currentLevelId",
                                     fontSize = 14.sp,
                                     color = ModernSecondary,
                                     letterSpacing = 4.sp
                                 )

                                 Spacer(modifier = Modifier.height(64.dp))
                                 
                                 Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                     val totalLevels = getLevelsCount()
                                     if (debugLevelPath != null || currentLevelId == 999) {
                                         // En nivel debug, permitimos volver a jugar el mismo nivel de pruebas
                                         Button(
                                             onClick = { 
                                                 if (debugLevelPath != null) {
                                                     gameView?.loadDebugLevel(debugLevelPath)
                                                 } else {
                                                     gameView?.loadLevel(999)
                                                 }
                                                 gameStatus = "PLAYING"
                                             },
                                             colors = ButtonDefaults.buttonColors(containerColor = ModernAccent),
                                             shape = RoundedCornerShape(4.dp)
                                         ) {
                                             Text("VOLVER A JUGAR", color = ModernDarkBg, fontWeight = FontWeight.Bold)
                                         }
                                     } else if (currentLevelId < totalLevels) {
                                         OutlinedButton(
                                             onClick = { 
                                                 gameView?.loadLevel(currentLevelId)
                                                 gameStatus = "PLAYING"
                                             },
                                             border = BorderStroke(1.dp, ModernAccent),
                                             shape = RoundedCornerShape(4.dp)
                                         ) {
                                             Text("VOLVER A JUGAR", color = ModernAccent)
                                         }

                                         Button(
                                             onClick = { 
                                                 currentLevelId++
                                                 gameView?.loadLevel(currentLevelId)
                                                 gameStatus = "PLAYING"
                                             },
                                             colors = ButtonDefaults.buttonColors(containerColor = ModernAccent),
                                             shape = RoundedCornerShape(4.dp)
                                         ) {
                                             Text("SIGUIENTE", color = ModernDarkBg, fontWeight = FontWeight.Bold)
                                         }
                                     } else {
                                         // Final del juego (Último nivel detectado)
                                         Button(
                                             onClick = { 
                                                 currentLevelId = 1
                                                 gameView?.loadLevel(1)
                                                 gameStatus = "PLAYING"
                                             },
                                             colors = ButtonDefaults.buttonColors(containerColor = ModernAccent),
                                             shape = RoundedCornerShape(4.dp)
                                         ) {
                                             Text("REINICIAR TODO", color = ModernDarkBg, fontWeight = FontWeight.Bold)
                                         }
                                     }
                                 }

                                Spacer(modifier = Modifier.height(16.dp))

                                TextButton(onClick = { finish() }) {
                                    Text(
                                        "MENU PRINCIPAL",
                                        color = ModernSecondary,
                                        fontSize = 12.sp,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        }
                    }

                    // Botón atrás discreto
                    IconButton(
                        onClick = { finish() },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar",
                            tint = ModernSecondary.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    private fun getLevelsCount(): Int {
        return try {
            assets.list("levels")?.count { it.startsWith("level_") && it.endsWith(".json") } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { acc ->
            sensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            // Mapeo estándar para modo horizontal sin forzar inversiones manuales
            val tx = event.values[1]
            val ty = event.values[0]
            tiltX = tx
            tiltY = ty
            gameView?.let {
                it.gameState.tiltX = tx
                it.gameState.tiltY = ty
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
