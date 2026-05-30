# Documentación Maestra: Juego de Pelota con Sensores

Este documento es la referencia técnica oficial del proyecto "Juego de Pelota con Sensores". Describe a detalle la arquitectura del software, los patrones de diseño aplicados, el flujo de ejecución y documenta exhaustivamente todas las clases y funciones del sistema.

---

## 1. Arquitectura General del Proyecto

El proyecto ha evolucionado de un diseño monolítico (donde una sola vista manejaba físicas, dibujo y entrada) hacia una arquitectura moderna basada en la **Separación de Responsabilidades (Single Responsibility Principle)**, fuertemente inspirada en una variante ligera del patrón **Entity-Component-System (ECS)** y el patrón **MVC (Model-View-Controller)**.

El sistema se divide en **5 Pilares Fundamentales**:

1. **Estado y Modelos (`models/`)**: La única fuente de verdad. Almacena todos los datos crudos, coordenadas, booleanos y listas del juego en memoria. No tiene lógica de negocio.
2. **Motor de Físicas e IA (`engine/`)**: El "músculo". Recibe el estado actual, procesa las matemáticas puras (colisiones, inercia, trigonometría, inteligencia de enemigos) y muta el Estado. No sabe cómo se dibujan las cosas.
3. **Renderizado (`rendering/`)**: El "ojo". Lee el Estado y utiliza un `Canvas` de Android para pintar las cosas en pantalla (colores, formas, partículas, rotaciones). No altera posiciones ni variables de juego.
4. **Input (`engine/input/`)**: Los "sentidos". Lee los sensores de hardware del dispositivo (acelerómetro) y actualiza los vectores de inclinación en el Estado.
5. **Ciclo de Vida (`GameView` y `GameActivity`)**: El "director de orquesta". Inicializa las dependencias, orquesta el game loop (a 60 FPS aprox.) dictando en qué orden se ejecuta el Input, el Motor y el Renderizado.

---

## 2. Estructura de Directorios

El código fuente está organizado semánticamente para facilitar la escalabilidad:

```text
app/src/main/java/com/example/juegodepelotaconsensores/
├── GameActivity.kt       # Actividad principal (UI de Jetpack Compose y contenedor)
├── GameView.kt           # Vista personalizada (Game Loop y SurfaceView)
├── engine/               # Lógica de juego pura
│   ├── LevelManager.kt   # Parsea archivos JSON a objetos de dominio
│   ├── PhysicsEngine.kt  # Resuelve colisiones, inercia y arrastre
│   ├── SignalEvaluator.kt# Maneja la lógica Booleana de los Logic Gates (AND, OR, XOR)
│   ├── ThemeManager.kt   # Paletas de colores dinámicas (Neón, Bosque, Lava, etc.)
│   ├── ai/               # Lógica de enemigos
│   │   ├── BossAIController.kt   # Patrones de ataque y movimiento de jefes
│   │   └── ProjectileManager.kt  # Actualización vectorial de proyectiles
│   └── input/            # Entradas de usuario
│       └── InputController.kt    # Interfaz con el Acelerómetro de Android
├── models/               # Clases de Datos (Dominio)
│   ├── EntityModels.kt   # BossData, BoxData, GateData, etc.
│   ├── GameState.kt      # El almacén centralizado de memoria del juego activo
│   ├── GameTheme.kt      # Estructura de colores
│   ├── Particle.kt       # Datos matemáticos para chispas y explosiones
│   ├── PhysicsState.kt   # Sub-estado inyectado solo para las físicas
│   └── RawEntity.kt      # DTO utilizado para parsear el JSON crudo
└── rendering/            # Sistemas gráficos (Pintores)
    ├── BossRenderer.kt   # Dibuja al jefe, su barra de vida y aura
    ├── EnvironmentRenderer.kt # Dibuja paredes, compuertas, botones, viento, etc.
    ├── LogicGateRenderer.kt   # Dibuja los cables visuales (nodos lógicos)
    ├── ParticleManager.kt     # Orquesta el ciclo de vida gráfico de las partículas
    ├── PlayerRenderer.kt # Dibuja la pelota del jugador y su estela de movimiento
    └── UIRenderer.kt     # Dibuja el HUD interactivo, transiciones y superposiciones
```

---

## 3. El Corazón del Juego (Ciclo de Vida)

