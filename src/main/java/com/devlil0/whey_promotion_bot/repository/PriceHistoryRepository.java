package com.devlil0.whey_promotion_bot.repository;

import com.devlil0.whey_promotion_bot.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    @Query("SELECT AVG(p.priceSnapshot) FROM PriceHistory p " +
            "WHERE p.productOfferId = :productOfferId " +
            "AND p.recordedAt >= :since " +
            "AND p.recordedAt < :excludingFrom")
    BigDecimal averagePriceBetween(@Param("productOfferId") Long productOfferId,
                                   @Param("since") LocalDateTime since,
                                   @Param("excludingFrom") LocalDateTime excludingFrom);

    @Query("SELECT COUNT(p) FROM PriceHistory p " +
            "WHERE p.productOfferId = :productOfferId " +
            "AND p.recordedAt >= :since " +
            "AND p.recordedAt < :excludingFrom")
    long countBetween(@Param("productOfferId") Long productOfferId,
                      @Param("since") LocalDateTime since,
                      @Param("excludingFrom") LocalDateTime excludingFrom);

    @Modifying
    @Transactional
    @Query("DELETE FROM PriceHistory p WHERE p.recordedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
