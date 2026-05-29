package com.example.juegodepelotaconsensores.engine

import android.graphics.RectF
import com.example.juegodepelotaconsensores.models.SwitchData
import com.example.juegodepelotaconsensores.models.LogicGateData
import com.example.juegodepelotaconsensores.models.BoxData
import com.example.juegodepelotaconsensores.models.TimerData

object SignalEvaluator {

    fun evaluate(
        dt: Float,
        posX: Float,
        posY: Float,
        radius: Float,
        switches: List<SwitchData>,
        boxes: List<BoxData>,
        logicGates: List<LogicGateData>,
        timers: List<TimerData>,
        signalBus: MutableMap<String, Boolean>,
        triggerSwitchAnimation: (Float, Float) -> Unit
    ) {
        signalBus.clear()

        val ballRect = RectF(posX - radius, posY - radius, posX + radius, posY + radius)
        
        for (sw in switches) {
            if (sw.visualTimer > 0f) {
                sw.visualTimer -= dt / 60f
            }

            var isTouched = RectF.intersects(sw.rect, ballRect)
            if (!isTouched) {
                for (box in boxes) {
                    if (RectF.intersects(sw.rect, box.rect)) {
                        isTouched = true
                        break
                    }
                }
            }

            sw.isHoldActive = isTouched

            if (isTouched) {
                if (!sw.wasTouchedLastFrame) {
                    if (sw.switchMode == "toggle") {
                        val targetState = !sw.isToggleActive
                        for (otherSw in switches) {
                            if (otherSw.outputLinkId == sw.outputLinkId && otherSw.switchMode == "toggle") {
                                otherSw.isToggleActive = targetState
                            }
                        }
                    } else if (sw.switchMode == "latch") {
                        sw.isToggleActive = true
                    } else if (sw.switchMode == "pulse") {
                        sw.isToggleActive = true
                    }
                    sw.visualTimer = 0.4f
                    triggerSwitchAnimation(sw.rect.centerX(), sw.rect.centerY())
                } else {
                    if (sw.switchMode == "pulse") {
                        sw.isToggleActive = false
                    }
                }
                sw.wasTouchedLastFrame = true
            } else {
                if (sw.switchMode == "pulse") {
                    sw.isToggleActive = false
                }
                sw.wasTouchedLastFrame = false
            }
        }

        for (sw in switches) {
            if (sw.outputLinkId.isNotBlank()) {
                val swValue = when (sw.switchMode) {
                    "toggle", "latch", "pulse" -> sw.isToggleActive
                    "hold", "pressure" -> sw.isHoldActive
                    else -> sw.isToggleActive
                }
                signalBus[sw.outputLinkId] = (signalBus[sw.outputLinkId] == true) || swValue
            }
        }

        for (timer in timers) {
            val isInputActive = timer.inputLinkId.isNotBlank() && (signalBus[timer.inputLinkId] == true)
            
            // Iniciar o reiniciar el temporizador al recibir cualquier señal (flanco de subida)
            if (isInputActive && !timer.wasInputActiveLastFrame) {
                timer.timeLeft = timer.duration
                timer.isCounting = true
            }
            
            if (timer.isCounting) {
                timer.timeLeft -= dt / 60f
                if (timer.timeLeft <= 0f) {
                    timer.timeLeft = 0f
                    timer.isCounting = false
                }
            }
            
            timer.wasInputActiveLastFrame = isInputActive
            
            // Emitir señal de salida mientras el temporizador esté activo contando
            if (timer.isCounting && timer.outputLinkId.isNotBlank()) {
                signalBus[timer.outputLinkId] = true
            }
        }

        val baseSignals = signalBus.toMap()
        var logicChanged = true
        var logicAttempts = 0
        while (logicChanged && logicAttempts < 10) {
            logicChanged = false
            logicAttempts++
            
            val nextBus = baseSignals.toMutableMap()
            for (lg in logicGates) {
                if (lg.outputLinkId.isBlank()) continue
                
                val inputs = lg.inputLinkIds.map { signalBus[it] == true }
                val result = when (lg.type) {
                    "AND" -> inputs.isNotEmpty() && inputs.all { it }
                    "OR" -> inputs.any { it }
                    "NOT" -> if (inputs.isNotEmpty()) !inputs[0] else false
                    else -> false
                }
                
                nextBus[lg.outputLinkId] = (nextBus[lg.outputLinkId] == true) || result
            }
            
            if (nextBus != signalBus) {
                signalBus.clear()
                signalBus.putAll(nextBus)
                logicChanged = true
            }
        }

        for (sw in switches) {
            if (sw.switchMode == "latch" && sw.outputLinkId.isNotBlank()) {
                if (signalBus[sw.outputLinkId] == true) {
                    sw.isToggleActive = true
                }
            }
        }
    }
}
