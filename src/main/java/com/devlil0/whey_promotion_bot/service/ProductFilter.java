package com.devlil0.whey_promotion_bot.service;

import java.text.Normalizer;

public final class ProductFilter {

    private ProductFilter() {}

    public static boolean isProteinOrWhey(String name) {
        String n = normalize(name);
        return n.contains("whey")
                || n.contains("protein")
                || n.contains("proteina")
                || n.contains("isolado")
                || n.contains("isolate")
                || n.contains("hidrolisado")
                || n.contains("concentrado")
                || n.contains("albumina")
                || n.contains("caseina");
    }

    public static boolean isWheyMainRankingCandidate(String name) {
        String n = normalize(name);
        boolean looksLikeWhey = n.contains("whey")
                || n.contains("isolate protein")
                || n.contains("iso whey")
                || n.contains("isolado")
                || n.contains("hidrolisado")
                || n.contains("concentrado");

        boolean excluded = n.contains("kit")
                || n.contains("combo")
                || n.contains("sache")
                || n.contains("sache")
                || n.contains("barra")
                || n.contains("display")
                || n.contains("camiseta")
                || n.contains("creatine")
                || n.contains("creatina")
                || n.contains("bcaa");

        return looksLikeWhey && !excluded;
    }

    public static String normalize(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase().trim();
    }
}
