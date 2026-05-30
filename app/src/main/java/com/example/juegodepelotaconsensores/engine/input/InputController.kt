package com.example.juegodepelotaconsensores.engine.input

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import com.example.juegodepelotaconsensores.models.GameState

class InputController(private val gameState: GameState) : SensorEventListener {

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            // Mapeo estándar para modo horizontal sin forzar inversiones manuales
            val tx = event.values[1]
            val ty = event.values[0]
            gameState.tiltX = tx
            gameState.tiltY = ty
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
