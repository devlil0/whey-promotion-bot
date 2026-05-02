package com.devlil0.whey_promotion_bot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.devlil0.whey_promotion_bot.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class GrowthClient {

    private final WebClient webClient;

    public GrowthClient(
            WebClient.Builder builder,
            @Value("${growth.api.base-url}") String baseUrl,
            @Value("${growth.api.app-token}") String appToken
    ) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("App-Token", appToken)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    public JsonNode getCategory(String categoryUrl, String order, int offset, int limit) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v2/front/url/product/listing/category")
                            .queryParam("url", categoryUrl)
                            .queryParam("order", order)
                            .queryParam("offset", offset)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            throw new ExternalApiException("Falha ao consultar Growth category: " + categoryUrl, e);
        }
    }

    public JsonNode getWheyShowcase() {
        try {
            return webClient.get()
                    .uri("/api/v2/front/showcase/products/vitrineWheyVue")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            throw new ExternalApiException("Falha ao consultar vitrine Growth Whey", e);
        }
    }
}
