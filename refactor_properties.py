import os

file_path = r'c:\Users\Robert\Desarrollo-Juego-de-Pelota-con-Sensores\Constructor-de-Niveles-Web-para-Juego-de-Pelota-con-Sensores\js\ui\PropertiesPanel.js'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# We will replace the block from "if (en.type === 'checkpoint') {" to "}// Múltiple selección o ninguna"
# We can find the start and end by regex or string split

import re

new_extra_properties = """                if (en.type === 'checkpoint') {
                    const group = document.createElement('div');
                    group.className = 'control-group';
                    group.style.marginTop = '10px';
                    group.innerHTML = `
                        <label>Índice (Orden)</label>
                        <input type="number" step="1" id="propCpIndex" class="styled-input" value="${en.checkpointIndex !== undefined ? en.checkpointIndex : 0}">
                    `;
                    extraPanel.appendChild(group);

                    document.getElementById('propCpIndex').addEventListener('input', (e) => {
                        en.checkpointIndex = parseInt(e.target.value) || 0;
                        updateJSON();
                    });
                } else if (en.type === 'switch') {
                    const groupMode = document.createElement('div');
                    groupMode.className = 'control-group';
                    groupMode.style.marginTop = '10px';
                    groupMode.innerHTML = `
                        <label>Modo de Activación</label>
                        <select id="propSwitchMode" class="styled-input" style="padding: 4px; border-radius: 4px; background: #252525; color: white; border: 1px solid #444; width: 100%;">
                            <option value="toggle" ${en.switchMode === 'toggle' ? 'selected' : ''}>Alternar (Toggle)</option>
                            <option value="hold" ${en.switchMode === 'hold' || en.switchMode === 'pressure' ? 'selected' : ''}>Mantener (Hold)</option>
                            <option value="latch" ${en.switchMode === 'latch' ? 'selected' : ''}>Permanente (Latch)</option>
                            <option value="pulse" ${en.switchMode === 'pulse' ? 'selected' : ''}>Impulso (Pulse)</option>
                        </select>
                    `;
                    extraPanel.appendChild(groupMode);

                    const group = document.createElement('div');
                    group.className = 'control-group';
                    group.style.marginTop = '10px';
                    group.innerHTML = `
                        <label>Canal de Salida</label>
                        <input type="text" id="propLinkId" class="styled-input" value="${en.linkId || ''}">
                    `;
                    extraPanel.appendChild(group);

                    document.getElementById('propSwitchMode').addEventListener('change', (e) => {
                        en.switchMode = e.target.value;
                        updateJSON();
                    });
                    document.getElementById('propLinkId').addEventListener('input', (e) => {
                        en.linkId = e.target.value || '';
                        updateJSON();
                    });
                } else if (en.type === 'gate') {
                    const row = document.createElement('div');
                    row.className = 'control-row';
                    row.style.marginTop = '10px';
                    row.innerHTML = `
                        <div class="control-group">
                            <label>Entrada (Link)</label>
                            <input type="text" id="propLinkId" class="styled-input" value="${en.linkId || ''}">
                        </div>
                        <div class="control-group">
                            <label>Duración (0=perm)</label>
                            <input type="number" step="0.1" id="propDuration" class="styled-input" value="${en.duration !== undefined ? en.duration : 0}">
                        </div>
                    `;
                    extraPanel.appendChild(row);

                    document.getElementById('propLinkId').addEventListener('input', (e) => {
                        en.linkId = e.target.value || '';
                        updateJSON();
                    });
                    document.getElementById('propDuration').addEventListener('input', (e) => {
                        en.duration = parseFloat(e.target.value) || 0;
                        updateJSON();
                    });
                } else if (en.type === 'moving_wall' || en.type === 'moving_hazard') {
                    const row1 = document.createElement('div');
                    row1.className = 'control-row';
                    row1.style.marginTop = '10px';
                    row1.innerHTML = `
                        <div class="control-group">
                            <label>Despl. X (dx)</label>
                            <input type="number" step="10" id="propDX" class="styled-input" value="${en.dx !== undefined ? en.dx : 0}">
                        </div>
                        <div class="control-group">
                            <label>Despl. Y (dy)</label>
                            <input type="number" step="10" id="propDY" class="styled-input" value="${en.dy !== undefined ? en.dy : 0}">
                        </div>
                    `;
                    extraPanel.appendChild(row1);
                    
                    const row2 = document.createElement('div');
                    row2.className = 'control-row';
                    row2.style.marginTop = '10px';
                    row2.innerHTML = `
                        <div class="control-group">
                            <label>Ciclo (segs)</label>
                            <input type="number" step="0.1" id="propSpeed" class="styled-input" value="${en.speed !== undefined ? en.speed : 2.0}">
                        </div>
                        <div class="control-group">
                            <label>Activación (Link)</label>
                            <input type="text" id="propLinkId" class="styled-input" value="${en.linkId || ''}">
                        </div>
                    `;
                    extraPanel.appendChild(row2);

                    document.getElementById('propDX').addEventListener('input', (e) => {
                        en.dx = parseFloat(e.target.value) || 0;
                        updateJSON();
                    });
                    document.getElementById('propDY').addEventListener('input', (e) => {
                        en.dy = parseFloat(e.target.value) || 0;
                        updateJSON();
                    });
                    document.getElementById('propSpeed').addEventListener('input', (e) => {
                        en.speed = parseFloat(e.target.value) || 2.0;
                        updateJSON();
                    });
                    document.getElementById('propLinkId').addEventListener('input', (e) => {
                        en.linkId = e.target.value || '';
                        updateJSON();
                    });
                } else if (en.type === 'logic_gate') {
                    if (en.gateType === 'NOT' && en.inputLinkIds && en.inputLinkIds.includes(',')) {
                        en.inputLinkIds = en.inputLinkIds.split(',')[0].trim();
                    }

                    const groupGateType = document.createElement('div');
                    groupGateType.className = 'control-group';
                    groupGateType.style.marginTop = '10px';
                    groupGateType.innerHTML = `
                        <label>Operación Lógica</label>
                        <select id="propGateType" class="styled-input" style="padding: 4px; border-radius: 4px; background: #252525; color: white; border: 1px solid #444; width: 100%;">
                            <option value="AND" ${en.gateType === 'AND' ? 'selected' : ''}>AND (Todos)</option>
                            <option value="OR" ${en.gateType === 'OR' ? 'selected' : ''}>OR (Al menos uno)</option>
                            <option value="NOT" ${en.gateType === 'NOT' ? 'selected' : ''}>NOT (Inversor)</option>
                        </select>
                    `;
                    extraPanel.appendChild(groupGateType);

                    const rowInputs = document.createElement('div');
                    rowInputs.className = 'control-row';
                    rowInputs.style.marginTop = '10px';
                    const inputsLabel = en.gateType === 'NOT' ? 'Entrada (Link)' : 'Entradas (Links)';
                    rowInputs.innerHTML = `
                        <div class="control-group">
                            <label id="labelInputLinks">${inputsLabel}</label>
                            <input type="text" id="propInputLinkIds" class="styled-input" value="${en.inputLinkIds || ''}">
                        </div>
                        <div class="control-group">
                            <label>Salida (Link)</label>
                            <input type="text" id="propOutputLinkId" class="styled-input" value="${en.outputLinkId || ''}">
                        </div>
                    `;
                    extraPanel.appendChild(rowInputs);

                    document.getElementById('propGateType').addEventListener('change', (e) => {
                        en.gateType = e.target.value;
                        if (en.gateType === 'NOT') {
                            if (en.inputLinkIds && en.inputLinkIds.includes(',')) {
                                en.inputLinkIds = en.inputLinkIds.split(',')[0].trim();
                            }
                            const inputField = document.getElementById('propInputLinkIds');
                            if (inputField) {
                                inputField.value = en.inputLinkIds || '';
                                document.getElementById('labelInputLinks').textContent = 'Entrada (Link)';
                            }
                        } else {
                            const inputField = document.getElementById('propInputLinkIds');
                            if (inputField) {
                                document.getElementById('labelInputLinks').textContent = 'Entradas (Links)';
                            }
                        }
                        updateJSON();
                    });
                    document.getElementById('propInputLinkIds').addEventListener('input', (e) => {
                        let val = e.target.value || '';
                        if (en.gateType === 'NOT' && val.includes(',')) {
                            val = val.split(',')[0].trim();
                            e.target.value = val;
                        }
                        en.inputLinkIds = val;
                        updateJSON();
                    });
                    document.getElementById('propOutputLinkId').addEventListener('input', (e) => {
                        en.outputLinkId = e.target.value || '';
                        updateJSON();
                    });
                } else if (en.type === 'wind_zone') {
                    const row = document.createElement('div');
                    row.className = 'control-row';
                    row.style.marginTop = '10px';
                    row.innerHTML = `
                        <div class="control-group">
                            <label>Fuerza X</label>
                            <input type="number" step="0.5" id="propFX" class="styled-input" value="${en.dx !== undefined ? en.dx : 0}">
                        </div>
                        <div class="control-group">
                            <label>Fuerza Y</label>
                            <input type="number" step="0.5" id="propFY" class="styled-input" value="${en.dy !== undefined ? en.dy : -1.5}">
                        </div>
                    `;
                    extraPanel.appendChild(row);
                    document.getElementById('propFX').addEventListener('input', (e) => { en.dx = parseFloat(e.target.value) || 0; updateJSON(); });
                    document.getElementById('propFY').addEventListener('input', (e) => { en.dy = parseFloat(e.target.value) || 0; updateJSON(); });
                } else if (en.type === 'speed_pad') {
                    const row = document.createElement('div');
                    row.className = 'control-row';
                    row.style.marginTop = '10px';
                    row.innerHTML = `
                        <div class="control-group">
                            <label>Boost X</label>
                            <input type="number" step="1" id="propBX" class="styled-input" value="${en.dx !== undefined ? en.dx : 15}">
                        </div>
                        <div class="control-group">
                            <label>Boost Y</label>
                            <input type="number" step="1" id="propBY" class="styled-input" value="${en.dy !== undefined ? en.dy : 0}">
                        </div>
                    `;
                    extraPanel.appendChild(row);
                    document.getElementById('propBX').addEventListener('input', (e) => { en.dx = parseFloat(e.target.value) || 0; updateJSON(); });
                    document.getElementById('propBY').addEventListener('input', (e) => { en.dy = parseFloat(e.target.value) || 0; updateJSON(); });
                } else if (en.type === 'timer') {
                    const row1 = document.createElement('div');
                    row1.className = 'control-row';
                    row1.style.marginTop = '10px';
                    row1.innerHTML = `
                        <div class="control-group">
                            <label>Entrada (Link)</label>
                            <input type="text" id="propLinkId" class="styled-input" value="${en.linkId || ''}">
                        </div>
                        <div class="control-group">
                            <label>Salida (Link)</label>
                            <input type="text" id="propOutputLinkId" class="styled-input" value="${en.outputLinkId || ''}">
                        </div>
                    `;
                    extraPanel.appendChild(row1);

                    const groupDur = document.createElement('div');
                    groupDur.className = 'control-group';
                    groupDur.style.marginTop = '10px';
                    groupDur.innerHTML = `
                        <label>Duración Temporizador (segs)</label>
                        <input type="number" step="0.1" id="propDuration" class="styled-input" value="${en.duration !== undefined ? en.duration : 2.0}">
                    `;
                    extraPanel.appendChild(groupDur);

                    document.getElementById('propLinkId').addEventListener('input', (e) => { en.linkId = e.target.value || ''; updateJSON(); });
                    document.getElementById('propOutputLinkId').addEventListener('input', (e) => { en.outputLinkId = e.target.value || ''; updateJSON(); });
                    document.getElementById('propDuration').addEventListener('input', (e) => { en.duration = parseFloat(e.target.value) || 2.0; updateJSON(); });
                } else if (en.type === 'portal') {
                    const groupPortal = document.createElement('div');
                    groupPortal.className = 'control-group';
                    groupPortal.style.marginTop = '10px';
                    groupPortal.innerHTML = `
                        <label>Número de Portal</label>
                        <input type="number" step="1" id="propCpIndex" class="styled-input" value="${en.checkpointIndex !== undefined ? en.checkpointIndex : 1}">
                    `;
                    extraPanel.appendChild(groupPortal);
                    document.getElementById('propCpIndex').addEventListener('input', (e) => { en.checkpointIndex = parseInt(e.target.value) || 1; updateJSON(); });
                } else if (en.type === 'boss') {
                    const groupBossType = document.createElement('div');
                    groupBossType.className = 'control-group';
                    groupBossType.style.marginTop = '10px';
                    groupBossType.innerHTML = `
                        <label>Estilo de Combate</label>
                        <select id="propBossType" class="styled-input" style="padding: 4px; border-radius: 4px; background: #252525; color: white; border: 1px solid #444; width: 100%;">
                            <option value="scatter" ${en.bossType === 'scatter' ? 'selected' : ''}>Dispersor Caótico</option>
                            <option value="tracker" ${en.bossType === 'tracker' ? 'selected' : ''}>Cazador Preciso</option>
                            <option value="spinner" ${en.bossType === 'spinner' ? 'selected' : ''}>Hélice Mortal</option>
                        </select>
                    `;
                    extraPanel.appendChild(groupBossType);

                    const row1 = document.createElement('div');
                    row1.className = 'control-row';
                    row1.style.marginTop = '10px';
                    row1.innerHTML = `
                        <div class="control-group">
                            <label>Daño (Input)</label>
                            <input type="text" id="propLinkId" class="styled-input" value="${en.linkId || ''}">
                        </div>
                        <div class="control-group">
                            <label>Nombre</label>
                            <input type="text" id="propBossName" class="styled-input" value="${en.name || ''}">
                        </div>
                    `;
                    extraPanel.appendChild(row1);

                    const row2 = document.createElement('div');
                    row2.className = 'control-row';
                    row2.style.marginTop = '10px';
                    row2.innerHTML = `
                        <div class="control-group">
                            <label>Vida (PV)</label>
                            <input type="number" step="1" id="propHealth" class="styled-input" value="${en.health !== undefined ? en.health : 5}">
                        </div>
                        <div class="control-group">
                            <label>Fases</label>
                            <input type="number" step="1" id="propPhases" class="styled-input" value="${en.phases !== undefined ? en.phases : 2}">
                        </div>
                    `;
                    extraPanel.appendChild(row2);

                    const row3 = document.createElement('div');
                    row3.className = 'control-row';
                    row3.style.marginTop = '10px';
                    row3.innerHTML = `
                        <div class="control-group">
                            <label>Velocidad</label>
                            <input type="number" step="1" id="propSpeed" class="styled-input" value="${en.speed !== undefined ? en.speed : 150}">
                        </div>
                        <div class="control-group">
                            <label>Densidad Atq.</label>
                            <input type="number" step="1" id="propAttackDensity" class="styled-input" value="${en.attackDensity !== undefined ? en.attackDensity : 3}">
                        </div>
                    `;
                    extraPanel.appendChild(row3);

                    const row4 = document.createElement('div');
                    row4.className = 'control-row';
                    row4.style.marginTop = '10px';
                    row4.innerHTML = `
                        <div class="control-group">
                            <label>Frecuencia Atq.</label>
                            <input type="number" step="0.1" id="propAttackFrequency" class="styled-input" value="${en.attackFrequency !== undefined ? en.attackFrequency : 2.0}">
                        </div>
                        <div class="control-group">
                            <label>Golpes x Especial</label>
                            <input type="number" step="1" id="propSpecialAttackFrequency" class="styled-input" value="${en.specialAttackFrequency !== undefined ? en.specialAttackFrequency : 3}">
                        </div>
                    `;
                    extraPanel.appendChild(row4);

                    document.getElementById('propLinkId').addEventListener('input', (e) => { en.linkId = e.target.value || ''; updateJSON(); });
                    document.getElementById('propBossName').addEventListener('input', (e) => { en.name = e.target.value || ''; updateJSON(); });
                    document.getElementById('propBossType').addEventListener('change', (e) => { en.bossType = e.target.value; updateJSON(); });
                    document.getElementById('propHealth').addEventListener('input', (e) => { en.health = parseInt(e.target.value) || 5; updateJSON(); });
                    document.getElementById('propPhases').addEventListener('input', (e) => { en.phases = parseInt(e.target.value) || 2; updateJSON(); });
                    document.getElementById('propSpeed').addEventListener('input', (e) => { en.speed = parseFloat(e.target.value) || 150; updateJSON(); });
                    document.getElementById('propAttackDensity').addEventListener('input', (e) => { en.attackDensity = parseInt(e.target.value) || 3; updateJSON(); });
                    document.getElementById('propAttackFrequency').addEventListener('input', (e) => { en.attackFrequency = parseFloat(e.target.value) || 2.0; updateJSON(); });
                    document.getElementById('propSpecialAttackFrequency').addEventListener('input', (e) => { en.specialAttackFrequency = parseInt(e.target.value) || 3; updateJSON(); });
                }"""

split1 = "if (en.type === 'checkpoint') {"
split2 = "            }\n        }\n    } else {\n        // Múltiple selección o ninguna"

parts = content.split(split1)
part1 = parts[0]
part2 = split1 + parts[1]

subparts = part2.split(split2)

final_content = part1 + new_extra_properties + "\n" + split2 + subparts[1]

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(final_content)

print('Properties panel successfully refactored into two columns.')
