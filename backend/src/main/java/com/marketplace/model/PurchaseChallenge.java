package com.marketplace.model;

public class PurchaseChallenge {
    private String challengeId;
    private Listing product;
    private double amount;
    private String message;

    public PurchaseChallenge() {}

    public PurchaseChallenge(String challengeId, Listing product, double amount, String message) {
        this.challengeId = challengeId;
        this.product = product;
        this.amount = amount;
        this.message = message;
    }

    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }

    public Listing getProduct() { return product; }
    public void setProduct(Listing product) { this.product = product; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
