<div align="center">
  <img src="assets/auriqo-logo.svg" alt="Auriqo" width="180"/>

  <h1>Auriqo</h1>

  <p><strong>Android music player — ad-free streaming, synced lyrics, offline playback.</strong></p>

  [![License](https://img.shields.io/badge/license-GPL--3.0-28a745?style=for-the-badge)](LICENSE)
  [![Platform](https://img.shields.io/badge/platform-Android-6f42c1?style=for-the-badge&logo=android)](https://github.com/Auriqo/Auriqo/releases)
</div>

---

## Overview

Auriqo streams from YouTube Music without ads and adds offline downloads, synced lyrics, and music recognition. Built on [Metrolist](https://github.com/MetrolistGroup/Metrolist) and [Vivi Music](https://github.com/vivizzz007/vivi-music).

---

## Screenshots

<div align="center">
  <table>
    <tr>
      <td align="center"><strong>Home</strong><br><img src="Screenshots/sc_1.png" width="200"/></td>
      <td align="center"><strong>Player</strong><br><img src="Screenshots/sc_2.png" width="200"/></td>
      <td align="center"><strong>Lyrics</strong><br><img src="Screenshots/sc_3.png" width="200"/></td>
    </tr>
    <tr>
      <td align="center"><strong>Search</strong><br><img src="Screenshots/sc_4.png" width="200"/></td>
      <td align="center"><strong>Library</strong><br><img src="Screenshots/sc_5.png" width="200"/></td>
      <td align="center"><strong>Recognition</strong><br><img src="Screenshots/sc_6.png" width="200"/></td>
    </tr>
  </table>
</div>

---

## Features

- Ad-free YouTube Music streaming
- Offline downloads with download manager
- Synced lyrics (word-by-word, multiple animation styles, AI translation)
- Music recognition (identify songs around you)
- Background and audio-only playback
- Spotify playlist import
- Listen Together (real-time sync)
- Crossfade, sleep timer, podcast support
- Lossless audio (16/24-bit FLAC)
- Canvas animations
- Equalizer, ringtone export
- Discord Rich Presence, Last.fm scrobbling

---

## Install

Get the latest APK from [Releases](https://github.com/Auriqo/Auriqo/releases).

### Build from source

```bash
git clone https://github.com/Auriqo/Auriqo.git
cd Auriqo
echo "sdk.dir=/path/to/android/sdk" > local.properties

# FOSS variant (no Google Play Services)
./gradlew assembleUniversalFossDebug

# GMS variant (with Cast support)
./gradlew assembleUniversalGmsDebug
```

---

## License

[GPL-3.0](LICENSE)