El flujo de ejecución del programa comienza en la interacción entre la Actividad de Android y el Game Loop personalizado.

### `GameActivity.kt`

Es el contenedor principal de Android. Cumple las siguientes funciones:

- Oculta las barras del sistema para lograr el modo inmersivo a pantalla completa.
- Fija la rotación de pantalla (`SCREEN_ORIENTATION_LOCKED`) para evitar que el giro físico del móvil recargue la app.
- Despliega una interfaz moderna construida con **Jetpack Compose**, la cual actúa como contenedor/fondo e implementa el HUD superpuesto de "Victoria" / "Nivel Completado" / "Debug".
- Inicializa el `SensorManager` de Android y, al momento de componer el entorno, instancia la clase `GameView` pasándole la ruta del nivel seleccionado.
- Conecta el hardware con la lógica registrando el `InputController` como *Listener* del Acelerómetro justo cuando el `GameView` nace.

### `GameView.kt`

Hereda de `SurfaceView` e implementa `Runnable`. Es el motor de latidos del juego.
Posee un Hilo (Thread) secundario dedicado exclusivamente al juego para no congelar la UI de Android.

**Su Game Loop (`run()`):**
La función `run()` se ejecuta infinitamente mientras el juego está activo, apuntando a ~60 Frames Por Segundo:

1. **Captura de Tiempo:** Calcula el `deltaTime` (dt) en relación a los milisegundos reales transcurridos para asegurar que la velocidad del juego no dependa de los hercios de la pantalla.
2. **Actualización Lógica (`updatePhysics()`):**
   - Genera una "fotografía" (`PhysicsState`) del estado actual y se la envía al `PhysicsEngine`.
   - Se procesan señales de puertas lógicas con `SignalEvaluator`.
   - Actualiza temporizadores gráficos (`ParticleManager.update()`).
3. **Renderizado (`draw()`):**
   - Bloquea el Canvas (`holder.lockCanvas()`).
   - Llama en orden a los *Renderers* pasándoles el Canvas y los datos necesarios:
     - `EnvironmentRenderer` -> `PlayerRenderer` -> `BossRenderer` -> `LogicGateRenderer` -> `UIRenderer`.
   - Libera el Canvas y lo envía a la pantalla gráfica (`holder.unlockCanvasAndPost()`).

> **Nota Arquitectónica:** `GameView` no contiene una sola operación de suma o resta referente a colisiones, ni sabe de qué color es la pelota. Su único trabajo es coordinar quién habla y en qué momento.

## 4. El Cerebro (Estado y Modelos)

El pilar fundamental de la reestructuración radica en el paquete `models/`. Aquí residen los datos crudos, divorciados totalmente de la lógica y la interfaz visual. Esto permite que el sistema sea testeable, escalable y fácilmente serializable (por ejemplo, para guardar partidas).

### 4.1. `GameState.kt` (Fuente Única de la Verdad)

Es la memoria viva del juego. Esta clase mantiene absolutamente todas las listas, diccionarios y coordenadas de una sesión de nivel activo.

**Componentes Principales:**

- **Atributos Físicos de Nivel**: `width`, `height`, `goal` (meta).
- **Atributos de Jugador**: Posición (`posX`, `posY`), velocidad (`velX`, `velY`), y aceleración entrante desde los sensores (`tiltX`, `tiltY`).
- **Listas de Entidades Estáticas y Dinámicas**:
  - `boxes` (Bloques), `windZones` (Zonas de viento), `speedPads` (Aceleradores), `portals` (Teletransportadores).
- **Mecanismos Lógicos (El Cerebro dentro del Cerebro)**:
  - `switches`, `gates`, `logicGates`, `timers` y el `signalBus` (Diccionario que mapea los IDs de las señales eléctricas y si están activas o apagadas).
- **Entidades Complejas**:
  - `activeBoss` (Los datos del Jefe) y `bossProjectiles` (Balas).
- **Variables de Flujo**:
  - Temporizadores de victoria y animación de muerte (`isExploding`, `explosionTime`).

### 4.2. `EntityModels.kt` y Encapsulamiento

Cada objeto dentro del juego es representado por una `data class` dedicada. Esto evita inflar un modelo masivo de entidad universal.

