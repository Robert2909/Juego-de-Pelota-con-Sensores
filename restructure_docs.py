import os
import shutil

# Paths
base_dir = r"c:\Users\Robert\Desarrollo-Juego-de-Pelota-con-Sensores"
web_dir = os.path.join(base_dir, "Constructor-de-Niveles-Web-para-Juego-de-Pelota-con-Sensores")
android_dir = os.path.join(base_dir, "Juego-de-Pelota-con-Sensores")
docs_folder = r"C:\Users\Robert\Desarrollo-Juego-de-Pelota-con-Sensores\Documentos de seguimiento\Documentación maestra"

web_source_doc = os.path.join(docs_folder, "Constructor-de-Niveles-Web-para-Juego-de-Pelota-con-Sensores.md")
android_source_doc = os.path.join(docs_folder, "Juego-de-Pelota-con-Sensores.md")

web_docs_dir = os.path.join(web_dir, "docs")
android_docs_dir = os.path.join(android_dir, "docs")

# Create docs directories
os.makedirs(web_docs_dir, exist_ok=True)
os.makedirs(android_docs_dir, exist_ok=True)

# Copy architecture docs
if os.path.exists(web_source_doc):
    shutil.copy2(web_source_doc, os.path.join(web_docs_dir, "ARCHITECTURE.md"))

if os.path.exists(android_source_doc):
    shutil.copy2(android_source_doc, os.path.join(android_docs_dir, "ARCHITECTURE.md"))

# Generate README for Web Project
web_readme = """# 🛠️ Constructor de Niveles Web - Juego de Pelota con Sensores

![LevelBuilder UI](docs/assets/banner.png)

Un editor de niveles web avanzado, modular y altamente reactivo construido puramente en HTML5, CSS3 (Vanilla) y JavaScript moderno.

Diseñado específicamente para generar ecosistemas compatibles 1:1 con el motor en Android de "Juego de Pelota con Sensores".

## ✨ Características Principales
- **Motor de Renderizado Custom (60FPS)**: No usa el DOM para renderizar niveles, sino un entorno `Canvas2D` súper optimizado con patrones de delegación visual.
- **Herramientas de Autoría Complejas**: Soporta Colocación (Pluma), Selección en Masa (Bounding Box), Borrado y Herramientas Especiales.
- **Álgebra y Precisión**: Guías Inteligentes ("Smart Guides" y Snapping magnético), centrado de cámara y transformaciones múltiples (Alinear y Distribuir).
- **Sistema de Nodos "Lógicos"**: Editor Visual que simula circuitos y compuertas lógicas (AND, OR, NOT) asignando colores hash dinámicos a las uniones.

## 🚀 Cómo Empezar (Desarrollo Local)

No requiere bundlers pesados, simplemente puedes levantar un servidor HTTP estático en la raíz del proyecto.

```bash
# Si tienes python instalado:
python -m http.server 8000
```
Abre `http://localhost:8000` en tu navegador.

## 🏗️ Arquitectura de Software
Este proyecto utiliza Inyección de Dependencias, el patrón Strategy y un State Manager descentralizado para evitar el "Callback Hell".

Para entender a fondo la magia de sus matemáticas y renderizadores modulares, por favor consulta la biblia técnica del proyecto:
👉 **[Documentación de Arquitectura de Software](docs/ARCHITECTURE.md)**
"""

with open(os.path.join(web_dir, "README.md"), "w", encoding="utf-8") as f:
    f.write(web_readme)


# Generate README for Android Project
android_readme = """# 📱 Juego de Pelota con Sensores (Motor Nativo)

![Game Banner](docs/assets/banner.png)

Un videojuego físico y de lógica construido de forma nativa para Android en **Kotlin**, haciendo uso de acelerómetros físicos (Hardware del celular), Canvas Nativo y una arquitectura de Software limpia.

## ✨ Características Principales
- **Físicas Basadas en Gravedad Real**: El Acelerómetro inyecta vectores de velocidad directamente sobre un Motor de Físicas AABB puro, sin depender de motores gráficos de terceros (Ni Unity, Ni Godot).
- **Jetpack Compose + SurfaceView**: Usa lo último de Android UI para la interfaz (HUD), pero conserva un Game Loop clásico en hilos separados para mantener 60 FPS estables.
- **Inteligencia Artificial y Jefes**: Contiene controladores IA (`BossAIController`) y sistemas de combate cinemático.
- **Serialización Agnóstica de Niveles**: Lee mapas en formato `.json` originados por un Creador de Niveles Web.

## 🚀 Cómo Empezar (Desarrollo Local)

1. Clona el repositorio.
2. Ábrelo en **Android Studio**.
3. Sincroniza Gradle.
4. Conecta un dispositivo físico de Android (se requiere hardware de giroscopio/acelerómetro para jugar).
5. Compila y Ejecuta.

## 🏗️ Arquitectura de Software
Este motor está compuesto por una estricta separación de entidades. Los dibujantes nunca alteran vectores, y las matemáticas del `PhysicsEngine` ignoran las capas visuales. 

Para un desglose profundo de cómo el ecosistema resuelve colisiones, señales lógicas de cables o evalúa la IA, consulta nuestra biblia técnica:
👉 **[Documentación de Arquitectura de Software (Motor)](docs/ARCHITECTURE.md)**
"""

with open(os.path.join(android_dir, "README.md"), "w", encoding="utf-8") as f:
    f.write(android_readme)

print("Repos successfully structured with READMEs and docs/ARCHITECTURE.md")
