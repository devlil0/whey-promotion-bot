package com.devlil0.whey_promotion_bot.controller;

import com.devlil0.whey_promotion_bot.dto.MlProductRanking;
import com.devlil0.whey_promotion_bot.dto.ProductOfferResponse;
import com.devlil0.whey_promotion_bot.dto.PromotionAlert;
import com.devlil0.whey_promotion_bot.dto.RankingItemResponse;
import com.devlil0.whey_promotion_bot.service.MlCostBenefitService;
import com.devlil0.whey_promotion_bot.service.OfferPersistenceService;
import com.devlil0.whey_promotion_bot.service.PromotionService;
import com.devlil0.whey_promotion_bot.service.RankingService;
import com.devlil0.whey_promotion_bot.service.StoreCollectorService;
import com.devlil0.whey_promotion_bot.service.TelegramNotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/telegram")
public class TelegramTriggerController {

    private final TelegramNotificationService telegramService;
    private final RankingService rankingService;
    private final PromotionService promotionService;
    private final MlCostBenefitService mlCostBenefitService;
    private final StoreCollectorService collectorService;
    private final OfferPersistenceService persistenceService;

    public TelegramTriggerController(TelegramNotificationService telegramService,
                                     RankingService rankingService,
                                     PromotionService promotionService,
                                     MlCostBenefitService mlCostBenefitService,
                                     StoreCollectorService collectorService,
                                     OfferPersistenceService persistenceService) {
        this.telegramService = telegramService;
        this.rankingService = rankingService;
        this.promotionService = promotionService;
        this.mlCostBenefitService = mlCostBenefitService;
        this.collectorService = collectorService;
        this.persistenceService = persistenceService;
    }

    @PostMapping("/trigger/collect")
    public Map<String, Object> triggerCollect() {
        List<ProductOfferResponse> offers = collectorService.collectAllWheyOffers();
        int saved = persistenceService.upsertAll(offers);
        List<RankingItemResponse> ranking = rankingService.refreshRanking();
        return Map.of("saved", saved, "rankingSize", ranking.size());
    }

    @PostMapping("/trigger/ranking")
    public Map<String, Object> triggerRanking(
            @RequestParam(defaultValue = "10") int top
    ) {
        List<RankingItemResponse> ranking = rankingService.getRanking(top, null);
        telegramService.sendRanking(ranking);
        return Map.of(
                "sent", true,
                "itemCount", ranking.size()
        );
    }

    @PostMapping("/trigger/promotions")
    public Map<String, Object> triggerPromotions() {
        List<PromotionAlert> promotions = promotionService.detectPromotions(LocalDateTime.now());
        telegramService.sendPromotions(promotions);
        return Map.of(
                "sent", !promotions.isEmpty(),
                "promotionCount", promotions.size()
        );
    }

    @GetMapping("/trigger/ml-top3/debug")
    public Object debugMlTop3() {
        return mlCostBenefitService.diagnose();
    }

    @PostMapping("/trigger/ml-top3")
    public Map<String, Object> triggerMlTop3() {
        List<MlProductRanking> top3 = mlCostBenefitService.getTop3();
        telegramService.sendMlTop3(top3);
        return Map.of(
                "sent", !top3.isEmpty(),
                "products", top3
        );
    }
}
