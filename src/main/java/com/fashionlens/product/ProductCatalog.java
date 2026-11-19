package com.fashionlens.product;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@ApplicationScoped
public class ProductCatalog {
    private final List<Product> products = List.of(
            product(
                    "cotton-kurta",
                    "Printed cotton straight kurta",
                    "Women",
                    "Kurtas & Kurtis",
                    "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=900&q=85",
                    96,
                    "Cotton · 3/4 sleeves · Straight fit",
                    offer("Myntra", 899),
                    offer("Amazon", 1049),
                    offer("Meesho", 649)),
            product(
                    "indigo-set",
                    "Indigo hand-block kurta set",
                    "Women",
                    "Kurtas & Kurtis",
                    "https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?auto=format&fit=crop&w=900&q=85",
                    92,
                    "Rayon · Straight fit · 2-piece set",
                    offer("Myntra", 1499),
                    offer("Amazon", 1299),
                    offer("Meesho", 999)),
            product(
                    "floral-kurta",
                    "Floral everyday A-line kurta",
                    "Women",
                    "Kurtas & Kurtis",
                    "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=900&q=85",
                    89,
                    "Viscose · A-line · Calf length",
                    offer("Myntra", 1199),
                    offer("Amazon", 949),
                    offer("Meesho", 799)),
            product(
                    "festive-kurta",
                    "Embroidered festive kurta set",
                    "Women",
                    "Kurtas & Kurtis",
                    "https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?auto=format&fit=crop&w=900&q=85",
                    84,
                    "Chanderi blend · Embroidered · 3-piece set",
                    offer("Myntra", 2299),
                    offer("Amazon", 1999),
                    offer("Meesho", 1749)),
            product(
                    "printed-midi-dress",
                    "Printed everyday midi dress",
                    "Women",
                    "Dresses",
                    "https://images.unsplash.com/photo-1539109136881-3be0616acf4b?auto=format&fit=crop&w=900&q=85",
                    91,
                    "Cotton blend · Midi length · Relaxed fit",
                    offer("Myntra", 1399),
                    offer("Amazon", 1249),
                    offer("Meesho", 899)),
            product(
                    "woven-saree",
                    "Woven occasion-ready saree",
                    "Women",
                    "Sarees",
                    "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=900&q=85",
                    88,
                    "Woven blend · Contrast border · Unstitched",
                    offer("Myntra", 1899),
                    offer("Amazon", 1699),
                    offer("Meesho", 1299)),
            product(
                    "casual-coord",
                    "Easy-breezy printed co-ord set",
                    "Women",
                    "Co-ord Sets",
                    "https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=900&q=85",
                    86,
                    "Viscose · Matching top and trousers",
                    offer("Myntra", 1599),
                    offer("Amazon", 1399),
                    offer("Meesho", 1099)),
            product(
                    "women-embroidered-ethnic-set",
                    "Embroidered festive ethnic set",
                    "Women",
                    "Ethnic Sets",
                    "https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?auto=format&fit=crop&w=900&q=85",
                    90,
                    "Cotton blend · Kurta, trousers and dupatta",
                    offer("Myntra", 1899),
                    offer("Amazon", 1699),
                    offer("Meesho", 1299)),
            product(
                    "women-cotton-top",
                    "Everyday striped cotton top",
                    "Women",
                    "Tops & T-shirts",
                    "https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=900&q=85",
                    89,
                    "Cotton · Short sleeves · Relaxed fit",
                    offer("Myntra", 699),
                    offer("Amazon", 799),
                    offer("Meesho", 449)),
            product(
                    "women-straight-jeans",
                    "High-rise straight-fit jeans",
                    "Women",
                    "Jeans & Trousers",
                    "https://images.unsplash.com/photo-1541099649105-f69ad21f3246?auto=format&fit=crop&w=900&q=85",
                    87,
                    "Stretch denim · High rise · Straight leg",
                    offer("Myntra", 1299),
                    offer("Amazon", 1199),
                    offer("Meesho", 899)),
            product(
                    "women-pleated-skirt",
                    "Easy pleated midi skirt",
                    "Women",
                    "Skirts",
                    "https://images.unsplash.com/photo-1583496661160-fb5886a0aaaa?auto=format&fit=crop&w=900&q=85",
                    85,
                    "Flowy fabric · Midi length · Elastic waist",
                    offer("Myntra", 1099),
                    offer("Amazon", 949),
                    offer("Meesho", 699)),
            product(
                    "women-everyday-sandals",
                    "Everyday comfortable sandals",
                    "Women",
                    "Footwear",
                    "https://images.unsplash.com/photo-1543163521-1bf539c55dd2?auto=format&fit=crop&w=900&q=85",
                    88,
                    "Faux leather · Cushioned sole · Everyday wear",
                    offer("Myntra", 999),
                    offer("Amazon", 849),
                    offer("Meesho", 599)),
            product(
                    "women-crossbody-bag",
                    "Everyday mini crossbody bag",
                    "Women",
                    "Bags & Accessories",
                    "https://images.unsplash.com/photo-1584917865442-de89df76afd3?auto=format&fit=crop&w=900&q=85",
                    86,
                    "Vegan leather · Adjustable strap · Zip closure",
                    offer("Myntra", 1199),
                    offer("Amazon", 999),
                    offer("Meesho", 649)),
            product(
                    "mens-cotton-tee",
                    "Classic cotton crew-neck T-shirt",
                    "Men",
                    "T-shirts",
                    "https://images.unsplash.com/photo-1523381210434-271e8be1f52b?auto=format&fit=crop&w=900&q=85",
                    92,
                    "Pure cotton · Crew neck · Regular fit",
                    offer("Myntra", 599),
                    offer("Amazon", 499),
                    offer("Meesho", 349)),
            product(
                    "mens-slim-jeans",
                    "Classic slim-fit denim jeans",
                    "Men",
                    "Jeans",
                    "https://images.unsplash.com/photo-1542272604-787c3835535d?auto=format&fit=crop&w=900&q=85",
                    90,
                    "Stretch denim · Mid rise · Slim fit",
                    offer("Myntra", 1499),
                    offer("Amazon", 1299),
                    offer("Meesho", 999)),
            product(
                    "mens-chino-trousers",
                    "Everyday straight-fit chino trousers",
                    "Men",
                    "Trousers",
                    "https://images.unsplash.com/photo-1473966968600-fa801b869a1a?auto=format&fit=crop&w=900&q=85",
                    87,
                    "Cotton stretch · Mid rise · Straight fit",
                    offer("Myntra", 1199),
                    offer("Amazon", 999),
                    offer("Meesho", 749)),
            product(
                    "mens-ethnic-kurta",
                    "Classic solid cotton festive kurta",
                    "Men",
                    "Kurtas & Ethnic Wear",
                    "https://images.unsplash.com/photo-1617137968427-85924c800a22?auto=format&fit=crop&w=900&q=85",
                    89,
                    "Cotton blend · Full sleeves · Straight fit",
                    offer("Myntra", 1299),
                    offer("Amazon", 1099),
                    offer("Meesho", 799)),
            product(
                    "mens-sneakers",
                    "Versatile everyday low-top sneakers",
                    "Men",
                    "Footwear",
                    "https://images.unsplash.com/photo-1495555961986-6d4c1ecb7be3?auto=format&fit=crop&w=900&q=85",
                    88,
                    "Lightweight · Lace-up · Cushioned footbed",
                    offer("Myntra", 1699),
                    offer("Amazon", 1499),
                    offer("Meesho", 1099)),
            product(
                    "mens-wallet",
                    "Everyday slim-fold wallet",
                    "Men",
                    "Accessories",
                    "https://images.unsplash.com/photo-1627123424574-724758594e93?auto=format&fit=crop&w=900&q=85",
                    85,
                    "Vegan leather · Multiple card slots · Compact",
                    offer("Myntra", 799),
                    offer("Amazon", 699),
                    offer("Meesho", 449)),
            product(
                    "kids-girls-dress",
                    "Colourful girls everyday dress",
                    "Kids & Baby",
                    "Girls Clothing",
                    "https://images.unsplash.com/photo-1516627145497-ae6968895b74?auto=format&fit=crop&w=900&q=85",
                    91,
                    "Soft cotton · Easy fit · Ages 2–8",
                    offer("Myntra", 799),
                    offer("Amazon", 699),
                    offer("Meesho", 499)),
            product(
                    "kids-sneakers",
                    "Lightweight kids everyday sneakers",
                    "Kids & Baby",
                    "Kids Footwear",
                    "https://images.unsplash.com/photo-1514989940723-e8e51635b782?auto=format&fit=crop&w=900&q=85",
                    88,
                    "Lightweight · Easy fastening · Growing feet fit",
                    offer("Myntra", 999),
                    offer("Amazon", 849),
                    offer("Meesho", 599)),
            product(
                    "baby-soft-blanket",
                    "Soft cotton baby blanket",
                    "Kids & Baby",
                    "Baby Accessories",
                    "https://images.unsplash.com/photo-1519689680058-324335c77eba?auto=format&fit=crop&w=900&q=85",
                    86,
                    "Gentle cotton · Lightweight · Pram friendly",
                    offer("Myntra", 899),
                    offer("Amazon", 749),
                    offer("Meesho", 549)),
            product(
                    "mens-linen-shirt",
                    "Classic relaxed-fit linen shirt",
                    "Men",
                    "Shirts",
                    "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?auto=format&fit=crop&w=900&q=85",
                    94,
                    "Linen blend · Full sleeves · Relaxed fit",
                    offer("Myntra", 999),
                    offer("Amazon", 1199),
                    offer("Meesho", 749)),
            product(
                    "mens-casual-jacket",
                    "Everyday casual zip-up jacket",
                    "Men",
                    "Jackets",
                    "https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=900&q=85",
                    88,
                    "Cotton blend · Zip closure · Regular fit",
                    offer("Myntra", 1799),
                    offer("Amazon", 1599),
                    offer("Meesho", 1249)),
            product(
                    "kids-graphic-tee",
                    "Colourful kids graphic T-shirt",
                    "Kids & Baby",
                    "Boys Clothing",
                    "https://images.unsplash.com/photo-1519238263530-99bdd11df2ea?auto=format&fit=crop&w=900&q=85",
                    93,
                    "Soft cotton · Short sleeves · Ages 2–8",
                    offer("Myntra", 499),
                    offer("Amazon", 599),
                    offer("Meesho", 349)),
            product(
                    "baby-cotton-romper",
                    "Soft cotton everyday romper",
                    "Kids & Baby",
                    "Baby Clothing",
                    "https://images.unsplash.com/photo-1516627145497-ae6968895b74?auto=format&fit=crop&w=900&q=85",
                    90,
                    "Gentle cotton · Easy snaps · Ages 0–24 months",
                    offer("Myntra", 699),
                    offer("Amazon", 549),
                    offer("Meesho", 399)));

