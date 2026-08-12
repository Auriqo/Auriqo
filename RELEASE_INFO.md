# Auriqo v1.0.0

Primer release de Auriqo, el reproductor de música basado en Echo Music v5.2.89.

### Qué trae
- **Rebranding completo**: nueva identidad Auriqo (package `com.auriqo.music`) sobre la base de Echo Music v5.2.89 con todas sus funciones.
- **Wear OS companion**: tile con branding Auriqo que muestra la canción actual (artwork, título, artista) con controles de play/pause, anterior/siguiente, like, shuffle y repeat desde el reloj. Incluye app base en el watch para la futura app completa.
- **Fix de playback**: recuperación robusta de streams de YouTube (n-transform fallback, cancelación de PoToken, invalidación de resoluciones inválidas).
- **Nuevo provider de letras**: Letras.com con búsqueda verificada y toggle en settings.
- **Fix de crashes**: guard del blur de lyrics tras API 31, doble unbind del MusicService, colisiones de keys en LazyColumn.

### Instalación
1. Instalar `Auriqo-1.0.0-Universal.apk` en el teléfono.
2. Instalar `Auriqo-Wear-1.0.0.apk` en el reloj (Wear OS 3+, vía adb o sideload).
3. Para el tile: long-press en la watch face → Add tile → Auriqo.

### Notas
- El wear app requiere que el teléfono y el reloj estén conectados por Bluetooth y Auriqo abierto (o en reproducción).
- Firma: esta build usa la firma debug persistente. La firma de release definitiva se configurará próximamente.
