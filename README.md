<div align="center">
  <h1>FxzMusic 🎵</h1>

  <p><strong>Un reproductor de música moderno para Android con streaming sin anuncios, letras sincronizadas en tiempo real, modo sin conexión y ecualizador avanzado.</strong></p>

  [![GitHub Release](https://img.shields.io/github/v/release/sebaxxfxz/FxzMusic?style=for-the-badge&color=6f42c1)](https://github.com/sebaxxfxz/FxzMusic/releases)
  [![GitHub Downloads](https://img.shields.io/github/downloads/sebaxxfxz/FxzMusic/total?style=for-the-badge&color=007ec6)](https://github.com/sebaxxfxz/FxzMusic/releases)
  [![GitHub Stars](https://img.shields.io/github/stars/sebaxxfxz/FxzMusic?style=for-the-badge&color=e3b341)](https://github.com/sebaxxfxz/FxzMusic/stargazers)
  [![GitHub Issues](https://img.shields.io/github/issues/sebaxxfxz/FxzMusic?style=for-the-badge&color=d9534f)](https://github.com/sebaxxfxz/FxzMusic/issues)
  [![License](https://img.shields.io/github/license/sebaxxfxz/FxzMusic?style=for-the-badge&color=28a745)](LICENSE)

</div>

---

## 📌 Tabla de Contenidos

- [Visión General](#-visión-general)
- [Características Principales](#-características-principales)
  - [Reproducción & Streaming](#reproducción--streaming)
  - [Letras Sincronizadas](#letras-sincronizadas)
  - [Ecualización & Audio](#ecualización--audio)
  - [Interfaz & Personalización](#interfaz--personalización)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Compilación e Instalación](#-compilación-e-instalación)
- [Especiales Agradecimientos](#-especiales-agradecimientos)
- [Estadísticas del Repositorio](#-estadísticas-del-repositorio)

---

## 🔍 Visión General

**FxzMusic** es una aplicación nativa para Android diseñada en **Kotlin** y **Jetpack Compose** que ofrece una experiencia auditiva fluida y sin interrupciones. Incorpora streaming en alta definición, soporte para letras dinámicas paso a paso, almacenamiento sin conexión y una interfaz elegante basada en Material Design 3.

---

## 🚀 Características Principales

### Reproducción & Streaming
- 🚫 **Sin Anuncios**: Streaming continuo y limpio.
- 📶 **Modo Sin Conexión**: Descarga canciones y listas completas para escucharlas offline.
- ⚡ **Media3 Pipeline**: Integración nativa con AndroidX Media3 y ExoPlayer para transiciones suaves.
- 🔄 **Segundo Plano**: Reproducción activa con la pantalla apagada o usando otras aplicaciones.

### Letras Sincronizadas
- 🎤 **Sincronización LRC & Rich Sync**: Letras en vivo línea por línea y palabra por palabra.
- 🎨 **Animaciones Dinámicas**: Efectos visuales adaptables al ritmo de la música.

### Ecualización & Audio
- 🎚️ **Ecualizador Gráfico**: Ajuste personalizado de frecuencias y preajustes de sonido.
- 🔊 **Procesamiento de Audio**: Efectos integrados y control de salida balanceado.

### Interfaz & Personalización
- 🎨 **Material Design 3**: Colores dinámicos adaptables y diseño moderno.
- 🌙 **Modo Oscuro & Claro**: Visualización óptima en cualquier entorno.
- 📱 **Mini Reproductor Táctil**: Controles rápidos y navegación por gestos.

---

## 🧱 Estructura del Proyecto

El código fuente está modularizado para garantizar un rendimiento óptimo:

| Módulo | Descripción |
| :--- | :--- |
| **[`:app`](app)** | Interfaz de usuario Compose, ViewModels, Base de datos Room y Servicio de Reproducción (`PlaybackService`). |
| **[`:innertube`](innertube)** | Extracción, motores de búsqueda y modelos de datos de música. |
| **[`:ytpipeline`](ytpipeline)** | Decodificación de audio, desofuscación de cifrados y pipeline de streaming. |

---

## 🛠️ Compilación e Instalación

### Requisitos Previos
- Android Studio Ladybug / Meerkat o superior
- JDK 17+
- Android SDK 34+

### Compilación desde Consola

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/sebaxxfxz/FxzMusic.git
   cd FxzMusic
   ```

2. **Compilar el APK de depuración:**
   ```bash
   ./gradlew :app:assembleDebug
   ```

El ejecutable estará disponible en: `app/build/outputs/apk/debug/app-debug.apk`

---

## Special Thanks

FxzMusic stands on the shoulders of several excellent open-source projects. Sincere thanks to:

| Project | Description |
| :--- | :--- |
| [Better Lyrics](https://github.com/better-lyrics/better-lyrics.git) | Lyrics enhancement and synchronization |
| [InnerTune](https://github.com/z-huang/InnerTune.git) | Foundational inspiration and architecture reference |
| [Echo Music](https://github.com/EchoMusicApp/Echo-Music) | Playback pipeline, UI inspiration, and features reference |

---

## 📈 Estadísticas del Repositorio

### Historial de Estrellas (Star History)

[![Star History Chart](https://api.star-history.com/svg?repos=sebaxxfxz/FxzMusic&type=Date)](https://star-history.com/#sebaxxfxz/FxzMusic&Date)

### Métricas del Proyecto

<div align="center">
  <img src="https://github-readme-stats.vercel.app/api/pin/?username=sebaxxfxz&repo=FxzMusic&theme=dark" alt="Estadísticas de FxzMusic"/>
</div>

---

<div align="center">
  <sub>Licenciado bajo la Licencia Abierta GPL-3.0. Desarrollado con ❤️ para los entusiastas de la música.</sub>
</div>
