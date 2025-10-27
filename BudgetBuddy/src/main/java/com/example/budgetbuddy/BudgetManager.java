package com.example.budgetbuddy;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

public class BudgetManager {
    // Stores: username -> (category -> Budget object)
    private final Map<String, Map<String, Budget>> userBudgets;

    // Stores: username -> List of Transaction objects
    private final Map<String, List<Transaction>> userTransactions;

    public BudgetManager() {
        this.userBudgets = new HashMap<>();
        this.userTransactions = new HashMap<>();
        // Seed test data for 'testuser'
        seedTestData("testuser");
    }

    private void seedTestData(String username) {
        userBudgets.put(username, new HashMap<>());
        userTransactions.put(username, new ArrayList<>());

        // Transactions
        addExpense(username, "2025-10-25", "Food", "Dinner with friends", 1500.00);
        addExpense(username, "2025-10-26", "Transportation", "Gas refill", 800.00);
        addExpense(username, "2025-09-10", "Entertainment", "Movie tickets", 500.00);
        addIncome(username, "2025-10-01", "Salary", "Monthly pay", 50000.00);

        // Budgets (must be added after expenses to update spent amount)
        addBudget(username, "Food", 3000.00);
        addBudget(username, "Transportation", 2000.00);
    }

    private void ensureUserExists(String username) {
        userBudgets.putIfAbsent(username, new HashMap<>());
        userTransactions.putIfAbsent(username, new ArrayList<>());
    }

    // --- TRANSACTION MANAGEMENT ---

    private void addTransaction(String username, String date, String category, String description, double amount, String type) {
        ensureUserExists(username);
        String id = UUID.randomUUID().toString();
        Transaction transaction = new Transaction(id, date, category, description, amount, type);
        userTransactions.get(username).add(transaction);

        if (type.equals("Expense")) {
            // Update budget when an expense is added
            Budget budget = userBudgets.get(username).get(category);
            if (budget != null) {
                budget.recordExpense(amount);
            }
        }
    }

    public void addExpense(String username, String date, String category, String description, double amount) {
        addTransaction(username, date, category, description, amount, "Expense");
    }

    public void addIncome(String username, String date, String category, String description, double amount) {
        addTransaction(username, date, category, description, amount, "Income");
    }

    public void deleteTransaction(String username, String transactionId) {
        List<Transaction> transactions = userTransactions.get(username);
        if (transactions == null) return;

        transactions.stream()
                .filter(t -> t.getId().equals(transactionId))
                .findFirst()
                .ifPresent(t -> {
                    if (t.getType().equals("Expense")) {
                        // Revert budget spent amount
                        Budget budget = userBudgets.get(username).get(t.getCategory());
                        if (budget != null) {
                            budget.removeExpense(t.getAmount());
                        }
                    }
                    transactions.remove(t);
                });
    }

    public List<Transaction> getExpenses(String username) {
        ensureUserExists(username);
        return userTransactions.get(username).stream()
                .filter(t -> t.getType().equals("Expense"))
                .sorted(Comparator.comparing(Transaction::getDate).reversed())
                .collect(Collectors.toList());
    }

    public List<Transaction> getIncome(String username) {
        ensureUserExists(username);
        return userTransactions.get(username).stream()
                .filter(t -> t.getType().equals("Income"))
                .sorted(Comparator.comparing(Transaction::getDate).reversed())
                .collect(Collectors.toList());
    }

    public List<Transaction> getRecentTransactions(String username, int count) {
        ensureUserExists(username);
        return userTransactions.get(username).stream()
                .sorted(Comparator.comparing(Transaction::getDate).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    // --- BUDGET MANAGEMENT ---

    public void addBudget(String username, String category, double limit) {
        ensureUserExists(username);

        // Calculate current spent amount for the category
        double currentSpent = getExpenses(username).stream()
                .filter(t -> t.getCategory().equals(category))
                .mapToDouble(Transaction::getAmount)
                .sum();

        Budget budget = new Budget(category, limit, currentSpent);
        userBudgets.get(username).put(category, budget);
    }

    public Map<String, Budget> getUserBudgets(String username) {
        ensureUserExists(username);
        return userBudgets.get(username);
    }

    // --- REPORTING / SUMMARY ---

    public double getTotalIncome(String username) {
        return getIncome(username).stream().mapToDouble(Transaction::getAmount).sum();
    }

    public double getTotalExpenses(String username) {
        return getExpenses(username).stream().mapToDouble(Transaction::getAmount).sum();
    }

    public Map<String, Double> getExpensesByCategory(String username) {
        return getExpenses(username).stream()
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)
                ));
    }

    public Map<String, Double> getMonthlyExpenses(String username) {
        return getMonthlySummary(getExpenses(username));
    }

    public Map<String, Double> getMonthlyIncome(String username) {
        return getMonthlySummary(getIncome(username));
    }

    private Map<String, Double> getMonthlySummary(List<Transaction> transactions) {
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy");

        // Group by month and sum the amounts
        Map<String, Double> monthlyData = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> {
                            // Assuming the date is in "yyyy-MM-dd" format
                            return t.getDate().substring(0, 7); // Get "yyyy-MM" for sorting
                        },
                        Collectors.summingDouble(Transaction::getAmount)
                ));

        // Sort by year-month key and format the key to "MMM yyyy"
        return monthlyData.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        entry -> {
                            // Convert "yyyy-MM" back to a readable month/year format
                            String[] parts = entry.getKey().split("-");
                            int year = Integer.parseInt(parts[0]);
                            int month = Integer.parseInt(parts[1]);
                            return Month.of(month).toString().substring(0, 3) + " " + year;
                        },
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));
    }

    // --- DATA MANAGEMENT ---

    public boolean exportUserData(String username, String filePath) {
        ensureUserExists(username);
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("Date,Type,Category,Description,Amount");

            // Combine all transactions and sort by date
            List<Transaction> allTransactions = userTransactions.get(username).stream()
                    .sorted(Comparator.comparing(Transaction::getDate))
                    .collect(Collectors.toList());

            for (Transaction t : allTransactions) {
                writer.printf("%s,%s,%s,%s,%.2f%n",
                        t.getDate(),
                        t.getType(),
                        t.getCategory(),
                        t.getDescription().replace(",", ""), // Simple CSV safeguard
                        t.getAmount()
                );
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void clearUserData(String username) {
        userBudgets.remove(username);
        userTransactions.remove(username);
        // It's good practice to re-add empty structures to prevent NullPointerException on next operation
        ensureUserExists(username);
    }
}