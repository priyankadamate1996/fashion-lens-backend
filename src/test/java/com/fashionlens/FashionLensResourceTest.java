package com.fashionlens;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class FashionLensResourceTest {
    @Test
    void healthEndpointReportsUp() {
        given()
                .when().get("/api/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("service", equalTo("Fashion Lens API"));
    }

    @Test
    void productCatalogReturnsDemoOffersAndBestMatchFirst() {
        given()
                .when().get("/api/products")
                .then()
                .statusCode(200)
                .body("demoData", equalTo(true))
                .body("products", hasSize(26))
                .body("products[0].id", equalTo("cotton-kurta"))
                .body("products[0].offers[2].store", equalTo("Meesho"))
                .body("products[0].offers[2].price", equalTo(649))
                .body("message", containsString("SERPAPI_KEY to .env"));
    }

    @Test
    void sellerComparisonRequiresAProductOffersToken() {
        given()
                .when().get("/api/products/offers")
                .then()
                .statusCode(400)
                .body("error", containsString("token"));
    }

    @Test
    void sellerComparisonExplainsWhenProviderIsNotConfigured() {
        given()
                .queryParam("pageToken", "sample-token")
                .when().get("/api/products/offers")
                .then()
                .statusCode(503)
                .body("error", containsString("SERPAPI_KEY to .env"));
    }

    @Test
    void productsCanBeFilteredAndSortedByPrice() {
        given()
                .queryParam("category", "Dresses")
                .queryParam("sort", "price")
                .when().get("/api/products")
                .then()
                .statusCode(200)
                .body("products", hasSize(1))
                .body("products[0].id", equalTo("printed-midi-dress"));
    }

    @Test
    void productsCanBeSearchedByNameAndDetails() {
        given()
                .queryParam("query", "hand-block")
                .when().get("/api/products")
                .then()
                .statusCode(200)
                .body("products", hasSize(1))
                .body("products[0].id", equalTo("indigo-set"));
    }

    @Test
    void productsCanBeFilteredByAudience() {
        given()
                .queryParam("audience", "women")
                .when().get("/api/products")
                .then()
                .statusCode(200)
                .body("products", hasSize(13))
                .body("products[0].audience", equalTo("Women"));

        given()
                .queryParam("audience", "men")
                .when().get("/api/products")
                .then()
                .statusCode(200)
                .body("products", hasSize(8))
                .body("products[0].audience", equalTo("Men"));

        given()
                .queryParam("audience", "kids")
                .when().get("/api/products")
                .then()
                .statusCode(200)
                .body("products", hasSize(5))
                .body("products[0].audience", equalTo("Kids & Baby"));
    }

    @Test
    void productSubcategoriesAreFilterable() {
        given()
                .queryParam("audience", "women")
                .queryParam("category", "Jeans & Trousers")
                .when().get("/api/products")
                .then()
                .statusCode(200)
                .body("products", hasSize(1))
                .body("products[0].id", equalTo("women-straight-jeans"));

        given()
                .queryParam("audience", "kids")
                .queryParam("category", "Baby Accessories")
                .when().get("/api/products")
                .then()
                .statusCode(200)
                .body("products", hasSize(1))
                .body("products[0].id", equalTo("baby-soft-blanket"));
    }

    @Test
    void invalidAudienceIsReportedAsBadRequest() {
        given()
                .queryParam("audience", "pets")
                .when().get("/api/products")
                .then()
                .statusCode(400)
                .body("error", containsString("Audience must be"));
    }

    @Test
    void invalidSortIsReportedAsBadRequest() {
        given()
                .queryParam("sort", "random")
                .when().get("/api/products")
                .then()
                .statusCode(400)
                .body("error", containsString("Sort must be"));
    }

    @Test
    void imageUploadReturnsClearlyMarkedDemoResults() {
        given()
                .multiPart("image", "kurta.png", new byte[] { 1, 2, 3 }, "image/png")
                .when().post("/api/search/image")
                .then()
                .statusCode(200)
                .body("demoData", equalTo(true))
                .body("matches", hasSize(26))
                .body("message", containsString("Sample matches"))
                .body("message", containsString("SERPAPI_KEY to .env"));
    }

    @Test
    void imageUploadCanFilterResultsByAudience() {
        given()
                .multiPart("image", "shirt.png", new byte[] { 1, 2, 3 }, "image/png")
                .multiPart("audience", "kids")
                .when().post("/api/search/image")
                .then()
                .statusCode(200)
                .body("matches", hasSize(5))
                .body("matches[0].audience", equalTo("Kids & Baby"));
    }

    @Test
    void imageUploadRejectsUnsupportedMediaTypes() {
        given()
                .multiPart("image", "notes.txt", new byte[] { 1, 2, 3 }, "text/plain")
                .when().post("/api/search/image")
                .then()
                .statusCode(415)
                .body("error", containsString("JPEG, PNG, or WebP"));
    }

    @Test
    void imageUploadRejectsEmptyFiles() {
        given()
                .multiPart("image", "empty.png", new byte[0], "image/png")
                .when().post("/api/search/image")
                .then()
                .statusCode(400)
                .body("error", containsString("empty"));
    }

    @Test
    void imageUploadRejectsFilesLargerThan500Kb() {
        given()
                .multiPart("image", "large.png", new byte[500 * 1024 + 1], "image/png")
                .when().post("/api/search/image")
                .then()
                .statusCode(413)
                .body("error", containsString("500 KB"));
    }
}
