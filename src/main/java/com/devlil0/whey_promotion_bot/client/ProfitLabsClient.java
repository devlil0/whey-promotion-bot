package com.devlil0.whey_promotion_bot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.devlil0.whey_promotion_bot.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ProfitLabsClient {

    private final WebClient webClient;

    public ProfitLabsClient(WebClient.Builder builder, @Value("${profitlabs.api.base-url}") String baseUrl) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    public JsonNode getProducts(int page, int limit) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/web_api/products")
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            throw new ExternalApiException("Falha ao consultar Profit Labs web_api/products", e);
        }
    }

    public JsonNode getPromocoes(int page, int limit) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/web_api/products/")
                            .queryParam("category_id", 35)
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            throw new ExternalApiException("Falha ao consultar Profit Labs promoções (category_id=35)", e);
        }
    }
}
