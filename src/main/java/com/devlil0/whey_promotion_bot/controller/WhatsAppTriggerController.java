package com.devlil0.whey_promotion_bot.controller;

import com.devlil0.whey_promotion_bot.dto.OfertasFaixaResponse;
import com.devlil0.whey_promotion_bot.dto.ProductOfferResponse;
import com.devlil0.whey_promotion_bot.dto.PromotionAlert;
import com.devlil0.whey_promotion_bot.dto.RankingItemResponse;
import com.devlil0.whey_promotion_bot.service.WhatsAppNotificationService;
import com.devlil0.whey_promotion_bot.service.OfferPersistenceService;
import com.devlil0.whey_promotion_bot.service.PromotionService;
import com.devlil0.whey_promotion_bot.service.RankingService;
import com.devlil0.whey_promotion_bot.service.StoreCollectorService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppTriggerController {

    private final WhatsAppNotificationService whatsappService;
    private final RankingService rankingService;
    private final PromotionService promotionService;
    private final StoreCollectorService collectorService;
    private final OfferPersistenceService persistenceService;

    public WhatsAppTriggerController(WhatsAppNotificationService whatsappService,
                                     RankingService rankingService,
                                     PromotionService promotionService,
                                     StoreCollectorService collectorService,
                                     OfferPersistenceService persistenceService) {
        this.whatsappService = whatsappService;
        this.rankingService = rankingService;
        this.promotionService = promotionService;
        this.collectorService = collectorService;
        this.persistenceService = persistenceService;
    }

    @PostMapping("/trigger/collect")
    public Map<String, Object> triggerCollect() {
        List<ProductOfferResponse> offers = collectorService.collectAllWheyOffers();
        int saved = persistenceService.upsertAll(offers);
        List<RankingItemResponse> ranking = rankingService.refreshRanking();
        return Map.of("saved", saved, "rankingSize", ranking.size());
    }

    @PostMapping("/trigger/ranking")
    public Map<String, Object> triggerRanking(@RequestParam(defaultValue = "10") int top) {
        List<RankingItemResponse> ranking = rankingService.getRanking(top, null);
        whatsappService.sendRanking(ranking);
        return Map.of("sent", true, "itemCount", ranking.size());
    }

    @PostMapping("/trigger/promotions")
    public Map<String, Object> triggerPromotions() {
        List<PromotionAlert> promotions = promotionService.detectPromotions(LocalDateTime.now());
        whatsappService.sendPromotions(promotions);
        return Map.of("sent", !promotions.isEmpty(), "promotionCount", promotions.size());
    }

    @PostMapping("/trigger/growth/ofertas")
    public Map<String, Object> triggerGrowthOfertas() {
        List<OfertasFaixaResponse> ofertas = collectorService.collectGrowthOfertasByBand();
        whatsappService.sendOfertas(ofertas, "Growth Supplements");
        int total = ofertas.stream().mapToInt(OfertasFaixaResponse::quantidade).sum();
        return Map.of("sent", total > 0, "offerCount", total);
    }

    @PostMapping("/trigger/profitlabs/promocoes")
    public Map<String, Object> triggerProfitLabsPromocoes() {
        List<OfertasFaixaResponse> promocoes = collectorService.collectProfitLabsPromocoesByBand();
        whatsappService.sendOfertas(promocoes, "ProFit Labs");
        int total = promocoes.stream().mapToInt(OfertasFaixaResponse::quantidade).sum();
        return Map.of("sent", total > 0, "offerCount", total);
    }

    @PostMapping("/trigger/soldiers/oferta-relampago")
    public Map<String, Object> triggerSoldiersOfertaRelampago() {
        List<ProductOfferResponse> ofertas = collectorService.collectSoldiersOfertaRelampago();
        whatsappService.sendOfertaRelampago(ofertas);
        return Map.of("sent", !ofertas.isEmpty(), "offerCount", ofertas.size());
    }
}
