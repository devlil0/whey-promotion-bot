package com.devlil0.whey_promotion_bot.service;

import com.devlil0.whey_promotion_bot.entity.NutritionInfo;
import com.devlil0.whey_promotion_bot.entity.ProductOffer;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NutritionMatcher {

    public Optional<NutritionInfo> match(ProductOffer offer, List<NutritionInfo> nutritionList) {
        if (offer == null || offer.getName() == null || offer.getStore() == null) {
            return Optional.empty();
        }

        String offerNameNorm = ProductFilter.normalize(offer.getName());

        List<NutritionInfo> storeMatches = nutritionList.stream()
                .filter(n -> n.getStore() != null && n.getStore().equalsIgnoreCase(offer.getStore()))
                .collect(Collectors.toList());

        Optional<NutritionInfo> exactNameMatch = storeMatches.stream()
                .filter(n -> offerNameNorm.equals(n.getProductNameNormalized()))
                .findFirst();
        if (exactNameMatch.isPresent()) {
            return exactNameMatch;
        }

        Optional<NutritionInfo> brandAndWeightMatch = findByBrandAndWeight(offer, storeMatches);
        if (brandAndWeightMatch.isPresent()) {
            return brandAndWeightMatch;
        }

        List<NutritionInfo> closestWeightMatches = filterByClosestWeight(offer.getWeightGrams(), storeMatches);
        if (closestWeightMatches.isEmpty()) {
            return Optional.empty();
        }

        return closestWeightMatches.stream()
                .max(Comparator.comparingInt(
                        n -> countCommonWords(offerNameNorm, n.getProductNameNormalized())))
                .filter(n -> countCommonWords(offerNameNorm, n.getProductNameNormalized()) >= 2);
    }

    private Optional<NutritionInfo> findByBrandAndWeight(ProductOffer offer, List<NutritionInfo> nutritionList) {
        if (offer.getBrand() == null || offer.getWeightGrams() == null) {
            return Optional.empty();
        }

        String offerBrandNorm = ProductFilter.normalize(offer.getBrand());

        return nutritionList.stream()
                .filter(n -> n.getBrand() != null
                        && ProductFilter.normalize(n.getBrand()).equals(offerBrandNorm))
                .filter(n -> n.getWeightGrams() != null
                        && Math.abs(n.getWeightGrams() - offer.getWeightGrams()) <= 100)
                .max(Comparator.comparingInt(
                        n -> countCommonWords(ProductFilter.normalize(offer.getName()), n.getProductNameNormalized())));
    }

    private List<NutritionInfo> filterByClosestWeight(Integer offerWeight, List<NutritionInfo> nutritionList) {
        if (offerWeight == null || offerWeight <= 0) {
            return List.of();
        }

        return nutritionList.stream()
                .filter(n -> n.getWeightGrams() != null && Math.abs(n.getWeightGrams() - offerWeight) <= 100)
                .collect(Collectors.toList());
    }

    private int countCommonWords(String a, String b) {
        Set<String> wordsA = new HashSet<>(Arrays.asList(a.split("\\s+")));
        Set<String> wordsB = Set.of(b.split("\\s+"));
        return (int) wordsA.stream().filter(wordsB::contains).count();
    }
}
