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

    // ── Ranking ──────────────────────────────────────────────────────────────

    public void sendRanking(List<RankingItemResponse> ranking) {
        if (!enabled || ranking.isEmpty()) return;
        sendMessage(formatRankingHeader(ranking.size()));
        for (RankingItemResponse item : ranking) {
            String caption = formatRankingCaption(item);
            String imageUrl = resolveImageUrl(item.imageUrl());
            if (imageUrl != null) {
                sendPhoto(imageUrl, caption);
            } else {
                sendMessage(caption);
            }
        }
    }

    // ── Promoções ─────────────────────────────────────────────────────────────

    public void sendPromotions(List<PromotionAlert> promotions) {
        if (!enabled || promotions.isEmpty()) return;
        sendMessage(formatPromotionHeader(promotions.size()));
        for (PromotionAlert p : promotions) {
            String caption = formatPromotionCaption(p);
            String imageUrl = resolveImageUrl(p.imageUrl());
            if (imageUrl != null) {
                sendPhoto(imageUrl, caption);
            } else {
                sendMessage(caption);
            }
        }
    }

    // ── Templates ─────────────────────────────────────────────────────────────

    /*
     * Promoção — cabeçalho
     *
     * 🔥 Promoções de Whey — 2 produtos
     * 📅 02/05/2026 20:00
     */
    private String formatPromotionHeader(int count) {
        return String.format(
                "🔥 <b>Promoções de Whey</b> — %d produto%s\n📅 %s",
                count, count == 1 ? "" : "s",
                LocalDateTime.now().format(FORMATTER)
        );
    }

    /*
     * Promoção — card (caption do /sendPhoto ou mensagem de fallback)
     *
     * 🏷 Whey Protein Isolado 900g
     * 🏪 Growth Supplements
     *
     * 📉 18,5% abaixo da média de 7 dias
     * 💰 De R$ 110,29 → R$ 89,99
     * 💪 R$ 0,0891/g de proteína
     *
     * 🛒 Comprar agora
     */
    private String formatPromotionCaption(PromotionAlert p) {
        BigDecimal discountPct = p.discountPercent().multiply(BigDecimal.valueOf(100));
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🏷 <b>%s</b>\n", p.name()));
        sb.append(String.format("🏪 %s\n\n", storeLabel(p.store())));
        sb.append(String.format("📉 <b>%.1f%%</b> abaixo da média de 7 dias\n", discountPct));
        sb.append(String.format("💰 De R$ %.2f → <b>R$ %.2f</b>\n",
                p.averagePrice(), p.currentPrice()));
        sb.append(String.format("💪 <b>R$ %.4f</b>/g de proteína\n", p.pricePerProteinGram()));
        if (p.productUrl() != null) {
            sb.append(String.format("\n🛒 <a href=\"%s\">Comprar agora</a>", p.productUrl()));
        }
        return sb.toString();
    }

    /*
     * Ranking — cabeçalho
     *
     * 🏆 Ranking de Whey — Top 10
     * 📅 02/05/2026 20:00
     * 💡 Ordenado por custo por grama de proteína
     */
    private String formatRankingHeader(int total) {
        return String.format(
                "🏆 <b>Ranking de Whey</b> — Top %d\n📅 %s\n💡 <i>Ordenado por custo/g de proteína</i>",
                total,
                LocalDateTime.now().format(FORMATTER)
        );
    }

    /*
     * Ranking — card por produto (caption do /sendPhoto ou mensagem de fallback)
     *
     * #1  Whey Protein Isolado 900g
     * 🏪 Growth Supplements  •  💪 270g de proteína total
     *
     * 💰 R$ 89,99  →  R$ 0,0891/g prot.
     *
     * 🔗 Ver produto
     */
    private String formatRankingCaption(RankingItemResponse item) {
        BigDecimal effectivePrice = item.cashPrice() != null ? item.cashPrice() : item.price();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("<b>#%d  %s</b>\n", item.position(), item.name()));
        sb.append(String.format("🏪 %s", storeLabel(item.store())));
        if (item.totalProteinGrams() != null) {
            sb.append(String.format("  •  💪 %.0fg de proteína total", item.totalProteinGrams()));
        }
        sb.append("\n\n");
        sb.append(String.format("💰 R$ %.2f  →  <b>R$ %.4f/g prot.</b>\n",
                effectivePrice, item.pricePerProteinGram()));
        if (item.productUrl() != null) {
            sb.append(String.format("\n🔗 <a href=\"%s\">Ver produto</a>", item.productUrl()));
        }
        return sb.toString();
    }

    // ── Utilitários ───────────────────────────────────────────────────────────

    /*
     * Garante que apenas URLs absolutas (http/https) são enviadas ao Telegram.
     * URLs relativas ou nulas resultam em null, forçando fallback para texto.
     * ML thumbnail: troca sufixo -I.jpg/-I.webp por -O.jpg para resolução maior.
     */
    private String resolveImageUrl(String url) {
        if (url == null || url.isBlank()) return null;
        if (!url.startsWith("http://") && !url.startsWith("https://")) return null;
        // Mercado Livre: upgrade de thumbnail para imagem original
        return url.replaceAll("-[A-Z]\\.jpg$", "-O.jpg")
                  .replaceAll("-[A-Z]\\.webp$", "-O.webp");
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

    // ── Telegram API ──────────────────────────────────────────────────────────

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
            log.error("Falha ao enviar mensagem Telegram: {}", e.getMessage());
        }
    }

    private void sendPhoto(String photoUrl, String caption) {
        try {
            webClient.post()
                    .uri("/sendPhoto")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.<String, Object>of(
                            "chat_id", chatId,
                            "photo", photoUrl,
                            "caption", caption,
                            "parse_mode", "HTML"
                    ))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("Falha ao enviar foto Telegram (url={}): {}", photoUrl, e.getMessage());
        }
    }
}
