package se.jonsen.microservices.composite.product.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import se.jonsen.api.core.product.Product;
import se.jonsen.api.core.product.ProductService;
import se.jonsen.api.core.recommendation.Recommendation;
import se.jonsen.api.core.recommendation.RecommendationService;
import se.jonsen.api.core.review.Review;
import se.jonsen.api.core.review.ReviewService;
import se.jonsen.api.exceptions.InvalidInputException;
import se.jonsen.api.exceptions.NotFoundException;
import se.jonsen.util.HttpErrorInfo;

import java.io.IOException;
import java.util.List;

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    private final String productServiceUrl;
    private final String recommendationServiceUrl;
    private final String reviewServiceUrl;

    public ProductCompositeIntegration(
            RestTemplate restTemplate,
            ObjectMapper mapper,
            @Value("${app.product-service.host}") String productServiceHost,
            @Value("${app.product-service.port}") int productServicePort,
            @Value("${app.recommendation-service.host}") String recommendationServiceHost,
            @Value("${app.recommendation-service.port}") int recommendationServicePort,
            @Value("${app.review-service.host}") String reviewServiceHost,
            @Value("${app.review-service.port}") int reviewServicePort) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;

        productServiceUrl = "http://%s:%d/product/".formatted(productServiceHost, productServicePort);
        recommendationServiceUrl = "http://%s:%d/recommendation?productId=".formatted(recommendationServiceHost, recommendationServicePort);
        reviewServiceUrl = "http://%s:%d/review?productId=".formatted(reviewServiceHost, reviewServicePort);
    }

    private String getErrorMessage(HttpClientErrorException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }

    public Product getProduct(int productId) {
        var url = productServiceUrl + productId;
        LOG.debug("Will call getProduct API on URL: {}", url);

        try {
            var product = restTemplate.getForObject(url, Product.class);
            LOG.debug("Found a product with id: {}", product.getProductId());
            return product;
        } catch (HttpClientErrorException ex) {
            switch (HttpStatus.resolve(ex.getStatusCode().value())) {
                case NOT_FOUND -> throw new NotFoundException(getErrorMessage(ex));
                case UNPROCESSABLE_ENTITY -> throw new InvalidInputException(getErrorMessage(ex));
                default -> throw new RuntimeException(getErrorMessage(ex));
            }
        }
    }

    public List<Recommendation> getRecommendations(int productId) {
        var url = recommendationServiceUrl + productId;
        LOG.debug("Will call getRecommendations API on URL: {}", url);

        try {

            var recommendations = restTemplate
                    .exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<Recommendation>>() {
                    })
                    .getBody();

            LOG.debug("Found {} recommendations for a product with id: {}", recommendations.size(), productId);
            return recommendations;
        } catch (Exception ex) {
            LOG.warn("Got an exception while requesting recommendations, return zero recommendations: {}", ex.getMessage());
            return List.of();
        }
    }

    public List<Review> getReviews(int productId) {
        var url = reviewServiceUrl + productId;

        LOG.debug("Will call getReviews API on URL: {}", url);
        try {
            var reviews = restTemplate
                    .exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<Review>>() {
                    })
                    .getBody();

            LOG.debug("Found {} reviews for a product with id: {}", reviews.size(), productId);
            return reviews;
        } catch (Exception ex) {
            LOG.warn("Got an exception while requesting reviews, return zero reviews: {}", ex.getMessage());
            return List.of();
        }
    }
}
