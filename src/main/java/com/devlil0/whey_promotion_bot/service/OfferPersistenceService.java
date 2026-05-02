package com.devlil0.whey_promotion_bot.service;

import com.devlil0.whey_promotion_bot.dto.ProductOfferResponse;
import com.devlil0.whey_promotion_bot.entity.PriceHistory;
import com.devlil0.whey_promotion_bot.entity.ProductOffer;
import com.devlil0.whey_promotion_bot.repository.PriceHistoryRepository;
import com.devlil0.whey_promotion_bot.repository.ProductOfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OfferPersistenceService {

    private final ProductOfferRepository offerRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    public OfferPersistenceService(ProductOfferRepository offerRepository,
                                    PriceHistoryRepository priceHistoryRepository) {
        this.offerRepository = offerRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @Transactional
    public int upsertAll(List<ProductOfferResponse> offers) {
        Map<String, ProductOffer> existing = offerRepository.findAll().stream()
                .collect(Collectors.toMap(
                        p -> p.getStore() + ":" + p.getExternalId(),
                        p -> p
                ));

        LocalDateTime now = LocalDateTime.now();
        List<ProductOffer> toSave = offers.stream()
                .map(dto -> {
                    String key = dto.store() + ":" + dto.externalId();
                    ProductOffer entity = existing.getOrDefault(key, new ProductOffer());
                    mapToEntity(entity, dto, now);
                    return entity;
                })
                .collect(Collectors.toList());

        List<ProductOffer> saved = offerRepository.saveAll(toSave);
        recordPriceHistory(saved, now);
        return saved.size();
    }

    private void recordPriceHistory(List<ProductOffer> saved, LocalDateTime now) {
        List<PriceHistory> snapshots = new ArrayList<>();
        for (ProductOffer offer : saved) {
            BigDecimal effective = offer.getCashPrice() != null ? offer.getCashPrice() : offer.getPrice();
            if (effective == null || effective.compareTo(BigDecimal.ZERO) <= 0) continue;
            if (Boolean.FALSE.equals(offer.getAvailable())) continue;
            snapshots.add(new PriceHistory(offer.getId(), offer.getStore(), effective, now));
        }
        if (!snapshots.isEmpty()) {
            priceHistoryRepository.saveAll(snapshots);
        }
    }

    private void mapToEntity(ProductOffer entity, ProductOfferResponse dto, LocalDateTime now) {
        entity.setStore(dto.store());
        entity.setExternalId(dto.externalId());
        entity.setVariantId(dto.variantId());
        entity.setSku(dto.sku());
        entity.setName(dto.name());
        entity.setBrand(dto.brand());
        entity.setCategory(dto.category());
        entity.setPrice(dto.price());
        entity.setCashPrice(dto.cashPrice());
        entity.setOldPrice(dto.oldPrice());
        entity.setAvailable(dto.available());
        entity.setStock(dto.stock());
        entity.setWeightGrams(dto.weightGrams());
        entity.setProductUrl(dto.productUrl());
        entity.setImageUrl(dto.imageUrl());
        entity.setSourceType(dto.sourceType());
        entity.setLastUpdatedAt(now);
    }
}
