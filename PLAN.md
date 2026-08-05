# Auriqo — Plan de Desarrollo

## Repositorio

- **Local**: `/home/sebas/proyectos/Despu-s-vemos` (el directorio conserva el nombre viejo)
- **Remote**: `git@github.com:Auriqo/Auriqo.git` (o `https://github.com/Auriqo/Auriqo`)
- **Rama activa**: `main`
- **Licencia**: GPL-3.0

## Contexto

Auriqo es un fork de [Echo Music](https://github.com/EchoMusicApp/Echo-Music) (GPLv3), un reproductor de música Android con streaming desde YouTube Music, letras sincronizadas, descargas offline y reconocimiento musical. Stack: Kotlin, Jetpack Compose, Hilt, Media3/ExoPlayer, Room, Coil.

El fork elimina toda la marca Echo y la reemplaza por **Auriqo**.

---

## Fase 0 — Rebrand ✅ (completada)

Lo ejecutado hasta ahora:

- **Package**: `iad1tya.echo.music` / `com.music.echo` → `com.auriqo.music`
- **Application ID**: `iad1tya.echo.music` → `com.auriqo.music`
- **Nombre de app**: Echo Music → Auriqo (`app_name.xml`, ~150 archivos .kt/.xml)
- **Módulo**: `echomusiccanvas/` → `auriqocanvas/`
- **Directorio fuente**: `com/music/echo/` → `com/auriqo/music/` (app + 7 submódulos)
- **Strings**: `echo_strings.xml` → `auriqo_strings.xml` (58 locales)
- **Drawables**: 8 drawables con "echo" → "auriqo" (ej. `ic_qs_echo_logo.png` → `ic_qs_auriqo_logo.png`)
- **Dominios**: `echomusic.fun` → placeholder `auriqo.app`, luego → `music.youtube.com` (share URLs)
- **Workflows CI**: eliminados pasos de Telegram/Discord; `EchoMusicBot` → `SoniqoBot`
- **Datos del dev anterior**: removidos `FUNDING.yml`, scripts personales, path `/Users/aditya/`, links a BMC/Patreon/UPI de `iad1tya`
- **Logo**: SVG nuevo en `assets/auriqo-logo.svg`, removidos `bmac.png`, `patreon3.png`, `upi.svg`, `telegram.png`, `discord.png`, `LMEB.gif`
- **README**: reescrito
- **GitHub**: repo creado en `Auriqo/Auriqo` (privado), sin upstream fork
- **Lyrics perf**: cambios de `perf/lyrics-loading-speed` (`e4f2dd2d`) ya están en main (timeout 6s, cache unificado, parsing fuera del main thread)

### Estado del token GitHub
El repo usa autenticación vía `gh` CLI con token PAT (classic). El token actual es `ghp_*` con scopes `admin:org, repo, workflow`. Está guardado en `~/.config/gh/hosts.yml`. Funciona para pushes. Fue rotado durante esta sesión (el anterior `gho_*` estaba expuesto).

### Ramas locales
```
main                     ← rama activa, todos los cambios commiteados y pusheados
perf/lyrics-loading-speed ← branch vieja, cambios ya aplicados en main, puede borrarse
```

---

## Roadmap

### Fase 1 — Íconos y recursos visuales propios
- Crear/obtener ícono de launcher (mipmap) — actualmente usa el de Echo
- Ícono de notificación
- Splash / welcome screen
- Feature graphic para Play Store / F-Droid

### Fase 2 — Limpiar features muertas
- **auriqocanvas/**: módulo entero depende de `canvas.echomusic.fun`, no existe más. Evaluar si se puede reemplazar con Tidal/Apple canvas o eliminar.
- **UptimeScreen.kt**: monitorea uptime de servicios de Echo, no aplica. Eliminar pantalla y referencias.
- **FundingProgressCard.kt / FundingRepository.kt**: barra de progreso de donaciones vía Buymeacoffee de iad1tya. Eliminar.
- **LosslessAPI.kt / LosslessContributeScreen.kt / LosslessContributeViewModel.kt**: feature de contribución de FLAC a base de datos comunitaria de Echo. Revisar si se adapta o se elimina.
- **PlayerExtractorScreen.kt**: antes "Echo Extractor", ver si la funcionalidad de cipher/token sigue siendo útil.

### Fase 3 — Settings limpio
- Eliminar entradas de settings que apuntan a features removidas
- Reordenar categorías del settings screen
- Evaluar settings de Discord RPC, Last.fm — ¿se mantienen?
- Unificar preferencias dispersas entre `DataStore` y `SharedPreferences`

### Fase 4 — Strings y locales
- `auriqo_strings.xml` tiene keys heredadas de Echo (nombres de features, marcas, agradecimientos)
- Revisar qué keys se usan realmente en código y eliminar las huérfanas
- 58 archivos de traducción: muchas traducciones parciales o desactualizadas
- Decidir si mantener multilenguaje o resetear a inglés con contribuciones nuevas

### Fase 5 — UI y tema propio
- Paleta de colores Auriqo (hoy usa los themes heredados de Echo)
- Tipografía: definir fuentes para la app (¿Cabinet Grotesk / Satoshi u otras?)
- Player UI: revisar colores del player, animaciones, mini-player flotante
- Evaluar temas built-in: liquid glass, glass effects, etc.

### Fase 6 — Revisar y fixear features existentes
- **Lyrics**: verificar todos los providers (YouTube, LrcLib, KuGou, BetterLyrics, etc.), sync, traducción AI
- **Downloads**: probar download manager (cola, pausa, resume, storage)
- **Music Recognition**: verificar ShazamKit / Vibra fingerprinting
- **Listen Together**: ¿el servidor (`soniqo-listen-together.example.com`) existe? Era `echomusic-listen-together.onrender.com`.
- **Spotify import**: probar flujo OAuth, mapeo de playlists
- **Discord RPC**: ¿funciona? ¿queremos mantenerlo?
- **Equalizador**: AxionEQ, presets, funcionalidad
- **Widgets**: music widget, playlist widget, recognizer widget

### Fase 7 — Build y dependencias
- Simplificar build variants (FOSS sin GMS vs GMS con Cast). ¿Ambos necesarios?
- Actualizar AGP, Kotlin, Compose Compiler, dependencias
- Limpiar dependencias no usadas en `libs.versions.toml`
- Revisar `gradle.properties` (FLOW_NEURO_API_KEY, etc.)

### Fase 8 — CI/CD
- Workflow `gradle.yml`: build + release APK en push a main / tag
- `sync-player-configs.yml`: sync de zemer-cipher. Revisar si sigue siendo útil.
- `codeql.yml`: análisis estático. Dejar.
- Firmado de release: configurar keystore y secrets en GitHub

### Fase 9 — Testing
- Tests unitarios: LyricsHelper, providers, cache, parser
- Tests de UI: pantallas principales (Home, Player, Search, Settings)
- Cobertura básica con JUnit + Compose Testing

### Fase 10 — Documentación
- README con badges reales (cuando el repo sea público)
- BUILD.md con instrucciones de compilación detalladas
- ARCHITECTURE.md con diagrama de módulos y decisiones de diseño
- CONTRIBUTING.md actualizado para Auriqo

### Fase 11 — Distribución
- Preparar metadata para F-Droid (build recipe, descripción)
- Preparar listing para Google Play Store
- Landing page o web de descarga (`auriqo.app` si se compra el dominio)

### Fase 12 — Privacy y compliance
- Política de privacidad real (qué datos se recolectan, cómo se usan)
- GDPR: consentimiento en primer launch, data collection disclosure
- Atribución de librerías open source (OSS licenses screen)
- Revisar `PRIVACY_POLICY.md` y `SECURITY.md` actuales

### Fase 13 — Performance
- Profiling de cold start
- Optimización de carga de imágenes (Coil cache, memory)
- Caché de red y offline first
- Reducir APK size (proguard, resources, ABI splits)

### Fase 14 — Release v1.0.0
- Version bump a `1.0.0`
- Changelog completo
- APK firmado (universal + ABI splits)
- Tag `v1.0.0` y GitHub Release
- Anuncio en canales

---

## Convenciones importantes

- **Antes de tocar cualquier archivo, leerlo completo y entender su contexto.**
- **No usar `sed` a ciegas.** Los replaces masivos rompieron strings en Fase 0 y hubo que repararlos manualmente.
- **Seguir el código existente** como guía de estilo (indentación, nombres, patrones).
- **Build variants**: FOSS y GMS tienen directorios `app/src/foss/` y `app/src/gms/` con override de clases. Tocar ambos si se modifica una clase con variante.
- **No commitear `.github/workflows/` sin tener el token de GitHub con scope `workflow`.**
- **El repo es privado**, no exponer URLs ni badges que asuman repo público.
- **Los cambios van a `main` directamente**, no crear branches innecesarios.
