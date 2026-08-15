# Auriqo YouTube attribution Worker

This optional Cloudflare Worker proxies `youtube/v3/playlistItems` so the app can display channel attribution for playlist items. It does not store cookies or tokens in application code or a database.

Full endpoint, privacy and deployment documentation is in [../../docs/WORKERS.md](../../docs/WORKERS.md).

## Quick reference

```bash
npm ci
npm run typecheck
npm run dev
```

The deployment uses `YOUTUBE_DATA_API_KEY` and, when the client authentication design is ready, `PROXY_SHARED_SECRET` as Wrangler secrets. Never commit their values. The current app does not send the shared-secret header, so enabling that secret on the app's existing endpoint will break current clients until both sides are updated.
