package com.devlil0.whey_promotion_bot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.devlil0.whey_promotion_bot.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class MercadoLivreClient {

    private static final Logger log = LoggerFactory.getLogger(MercadoLivreClient.class);

    private final WebClient webClient;

    public MercadoLivreClient(
            WebClient.Builder builder,
            @Value("${mercadolivre.api.base-url}") String baseUrl
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
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
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            throw new ExternalApiException("Falha ao consultar Mercado Livre", e);
        }
    }

    public JsonNode searchWhey(int limit) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/sites/MLB/search")
                            .queryParam("q", "whey protein")
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .onStatus(status -> status.isError(), response ->
                            response.bodyToMono(String.class).map(body -> {
                                log.error("ML API erro {}: {}", response.statusCode(), body);
                                return new ExternalApiException("ML API retornou " + response.statusCode());
                            }))
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("Falha em searchWhey: {} — causa: {}", e.getMessage(),
                    e.getCause() != null ? e.getCause().getMessage() : "sem causa");
            throw new ExternalApiException("Falha ao buscar whey no Mercado Livre", e);
        }
    }

    public String getItemDescription(String itemId) {
        try {
            JsonNode response = webClient.get()
                    .uri("/items/{id}/descriptions", itemId)
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
}
