package com.devlil0.whey_promotion_bot.repository;

import com.devlil0.whey_promotion_bot.entity.ProductOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductOfferRepository extends JpaRepository<ProductOffer, Long> {

    List<ProductOffer> findByStore(String store);

    List<ProductOffer> findByAvailableTrue();

    Optional<ProductOffer> findByStoreAndExternalId(String store, String externalId);
}