- Ejemplos de Entidades: `GateData`, `SwitchData`, `WindZoneData`, `SpeedPadData`, `TimerData`.
- **Encapsulamiento de Dominio:** A pesar de ser clases de datos puros, implementan funciones de auto-mutación si la lógica le pertenece exclusivamente a la entidad en sí.
  - *Ejemplo: `BossData.takeDamage()`*: Resta la vida, valida el temporizador de invulnerabilidad local y decide si `isDefeated = true` internamente, protegiendo a los controladores externos de modificar crudos booleanos sin querer.

### 4.3. `RawEntity.kt` (Data Transfer Object)

Clase utilitaria utilizada por la librería `Gson` al parsear los archivos `.json` de los niveles. Representa el esquema de la interfaz web que usa el creador. Todo recae temporalmente aquí y luego el `LevelManager` lo descompone hacia las entidades puras de `EntityModels.kt`.

### 4.4. `PhysicsState.kt` (El Contenedor de Contexto Limitado)

Para evitar pasar el enorme `GameState` al `PhysicsEngine` e inyectar acoplamiento excesivo, se creó `PhysicsState`. Es una "fotografía" o envoltorio temporal que el Game Loop ensambla cada fotograma (frame). Contiene solo las coordenadas necesarias para la colisión y variables inyectadas de "callbacks" locales (`onTriggerDeath`, `onLevelComplete`). Mantiene el motor de físicas seguro e independiente.

## 5. El Motor (Físicas, IA e Input)

Todo el trabajo algorítmico pesado, las matemáticas puras y la inteligencia del juego residen en el directorio `engine/`. Estas clases actúan como "servicios" inyectados al ciclo de vida principal.

### 5.1. `PhysicsEngine.kt` (El Colisionador AABB)

Es el archivo más intensivo matemáticamente de todo el proyecto. Trabaja de forma agnóstica; es decir, no le interesa de qué color es una caja, solo sabe dónde está y cómo detener a la pelota.

- **Fricción e Inercia:** Toma el `tiltX` / `tiltY` del jugador para inyectarle aceleración, aplicándole resistencia aerodinámica o fricción para evitar que ruede infinitamente.
- **Detección AABB (Axis-Aligned Bounding Box):**
  - Resuelve las colisiones contra todas las `boxes` y `gates` cerradas verificando la superposición geométrica entre Rectángulos.
  - Al colisionar, rebota la velocidad de la pelota invirtiendo el vector (`velX *= -0.5f`) de acuerdo con la elasticidad programada.
- **Interacción Estática/Dinámica:** Revisa el contacto del jugador contra portales, aceleradores (SpeedPads), zonas de viento, zonas letales, Checkpoints, temporizadores y botones (Switches). Emite eventos (Callbacks) cuando suceden hitos importantes.

### 5.2. Inteligencia Artificial (`engine/ai/`)

Extraído durante la reestructuración para aliviar al Motor Físico.

1. **`BossAIController.kt`**:
   - Controla las fases de los Jefes (ej. Fase 1: Escopeta, Fase 2: Ráfagas letales).
   - Movimiento autónomo: Emplea ondas senoidales (`sin()`) y cálculo vectorial con punto de anclaje para que el jefe flote fluidamente por su zona designada.
   - Resuelve el daño: Si su compuerta lógica asignada (ej. Link "boss_damage") está activada, le envía la orden `.takeDamage()` al jefe y desencadena animaciones.
2. **`ProjectileManager.kt`**:
   - Gestiona el *Spawn* (nacimiento) y el ciclo de actualización de `bossProjectiles`. Utiliza trigonometría básica (Seno y Coseno) junto con vectores direccionales (Normalizados) para dirigir láseres o ráfagas hacia el jugador.
   - Detecta si el jugador toca un misil para llamar a la rutina `onTriggerDeath()`.

### 5.3. Sistema de Lógica Booleana (`SignalEvaluator.kt`)

El juego cuenta con un sistema de redondos y cables inspirados en la electrónica digital.

- Cuando la pelota toca un interruptor (`Switch`), este envía un `true` al `GameState.signalBus`.
- El `SignalEvaluator` es un procesador de dependencias. Lee las compuertas lógicas (`LogicGateData` tipo `AND`, `OR`, `XOR`, `NOT`) y evalúa recursivamente si se cumple la condición para abrir una puerta pesada (`GateData`) o dañar al jefe.

