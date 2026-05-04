package com.devlil0.whey_promotion_bot.config;

import com.devlil0.whey_promotion_bot.entity.NutritionInfo;
import com.devlil0.whey_promotion_bot.repository.NutritionInfoRepository;
import com.devlil0.whey_promotion_bot.service.ProductFilter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class NutritionDataSeeder implements ApplicationRunner {

    private final NutritionInfoRepository repository;

    public NutritionDataSeeder(NutritionInfoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) return;

        repository.saveAll(List.of(

            // ── Growth Supplements ────────────────────────────────────────────
            build("3W Whey Protein 1Kg",                    "GROWTH", "Growth Supplements", 1000, bd(30), bd(24),   bd(33)),
            build("Basic Whey Protein 1Kg",                 "GROWTH", "Growth Supplements", 1000, bd(30), bd(10),   bd(33)),
            build("Medium Whey Protein 1Kg",                "GROWTH", "Growth Supplements", 1000, bd(30), bd(18),   bd(33)),
            build("(TOP) Whey Protein Concentrado 1Kg",     "GROWTH", "Growth Supplements", 1000, bd(30), bd(24),   bd(33)),
            build("(TOP) Whey Protein Concentrado 450g",    "GROWTH", "Growth Supplements",  450, bd(30), bd(24),   bd(15)),
            build("(TOP) Whey Protein Isolado 1Kg",         "GROWTH", "Growth Supplements", 1000, bd(30), bd(27),   bd(33)),
            build("Whey Protein Hidrolisado 1Kg",           "GROWTH", "Growth Supplements", 1000, bd(30), bd(27),   bd(33)),

            // ── Dark Lab ──────────────────────────────────────────────────────
            build("Whey Protein Concentrado Dark Lab",          "DARK_LAB", "Dark Lab",  1000, bd(50), bd(32),    bd(20)),
            build("100% Whey Protein Refil 900g",               "DARK_LAB", "Dark Lab",   900, bd(30), bd(21),    bd(30)),
            build("100% Whey Protein Pote 900g",                "DARK_LAB", "Dark Lab",   900, bd(30), bd(21),    bd(30)),
            build("100% Whey Protein 1,8KG",                    "DARK_LAB", "Dark Lab",  1800, bd(30), bd(21),    bd(60)),
            build("Whey One Refil 900g",                        "DARK_LAB", "Dark Lab",   900, bd(60), bd(30),    bd(15)),
            build("Isolate Protein Fuse 900g",                  "DARK_LAB", "Dark Lab",   900, bd(40), bd(26),    bd(22)),
            build("Isolate Protein Fuse 1,8kg",                 "DARK_LAB", "Dark Lab",  1800, bd(40), bd(26),    bd(45)),
            build("Iso Whey Zero Low Carb",                     "DARK_LAB", "Dark Lab",   900, bd(30), bd(26),    bd(30)),
            build("Iso Whey Zero 0 Carb",                       "DARK_LAB", "Dark Lab",   900, bd(17), bd(15), new BigDecimal("52.9")),

            // ── Profit Labs ───────────────────────────────────────────────────
            build("100% Whey Refil 900g",              "PROFIT_LABS", "ProFit Laboratórios",  900, bd(40), bd(24), new BigDecimal("22.5")),
            build("Hyper Whey Refil 900g",             "PROFIT_LABS", "ProFit Laboratórios",  900, bd(120), bd(35), bd(7)),
            build("Isolate Protein Mix Pote 907g",     "PROFIT_LABS", "ProFit Laboratórios",  907, bd(40), bd(26), new BigDecimal("22.7")),
            build("Isolate Protein Mix Refil 900g",    "PROFIT_LABS", "ProFit Laboratórios",  900, bd(40), bd(26), bd(22)),
            build("Isolate Protein Mix Refil 1,8Kg",   "PROFIT_LABS", "ProFit Laboratórios", 1800, bd(40), bd(26), bd(45)),

            build("(TOP) Whey e Egg Sabor Natural 1Kg", "GROWTH", "Growth Supplements", 1000, bd(30), bd(25), new BigDecimal("33.3"))
        ));
    }

    private NutritionInfo build(String productName, String store, String brand, int weightGrams,
                                 BigDecimal servingSize, BigDecimal protein, BigDecimal servings) {
        NutritionInfo info = new NutritionInfo();
        info.setProductNameNormalized(ProductFilter.normalize(productName));
        info.setStore(store);
        info.setBrand(brand);
        info.setWeightGrams(weightGrams);
        info.setServingSizeGrams(servingSize);
        info.setProteinPerServingGrams(protein);
        info.setServingsPerContainer(servings);
        info.setTotalProteinGrams(protein.multiply(servings).setScale(2, RoundingMode.HALF_UP));
        return info;
    }

    private static BigDecimal bd(int value) {
        return BigDecimal.valueOf(value);
    }
}
