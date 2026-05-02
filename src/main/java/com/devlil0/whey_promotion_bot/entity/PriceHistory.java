package com.devlil0.whey_promotion_bot.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_history", indexes = {
        @Index(name = "idx_price_history_offer_recorded", columnList = "product_offer_id, recorded_at")
})
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_offer_id", nullable = false)
    private Long productOfferId;

    @Column(name = "store", nullable = false)
    private String store;

    @Column(name = "price_snapshot", precision = 10, scale = 2, nullable = false)
    private BigDecimal priceSnapshot;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    public PriceHistory() {}

    public PriceHistory(Long productOfferId, String store, BigDecimal priceSnapshot, LocalDateTime recordedAt) {
        this.productOfferId = productOfferId;
        this.store = store;
        this.priceSnapshot = priceSnapshot;
        this.recordedAt = recordedAt;
    }

    public Long getId() { return id; }

    public Long getProductOfferId() { return productOfferId; }
    public void setProductOfferId(Long productOfferId) { this.productOfferId = productOfferId; }

    public String getStore() { return store; }
    public void setStore(String store) { this.store = store; }

    public BigDecimal getPriceSnapshot() { return priceSnapshot; }
    public void setPriceSnapshot(BigDecimal priceSnapshot) { this.priceSnapshot = priceSnapshot; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