### 5.4. `InputController.kt` (Lectura de Hardware)

Implementa la interfaz nativa de Android `SensorEventListener`. Al ser detectado un cambio en el sensor giroscópico/acelerómetro, extrae los ejes del hardware (compensando la rotación horizontal por defecto) y empuja las magnitudes de gravedad crudas directamente al `GameState`.

## 6. El Ojo (Sistemas de Renderizado)

La carpeta `rendering/` engloba todas las clases responsables de interactuar con el lienzo visual (Canvas de Android). Estas clases siguen una regla de oro: **Solo leen, nunca escriben**. Ningún Renderizador está autorizado para modificar coordenadas de juego; su única misión es interpretar el `GameState` y pintarlo con el `ThemeManager` correspondiente.

### 6.1. `PlayerRenderer.kt`

Encargado exclusivamente de dibujar el avatar del usuario.

- Dibuja el núcleo de la pelota (Radio).
- Implementa sombras y efectos de iluminación suave (`setShadowLayer`).
- Pinta la **Estela de Movimiento (Trail)** leyendo el historial de `trailPoints` y difuminando la opacidad (`alpha`) de las esferas más antiguas.

### 6.2. `EnvironmentRenderer.kt`

El dibujante más grande del ecosistema. Construye todo el mapa estático e interactivo (Cajas, Viento, Portales).

- Emplea objetos `Paint` preconfigurados para mantener una alta tasa de cuadros (FPS) sin re-instanciar objetos.
- **Portales**: Dibuja efectos giratorios (Arcos o Círculos continuos) en bucle calculados mediante los milisegundos del sistema.
- **Botones y Puertas (`Switches` y `Gates`)**: Interpreta si están activos o apagados para cambiar sus opacidades o dibujar compuertas con barras metálicas. Utiliza el temporizador de cierre inyectado desde el estado lógico para dibujar "radios de reloj" o anillos de cuenta atrás.

### 6.3. `BossRenderer.kt`

Dibuja el Jefe final y sus misiles.

- Renderiza múltiples capas geométricas superpuestas (Núcleo, Corazas Exteriores flotantes).
- Calcula pulsos de luces de Neón e interactúa con el `boss.entranceProgress` o `boss.health` para cambiar el grosor y color del enemigo de acuerdo con el daño.
- Dibuja el haz de láser u ondas expansivas cuando el jefe ataca.

### 6.4. `LogicGateRenderer.kt`

Encargado de renderizar la "circuitería" invisible del nivel.

- Solo es visible en Niveles Debug o si se activa el modo de desarrollador localmente.
- Traza líneas de `Paint` (Pincel punteado) desde cada botón (`Switch`) hacia sus Compuertas (`Gate`) o sus operadores Lógicos intermedios (Cajas OR, AND).
- Los cables se iluminan con un efecto de Neón si la energía (señal lógica) está fluyendo por ellos a través del `signalBus`.

### 6.5. `UIRenderer.kt`

Se pinta al final del Ciclo de Renderizado para superponerse siempre encima del mapa.

- Interfaz Transitoria: Dibuja las cortinas negras y sus difuminados cuando cargas un nivel o mueres.
- Barra de Vida (HUD): Calcula y suaviza (`Lerp` o Interpolación) una barra masiva en la pantalla cuando un Jefe entra en escena, animando destellos blancos en el daño.

### 6.6. `ParticleManager.kt`

Un administrador de un `Pool` de Partículas.

- Al instanciar nuevas chispas, polvo al chocar, explosiones de muerte o daño al jefe, este manager escupe docenas de mini círculos con vectores de velocidad aleatorios dispersos hacia todas direcciones.
- Posee un método `update()` que encoge el radio y aumenta la transparencia de cada partícula en cada fotograma hasta que desaparecen de la memoria, permitiendo efectos visuales orgánicos.

## 7. Parseo y Ecosistema de Niveles (Conexión JSON)

Una de las grandes fortalezas del "Juego de Pelota con Sensores" es su ecosistema disociado. El motor de Android no necesita recompilarse para añadir o probar un nivel nuevo. Toda la arquitectura se basa en la ingesta de archivos `.json` que pueden ser producidos de manera externa por el Constructor de Niveles Web.

### 7.1. Esquema del JSON (El Contrato Backend-Frontend)

