package com.fashionlens.product;

import java.util.List;

public record ProductOffersResponse(List<ProductOffer> offers, String message) {
}
