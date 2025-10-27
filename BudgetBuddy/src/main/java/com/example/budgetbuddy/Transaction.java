package com.example.budgetbuddy;

public class Transaction {
    private String id;
    private String date;
    private String category;
    private String description;
    private double amount;
    private String type; // "Expense" or "Income"

    public Transaction(String id, String date, String category, String description, double amount, String type) {
        this.id = id;
        this.date = date;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.type = type;
    }

    // Getters
    public String getId() { return id; }
    public String getDate() { return date; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public String getType() { return type; }

    // Setters (if needed, but kept minimal for a data model)
}