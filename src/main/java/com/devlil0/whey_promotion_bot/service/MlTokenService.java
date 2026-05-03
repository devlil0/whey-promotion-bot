package com.devlil0.whey_promotion_bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.devlil0.whey_promotion_bot.entity.MlOAuthToken;
import com.devlil0.whey_promotion_bot.repository.MlOAuthTokenRepository;
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
import java.util.Optional;

@Service
public class MlTokenService {

    private static final Logger log = LoggerFactory.getLogger(MlTokenService.class);
    private static final int EXPIRY_BUFFER_SECONDS = 300;

    private final WebClient webClient;
    private final MlOAuthTokenRepository tokenRepository;
    private final String appId;
    private final String secretKey;
    private final String redirectUri;

    public MlTokenService(
            WebClient.Builder builder,
            MlOAuthTokenRepository tokenRepository,
            @Value("${mercadolivre.api.base-url}") String baseUrl,
            @Value("${mercadolivre.app-id:}") String appId,
            @Value("${mercadolivre.secret-key:}") String secretKey,
            @Value("${mercadolivre.redirect-uri}") String redirectUri
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.tokenRepository = tokenRepository;
        this.appId = appId;
        this.secretKey = secretKey;
        this.redirectUri = redirectUri;
    }

    public String getAccessToken() {
        Optional<MlOAuthToken> stored = tokenRepository.findById(1L);
        if (stored.isEmpty()) {
            log.warn("ML OAuth: nenhum token no banco — acesse /api/ml/oauth/start para autorizar.");
            return null;
        }

        MlOAuthToken token = stored.get();
        if (Instant.now().isBefore(token.getExpiresAt())) {
            return token.getAccessToken();
        }

        return refreshAccessToken(token);
    }

    public String buildAuthorizationUrl() {
        return "https://auth.mercadolivre.com.br/authorization"
                + "?response_type=code"
                + "&client_id=" + appId
                + "&redirect_uri=" + redirectUri;
    }

    public void handleAuthorizationCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", appId);
        form.add("client_secret", secretKey);
        form.add("code", code);
        form.add("redirect_uri", redirectUri);

        JsonNode response = callTokenEndpoint(form);
        if (response == null) throw new RuntimeException("Falha ao trocar código por token");

        saveToken(response);
        log.info("ML OAuth: token autorizado e salvo com sucesso.");
    }

    private String refreshAccessToken(MlOAuthToken token) {
        log.info("ML OAuth: access token expirado, renovando com refresh token...");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", appId);
        form.add("client_secret", secretKey);
        form.add("refresh_token", token.getRefreshToken());

        JsonNode response = callTokenEndpoint(form);
        if (response == null) {
            log.error("ML OAuth: falha ao renovar token.");
            return null;
        }

        saveToken(response);
        log.info("ML OAuth: token renovado com sucesso.");
        return tokenRepository.findById(1L).map(MlOAuthToken::getAccessToken).orElse(null);
    }

    private void saveToken(JsonNode response) {
        String accessToken = response.path("access_token").asText(null);
        String refreshToken = response.path("refresh_token").asText(null);
        int expiresIn = response.path("expires_in").asInt(21600);

        MlOAuthToken token = tokenRepository.findById(1L).orElse(new MlOAuthToken());
        token.setAccessToken(accessToken);
        if (refreshToken != null && !refreshToken.isBlank()) {
            token.setRefreshToken(refreshToken);
        }
        token.setExpiresAt(Instant.now().plusSeconds(expiresIn - EXPIRY_BUFFER_SECONDS));
        tokenRepository.save(token);
    }

    private JsonNode callTokenEndpoint(MultiValueMap<String, String> form) {
        try {
            return webClient.post()
                    .uri("/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("ML OAuth: erro ao chamar /oauth/token — {}", e.getMessage());
            return null;
        }
    }
}
