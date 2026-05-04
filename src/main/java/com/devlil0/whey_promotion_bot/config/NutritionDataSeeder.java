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
        seedGrowthAndEarly();
        seedSoldiersNutrition();
        seedBlackSkull();
        seedNutrata();
        seedAdaptogen();
        seedAbsolutNutrition();
    }

    // ── Lojas originais ───────────────────────────────────────────────────────

    private void seedGrowthAndEarly() {
        if (repository.findByStore("GROWTH").isEmpty()) {
            repository.saveAll(List.of(
                build("3W Whey Protein 1Kg",                    "GROWTH", "Growth Supplements", 1000, bd(30), bd(24),   bd(33)),
                build("Basic Whey Protein 1Kg",                 "GROWTH", "Growth Supplements", 1000, bd(30), bd(10),   bd(33)),
                build("Medium Whey Protein 1Kg",                "GROWTH", "Growth Supplements", 1000, bd(30), bd(18),   bd(33)),
                build("(TOP) Whey Protein Concentrado 1Kg",     "GROWTH", "Growth Supplements", 1000, bd(30), bd(24),   bd(33)),
                build("(TOP) Whey Protein Concentrado 450g",    "GROWTH", "Growth Supplements",  450, bd(30), bd(24),   bd(15)),
                build("(TOP) Whey Protein Isolado 1Kg",         "GROWTH", "Growth Supplements", 1000, bd(30), bd(27),   bd(33)),
                build("Whey Protein Hidrolisado 1Kg",           "GROWTH", "Growth Supplements", 1000, bd(30), bd(27),   bd(33)),
                build("(TOP) Whey e Egg Sabor Natural 1Kg",     "GROWTH", "Growth Supplements", 1000, bd(30), bd(25),   new BigDecimal("33.3"))
            ));
        }

        if (repository.findByStore("DARK_LAB").isEmpty()) {
            repository.saveAll(List.of(
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
        }

        if (repository.findByStore("PROFIT_LABS").isEmpty()) {
            repository.saveAll(List.of(
                build("100% Whey Refil 900g",              "PROFIT_LABS", "ProFit Laboratórios",  900, bd(40), bd(24), new BigDecimal("22.5")),
                build("Hyper Whey Refil 900g",             "PROFIT_LABS", "ProFit Laboratórios",  900, bd(120), bd(35), bd(7)),
                build("Isolate Protein Mix Pote 907g",     "PROFIT_LABS", "ProFit Laboratórios",  907, bd(40), bd(26), new BigDecimal("22.7")),
                build("Isolate Protein Mix Refil 900g",    "PROFIT_LABS", "ProFit Laboratórios",  900, bd(40), bd(26), bd(22)),
                build("Isolate Protein Mix Refil 1,8Kg",   "PROFIT_LABS", "ProFit Laboratórios", 1800, bd(40), bd(26), bd(45))
            ));
        }
    }

    // ── Soldiers Nutrition ────────────────────────────────────────────────────

    private void seedSoldiersNutrition() {
        if (!repository.findByStore("SOLDIERS_NUTRITION").isEmpty()) return;
        repository.saveAll(List.of(
            // 80% concentrado → total proteína = 800g; porção estimada 30g → 24g proteína
            build("Whey Protein Elite Pro 80% 1kg", "SOLDIERS_NUTRITION", "Soldiers Nutrition",
                  1000, bd(30), bd(24), new BigDecimal("33.33"))
        ));
    }

    // ── Black Skull ───────────────────────────────────────────────────────────

    private void seedBlackSkull() {
        if (!repository.findByStore("BLACK_SKULL").isEmpty()) return;
        repository.saveAll(List.of(
            // Whey Zero Isolado Refil 837g — 836g/36 doses = 23,25g/dose; 20g prot; total 720g
            build("Whey Zero",                 "BLACK_SKULL", "Black Skull",  837, new BigDecimal("23.25"), bd(20), bd(36)),
            // Anabolic Whey 900g — 21g prot/dose; serving estimado 30g (concentrado)
            build("Anabolic Whey",             "BLACK_SKULL", "Black Skull",  900, bd(30), bd(21), bd(30)),
            // Protein Muscle / Whey Blend 900g — 23g prot/dose; serving estimado 33g (blend)
            build("Protein Muscle",            "BLACK_SKULL", "Black Skull",  900, bd(33), bd(23), new BigDecimal("27.27")),
            // Whey Turbo 907g — 30g prot/dose; serving estimado 40g (concentrado premium)
            build("Whey Turbo",                "BLACK_SKULL", "Black Skull",  907, bd(40), bd(30), new BigDecimal("22.68")),
            // Isolate HD 900g — 24g prot/dose; serving estimado 30g (isolado)
            build("Isolate HD",                "BLACK_SKULL", "Black Skull",  900, bd(30), bd(24), bd(30)),
            // Whey 3HD 900g WPC/WPI/WPH — 32g prot/dose; serving estimado 40g (blend premium)
            build("Whey 3HD",                  "BLACK_SKULL", "Black Skull",  900, bd(40), bd(32), new BigDecimal("22.5"))
        ));
    }

    // ── Nutrata ───────────────────────────────────────────────────────────────

    private void seedNutrata() {
        if (!repository.findByStore("NUTRATA").isEmpty()) return;
        repository.saveAll(List.of(
            // Iso Whey 900g — 25g prot/dose; serving estimado 30g (isolado)
            build("Iso Whey 900g",             "NUTRATA", "Nutrata",  900, bd(30), bd(25), bd(30)),
            // Iso Whey 1800g — mesma linha, embalagem maior
            build("Iso Whey 1800g",            "NUTRATA", "Nutrata", 1800, bd(30), bd(25), bd(60)),
            // Whey Zero Lactose 900g — 21g prot/dose; serving estimado 30g (concentrado)
            build("Whey Zero Lactose 900g",    "NUTRATA", "Nutrata",  900, bd(30), bd(21), bd(30))
        ));
    }

    // ── Adaptogen ─────────────────────────────────────────────────────────────

    private void seedAdaptogen() {
        if (!repository.findByStore("ADAPTOGEN").isEmpty()) return;
        repository.saveAll(List.of(
            // Tasty Iso 900g — tabela da 1800g (mesma linha): 30g/porção, 25g prot, 30 porções
            build("Tasty Iso 900g",                        "ADAPTOGEN", "Adaptogen",  900, bd(30), bd(25), bd(30)),
            // Tasty Iso 1800g — confirmado via tabela nutricional oficial
            build("Tasty Iso 1800g",                       "ADAPTOGEN", "Adaptogen", 1800, bd(30), bd(25), bd(60)),
            // Tasty Whey 3W Gourmet 912g — confirmado: 34g/porção, 24g prot, 26 porções
            build("Tasty Whey 3W Gourmet 912g",            "ADAPTOGEN", "Adaptogen",  912, bd(34), bd(24), bd(26)),
            // Adapto Whey 3W 912g — confirmado: 32g/porção, 25g prot, 28,5 porções
            build("Adapto Whey 3W 912g",                   "ADAPTOGEN", "Adaptogen",  912, bd(32), bd(25), new BigDecimal("28.5")),
            // Adapto Whey 3W Cookies and Cream 912g — 33g/porção, 25g prot (página específica)
            build("Adapto Whey 3W Cookies and Cream 912g", "ADAPTOGEN", "Adaptogen",  912, bd(33), bd(25), new BigDecimal("27.64")),
            // Adapto Whey 3W Refil 2200g — confirmado: 32g/porção, 25g prot, 70 porções
            build("Adapto Whey 3W Refil 2200g",            "ADAPTOGEN", "Adaptogen", 2200, bd(32), bd(25), bd(70))
        ));
    }

    // ── Absolut Nutrition ─────────────────────────────────────────────────────

    private void seedAbsolutNutrition() {
        if (!repository.findByStore("ABSOLUT_NUTRITION").isEmpty()) return;
        repository.saveAll(List.of(
            // Whey High Protein 900g — 23g prot/dose; serving estimado 30g (concentrado)
            build("Whey High Protein 900g",        "ABSOLUT_NUTRITION", "Absolut Nutrition",  900, bd(30), bd(23), bd(30)),
            // Whey Concentrado 100% Pure Pouch 900g — 20g prot/dose; serving estimado 30g
            build("Whey 100% Pure 900g",           "ABSOLUT_NUTRITION", "Absolut Nutrition",  900, bd(30), bd(20), bd(30)),
            // Whey 3W Trisabor 900g — 25g prot/dose; serving estimado 35g (blend 3W)
            build("Whey Grego 3W 900g",            "ABSOLUT_NUTRITION", "Absolut Nutrition",  900, bd(35), bd(25), new BigDecimal("25.71")),
            // Whey 3W Minot 900g — 21g prot/dose; serving estimado 35g (blend 3W)
            build("Whey 3W Minot 900g",            "ABSOLUT_NUTRITION", "Absolut Nutrition",  900, bd(35), bd(21), new BigDecimal("25.71")),
            // Bluster Whey 100% Power 900g — 20g prot/dose; Bluster brand, sold in Absolut store
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

    private static BigDecimal bd(int value) {
        return BigDecimal.valueOf(value);
    }
}
