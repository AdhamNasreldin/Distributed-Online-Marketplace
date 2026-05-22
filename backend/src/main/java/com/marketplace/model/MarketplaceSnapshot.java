package com.marketplace.model;

import java.util.List;

public class MarketplaceSnapshot {
    private List<Listing> listings;
    private List<InventoryItem> inventory;
    private List<Transaction> transactions;
    private List<Purchase> purchases;
    private ReportSummary report;
    private User currentUser;

    public MarketplaceSnapshot() {}

    public MarketplaceSnapshot(List<Listing> listings, List<InventoryItem> inventory, List<Transaction> transactions, List<Purchase> purchases, ReportSummary report, User currentUser) {
        this.listings = listings;
        this.inventory = inventory;
        this.transactions = transactions;
        this.purchases = purchases;
        this.report = report;
        this.currentUser = currentUser;
    }

    public List<Listing> getListings() { return listings; }
    public void setListings(List<Listing> listings) { this.listings = listings; }

    public List<InventoryItem> getInventory() { return inventory; }
    public void setInventory(List<InventoryItem> inventory) { this.inventory = inventory; }

    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }

    public List<Purchase> getPurchases() { return purchases; }
    public void setPurchases(List<Purchase> purchases) { this.purchases = purchases; }

    public ReportSummary getReport() { return report; }
    public void setReport(ReportSummary report) { this.report = report; }

    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User currentUser) { this.currentUser = currentUser; }
}
