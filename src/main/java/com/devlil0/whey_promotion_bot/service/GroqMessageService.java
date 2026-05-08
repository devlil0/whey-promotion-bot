package com.devlil0.whey_promotion_bot.service;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class GroqMessageService {

    private static final Logger log = LoggerFactory.getLogger(GroqMessageService.class);
    private static final Locale BR_LOCALE = new Locale("pt", "BR");
    private static final DecimalFormat CURRENCY_FORMAT =
            new DecimalFormat("'R$' #,##0.00", DecimalFormatSymbols.getInstance(BR_LOCALE));
    private static final DecimalFormat ONE_DECIMAL =
            new DecimalFormat("#,##0.0", DecimalFormatSymbols.getInstance(BR_LOCALE));

    private static final String SYSTEM_PROMPT_WHATSAPP = """
            Você é curador de ofertas de suplementos esportivos para um grupo de WhatsApp.
            Seu estilo é direto, confiante e levemente persuasivo.
            Use markdown do WhatsApp: *negrito*, ~tachado~.
            Siga EXATAMENTE esta estrutura (blocos separados por linha em branco):

            *📌 NOME DO PRODUTO EM CAIXA ALTA*

            Uma frase persuasiva curta sobre o produto ou a oportunidade.

            🔥 ~De R$ X,XX~ por *R$ Y,YY* (Z% OFF)
            (se não houver preço antigo, mostre apenas: 🔥 *R$ Y,YY*)

            Adapte o entusiasmo à relevância: 1º lugar merece empolgação, 8º-10º merece tom factual.
            Não inclua o link — ele será adicionado automaticamente.
            Responda SOMENTE com a mensagem final, sem explicações adicionais.
            """;

    private static final String SYSTEM_PROMPT_HTML = """
            Você é curador de ofertas de suplementos esportivos para um canal do Telegram.
            Seu estilo é direto, confiante e levemente persuasivo.
            Use HTML do Telegram: <b>negrito</b>, <i>itálico</i>, <s>tachado</s>.
            Siga EXATAMENTE esta estrutura (blocos separados por linha em branco):

            <b>📌 NOME DO PRODUTO EM CAIXA ALTA</b>

            Uma frase persuasiva curta sobre o produto ou a oportunidade.

            🔥 <s>De R$ X,XX</s> por <b>R$ Y,YY</b> (Z% OFF)
            (se não houver preço antigo, mostre apenas: 🔥 <b>R$ Y,YY</b>)

            Adapte o entusiasmo à relevância: 1º lugar merece empolgação, 8º-10º merece tom factual.
            Não inclua o link — ele será adicionado automaticamente.
            Responda SOMENTE com a mensagem final, sem explicações adicionais.
            """;

    public enum Format { WHATSAPP, HTML }

    private final WebClient webClient;
    private final String model;
    private final boolean enabled;

    public GroqMessageService(
            WebClient.Builder builder,
            @Value("${groq.api.base-url:https://api.groq.com}") String baseUrl,
            @Value("${groq.api.api-key:}") String apiKey,
            @Value("${groq.api.model:llama-3.3-70b-versatile}") String model
    ) {
        this.model = model;
        this.enabled = !apiKey.isBlank();
        this.webClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public String generateRankingCaption(RankingItemResponse item) {
        return generateRankingCaption(item, Format.WHATSAPP);
    }

    public String generateRankingCaption(RankingItemResponse item, Format format) {
        if (!enabled) return null;
        return call(buildRankingPrompt(item), format);
    }

    public String generatePromotionCaption(PromotionAlert p) {
        return generatePromotionCaption(p, Format.WHATSAPP);
    }

    public String generatePromotionCaption(PromotionAlert p, Format format) {
        if (!enabled) return null;
        return call(buildPromotionPrompt(p), format);
    }

    public String generateOfertaCaption(ProductOfferResponse p, String badge) {
        return generateOfertaCaption(p, badge, Format.WHATSAPP);
    }

    public String generateOfertaCaption(ProductOfferResponse p, String badge, Format format) {
        if (!enabled) return null;
        return call(buildOfertaPrompt(p, badge), format);
    }

    private String call(String userPrompt, Format format) {
        String systemPrompt = format == Format.HTML ? SYSTEM_PROMPT_HTML : SYSTEM_PROMPT_WHATSAPP;
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "max_tokens", 400,
                    "temperature", 0.8
            );

            JsonNode response = webClient.post()
                    .uri("/openai/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(8))
                    .block();

            if (response != null && response.has("choices")) {
                String text = response.get("choices").get(0).get("message").get("content").asText().trim();
                return ensurePinEmoji(text, format);
            }
        } catch (Exception e) {
            log.warn("Groq indisponível, usando template: {}", e.getMessage());
        }
        return null;
    }

    private String ensurePinEmoji(String text, Format format) {
        if (text == null || text.startsWith("📌")) return text;
        String firstLine = text.lines().findFirst().orElse("");
        if (format == Format.HTML) {
            // strip leading <b> to re-wrap with 📌 inside
            String stripped = firstLine.replaceFirst("^<b>", "").replaceFirst("</b>$", "");
            return text.replaceFirst(java.util.regex.Pattern.quote(firstLine), "<b>📌 " + stripped + "</b>");
        } else {
            // strip leading * to re-wrap with 📌 inside
            String stripped = firstLine.replaceAll("^\\*", "").replaceAll("\\*$", "");
            return text.replaceFirst(java.util.regex.Pattern.quote(firstLine), "*📌 " + stripped + "*");
        }
    }

    private String buildRankingPrompt(RankingItemResponse item) {
        BigDecimal price = item.cashPrice() != null ? item.cashPrice() : item.price();
        String pixNote = item.cashPrice() != null ? " no Pix" : "";
        int pos = item.position();
        String tone = pos == 1 ? "Destaque com entusiasmo — é o melhor custo-benefício do ranking."
                : pos <= 3 ? "Tom positivo e direto — está no pódio."
                : pos <= 7 ? "Tom neutro e informativo — boa opção no ranking."
                : "Tom breve e factual — apenas registre a posição sem exagerar.";
        return "Escreva uma notificação de ranking de whey protein para Telegram.\n" +
                "Posição: " + pos + "º lugar (de 10)\n" +
                "Tom: " + tone + "\n" +
                "Produto: " + item.name() + "\n" +
                "Tipo de whey: " + wheyType(item.name()) + "\n" +
                "Loja: " + storeLabel(item.store()) + "\n" +
                (price != null ? "Preço: " + fmt(price) + pixNote + "\n" : "") +
                "Custo por grama de proteína: " + centavos(item.pricePerProteinGram()) + " centavos/g\n" +
                (item.weightGrams() != null ? "Gramatura: " + item.weightGrams() + "g\n" : "") +
                (item.proteinPerServingGrams() != null
                        ? "Proteína por dose: " + item.proteinPerServingGrams().stripTrailingZeros().toPlainString() + "g\n"
                        : "");
    }

    private String buildPromotionPrompt(PromotionAlert p) {
        BigDecimal disc = p.discountPercent().multiply(BigDecimal.valueOf(100));
        return "Gere uma mensagem de alerta: preço do produto caiu abaixo da média histórica.\n" +
                "Produto: " + p.name() + "\n" +
                "Tipo de whey: " + wheyType(p.name()) + "\n" +
                "Loja: " + storeLabel(p.store()) + "\n" +
                "Preço anterior (média 7 dias): " + fmt(p.averagePrice()) + "\n" +
                "Preço atual: " + fmt(p.currentPrice()) + "\n" +
                "Queda: " + String.format("%.1f%%", disc) + " abaixo da média\n" +
                "Custo de proteína: " + centavos(p.pricePerProteinGram()) + " centavos/g\n" +
                (p.weightGrams() != null ? "Gramatura: " + p.weightGrams() + "g\n" : "") +
                (p.proteinPerServingGrams() != null
                        ? "Proteína por dose: " + p.proteinPerServingGrams().stripTrailingZeros().toPlainString() + "g\n"
                        : "");
    }

    private String buildOfertaPrompt(ProductOfferResponse p, String badge) {
        BigDecimal price = p.cashPrice() != null ? p.cashPrice() : p.price();
        boolean isPix = p.cashPrice() != null && p.price() != null
                && p.cashPrice().compareTo(p.price()) < 0;
        String priceStr = p.oldPrice() != null && price != null
                ? "~" + fmt(p.oldPrice()) + "~ → " + fmt(price) + (isPix ? " (no pix)" : "")
                : (price != null ? fmt(price) + (isPix ? " (no pix)" : "") : "não informado");
        return "Gere uma mensagem de oferta para suplemento. Tipo: " + badge + "\n" +
                "Produto: " + p.name() + "\n" +
                "Tipo de whey: " + wheyType(p.name()) + "\n" +
                "Loja: " + storeLabel(p.store()) + "\n" +
                "Preço: " + priceStr + "\n" +
                (p.weightGrams() != null ? "Gramatura: " + p.weightGrams() + "g\n" : "");
    }

    private String wheyType(String name) {
        if (name == null) return "Whey Protein";
        String n = name.toLowerCase();
        boolean hasHydro    = n.contains("hidrolisado") || n.contains("hydrolyze") || n.contains("hydro") || n.contains("wph");
        boolean hasIsolate  = n.contains("isolado") || n.contains("isolate") || n.contains("wpi");
        boolean hasConc     = n.contains("concentrado") || n.contains("concentrate") || n.contains("wpc");
        boolean hasBlend    = n.contains("blend") || n.contains("matrix") || n.contains("complex")
                           || n.contains("mix") || n.contains("3w") || n.contains("tri");
        if (hasBlend || (hasHydro && hasIsolate) || (hasHydro && hasConc) || (hasIsolate && hasConc))
            return "Blend (mix de concentrado, isolado e/ou hidrolisado)";
        if (hasHydro)   return "Hidrolisado — absorção mais rápida, ideal pós-treino";
        if (hasIsolate) return "Isolado — baixo carboidrato e gordura, alto teor proteico";
        if (hasConc)    return "Concentrado — proteína completa com custo acessível";
        return "Concentrado — proteína completa com custo acessível";
    }

    private String positionLabel(int pos) {
        if (pos == 1) return "🥇 1º lugar";
        if (pos == 2) return "🥈 2º lugar";
        if (pos == 3) return "🥉 3º lugar";
        return pos + "º lugar";
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

    private String fmt(BigDecimal value) {
        return CURRENCY_FORMAT.format(value);
    }

    private String centavos(BigDecimal pricePerG) {
        return ONE_DECIMAL.format(pricePerG.multiply(BigDecimal.valueOf(100)));
    }
}
