package com.devlil0.whey_promotion_bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.devlil0.whey_promotion_bot.dto.OfertasFaixaResponse;
import com.devlil0.whey_promotion_bot.dto.ProductOfferResponse;
import com.devlil0.whey_promotion_bot.dto.PromotionAlert;
import com.devlil0.whey_promotion_bot.dto.RankingItemResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EvolutionNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EvolutionNotificationService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale BR_LOCALE = new Locale("pt", "BR");
    private static final DecimalFormat CURRENCY_FORMAT =
            new DecimalFormat("'R$' #,##0.00", DecimalFormatSymbols.getInstance(BR_LOCALE));
    private static final DecimalFormat ONE_DECIMAL_FORMAT =
            new DecimalFormat("#,##0.0", DecimalFormatSymbols.getInstance(BR_LOCALE));

    private final WebClient webClient;
    private final String instance;
    private final String number;
    private final boolean enabled;
    private final GroqMessageService groq;

    public EvolutionNotificationService(
            WebClient.Builder builder,
            @Value("${evolution.api.base-url:}") String baseUrl,
            @Value("${evolution.api.api-key:}") String apiKey,
            @Value("${evolution.api.instance:}") String instance,
            @Value("${evolution.api.number:}") String number,
            GroqMessageService groq
    ) {
        this.instance = instance;
        this.number = number;
        this.enabled = !baseUrl.isBlank() && !apiKey.isBlank() && !instance.isBlank() && !number.isBlank();
        this.groq = groq;
        this.webClient = builder
                .baseUrl(baseUrl.isBlank() ? "http://localhost:8081" : baseUrl)
                .defaultHeader("apikey", apiKey)
                .build();
    }

    // ── Public methods ────────────────────────────────────────────────────────

    public void sendRanking(List<RankingItemResponse> ranking) {
        if (!enabled || ranking.isEmpty()) return;
        sendText(formatRankingHeader(ranking.size()));
        for (RankingItemResponse item : ranking) {
            String caption = formatRankingCaption(item);
            String imageUrl = resolveImageUrl(item.imageUrl());
            if (imageUrl != null) sendMedia(imageUrl, caption);
            else sendText(caption);
        }
    }

    public void sendPromotions(List<PromotionAlert> promotions) {
        if (!enabled || promotions.isEmpty()) return;
        sendText(formatPromotionHeader(promotions.size()));
        for (PromotionAlert p : promotions) {
            String caption = formatPromotionCaption(p);
            String imageUrl = resolveImageUrl(p.imageUrl());
            if (imageUrl != null) sendMedia(imageUrl, caption);
            else sendText(caption);
        }
    }

    public void sendOfertas(List<OfertasFaixaResponse> bands, String storeLabel) {
        if (!enabled || bands.isEmpty()) return;
        int total = bands.stream().mapToInt(OfertasFaixaResponse::quantidade).sum();
        sendText(String.format(
                "🔥 *Ofertas %s*\n%d produto%s encontrados\n📅 %s",
                storeLabel, total, total == 1 ? "" : "s",
                LocalDateTime.now().format(FORMATTER)
        ));
        for (OfertasFaixaResponse band : bands) {
            sendText(String.format("💰 *%s*", band.faixa()));
            for (ProductOfferResponse p : band.produtos()) {
                String caption = formatOfertaCaption(p, "🔥 Oferta encontrada");
                String imageUrl = resolveImageUrl(p.imageUrl());
                if (imageUrl != null) sendMedia(imageUrl, caption);
                else sendText(caption);
            }
        }
    }

    public void sendOfertaRelampago(List<ProductOfferResponse> products) {
        if (!enabled || products.isEmpty()) return;
        sendText(String.format(
                "⚡ *Oferta Relâmpago — Soldiers Nutrition* — %d produto%s\n📅 %s",
                products.size(), products.size() == 1 ? "" : "s",
                LocalDateTime.now().format(FORMATTER)
        ));
        for (ProductOfferResponse p : products) {
            String caption = formatOfertaCaption(p, "⚡ Oferta relâmpago");
            String imageUrl = resolveImageUrl(p.imageUrl());
            if (imageUrl != null) sendMedia(imageUrl, caption);
            else sendText(caption);
        }
    }

    // ── Templates (WhatsApp markdown) ─────────────────────────────────────────

    private String formatOfertaCaption(ProductOfferResponse p, String badge) {
        String ai = groq.generateOfertaCaption(p, badge);
        if (ai != null) return p.productUrl() != null ? ai + "\n\n🔗 Comprar agora: " + p.productUrl() : ai;

        BigDecimal effectivePrice = p.cashPrice() != null ? p.cashPrice() : p.price();
        StringBuilder sb = new StringBuilder();
        sb.append(badge).append("\n");
        sb.append(String.format("*%s*\n", p.name()));
        sb.append(String.format("%s\n", storeLabel(p.store())));
        if (p.oldPrice() != null && effectivePrice != null) {
            sb.append(String.format("~%s~ → *%s*\n", formatCurrency(p.oldPrice()), formatCurrency(effectivePrice)));
        } else if (effectivePrice != null) {
            sb.append(formatCurrency(effectivePrice)).append("\n");
        }
        if (p.cashPrice() != null && p.price() != null && p.cashPrice().compareTo(p.price()) < 0) {
            sb.append("_no pix_\n");
        }
        if (p.weightGrams() != null) sb.append(String.format("%dg\n", p.weightGrams()));
        if (p.productUrl() != null) sb.append(String.format("\n🔗 Comprar agora: %s", p.productUrl()));
        return sb.toString();
    }

    private String formatRankingHeader(int total) {
        return String.format(
                "🏆 *Ranking Diário de Whey* — Top %d\n📅 %s",
                total,
                LocalDateTime.now().format(FORMATTER)
        );
    }

    private String formatRankingCaption(RankingItemResponse item) {
        String ai = groq.generateRankingCaption(item);
        if (ai != null) return item.productUrl() != null ? ai + "\n\n🔗 Comprar agora: " + item.productUrl() : ai;

        BigDecimal effectivePrice = item.cashPrice() != null ? item.cashPrice() : item.price();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s Melhor custo-benefício\n", positionMedal(item.position())));
        sb.append(String.format("*%s*\n", item.name()));
        sb.append(String.format("%s\n", storeLabel(item.store())));
        if (effectivePrice != null) sb.append(formatCurrency(effectivePrice)).append("\n");
        sb.append(String.format("*%s centavos/g de proteína*\n", formatCentavos(item.pricePerProteinGram())));
        if (item.weightGrams() != null || item.proteinPerServingGrams() != null) {
            String weight = item.weightGrams() != null ? item.weightGrams() + "g" : "";
            String protein = item.proteinPerServingGrams() != null
                    ? formatGrams(item.proteinPerServingGrams()) + "g de proteína por dose" : "";
            if (!weight.isBlank() && !protein.isBlank()) sb.append(weight).append(" · ").append(protein).append("\n");
            else sb.append(weight).append(protein).append("\n");
        }
        if (item.productUrl() != null) sb.append(String.format("\n🔗 Comprar agora: %s", item.productUrl()));
        return sb.toString();
    }

    private String formatPromotionHeader(int count) {
        return String.format(
                "📉 *Alertas de Promoção* — %d produto%s\n📅 %s",
                count, count == 1 ? "" : "s",
                LocalDateTime.now().format(FORMATTER)
        );
    }

    private String formatPromotionCaption(PromotionAlert p) {
        String ai = groq.generatePromotionCaption(p);
        if (ai != null) return p.productUrl() != null ? ai + "\n\n🔗 Comprar agora: " + p.productUrl() : ai;

        BigDecimal discountPct = p.discountPercent().multiply(BigDecimal.valueOf(100));
        StringBuilder sb = new StringBuilder();
        sb.append("📉 Preço caiu\n");
        sb.append(String.format("*%s*\n", p.name()));
        sb.append(String.format("%s\n", storeLabel(p.store())));
        sb.append(String.format("~%s~ → *%s*\n", formatCurrency(p.averagePrice()), formatCurrency(p.currentPrice())));
        sb.append(String.format("*%.1f%%* abaixo da média (7 dias)\n", discountPct));
        sb.append(String.format("%s centavos/g de proteína\n", formatCentavos(p.pricePerProteinGram())));
        if (p.weightGrams() != null || p.proteinPerServingGrams() != null) {
            String weight = p.weightGrams() != null ? p.weightGrams() + "g" : "";
            String protein = p.proteinPerServingGrams() != null
                    ? formatGrams(p.proteinPerServingGrams()) + "g de proteína por dose" : "";
            if (!weight.isBlank() && !protein.isBlank()) sb.append(weight).append(" · ").append(protein).append("\n");
            else sb.append(weight).append(protein).append("\n");
        }
        if (p.productUrl() != null) sb.append(String.format("\n🔗 Comprar agora: %s", p.productUrl()));
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

    private String formatCurrency(BigDecimal value) {
        return CURRENCY_FORMAT.format(value);
    }

    private String formatCentavos(BigDecimal pricePerProteinGram) {
        return ONE_DECIMAL_FORMAT.format(pricePerProteinGram.multiply(BigDecimal.valueOf(100)));
    }

    private String formatGrams(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString().replace(".", ",");
    }

    // ── Evolution API HTTP ────────────────────────────────────────────────────

    private void sendText(String text) {
        try {
            webClient.post()
                    .uri("/message/sendText/" + instance)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("number", number, "text", text))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("Falha ao enviar texto Evolution API: {}", e.getMessage());
        }
    }

    private void sendMedia(String mediaUrl, String caption) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("number", number);
            body.put("mediatype", "image");
            body.put("mimetype", mimeTypeFromUrl(mediaUrl));
            body.put("caption", caption);
            body.put("media", mediaUrl);
            webClient.post()
                    .uri("/message/sendMedia/" + instance)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("Falha ao enviar mídia Evolution API (url={}): {}", mediaUrl, e.getMessage());
            sendText(caption);
        }
    }

    private String mimeTypeFromUrl(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".png")) return "image/png";
        if (lower.contains(".webp")) return "image/webp";
        if (lower.contains(".gif")) return "image/gif";
        return "image/jpeg";
    }
}
