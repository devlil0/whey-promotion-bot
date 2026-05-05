package com.devlil0.whey_promotion_bot.dto;

import java.math.BigDecimal;
import java.util.List;

public record OfertasFaixaResponse(
        String faixa,
        BigDecimal minimo,
        BigDecimal maximo,
        int quantidade,
        List<ProductOfferResponse> produtos
) {}
