package com.devlil0.whey_promotion_bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.devlil0.whey_promotion_bot.client.AbsolutNutritionClient;
import com.devlil0.whey_promotion_bot.client.AdaptogenClient;
import com.devlil0.whey_promotion_bot.client.BlackSkullClient;
import com.devlil0.whey_promotion_bot.client.DarkLabClient;
import com.devlil0.whey_promotion_bot.client.GrowthClient;
import com.devlil0.whey_promotion_bot.client.NutrataClient;
import com.devlil0.whey_promotion_bot.client.ProfitLabsClient;
import com.devlil0.whey_promotion_bot.client.SoldiersNutritionClient;
import com.devlil0.whey_promotion_bot.dto.OfertasFaixaResponse;
import com.devlil0.whey_promotion_bot.dto.ProductOfferResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Service
public class StoreCollectorService {

    private static final Logger log = LoggerFactory.getLogger(StoreCollectorService.class);

    private final GrowthClient growthClient;
    private final DarkLabClient darkLabClient;
    private final ProfitLabsClient profitLabsClient;
    private final SoldiersNutritionClient soldiersNutritionClient;
    private final BlackSkullClient blackSkullClient;
    private final NutrataClient nutrataClient;
    private final AdaptogenClient adaptogenClient;
    private final AbsolutNutritionClient absolutNutritionClient;
    private final ProductOfferMapper mapper;

    public StoreCollectorService(
            GrowthClient growthClient,
            DarkLabClient darkLabClient,
            ProfitLabsClient profitLabsClient,
            SoldiersNutritionClient soldiersNutritionClient,
            BlackSkullClient blackSkullClient,
            NutrataClient nutrataClient,
            AdaptogenClient adaptogenClient,
            AbsolutNutritionClient absolutNutritionClient,
            ProductOfferMapper mapper
    ) {
        this.growthClient = growthClient;
        this.darkLabClient = darkLabClient;
        this.profitLabsClient = profitLabsClient;
        this.soldiersNutritionClient = soldiersNutritionClient;
        this.blackSkullClient = blackSkullClient;
        this.nutrataClient = nutrataClient;
        this.adaptogenClient = adaptogenClient;
        this.absolutNutritionClient = absolutNutritionClient;
        this.mapper = mapper;
    }

    // ── Growth ────────────────────────────────────────────────────────────────

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

    public List<ProductOfferResponse> collectGrowthOfertasOffers() {
        List<ProductOfferResponse> offers = new ArrayList<>();
        int offset = 0;
        int limit = 30;
        int total = Integer.MAX_VALUE;

        while (offset < total) {
            JsonNode response = growthClient.getCategory("/ofertas/", "Maior desconto", offset, limit);
            offers.addAll(mapper.fromGrowthOfertas(response));

            JsonNode info = response.path("info");
            if (!info.hasNonNull("total")) break;

            total = info.path("total").asInt();
            int exibindo = info.path("exibindo").asInt(limit);

            if (exibindo <= 0 || info.path("next").asText("").isBlank()) break;
            offset += exibindo;
        }

        return offers;
    }

    public List<OfertasFaixaResponse> collectGrowthOfertasByBand() {
        List<ProductOfferResponse> all = collectGrowthOfertasOffers();

        record Band(String label, BigDecimal min, BigDecimal max) {}
        List<Band> bands = List.of(
                new Band("Até R$ 20",        BigDecimal.ZERO,              new BigDecimal("20")),
                new Band("R$ 20 a R$ 40",    new BigDecimal("20"),         new BigDecimal("40")),
                new Band("R$ 40 a R$ 60",    new BigDecimal("40"),         new BigDecimal("60")),
                new Band("R$ 60 a R$ 100",   new BigDecimal("60"),         new BigDecimal("100")),
                new Band("R$ 100 a R$ 150",  new BigDecimal("100"),        new BigDecimal("150")),
                new Band("R$ 150 a R$ 200",  new BigDecimal("150"),        new BigDecimal("200")),
                new Band("Acima de R$ 200",  new BigDecimal("200"),        new BigDecimal("999999"))
        );

        List<OfertasFaixaResponse> result = new ArrayList<>();
        for (Band band : bands) {
            List<ProductOfferResponse> inBand = all.stream()
                    .filter(p -> p.price() != null
                            && p.price().compareTo(band.min()) >= 0
                            && p.price().compareTo(band.max()) < 0)
                    .limit(3)
                    .toList();

            if (!inBand.isEmpty()) {
                result.add(new OfertasFaixaResponse(
                        band.label(),
                        band.min(),
                        band.max(),
                        inBand.size(),
                        inBand
                ));
            }
        }

        return result;
    }

