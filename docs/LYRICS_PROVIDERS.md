# Lyrics providers

Auriqo uses independent lyrics integrations. They are best-effort network clients, not a bundled lyrics catalogue. Availability, response formats, rate limits and terms can change without an app release.

## Providers in the current tree

| Provider/module | Current endpoint(s) | Request behavior | Authentication |
| --- | --- | --- | --- |
| BetterLyrics | `https://lyrics-api.boidu.dev` | Sends title, artist, duration and optional album/video identifier; parses TTML locally | None in the Android client |
| LRCLIB | `https://lrclib.net` | Searches for timed/plain lyrics using track metadata | None in the Android client |
| Paxsenix | `https://lyrics.paxsenix.org` | Queries the provider with track metadata and an Auriqo user agent | None in the Android client |
| KuGou | `https://mobileservice.kugou.com`, `https://lyrics.kugou.com` | Searches songs and downloads lyric data | None in the Android client |
| SimpMusic | `https://api-lyrics.simpmusic.org/v1/`, with a fallback server in code | Requests lyrics using track metadata | None in the Android client |
| YouLyPlus | Several public mirrors listed in `youlyplus/YouLyPlus.kt` | Tries the configured mirrors for the requested track | None in the Android client |
| Letras.com | `https://www.letras.com` and its search endpoint | Searches and parses public HTML/pages | None in the Android client |

The exact request fields are defined in each module. Do not add a provider to this table without checking the source, documenting its data flow and recording the provider's license/terms where relevant.

## BetterLyrics implementation

The `betterlyrics` module sends a Ktor request to the BetterLyrics API and parses TTML locally. It does not store cookies or provider credentials. The upstream Better Lyrics project and the Android adaptation have separate provenance questions; see [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) and [PROVENANCE.md](PROVENANCE.md).

## Failure and privacy behavior

- Lyrics requests are initiated when lyrics are requested or a lyrics-enabled playback surface needs them; they are not a general upload of the local library.
- Track metadata can identify what a user is trying to play. Users who need local-only behavior should disable remote lyrics providers.
- Provider errors should be logged without response bodies, cookies or authorization headers.
- Test fixtures under `letras/src/test/resources/` are provider-shaped samples. Keep them minimal, free of personal data and legally reviewable; do not add full copyrighted catalogues as fixtures.

## Adding or changing a provider

Include in the pull request:

1. endpoint and transport/security requirements;
2. exact fields sent and received;
3. authentication, rate-limit and failure behavior;
4. a test that does not require a live secret or an account;
5. license/attribution and upstream provenance; and
6. updates to this document and [PRIVACY_POLICY.md](../PRIVACY_POLICY.md).
