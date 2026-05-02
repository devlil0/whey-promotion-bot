package com.devlil0.whey_promotion_bot.controller;

import com.devlil0.whey_promotion_bot.dto.PromotionAlert;
import com.devlil0.whey_promotion_bot.dto.RankingItemResponse;
import com.devlil0.whey_promotion_bot.service.PromotionService;
import com.devlil0.whey_promotion_bot.service.RankingService;
import com.devlil0.whey_promotion_bot.service.TelegramNotificationService;
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

    public TelegramTriggerController(TelegramNotificationService telegramService,
                                     RankingService rankingService,
                                     PromotionService promotionService) {
        this.telegramService = telegramService;
        this.rankingService = rankingService;
        this.promotionService = promotionService;
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
}
