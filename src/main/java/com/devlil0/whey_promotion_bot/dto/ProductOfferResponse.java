package com.devlil0.whey_promotion_bot.dto;

import java.math.BigDecimal;

public record ProductOfferResponse(
        String store,
        String externalId,
        String variantId,
        String sku,
        String name,
        String brand,
        String category,
        BigDecimal price,
        BigDecimal cashPrice,
        BigDecimal oldPrice,
        Boolean available,
        Integer stock,
        Integer weightGrams,
        String productUrl,
        String imageUrl,
        String sourceType
) {}
