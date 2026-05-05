package com.devlil0.whey_promotion_bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.devlil0.whey_promotion_bot.dto.ProductOfferResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class ProductOfferMapper {

    // ── Growth ────────────────────────────────────────────────────────────────

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

    public List<ProductOfferResponse> fromGrowthOfertas(JsonNode response) {
        List<ProductOfferResponse> offers = new ArrayList<>();
        JsonNode products = response.path("conteudo").path("produtos");
        if (!products.isArray()) products = response.path("produtos");
        if (!products.isArray()) return offers;

        for (JsonNode product : products) {
            String name = JsonHelper.text(product, "nome");
            JsonNode prices = product.path("precos");
            String brand = product.path("marca").path("nome").asText("Growth Supplements");

            offers.add(new ProductOfferResponse(
                    "GROWTH",
                    asString(product.path("id")),
                    null,
                    JsonHelper.text(product, "sku"),
                    name,
                    brand,
                    null,
                    decimalFlexible(prices, "por"),
                    decimalFlexible(prices, "vista"),
                    decimalFlexible(prices, "de"),
                    isGrowthAvailable(product),
                    JsonHelper.integer(product, "estoque"),
                    extractWeightInGrams(name),
                    JsonHelper.text(product, "url"),
                    firstGrowthImage(product),
                    "GROWTH_OFERTAS"
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

    // ── Shopify (Dark Lab, Soldiers Nutrition) ────────────────────────────────
    // Lojas Shopify brasileiras tipicamente oferecem 5% de desconto no Pix.
    // O preço Pix não está disponível na API products.json — é calculado no checkout.

    public List<ProductOfferResponse> fromDarkLab(JsonNode response) {
        return fromShopify(response, "DARK_LAB", "https://darklabsuplementos.com.br");
    }

    public List<ProductOfferResponse> fromSoldiersNutrition(JsonNode response) {
        return fromShopify(response, "SOLDIERS_NUTRITION", "https://soldiersnutrition.com.br");
    }

    public List<ProductOfferResponse> fromSoldiersOfertaRelampago(JsonNode response) {
        List<ProductOfferResponse> offers = new ArrayList<>();
        JsonNode products = response.path("products");
        if (!products.isArray()) return offers;

        for (JsonNode product : products) {
            String title = JsonHelper.text(product, "title");
            String type = JsonHelper.text(product, "product_type");

            JsonNode selectedVariant = cheapestAvailableVariant(product.path("variants"));
            if (selectedVariant == null || selectedVariant.isMissingNode()) continue;

            String handle = JsonHelper.text(product, "handle");
            BigDecimal price = JsonHelper.decimalFromText(JsonHelper.text(selectedVariant, "price"));
            BigDecimal compareAt = JsonHelper.decimalFromText(JsonHelper.text(selectedVariant, "compare_at_price"));
            BigDecimal pixPrice = price != null
                    ? price.multiply(new BigDecimal("0.95")).setScale(2, RoundingMode.HALF_UP)
                    : null;
            BigDecimal oldPrice = (compareAt != null && price != null && compareAt.compareTo(price) > 0)
                    ? compareAt : null;

            offers.add(new ProductOfferResponse(
                    "SOLDIERS_NUTRITION",
                    asString(product.path("id")),
                    asString(selectedVariant.path("id")),
                    JsonHelper.text(selectedVariant, "sku"),
                    title,
                    JsonHelper.text(product, "vendor"),
                    type,
                    price,
                    pixPrice,
                    oldPrice,
                    selectedVariant.path("available").asBoolean(false),
                    null,
                    selectedVariant.path("grams").isNumber()
                            ? selectedVariant.path("grams").asInt()
                            : extractWeightInGrams(title),
                    handle != null ? "https://soldiersnutrition.com.br/products/" + handle : null,
                    firstShopifyImage(product),
                    "SHOPIFY_OFERTA_RELAMPAGO"
            ));
        }

        return offers;
    }

    private List<ProductOfferResponse> fromShopify(JsonNode response, String storeId, String baseUrl) {
        List<ProductOfferResponse> offers = new ArrayList<>();
        JsonNode products = response.path("products");
        if (!products.isArray()) return offers;

        for (JsonNode product : products) {
            String title = JsonHelper.text(product, "title");
            String type = JsonHelper.text(product, "product_type");
            if (!ProductFilter.isWheyMainRankingCandidate((title + " " + type).trim())) continue;

            JsonNode selectedVariant = cheapestAvailableVariant(product.path("variants"));
            if (selectedVariant == null || selectedVariant.isMissingNode()) continue;

            String handle = JsonHelper.text(product, "handle");
            BigDecimal price = JsonHelper.decimalFromText(JsonHelper.text(selectedVariant, "price"));
            BigDecimal compareAt = JsonHelper.decimalFromText(JsonHelper.text(selectedVariant, "compare_at_price"));
            BigDecimal pixPrice = price != null
                    ? price.multiply(new BigDecimal("0.95")).setScale(2, RoundingMode.HALF_UP)
                    : null;
            BigDecimal oldPrice = (compareAt != null && price != null && compareAt.compareTo(price) > 0)
                    ? compareAt : null;

            offers.add(new ProductOfferResponse(
                    storeId,
                    asString(product.path("id")),
                    asString(selectedVariant.path("id")),
                    JsonHelper.text(selectedVariant, "sku"),
                    title,
                    JsonHelper.text(product, "vendor"),
                    type,
                    price,
                    pixPrice,
                    oldPrice,
                    selectedVariant.path("available").asBoolean(false),
                    null,
                    selectedVariant.path("grams").isNumber()
                            ? selectedVariant.path("grams").asInt()
                            : extractWeightInGrams(title),
                    handle != null ? baseUrl + "/products/" + handle : null,
                    firstShopifyImage(product),
                    "SHOPIFY_PRODUCTS_JSON"
            ));
        }

        return offers;
    }

    // ── Tray (ProFit Labs, Nutrata) ───────────────────────────────────────────

    public List<ProductOfferResponse> fromProfitLabs(JsonNode response) {
        List<ProductOfferResponse> offers = new ArrayList<>();
        JsonNode products = response.path("Products");
        if (!products.isArray()) return offers;

        for (JsonNode wrapper : products) {
            JsonNode product = wrapper.path("Product");
            String name = JsonHelper.text(product, "name");
            String slug = JsonHelper.text(product, "slug");
            if (!ProductFilter.isWheyMainRankingCandidate((name + " " + slug).trim())) continue;

            BigDecimal cashPrice = firstAVistaValue(product.path("payment_option_details"));

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

    public List<ProductOfferResponse> fromProfitLabsPromocoes(JsonNode response) {
        List<ProductOfferResponse> offers = new ArrayList<>();
        JsonNode products = response.path("Products");
        if (!products.isArray()) return offers;

        for (JsonNode wrapper : products) {
            JsonNode product = wrapper.path("Product");
            String name = JsonHelper.text(product, "name");

            BigDecimal cashPrice = firstAVistaValue(product.path("payment_option_details"));

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

    public List<ProductOfferResponse> fromNutrata(JsonNode response) {
        List<ProductOfferResponse> offers = new ArrayList<>();
        JsonNode products = response.path("Products");
        if (!products.isArray()) return offers;

        for (JsonNode wrapper : products) {
            JsonNode product = wrapper.path("Product");
            String name = JsonHelper.text(product, "name");
            if (!ProductFilter.isWheyMainRankingCandidate(name)) continue;

            BigDecimal regularPrice = JsonHelper.decimalFromText(JsonHelper.text(product, "price"));
            BigDecimal promoPrice = JsonHelper.decimalFromText(JsonHelper.text(product, "promotional_price"));
            BigDecimal cashPrice = (promoPrice != null && promoPrice.compareTo(BigDecimal.ZERO) > 0)
                    ? promoPrice
                    : firstAVistaValue(product.path("payment_option_details"));

            String imageUrl = null;
            JsonNode images = product.path("ProductImage");
            if (images.isArray() && images.size() > 0) {
                imageUrl = JsonHelper.text(images.get(0), "https");
            }

            String productUrl = product.path("url").path("https").asText(null);
            String availableForPurchase = JsonHelper.text(product, "available_for_purchase");
            String available = JsonHelper.text(product, "available");

            offers.add(new ProductOfferResponse(
                    "NUTRATA",
                    JsonHelper.text(product, "id"),
                    null,
                    null,
                    name,
                    JsonHelper.text(product, "brand"),
                    null,
                    regularPrice,
                    cashPrice,
                    null,
                    "1".equals(availableForPurchase) || "1".equals(available),
                    JsonHelper.integer(product, "stock"),
                    extractWeightInGrams(name),
                    productUrl,
                    imageUrl,
                    "TRAY_WEB_API_SEARCH"
            ));
        }

        return offers;
    }

    // ── VTEX (Black Skull) ────────────────────────────────────────────────────

    public List<ProductOfferResponse> fromBlackSkull(JsonNode response) {
        return fromVtex(response, "BLACK_SKULL", "https://www.blackskullusa.com.br");
    }

    private List<ProductOfferResponse> fromVtex(JsonNode response, String storeId, String baseUrl) {
        List<ProductOfferResponse> offers = new ArrayList<>();
        if (!response.isArray()) return offers;

        for (JsonNode product : response) {
            String name = JsonHelper.text(product, "productName");
            if (!ProductFilter.isWheyMainRankingCandidate(name)) continue;

            JsonNode items = product.path("items");
            if (!items.isArray() || items.size() == 0) continue;

            JsonNode chosenItem = null;
            JsonNode chosenOffer = null;
            for (JsonNode item : items) {
                JsonNode sellers = item.path("sellers");
                if (!sellers.isArray() || sellers.size() == 0) continue;
                JsonNode offer = sellers.get(0).path("commertialOffer");
                if (offer.path("AvailableQuantity").asInt(0) > 0) {
                    chosenItem = item;
                    chosenOffer = offer;
                    break;
                }
            }
            if (chosenItem == null || chosenOffer == null) continue;

            BigDecimal price = chosenOffer.path("Price").isNumber()
                    ? chosenOffer.path("Price").decimalValue() : null;
            if (price == null) continue;

            BigDecimal listPrice = chosenOffer.path("ListPrice").isNumber()
                    ? chosenOffer.path("ListPrice").decimalValue() : null;
            BigDecimal oldPrice = (listPrice != null && listPrice.compareTo(price) > 0) ? listPrice : null;
            BigDecimal pixPrice = findPixPrice(chosenOffer.path("Installments"));

            String imageUrl = null;
            JsonNode images = chosenItem.path("images");
            if (images.isArray() && images.size() > 0) {
                imageUrl = images.get(0).path("imageUrl").asText(null);
            }

            String productUrl = product.path("link").asText(null);
            if (productUrl == null || productUrl.isBlank()) {
                String linkText = product.path("linkText").asText(null);
                if (linkText != null) productUrl = baseUrl + "/" + linkText + "/p";
            }

            String sku = null;
            JsonNode refIds = chosenItem.path("referenceId");
            if (refIds.isArray() && refIds.size() > 0) {
                sku = refIds.get(0).path("Value").asText(null);
            }

            offers.add(new ProductOfferResponse(
                    storeId,
                    JsonHelper.text(product, "productId"),
                    JsonHelper.text(chosenItem, "itemId"),
                    sku,
                    name,
                    JsonHelper.text(product, "brand"),
                    "Whey Protein",
                    price,
                    pixPrice,
                    oldPrice,
                    true,
                    chosenOffer.path("AvailableQuantity").asInt(0),
                    extractWeightInGrams(name),
                    productUrl,
                    imageUrl,
                    "VTEX_CATALOG_API"
            ));
        }

        return offers;
    }

    // ── WooCommerce (Adaptogen, Absolut Nutrition) ────────────────────────────

    public List<ProductOfferResponse> fromAdaptogen(JsonNode response) {
        return fromWooCommerce(response, "ADAPTOGEN");
    }

    public List<ProductOfferResponse> fromAbsolutNutrition(JsonNode response) {
        return fromWooCommerce(response, "ABSOLUT_NUTRITION");
    }

    private List<ProductOfferResponse> fromWooCommerce(JsonNode response, String storeId) {
        List<ProductOfferResponse> offers = new ArrayList<>();
        if (!response.isArray()) return offers;

        for (JsonNode product : response) {
            String name = JsonHelper.text(product, "name");
            if (!ProductFilter.isWheyMainRankingCandidate(name)) continue;

            boolean inStock = product.path("is_in_stock").asBoolean(false);
            JsonNode prices = product.path("prices");
            // WooCommerce retorna preços em centavos (ex: 28050 = R$ 280,50)
            BigDecimal price = wooPrice(prices, "price");
            BigDecimal regularPrice = wooPrice(prices, "regular_price");
            BigDecimal oldPrice = (regularPrice != null && price != null && regularPrice.compareTo(price) > 0)
                    ? regularPrice : null;

            if (price == null) continue;

            String imageUrl = null;
            JsonNode images = product.path("images");
            if (images.isArray() && images.size() > 0) {
                imageUrl = images.get(0).path("src").asText(null);
            }

            String brand = null;
            JsonNode brands = product.path("brands");
            if (brands.isArray() && brands.size() > 0) {
                brand = brands.get(0).path("name").asText(null);
            }

            offers.add(new ProductOfferResponse(
                    storeId,
                    asString(product.path("id")),
                    null,
                    JsonHelper.text(product, "sku"),
                    name,
                    brand,
                    null,
                    price,
                    null,
                    oldPrice,
                    inStock,
                    null,
                    extractWeightInGrams(name),
                    product.path("permalink").asText(null),
                    imageUrl,
                    "WOOCOMMERCE_STORE_API"
            ));
        }

        return offers;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BigDecimal firstAVistaValue(JsonNode paymentDetails) {
        if (!paymentDetails.isArray()) return null;
        for (JsonNode detail : paymentDetails) {
            if ("1".equals(JsonHelper.text(detail, "plots"))) {
                return JsonHelper.decimalFromText(JsonHelper.text(detail, "value"));
            }
        }
        return null;
    }

    private BigDecimal findPixPrice(JsonNode installments) {
        if (!installments.isArray()) return null;
        for (JsonNode inst : installments) {
            String paymentName = inst.path("PaymentSystemName").asText("").toLowerCase();
            if (inst.path("NumberOfInstallments").asInt(0) == 1 && paymentName.contains("pix")) {
                return inst.path("Value").isNumber() ? inst.path("Value").decimalValue() : null;
            }
        }
        for (JsonNode inst : installments) {
            String paymentName = inst.path("PaymentSystemName").asText("").toLowerCase();
            if (inst.path("NumberOfInstallments").asInt(0) == 1 && paymentName.contains("boleto")) {
                return inst.path("Value").isNumber() ? inst.path("Value").decimalValue() : null;
            }
        }
        return null;
    }

    private BigDecimal wooPrice(JsonNode prices, String field) {
        if (prices == null || prices.isMissingNode()) return null;
        String val = JsonHelper.text(prices, field);
        if (val == null || val.isBlank() || "0".equals(val)) return null;
        try {
            return new BigDecimal(val).movePointLeft(2);
        } catch (NumberFormatException e) {
            return null;
        }
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

    private String firstShopifyImage(JsonNode product) {
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
            JsonNode img = images.get(0);
            JsonNode originals = img.path("arquivosOriginais");
            if (!originals.isMissingNode()) {
                if (originals.hasNonNull("zoom"))   return originals.path("zoom").asText();
                if (originals.hasNonNull("big"))    return originals.path("big").asText();
                if (originals.hasNonNull("medium")) return originals.path("medium").asText();
                if (originals.hasNonNull("small"))  return originals.path("small").asText();
            }
            JsonNode files = img.path("arquivos");
            if (files.hasNonNull("zoom"))   return files.path("zoom").asText();
            if (files.hasNonNull("big"))    return files.path("big").asText();
            if (files.hasNonNull("medium")) return files.path("medium").asText();
            if (files.hasNonNull("small"))  return files.path("small").asText();
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

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(\\d+(?:[.,]\\d+)?)\\s*(kg|kilo|g|gramas|grama)",
                java.util.regex.Pattern.CASE_INSENSITIVE);
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
