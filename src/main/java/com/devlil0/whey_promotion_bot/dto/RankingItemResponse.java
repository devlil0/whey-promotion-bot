package com.devlil0.whey_promotion_bot.dto;

import java.math.BigDecimal;

public record RankingItemResponse(
        int position,
        String store,
        String name,
        String brand,
        Integer weightGrams,
        BigDecimal price,
        BigDecimal cashPrice,
        BigDecimal proteinPerServingGrams,
        BigDecimal totalProteinGrams,
        BigDecimal pricePerProteinGram,
        String productUrl,
        String imageUrl
) {}
