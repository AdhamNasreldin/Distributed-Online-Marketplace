package com.marketplace.model;

public class Transaction {
    private String id;
    private String type; // "deposit", "purchase", "sale", "refund"
    private double amount;
    private String fromUserId;
    private String toUserId;
    private String productId;
    private String description;
    private String createdAt;
    private String status;

    public Transaction() {}

    public Transaction(String id, String type, double amount, String fromUserId, String toUserId, String productId, String description, String createdAt, String status) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.productId = productId;
        this.description = description;
        this.createdAt = createdAt;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
