package com.devlil0.whey_promotion_bot.service;

import com.devlil0.whey_promotion_bot.dto.RankingItemResponse;
import com.devlil0.whey_promotion_bot.entity.NutritionInfo;
import com.devlil0.whey_promotion_bot.entity.ProductOffer;
import com.devlil0.whey_promotion_bot.entity.ProductScore;
import com.devlil0.whey_promotion_bot.repository.NutritionInfoRepository;
import com.devlil0.whey_promotion_bot.repository.ProductOfferRepository;
import com.devlil0.whey_promotion_bot.repository.ProductScoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RankingService {

    private static final Logger log = LoggerFactory.getLogger(RankingService.class);

    private final ProductOfferRepository offerRepository;
    private final NutritionInfoRepository nutritionRepository;
    private final ProductScoreRepository scoreRepository;
    private final NutritionMatcher nutritionMatcher;

    public RankingService(ProductOfferRepository offerRepository,
                          NutritionInfoRepository nutritionRepository,
                          ProductScoreRepository scoreRepository,
                          NutritionMatcher nutritionMatcher) {
        this.offerRepository = offerRepository;
        this.nutritionRepository = nutritionRepository;
        this.scoreRepository = scoreRepository;
        this.nutritionMatcher = nutritionMatcher;
    }

    @Transactional
    public List<RankingItemResponse> refreshRanking() {
        List<ProductOffer> offers = offerRepository.findByAvailableTrue();
        List<NutritionInfo> allNutrition = nutritionRepository.findAll();
        List<ProductScore> scores = calculateScores(offers, allNutrition);

        scoreRepository.deleteAllInBatch();
        scoreRepository.saveAll(scores);

        return scores.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RankingItemResponse> getRanking(int top, String storeFilter) {
        int limit = top > 0 ? top : 10;
        List<ProductScore> scores = storeFilter != null && !storeFilter.isBlank()
                ? scoreRepository.findByStoreIgnoreCaseOrderByRankPositionAsc(storeFilter)
                : scoreRepository.findAllByOrderByRankPositionAsc();

        return scores.stream()
                .limit(limit)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private List<ProductScore> calculateScores(List<ProductOffer> offers, List<NutritionInfo> allNutrition) {
        LocalDateTime now = LocalDateTime.now();
        List<ProductScore> scores = new ArrayList<>();
        List<ProductOffer> unmatched = new ArrayList<>();

        for (ProductOffer offer : offers) {
            BigDecimal effectivePrice = offer.getCashPrice() != null ? offer.getCashPrice() : offer.getPrice();
            if (effectivePrice == null || effectivePrice.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Optional<NutritionInfo> nutrition = nutritionMatcher.match(offer, allNutrition);
            if (nutrition.isEmpty()) {
                unmatched.add(offer);
                continue;
            }

            NutritionInfo info = nutrition.get();
            if (info.getTotalProteinGrams() == null
                    || info.getTotalProteinGrams().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal costPerProteinGram = effectivePrice.divide(
                    info.getTotalProteinGrams(), 4, RoundingMode.HALF_UP);

            ProductScore score = new ProductScore();
            score.setProductOffer(offer);
            score.setNutritionInfo(info);
            score.setStore(offer.getStore());
            score.setCostPerProteinGram(costPerProteinGram);
            score.setScoredAt(now);
            scores.add(score);
        }

        scores.sort(Comparator.comparing(ProductScore::getCostPerProteinGram));
        for (int i = 0; i < scores.size(); i++) {
            scores.get(i).setRankPosition(i + 1);
        }

        if (!unmatched.isEmpty()) {
            List<String> sample = unmatched.stream()
                    .limit(5)
                    .map(o -> o.getStore() + ":" + o.getName())
                    .collect(Collectors.toList());
            log.warn("{} ofertas sem nutrition match — sample: {}", unmatched.size(), sample);
        }

        return scores;
    }

    private RankingItemResponse toResponse(ProductScore score) {
        ProductOffer offer = score.getProductOffer();
        NutritionInfo info = score.getNutritionInfo();

        return new RankingItemResponse(
                score.getRankPosition(),
                offer.getStore(),
                offer.getName(),
                offer.getBrand(),
                offer.getWeightGrams(),
                offer.getPrice(),
                offer.getCashPrice(),
                info.getProteinPerServingGrams(),
                info.getTotalProteinGrams(),
                score.getCostPerProteinGram(),
                offer.getProductUrl(),
                offer.getImageUrl()
        );
    }
}
