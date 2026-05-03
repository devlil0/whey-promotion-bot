package com.devlil0.whey_promotion_bot.service;

import com.devlil0.whey_promotion_bot.client.MercadoLivreClient;
import com.devlil0.whey_promotion_bot.dto.MlProductRanking;
import com.devlil0.whey_promotion_bot.entity.ProductOffer;
import com.devlil0.whey_promotion_bot.repository.ProductOfferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MlCostBenefitService {

    private static final Logger log = LoggerFactory.getLogger(MlCostBenefitService.class);

    private static final BigDecimal MIN_PRICE = new BigDecimal("50");

    private static final Pattern PROTEIN_PER_SERVING = Pattern.compile(
            "(?i)prote[íi]nas?\\s*[:\\-]?\\s*(\\d+(?:[,.]\\d+)?)\\s*g");
    private static final Pattern SERVINGS_INLINE = Pattern.compile(
            "(?i)(\\d+)\\s*doses?");
    private static final Pattern SERVINGS_LABEL = Pattern.compile(
            "(?i)rendimento[^\\d]*(\\d+)");
    private static final Pattern SERVINGS_PORCOES = Pattern.compile(
            "(?i)(\\d+)\\s*por[çc][oõ]es?");
    private static final Pattern TOTAL_PROTEIN = Pattern.compile(
            "(?i)(?:prote[íi]nas?\\s+total|total\\s+(?:de\\s+)?prote[íi]nas?)[^\\d]*(\\d+(?:[,.]\\d+)?)\\s*g");

    private final MercadoLivreClient mlClient;
    private final ProductOfferRepository offerRepository;

    public MlCostBenefitService(MercadoLivreClient mlClient, ProductOfferRepository offerRepository) {
        this.mlClient = mlClient;
        this.offerRepository = offerRepository;
    }

    public List<MlProductRanking> getTop3() {
        List<ProductOffer> mlOffers = offerRepository.findByStore("MERCADO_LIVRE");
        if (mlOffers.isEmpty()) {
            log.warn("Nenhum produto ML no banco — coleta ainda não foi executada?");
            return List.of();
        }

        List<MlProductRanking> ranked = new ArrayList<>();
        for (ProductOffer offer : mlOffers) {
            try {
                MlProductRanking r = processOffer(offer);
                if (r != null) ranked.add(r);
            } catch (Exception e) {
                log.warn("Falha ao processar ML externalId={}: {}", offer.getExternalId(), e.getMessage());
            }
        }

        log.info("ML Top 3: {}/{} produtos com dados de proteína suficientes", ranked.size(), mlOffers.size());

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

    public List<Map<String, Object>> diagnose() {
        List<ProductOffer> mlOffers = offerRepository.findByStore("MERCADO_LIVRE");
        if (mlOffers.isEmpty()) {
            return List.of(Map.of("error", "Nenhum produto ML no banco"));
        }

        List<Map<String, Object>> report = new ArrayList<>();
        for (ProductOffer offer : mlOffers.stream().limit(5).toList()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("externalId", offer.getExternalId());
            entry.put("name", offer.getName());
            entry.put("price", offer.getPrice());
            entry.put("weightGrams", offer.getWeightGrams());

            String description = mlClient.getItemDescription(offer.getExternalId());
            entry.put("descriptionNull", description == null);
            if (description != null) {
                entry.put("descriptionSnippet", description.length() > 600 ? description.substring(0, 600) : description);
                entry.put("proteinPerServingFound", parseProteinPerServing(description) != null ? parseProteinPerServing(description) : "not found");
                entry.put("servingsFound", parseServings(description) != null ? parseServings(description) : "not found");
                entry.put("totalProteinFound", parseTotalProtein(description) != null ? parseTotalProtein(description) : "not found");
            }
            report.add(entry);
        }
        return report;
    }

    private MlProductRanking processOffer(ProductOffer offer) {
        if (offer.getExternalId() == null) return null;
        if (offer.getPrice() == null || offer.getPrice().compareTo(MIN_PRICE) < 0) return null;

        String description = mlClient.getItemDescription(offer.getExternalId());
        if (description == null || description.isBlank()) return null;

        BigDecimal totalProtein = parseTotalProtein(description);
        if (totalProtein == null) {
            BigDecimal perServing = parseProteinPerServing(description);
            Integer servings = parseServings(description);
            if (perServing == null || servings == null || servings <= 0) return null;
            totalProtein = perServing.multiply(BigDecimal.valueOf(servings));
        }

        if (totalProtein.compareTo(BigDecimal.ZERO) <= 0) return null;

        BigDecimal costPerProteinGram = offer.getPrice().divide(totalProtein, 4, RoundingMode.HALF_UP);

        return new MlProductRanking(
                0,
                offer.getName(),
                offer.getBrand() != null ? offer.getBrand() : "Mercado Livre",
                offer.getPrice(),
                offer.getWeightGrams(),
                totalProtein,
                costPerProteinGram,
                offer.getProductUrl(),
                offer.getImageUrl()
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
}
