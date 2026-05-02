package com.devlil0.whey_promotion_bot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.devlil0.whey_promotion_bot.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class DarkLabClient {

    private final WebClient webClient;

    public DarkLabClient(WebClient.Builder builder, @Value("${darklab.api.base-url}") String baseUrl) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    public JsonNode getProducts(int page, int limit) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/products.json")
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            throw new ExternalApiException("Falha ao consultar Dark Lab products.json", e);
        }
    }
}
