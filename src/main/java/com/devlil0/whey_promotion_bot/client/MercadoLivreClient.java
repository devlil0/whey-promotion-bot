package com.devlil0.whey_promotion_bot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.devlil0.whey_promotion_bot.exception.ExternalApiException;
import com.devlil0.whey_promotion_bot.service.MlTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MercadoLivreClient {

    private static final Logger log = LoggerFactory.getLogger(MercadoLivreClient.class);
    private static final String BROWSER_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    private final WebClient directClient;
    private final WebClient proxyClient;
    private final MlTokenService tokenService;
    private final String proxyUrl;
    private final String proxyToken;

    public MercadoLivreClient(
            WebClient.Builder builder,
            @Value("${mercadolivre.api.base-url}") String baseUrl,
            @Value("${mercadolivre.proxy.url:}") String proxyUrl,
            @Value("${mercadolivre.proxy.token:}") String proxyToken,
            MlTokenService tokenService
    ) {
        this.directClient = builder.baseUrl(baseUrl).build();
        this.proxyUrl = proxyUrl;
        this.proxyToken = proxyToken;
        this.proxyClient = (proxyUrl != null && !proxyUrl.isBlank())
                ? builder.baseUrl(proxyUrl).build()
                : null;
        this.tokenService = tokenService;
        if (proxyClient != null) {
            log.info("ML proxy ativo em {}", proxyUrl);
        }
    }

    public JsonNode searchWheyProtein(int limit, int offset) {
        WebClient client = proxyClient != null ? proxyClient : directClient;
        try {
            return client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/sites/MLB/search")
                            .queryParam("q", "whey protein")
                            .queryParam("limit", limit)
                            .queryParam("offset", offset)
                            .build())
                    .headers(this::applyHeaders)
                    .retrieve()
                    .onStatus(status -> status.isError(), response ->
                            response.bodyToMono(String.class).map(body -> {
                                log.error("ML search erro {}: {}", response.statusCode(), body);
                                return new ExternalApiException("ML search retornou " + response.statusCode());
                            }))
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("Falha em searchWheyProtein: {} — causa: {}", e.getMessage(),
                    e.getCause() != null ? e.getCause().getMessage() : "sem causa");
            throw new ExternalApiException("Falha ao consultar Mercado Livre", e);
        }
    }

    public String getItemDescription(String itemId) {
        try {
            JsonNode response = directClient.get()
                    .uri("/items/{id}/descriptions", itemId)
                    .headers(this::applyHeaders)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            if (response == null || !response.isArray() || response.isEmpty()) return null;
            JsonNode first = response.get(0);
            String plainText = first.path("plain_text").asText(null);
            if (plainText != null && !plainText.isBlank()) return plainText;
            return first.path("text").asText(null);
        } catch (Exception e) {
            log.warn("Falha ao buscar descrição do item ML {}: {}", itemId, e.getMessage());
            return null;
        }
    }

    public Map<String, Object> diagnose() {
        Map<String, Object> result = new LinkedHashMap<>();
        String token = tokenService.getAccessToken();
        result.put("hasToken", token != null);
        result.put("tokenPreview", token != null ? token.substring(0, Math.min(12, token.length())) + "..." : null);
        result.put("proxyConfigured", proxyClient != null);

        result.put("usersMe", probe(directClient, "/users/me", token, false));
        result.put("itemsExample", probe(directClient, "/items/MLB1276222608", token, false));
        result.put("sitesSearchDirect", probe(directClient, "/sites/MLB/search?q=whey&limit=1", token, false));
        result.put("sitesSearchDirectNoAuth", probe(directClient, "/sites/MLB/search?q=whey&limit=1", null, false));
        if (proxyClient != null) {
            result.put("sitesSearchProxy", probe(proxyClient, "/sites/MLB/search?q=whey&limit=1", token, true));
        }

        return result;
    }

    private Map<String, Object> probe(WebClient client, String path, String token, boolean proxied) {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            ResponseEntity<String> resp = client.get()
                    .uri(path)
                    .headers(headers -> {
                        headers.set(HttpHeaders.ACCEPT, "application/json");
                        headers.set(HttpHeaders.USER_AGENT, BROWSER_UA);
                        if (token != null) headers.setBearerAuth(token);
                        if (proxied && proxyToken != null && !proxyToken.isBlank()) {
                            headers.set("X-Proxy-Token", proxyToken);
                        }
                    })
                    .retrieve()
                    .onStatus(status -> status.isError(), r -> Mono.empty())
                    .toEntity(String.class)
                    .block();
            out.put("status", resp != null ? resp.getStatusCode().value() : null);
            String body = resp != null ? resp.getBody() : null;
            out.put("bodySnippet", body != null ? body.substring(0, Math.min(200, body.length())) : null);
        } catch (Exception e) {
            out.put("status", "exception");
            out.put("error", e.getMessage());
        }
        return out;
    }

    private void applyHeaders(HttpHeaders headers) {
        headers.set(HttpHeaders.ACCEPT, "application/json");
        headers.set(HttpHeaders.USER_AGENT, BROWSER_UA);
        String token = tokenService.getAccessToken();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        if (proxyClient != null && proxyToken != null && !proxyToken.isBlank()) {
            headers.set("X-Proxy-Token", proxyToken);
        }
    }
}
