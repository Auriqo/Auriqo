interface Env {
  YOUTUBE_DATA_API_KEY?: string;
  PROXY_SHARED_SECRET?: string;
  ALLOWED_ORIGINS?: string;
  ALLOW_PUBLIC_PLAYLISTS?: string;
}

interface YouTubeResponse {
  items?: Array<{
    snippet?: {
      publishedAt?: string;
      channelId?: string;
      channelTitle?: string;
      thumbnails?: Record<string, { url?: string }>;
      resourceId?: { videoId?: string };
    };
  }>;
  nextPageToken?: string;
  error?: { message?: string };
}

const API_URL = "https://www.googleapis.com/youtube/v3/playlistItems";

function allowedOrigins(env: Env): Set<string> {
  return new Set(
    (env.ALLOWED_ORIGINS ?? "")
      .split(",")
      .map((value) => value.trim())
      .filter((value) => value.length > 0 && value !== "*"),
  );
}

function originAllowed(request: Request, env: Env): boolean {
  const requestedOrigin = request.headers.get("Origin");
  return !requestedOrigin || allowedOrigins(env).has(requestedOrigin);
}

function corsHeaders(request: Request, env: Env): Headers {
  const requestedOrigin = request.headers.get("Origin");
  const headers = new Headers({
    "Access-Control-Allow-Headers": "Authorization, Content-Type, X-Auriqo-Proxy-Secret",
    "Access-Control-Allow-Methods": "GET, OPTIONS",
    "Access-Control-Max-Age": "600",
    "Vary": "Origin",
  });
  if (requestedOrigin && allowedOrigins(env).has(requestedOrigin)) {
    headers.set("Access-Control-Allow-Origin", requestedOrigin);
  }
  return headers;
}

function json(request: Request, env: Env, body: unknown, status = 200): Response {
  const headers = corsHeaders(request, env);
  headers.set("Content-Type", "application/json; charset=utf-8");
  return new Response(JSON.stringify(body), { status, headers });
}

function authorized(request: Request, env: Env): boolean {
  const sharedSecret = env.PROXY_SHARED_SECRET?.trim();
  if (sharedSecret && request.headers.get("X-Auriqo-Proxy-Secret") === sharedSecret) return true;

  const bearer = request.headers.get("Authorization")?.trim() ?? "";
  if (/^Bearer\s+\S+$/i.test(bearer)) return true;

  return env.ALLOW_PUBLIC_PLAYLISTS?.trim().toLowerCase() === "true";
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (!originAllowed(request, env)) return json(request, env, { error: "origin_not_allowed" }, 403);
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: corsHeaders(request, env) });
    if (request.method !== "GET") return json(request, env, { error: "method_not_allowed" }, 405);

    const url = new URL(request.url);
    if (url.pathname === "/health") return json(request, env, { ok: true, service: "auriqo-youtube-attribution" });
    if (url.pathname !== "/v1/playlist-attributions") return json(request, env, { error: "not_found" }, 404);
    if (!authorized(request, env)) return json(request, env, { error: "unauthorized" }, 401);

    const playlistId = url.searchParams.get("playlistId")?.trim();
    if (!playlistId || !/^[A-Za-z0-9_-]+$/.test(playlistId)) {
      return json(request, env, { error: "invalid_playlist_id" }, 400);
    }

    const bearer = request.headers.get("Authorization")?.trim();
    const apiKey = env.YOUTUBE_DATA_API_KEY;
    if (!bearer && !apiKey) {
      return json(request, env, { error: "youtube_auth_not_configured" }, 503);
    }

    const items: Record<string, {
      channelId: string;
      channelTitle: string;
      addedAt: string | null;
      avatarUrl: string | null;
    }> = {};
    let pageToken: string | undefined;

    try {
      do {
        const requestUrl = new URL(API_URL);
        requestUrl.searchParams.set("part", "snippet");
        requestUrl.searchParams.set("playlistId", playlistId);
        requestUrl.searchParams.set("maxResults", "50");
        if (pageToken) requestUrl.searchParams.set("pageToken", pageToken);
        if (apiKey && !bearer) requestUrl.searchParams.set("key", apiKey);

        const headers = new Headers({ Accept: "application/json" });
        if (bearer) headers.set("Authorization", bearer);
        const upstream = await fetch(requestUrl, { headers });
        const payload = await upstream.json() as YouTubeResponse;
        if (!upstream.ok) {
          return json(request, env, { error: "youtube_api_error" }, upstream.status === 403 ? 403 : 502);
        }
        for (const item of payload.items ?? []) {
          const snippet = item.snippet;
          const videoId = snippet?.resourceId?.videoId;
          const channelId = snippet?.channelId;
          if (!videoId || !channelId) continue;
          items[videoId] = {
            channelId,
            channelTitle: snippet.channelTitle || channelId,
            addedAt: snippet.publishedAt ?? null,
            avatarUrl: snippet.thumbnails?.default?.url ?? null,
          };
        }
        pageToken = payload.nextPageToken;
      } while (pageToken);
    } catch {
      return json(request, env, { error: "upstream_unavailable" }, 502);
    }

    return json(request, env, { playlistId, items, source: bearer ? "youtube-data-api-oauth" : "youtube-data-api-key" });
  },
};
