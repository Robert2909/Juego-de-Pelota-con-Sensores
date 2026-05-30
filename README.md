# Juego de Pelota con Sensores (Motor Nativo)

Un motor físico de lógica y habilidad construido de forma nativa para el sistema operativo Android utilizando el lenguaje Kotlin. El proyecto basa su control en la integración y lectura de los sensores de hardware del dispositivo móvil.

## Características del Motor

- **Físicas Basadas en Gravedad Vectorial**: El controlador principal procesa datos del acelerómetro inyectando vectores de aceleración de forma directa a un Motor de Físicas basado en colisiones AABB (Axis-Aligned Bounding Box), operando sin dependencia de frameworks gráficos externos.
- **Renderizado Nativo y Ciclo de Vida**: Utiliza componentes avanzados de la UI de Android (Jetpack Compose) combinados con un SurfaceView. El proyecto implementa un Game Loop clásico en un hilo independiente, asegurando la estabilidad del procesamiento lógico.
- **Inteligencia Artificial y Patrones de Comportamiento**: Incluye controladores lógicos encargados de operar fases de ataque, desplazamiento cinemático paramétrico y rutinas de evaluación de daño para entidades hostiles complejas.
- **Serialización e Ingesta de Datos**: Arquitectura agnóstica de niveles capaz de interpretar y renderizar mapas estructurados en formato JSON generados por utilidades externas.
