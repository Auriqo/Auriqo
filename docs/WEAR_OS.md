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

The `wear` module provides the branded Auriqo player and Tile. The companion Activity is organized
as a small music surface rather than a single remote-control screen:

- `Now Playing`: title/artist, progress and plain transport controls, with the Auriqo mark as the
  deliberate entry point for like, shuffle and repeat.
- The physical rotary bezel adjusts media volume on the phone (or the Cast receiver when casting).
  It does not seek. Seeking is deliberately reserved for dragging the thin progress rail, so the
  two interactions cannot fight each other.
- `Home`: the library entry point and current-track handoff.
- `Tracks`, `Albums`, `Artists`, `Playlists` and `Queue`: scrollable, actionable lists. Tapping an
  item asks the phone to build that item (or collection) into the active player queue.

The visual system uses the same Material You direction as the phone app. On Android 12 and newer,
the Wear companion maps the device's dynamic Material 3 scheme into Wear Material tokens for the
background, surface containers, text, tonal controls, outlines and progress rail; older versions
use the Auriqo fallback palette. The Tile resolves the same system palette without Compose.
Companion screens derive their top/bottom padding, horizontal safe area, card radius, artwork size
and control size from the device configuration, including whether the screen is round. Round
watches get a protected circular safe area; square watches keep tighter rectangular margins instead
of inheriting round-display padding. Vector controls and the Cabinet-derived Auriqo mark replace
emoji and the generic system-player layout. Secondary playback modes remain available behind the
mark instead of being removed from the product.

The Wear scheme follows the watch's system dynamic colors. Matching a user-selected custom phone
seed exactly would require sending the phone's resolved color tokens over the Data Layer; that is a
separate synchronization feature, not a reason to duplicate the phone's Material 3 implementation
inside the watch.

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
- `/auriqo/browse_request` (Wear -> phone message: `tracks`, `albums`, `artists`, `playlists` or `queue`)
- `/auriqo/browse_state` (phone -> Wear DataItem with bounded parallel arrays of item metadata)
- `/auriqo/browse_command` (Wear -> phone message: `kind|id` to play a track, collection or queue item)

Commands currently forwarded by the Wear proxy are play/pause, previous, next, seek, volume step,
like, shuffle and repeat. The Wear UI follows the same transport and mode state, while the system
surface may choose a different layout or put custom actions in overflow.

Browse data is intentionally bounded to 80 entries per request and is sourced from the phone's
existing Room library/player queue. It is not a second library database on the watch. If the phone
APK predates the browse protocol, the watch keeps the current-track fallback and labels the list as
syncing until a matching GMS build is installed.

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
