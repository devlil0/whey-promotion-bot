package com.devlil0.whey_promotion_bot.service;

import com.devlil0.whey_promotion_bot.entity.NutritionInfo;
import com.devlil0.whey_promotion_bot.entity.ProductOffer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class NutritionMatcher {

    // Tenta encontrar qual NutritionInfo corresponde a uma oferta de produto.
    // Retorna null se não encontrar nenhuma correspondência.
    public NutritionInfo match(ProductOffer offer, List<NutritionInfo> nutritionList) {
        if (offer == null || offer.getName() == null || offer.getStore() == null) {
            return null;
        }

        String offerNameNorm = ProductFilter.normalize(offer.getName());

        // 1ª tentativa: filtra só os registros da mesma loja
        List<NutritionInfo> storeMatches = new ArrayList<>();
        for (NutritionInfo n : nutritionList) {
            if (n.getStore() != null && n.getStore().equalsIgnoreCase(offer.getStore())) {
                storeMatches.add(n);
            }
        }

        // 2ª tentativa: nome exato
        for (NutritionInfo n : storeMatches) {
            if (offerNameNorm.equals(n.getProductNameNormalized())) {
                return n;
            }
        }

        // 3ª tentativa: mesma marca + gramatura próxima
        NutritionInfo byBrandAndWeight = findByBrandAndWeight(offer, storeMatches);
        if (byBrandAndWeight != null) {
            return byBrandAndWeight;
        }

        // 4ª tentativa: gramatura próxima + maior número de palavras em comum no nome
        List<NutritionInfo> closestWeightMatches = filterByClosestWeight(offer.getWeightGrams(), storeMatches);
        if (closestWeightMatches.isEmpty()) {
            return null;
        }

        NutritionInfo best = null;
        int bestScore = 0;
        for (NutritionInfo n : closestWeightMatches) {
            int score = countCommonWords(offerNameNorm, n.getProductNameNormalized());
            if (score > bestScore) {
                bestScore = score;
                best = n;
            }
        }

        // Exige pelo menos 2 palavras em comum para considerar um match válido
        if (bestScore >= 2) {
            return best;
        }
        return null;
    }

    private NutritionInfo findByBrandAndWeight(ProductOffer offer, List<NutritionInfo> nutritionList) {
        if (offer.getBrand() == null || offer.getWeightGrams() == null) {
            return null;
        }

        String offerBrandNorm = ProductFilter.normalize(offer.getBrand());
        String offerNameNorm = ProductFilter.normalize(offer.getName());

        NutritionInfo best = null;
        int bestScore = 0;

        for (NutritionInfo n : nutritionList) {
            if (n.getBrand() == null || n.getWeightGrams() == null) continue;

            boolean sameBrand = ProductFilter.normalize(n.getBrand()).equals(offerBrandNorm);
            boolean similarWeight = Math.abs(n.getWeightGrams() - offer.getWeightGrams()) <= 100;

            if (sameBrand && similarWeight) {
                int score = countCommonWords(offerNameNorm, n.getProductNameNormalized());
                if (score > bestScore) {
                    bestScore = score;
                    best = n;
                }
            }
        }

        return best;
    }

    private List<NutritionInfo> filterByClosestWeight(Integer offerWeight, List<NutritionInfo> nutritionList) {
        List<NutritionInfo> result = new ArrayList<>();
        if (offerWeight == null || offerWeight <= 0) {
            return result;
        }

        for (NutritionInfo n : nutritionList) {
            if (n.getWeightGrams() != null && Math.abs(n.getWeightGrams() - offerWeight) <= 100) {
                result.add(n);
            }
        }

        return result;
    }

    private int countCommonWords(String a, String b) {
        String[] wordsA = a.split("\\s+");
        String[] wordsB = b.split("\\s+");

        Set<String> setB = new HashSet<>();
        for (String word : wordsB) {
            setB.add(word);
        }

        int count = 0;
        for (String word : wordsA) {
            if (setB.contains(word)) {
                count++;
            }
        }
        return count;
    }
}
