package com.devlil0.whey_promotion_bot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.devlil0.whey_promotion_bot.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class MercadoLivreClient {

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
}
