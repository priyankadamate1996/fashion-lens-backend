package com.fashionlens.product;

import java.util.List;

public record ProductSearchResponse(
        List<Product> products,
        boolean demoData,
        String message) {
}