Los archivos alojados en `app/src/main/assets/levels/` (incluyendo la carpeta `debug/`) se adhieren a un esquema estricto. Cada archivo contiene:

- `width` y `height`: Dimensiones del nivel estipuladas por el editor web.
- `goal`: El rectángulo que representa la meta de salida.
- `theme`: String con el nombre del tema ("Neón", "Hielo", "Lava", etc.).
- `entities`: Un arreglo (Array) masivo de objetos heterogéneos. Cada objeto cuenta obligatoriamente con los atributos `id`, `type`, `x`, `y`, `width` y `height`. Atributos adicionales (como `inputLinkId` o `bossType`) dependen estrictamente del tipo de entidad.

### 7.2. `LevelManager.kt`

Este es el único componente que interactúa con el sistema de archivos (Storage/Assets) de Android para convertir el JSON en variables.

1. **Lector Raw (`Gson`)**: Lee el archivo local de texto plano y lo convierte todo en un objeto masivo de mapeo: `RawEntity`. En este punto los objetos están crudos y sin verificar.
2. **Escalamiento Responsivo (Scaling)**: Antes de poblar la memoria, `LevelManager` calcula un factor matemático. Adapta las dimensiones de la coordenada `X, Y` del Editor Web (usualmente fijas a la resolución del navegador) a los pixeles reales físicos de la pantalla del celular activo. Así asegura que una caja a la mitad de la pantalla web, esté a la mitad de cualquier pantalla móvil sin importar el modelo.
3. **Distribución de Estado (`GameState`)**: Una vez calculadas las físicas base y coordenadas corregidas, el `LevelManager` utiliza estructuras `when(entity.type)` para inyectar cada objeto en su lista correspondiente dentro de `GameState` (ej: Transforma un objeto tipo `wind_zone` al data class `WindZoneData` nativo).

---

## 8. Conclusión del Proyecto y Escalabilidad

Con la arquitectura aquí detallada, se ha garantizado:

- **Ausencia de God Objects**: `GameView` ya no domina la lógica de todo el programa.
- **Flujo Unidireccional Predictible**: Las matemáticas siempre se calculan antes que la pintura de gráficos, eliminando parpadeos o errores visuales.
- **Inyección de Componentes Pluggables**: El sistema de sensores (`InputController`), el evaluador de cables (`SignalEvaluator`) y los Jefes (`BossAIController`) son módulos conectados a demanda, demostrando un verdadero encapsulamiento orientado a objetos.

> *Fin del Documento Maestro.*

---

## ANEXO TÉCNICO: Referencia Detallada de Clases y Funciones (API)

Este anexo lista granularmente las funciones, firmas y responsabilidades matemáticas de cada clase dentro de la aplicación.

### A. Ciclo de Vida y Entorno

#### `GameActivity`

Actúa como punto de entrada y manejador de Jetpack Compose.

- `onCreate(savedInstanceState: Bundle?)`: Oculta las barras del sistema (Immersive Mode), lee los `Intent` para identificar si se debe cargar un nivel normal o debug, e inicializa el `GameView` y la UI de Compose (HUD de victoria).
- `getLevelsCount(): Int`: Explora la carpeta `assets/levels` buscando archivos de la forma `level_X.json` para contar cuántos niveles existen y permitir el avance dinámico.
- `onResume()` / `onPause()`: Gestionan la subscripción y des-subscripción del `InputController` al acelerómetro para ahorrar batería.

#### `GameView`

- `run()`: El *Game Loop* infinito. Mide el `deltaTime` mediante `System.currentTimeMillis()` y ejecuta secuencialmente `updatePhysics()` y `draw()`.
- `updatePhysics()`: Compila las variables vivas del `GameState` en un objeto de solo lectura temporal (`PhysicsState`) y se lo inyecta a los motores (`PhysicsEngine`, `SignalEvaluator`, `ParticleManager`).
- `draw(canvas: Canvas)`: Aplica color de fondo y ejecuta en cascada los renderizadores.
- `loadLevel(levelId: Int)` / `loadDebugLevel(path: String)`: Llama al `LevelManager` e inicializa el estado limpio de un nuevo escenario.
- `processLevelScaling()`: Calcula los ratios físicos entre las medidas del nivel original JSON y la pantalla del dispositivo Android.
- `saveCheckpointState()` / `triggerDeath()` / `resetBall()`: Controladores de respawn. Al morir, restauran las coordenadas y estado del último checkpoint activo.

