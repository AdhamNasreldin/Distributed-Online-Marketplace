package com.marketplace.model;

import java.util.List;

public class CsvImportRow {
    private String name;
    private String brand;
    private double price;
    private int quantity;
    private String category;
    private String description;
    private boolean valid;
    private List<String> errors;

    public CsvImportRow() {}

    public CsvImportRow(String name, String brand, double price, int quantity, String category, String description, boolean valid, List<String> errors) {
        this.name = name;
        this.brand = brand;
        this.price = price;
        this.quantity = quantity;
        this.category = category;
        this.description = description;
        this.valid = valid;
        this.errors = errors;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}
