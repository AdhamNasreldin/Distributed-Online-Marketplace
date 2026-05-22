package com.marketplace.model;

public class InventoryItem {
    private String productId;
    private String productName;
    private String brand;
    private int quantity;
    private int reserved;
    private int sold;
    private String status;
    private String updatedAt;

    public InventoryItem() {}

    public InventoryItem(String productId, String productName, String brand, int quantity, int reserved, int sold, String status, String updatedAt) {
        this.productId = productId;
        this.productName = productName;
        this.brand = brand;
        this.quantity = quantity;
        this.reserved = reserved;
        this.sold = sold;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getReserved() { return reserved; }
    public void setReserved(int reserved) { this.reserved = reserved; }

    public int getSold() { return sold; }
    public void setSold(int sold) { this.sold = sold; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
