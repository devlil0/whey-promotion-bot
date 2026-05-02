package com.devlil0.whey_promotion_bot.dto;

import java.math.BigDecimal;

public record PromotionAlert(
        String store,
        String name,
        String brand,
        BigDecimal currentPrice,
        BigDecimal averagePrice,
        BigDecimal discountPercent,
        BigDecimal pricePerProteinGram,
        String productUrl
) {}
