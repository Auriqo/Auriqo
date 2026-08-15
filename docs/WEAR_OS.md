# Wear OS playback surfaces

Auriqo exposes two complementary Wear OS experiences.

## Standard system controls

The phone media session publishes button preferences with Media3. Like remains an Auriqo custom
command. Shuffle and repeat use the standard `Player` commands, allowing Android, Bluetooth,
automotive and Wear surfaces to understand the actual player state instead of custom lookalikes.

The exact number and placement of buttons is chosen by each system surface. Auriqo publishes
preferences; it does not replace Samsung/Google's system media controller UI.

## Auriqo companion app and Tile

The `wear` module provides the branded Auriqo player and Tile: artwork, progress, title/artist,
play/pause, previous/next, like, shuffle and repeat. It uses vector controls and the outlined Auriqo
mark rather than emoji or the generic system-player layout.

The phone's Data Layer publisher lives in `app/src/gms`, so rich companion synchronization requires
the GMS phone variant. The FOSS phone variant still exposes the standard Media3 session controls,
but it does not publish Auriqo's private Data Layer payload.

The phone and Wear APKs must use the same `applicationId` and signing certificate. Both Auriqo
variants now use `com.auriqo.music` for the installed package; the Wear Kotlin namespace remains
`com.auriqo.music.wear` only to keep its source-level classes organized. Builds made before
`v1.0.3-alpha.1` used `com.auriqo.music.wear` and cannot participate in this Data Layer channel.

The version 2 payload includes media id, metadata, artwork, playing state, monotonic position,
duration, speed, repeat, shuffle, like/capabilities, boot count, a phone-process session UUID and a
monotonic state sequence. The Wear client reads existing DataItems on startup before listening for
changes and projects position locally between updates. Snapshot ordering uses boot count, session
start from `SystemClock.elapsedRealtime()` and sequence: a delayed old-process message is rejected,
while a real phone-process or device restart resets ordering explicitly without trusting wall time.

Current paths are:

- `/auriqo/now_playing`
- `/auriqo/command`

The historical `/auriqa/...` paths are accepted for one compatibility cycle and should be removed
only in a documented breaking release.

## Build and test

Build phone variants sequentially on memory-constrained machines:

```bash
./gradlew :app:compileUniversalFossDebugKotlin --no-daemon
./gradlew :app:compileUniversalGmsDebugKotlin --no-daemon
./gradlew :wear:testDebugUnitTest :wear:assembleDebug --no-daemon
```

The Wear APK is written to `wear/build/outputs/apk/debug/wear-debug.apk`. Install it only on an
authorized Wear OS device with `adb install -r`. A real phone running the matching GMS Auriqo build
is required for end-to-end Data Layer validation.
