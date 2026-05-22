package com.marketplace.model;

public class Purchase {
    private String id;
    private String productId;
    private String productName;
    private String buyerId;
    private String sellerId;
    private double amount;
    private String purchasedAt;
    private String status;

    public Purchase() {}

    public Purchase(String id, String productId, String productName, String buyerId, String sellerId, double amount, String purchasedAt, String status) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.amount = amount;
        this.purchasedAt = purchasedAt;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(String purchasedAt) { this.purchasedAt = purchasedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
