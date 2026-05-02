package com.devlil0.whey_promotion_bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.devlil0.whey_promotion_bot.dto.ProductOfferResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class ProductOfferMapper {

    public List<ProductOfferResponse> fromGrowthCategory(JsonNode response) {
        List<ProductOfferResponse> offers = new ArrayList<>();
        JsonNode products = response.path("conteudo").path("produtos");

        if (!products.isArray()) {
            products = response.path("produtos");
        }

        if (!products.isArray()) return offers;

        for (JsonNode product : products) {
            String name = JsonHelper.text(product, "nome");
            if (!ProductFilter.isWheyMainRankingCandidate(name)) continue;

            JsonNode prices = product.path("precos");
            String imageUrl = firstGrowthImage(product);
            String brand = product.path("marca").path("nome").asText("Growth Supplements");

            offers.add(new ProductOfferResponse(
                    "GROWTH",
                    asString(product.path("id")),
                    null,
                    JsonHelper.text(product, "sku"),
                    name,
                    brand,
                    "Proteína/Whey",
                    decimalFlexible(prices, "por"),
                    decimalFlexible(prices, "vista"),
                    decimalFlexible(prices, "de"),
                    isGrowthAvailable(product),
                    JsonHelper.integer(product, "estoque"),
                    extractWeightInGrams(name),
                    JsonHelper.text(product, "url"),
                    imageUrl,
                    "GROWTH_CATEGORY"
            ));
        }

        return offers;
    }

    public List<ProductOfferResponse> fromGrowthShowcase(JsonNode response) {
        List<ProductOfferResponse> offers = new ArrayList<>();
        JsonNode products = response.path("produtos");
        if (!products.isArray()) return offers;

        for (JsonNode product : products) {
            String name = JsonHelper.text(product, "nome");
            if (!ProductFilter.isWheyMainRankingCandidate(name)) continue;

            JsonNode prices = product.path("precos");
            String imageUrl = firstGrowthImage(product);
            String brand = product.path("marca").path("nome").asText("Growth Supplements");

            offers.add(new ProductOfferResponse(
                    "GROWTH",
                    asString(product.path("id")),
                    null,
                    JsonHelper.text(product, "sku"),
                    name,
                    brand,
                    "Whey Protein",
                    decimalFlexible(prices, "por"),
                    decimalFlexible(prices, "vista"),
                    decimalFlexible(prices, "de"),
                    isGrowthAvailable(product),
                    JsonHelper.integer(product, "estoque"),
                    extractWeightInGrams(name),
                    JsonHelper.text(product, "url"),
                    imageUrl,
                    "GROWTH_SHOWCASE"
            ));
        }

        return offers;
    }

    public List<ProductOfferResponse> fromDarkLab(JsonNode response) {
        List<ProductOfferResponse> offers = new ArrayList<>();
        JsonNode products = response.path("products");
        if (!products.isArray()) return offers;

        for (JsonNode product : products) {
            String title = JsonHelper.text(product, "title");
            String type = JsonHelper.text(product, "product_type");
            String searchable = (title + " " + type).trim();
            if (!ProductFilter.isWheyMainRankingCandidate(searchable)) continue;

            JsonNode selectedVariant = cheapestAvailableVariant(product.path("variants"));
            if (selectedVariant == null || selectedVariant.isMissingNode()) continue;

            String handle = JsonHelper.text(product, "handle");
            String imageUrl = firstDarkLabImage(product);

            offers.add(new ProductOfferResponse(
                    "DARK_LAB",
                    asString(product.path("id")),
                    asString(selectedVariant.path("id")),
                    JsonHelper.text(selectedVariant, "sku"),
                    title,
                    JsonHelper.text(product, "vendor"),
                    type,
                    JsonHelper.decimalFromText(JsonHelper.text(selectedVariant, "price")),
                    null,
                    JsonHelper.decimalFromText(JsonHelper.text(selectedVariant, "compare_at_price")),
                    selectedVariant.path("available").asBoolean(false),
                    null,
                    selectedVariant.path("grams").isNumber() ? selectedVariant.path("grams").asInt() : extractWeightInGrams(title),
                    handle != null ? "https://darklabsuplementos.com.br/products/" + handle : null,
                    imageUrl,
                    "SHOPIFY_PRODUCTS_JSON"
            ));
        }

        return offers;
    }

    public List<ProductOfferResponse> fromProfitLabs(JsonNode response) {
        List<ProductOfferResponse> offers = new ArrayList<>();
        JsonNode products = response.path("Products");
        if (!products.isArray()) return offers;

        for (JsonNode wrapper : products) {
            JsonNode product = wrapper.path("Product");
            String name = JsonHelper.text(product, "name");
            String slug = JsonHelper.text(product, "slug");
            String searchable = (name + " " + slug).trim();
            if (!ProductFilter.isWheyMainRankingCandidate(searchable)) continue;

            BigDecimal cashPrice = firstPaymentValue(product.path("payment_option_details"));
            String imageUrl = null;
            JsonNode images = product.path("ProductImage");
            if (images.isArray() && images.size() > 0) {
                imageUrl = JsonHelper.text(images.get(0), "https");
            }

            String productUrl = product.path("url").path("https").asText(null);
            String availableForPurchase = JsonHelper.text(product, "available_for_purchase");
            String available = JsonHelper.text(product, "available");

            offers.add(new ProductOfferResponse(
                    "PROFIT_LABS",
                    JsonHelper.text(product, "id"),
                    null,
                    null,
                    name,
                    JsonHelper.text(product, "brand"),
                    JsonHelper.text(product, "category_id"),
                    JsonHelper.decimalFromText(JsonHelper.text(product, "price")),
                    cashPrice,
                    null,
                    "1".equals(availableForPurchase) || "1".equals(available),
                    JsonHelper.integer(product, "stock"),
                    extractWeightInGrams(name),
                    productUrl,
                    imageUrl,
                    "TRAY_WEB_API_PRODUCTS"
            ));
        }

        return offers;
    }

    public List<ProductOfferResponse> fromMercadoLivre(JsonNode response) {
        List<ProductOfferResponse> offers = new ArrayList<>();
        JsonNode results = response.path("results");
        if (!results.isArray()) return offers;

        for (JsonNode item : results) {
            if (!"new".equals(item.path("condition").asText())) continue;

            String title = JsonHelper.text(item, "title");
            if (!ProductFilter.isWheyMainRankingCandidate(title)) continue;

            int qty = item.path("available_quantity").asInt(0);
            if (qty <= 0) continue;

            BigDecimal price = item.path("price").isNumber()
                    ? item.path("price").decimalValue() : null;
            if (price == null || price.compareTo(MIN_ML_PRICE) < 0) continue;

            String powerSeller = item.path("seller").path("power_seller_status").asText(null);
            if (powerSeller == null || powerSeller.isBlank() || "null".equalsIgnoreCase(powerSeller)) continue;

            String brand = mlAttribute(item, "BRAND");
            if (brand == null || brand.isBlank()) continue;
            String titleNorm = ProductFilter.normalize(title);
            if (!titleNorm.contains(ProductFilter.normalize(brand))) continue;

            String weightAttr = mlAttribute(item, "NET_WEIGHT");
            Integer weightGrams = parseMlWeightGrams(weightAttr);
            if (weightGrams == null) weightGrams = extractWeightInGrams(title);

            BigDecimal oldPrice = item.path("original_price").isNumber()
                    ? item.path("original_price").decimalValue() : null;

            offers.add(new ProductOfferResponse(
                    "MERCADO_LIVRE",
                    JsonHelper.text(item, "id"),
                    null,
                    null,
                    title,
                    brand,
                    "Whey Protein",
                    price,
                    null,
                    oldPrice,
                    true,
                    qty,
                    weightGrams,
                    item.path("permalink").asText(null),
                    item.path("thumbnail").asText(null),
                    "ML_SEARCH_API"
            ));
        }

        return offers;
    }

    private static final BigDecimal MIN_ML_PRICE = new BigDecimal("50");

    private String mlAttribute(JsonNode item, String attributeId) {
        JsonNode attributes = item.path("attributes");
        if (!attributes.isArray()) return null;
        for (JsonNode attr : attributes) {
            if (attributeId.equals(attr.path("id").asText())) {
                String val = attr.path("value_name").asText(null);
                return (val == null || val.isBlank()) ? null : val;
            }
        }
        return null;
    }

    private Integer parseMlWeightGrams(String weightStr) {
        if (weightStr == null) return null;
        String s = weightStr.toLowerCase().replace(",", ".").trim();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d+(?:\\.\\d+)?)\\s*(kg|g)")
                .matcher(s);
        if (!m.find()) return null;
        double value = Double.parseDouble(m.group(1));
        return m.group(2).equals("kg") ? (int) Math.round(value * 1000) : (int) Math.round(value);
    }

    private JsonNode cheapestAvailableVariant(JsonNode variants) {
        if (!variants.isArray()) return null;

        List<JsonNode> availableVariants = new ArrayList<>();
        variants.forEach(variant -> {
            if (variant.path("available").asBoolean(false)) {
                availableVariants.add(variant);
            }
        });

        if (availableVariants.isEmpty()) {
            variants.forEach(availableVariants::add);
        }

        Optional<JsonNode> cheapest = availableVariants.stream()
                .min(Comparator.comparing(variant -> {
                    BigDecimal price = JsonHelper.decimalFromText(JsonHelper.text(variant, "price"));
                    return price != null ? price : BigDecimal.valueOf(Long.MAX_VALUE);
                }));

        return cheapest.orElse(null);
    }

    private BigDecimal firstPaymentValue(JsonNode paymentDetails) {
        if (!paymentDetails.isArray() || paymentDetails.size() == 0) return null;
        return JsonHelper.decimalFromText(JsonHelper.text(paymentDetails.get(0), "value"));
    }

    private String firstDarkLabImage(JsonNode product) {
        JsonNode images = product.path("images");
        if (images.isArray() && images.size() > 0) {
            JsonNode first = images.get(0);
            if (first.isTextual()) return first.asText();
            if (first.has("src")) return first.path("src").asText(null);
        }
        JsonNode image = product.path("image");
        if (image.isObject() && image.has("src")) return image.path("src").asText(null);
        return null;
    }

    private String firstGrowthImage(JsonNode product) {
        JsonNode images = product.path("midias").path("imagens");
        if (images.isArray() && images.size() > 0) {
            JsonNode files = images.get(0).path("arquivos");
            if (files.hasNonNull("medium")) return files.path("medium").asText();
            if (files.hasNonNull("big")) return files.path("big").asText();
            if (files.hasNonNull("zoom")) return files.path("zoom").asText();
            if (files.hasNonNull("small")) return files.path("small").asText();
        }
        return null;
    }

    private boolean isGrowthAvailable(JsonNode product) {
        String status = JsonHelper.text(product, "status");
        Integer stock = JsonHelper.integer(product, "estoque");
        return "disponivel".equalsIgnoreCase(status) && stock != null && stock > 0;
    }

    private BigDecimal decimalFlexible(JsonNode node, String field) {
        if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) return null;
        if (node.path(field).isNumber()) return node.path(field).decimalValue();
        return JsonHelper.decimalFromText(node.path(field).asText());
    }

    private String asString(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return null;
        return node.asText();
    }

    private Integer extractWeightInGrams(String productName) {
        if (productName == null) return null;

        String normalized = productName.toLowerCase()
                .replace("1,8kg", "1.8kg")
                .replace("1,814kg", "1.814kg")
                .replace("1,814 kg", "1.814kg");

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(kg|kilo|g|gramas|grama)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(normalized);

        if (matcher.find()) {
            double value = Double.parseDouble(matcher.group(1).replace(",", "."));
            String unit = matcher.group(2).toLowerCase();
            if (unit.startsWith("kg") || unit.startsWith("kilo")) {
                return (int) Math.round(value * 1000);
            }
            return (int) Math.round(value);
        }

        return null;
    }
}
