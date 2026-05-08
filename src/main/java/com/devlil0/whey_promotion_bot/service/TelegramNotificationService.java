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
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
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
public class TelegramNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final Locale BR_LOCALE = new Locale("pt", "BR");
    private static final DecimalFormat CURRENCY_FORMAT =
            new DecimalFormat("'R$' #,##0.00", DecimalFormatSymbols.getInstance(BR_LOCALE));
    private static final DecimalFormat ONE_DECIMAL_FORMAT =
            new DecimalFormat("#,##0.0", DecimalFormatSymbols.getInstance(BR_LOCALE));

    private final WebClient webClient;
    private final String chatId;
    private final boolean enabled;
    private final ImageProcessingService imageProcessingService;
    private final GroqMessageService groq;

    public TelegramNotificationService(
            WebClient.Builder builder,
            @Value("${telegram.bot-token:}") String botToken,
            @Value("${telegram.chat-id:}") String chatId,
            ImageProcessingService imageProcessingService,
            GroqMessageService groq
    ) {
        this.chatId = chatId;
        this.enabled = !botToken.isBlank() && !chatId.isBlank();
        this.webClient = builder
                .baseUrl("https://api.telegram.org/bot" + botToken)
                .build();
        this.imageProcessingService = imageProcessingService;
        this.groq = groq;
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

    // ── Ofertas por faixa (Growth / Profit Labs) ─────────────────────────────

    public void sendOfertas(List<OfertasFaixaResponse> bands, String storeLabel) {
        if (!enabled || bands.isEmpty()) return;
        int total = bands.stream().mapToInt(OfertasFaixaResponse::quantidade).sum();
        sendMessage(String.format(
                "🔥 <b>Ofertas %s</b>\n%d produto%s encontrados\n📅 %s",
                storeLabel, total, total == 1 ? "" : "s",
                ZonedDateTime.now(SAO_PAULO).format(FORMATTER)
        ));
        for (OfertasFaixaResponse band : bands) {
            sendMessage(String.format("💰 <b>%s</b>", band.faixa()));
            for (ProductOfferResponse p : band.produtos()) {
                String caption = formatOfertaCaption(p, "🔥 Oferta encontrada");
                String imageUrl = resolveImageUrl(p.imageUrl());
                if (imageUrl != null) sendPhoto(imageUrl, caption);
                else sendMessage(caption);
            }
        }
    }

    // ── Oferta Relâmpago (Soldiers) ───────────────────────────────────────────

    public void sendOfertaRelampago(List<ProductOfferResponse> products) {
        if (!enabled || products.isEmpty()) return;
        sendMessage(String.format(
                "⚡ <b>Oferta Relâmpago — Soldiers Nutrition</b> — %d produto%s\n📅 %s",
                products.size(), products.size() == 1 ? "" : "s",
                ZonedDateTime.now(SAO_PAULO).format(FORMATTER)
        ));
        for (ProductOfferResponse p : products) {
            String caption = formatOfertaCaption(p, "⚡ Oferta relâmpago");
            String imageUrl = resolveImageUrl(p.imageUrl());
            if (imageUrl != null) sendPhoto(imageUrl, caption);
            else sendMessage(caption);
        }
    }

    // ── Templates ─────────────────────────────────────────────────────────────

    /*
     * 🔥 Oferta encontrada
     * <b>Whey Blend Protein 900g</b>
     * Soldiers Nutrition
     * R$ 56,91
     * 900g
     * 🔗 Ver produto
     */
    private String formatOfertaCaption(ProductOfferResponse p, String badge) {
        String ai = groq.generateOfertaCaption(p, badge, GroqMessageService.Format.HTML);
        if (ai != null) return p.productUrl() != null
                ? ai + "\n\n🔗 Comprar agora na " + storeLabel(p.store()) + ":\n<a href=\"" + p.productUrl() + "\">" + p.productUrl() + "</a>"
                : ai;

        BigDecimal effectivePrice = p.cashPrice() != null ? p.cashPrice() : p.price();
        StringBuilder sb = new StringBuilder();
        sb.append(badge).append("\n");
        sb.append(String.format("<b>%s</b>\n", p.name()));
        sb.append(String.format("%s\n", storeLabel(p.store())));
        if (p.oldPrice() != null && effectivePrice != null) {
            sb.append(String.format("<s>%s</s> → <b>%s</b>\n", formatCurrency(p.oldPrice()), formatCurrency(effectivePrice)));
        } else if (effectivePrice != null) {
            sb.append(formatCurrency(effectivePrice)).append("\n");
        }
        if (p.cashPrice() != null && p.price() != null && p.cashPrice().compareTo(p.price()) < 0) {
            sb.append("<i>no pix</i>\n");
        }
        if (p.weightGrams() != null) sb.append(String.format("%dg\n", p.weightGrams()));
        if (p.productUrl() != null) {
            sb.append(String.format("\n\n🔗 Comprar agora na %s:\n<a href=\"%s\">%s</a>", storeLabel(p.store()), p.productUrl(), p.productUrl()));
        }
        return sb.toString();
    }

    /*
     * Ranking — cabeçalho
     *
     * 🏆 Ranking Diário de Whey — Top 10
     * 📅 03/05/2026 08:05
     */
    private String formatRankingHeader(int total) {
        return String.format(
                "🏆 <b>Ranking Diário de Whey</b> — Top %d\n📅 %s",
                total,
                ZonedDateTime.now(SAO_PAULO).format(FORMATTER)
        );
    }

    /*
     * 🥇 Melhor custo-benefício
     * <b>Whey Blend Protein 900g</b>
     * Soldiers Nutrition
     * R$ 56,91
     * 7,1 centavos/g de proteína
     * 900g · 20g de proteína por dose
     * 🔗 Ver produto
     */
    private String formatRankingCaption(RankingItemResponse item) {
        String ai = groq.generateRankingCaption(item, GroqMessageService.Format.HTML);
        if (ai != null) return item.productUrl() != null
                ? ai + "\n\n🔗 Comprar agora na " + storeLabel(item.store()) + ":\n<a href=\"" + item.productUrl() + "\">" + item.productUrl() + "</a>"
                : ai;

        BigDecimal effectivePrice = item.cashPrice() != null ? item.cashPrice() : item.price();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s Melhor custo-benefício\n", positionMedal(item.position())));
        sb.append(String.format("<b>%s</b>\n", item.name()));
        sb.append(String.format("%s\n", storeLabel(item.store())));
        if (effectivePrice != null) sb.append(formatCurrency(effectivePrice)).append("\n");
        sb.append(String.format("<b>%s centavos/g de proteína</b>\n", formatCentavos(item.pricePerProteinGram())));
        if (item.weightGrams() != null || item.proteinPerServingGrams() != null) {
            String weight = item.weightGrams() != null ? item.weightGrams() + "g" : "";
            String protein = item.proteinPerServingGrams() != null
                    ? formatGrams(item.proteinPerServingGrams()) + "g de proteína por dose" : "";
            if (!weight.isBlank() && !protein.isBlank()) sb.append(weight).append(" · ").append(protein).append("\n");
            else sb.append(weight).append(protein).append("\n");
        }
        if (item.productUrl() != null) {
            sb.append(String.format("\n\n🔗 Comprar agora na %s:\n<a href=\"%s\">%s</a>", storeLabel(item.store()), item.productUrl(), item.productUrl()));
        }
        return sb.toString();
    }

    /*
     * Promoção — cabeçalho
     *
     * 📉 Alertas de Promoção — 2 produtos
     * 📅 03/05/2026 08:00
     */
    private String formatPromotionHeader(int count) {
        return String.format(
                "📉 <b>Alertas de Promoção</b> — %d produto%s\n📅 %s",
                count, count == 1 ? "" : "s",
                ZonedDateTime.now(SAO_PAULO).format(FORMATTER)
        );
    }

    /*
     * 📉 Preço caiu
     * <b>Whey Protein Isolado 900g</b>
     * Growth Supplements
     * <s>R$ 110,29</s> → R$ 89,99
     * 18,5% abaixo da média (7 dias)
     * 900g · 20g de proteína por dose
     * 🔗 Comprar agora
     */
    private String formatPromotionCaption(PromotionAlert p) {
        String ai = groq.generatePromotionCaption(p, GroqMessageService.Format.HTML);
        if (ai != null) return p.productUrl() != null
                ? ai + "\n\n🔗 Comprar agora na " + storeLabel(p.store()) + ":\n<a href=\"" + p.productUrl() + "\">" + p.productUrl() + "</a>"
                : ai;

        BigDecimal discountPct = p.discountPercent().multiply(BigDecimal.valueOf(100));
        StringBuilder sb = new StringBuilder();
        sb.append("📉 Preço caiu\n");
        sb.append(String.format("<b>%s</b>\n", p.name()));
        sb.append(String.format("%s\n", storeLabel(p.store())));
        sb.append(String.format("<s>%s</s> → <b>%s</b>\n", formatCurrency(p.averagePrice()), formatCurrency(p.currentPrice())));
        sb.append(String.format("<b>%.1f%%</b> abaixo da média (7 dias)\n", discountPct));
        sb.append(String.format("%s centavos/g de proteína\n", formatCentavos(p.pricePerProteinGram())));
        if (p.weightGrams() != null || p.proteinPerServingGrams() != null) {
            String weight = p.weightGrams() != null ? p.weightGrams() + "g" : "";
            String protein = p.proteinPerServingGrams() != null
                    ? formatGrams(p.proteinPerServingGrams()) + "g de proteína por dose" : "";
            if (!weight.isBlank() && !protein.isBlank()) sb.append(weight).append(" · ").append(protein).append("\n");
            else sb.append(weight).append(protein).append("\n");
        }
        if (p.productUrl() != null) {
            sb.append(String.format("\n\n🔗 Comprar agora na %s:\n<a href=\"%s\">%s</a>", storeLabel(p.store()), p.productUrl(), p.productUrl()));
        }
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
            byte[] imageBytes = imageProcessingService.enhance(photoUrl);
            if (imageBytes != null) {
                boolean isPng = imageBytes.length >= 4
                        && imageBytes[0] == (byte) 0x89 && imageBytes[1] == 'P'
                        && imageBytes[2] == 'N' && imageBytes[3] == 'G';
                String filename = isPng ? "photo.png" : "photo.jpg";
                MediaType mediaType = isPng ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
                MultipartBodyBuilder builder = new MultipartBodyBuilder();
                builder.part("chat_id", chatId);
                builder.part("photo", imageBytes).filename(filename).contentType(mediaType);
                builder.part("caption", caption);
                builder.part("parse_mode", "HTML");
                webClient.post()
                        .uri("/sendPhoto")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(builder.build()))
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();
            } else {
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
            }
        } catch (Exception e) {
            log.error("Falha ao enviar foto Telegram (url={}): {}", photoUrl, e.getMessage());
        }
    }
}
