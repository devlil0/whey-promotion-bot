package com.devlil0.whey_promotion_bot.controller;

import com.devlil0.whey_promotion_bot.dto.RankingItemResponse;
import com.devlil0.whey_promotion_bot.service.RankingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/api/rankings/whey/top-cost-benefit")
    public List<RankingItemResponse> getRanking(
            @RequestParam(defaultValue = "10") int top,
            @RequestParam(required = false) String store
    ) {
        return rankingService.getRanking(top, store);
    }
}
