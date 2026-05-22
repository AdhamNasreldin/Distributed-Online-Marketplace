package com.marketplace.model;

public class StoredProduct extends Product {
    private String schema;

    public StoredProduct() {}

    public StoredProduct(Product product, String schema) {
        super(product.getId(), product.getOwnerId(), product.getName(), product.getBrand(), product.getCategory(),
              product.getDescription(), product.getPrice(), product.getQuantity(), product.getCondition(),
              product.getStatus(), product.getListedAt(), product.getSoldCount(), product.getColor());
        this.schema = schema;
    }

    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }
}
