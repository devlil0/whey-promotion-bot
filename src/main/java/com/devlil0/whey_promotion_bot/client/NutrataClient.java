package com.devlil0.whey_promotion_bot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.devlil0.whey_promotion_bot.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class NutrataClient {

    private final WebClient webClient;

    public NutrataClient(WebClient.Builder builder,
                          @Value("${nutrata.api.base-url}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    public JsonNode searchWhey(int page, int limit) {
        try {
            return webClient.get()
                    .uri(u -> u.path("/web_api/search")
                            .queryParam("query", "whey")
                            .queryParam("limit", limit)
                            .queryParam("page", page)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            throw new ExternalApiException("Falha ao consultar Nutrata", e);
        }
    }
}
