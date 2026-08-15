# Wear OS playback surfaces

Auriqo exposes two complementary Wear OS experiences.

## Standard system controls

The phone media session publishes button preferences with Media3. Like remains an Auriqo custom
command. Shuffle and repeat use the standard `Player` commands on the phone, allowing Android,
Bluetooth, automotive and Wear surfaces to understand the actual player state instead of custom
lookalikes.

The exact number and placement of buttons is chosen by each system surface. Auriqo does not try to
draw over Samsung/Google's controller. The Wear build now also publishes a local `MediaSession`
proxy, so the system controller sees Auriqo's metadata and actions as the active local session
instead of falling back to an anonymous generic remote session.

## Auriqo companion app and Tile

The `wear` module provides the branded Auriqo player and Tile: artwork, progress, title/artist,
play/pause, previous/next, like, shuffle and repeat. It uses vector controls and the outlined Auriqo
mark rather than emoji or the generic system-player layout.

The phone's Data Layer publisher lives in `app/src/gms`, so rich companion synchronization requires
the GMS phone variant. The FOSS phone variant still exposes the standard Media3 session controls,
but it does not publish Auriqo's private Data Layer payload.

`AuriqoDataLayerListenerService` receives playback DataItems while the Wear Activity is closed.
`AuriqoMediaSessionService` mirrors the state into a local platform session and forwards system
commands over `/auriqo/command`. The first playback update can therefore surface Auriqo controls
without opening the app. The GMS phone publisher also sends a periodic state heartbeat while
playing so a newly installed Wear app does not have to wait for a song transition.

The local proxy owns a low-importance media notification while a track exists. This is required
to keep the session alive under modern Wear OS background limits; tapping it opens the branded
companion UI, but opening that UI is not required for playback controls to work.

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

Commands currently forwarded by the Wear proxy are play/pause, previous, next, seek, like, shuffle
and repeat. The Wear UI follows the same transport and mode state, while the system surface may
choose a different layout or put custom actions in overflow.

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
