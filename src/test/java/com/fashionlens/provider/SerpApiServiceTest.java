package com.fashionlens.provider;

import com.fashionlens.product.Product;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerpApiServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void providerErrorIncludesSafeProviderReasonAndRedactsCredential() throws IOException {
        SerpApiService service = new SerpApiService();
        service.apiKey = "sensitive-test-key";
        JsonNode response = objectMapper.readTree("""
                {"error": "Invalid API key sensitive-test-key"}
                """);

        String message = service.providerException(response, 401, "image upload").getMessage();

        assertTrue(message.contains("HTTP 401"));
        assertTrue(message.contains("Invalid API key"));
        assertFalse(message.contains("sensitive-test-key"));
        assertTrue(message.contains("[redacted]"));
    }

    @Test
    void sendsApiKeyAsEncodedParameterInShoppingRequest() throws IOException {
        String apiKey = "test-key+with/special";
        AtomicReference<String> requestQuery = new AtomicReference<>();
        HttpServer server = startServer(exchange -> {
            requestQuery.set(exchange.getRequestURI().getRawQuery());
            respond(exchange, "{\"shopping_results\":[]}");
        });
        try {
            SerpApiService service = serviceFor(server, apiKey);

            service.searchProducts("kurta", "all", null, "match");

            assertTrue(requestQuery.get().contains("api_key=test-key%2Bwith%2Fspecial"));
            assertTrue(requestQuery.get().contains("engine=google_shopping"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void includesSelectedAudienceInShoppingQuery() throws IOException {
        AtomicReference<String> requestQuery = new AtomicReference<>();
        HttpServer server = startServer(exchange -> {
            requestQuery.set(exchange.getRequestURI().getRawQuery());
            respond(exchange, "{\"shopping_results\":[]}");
        });
        try {
            SerpApiService service = serviceFor(server, "test-key");

            service.searchProducts("kurta", "women", null, "match");

            String decodedQuery = URLDecoder.decode(requestQuery.get(), StandardCharsets.UTF_8);
            assertTrue(decodedQuery.contains("q=kurta women's clothing"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void sendsApiKeyInImageUploadAndLensSearchRequests(@org.junit.jupiter.api.io.TempDir Path tempDir)
            throws IOException {
        String apiKey = "test-image-key";
        AtomicReference<String> uploadBody = new AtomicReference<>();
        AtomicReference<String> lensQuery = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            if ("/image".equals(exchange.getRequestURI().getPath())) {
                uploadBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                respond(exchange, "{\"image_id\":\"uploaded-image\"}");
            } else {
                lensQuery.set(exchange.getRequestURI().getRawQuery());
                respond(exchange, "{\"visual_matches\":[]}");
            }
        });
        server.start();

        try {
            SerpApiService service = serviceFor(server, apiKey);
            Path image = tempDir.resolve("outfit.png");
            Files.write(image, new byte[] { 1, 2, 3 });

            service.searchByImage(image, 3, "image/png", "outfit.png", "all", null);

            assertTrue(uploadBody.get().contains(
                    "Content-Disposition: form-data; name=\"api_key\"\r\n\r\n" + apiKey + "\r\n"));
            assertTrue(lensQuery.get().contains("api_key=" + apiKey));
        } finally {
            server.stop(0);
        }
    }

    private static SerpApiService serviceFor(HttpServer server, String apiKey) {
        SerpApiService service = new SerpApiService();
        service.apiKey = apiKey;
        service.baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        service.objectMapper = new ObjectMapper();
        return service;
    }

    private static HttpServer startServer(
            com.sun.net.httpserver.HttpHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", handler);
        server.start();
        return server;
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    @Test
    void mapsDirectMerchantLinkFromShoppingResult() throws IOException {
        JsonNode results = objectMapper.readTree("""
                [{
                  "title": "Printed kurta",
                  "source": "Myntra",
                  "thumbnail": "https://images.example.com/kurta.jpg",
                  "link": "https://www.myntra.com/kurta",
                  "price": "₹999",
                  "extracted_price": 999
                }]
                """);

        List<Product> products = SerpApiService.mapShoppingResults(
                results, "all", null, "match");

        assertEquals(1, products.size());
        assertEquals("https://www.myntra.com/kurta", products.get(0).offers().get(0).url());
    }

    @Test
    void filtersExplicitMensShoppingResultsFromWomenPhotoSearch() throws IOException {
        JsonNode results = objectMapper.readTree("""
                [
                  {
                    "title": "Men's cotton kurta",
                    "source": "Store",
                    "thumbnail": "https://images.example.com/mens-kurta.jpg",
                    "extracted_price": 799
                  },
                  {
                    "title": "Women's embroidered kurta",
                    "source": "Store",
                    "thumbnail": "https://images.example.com/womens-kurta.jpg",
                    "extracted_price": 999
                  }
                ]
                """);

        List<Product> products = SerpApiService.mapShoppingResults(
                results, "women", null, "match");

        assertEquals(1, products.size());
        assertEquals("Women's embroidered kurta", products.get(0).name());
        assertEquals("Women", products.get(0).audience());
    }

    @Test
    void fallsBackToProductLinkAndIgnoresNonHttpLinks() throws IOException {
        JsonNode results = objectMapper.readTree("""
                [
                  {
                    "title": "Invalid link",
                    "source": "Store",
                    "thumbnail": "https://images.example.com/invalid.jpg",
                    "link": "javascript:alert(1)",
                    "product_link": "https://www.example.com/product",
                    "extracted_price": 500
                  }
                ]
                """);

        List<Product> products = SerpApiService.mapShoppingResults(
                results, "all", null, "match");

        assertEquals("https://www.example.com/product", products.get(0).offers().get(0).url());
    }

    @Test
    void buildsImageShoppingQueryFromRepeatedFashionTermsInsteadOfBrandNames() throws IOException {
        JsonNode visualMatches = objectMapper.readTree("""
                [
                  {"title": "BrandOne Blue Embroidered Cotton Kurta"},
                  {"title": "BrandTwo Blue Embroidered Kurta Set"},
                  {"title": "BrandThree Floral Embroidered Kurta"}
                ]
                """);

        String query = SerpApiService.buildVisualShoppingQuery(visualMatches);

        assertTrue(query.contains("kurta"));
        assertTrue(query.contains("embroidered"));
        assertTrue(query.contains("blue"));
        assertFalse(query.contains("BrandOne"));
        assertFalse(query.contains("BrandTwo"));
    }

    @Test
    void skipsImageShoppingQueryWhenVisualTitlesHaveNoClothingTerms() throws IOException {
        JsonNode visualMatches = objectMapper.readTree("""
                [{"title": "BrandOne product official page"}]
                """);

        assertEquals("", SerpApiService.buildVisualShoppingQuery(visualMatches));
    }
}
