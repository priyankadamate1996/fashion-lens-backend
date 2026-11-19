package com.fashionlens.product;

public record ProductOffer(String store, int price, String url) {
    public ProductOffer(String store, int price) {
        this(store, price, null);
    }
}
