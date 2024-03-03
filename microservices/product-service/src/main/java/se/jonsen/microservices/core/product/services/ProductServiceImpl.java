package se.jonsen.microservices.core.product.services;

import org.slf4j.Logger;
import se.jonsen.api.core.product.Product;
import se.jonsen.api.core.product.ProductService;
import org.springframework.web.bind.annotation.RestController;
import se.jonsen.api.exceptions.InvalidInputException;
import se.jonsen.api.exceptions.NotFoundException;
import se.jonsen.util.ServiceUtil;

@RestController
public class ProductServiceImpl implements ProductService {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ServiceUtil serviceUtil;

    public ProductServiceImpl(ServiceUtil serviceUtil) {
        this.serviceUtil = serviceUtil;
    }

    public Product getProduct(int productId) {
        LOG.debug("/product return the found product for productId={}", productId);

        if (productId < 1) {
            throw new InvalidInputException("Invalid product: " + productId);
        }

        if (productId == 13) {
            throw new NotFoundException("No product found for productId: " + productId);
        }

        return new Product(productId, "name-" + productId, 123, serviceUtil.getServiceAddress());
    }
}
