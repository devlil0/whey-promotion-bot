package com.devlil0.whey_promotion_bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.devlil0.whey_promotion_bot.dto.PromotionAlert;
import com.devlil0.whey_promotion_bot.dto.RankingItemResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class TelegramNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final WebClient webClient;
    private final String chatId;
    private final boolean enabled;

    public TelegramNotificationService(
            WebClient.Builder builder,
            @Value("${telegram.bot-token:}") String botToken,
            @Value("${telegram.chat-id:}") String chatId
    ) {
        this.chatId = chatId;
        this.enabled = !botToken.isBlank() && !chatId.isBlank();
        this.webClient = builder
                .baseUrl("https://api.telegram.org/bot" + botToken)
                .build();
    }

    public void sendRanking(List<RankingItemResponse> ranking) {
        if (!enabled || ranking.isEmpty()) return;
        sendMessage(formatRanking(ranking));
    }

    public void sendPromotions(List<PromotionAlert> promotions) {
        if (!enabled || promotions.isEmpty()) return;
        sendMessage(formatPromotions(promotions));
    }

    private void sendMessage(String text) {
        try {
            webClient.post()
                    .uri("/sendMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.<String, Object>of(
                            "chat_id", chatId,
                            "text", text,
                            "parse_mode", "HTML",
                            "disable_web_page_preview", true
                    ))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("Falha ao enviar notificação Telegram: {}", e.getMessage());
        }
    }

    private String formatRanking(List<RankingItemResponse> ranking) {
        StringBuilder sb = new StringBuilder();
        sb.append("🏆 <b>Ranking Whey — Custo por Grama de Proteína</b>\n");
        sb.append("📅 ").append(LocalDateTime.now().format(FORMATTER)).append("\n\n");

        for (RankingItemResponse item : ranking) {
            BigDecimal effectivePrice = item.cashPrice() != null ? item.cashPrice() : item.price();
            sb.append(String.format("<b>#%d %s</b>\n", item.position(), item.name()));
            sb.append(String.format("🏪 %s | 💪 %.0fg proteína\n",
                    storeLabel(item.store()), item.totalProteinGrams()));
            sb.append(String.format("💰 R$ %.2f → <b>R$ %.4f/g prot.</b>\n",
                    effectivePrice, item.pricePerProteinGram()));
            if (item.productUrl() != null) {
                sb.append(String.format("🔗 <a href=\"%s\">Ver produto</a>\n", item.productUrl()));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String formatPromotions(List<PromotionAlert> promotions) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔥 <b>Promoções detectadas — Whey</b>\n");
        sb.append("📅 ").append(LocalDateTime.now().format(FORMATTER)).append("\n\n");

        for (PromotionAlert p : promotions) {
            BigDecimal discountPct = p.discountPercent().multiply(BigDecimal.valueOf(100));
            sb.append(String.format("<b>%s</b>\n", p.name()));
            sb.append(String.format("🏪 %s | 🔻 %.1f%% abaixo da média de 7 dias\n",
                    storeLabel(p.store()), discountPct));
            sb.append(String.format("💰 R$ %.2f (média R$ %.2f) | <b>R$ %.4f/g prot.</b>\n",
                    p.currentPrice(), p.averagePrice(), p.pricePerProteinGram()));
            if (p.productUrl() != null) {
                sb.append(String.format("🔗 <a href=\"%s\">Ver produto</a>\n", p.productUrl()));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String storeLabel(String store) {
        return switch (store) {
            case "GROWTH" -> "Growth Supplements";
            case "DARK_LAB" -> "Dark Lab";
            case "PROFIT_LABS" -> "ProFit Labs";
            case "MERCADO_LIVRE" -> "Mercado Livre";
            default -> store;
        };
    }
}
