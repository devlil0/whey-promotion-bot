// Cloudflare Worker — proxy para o endpoint de busca do Mercado Livre.
// Deploy: https://dash.cloudflare.com → Workers & Pages → Create → Hello World →
// cole este conteúdo em src/index.js → Deploy.
// URL final fica algo como: https://ml-search-proxy.<seu-subdomain>.workers.dev
//
// Opcional: defina a env var PROXY_TOKEN no Worker e mande o mesmo valor no header
// X-Proxy-Token a partir do Java, para evitar que terceiros usem seu Worker.

const ML_BASE = "https://api.mercadolibre.com";
const ALLOWED_PATHS = ["/sites/", "/items/", "/users/"];

export default {
  async fetch(request, env) {
    if (env.PROXY_TOKEN) {
      const provided = request.headers.get("X-Proxy-Token");
      if (provided !== env.PROXY_TOKEN) {
        return new Response("Unauthorized", { status: 401 });
      }
    }

    const url = new URL(request.url);
    const targetPath = url.pathname + url.search;

    if (!ALLOWED_PATHS.some(p => url.pathname.startsWith(p))) {
      return new Response("Path not allowed", { status: 400 });
    }

    const upstream = new Request(ML_BASE + targetPath, {
      method: request.method,
      headers: {
        "Accept": "application/json",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36",
        ...(request.headers.get("Authorization")
          ? { "Authorization": request.headers.get("Authorization") }
          : {}),
      },
      body: request.method !== "GET" && request.method !== "HEAD" ? request.body : undefined,
    });

    const resp = await fetch(upstream);
    return new Response(resp.body, {
      status: resp.status,
      headers: {
        "Content-Type": resp.headers.get("Content-Type") || "application/json",
        "X-Proxied-By": "cloudflare-worker",
      },
    });
  },
};