    public List<ProductOfferResponse> collectGrowthShowcaseOffers() {
        return mapper.fromGrowthShowcase(growthClient.getWheyShowcase());
    }

    // ── Dark Lab ──────────────────────────────────────────────────────────────

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

    // ── ProFit Labs ───────────────────────────────────────────────────────────

    public List<ProductOfferResponse> collectProfitLabsPromocoesOffers() {
        List<ProductOfferResponse> offers = new ArrayList<>();
        int page = 1;
        int limit = 50;
        int total = Integer.MAX_VALUE;
        int collected = 0;

        while (collected < total && page <= 20) {
            JsonNode response = profitLabsClient.getPromocoes(page, limit);
            offers.addAll(mapper.fromProfitLabsPromocoes(response));

            JsonNode paging = response.path("paging");
            JsonNode products = response.path("Products");

            if (!products.isArray() || products.size() == 0) break;
            if (paging.hasNonNull("total")) total = paging.path("total").asInt();

            collected += products.size();
            page++;
        }

        return offers;
    }

    public List<OfertasFaixaResponse> collectProfitLabsPromocoesByBand() {
        List<ProductOfferResponse> all = collectProfitLabsPromocoesOffers();

        record Band(String label, BigDecimal min, BigDecimal max) {}
        List<Band> bands = List.of(
                new Band("Até R$ 20",        BigDecimal.ZERO,              new BigDecimal("20")),
                new Band("R$ 20 a R$ 40",    new BigDecimal("20"),         new BigDecimal("40")),
                new Band("R$ 40 a R$ 60",    new BigDecimal("40"),         new BigDecimal("60")),
                new Band("R$ 60 a R$ 100",   new BigDecimal("60"),         new BigDecimal("100")),
                new Band("R$ 100 a R$ 150",  new BigDecimal("100"),        new BigDecimal("150")),
                new Band("R$ 150 a R$ 200",  new BigDecimal("150"),        new BigDecimal("200")),
                new Band("Acima de R$ 200",  new BigDecimal("200"),        new BigDecimal("999999"))
        );

        List<OfertasFaixaResponse> result = new ArrayList<>();
        for (Band band : bands) {
            List<ProductOfferResponse> inBand = all.stream()
                    .filter(p -> p.price() != null
                            && p.price().compareTo(band.min()) >= 0
                            && p.price().compareTo(band.max()) < 0)
                    .limit(3)
                    .toList();

            if (!inBand.isEmpty()) {
                result.add(new OfertasFaixaResponse(
                        band.label(),
                        band.min(),
                        band.max(),
                        inBand.size(),
                        inBand
                ));
            }
        }

        return result;
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

    // ── Soldiers Nutrition ────────────────────────────────────────────────────

    public List<ProductOfferResponse> collectSoldiersNutritionOffers() {
        List<ProductOfferResponse> offers = new ArrayList<>();
        int page = 1;
        int limit = 250;

        while (page <= 5) {
            JsonNode response = soldiersNutritionClient.getWheyProducts(page, limit);
            JsonNode products = response.path("products");

            if (!products.isArray() || products.size() == 0) break;

            offers.addAll(mapper.fromSoldiersNutrition(response));

            if (products.size() < limit) break;
            page++;
        }

        return offers;
    }

    public List<ProductOfferResponse> collectSoldiersOfertaRelampago() {
        List<ProductOfferResponse> offers = new ArrayList<>();
        int page = 1;

        while (offers.size() < 10) {
            JsonNode response = soldiersNutritionClient.getOfertaRelampago(page, 250);
            JsonNode products = response.path("products");

            if (!products.isArray() || products.size() == 0) break;

            offers.addAll(mapper.fromSoldiersOfertaRelampago(response));

            if (products.size() < 250) break;
            page++;
        }

        return offers.size() > 10 ? offers.subList(0, 10) : offers;
    }

    // ── Black Skull ───────────────────────────────────────────────────────────

    public List<ProductOfferResponse> collectBlackSkullOffers() {
        List<ProductOfferResponse> offers = new ArrayList<>();
        int pageSize = 50;
        int maxPages = 5;

        for (int page = 0; page < maxPages; page++) {
            int from = page * pageSize;
            int to = from + pageSize - 1;
            JsonNode response = blackSkullClient.searchWhey(from, to);

            if (!response.isArray() || response.size() == 0) break;

            offers.addAll(mapper.fromBlackSkull(response));

            if (response.size() < pageSize) break;
        }

        return offers;
    }

    // ── Nutrata ───────────────────────────────────────────────────────────────

    public List<ProductOfferResponse> collectNutrataOffers() {
        List<ProductOfferResponse> offers = new ArrayList<>();
        int page = 1;
        int limit = 50;
        int total = Integer.MAX_VALUE;
        int collected = 0;

        while (collected < total && page <= 10) {
            JsonNode response = nutrataClient.searchWhey(page, limit);
            offers.addAll(mapper.fromNutrata(response));

            JsonNode paging = response.path("paging");
            JsonNode products = response.path("Products");

            if (!products.isArray() || products.size() == 0) break;
            if (paging.hasNonNull("total")) total = paging.path("total").asInt();

            collected += products.size();
            page++;
        }

        return offers;
    }

    // ── Adaptogen ─────────────────────────────────────────────────────────────

    public List<ProductOfferResponse> collectAdaptogenOffers() {
        List<ProductOfferResponse> offers = new ArrayList<>();
        int page = 1;
        int perPage = 100;

        while (page <= 5) {
            JsonNode response = adaptogenClient.searchWhey(page, perPage);

            if (!response.isArray() || response.size() == 0) break;

            offers.addAll(mapper.fromAdaptogen(response));

            if (response.size() < perPage) break;
            page++;
        }

        return offers;
    }

    // ── Absolut Nutrition ─────────────────────────────────────────────────────

    public List<ProductOfferResponse> collectAbsolutNutritionOffers() {
        List<ProductOfferResponse> offers = new ArrayList<>();
        int page = 1;
        int perPage = 100;

        while (page <= 5) {
            JsonNode response = absolutNutritionClient.searchWhey(page, perPage);

            if (!response.isArray() || response.size() == 0) break;

            offers.addAll(mapper.fromAbsolutNutrition(response));

            if (response.size() < perPage) break;
            page++;
        }

        return offers;
    }

    public List<ProductOfferResponse> collectGrowthWheyOffers() {
        try {
            List<ProductOfferResponse> offers = collectGrowthCategoryOffers("/whey-protein/");
            if (offers.isEmpty()) offers = collectGrowthCategoryOffers("/proteina/");
            if (offers.isEmpty()) offers = collectGrowthShowcaseOffers();
            return offers;
        } catch (Exception e) {
            log.warn("Coleta Growth category falhou, tentando vitrine: {}", e.getMessage());
            return collectGrowthShowcaseOffers();
        }
    }

    // ── Orquestração ──────────────────────────────────────────────────────────

    public List<ProductOfferResponse> collectAllWheyOffers() {
        List<ProductOfferResponse> offers = new ArrayList<>();

        tryCollect(offers, this::collectGrowthWheyOffers, "Growth");

        tryCollect(offers, this::collectDarkLabOffers,            "Dark Lab");
        tryCollect(offers, this::collectProfitLabsOffers,         "ProFit Labs");
        tryCollect(offers, this::collectSoldiersNutritionOffers,  "Soldiers Nutrition");
        tryCollect(offers, this::collectBlackSkullOffers,         "Black Skull");
        tryCollect(offers, this::collectNutrataOffers,            "Nutrata");
        tryCollect(offers, this::collectAdaptogenOffers,          "Adaptogen");
        tryCollect(offers, this::collectAbsolutNutritionOffers,   "Absolut Nutrition");

        return offers;
    }

    private void tryCollect(List<ProductOfferResponse> target,
                             Supplier<List<ProductOfferResponse>> collector,
                             String storeName) {
        try {
            target.addAll(collector.get());
        } catch (Exception e) {
            log.warn("Coleta {} falhou: {}", storeName, e.getMessage());
        }
    }
}
