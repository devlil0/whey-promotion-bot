package com.devlil0.whey_promotion_bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.devlil0.whey_promotion_bot.client.MercadoLivreClient;
import com.devlil0.whey_promotion_bot.dto.MlProductRanking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MlCostBenefitService {

    private static final Logger log = LoggerFactory.getLogger(MlCostBenefitService.class);

    private static final BigDecimal MIN_PRICE = new BigDecimal("50");

    // "Proteínas: 24g", "Proteína: 24 g", "Proteína - 24g"
    private static final Pattern PROTEIN_PER_SERVING = Pattern.compile(
            "(?i)prote[íi]nas?\\s*[:\\-]?\\s*(\\d+(?:[,.]\\d+)?)\\s*g");

    // "30 doses", "Rendimento: 30", "30 porções"
    private static final Pattern SERVINGS_INLINE = Pattern.compile(
            "(?i)(\\d+)\\s*doses?");
    private static final Pattern SERVINGS_LABEL = Pattern.compile(
            "(?i)rendimento[^\\d]*(\\d+)");
    private static final Pattern SERVINGS_PORCOES = Pattern.compile(
            "(?i)(\\d+)\\s*por[çc][oõ]es?");

    // "Proteína Total: 720g", "Total de Proteínas: 720g"
    private static final Pattern TOTAL_PROTEIN = Pattern.compile(
            "(?i)(?:prote[íi]nas?\\s+total|total\\s+(?:de\\s+)?prote[íi]nas?)[^\\d]*(\\d+(?:[,.]\\d+)?)\\s*g");

    // NET_WEIGHT attribute parsing: "900 g", "1,8 kg"
    private static final Pattern WEIGHT_PATTERN = Pattern.compile(
            "(\\d+(?:[,.]\\d+)?)\\s*(kg|g)", Pattern.CASE_INSENSITIVE);

    private final MercadoLivreClient mlClient;

    public MlCostBenefitService(MercadoLivreClient mlClient) {
        this.mlClient = mlClient;
    }

    public List<MlProductRanking> getTop3() {
        JsonNode response;
        try {
            response = mlClient.searchWhey(20);
        } catch (Exception e) {
            log.error("Falha ao buscar produtos ML para Top 3: {}", e.getMessage());
            return List.of();
        }

        JsonNode results = response.path("results");
        if (!results.isArray()) return List.of();

        List<MlProductRanking> ranked = new ArrayList<>();
        for (JsonNode item : results) {
            try {
                MlProductRanking r = processItem(item);
                if (r != null) ranked.add(r);
            } catch (Exception e) {
                log.warn("Falha ao processar item ML {}: {}", item.path("id").asText("?"), e.getMessage());
            }
        }

        List<MlProductRanking> top3 = ranked.stream()
                .sorted(Comparator.comparing(MlProductRanking::costPerProteinGram))
                .limit(3)
                .toList();

        List<MlProductRanking> result = new ArrayList<>();
        for (int i = 0; i < top3.size(); i++) {
            MlProductRanking r = top3.get(i);
            result.add(new MlProductRanking(
                    i + 1,
                    r.name(), r.brand(), r.price(),
                    r.weightGrams(), r.totalProteinGrams(),
                    r.costPerProteinGram(), r.productUrl(), r.imageUrl()
            ));
        }
        return result;
    }

    private MlProductRanking processItem(JsonNode item) {
        if (!"new".equals(item.path("condition").asText())) return null;

        String title = item.path("title").asText(null);
        if (title == null || !ProductFilter.isWheyMainRankingCandidate(title)) return null;

        BigDecimal price = item.path("price").isNumber() ? item.path("price").decimalValue() : null;
        if (price == null || price.compareTo(MIN_PRICE) < 0) return null;

        String itemId = item.path("id").asText(null);
        if (itemId == null) return null;

        String brand = mlAttribute(item, "BRAND");
        String imageUrl = item.path("thumbnail").asText(null);
        String productUrl = item.path("permalink").asText(null);

        Integer weightGrams = parseWeightGrams(mlAttribute(item, "NET_WEIGHT"));
        if (weightGrams == null) weightGrams = extractWeightFromTitle(title);

        String description = mlClient.getItemDescription(itemId);
        if (description == null || description.isBlank()) return null;

        BigDecimal totalProtein = parseTotalProtein(description);
        if (totalProtein == null) {
            BigDecimal perServing = parseProteinPerServing(description);
            Integer servings = parseServings(description);
            if (perServing == null || servings == null || servings <= 0) return null;
            totalProtein = perServing.multiply(BigDecimal.valueOf(servings));
        }

        if (totalProtein.compareTo(BigDecimal.ZERO) <= 0) return null;

        BigDecimal costPerProteinGram = price.divide(totalProtein, 4, RoundingMode.HALF_UP);

        return new MlProductRanking(
                0, title,
                brand != null ? brand : "Mercado Livre",
                price, weightGrams, totalProtein,
                costPerProteinGram,
                productUrl, imageUrl
        );
    }

    private BigDecimal parseTotalProtein(String text) {
        Matcher m = TOTAL_PROTEIN.matcher(text);
        return m.find() ? new BigDecimal(m.group(1).replace(",", ".")) : null;
    }

    private BigDecimal parseProteinPerServing(String text) {
        Matcher m = PROTEIN_PER_SERVING.matcher(text);
        return m.find() ? new BigDecimal(m.group(1).replace(",", ".")) : null;
    }

    private Integer parseServings(String text) {
        for (Pattern p : List.of(SERVINGS_LABEL, SERVINGS_INLINE, SERVINGS_PORCOES)) {
            Matcher m = p.matcher(text);
            if (m.find()) {
                int val = Integer.parseInt(m.group(1));
                if (val > 1) return val;
            }
        }
        return null;
    }

    private Integer parseWeightGrams(String weightStr) {
        if (weightStr == null) return null;
        String s = weightStr.toLowerCase().replace(",", ".");
        Matcher m = WEIGHT_PATTERN.matcher(s);
        if (!m.find()) return null;
        double value = Double.parseDouble(m.group(1));
        return m.group(2).equalsIgnoreCase("kg") ? (int) Math.round(value * 1000) : (int) Math.round(value);
    }

    private Integer extractWeightFromTitle(String title) {
        if (title == null) return null;
        String normalized = title.toLowerCase().replace(",", ".");
        Matcher m = WEIGHT_PATTERN.matcher(normalized);
        if (!m.find()) return null;
        double value = Double.parseDouble(m.group(1));
        return m.group(2).equalsIgnoreCase("kg") ? (int) Math.round(value * 1000) : (int) Math.round(value);
    }

    private String mlAttribute(JsonNode item, String attributeId) {
        JsonNode attributes = item.path("attributes");
        if (!attributes.isArray()) return null;
        for (JsonNode attr : attributes) {
            if (attributeId.equals(attr.path("id").asText())) {
                String val = attr.path("value_name").asText(null);
                return (val == null || val.isBlank()) ? null : val;
            }
        }
        return null;
    }
}
