package com.devlil0.whey_promotion_bot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/whatsapp/setup")
public class EvolutionSetupController {

    private static final Logger log = LoggerFactory.getLogger(EvolutionSetupController.class);

    private final WebClient webClient;
    private final String instance;
    private final boolean configured;

    public EvolutionSetupController(
            WebClient.Builder builder,
            @Value("${evolution.api.base-url:}") String baseUrl,
            @Value("${evolution.api.api-key:}") String apiKey,
            @Value("${evolution.api.instance:}") String instance
    ) {
        this.instance = instance;
        this.configured = !baseUrl.isBlank() && !apiKey.isBlank();
        this.webClient = builder
                .baseUrl(baseUrl.isBlank() ? "http://localhost:8081" : baseUrl)
                .defaultHeader("apikey", apiKey)
                .build();
    }

    /**
     * Cria a instância (se não existir) e retorna o QR code em base64.
     * Cole o valor de "imgTag" diretamente num arquivo HTML para escanear.
     */
    @PostMapping("/connect")
    public Map<String, Object> connect() {
        if (!configured) return Map.of("error", "Evolution API não configurada (base-url ou api-key ausentes)");
        try {
            ensureInstanceExists();

            JsonNode response = webClient.get()
                    .uri("/instance/connect/" + instance)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) return Map.of("error", "Sem resposta da Evolution API");

            String base64 = response.path("base64").asText(null);
            if (base64 == null || base64.isBlank()) {
                base64 = response.path("qrcode").path("base64").asText(null);
            }

            if (base64 != null) {
                return Map.of(
                        "status", "qrcode_gerado",
                        "instrucao", "Abra o WhatsApp > Dispositivos Vinculados > Vincular dispositivo > Escanear QR",
                        "base64", base64,
                        "imgTag", "<img src=\"" + base64 + "\">"
                );
            }
            String state = response.path("instance").path("state").asText("");
            if ("open".equals(state)) {
                return Map.of("status", "already_connected", "instance", instance);
            }
            return Map.of("status", "resposta_inesperada", "raw", response.toString());
        } catch (Exception e) {
            log.error("Erro ao conectar instância Evolution: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    private void ensureInstanceExists() {
        boolean exists = false;
        try {
            webClient.get()
                    .uri("/instance/connectionState/" + instance)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            exists = true;
            log.info("Instância '{}' já existe.", instance);
        } catch (Exception e) {
            log.info("Instância '{}' não encontrada ({}), criando...", instance, e.getMessage());
        }

        if (!exists) {
            try {
                JsonNode created = webClient.post()
                        .uri("/instance/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of(
                                "instanceName", instance,
                                "qrcode", true,
                                "integration", "WHATSAPP-BAILEYS"
                        ))
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();
                log.info("Instância '{}' criada: {}", instance, created);
            } catch (Exception e) {
                log.error("Falha ao criar instância '{}': {}", instance, e.getMessage());
                throw new RuntimeException("Falha ao criar instância Evolution: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Retorna o estado atual da conexão WhatsApp.
     * Valores possíveis: open (conectado), connecting, close (desconectado).
     */
    @GetMapping("/status")
    public Map<String, Object> status() {
        if (!configured) return Map.of("error", "Evolution API não configurada");
        try {
            JsonNode response = webClient.get()
                    .uri("/instance/connectionState/" + instance)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) return Map.of("error", "Sem resposta da Evolution API");

            String state = response.path("instance").path("state").asText(
                    response.path("state").asText("unknown")
            );
            return Map.of("state", state, "instance", instance);
        } catch (Exception e) {
            log.error("Erro ao verificar status Evolution: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Lista todos os grupos WhatsApp da instância com nome e JID.
     * Use o "jid" retornado como valor de EVOLUTION_NUMBER (ex: 120363xxx@g.us).
     */
    @GetMapping("/groups")
    public Map<String, Object> groups() {
        if (!configured) return Map.of("error", "Evolution API não configurada");
        try {
            JsonNode response = webClient.get()
                    .uri(u -> u.path("/group/fetchAllGroups/" + instance)
                            .queryParam("getParticipants", "false")
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) return Map.of("error", "Sem resposta da Evolution API");

            List<Map<String, String>> groups = new ArrayList<>();
            if (response.isArray()) {
                for (JsonNode group : response) {
                    String jid  = group.path("id").asText(group.path("remoteJid").asText(null));
                    String name = group.path("subject").asText(group.path("name").asText("(sem nome)"));
                    if (jid != null) groups.add(Map.of("name", name, "jid", jid));
                }
            }

            groups.sort((a, b) -> a.get("name").compareToIgnoreCase(b.get("name")));
            return Map.of(
                    "total", groups.size(),
                    "instrucao", "Copie o 'jid' do grupo desejado e configure como EVOLUTION_NUMBER",
                    "groups", groups
            );
        } catch (Exception e) {
            log.error("Erro ao listar grupos Evolution: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Lista todos os canais (newsletters) WhatsApp da instância com nome e JID.
     * Use o "jid" retornado como valor de EVOLUTION_NUMBER (ex: 120363xxx@newsletter).
     */
    @GetMapping("/channels")
    public Map<String, Object> channels() {
        if (!configured) return Map.of("error", "Evolution API não configurada");
        try {
            JsonNode response = webClient.post()
                    .uri("/chat/findChats/" + instance)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) return Map.of("error", "Sem resposta da Evolution API");

            List<Map<String, String>> channels = new ArrayList<>();
            if (response.isArray()) {
                for (JsonNode chat : response) {
                    String jid = chat.path("remoteJid").asText(chat.path("id").asText(null));
                    if (jid == null || !jid.endsWith("@newsletter")) continue;
                    String name = chat.path("name").asText(chat.path("pushName").asText("(sem nome)"));
                    channels.add(Map.of("name", name, "jid", jid));
                }
            }

            channels.sort((a, b) -> a.get("name").compareToIgnoreCase(b.get("name")));
            return Map.of(
                    "total", channels.size(),
                    "instrucao", "Copie o 'jid' do canal desejado e configure como EVOLUTION_NUMBER",
                    "channels", channels
            );
        } catch (Exception e) {
            log.error("Erro ao listar canais Evolution: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
}
