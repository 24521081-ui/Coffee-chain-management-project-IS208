package com.phungloccoffee.model.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ProductCatalogModels {
    public static final String STATUS_ACTIVE = "Đang hoạt động";
    public static final String STATUS_INACTIVE = "Ngừng bán";
    public static final String DEFAULT_PRODUCT_TYPE = "THANH_PHAM";
    public static final String DEFAULT_PRODUCT_UNIT = "ML";
    public static final String RECIPE_ACTIVE = "Đang áp dụng";
    public static final String RECIPE_REVIEW = "Cần rà soát";

    private ProductCatalogModels() {
    }

    public record ProductMetadata(String note, LocalDateTime publishedAt, LocalDateTime stoppedAt) {
    }

    public record ProductRecipe(String materialCode, String materialName, BigDecimal quantity, String unit, String status) {
    }

    public record Ingredient(String code, String name, String defaultUnit) {
        @Override
        public String toString() {
            return code + " - " + name;
        }
    }

    public record RecipeDisplayRow(String productCode, String productName, String material,
                                   String quantity, String unit, String status) {
    }
}
