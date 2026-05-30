import os

doc_path = r'C:\Users\Robert\.gemini\antigravity-ide\brain\c955e611-9606-46ce-9795-ea6d81ad3bca\DOCUMENTACION_MAESTRA_WEB.md'

content_to_append = """

## FASE 4: Álgebra Espacial y Matemáticas (`js/core/`)

Para mantener `utils.js` ligero, todas las operaciones pesadas de colisión, geometría y manipulación masiva se aislaron en la carpeta `js/core/`.

### `math.js`
Es la calculadora del juego. Alberga funciones puramente aritméticas o geométricas:
*   `getCanvasCoords()`: Traduce los clics del mouse desde la pantalla del navegador (Pixeles crudos) al sistema de coordenadas virtuales del Canvas (afectadas por el Zoom y el Paneo de la cámara).
*   `checkSmartGuides()`: Un escáner de proximidad. Revisa si el borde del bloque que estás moviendo está "cerca" (menos de 10 píxeles virtuales) de la fracción matemática del mapa (mitad, tercios, cuartos). Si lo está, "imanta" (hace *snap*) la coordenada del bloque a esa posición y dibuja la línea de guía de la fracción matemática descubierta usando la función auxiliar `gcd()` (Máximo Común Divisor).
*   `centerLevel()`: Calcula la Caja Delimitadora (Bounding Box) de todos los bloques existentes y ajusta el Paneo de la cámara para que el nivel quede centrado visualmente en tu monitor.

### `transform.js`
Es el controlador de mutaciones masivas para múltiples bloques seleccionados:
*   `alignSelection()`: Toma todos los objetos de `state.selectedIds` y los alinea arriba, al centro, abajo, izquierda o derecha respecto al bloque que funciona como "Ancla" (generalmente el primero o el último seleccionado).
*   `distributeSelection()`: Espacia uniformemente un conjunto de bloques calculando la diferencia entre el mínimo y máximo de sus ejes.
*   `bringSelectionToFront()` / `sendSelectionToBack()`: Reordena los elementos en el arreglo de `state.entities` alterando su Z-Index de dibujado.

---

## FASE 5: Serialización y Manejo de Archivos (JSON)

El motor exporta la información en un formato agnóstico e idéntico al que requiere el motor Android. Toda esta lógica vive en `js/core/serializer.js`.

### `getLevelJSON()`
Lee las propiedades del nivel actual (Ancho, Alto, Columnas, Tema) y mapea el arreglo `state.entities` purgando valores internos innecesarios o recalculando escalas. Devuelve un texto formateado listo para descarga.

### `updateJSON()`
Hace el proceso inverso. Actualiza la estructura del proyecto en tiempo real cargando el texto desde la ventana Modal de Importación o desde el LocalStorage del navegador, hidratando a `state.entities` con instancias frescas de la clase `Entity` (ver `entities.js`).

**Estructura del Archivo Generado (`.json`)**:
```json
{
    "levelId": 999,
    "theme": "volcano",
    "width": 3840,
    "height": 2160,
    "gridCols": 192,
    "gridRows": 108,
    "entities": [
        {
            "type": "wall",
            "x": 620,
            "y": 1040,
            "w": 320,
            "h": 80
        }
    ]
}
```
*Esta estructura de datos es parseada directamente por el sistema nativo de Android y dibujada 1:1 en el dispositivo móvil.*

---

## FASE 6: Controladores Reactivos del DOM (Subsistema UI)

El Canvas no es el único elemento visual. La barra lateral, los modales y las alertas están escritos en HTML/CSS, y su manipulación se confina a la carpeta `js/ui/`.

### `PropertiesPanel.js`
Actualiza el menú lateral izquierdo cuando haces clic en un objeto. 
*   **Bidireccionalidad**: Si haces clic en el canvas, este panel lee el `state.selectedIds` y dibuja las propiedades (X, Y, Ancho, Tipo). Si tú modificas los números en el panel HTML con el teclado, este módulo inyecta los valores de regreso a `state.entities` y pide un redibujado instantáneo del Canvas.

### `OSD.js` (On-Screen Display)
El responsable de esos bonitos avisos emergentes flotantes (Toasts).
Su arquitectura es **Auto-Lampiadora**: Antes de inyectar un nuevo div en el DOM, borra su propio contenedor (`container.innerHTML = ''`). De este modo, garantiza que si envías 10 alertas por segundo, el HTML no se congele acumulando nodos huérfanos.

### `rulers.js`
Controla las reglas graduadas visuales en los márgenes Izquierdo y Superior de la ventana. Ajusta los intervalos métricos (de 1/8, 1/4, 1/2) sincronizando sus divisiones con el factor `totalZoom` de la cámara actual.

---
> [!TIP]
> ¡La refactorización es un ciclo, no un destino! El proyecto actual goza de una salud excepcional, desacoplamiento y organización; consérvalo así evitando que `utils.js` o `editor.js` vuelvan a acumular tareas que no les corresponden.
"""

with open(doc_path, 'a', encoding='utf-8') as f:
    f.write(content_to_append)

print('Success')
