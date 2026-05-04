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
     * Ranking — cabeçalho
     *
     * 🏆 Ranking Diário de Whey — Top 10
     * 📅 03/05/2026 08:05
     * 💡 Ordenado por custo/g de proteína
     */
    private String formatRankingHeader(int total) {
        return String.format(
                "🏆 <b>Ranking Diário de Whey</b> — Top %d\n📅 %s\n💡 <i>Ordenado por custo/g de proteína</i>",
                total,
                LocalDateTime.now().format(FORMATTER)
        );
    }

    /*
     * Ranking — card por produto
     *
     * 🥇 Whey Protein Isolado 900g
     * 🏪 Growth Supplements
     *
     * ⚖️ 900g  •  🧬 270g de proteína
     *
     * 💰 R$ 89,99
     * 📊 R$ 0,0891/g de proteína
     *
     * 🔗 Ver produto
     */
    private String formatRankingCaption(RankingItemResponse item) {
        BigDecimal effectivePrice = item.cashPrice() != null ? item.cashPrice() : item.price();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s <b>%s</b>\n", positionMedal(item.position()), item.name()));
        sb.append(String.format("🏪 %s\n\n", storeLabel(item.store())));
        if (item.weightGrams() != null || item.totalProteinGrams() != null) {
            if (item.weightGrams() != null) sb.append(String.format("⚖️ %dg", item.weightGrams()));
            if (item.weightGrams() != null && item.totalProteinGrams() != null) sb.append("  •  ");
            if (item.totalProteinGrams() != null) sb.append(String.format("🧬 %.0fg de proteína", item.totalProteinGrams()));
            sb.append("\n\n");
        }
        sb.append(String.format("💰 R$ %.2f\n", effectivePrice));
        sb.append(String.format("📊 <b>R$ %.4f/g de proteína</b>\n", item.pricePerProteinGram()));
        if (item.productUrl() != null) {
            sb.append(String.format("\n🔗 <a href=\"%s\">Ver produto</a>", item.productUrl()));
        }
        return sb.toString();
    }

    /*
     * Promoção — cabeçalho
     *
     * 🔥 Alertas de Promoção — 2 produtos
     * 📅 03/05/2026 08:00
     */
    private String formatPromotionHeader(int count) {
        return String.format(
                "🔥 <b>Alertas de Promoção</b> — %d produto%s\n📅 %s",
                count, count == 1 ? "" : "s",
                LocalDateTime.now().format(FORMATTER)
        );
    }

    /*
     * Promoção — card
     *
     * 🏷 Whey Protein Isolado 900g
     * 🏪 Growth Supplements
     *
     * 📉 18,5% de desconto vs. média (7 dias)
     * 💸 R$ 110,29 → R$ 89,99
     * 🧬 R$ 0,0891/g de proteína
     *
     * 🛒 Comprar agora
     */
    private String formatPromotionCaption(PromotionAlert p) {
        BigDecimal discountPct = p.discountPercent().multiply(BigDecimal.valueOf(100));
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🏷 <b>%s</b>\n", p.name()));
        sb.append(String.format("🏪 %s\n\n", storeLabel(p.store())));
        sb.append(String.format("📉 <b>%.1f%%</b> de desconto vs. média (7 dias)\n", discountPct));
        sb.append(String.format("💸 <s>R$ %.2f</s> → <b>R$ %.2f</b>\n",
                p.averagePrice(), p.currentPrice()));
        sb.append(String.format("🧬 <b>R$ %.4f</b>/g de proteína\n", p.pricePerProteinGram()));
        if (p.productUrl() != null) {
            sb.append(String.format("\n🛒 <a href=\"%s\">Comprar agora</a>", p.productUrl()));
        }
        return sb.toString();
    }

    // ── Utilitários ───────────────────────────────────────────────────────────

    private String positionMedal(int position) {
        return switch (position) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> "#" + position;
        };
    }

    private String resolveImageUrl(String url) {
        if (url == null || url.isBlank()) return null;
        if (!url.startsWith("http://") && !url.startsWith("https://")) return null;
        return url;
    }

    private String storeLabel(String store) {
        return switch (store) {
            case "GROWTH"             -> "Growth Supplements";
            case "DARK_LAB"           -> "Dark Lab";
            case "PROFIT_LABS"        -> "ProFit Labs";
            case "SOLDIERS_NUTRITION" -> "Soldiers Nutrition";
            case "BLACK_SKULL"        -> "Black Skull";
            case "NUTRATA"            -> "Nutrata";
            case "ADAPTOGEN"          -> "Adaptogen";
            case "ABSOLUT_NUTRITION"  -> "Absolut Nutrition";
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
