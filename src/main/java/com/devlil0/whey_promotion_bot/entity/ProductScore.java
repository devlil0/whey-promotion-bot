package com.devlil0.whey_promotion_bot.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_score")
public class ProductScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_offer_id", nullable = false)
    private ProductOffer productOffer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nutrition_info_id", nullable = false)
    private NutritionInfo nutritionInfo;

    @Column(name = "store", nullable = false)
    private String store;

    @Column(name = "cost_per_protein_gram", precision = 10, scale = 4, nullable = false)
    private BigDecimal costPerProteinGram;

    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition;

    @Column(name = "scored_at", nullable = false)
    private LocalDateTime scoredAt;

    public ProductScore() {}

    public Long getId() { return id; }

    public ProductOffer getProductOffer() { return productOffer; }
    public void setProductOffer(ProductOffer productOffer) { this.productOffer = productOffer; }

    public NutritionInfo getNutritionInfo() { return nutritionInfo; }
    public void setNutritionInfo(NutritionInfo nutritionInfo) { this.nutritionInfo = nutritionInfo; }

    public String getStore() { return store; }
    public void setStore(String store) { this.store = store; }

    public BigDecimal getCostPerProteinGram() { return costPerProteinGram; }
    public void setCostPerProteinGram(BigDecimal costPerProteinGram) { this.costPerProteinGram = costPerProteinGram; }

    public Integer getRankPosition() { return rankPosition; }
    public void setRankPosition(Integer rankPosition) { this.rankPosition = rankPosition; }

    public LocalDateTime getScoredAt() { return scoredAt; }
    public void setScoredAt(LocalDateTime scoredAt) { this.scoredAt = scoredAt; }
}
