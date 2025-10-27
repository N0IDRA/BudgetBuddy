package com.example.budgetbuddy;

public class Budget {
    private String category;
    private double limit;
    private double spent;

    public Budget(String category, double limit) {
        this.category = category;
        this.limit = limit;
        this.spent = 0.0;
    }

    // Constructor used for loading/reconstruction
    public Budget(String category, double limit, double spent) {
        this.category = category;
        this.limit = limit;
        this.spent = spent;
    }

    // Methods to manage spending
    public void recordExpense(double amount) {
        this.spent += amount;
    }

    public void removeExpense(double amount) {
        this.spent -= amount;
    }

    // Getters
    public String getCategory() { return category; }
    public double getLimit() { return limit; }
    public double getSpent() { return spent; }

    // Setters
    public void setLimit(double limit) { this.limit = limit; }
    public void setSpent(double spent) { this.spent = spent; }
}