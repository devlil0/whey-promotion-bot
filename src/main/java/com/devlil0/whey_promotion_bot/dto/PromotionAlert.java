package com.devlil0.whey_promotion_bot.dto;

import java.math.BigDecimal;

public record PromotionAlert(
        String store,
        String name,
        String brand,
        Integer weightGrams,
        BigDecimal proteinPerServingGrams,
        BigDecimal currentPrice,
        BigDecimal averagePrice,
        BigDecimal discountPercent,
        BigDecimal pricePerProteinGram,
        String productUrl,
        String imageUrl
) {}
