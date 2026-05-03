package com.devlil0.whey_promotion_bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;

@Service
public class MlTokenService {

    private static final Logger log = LoggerFactory.getLogger(MlTokenService.class);
    private static final int EXPIRY_BUFFER_SECONDS = 300;

    private final WebClient webClient;
    private final String appId;
    private final String secretKey;
    private final boolean enabled;

    private volatile String cachedToken;
    private volatile Instant tokenExpiry = Instant.MIN;

    public MlTokenService(
            WebClient.Builder builder,
            @Value("${mercadolivre.api.base-url}") String baseUrl,
            @Value("${mercadolivre.app-id:}") String appId,
            @Value("${mercadolivre.secret-key:}") String secretKey
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.appId = appId;
        this.secretKey = secretKey;
        this.enabled = !appId.isBlank() && !secretKey.isBlank();
        if (!enabled) {
            log.warn("ML_APP_ID / ML_SECRET_KEY não configurados — chamadas ao ML sem autenticação.");
        }
    }

    public String getAccessToken() {
        if (!enabled) return null;
        if (cachedToken == null || Instant.now().isAfter(tokenExpiry)) {
            refreshToken();
        }
        return cachedToken;
    }

    private synchronized void refreshToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) return;

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", appId);
            form.add("client_secret", secretKey);

            JsonNode response = webClient.post()
                    .uri("/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || !response.has("access_token")) {
                log.error("ML OAuth: resposta inválida — {}", response);
                return;
            }

            cachedToken = response.path("access_token").asText();
            int expiresIn = response.path("expires_in").asInt(21600);
            tokenExpiry = Instant.now().plusSeconds(expiresIn - EXPIRY_BUFFER_SECONDS);

            log.info("ML OAuth: token obtido via Client Credentials, expira em {}s.", expiresIn);

        } catch (Exception e) {
            log.error("ML OAuth: falha ao obter token — {}", e.getMessage());
        }
    }
}
