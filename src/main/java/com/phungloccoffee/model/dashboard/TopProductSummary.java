package com.phungloccoffee.model.dashboard;

import java.math.BigDecimal;

public record TopProductSummary(String productId, String productName, BigDecimal quantity) {
    public TopProductSummary {
        quantity = quantity == null ? BigDecimal.ZERO : quantity;
    }
}
