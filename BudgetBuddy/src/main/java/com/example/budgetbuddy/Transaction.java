package com.example.budgetbuddy;

import java.util.UUID;

public class Transaction {
    private final String id;
    private final String date;
    private final String category;
    private final String description;
    private final double amount;
    private final String type; // "Income" or "Expense"

    // Constructor for new transactions (generates ID)
    public Transaction(String date, String category, String description, double amount, String type) {
        this.id = UUID.randomUUID().toString();
        this.date = date;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.type = type;
    }

    // Constructor for loading from CSV (with existing ID)
    public Transaction(String date, String category, String description, double amount, String type, String id) {
        this.id = id;
        this.date = date;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.type = type;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s: %s (%.2f)",
                type, date, category, description, amount);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Transaction that = (Transaction) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
