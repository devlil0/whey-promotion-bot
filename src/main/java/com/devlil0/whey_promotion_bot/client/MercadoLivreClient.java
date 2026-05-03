package com.devlil0.whey_promotion_bot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.devlil0.whey_promotion_bot.exception.ExternalApiException;
import com.devlil0.whey_promotion_bot.service.MlTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class MercadoLivreClient {

    private static final Logger log = LoggerFactory.getLogger(MercadoLivreClient.class);

    private final WebClient webClient;
    private final MlTokenService tokenService;

    public MercadoLivreClient(
            WebClient.Builder builder,
            @Value("${mercadolivre.api.base-url}") String baseUrl,
            MlTokenService tokenService
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.tokenService = tokenService;
    }

    public JsonNode searchWheyProtein(int limit, int offset) {
        try {
            return webClient.get()
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
            JsonNode response = webClient.get()
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

    public java.util.Map<String, Object> diagnose() {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        String token = tokenService.getAccessToken();
        result.put("hasToken", token != null);
        result.put("tokenPreview", token != null ? token.substring(0, Math.min(12, token.length())) + "..." : null);

        result.put("usersMe", probe("/users/me", token));
        result.put("itemsExample", probe("/items/MLB1276222608", token));
        result.put("sitesSearch", probe("/sites/MLB/search?q=whey&limit=1", token));
        result.put("sitesSearchNoAuth", probe("/sites/MLB/search?q=whey&limit=1", null));

        return result;
    }

    private java.util.Map<String, Object> probe(String path, String token) {
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
        try {
            org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec<?> spec = webClient.get().uri(path);
            spec = spec.headers(headers -> {
                headers.set(org.springframework.http.HttpHeaders.ACCEPT, "application/json");
                headers.set(org.springframework.http.HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36");
                if (token != null) headers.setBearerAuth(token);
            });
            org.springframework.http.ResponseEntity<String> resp = spec.retrieve()
                    .onStatus(status -> status.isError(), r -> reactor.core.publisher.Mono.empty())
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

    private void applyHeaders(org.springframework.http.HttpHeaders headers) {
        headers.set(org.springframework.http.HttpHeaders.ACCEPT, "application/json");
        headers.set(org.springframework.http.HttpHeaders.USER_AGENT,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36");
        String token = tokenService.getAccessToken();
        if (token != null) {
            headers.setBearerAuth(token);
        }
    }
}
