package se.jonsen.microservices.composite.product.services;
import org.springframework.web.bind.annotation.RestController;
import se.jonsen.api.composite.product.*;
import se.jonsen.api.core.product.Product;
import se.jonsen.api.core.recommendation.Recommendation;
import se.jonsen.api.core.review.Review;
import se.jonsen.api.exceptions.NotFoundException;
import se.jonsen.util.ServiceUtil;

import java.util.List;

@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {
    private final ServiceUtil serviceUtil;
    private final ProductCompositeIntegration integration;

    public ProductCompositeServiceImpl(ServiceUtil serviceUtil, ProductCompositeIntegration integration) {
        this.serviceUtil = serviceUtil;
        this.integration = integration;
    }

    @Override
    public ProductAggregate getProduct(int productId) {
        var product = integration.getProduct(productId);
        if (product == null) {
            throw new NotFoundException("No product found for productId: " + productId);
        }
        var recommendations = integration.getRecommendations(productId);
        var reviews = integration.getReviews(productId);
        return createProductAggregate(product, recommendations, reviews, serviceUtil.getServiceAddress());
    }

    private ProductAggregate createProductAggregate(Product product, List<Recommendation> recommendations, List<Review> reviews, String serviceAddress) {
        var productId = product.getProductId();
        var name = product.getName();
        var weight = product.getWeight();

        var productRecommendations = (recommendations == null) ? null : recommendations.stream()
                .map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate()))
                .toList();

        var productReviews = (reviews == null) ? null : reviews.stream()
                .map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject()))
                .toList();

        var productAddress = product.getServiceAddress();
        var reviewAddress = (reviews != null && !reviews.isEmpty()) ? reviews.getFirst().getServiceAddress() : "";
        var recommendationAddress = (recommendations != null && !recommendations.isEmpty()) ? recommendations.getFirst().getServiceAddress() : "";

        var serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress, recommendationAddress);

        return new ProductAggregate(productId, name, weight, productRecommendations, productReviews, serviceAddresses);
    }
}
