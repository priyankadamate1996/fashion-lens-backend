package com.fashionlens.provider;

import com.fashionlens.product.Product;
import com.fashionlens.product.ProductOffer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class SerpApiService {
    private static final int MAX_IMAGE_SIZE_BYTES = 500 * 1024;
    private static final int MAX_RESULTS = 24;
    private static final int MAX_VISUAL_MATCH_TITLES = 8;
    private static final int MAX_VISUAL_QUERY_TERMS = 5;
    private static final Set<String> GARMENT_TERMS = Set.of(
            "kurta", "kurti", "kurtis", "saree", "sari", "salwar", "dupatta", "lehenga",
            "blouse", "shirt", "tshirt", "top", "dress", "gown", "jumpsuit", "jacket",
            "jeans", "trousers", "palazzo", "skirt", "coat", "sweater", "cardigan", "tunic");
    private static final Set<String> VISUAL_SEARCH_TERMS = Set.of(
            "kurta", "kurti", "kurtis", "saree", "sari", "salwar", "dupatta", "lehenga",
            "blouse", "shirt", "tshirt", "top", "dress", "gown", "jumpsuit", "jacket",
            "jeans", "trousers", "palazzo", "skirt", "coat", "sweater", "cardigan",
            "tunic", "ethnic", "western", "embroidered", "embroidery", "floral", "printed",
            "print", "block", "solid", "striped", "strip", "checked", "check", "geometric",
            "lace", "sequin", "mirror", "cotton", "silk", "linen", "rayon", "chiffon",
            "georgette", "velvet", "denim", "blue", "navy", "white", "cream", "ivory",
            "black", "red", "maroon", "green", "yellow", "mustard", "pink", "purple",
            "orange", "brown", "beige", "teal", "multicolor", "straight", "aline", "anarkali",
            "short", "long", "sleeve", "sleeves", "neck", "round", "vneck");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @ConfigProperty(name = "fashion-lens.serpapi.api-key", defaultValue = "not-configured")
    String apiKey;

    @ConfigProperty(name = "fashion-lens.serpapi.base-url", defaultValue = "https://serpapi.com")
    String baseUrl;

    @jakarta.inject.Inject
    ObjectMapper objectMapper;

    public boolean isConfigured() {
        return apiKey != null
                && !apiKey.isBlank()
                && !"not-configured".equalsIgnoreCase(apiKey.trim());
    }

    public List<Product> searchProducts(
            String query,
            String audience,
            String category,
            String sort) {
        String shoppingQuery = buildShoppingQuery(query, audience, category);
        JsonNode response = getSearchResponse("google_shopping", shoppingQuery, null);
        return mapShoppingResults(response.path("shopping_results"), audience, category, sort);
    }

    public List<ProductOffer> searchOtherOffers(String pageToken) {
        if (pageToken == null
                || pageToken.isBlank()
                || pageToken.length() > 5000
                || pageToken.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("A valid product offers token is required.");
        }

        String path = "/search.json?engine=google_immersive_product"
                + "&page_token=" + encode(pageToken)
                + "&more_stores=true";
        JsonNode stores = getJson(path, "other seller search").path("product_results").path("stores");
        if (!stores.isArray()) {
            return List.of();
        }

        List<ProductOffer> offers = new ArrayList<>();
        for (JsonNode store : stores) {
            String name = store.path("name").asText("").trim();
            String url = store.path("link").asText("").trim();
            JsonNode price = store.path("extracted_price");
            if (!name.isBlank() && !url.isBlank() && price.isNumber() && price.asInt() > 0) {
                offers.add(new ProductOffer(name, price.asInt(), url));
            }
        }
        return offers.stream()
                .sorted(Comparator.comparingInt(ProductOffer::price))
                .limit(13)
                .toList();
    }

    public List<Product> searchByImage(
            Path imagePath,
            long imageSize,
            String contentType,
            String fileName,
            String audience,
            String category) {
        if (imageSize > MAX_IMAGE_SIZE_BYTES) {
            throw new ProviderException("SerpApi image search accepts images up to 500 KB.");
        }

        String imageId = uploadImage(imagePath, contentType, fileName);
        JsonNode visualMatches = getLensResults(imageId).path("visual_matches");
        String visualQuery = buildVisualShoppingQuery(visualMatches);
        if (visualQuery.isBlank()) {
            return List.of();
        }

        String shoppingQuery = buildShoppingQuery(visualQuery, audience, category);
        JsonNode response = getSearchResponse("google_shopping", shoppingQuery, null);
        return mapShoppingResults(response.path("shopping_results"), audience, category, "match");
    }

    private String uploadImage(Path imagePath, String contentType, String fileName) {
        String boundary = "FashionLens-" + UUID.randomUUID();
        try {
            byte[] image = Files.readAllBytes(imagePath);
            if (image.length == 0 || image.length > MAX_IMAGE_SIZE_BYTES) {
                throw new ProviderException("Image must be between 1 byte and 500 KB.");
            }

            String safeName = fileName == null || fileName.isBlank()
                    ? "fashion-image"
                    : fileName.replaceAll("[^A-Za-z0-9._-]", "_");
            byte[] body = multipartBody(boundary, safeName, contentType, image, apiKey);
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/image"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = parseResponse(response, "image upload");
            String imageId = json.path("image_id").asText();
            if (imageId.isBlank()) {
                throw new ProviderException("The image search provider did not return an image ID.");
            }
            return imageId;
        } catch (IOException exception) {
            throw new ProviderException("Could not read or upload the selected image.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ProviderException("Image search was interrupted.");
        }
    }

    private JsonNode getLensResults(String imageId) {
        String path = "/search.json?engine=google_lens"
                + "&image_id=" + encode(imageId)
                + "&country=in&hl=en&type=visual_matches";
        return getJson(path, "visual image search");
    }

    private JsonNode getSearchResponse(String engine, String query, String pageToken) {
        String path = "/search.json?engine=" + encode(engine)
                + "&q=" + encode(query)
                + "&gl=in&google_domain=google.co.in&hl=en&device=mobile";
        if (pageToken != null && !pageToken.isBlank()) {
            path += "&page_token=" + encode(pageToken);
        }
        return getJson(path, engine + " search");
    }

    private JsonNode getJson(String path, String operation) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            return parseResponse(
                    httpClient.send(withApiKey(request), HttpResponse.BodyHandlers.ofString()),
                    operation);
        } catch (IOException exception) {
            throw new ProviderException("Could not connect to the product search provider.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ProviderException("Product search was interrupted.");
        }
    }

    private HttpRequest withApiKey(HttpRequest request) {
        URI uri = request.uri();
        String separator = uri.getRawQuery() == null ? "?" : "&";
        URI authenticatedUri = URI.create(uri + separator + "api_key=" + encode(apiKey));
        return HttpRequest.newBuilder(authenticatedUri)
                .timeout(Duration.ofSeconds(30))
                .headers(request.headers().map().entrySet().stream()
                        .flatMap(entry -> entry.getValue().stream()
                                .flatMap(value -> java.util.stream.Stream.of(entry.getKey(), value)))
                        .toArray(String[]::new))
                .method(request.method(), request.bodyPublisher().orElse(
                        HttpRequest.BodyPublishers.noBody()))
                .build();
    }

    private JsonNode parseResponse(HttpResponse<String> response, String operation) {
        JsonNode json;
        try {
            json = objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new ProviderException("The product search provider returned an invalid response.");
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw providerException(json, response.statusCode(), operation);
        }
        if (json.hasNonNull("error")) {
            throw providerException(json, response.statusCode(), operation);
        }
        return json;
    }

    ProviderException providerException(JsonNode response, int statusCode, String operation) {
        String providerMessage = response.path("error").asText("").trim();
        if (providerMessage.isBlank()) {
            return new ProviderException("The product search provider rejected the " + operation
                    + " request (HTTP " + statusCode + ").");
        }

        String safeMessage = providerMessage
                .replace(apiKey, "[redacted]")
                .replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ");
        if (safeMessage.length() > 200) {
            safeMessage = safeMessage.substring(0, 200);
        }
        return new ProviderException("The product search provider rejected the " + operation
                + " request (HTTP " + statusCode + "): " + safeMessage);
    }

    static List<Product> mapShoppingResults(
            JsonNode results,
            String audience,
            String category,
            String sort) {
        List<Product> products = new ArrayList<>();
        if (!results.isArray()) {
            return products;
        }

        for (JsonNode result : results) {
            String title = result.path("title").asText("").trim();
            String source = result.path("source").asText("").trim();
            String image = result.path("thumbnail").asText("");
            String productUrl = firstHttpUrl(
                    result.path("link").asText(""),
                    result.path("product_link").asText(""));
            String offersToken = result.path("immersive_product_page_token").asText("");
            JsonNode extractedPrice = result.path("extracted_price");
            if (title.isBlank()
                    || isExplicitlyForOtherAudience(title, audience)
                    || source.isBlank()
                    || image.isBlank()
                    || !extractedPrice.isNumber()) {
                continue;
            }

            int price = extractedPrice.asInt(-1);
            if (price <= 0) {
                continue;
            }

            String details = result.path("snippet").asText("");
            if (details.isBlank()) {
                details = "Listed by " + source + " · Confirm current price with the seller";
            }
            String id = result.path("product_id").asText("");
            if (id.isBlank()) {
                id = Integer.toHexString((title + source).hashCode());
            }
            products.add(new Product(
                    "shopping-" + id,
                    title,
                    normalizeAudience(audience),
                    category == null || category.isBlank() ? "Fashion" : category,
                    image,
                    null,
                    details,
                    List.of(new ProductOffer(source, price, productUrl)),
                    offersToken.isBlank() ? null : offersToken));
            if (products.size() >= MAX_RESULTS) {
                break;
            }
        }

        if ("price".equalsIgnoreCase(sort)) {
            products.sort(Comparator.comparingInt(product -> product.offers().get(0).price()));
        }
        return List.copyOf(products);
    }

    private static boolean isExplicitlyForOtherAudience(String title, String audience) {
        if ("women".equalsIgnoreCase(audience)) {
            return title.toLowerCase(Locale.ROOT)
                    .matches(".*(^|[^a-z0-9])(men|mens|man|male|boys?)(?=$|[^a-z0-9]).*");
        }
        if ("men".equalsIgnoreCase(audience)) {
            return title.toLowerCase(Locale.ROOT)
                    .matches(".*(^|[^a-z0-9])(women|womens|woman|female|girls?)(?=$|[^a-z0-9]).*");
        }
        return false;
    }

    private static String firstHttpUrl(String... candidates) {
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            try {
                URI uri = URI.create(candidate.trim());
                String scheme = uri.getScheme();
                if (uri.getHost() != null
                        && ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))) {
                    return uri.toString();
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed provider URLs and leave the product without a direct link.
            }
        }
        return null;
    }

    static String buildVisualShoppingQuery(JsonNode visualMatches) {
        if (!visualMatches.isArray()) {
            return "";
        }

        Map<String, Integer> termCounts = new LinkedHashMap<>();
        int matchCount = 0;
        for (JsonNode match : visualMatches) {
            String title = match.path("title").asText("").trim();
            if (title.isBlank()) {
                continue;
            }
            matchCount++;
            for (String token : title.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
                if (VISUAL_SEARCH_TERMS.contains(token)) {
                    termCounts.merge(token, 1, Integer::sum);
                }
            }
            if (matchCount >= MAX_VISUAL_MATCH_TITLES) {
                break;
            }
        }

        if (termCounts.isEmpty()) {
            return "";
        }

        List<String> terms = new ArrayList<>(termCounts.keySet());
        terms.sort(Comparator.comparingInt((String term) -> termCounts.get(term)).reversed());
        Set<String> selectedTerms = new LinkedHashSet<>();
        terms.stream()
                .filter(GARMENT_TERMS::contains)
                .findFirst()
                .ifPresent(selectedTerms::add);
        terms.stream()
                .filter(term -> !GARMENT_TERMS.contains(term))
                .limit(MAX_VISUAL_QUERY_TERMS - selectedTerms.size())
                .forEach(selectedTerms::add);
        return String.join(" ", selectedTerms);
    }

    private static String buildShoppingQuery(String query, String audience, String category) {
        List<String> terms = new ArrayList<>();
        addTerm(terms, query);
        addTerm(terms, category);
        if (audience != null && !audience.isBlank() && !"all".equalsIgnoreCase(audience)) {
            if ("women".equalsIgnoreCase(audience)) {
                terms.add("women's clothing");
            } else if ("men".equalsIgnoreCase(audience)) {
                terms.add("men's clothing");
            } else if ("kids".equalsIgnoreCase(audience)
                    || "baby".equalsIgnoreCase(audience)
                    || "kids & baby".equalsIgnoreCase(audience)) {
                terms.add("kids clothing");
            } else {
                terms.add(audience.trim());
            }
        }
        if (terms.isEmpty()) {
            terms.add("fashion clothing");
        }
        return String.join(" ", terms);
    }

    private static void addTerm(List<String> terms, String term) {
        if (term != null && !term.isBlank() && !"all styles".equalsIgnoreCase(term.trim())) {
            terms.add(term.trim());
        }
    }

    private static String normalizeAudience(String audience) {
        if (audience == null || audience.isBlank()) {
            return "All";
        }
        if ("kids".equalsIgnoreCase(audience) || "baby".equalsIgnoreCase(audience)) {
            return "Kids & Baby";
        }
        if ("women".equalsIgnoreCase(audience)) {
            return "Women";
        }
        if ("men".equalsIgnoreCase(audience)) {
            return "Men";
        }
        return audience;
    }

    private static byte[] multipartBody(
            String boundary,
            String fileName,
            String contentType,
            byte[] image,
            String apiKey) {
        String apiKeyPart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"api_key\"\r\n\r\n"
                + apiKey + "\r\n";
        String header = apiKeyPart + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"image\"; filename=\""
                + fileName + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        byte[] prefix = header.getBytes(StandardCharsets.UTF_8);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[prefix.length + image.length + suffix.length];
        System.arraycopy(prefix, 0, body, 0, prefix.length);
        System.arraycopy(image, 0, body, prefix.length, image.length);
        System.arraycopy(suffix, 0, body, prefix.length + image.length, suffix.length);
        return body;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
