package com.marketplace.model;

import java.util.List;

public class ReportSummary {
    private double totalRevenue;
    private int totalTransactions;
    private int activeListings;
    private int soldItems;
    private int lowStockItems;
    private List<CategorySummary> topCategories;
    private List<Transaction> recentTransactions;

    public ReportSummary() {}

    public ReportSummary(double totalRevenue, int totalTransactions, int activeListings, int soldItems, int lowStockItems, List<CategorySummary> topCategories, List<Transaction> recentTransactions) {
        this.totalRevenue = totalRevenue;
        this.totalTransactions = totalTransactions;
        this.activeListings = activeListings;
        this.soldItems = soldItems;
        this.lowStockItems = lowStockItems;
        this.topCategories = topCategories;
        this.recentTransactions = recentTransactions;
    }

    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }

    public int getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(int totalTransactions) { this.totalTransactions = totalTransactions; }

    public int getActiveListings() { return activeListings; }
    public void setActiveListings(int activeListings) { this.activeListings = activeListings; }

    public int getSoldItems() { return soldItems; }
    public void setSoldItems(int soldItems) { this.soldItems = soldItems; }

    public int getLowStockItems() { return lowStockItems; }
    public void setLowStockItems(int lowStockItems) { this.lowStockItems = lowStockItems; }

    public List<CategorySummary> getTopCategories() { return topCategories; }
    public void setTopCategories(List<CategorySummary> topCategories) { this.topCategories = topCategories; }

    public List<Transaction> getRecentTransactions() { return recentTransactions; }
    public void setRecentTransactions(List<Transaction> recentTransactions) { this.recentTransactions = recentTransactions; }

    public static class CategorySummary {
        private String category;
        private int count;
        private double revenue;

        public CategorySummary() {}

        public CategorySummary(String category, int count, double revenue) {
            this.category = category;
            this.count = count;
            this.revenue = revenue;
        }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }

        public double getRevenue() { return revenue; }
        public void setRevenue(double revenue) { this.revenue = revenue; }
    }
}
