package com.marketplace.model;

public class Product {
    private String id;
    private String ownerId;
    private String name;
    private String brand;
    private String category;
    private String description;
    private double price;
    private int quantity;
    private String condition; // "New", "Like New", "Used"
    private String status;    // "listed", "sold", "draft"
    private String listedAt;
    private int soldCount;
    private String color;

    public Product() {}

    public Product(String id, String ownerId, String name, String brand, String category, String description,
                   double price, int quantity, String condition, String status, String listedAt, int soldCount, String color) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.condition = condition;
        this.status = status;
        this.listedAt = listedAt;
        this.soldCount = soldCount;
        this.color = color;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getListedAt() { return listedAt; }
    public void setListedAt(String listedAt) { this.listedAt = listedAt; }

    public int getSoldCount() { return soldCount; }
    public void setSoldCount(int soldCount) { this.soldCount = soldCount; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
