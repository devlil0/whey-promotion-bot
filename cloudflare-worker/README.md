# Cloudflare Worker — ML Search Proxy

Proxy para contornar o bloqueio do Mercado Livre no endpoint `/sites/MLB/search` quando a aplicação está hospedada em IPs de datacenter (Railway, AWS, GCP).

## Deploy (5 minutos)

1. Acesse https://dash.cloudflare.com (crie conta grátis se necessário)
2. **Workers & Pages → Create application → Create Worker**
3. Nomeie como `ml-search-proxy` → **Deploy**
4. Clique em **Edit code**, apague o conteúdo e cole o de `ml-search-proxy.js`
5. **Save and Deploy**
6. Copie a URL gerada (ex: `https://ml-search-proxy.devmurilloliveira.workers.dev`)

## Segurança opcional (recomendado)

Para evitar que terceiros usem seu Worker:

1. No Worker, vá em **Settings → Variables and Secrets**
2. Adicione `PROXY_TOKEN` com um valor aleatório (ex: gere com `openssl rand -hex 32`)
3. No Railway, adicione a env var `ML_PROXY_TOKEN` com o mesmo valor

## Configuração no Railway

Adicione duas env vars:

```
ML_PROXY_URL=https://ml-search-proxy.<seu-subdomain>.workers.dev
ML_PROXY_TOKEN=<mesmo-valor-do-worker>   # opcional
```

## Validação

Após deploy do Railway, chame:

```
GET /api/telegram/trigger/ml/probe
```

`sitesSearch.status` deve passar de `403` para `200`.

## Limites

- **Free tier Cloudflare**: 100.000 requests/dia. Nosso uso (~30 chamadas/dia) é desprezível.
- **Latência adicional**: ~50-100ms (edge network).
