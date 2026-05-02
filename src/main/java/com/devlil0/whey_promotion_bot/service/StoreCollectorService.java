package com.devlil0.whey_promotion_bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.devlil0.whey_promotion_bot.client.DarkLabClient;
import com.devlil0.whey_promotion_bot.client.GrowthClient;
import com.devlil0.whey_promotion_bot.client.MercadoLivreClient;
import com.devlil0.whey_promotion_bot.client.ProfitLabsClient;
import com.devlil0.whey_promotion_bot.dto.ProductOfferResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StoreCollectorService {

    private static final Logger log = LoggerFactory.getLogger(StoreCollectorService.class);

    private final GrowthClient growthClient;
    private final DarkLabClient darkLabClient;
    private final ProfitLabsClient profitLabsClient;
    private final MercadoLivreClient mercadoLivreClient;
    private final ProductOfferMapper mapper;

    public StoreCollectorService(
            GrowthClient growthClient,
            DarkLabClient darkLabClient,
            ProfitLabsClient profitLabsClient,
            MercadoLivreClient mercadoLivreClient,
            ProductOfferMapper mapper
    ) {
        this.growthClient = growthClient;
        this.darkLabClient = darkLabClient;
        this.profitLabsClient = profitLabsClient;
        this.mercadoLivreClient = mercadoLivreClient;
        this.mapper = mapper;
    }

    public List<ProductOfferResponse> collectGrowthCategoryOffers(String categoryUrl) {
        List<ProductOfferResponse> offers = new ArrayList<>();
        int offset = 0;
        int limit = 30;
        int total = Integer.MAX_VALUE;

        while (offset < total) {
            JsonNode response = growthClient.getCategory(categoryUrl, "Menor Preço", offset, limit);
            offers.addAll(mapper.fromGrowthCategory(response));

            JsonNode info = response.path("info");
            if (!info.hasNonNull("total")) break;

            total = info.path("total").asInt();
            int exibindo = info.path("exibindo").asInt(limit);

            if (exibindo <= 0) break;
            offset += exibindo;
        }

        return offers;
    }

    public List<ProductOfferResponse> collectGrowthShowcaseOffers() {
        return mapper.fromGrowthShowcase(growthClient.getWheyShowcase());
    }

    public List<ProductOfferResponse> collectDarkLabOffers() {
        List<ProductOfferResponse> offers = new ArrayList<>();
        int page = 1;
        int limit = 250;

        while (page <= 20) {
            JsonNode response = darkLabClient.getProducts(page, limit);
            JsonNode products = response.path("products");

            if (!products.isArray() || products.size() == 0) break;

            offers.addAll(mapper.fromDarkLab(response));

            if (products.size() < limit) break;
            page++;
        }

        return offers;
    }

    public List<ProductOfferResponse> collectProfitLabsOffers() {
        List<ProductOfferResponse> offers = new ArrayList<>();
        int page = 1;
        int limit = 50;
        int total = Integer.MAX_VALUE;
        int collected = 0;

        while (collected < total && page <= 20) {
            JsonNode response = profitLabsClient.getProducts(page, limit);
            offers.addAll(mapper.fromProfitLabs(response));

            JsonNode paging = response.path("paging");
            JsonNode products = response.path("Products");

            if (!products.isArray() || products.size() == 0) break;
            if (paging.hasNonNull("total")) total = paging.path("total").asInt();

            collected += products.size();
            page++;
        }

        return offers;
    }

    public List<ProductOfferResponse> collectMercadoLivreOffers() {
        List<ProductOfferResponse> offers = new ArrayList<>();
        int limit = 50;
        int maxPages = 3;

        for (int page = 0; page < maxPages; page++) {
            JsonNode response = mercadoLivreClient.searchWheyProtein(limit, page * limit);
            List<ProductOfferResponse> page_offers = mapper.fromMercadoLivre(response);
            if (page_offers.isEmpty()) break;
            offers.addAll(page_offers);

            int total = response.path("paging").path("total").asInt(0);
            if (offers.size() >= total) break;
        }

        return offers;
    }

    public List<ProductOfferResponse> collectAllWheyOffers() {
        List<ProductOfferResponse> offers = new ArrayList<>();

        try {
            List<ProductOfferResponse> growth = collectGrowthCategoryOffers("/whey-protein/");
            if (growth.isEmpty()) {
                growth = collectGrowthCategoryOffers("/proteina/");
            }
            if (growth.isEmpty()) {
                growth = collectGrowthShowcaseOffers();
            }
            offers.addAll(growth);
        } catch (Exception e) {
            offers.addAll(collectGrowthShowcaseOffers());
        }

        offers.addAll(collectDarkLabOffers());
        offers.addAll(collectProfitLabsOffers());

        try {
            offers.addAll(collectMercadoLivreOffers());
        } catch (Exception e) {
            log.warn("Coleta Mercado Livre falhou, continuando sem ela: {}", e.getMessage());
        }

        return offers;
    }
}
