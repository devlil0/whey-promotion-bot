package com.devlil0.whey_promotion_bot.controller;

import com.devlil0.whey_promotion_bot.dto.ApiStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ApiStatusResponse health() {
        return new ApiStatusResponse(
                "UP",
                LocalDateTime.now(),
                Map.of(
                        "growthProteinRaw", "GET /api/growth/category/raw?category=/proteina/&offset=0&limit=30",
                        "growthWheyRaw", "GET /api/growth/category/raw?category=/whey-protein/&offset=0&limit=30",
                        "growthWheyOffers", "GET /api/growth/offers?category=/whey-protein/",
                        "darkLabRaw", "GET /api/darklab/products/raw?page=1&limit=250",
                        "profitLabsRaw", "GET /api/profitlabs/products/raw?page=1&limit=50",
                        "allOffers", "GET /api/offers/whey"
                )
        );
    }
}
