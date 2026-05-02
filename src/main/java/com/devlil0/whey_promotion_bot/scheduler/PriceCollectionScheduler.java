package com.devlil0.whey_promotion_bot.scheduler;

import com.devlil0.whey_promotion_bot.dto.ProductOfferResponse;
import com.devlil0.whey_promotion_bot.entity.ProductOffer;
import com.devlil0.whey_promotion_bot.repository.ProductOfferRepository;
import com.devlil0.whey_promotion_bot.service.StoreCollectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PriceCollectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(PriceCollectionScheduler.class);

    private final StoreCollectorService collectorService;
    private final ProductOfferRepository repository;

    public PriceCollectionScheduler(StoreCollectorService collectorService,
                                     ProductOfferRepository repository) {
        this.collectorService = collectorService;
        this.repository = repository;
    }

    @Scheduled(cron = "0 0 8,20 * * *")
    @Transactional
    public void collect() {
        log.info("Iniciando coleta de preços — {}", LocalDateTime.now());

        List<ProductOfferResponse> offers = collectorService.collectAllWheyOffers();

        Map<String, ProductOffer> existing = repository.findAll().stream()
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

        repository.saveAll(toSave);

        log.info("Coleta finalizada — {} produtos salvos", toSave.size());
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
