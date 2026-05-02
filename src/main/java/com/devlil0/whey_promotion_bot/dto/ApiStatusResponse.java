package com.devlil0.whey_promotion_bot.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiStatusResponse(
        String status,
        LocalDateTime timestamp,
        Map<String, String> endpoints
) {}
