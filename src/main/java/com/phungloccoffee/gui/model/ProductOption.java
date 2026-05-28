package com.phungloccoffee.gui.model;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

public class ProductOption {
    private static final Set<String> DRINK_CATEGORIES = Set.of(
            "c\u00e0 ph\u00ea",
            "tr\u00e0 s\u1eefa",
            "tr\u00e0"
    );

    private final String productId;
    private final String productName;
    private final String category;
    private final BigDecimal basePrice;
    private final ProductStatus status;

    public ProductOption(String productId, String productName, String category, BigDecimal basePrice, ProductStatus status) {
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.basePrice = basePrice;
        this.status = status;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public boolean isDrink() {
        return category != null && DRINK_CATEGORIES.contains(category.trim().toLowerCase(Locale.ROOT));
    }

    public boolean isToppingCategory() {
        String value = category == null ? "" : category.trim();
        return "Topping".equalsIgnoreCase(value) || "TOPPING".equalsIgnoreCase(value);
    }

    public enum ProductStatus {
        AVAILABLE("C\u00f2n ph\u1ee5c v\u1ee5"),
        OUT_OF_STOCK("H\u1ebft nguy\u00ean li\u1ec7u"),
        PAUSED("T\u1ea1m ng\u01b0ng");

        private final String label;

        ProductStatus(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public boolean isAvailable() {
            return this == AVAILABLE;
        }
    }
}
