package com.devlil0.whey_promotion_bot.scheduler;

import com.devlil0.whey_promotion_bot.dto.ProductOfferResponse;
import com.devlil0.whey_promotion_bot.dto.PromotionAlert;
import com.devlil0.whey_promotion_bot.dto.RankingItemResponse;
import com.devlil0.whey_promotion_bot.service.OfferPersistenceService;
import com.devlil0.whey_promotion_bot.service.PromotionService;
import com.devlil0.whey_promotion_bot.service.RankingService;
import com.devlil0.whey_promotion_bot.service.StoreCollectorService;
import com.devlil0.whey_promotion_bot.service.TelegramNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PriceCollectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(PriceCollectionScheduler.class);

    private final StoreCollectorService collectorService;
    private final OfferPersistenceService persistenceService;
    private final RankingService rankingService;
    private final PromotionService promotionService;
    private final TelegramNotificationService telegramService;

    public PriceCollectionScheduler(StoreCollectorService collectorService,
                                     OfferPersistenceService persistenceService,
                                     RankingService rankingService,
                                     PromotionService promotionService,
                                     TelegramNotificationService telegramService) {
        this.collectorService = collectorService;
        this.persistenceService = persistenceService;
        this.rankingService = rankingService;
        this.promotionService = promotionService;
        this.telegramService = telegramService;
    }

    @Scheduled(cron = "0 0 8,20 * * *", zone = "America/Sao_Paulo")
    public void collect() {
        log.info("Iniciando coleta de preços — {}", LocalDateTime.now());

        List<ProductOfferResponse> offers = collectorService.collectAllWheyOffers();
        int saved = persistenceService.upsertAll(offers);
        List<RankingItemResponse> ranking = rankingService.refreshRanking();

        log.info("Coleta finalizada — {} produtos salvos, {} no ranking", saved, ranking.size());

        List<PromotionAlert> promotions = promotionService.detectPromotions(LocalDateTime.now());
        if (promotions.isEmpty()) {
            log.info("Nenhuma promoção detectada nesta coleta.");
        } else {
            log.info("{} promoções detectadas — enviando ao Telegram.", promotions.size());
            telegramService.sendPromotions(promotions);
        }
    }

    @Scheduled(cron = "0 5 8 * * *", zone = "America/Sao_Paulo")
    public void sendDailyRanking() {
        log.info("Enviando ranking diário ao Telegram — {}", LocalDateTime.now());
        List<RankingItemResponse> ranking = rankingService.getRanking(10, null);
        telegramService.sendRanking(ranking);
    }
}
