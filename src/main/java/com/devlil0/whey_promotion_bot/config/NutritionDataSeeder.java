package com.devlil0.whey_promotion_bot.config;

import com.devlil0.whey_promotion_bot.entity.NutritionInfo;
import com.devlil0.whey_promotion_bot.repository.NutritionInfoRepository;
import com.devlil0.whey_promotion_bot.repository.ProductScoreRepository;
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
    private final ProductScoreRepository scoreRepository;

    public NutritionDataSeeder(NutritionInfoRepository repository,
                               ProductScoreRepository scoreRepository) {
        this.repository = repository;
        this.scoreRepository = scoreRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        scoreRepository.deleteAllInBatch();
        seedGrowthAndEarly();
        seedSoldiersNutrition();
        seedBlackSkull();
        seedNutrata();
        seedAdaptogen();
        seedAbsolutNutrition();
    }

    // ── Lojas originais ───────────────────────────────────────────────────────

    private void seedGrowthAndEarly() {
        reseed("GROWTH", List.of(
            build("3W Whey Protein 1Kg",                    "GROWTH", "Growth Supplements", 1000, bd(30), bd(24),   bd(33)),
            build("Basic Whey Protein 1Kg",                 "GROWTH", "Growth Supplements", 1000, bd(30), bd(10),   bd(33)),
            build("Medium Whey Protein 1Kg",                "GROWTH", "Growth Supplements", 1000, bd(30), bd(18),   bd(33)),
            build("(TOP) Whey Protein Concentrado 1Kg",     "GROWTH", "Growth Supplements", 1000, bd(30), bd(24),   bd(33)),
            build("(TOP) Whey Protein Concentrado 450g",    "GROWTH", "Growth Supplements",  450, bd(30), bd(24),   bd(15)),
            build("(TOP) Whey Protein Isolado 1Kg",         "GROWTH", "Growth Supplements", 1000, bd(30), bd(27),   bd(33)),
            build("Whey Protein Hidrolisado 1Kg",           "GROWTH", "Growth Supplements", 1000, bd(30), bd(27),   bd(33)),
            build("(TOP) Whey e Egg Sabor Natural 1Kg",     "GROWTH", "Growth Supplements", 1000, bd(30), bd(25),   new BigDecimal("33.3"))
        ));

        reseed("DARK_LAB", List.of(
            build("Whey Protein Concentrado Dark Lab",          "DARK_LAB", "Dark Lab",  1000, bd(50), bd(32),    bd(20)),
            build("100% Whey Protein Refil 900g",               "DARK_LAB", "Dark Lab",   900, bd(30), bd(21),    bd(30)),
            build("100% Whey Protein Pote 900g",                "DARK_LAB", "Dark Lab",   900, bd(30), bd(21),    bd(30)),
            build("100% Whey Protein 1,8KG",                    "DARK_LAB", "Dark Lab",  1800, bd(30), bd(21),    bd(60)),
            build("Whey One Refil 900g",                        "DARK_LAB", "Dark Lab",   900, bd(60), bd(30),    bd(15)),
            build("Isolate Protein Fuse 900g",                  "DARK_LAB", "Dark Lab",   900, bd(40), bd(26),    bd(22)),
            build("Isolate Protein Fuse 1,8kg",                 "DARK_LAB", "Dark Lab",  1800, bd(40), bd(26),    bd(45)),
            build("Iso Whey Zero Low Carb",                     "DARK_LAB", "Dark Lab",   900, bd(30), bd(26),    bd(30)),
            build("Iso Whey Zero 0 Carb",                       "DARK_LAB", "Dark Lab",   900, bd(17), bd(15), new BigDecimal("52.9"))
        ));

        reseed("PROFIT_LABS", List.of(
            build("100% Whey Refil 900g",              "PROFIT_LABS", "ProFit Laboratórios",  900, bd(40), bd(24), new BigDecimal("22.5")),
            build("Hyper Whey Refil 900g",             "PROFIT_LABS", "ProFit Laboratórios",  900, bd(120), bd(35), bd(7)),
            build("Isolate Protein Mix Pote 907g",     "PROFIT_LABS", "ProFit Laboratórios",  907, bd(40), bd(26), new BigDecimal("22.7")),
            build("Isolate Protein Mix Refil 900g",    "PROFIT_LABS", "ProFit Laboratórios",  900, bd(40), bd(26), bd(22)),
            build("Isolate Protein Mix Refil 1,8Kg",   "PROFIT_LABS", "ProFit Laboratórios", 1800, bd(40), bd(26), bd(45))
        ));
    }

    // ── Soldiers Nutrition ────────────────────────────────────────────────────

    private void seedSoldiersNutrition() {
        reseed("SOLDIERS_NUTRITION", List.of(
            build("Whey Protein Elite Pro Concentrado 80% 1kg", "SOLDIERS_NUTRITION", "Soldiers Nutrition", 1000, bd(50), bd(40), bd(20)),
            build("Whey Protein Concentrado 1kg",               "SOLDIERS_NUTRITION", "Soldiers Nutrition", 1000, bd(50), bd(30), bd(20)),
            build("Whey Protein Primal 1kg",                    "SOLDIERS_NUTRITION", "Soldiers Nutrition", 1000, bd(30), bd(10), new BigDecimal("33.33")),
            build("Whey Blend Protein Concentrado e Isolado 900g", "SOLDIERS_NUTRITION", "Soldiers Nutrition", 900, bd(30), bd(20), bd(30))
        ));
    }

    // ── Black Skull ───────────────────────────────────────────────────────────

    private void seedBlackSkull() {
        reseed("BLACK_SKULL", List.of(
            build("Whey Zero",      "BLACK_SKULL", "Black Skull",  837, new BigDecimal("23.25"), bd(20), bd(36)),
            build("Whey Turbo",     "BLACK_SKULL", "Black Skull",  907, bd(120), bd(30), new BigDecimal("7.56")),
            build("Anabolic Whey",  "BLACK_SKULL", "Black Skull",  907, bd(120), bd(30), new BigDecimal("7.56")),
            build("Isolate HD",     "BLACK_SKULL", "Black Skull",  900, bd(30),  bd(24), bd(30)),
            build("Protein Muscle", "BLACK_SKULL", "Black Skull",  900, bd(33),  bd(23), new BigDecimal("27.27")),
            build("Whey 3HD",       "BLACK_SKULL", "Black Skull",  900, bd(40),  bd(32), new BigDecimal("22.5"))
        ));
    }

    // ── Nutrata ───────────────────────────────────────────────────────────────

    private void seedNutrata() {
        reseed("NUTRATA", List.of(
            build("Iso Whey 900g",             "NUTRATA", "Nutrata",  900, bd(30), bd(25), bd(30)),
            build("Iso Whey 1800g",            "NUTRATA", "Nutrata", 1800, bd(30), bd(25), bd(60)),
            build("Whey Zero Lactose 900g",    "NUTRATA", "Nutrata",  900, bd(30), bd(21), bd(30))
        ));
    }

    // ── Adaptogen ─────────────────────────────────────────────────────────────

    private void seedAdaptogen() {
        reseed("ADAPTOGEN", List.of(
            build("Tasty Iso 900g",           "ADAPTOGEN", "Adaptogen",  900, bd(30), bd(25), bd(30)),
            build("Tasty Iso 1800g",          "ADAPTOGEN", "Adaptogen", 1800, bd(30), bd(25), bd(60)),
            build("Tasty Whey 3W Gourmet 912g", "ADAPTOGEN", "Adaptogen", 912, bd(34), bd(24), bd(26)),
            build("Adapto Whey 3W 912g",      "ADAPTOGEN", "Adaptogen",  912, bd(32), bd(25), new BigDecimal("28.5")),
            build("Adapto Whey 3W Refil 2200g", "ADAPTOGEN", "Adaptogen", 2200, bd(32), bd(25), bd(70))
        ));
    }

    // ── Absolut Nutrition ─────────────────────────────────────────────────────

    private void seedAbsolutNutrition() {
        reseed("ABSOLUT_NUTRITION", List.of(
            build("Whey High Protein 900g",        "ABSOLUT_NUTRITION", "Absolut Nutrition",  900, bd(30), bd(23), bd(30)),
            build("Whey 100% Pure 900g",           "ABSOLUT_NUTRITION", "Absolut Nutrition",  900, bd(30), bd(20), bd(30)),
            build("Whey Grego 3W 900g",            "ABSOLUT_NUTRITION", "Absolut Nutrition",  900, bd(35), bd(25), new BigDecimal("25.71")),
            build("Whey 3W Minot 900g",            "ABSOLUT_NUTRITION", "Absolut Nutrition",  900, bd(35), bd(21), new BigDecimal("25.71")),
            build("Whey 100% Power 900g",          "ABSOLUT_NUTRITION", "Bluster Nutrition",  900, bd(30), bd(20), bd(30))
        ));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    private void reseed(String store, List<NutritionInfo> products) {
        repository.deleteAll(repository.findByStore(store));
        repository.saveAll(products);
    }

    private static BigDecimal bd(int value) {
        return BigDecimal.valueOf(value);
    }
}
