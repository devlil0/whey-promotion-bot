package com.devlil0.whey_promotion_bot.repository;

import com.devlil0.whey_promotion_bot.entity.ProductScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductScoreRepository extends JpaRepository<ProductScore, Long> {

    List<ProductScore> findByStoreIgnoreCaseOrderByRankPositionAsc(String store);

    List<ProductScore> findAllByOrderByRankPositionAsc();
}
