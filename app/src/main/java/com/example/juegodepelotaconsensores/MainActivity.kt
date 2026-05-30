package com.example.juegodepelotaconsensores

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.juegodepelotaconsensores.ui.theme.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configuración de pantalla completa (Immersive Mode)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            JuegoDePelotaConSensoresTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ModernDarkBg
                ) {
                    var showDebugMenu by rememberSaveable { mutableStateOf(false) }
                    
                    Box(modifier = Modifier.fillMaxSize()) {
                        LevelSelectionScreen { levelId ->
                            val intent = Intent(this@MainActivity, GameActivity::class.java).apply {
                                putExtra("LEVEL_ID", levelId)
                            }
                            startActivity(intent)
                        }

                        // Botón flotante para el menú debug temporal
                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            TextButton(onClick = { showDebugMenu = true }) {
                                Text("DEBUG", color = ModernSecondary.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                            }
                        }

                        if (showDebugMenu) {
                            var debugLevels by remember { mutableStateOf<List<String>>(emptyList()) }
                            LaunchedEffect(Unit) {
                                debugLevels = try {
                                    assets.list("levels/debug")?.filter { it.endsWith(".json") }?.map { it.removeSuffix(".json") } ?: emptyList()
                                } catch (e: Exception) {
                                    emptyList()
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.95f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(0.6f).fillMaxHeight(0.8f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(), 
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("DEBUG LEVELS", color = ModernSuccess, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                        TextButton(onClick = { showDebugMenu = false }) {
                                            Text("X", color = Color.White, fontSize = 20.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    androidx.compose.foundation.lazy.LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(debugLevels.size) { index ->
                                            val levelName = debugLevels[index]
                                            Button(
                                                onClick = {
                                                    val intent = Intent(this@MainActivity, GameActivity::class.java).apply {
                                                        putExtra("DEBUG_LEVEL_PATH", "levels/debug/$levelName.json")
                                                    }
                                                    startActivity(intent)
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = ModernAccent),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(levelName.uppercase(), color = ModernDarkBg, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LevelSelectionScreen(onLevelSelected: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 32.dp), // Reducido vertical de 48 a 32
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "JUEGO DE PELOTA",
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 6.sp,
            color = ModernAccent,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        
        Text(
            text = "CON SENSORES",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 8.sp,
            color = ModernSecondary,
            modifier = Modifier.padding(bottom = 32.dp) // Reducido de 64 a 32
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .widthIn(max = 500.dp) // Reducido max width para tarjetas más pequeñas
                .fillMaxWidth()
        ) {
            items(10) { index ->
                LevelCard(levelId = index + 1, onClick = { onLevelSelected(index + 1) })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "ELIJA UN NIVEL PARA EMPEZAR", // Texto más descriptivo
            fontSize = 11.sp,
            color = ModernSecondary.copy(alpha = 0.5f),
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun LevelCard(levelId: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp)) // Menos redondeado para ser más industrial
            .background(ModernSurface)
            .border(1.dp, GlassWhite, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (levelId < 10) "0$levelId" else "$levelId",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = ModernAccent
        )
    }
}