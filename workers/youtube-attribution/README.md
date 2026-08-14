# Auriqo YouTube attribution Worker

Proxy mínimo para `youtube/v3/playlistItems`. No recibe ni almacena cookies de Google.

## Configuración

```bash
npm install
npx wrangler secret put YOUTUBE_DATA_API_KEY
npx wrangler secret put PROXY_SHARED_SECRET
npm run deploy
```

Para playlists privadas o colaborativas, la app debe enviar un OAuth bearer válido con permiso `youtube.readonly`:

```http
Authorization: Bearer <google-access-token>
X-Auriqo-Proxy-Secret: <secret>
```

El Worker no guarda el token. Si no se envía OAuth, usa `YOUTUBE_DATA_API_KEY` para playlists públicas.
