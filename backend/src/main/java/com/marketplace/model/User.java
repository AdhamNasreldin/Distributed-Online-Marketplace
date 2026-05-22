package com.marketplace.model;

public class User {
    private String id;
    private String fullName;
    private String email;
    private double balance;
    private String createdAt;
    private boolean twoFactorEnabled;

    public User() {}

    public User(String id, String fullName, String email, double balance, String createdAt, boolean twoFactorEnabled) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.balance = balance;
        this.createdAt = createdAt;
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }
}
