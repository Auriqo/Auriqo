# Auriqo YouTube attribution Worker

This optional Cloudflare Worker proxies `youtube/v3/playlistItems` so the app can display channel attribution for playlist items. It is not needed for the FOSS Android build.

Full endpoint, authentication, CORS and deployment documentation is in [../../docs/WORKERS.md](../../docs/WORKERS.md).

## Quick reference

```bash
npm ci
npm run typecheck
npm run dev
```

The checked-in deployment is closed to anonymous playlist requests and browser origins by default. OAuth bearer requests are supported; anonymous public-playlist mode is an explicit `ALLOW_PUBLIC_PLAYLISTS = "true"` deployment choice. Keep `YOUTUBE_DATA_API_KEY` and `PROXY_SHARED_SECRET` in Wrangler secrets and never commit their values.
