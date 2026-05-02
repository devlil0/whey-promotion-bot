package com.devlil0.whey_promotion_bot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.devlil0.whey_promotion_bot.client.DarkLabClient;
import com.devlil0.whey_promotion_bot.client.GrowthClient;
import com.devlil0.whey_promotion_bot.client.ProfitLabsClient;
import com.devlil0.whey_promotion_bot.dto.ProductOfferResponse;
import com.devlil0.whey_promotion_bot.service.StoreCollectorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class StoreOffersController {

    private final GrowthClient growthClient;
    private final DarkLabClient darkLabClient;
    private final ProfitLabsClient profitLabsClient;
    private final StoreCollectorService collectorService;

    public StoreOffersController(
            GrowthClient growthClient,
            DarkLabClient darkLabClient,
            ProfitLabsClient profitLabsClient,
            StoreCollectorService collectorService
    ) {
        this.growthClient = growthClient;
        this.darkLabClient = darkLabClient;
        this.profitLabsClient = profitLabsClient;
        this.collectorService = collectorService;
    }

    @GetMapping("/growth/category/raw")
    public JsonNode getGrowthCategoryRaw(
            @RequestParam(defaultValue = "/proteina/") String category,
            @RequestParam(defaultValue = "Menor Preço") String order,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "30") int limit
    ) {
        return growthClient.getCategory(category, order, offset, limit);
    }

    @GetMapping("/growth/showcase/raw")
    public JsonNode getGrowthShowcaseRaw() {
        return growthClient.getWheyShowcase();
    }

    @GetMapping("/growth/offers")
    public List<ProductOfferResponse> getGrowthOffers(
            @RequestParam(defaultValue = "/whey-protein/") String category
    ) {
        return collectorService.collectGrowthCategoryOffers(category);
    }

    @GetMapping("/darklab/products/raw")
    public JsonNode getDarkLabRaw(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "250") int limit
    ) {
        return darkLabClient.getProducts(page, limit);
    }

    @GetMapping("/darklab/offers")
    public List<ProductOfferResponse> getDarkLabOffers() {
        return collectorService.collectDarkLabOffers();
    }

    @GetMapping("/profitlabs/products/raw")
    public JsonNode getProfitLabsRaw(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return profitLabsClient.getProducts(page, limit);
    }

    @GetMapping("/profitlabs/offers")
    public List<ProductOfferResponse> getProfitLabsOffers() {
        return collectorService.collectProfitLabsOffers();
    }

    @GetMapping("/offers/whey")
    public List<ProductOfferResponse> getAllWheyOffers() {
        return collectorService.collectAllWheyOffers();
    }
}
