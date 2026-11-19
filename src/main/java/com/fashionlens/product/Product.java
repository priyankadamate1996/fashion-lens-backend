package com.fashionlens.product;

import java.util.List;

public record Product(
        String id,
        String name,
        String audience,
        String category,
        String image,
        Integer match,
        String details,
        List<ProductOffer> offers,
        String offersToken) {
    public Product(
            String id,
            String name,
            String audience,
            String category,
            String image,
            Integer match,
            String details,
            List<ProductOffer> offers) {
        this(id, name, audience, category, image, match, details, offers, null);
    }
}
