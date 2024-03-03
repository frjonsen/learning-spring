package se.jonsen.api.composite.product;

public class ServiceAddresses {
    private final String cmp;
    private final String pro;
    private final String rev;
    private final String rec;

    public ServiceAddresses() {
        cmp = null;
        pro = null;
        rev = null;
        rec = null;
    }

    public ServiceAddresses(String compositeAddress, String productAddress, String reviewAddress, String recommendationAddress) {
        this.cmp = compositeAddress;
        this.pro = productAddress;
        this.rev = reviewAddress;
        this.rec = recommendationAddress;
    }

    public String getCompositeAddress() {
        return cmp;
    }

    public String getProductAddress() {
        return pro;
    }

    public String getReviewAddress() {
        return rev;
    }

    public String getRecommendationAddress() {
        return rec;
    }
}
