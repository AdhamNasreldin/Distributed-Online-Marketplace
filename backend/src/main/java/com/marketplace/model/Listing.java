package com.marketplace.model;

public class Listing extends Product {
    private String sellerName;
    private String sellerEmail;

    public Listing() {}

    public Listing(Product product, String sellerName, String sellerEmail) {
        super(product.getId(), product.getOwnerId(), product.getName(), product.getBrand(), product.getCategory(),
              product.getDescription(), product.getPrice(), product.getQuantity(), product.getCondition(),
              product.getStatus(), product.getListedAt(), product.getSoldCount(), product.getColor());
        this.sellerName = sellerName;
        this.sellerEmail = sellerEmail;
    }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getSellerEmail() { return sellerEmail; }
    public void setSellerEmail(String sellerEmail) { this.sellerEmail = sellerEmail; }
}
