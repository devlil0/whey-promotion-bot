package com.devlil0.whey_promotion_bot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.devlil0.whey_promotion_bot.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AbsolutNutritionClient {

    private final WebClient webClient;

    public AbsolutNutritionClient(WebClient.Builder builder,
                                   @Value("${absolut.api.base-url}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    public JsonNode searchWhey(int page, int perPage) {
        try {
            return webClient.get()
                    .uri(u -> u.path("/wp-json/wc/store/v1/products")
                            .queryParam("search", "whey")
                            .queryParam("per_page", perPage)
                            .queryParam("page", page)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            throw new ExternalApiException("Falha ao consultar Absolut Nutrition", e);
        }
    }
}
