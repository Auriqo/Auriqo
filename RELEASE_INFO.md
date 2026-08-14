# Auriqo v1.0.2-alpha

Prerelease pública de Auriqo, apuntada a `main`.

## Cambios destacados

- Rebranding completo de la aplicación con el ícono Q-orbit, el monograma A y el wordmark fijo en Cabinet Grotesk.
- Pantalla de inicio espejada con las secciones de YouTube Music.
- Sincronización de eliminación y reordenamiento de canciones en playlists.
- Reproductor de video con pantalla completa, Picture-in-Picture y selector de calidad.
- Integración de letras con BetterLyrics, Letras.com y otros proveedores configurables.
- Atribución de colaboradores y fecha de alta para canciones de playlists cuando la cuenta o el proxy disponen de esos datos.
- Mejoras de navegación, reproducción y compatibilidad con Android.

## Variantes

- `UniversalFossDebug`: variante FOSS universal utilizada para las pruebas públicas.
- `UniversalGmsDebug`: variante con Google Mobile Services y Cast cuando se configura Firebase.
- El nombre de paquete de la aplicación es `com.auriqa.music`.

## Instalación

Descargá el APK desde la [release `v1.0.2-alpha`](https://github.com/Auriqo/Auriqo/releases/tag/v1.0.2-alpha).

Las builds debug usan el keystore persistente de desarrollo documentado en
`SECURITY.md`; no representan una firma de distribución definitiva.

## Compilación

```
./gradlew :app:assembleUniversalFossDebug --no-daemon
```

El artefacto se genera en
`app/build/outputs/apk/universalFoss/debug/app-universal-foss-debug.apk`.
