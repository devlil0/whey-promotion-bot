package com.devlil0.whey_promotion_bot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.devlil0.whey_promotion_bot.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class SoldiersNutritionClient {

    private static final Logger log = LoggerFactory.getLogger(SoldiersNutritionClient.class);

    private final WebClient webClient;

    public SoldiersNutritionClient(WebClient.Builder builder,
                                    @Value("${soldiers.api.base-url}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    public JsonNode getWheyProducts(int page, int limit) {
        try {
            return webClient.get()
                    .uri(u -> u.path("/collections/whey-protein/products.json")
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            throw new ExternalApiException("Falha ao consultar Soldiers Nutrition", e);
        }
    }

    public JsonNode getOfertaRelampago(int page, int limit) {
        try {
            return getCollectionProducts("/collections/oferta-relampago/products.json", page, limit);
        } catch (Exception e) {
            log.warn("Falha ao consultar Soldiers Nutrition em /oferta-relampago, tentando handle legado: {}", e.getMessage());
            try {
                return getCollectionProducts("/collections/oferta-relampago-1/products.json", page, limit);
            } catch (Exception fallbackException) {
                throw new ExternalApiException("Falha ao consultar Soldiers Nutrition oferta relâmpago", fallbackException);
            }
        }
    }

    private JsonNode getCollectionProducts(String path, int page, int limit) {
        return webClient.get()
                .uri(u -> u.path(path)
                        .queryParam("page", page)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }
}
