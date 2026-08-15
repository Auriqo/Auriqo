# Workers

The repository contains one optional Cloudflare Worker: `workers/youtube-attribution`. It is not required to compile or run the FOSS Android build.

## YouTube attribution Worker

The Worker exposes:

- `GET /health` for a basic health response;
- `GET /v1/playlist-attributions?playlistId=...` for playlist-item attribution data;
- `OPTIONS` for CORS preflight.

For each playlist page it forwards a request to Google's `youtube/v3/playlistItems` endpoint and returns channel IDs/titles, the playlist item's published timestamp and a default thumbnail URL. The Worker does not write request data or tokens to a database and does not return full upstream error messages.

### Configuration

The checked-in `wrangler.toml` is fail-closed by default:

- `ALLOWED_ORIGINS`: comma-separated exact browser origins. An empty value denies browser CORS; native Android requests do not need CORS.
- `ALLOW_PUBLIC_PLAYLISTS`: `false` by default. Set it to `true` only when the maintainer intentionally accepts an unauthenticated, quota-bearing public endpoint and has edge abuse controls in place.

Secrets must be provisioned with Wrangler and must never be committed:

- `YOUTUBE_DATA_API_KEY`: used for the explicit public-playlist mode.
- `PROXY_SHARED_SECRET`: accepted through `X-Auriqo-Proxy-Secret` for clients that can send it.

`GET /health` is public. Playlist attribution requires a valid-looking Google OAuth bearer token, the configured shared-secret header, or the explicit public-playlist switch. The Worker forwards OAuth to YouTube/Google; it does not validate or store the token itself.

### Android compatibility

The Android client sends an optional Google OAuth bearer token to the Worker. It does not send `X-Auriqo-Proxy-Secret`. When no OAuth token is configured, the client does not make an anonymous Worker call; a user-supplied YouTube API key is used directly as the existing fallback. Local attribution remains available without either credential.

If a deployment enables `ALLOW_PUBLIC_PLAYLISTS`, anonymous attribution remains available for clients that need it, but the maintainer is accepting exposure of the configured YouTube API quota. Keep that choice explicit in the deployment review.

### Local development and deployment

From the Worker directory:

```bash
npm ci
npm run typecheck
npm run dev
```

Deployment is maintainer-only:

```bash
npx wrangler secret put YOUTUBE_DATA_API_KEY
npx wrangler secret put PROXY_SHARED_SECRET
npm run deploy
```

The secrets are optional depending on the selected mode. Never paste secret values into `wrangler.toml`, documentation, issues or CI output. Configure exact browser origins in `wrangler.toml` only when a browser client is intentionally supported.

Before a production deployment, verify the Worker URL, Google quota, Cloudflare rate/abuse controls and operational log settings. The source-level no-database behavior does not define Cloudflare's platform retention.

## Other remote services

Listen Together, canvas/artwork providers and lyrics services are contacted directly by the app or by their own upstream infrastructure; they are not Workers maintained in this repository. Their endpoints and data flows are listed in [PRIVACY_POLICY.md](../PRIVACY_POLICY.md) and [LYRICS_PROVIDERS.md](LYRICS_PROVIDERS.md).
