package com.devlil0.whey_promotion_bot.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "nutrition_info")
public class NutritionInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name_normalized", nullable = false)
    private String productNameNormalized;

    @Column(name = "store", nullable = false)
    private String store;

    @Column(name = "brand")
    private String brand;

    @Column(name = "weight_grams")
    private Integer weightGrams;

    @Column(name = "serving_size_grams", precision = 10, scale = 2, nullable = false)
    private BigDecimal servingSizeGrams;

    @Column(name = "protein_per_serving_grams", precision = 10, scale = 2, nullable = false)
    private BigDecimal proteinPerServingGrams;

    @Column(name = "servings_per_container", precision = 10, scale = 2, nullable = false)
    private BigDecimal servingsPerContainer;

    @Column(name = "total_protein_grams", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalProteinGrams;

    public NutritionInfo() {}

    public Long getId() { return id; }

    public String getProductNameNormalized() { return productNameNormalized; }
    public void setProductNameNormalized(String productNameNormalized) { this.productNameNormalized = productNameNormalized; }

    public String getStore() { return store; }
    public void setStore(String store) { this.store = store; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public Integer getWeightGrams() { return weightGrams; }
    public void setWeightGrams(Integer weightGrams) { this.weightGrams = weightGrams; }

    public BigDecimal getServingSizeGrams() { return servingSizeGrams; }
    public void setServingSizeGrams(BigDecimal servingSizeGrams) { this.servingSizeGrams = servingSizeGrams; }

    public BigDecimal getProteinPerServingGrams() { return proteinPerServingGrams; }
    public void setProteinPerServingGrams(BigDecimal proteinPerServingGrams) { this.proteinPerServingGrams = proteinPerServingGrams; }

    public BigDecimal getServingsPerContainer() { return servingsPerContainer; }
    public void setServingsPerContainer(BigDecimal servingsPerContainer) { this.servingsPerContainer = servingsPerContainer; }

    public BigDecimal getTotalProteinGrams() { return totalProteinGrams; }
    public void setTotalProteinGrams(BigDecimal totalProteinGrams) { this.totalProteinGrams = totalProteinGrams; }
}
