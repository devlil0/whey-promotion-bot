package com.devlil0.whey_promotion_bot.scheduler;

import com.devlil0.whey_promotion_bot.repository.ProductScoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class StartupCollector {

    private static final Logger log = LoggerFactory.getLogger(StartupCollector.class);

    private final PriceCollectionScheduler scheduler;
    private final ProductScoreRepository scoreRepository;

    public StartupCollector(PriceCollectionScheduler scheduler,
                             ProductScoreRepository scoreRepository) {
        this.scheduler = scheduler;
        this.scoreRepository = scoreRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void runIfRankingEmpty() {
        long existing = scoreRepository.count();
        if (existing > 0) {
            log.info("Ranking já populado ({} registros) — pulando coleta inicial.", existing);
            return;
        }
        log.info("Ranking vazio no boot — disparando coleta inicial.");
        try {
            scheduler.collect();
        } catch (Exception e) {
            log.error("Coleta inicial falhou: {}", e.getMessage(), e);
        }
    }
}
