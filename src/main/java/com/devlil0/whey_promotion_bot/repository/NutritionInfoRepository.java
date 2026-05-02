package com.devlil0.whey_promotion_bot.repository;

import com.devlil0.whey_promotion_bot.entity.NutritionInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NutritionInfoRepository extends JpaRepository<NutritionInfo, Long> {

    List<NutritionInfo> findByStore(String store);

    Optional<NutritionInfo> findByProductNameNormalized(String productNameNormalized);

    List<NutritionInfo> findByBrandAndWeightGrams(String brand, Integer weightGrams);
}