### B. Motores y Algoritmia (`engine/`)

#### `PhysicsEngine`

El motor físico vectorial 2D.

- `update(state: PhysicsState, dt: Float)`:
  1. **Fricción**: `state.velX *= (1 - friction * dt)`.
  2. **Inclinación (Sensores)**: Suma `tiltX` y `tiltY` a las velocidades si el jugador no ha muerto.
  3. **Velocidad Terminal**: Restringe la velocidad máxima mediante una magnitud vectorial límite (`length = sqrt(velX^2 + velY^2)`).
  4. **Zonas Físicas**: Aplica sumas de vectores para `WindZones` (Viento) y multiplicadores directos para `SpeedPads` (Aceleradores).
  5. **Colisiones AABB**: Para cada pared geométrica (cajas, compuertas cerradas), compara los bordes superpuestos (overlap) y expulsa la pelota por el eje de menor penetración, invirtiendo la velocidad en ese eje multiplicada por el factor de rebote (`-0.5f`).

#### `LevelManager`

- `loadLevel(context, levelId, editorWidth, editorHeight): GameState`
- `loadDebugLevel(context, path, editorWidth, editorHeight): GameState`
Ambas funciones leen un `InputStream` local, usan la librería `Gson` para des-serializar a `RawEntity`, y luego iteran la lista poblando los arreglos tipados del `GameState`.

#### `SignalEvaluator`

- `evaluateSignals(state: PhysicsState, dt: Float)`:
  Limpia el `signalBus`. Itera primero sobre los botones activados (`Switches`), luego resuelve iterativamente las Compuertas Lógicas (AND: requiere todas sus entradas True; OR: requiere al menos una; XOR: requiere un número impar de Trues).

### C. Inteligencia Artificial (`engine/ai/`)

#### `BossAIController`

- `update(boss: BossData, state: PhysicsState, dt: Float, floatY: Float)`:
  Controla los patrones.
  - *Seno Vectorial*: Actualiza el desplazamiento vertical del jefe mediante `floatY = sin(System.currentTimeMillis() * velocidad) * amplitud`.
  - *Recepcion de Daño*: Evalúa el `boss.takeDamage()`. Si recibe daño, invoca el disparador de partículas.
- `checkDefeatedState(...)`: Si la salud cae a 0, detiene los proyectiles y comienza la animación cinemática de explosiones encadenadas.

#### `ProjectileManager`

- `update(projectiles, activeBoss, state, dt)`:
  Desplaza cada proyectil existente sumando `x += vx * dt` y `y += vy * dt`.
  Llama matemáticamente a `RectF.intersect()` para revisar si el radio del láser/proyectil colisiona matemáticamente con la `AABB` del jugador.

### D. Entidades de Dominio (`models/EntityModels.kt`)

#### `BossData`

- `takeDamage(): Boolean`: Función interna. Valida que `currentTimeMillis() - lastDamageTime > 1000ms`. Si es verdadero, decrementa la salud, actualiza el reloj interno y valida muerte. Previene recibir múltiples daños en el mismo frame.

#### `SwitchData`, `GateData`, `TimerData`

Almacenan información cruda de estado (ej: `var isToggleActive: Boolean = false`). Las puertas tienen `openTimer` para calcular el progreso de sus barras de metal al cerrarse.

### E. Renderizadores (`rendering/`)

Todos exponen una única función pública: `draw(canvas, state, theme, ...)`

- **`PlayerRenderer.draw`**: Dibuja la pelota del jugador. Actualiza internamente la opacidad de los rastros pasados calculando `alpha -= decay * dt`.
- **`EnvironmentRenderer.draw`**: Dibuja `RectF` directamente. Utiliza los flags temporales para dibujar efectos circulares rotatorios (Arcos animados de portales).
- **`UIRenderer.draw`**: Renderiza la salud del jefe mediante `Lerp` (Linear Interpolation). Dibuja el fondo de la barra basado en el daño máximo, y un segundo rectángulo intermedio de color blanco que simula impacto visual asimétrico.
- **`ParticleManager.update / draw`**: Orquesta entidades visuales descartables que viven pocos segundos. Usa cálculos de Euler explícitos simples (`x += vx * dt`) para expandir esquirlas.
