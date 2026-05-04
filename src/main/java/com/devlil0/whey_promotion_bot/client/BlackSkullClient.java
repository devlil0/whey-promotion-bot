package com.devlil0.whey_promotion_bot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.devlil0.whey_promotion_bot.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class BlackSkullClient {

    private final WebClient webClient;

    public BlackSkullClient(WebClient.Builder builder,
                             @Value("${blackskull.api.base-url}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    public JsonNode searchWhey(int from, int to) {
        try {
            return webClient.get()
                    .uri(u -> u.path("/api/catalog_system/pub/products/search/whey")
                            .queryParam("_from", from)
                            .queryParam("_to", to)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            throw new ExternalApiException("Falha ao consultar Black Skull", e);
        }
    }
}
