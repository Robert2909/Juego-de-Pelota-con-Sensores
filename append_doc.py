import os

doc_path = r'C:\Users\Robert\.gemini\antigravity-ide\brain\c955e611-9606-46ce-9795-ea6d81ad3bca\DOCUMENTACION_MAESTRA_WEB.md'

content_to_append = """

## FASE 2: Motor de Renderizado Modular

El Editor no confía en el DOM para renderizar niveles complejos (sería demasiado lento para 30,000 bloques). En su lugar, utiliza un `CanvasRenderingContext2D` que se repinta hasta 60 veces por segundo en sincronía con la pantalla (`requestAnimationFrame`).

### Arquitectura de `renderer.js`

El controlador de vista central, `renderer.js`, tiene un bucle maestro estricto:
1. **Limpia y escala el lienzo**: Aplica factores de Zoom (`totalZoom`) y traslación de Cámara (`panX`, `panY`).
2. **Pinta la capa base**: Fondo de color temático y rejilla (Grid).
3. **Traza Conexiones Físicas y Lógicas**: Dibuja los cables (curvas de Bézier) entre interruptores y puertas basándose en propiedades compartidas de `linkId`.
4. **Delega el pintado de Entidades**: Recorre `state.entities` y, en lugar de saber cómo se dibuja cada bloque, simplemente llama a la colección modular inyectada.

### Inyección de Renderers (`js/renderers/types/`)

Para evitar un archivo con 2000 líneas de `if/else`, aplicamos **Inyección de Dependencias**. Existe una carpeta dedicada `types/` donde cada archivo sabe cómo dibujarse a sí mismo:
*   `boss.js`: Traza polígonos, ojos, escudos rojos y animaciones nativas.
*   `moving_wall.js`: Utiliza las matemáticas de tiempo (`Date.now()`) para dibujar previsualizaciones del bloque en movimiento sin que realmente se desplace en memoria.

En la cima del gestor, un diccionario carga todas las librerías gráficas, por ejemplo:
```javascript
const renderers = {
    'boss': drawBoss,
    'moving_wall': drawMovingWall,
    // ...
};
```

### Colorimetría Procedimental (`colorUtils.js`)

Uno de los mayores logros del proyecto es la generación automática de paletas para conexiones lógicas:
1. Toma el texto plano introducido por el usuario (`linkId`, ej. "Puerta01").
2. Transforma los caracteres ASCII en un Hash de encriptación numérico.
3. Lo inyecta en el valor Hue (H) del espectro de colores HSL.
*   **Resultado**: Un identificador textual genera un color único matemáticamente perfecto para teñir el switch, la puerta y el láser que los conecta, evitando el trabajo manual de paletas por parte del Diseñador.

---

## FASE 3: El Subsistema de Herramientas (Patrón Strategy)

Los clicks, arrastres y atajos de teclado no mutan el Canvas directamente. Se rigen por el patrón de diseño **Strategy** a través de `ToolManager.js`.

```mermaid
graph LR
    Mouse(Eventos de Mouse) -->|Intercepta| TH[input-handler.js]
    TH -->|Pasa al Controlador| TM[ToolManager.js]
    TM -->|Estrategia: BrushTool| BT[Pinta Bloques Múltiples]
    TM -->|Estrategia: SelectTool| ST[Lazo de Selección]
    TM -->|Estrategia: BlockTool| BLT[Pinta un Bloque Gigante]
```

### 1. `ToolManager.js` (El Cerebro Receptor)

Se encarga de resolver acciones agnósticas (acciones que suceden sin importar qué herramienta esté activa):
*   Arrastrar el mapa usando Rueda de Mouse o Botón Medio (Pan).
*   Zoom in/Zoom out con la Rueda.
*   Seleccionar a través de herramientas la que debe estar activa en este momento.

### 2. Clases de Herramientas (`Tool.js` y Derivados)

Todas las herramientas de construcción heredan de la clase base abstracta `Tool`. Esto significa que en el futuro puedes crear `LassoTool.js` y el motor la aceptará porque cumple con los tres contratos obligatorios:
*   `onMouseDown()`: Qué pasa cuando hago clic inicial.
*   `onMouseMove()`: Qué sucede mientras mantengo el clic y arrastro.
*   `onMouseUp()`: Qué mutación se efectúa en memoria cuando suelto el ratón.

**Ejemplo Práctico**: La herramienta `SelectTool.js` dibuja dinámicamente un cuadrado semitransparente sobre el Canvas al arrastrar (`state.tempRect`). Al dispararse `onMouseUp()`, ejecuta una búsqueda por colisión de Área (Bounding Box) contra todo el arreglo de entidades. Aquellas entidades que se sobrepongan al área pasan inmediatamente a poblar el array de `state.selectedIds`, y el redibujado de selección ocurre mágicamente en el siguiente frame sin lag.
"""

with open(doc_path, 'a', encoding='utf-8') as f:
    f.write(content_to_append)

print('Success')
