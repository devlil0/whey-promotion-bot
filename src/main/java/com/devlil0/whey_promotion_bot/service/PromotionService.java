package com.devlil0.whey_promotion_bot.service;

import com.devlil0.whey_promotion_bot.dto.PromotionAlert;
import com.devlil0.whey_promotion_bot.entity.ProductOffer;
import com.devlil0.whey_promotion_bot.entity.ProductScore;
import com.devlil0.whey_promotion_bot.repository.PriceHistoryRepository;
import com.devlil0.whey_promotion_bot.repository.ProductScoreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PromotionService {

    private final ProductScoreRepository scoreRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final BigDecimal discountThreshold;
    private final int historyDays;
    private final long minSamples;

    public PromotionService(
            ProductScoreRepository scoreRepository,
            PriceHistoryRepository priceHistoryRepository,
            @Value("${promotion.discount-threshold:0.15}") BigDecimal discountThreshold,
            @Value("${promotion.history-days:7}") int historyDays,
            @Value("${promotion.min-history-samples:3}") long minSamples
    ) {
        this.scoreRepository = scoreRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.discountThreshold = discountThreshold;
        this.historyDays = historyDays;
        this.minSamples = minSamples;
    }

    @Transactional(readOnly = true)
    public List<PromotionAlert> detectPromotions(LocalDateTime now) {
        LocalDateTime windowStart = now.minusDays(historyDays);
        List<ProductScore> scores = scoreRepository.findAllByOrderByRankPositionAsc();
        List<PromotionAlert> alerts = new ArrayList<>();

        for (ProductScore score : scores) {
            ProductOffer offer = score.getProductOffer();
            BigDecimal currentPrice = offer.getCashPrice() != null ? offer.getCashPrice() : offer.getPrice();
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) continue;

            long samples = priceHistoryRepository.countBetween(offer.getId(), windowStart, now);
            if (samples < minSamples) continue;

            BigDecimal avg = priceHistoryRepository.averagePriceBetween(offer.getId(), windowStart, now);
            if (avg == null || avg.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal threshold = avg.multiply(BigDecimal.ONE.subtract(discountThreshold));
            if (currentPrice.compareTo(threshold) > 0) continue;

            BigDecimal discount = avg.subtract(currentPrice)
                    .divide(avg, 4, RoundingMode.HALF_UP);

            alerts.add(new PromotionAlert(
                    offer.getStore(),
                    offer.getName(),
                    offer.getBrand(),
                    currentPrice,
                    avg.setScale(2, RoundingMode.HALF_UP),
                    discount,
                    score.getCostPerProteinGram(),
                    offer.getProductUrl(),
                    offer.getImageUrl()
            ));
        }

        alerts.sort(Comparator.comparing(PromotionAlert::discountPercent).reversed());
        return alerts;
    }
}
