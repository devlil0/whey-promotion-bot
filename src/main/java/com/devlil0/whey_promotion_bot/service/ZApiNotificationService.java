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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ZApiNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ZApiNotificationService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final Locale BR_LOCALE = new Locale("pt", "BR");
    private static final DecimalFormat CURRENCY_FORMAT =
            new DecimalFormat("'R$' #,##0.00", DecimalFormatSymbols.getInstance(BR_LOCALE));
    private static final DecimalFormat ONE_DECIMAL_FORMAT =
            new DecimalFormat("#,##0.0", DecimalFormatSymbols.getInstance(BR_LOCALE));

    private final WebClient webClient;
    private final String phone;
    private final boolean enabled;
    private final GroqMessageService groq;

    public ZApiNotificationService(
            WebClient.Builder builder,
            @Value("${zapi.instance-id:}") String instanceId,
            @Value("${zapi.instance-token:}") String instanceToken,
            @Value("${zapi.client-token:}") String clientToken,
            @Value("${zapi.phone:}") String phone,
            GroqMessageService groq
    ) {
        this.phone = phone;
        this.enabled = !instanceId.isBlank() && !instanceToken.isBlank()
                && !clientToken.isBlank() && !phone.isBlank();
        this.groq = groq;
        this.webClient = builder
                .baseUrl("https://api.z-api.io/instances/" + instanceId + "/token/" + instanceToken)
                .defaultHeader("Client-Token", clientToken)
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
                ZonedDateTime.now(SAO_PAULO).format(FORMATTER)
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
                ZonedDateTime.now(SAO_PAULO).format(FORMATTER)
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
                ZonedDateTime.now(SAO_PAULO).format(FORMATTER)
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
                ZonedDateTime.now(SAO_PAULO).format(FORMATTER)
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
        if (position == 1) return "🥇";
        if (position == 2) return "🥈";
        if (position == 3) return "🥉";
        return "#" + position;
    }

    private String resolveImageUrl(String url) {
        if (url == null || url.isBlank()) return null;
        if (!url.startsWith("http://") && !url.startsWith("https://")) return null;
        return url;
    }

    private String storeLabel(String store) {
        if (store.equals("GROWTH"))             return "Growth Supplements";
        if (store.equals("DARK_LAB"))           return "Dark Lab";
        if (store.equals("PROFIT_LABS"))        return "ProFit Labs";
        if (store.equals("SOLDIERS_NUTRITION")) return "Soldiers Nutrition";
        if (store.equals("BLACK_SKULL"))        return "Black Skull";
        if (store.equals("NUTRATA"))            return "Nutrata";
        if (store.equals("ADAPTOGEN"))          return "Adaptogen";
        if (store.equals("ABSOLUT_NUTRITION"))  return "Absolut Nutrition";
        return store;
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

    // ── Z-API HTTP ────────────────────────────────────────────────────────────

    private void sendText(String text) {
        try {
            webClient.post()
                    .uri("/send-text")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("phone", phone, "message", text))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("Falha ao enviar texto Z-API: {}", e.getMessage());
        }
    }

    private void sendMedia(String mediaUrl, String caption) {
        try {
            webClient.post()
                    .uri("/send-image")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("phone", phone, "image", mediaUrl, "caption", caption))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("Falha ao enviar mídia Z-API (url={}): {}", mediaUrl, e.getMessage());
            sendText(caption);
        }
    }
}
