package com.fashionlens.api;

import com.fashionlens.product.ApiError;
import com.fashionlens.product.ImageSearchResponse;
import com.fashionlens.product.Product;
import com.fashionlens.product.ProductCatalog;
import com.fashionlens.product.ProductOffer;
import com.fashionlens.product.ProductOffersResponse;
import com.fashionlens.product.ProductSearchResponse;
import com.fashionlens.provider.ProviderException;
import com.fashionlens.provider.SerpApiService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.smallrye.common.annotation.Blocking;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.List;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Blocking
public class FashionLensResource {
    private static final Logger LOG = Logger.getLogger(FashionLensResource.class);
    private static final long MAX_IMAGE_SIZE_BYTES = 500L * 1024;

    @Inject
    ProductCatalog productCatalog;

    @Inject
    SerpApiService serpApiService;

    @GET
    @Path("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", "Fashion Lens API");
    }

    @GET
    @Path("/products")
    public Response products(
            @QueryParam("query") String query,
            @QueryParam("audience") @DefaultValue("all") String audience,
            @QueryParam("category") String category,
            @QueryParam("sort") @DefaultValue("match") String sort) {
        try {
            if (serpApiService.isConfigured()) {
                List<Product> results = serpApiService.searchProducts(query, audience, category, sort);
                return Response.ok(new ProductSearchResponse(
                        results,
                        false,
                        "Live shopping results. Prices and availability may change; confirm with the seller."))
                        .build();
            }

            List<Product> results = productCatalog.search(query, audience, category, sort);
            return Response.ok(new ProductSearchResponse(
                    results,
                    true,
                    "Sample listings are shown. Add SERPAPI_KEY to .env or the Windows user environment, then restart the backend to enable live shopping results."))
                    .build();
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError(exception.getMessage()))
                    .build();
        } catch (ProviderException exception) {
            return providerError(exception);
        }
    }

    @GET
    @Path("/products/offers")
    public Response otherProductOffers(@QueryParam("pageToken") String pageToken) {
        if (pageToken == null || pageToken.isBlank()) {
            return badRequest("A product offers token is required.");
        }
        if (!serpApiService.isConfigured()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ApiError("Add SERPAPI_KEY to .env or the Windows user environment, then restart the backend to load additional seller offers."))
                    .build();
        }
        try {
            List<ProductOffer> offers = serpApiService.searchOtherOffers(pageToken);
            return Response.ok(new ProductOffersResponse(
                    offers,
                    "Seller prices and availability may change; confirm with the retailer."))
                    .build();
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        } catch (ProviderException exception) {
            return providerError(exception);
        }
    }

    @POST
    @Path("/search/image")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response searchByImage(
            @RestForm("image") FileUpload image,
            @RestForm("audience") String audience) {
        if (image == null) {
            return badRequest("Upload an image in the 'image' form field.");
        }
        if (image.size() <= 0) {
            return badRequest("The uploaded image is empty.");
        }
        if (image.size() > MAX_IMAGE_SIZE_BYTES) {
            return Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE)
                    .entity(new ApiError("Image must be 500 KB or smaller."))
                    .build();
        }
        if (!isSupportedImage(image.contentType())) {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity(new ApiError("Upload a JPEG, PNG, or WebP image."))
                    .build();
        }

        try {
            if (serpApiService.isConfigured()) {
                List<Product> matches = serpApiService.searchByImage(
                        image.uploadedFile(),
                        image.size(),
                        image.contentType(),
                        image.fileName(),
                        audience,
                        null);
                return Response.ok(new ImageSearchResponse(
                        matches,
                        false,
                        "Live visual matches and shopping results. Prices and availability may change; confirm with the seller."))
                        .build();
            }

            return Response.ok(new ImageSearchResponse(
                    productCatalog.imageMatches(audience),
                    true,
                    "Sample matches are shown; your photo was not sent to a search provider. Add SERPAPI_KEY to .env or the Windows user environment, then restart the backend to enable live image search."))
                    .build();
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        } catch (ProviderException exception) {
            return providerError(exception);
        }
    }

    private static boolean isSupportedImage(String contentType) {
        return MediaType.valueOf("image/jpeg").toString().equalsIgnoreCase(contentType)
                || MediaType.valueOf("image/png").toString().equalsIgnoreCase(contentType)
                || MediaType.valueOf("image/webp").toString().equalsIgnoreCase(contentType);
    }

    private static Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ApiError(message))
                .build();
    }

    private static Response providerError(ProviderException exception) {
        LOG.errorf("Search provider request failed: %s", exception.getMessage());
        return Response.status(Response.Status.BAD_GATEWAY)
                .entity(new ApiError(exception.getMessage()))
                .build();
    }

    public record HealthResponse(String status, String service) {
    }
}
