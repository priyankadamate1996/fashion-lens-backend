package com.fashionlens.product;

import java.util.List;

public record ImageSearchResponse(
        List<Product> matches,
        boolean demoData,
        String message) {
}
