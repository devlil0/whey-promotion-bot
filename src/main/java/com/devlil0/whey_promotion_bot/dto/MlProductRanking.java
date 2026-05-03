package com.devlil0.whey_promotion_bot.dto;

import java.math.BigDecimal;

public record MlProductRanking(
        int position,
        String name,
        String brand,
        BigDecimal price,
        Integer weightGrams,
        BigDecimal totalProteinGrams,
        BigDecimal costPerProteinGram,
        String productUrl,
        String imageUrl
) {}
