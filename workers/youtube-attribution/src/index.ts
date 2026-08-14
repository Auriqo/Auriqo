interface Env {
  YOUTUBE_DATA_API_KEY?: string;
  PROXY_SHARED_SECRET?: string;
  ALLOWED_ORIGINS?: string;
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

function corsHeaders(request: Request, env: Env): Headers {
  const requestedOrigin = request.headers.get("Origin") ?? "";
  const allowed = (env.ALLOWED_ORIGINS ?? "*")
    .split(",")
    .map((value) => value.trim())
    .filter(Boolean);
  const origin = allowed.includes("*") || allowed.includes(requestedOrigin) ? requestedOrigin || "*" : allowed[0] ?? "*";
  return new Headers({
    "Access-Control-Allow-Origin": origin,
    "Access-Control-Allow-Headers": "Authorization, Content-Type, X-Auriqo-Proxy-Secret",
    "Access-Control-Allow-Methods": "GET, OPTIONS",
    "Vary": "Origin",
  });
}

function json(request: Request, env: Env, body: unknown, status = 200): Response {
  const headers = corsHeaders(request, env);
  headers.set("Content-Type", "application/json; charset=utf-8");
  return new Response(JSON.stringify(body), { status, headers });
}

function authorized(request: Request, env: Env): boolean {
  if (!env.PROXY_SHARED_SECRET) return true;
  return request.headers.get("X-Auriqo-Proxy-Secret") === env.PROXY_SHARED_SECRET;
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method === "OPTIONS") return new Response(null, { headers: corsHeaders(request, env) });
    if (request.method !== "GET") return json(request, env, { error: "method_not_allowed" }, 405);
    if (!authorized(request, env)) return json(request, env, { error: "unauthorized" }, 401);

    const url = new URL(request.url);
    if (url.pathname === "/health") return json(request, env, { ok: true, service: "auriqo-youtube-attribution" });
    if (url.pathname !== "/v1/playlist-attributions") return json(request, env, { error: "not_found" }, 404);

    const playlistId = url.searchParams.get("playlistId")?.trim();
    if (!playlistId || !/^[A-Za-z0-9_-]+$/.test(playlistId)) {
      return json(request, env, { error: "invalid_playlist_id" }, 400);
    }

    const bearer = request.headers.get("Authorization");
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
          return json(request, env, { error: "youtube_api_error", detail: payload.error?.message ?? "Upstream request failed" }, upstream.status === 403 ? 403 : 502);
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
    } catch (error) {
      return json(request, env, { error: "upstream_unavailable", detail: error instanceof Error ? error.message : "Unknown error" }, 502);
    }

    return json(request, env, { playlistId, items, source: bearer ? "youtube-data-api-oauth" : "youtube-data-api-key" });
  },
};
