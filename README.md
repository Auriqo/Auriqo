<div align="center">
  <img src="assets/Auriqo-new.png" alt="Auriqo Logo" width="140"/>

  <h1>Auriqo</h1>

  <p><strong>A modern Android music player with ad-free streaming, synced lyrics, offline playback, and an intuitive user experience.</strong></p>

  [![GitHub Release](https://img.shields.io/github/v/release/Auriqo/Auriqo?style=for-the-badge&color=6f42c1)](https://github.com/Auriqo/Auriqo/releases)
  [![GitHub Stars](https://img.shields.io/github/stars/Auriqo/Auriqo?style=for-the-badge&color=e3b341)](https://github.com/Auriqo/Auriqo/stargazers)
  [![License](https://img.shields.io/github/license/Auriqo/Auriqo?style=for-the-badge&color=28a745)](LICENSE)
</div>

---

## Overview

Auriqo delivers a seamless, premium listening experience by leveraging YouTube Music's vast library — without the ads. It adds powerful extras including offline downloads, real-time synchronized lyrics, and environment-aware music recognition.

---

## Screenshots

<div align="center">
  <table>
    <tr>
      <td align="center"><strong>Home Screen</strong><br><img src="Screenshots/sc_1.png" alt="Home Screen" width="200"/></td>
      <td align="center"><strong>Music Player</strong><br><img src="Screenshots/sc_2.png" alt="Music Player" width="200"/></td>
      <td align="center"><strong>Synchronized Lyrics</strong><br><img src="Screenshots/sc_3.png" alt="Synced Lyrics" width="200"/></td>
    </tr>
    <tr>
      <td align="center"><strong>Search & Explore</strong><br><img src="Screenshots/sc_4.png" alt="Search" width="200"/></td>
      <td align="center"><strong>Music Library</strong><br><img src="Screenshots/sc_5.png" alt="Library" width="200"/></td>
      <td align="center"><strong>Music Recognition</strong><br><img src="Screenshots/sc_6.png" alt="Recognition" width="200"/></td>
    </tr>
  </table>
</div>

---

## Features

### Streaming & Playback
- **Ad-Free** — Stream without interruptions.
- **Lossless Audio** — Support for 16-bit and 24-bit high fidelity FLAC audio.
- **Data Saver Mode** — Reduce data consumption when streaming on cellular networks.
- **Seamless Playback** — Switch effortlessly between audio-only and video modes.
- **Background Playback** — Listen while using other apps or with the screen off.
- **Offline Mode** — Download tracks, albums, and playlists via a dedicated download manager.
- **Crossfade** — Smooth transitions between tracks.
- **Canvas Animations** — Visual animations while playing music.

### Discovery & Recognition
- **Music Recognition** — Identify songs playing around you using advanced audio recognition.
- **Smart Recommendations** — Personalized suggestions based on your listening history.
- **Comprehensive Browsing** — Explore Charts, Podcasts, Moods, and Genres.

### Lyrics
- **Multiple Lyric Animations** — Choose from various lyric display styles.
- **Word-by-Word Lyrics** — Precise per-word synchronization.
- **Lyrics+** — New lyrics provider for improved accuracy and coverage.
- **AI Translation** — Built-in Google Translate integration for lyrics in any language.

### Integrations
- **Music Sharing via Odesli** — Share songs as Song.link for cross-platform listening.
- **Set as Ringtone** — Directly set any song as your device ringtone.
- **Import from Spotify** — Bring your playlists and tracks over with ease.
- **Listen Together** — Sync music in real time with friends.

### Smart Playback
- **Pause on Mute** — Auto-pause when your device is muted.
- **Resume on Bluetooth** — Playback resumes when headphones or earbuds reconnect.

### Customization
- **UI Density Scale** — Adjust interface spacing to your preference.
- **High Refresh Rate Support** — Smoother UI and animations on supported displays.
- **Hide Player Thumbnail** — Keep the player minimal without album art.
- **Crop Album Art** — Adjust album art display to fit your style.

---

## Installation

Download the latest APK from the [Releases Page](https://github.com/Auriqo/Auriqo/releases/latest).

### Building from Source

1. **Clone the Repository**
   ```bash
   git clone https://github.com/Auriqo/Auriqo.git
   cd Auriqo
   ```

2. **Configure Android SDK**
   Create a `local.properties` file:
   ```bash
   echo "sdk.dir=/path/to/your/android/sdk" > local.properties
   ```

3. **Build the Application**
   Auriqo has two build variants: **FOSS** (without Google Play Services/Cast) and **GMS** (with Cast support).

   FOSS Universal Debug:
   ```bash
   ./gradlew assembleUniversalFossDebug
   ```

   GMS Universal Debug:
   ```bash
   ./gradlew assembleUniversalGmsDebug
   ```

---

## License

Licensed under [GPL-3.0](LICENSE).

---

## Acknowledgments

Auriqo stands on the shoulders of several excellent open-source projects:

| Project | Description |
| :--- | :--- |
| [Metrolist](https://github.com/MetrolistGroup/Metrolist) & [Vivi Music](https://github.com/vivizzz007/vivi-music) | Foundational inspiration and architecture reference |
| [ArchiveTune](https://github.com/koiverse/ArchiveTune) | Material You UI inspiration |
| [Better Lyrics](https://better-lyrics.boidu.dev/) | Lyrics enhancement and synchronization |
| [SimpMusic](https://github.com/maxrave-dev/SimpMusic) | Lyrics implementation reference |
| [Music Recognizer](https://github.com/aleksey-saenko/MusicRecognizer) | Audio recognition engine |
| [zemer-cipher](https://github.com/ZemerTeam/zemer-cipher) | YouTube cipher deobfuscation and PoToken generation |