    public List<Product> search(String query, String audience, String category, String sort) {
        String normalizedQuery = normalize(query);
        String normalizedAudience = normalizeAudience(audience);
        String normalizedCategory = normalize(category);

        Stream<Product> results = products.stream()
                .filter(product -> normalizedAudience.isEmpty()
                        || product.audience().toLowerCase(Locale.ROOT).equals(normalizedAudience))
                .filter(product -> normalizedCategory.isEmpty()
                        || product.category().toLowerCase(Locale.ROOT).equals(normalizedCategory))
                .filter(product -> normalizedQuery.isEmpty()
                        || (product.name() + " " + product.details())
                                .toLowerCase(Locale.ROOT)
                                .contains(normalizedQuery));

        Comparator<Product> comparator = switch (sort.toLowerCase(Locale.ROOT)) {
            case "match" -> Comparator.comparingInt(Product::match).reversed();
            case "price" -> Comparator.comparingInt(ProductCatalog::lowestPrice);
            default -> throw new IllegalArgumentException("Sort must be 'match' or 'price'.");
        };

        return results.sorted(comparator).toList();
    }

    public List<Product> imageMatches(String audience) {
        return search("", audience, "", "match");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeAudience(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty() || normalized.equals("all")) {
            return "";
        }
        if (normalized.equals("kids") || normalized.equals("baby")) {
            return "kids & baby";
        }
        if (normalized.equals("women") || normalized.equals("men")
                || normalized.equals("kids & baby")) {
            return normalized;
        }
        throw new IllegalArgumentException(
                "Audience must be 'all', 'women', 'men', or 'kids'.");
    }

    private static int lowestPrice(Product product) {
        return product.offers().stream()
                .mapToInt(ProductOffer::price)
                .min()
                .orElse(Integer.MAX_VALUE);
    }

    private static Product product(
            String id,
            String name,
            String audience,
            String category,
            String image,
            int match,
            String details,
            ProductOffer... offers) {
        return new Product(id, name, audience, category, image, match, details, List.of(offers));
    }

    private static ProductOffer offer(String store, int price) {
        return new ProductOffer(store, price);
    }
}
