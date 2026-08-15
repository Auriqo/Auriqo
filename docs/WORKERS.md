# Workers

The repository contains one optional Cloudflare Worker: `workers/youtube-attribution`. It is not required to compile or run the FOSS Android build.

## YouTube attribution Worker

The Worker exposes:

- `GET /health` for a basic health response;
- `GET /v1/playlist-attributions?playlistId=...` for playlist-item attribution data;
- `OPTIONS` for CORS preflight.

For each playlist page it forwards a request to Google's `youtube/v3/playlistItems` endpoint and returns channel IDs/titles, the playlist item's published timestamp and a default thumbnail URL. The Worker source does not write request data or tokens to a database.

### Configuration

Secrets must be provisioned with Wrangler and must never be committed:

- `YOUTUBE_DATA_API_KEY`: allows public-playlist requests when the caller does not provide OAuth.
- `PROXY_SHARED_SECRET`: optional request gate checked through `X-Auriqo-Proxy-Secret`.
- `ALLOWED_ORIGINS`: comma-separated CORS allowlist.

The checked-in `wrangler.toml` currently uses `ALLOWED_ORIGINS = "*"`. The code also treats a missing `PROXY_SHARED_SECRET` as authorized. These defaults are deployment risks, not security recommendations.

### Compatibility warning

The current Android client sends an optional Google OAuth bearer token to the Worker but does not send `X-Auriqo-Proxy-Secret`. Enabling `PROXY_SHARED_SECRET` on the endpoint used by the current app will therefore return `401` and break playlist attribution until the client and deployment are changed together. Coordinate that change; do not silently enable it in production.

Before exposing a production Worker, the maintainer should:

1. choose and implement a client-to-Worker authentication design;
2. replace the wildcard CORS value with the actual allowed origins or an authenticated non-browser route;
3. keep Google API keys and shared secrets in Wrangler secrets;
4. avoid logging bearer tokens, playlist contents or full upstream errors;
5. set rate limits/abuse controls at the edge; and
6. verify Cloudflare operational logs and retention separately from the source-level no-storage behavior.

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

Only provision the shared secret after the Android client is prepared to send it. Never paste secret values into `wrangler.toml`, documentation, issues or CI output.

## Other remote services

Listen Together, canvas/artwork providers and lyrics services are contacted directly by the app or by their own upstream infrastructure; they are not Workers maintained in this repository. Their endpoints and data flows are listed in [PRIVACY_POLICY.md](../PRIVACY_POLICY.md) and [LYRICS_PROVIDERS.md](LYRICS_PROVIDERS.md).
