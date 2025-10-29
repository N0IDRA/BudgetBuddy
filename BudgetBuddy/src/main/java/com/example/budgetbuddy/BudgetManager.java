package com.example.budgetbuddy;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

public class BudgetManager {
    private static final String TRANSACTIONS_CSV = "transactions.csv";
    private static final String BUDGETS_CSV = "budgets.csv";
    private static final String TRANSACTIONS_HEADER = "username,date,category,description,amount,type,transaction_id";
    private static final String BUDGETS_HEADER = "username,category,limit,spent";
    private static final String DELIMITER = ",";

    // Stores: username -> (category -> Budget object)
    private final Map<String, Map<String, Budget>> userBudgets;

    // Stores: username -> List of Transaction objects
    private final Map<String, List<Transaction>> userTransactions;

    public BudgetManager() {
        this.userBudgets = new HashMap<>();
        this.userTransactions = new HashMap<>();

        // Load existing data from files
        loadTransactions();
        loadBudgets();

        // Seed test data for 'testuser' only if no data exists
        if (!userTransactions.containsKey("testuser") || userTransactions.get("testuser").isEmpty()) {
            seedTestData("testuser");
        }
    }

    private void seedTestData(String username) {
        userBudgets.put(username, new HashMap<>());
        userTransactions.put(username, new ArrayList<>());

        // Transactions
        addExpense(username, "2025-10-25", "Food", "Dinner with friends", 1500.00);
        addExpense(username, "2025-10-26", "Transportation", "Gas refill", 800.00);
        addExpense(username, "2025-09-10", "Entertainment", "Movie tickets", 500.00);
        addIncome(username, "2025-10-01", "Monthly pay", 50000.00);

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
        Transaction transaction = new Transaction(date, category, description, amount, type);
        userTransactions.get(username).add(transaction);

        if (type.equals("Expense")) {
            // Update budget when an expense is added
            Budget budget = userBudgets.get(username).get(category);
            if (budget != null) {
                budget.recordExpense(amount);
            }
        }

        // AUTO-SAVE: Persist to file immediately
        saveTransactions();
        if (type.equals("Expense")) {
            saveBudgets(); // Save budgets since spent amount changed
        }
    }

    public void addExpense(String username, String date, String category, String description, double amount) {
        addTransaction(username, date, category, description, amount, "Expense");
    }

    // UPDATED: Income no longer needs category parameter
    public void addIncome(String username, String date, String description, double amount) {
        addTransaction(username, date, "Income", description, amount, "Income");
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

                    // AUTO-SAVE: Persist changes immediately
                    saveTransactions();
                    if (t.getType().equals("Expense")) {
                        saveBudgets();
                    }
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

        // AUTO-SAVE: Persist to file immediately
        saveBudgets();
    }

    public void updateBudgetLimit(String username, String category, double newLimit) {
        Map<String, Budget> budgets = userBudgets.get(username);
        if (budgets != null && budgets.containsKey(category)) {
            budgets.get(category).setLimit(newLimit);
            // AUTO-SAVE: Persist changes immediately
            saveBudgets();
        }
    }

    public void deleteBudget(String username, String category) {
        Map<String, Budget> budgets = userBudgets.get(username);
        if (budgets != null) {
            budgets.remove(category);
            // AUTO-SAVE: Persist changes immediately
            saveBudgets();
        }
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

    /**
     * Export user data to a specified file path (for manual exports/reports)
     * This is different from auto-save - this is for user-requested exports
     */
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
                        escapeCSV(t.getDescription()),
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

        // AUTO-SAVE: Persist changes immediately
        saveTransactions();
        saveBudgets();

        // Re-add empty structures to prevent NullPointerException on next operation
        ensureUserExists(username);
    }

    // --- PERSISTENCE METHODS (AUTO-SAVE) ---

    /**
     * Load all transactions from CSV file on startup
     */
    private void loadTransactions() {
        if (!Files.exists(Paths.get(TRANSACTIONS_CSV))) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(TRANSACTIONS_CSV))) {
            String line;
            reader.readLine(); // Skip header line

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(DELIMITER, -1);
                if (data.length >= 7) {
                    String username = data[0];
                    String date = data[1];
                    String category = data[2];
                    String description = unescapeCSV(data[3]);
                    double amount = Double.parseDouble(data[4]);
                    String type = data[5];
                    String transactionId = data[6];

                    ensureUserExists(username);
                    Transaction transaction = new Transaction(date, category, description, amount, type, transactionId);
                    userTransactions.get(username).add(transaction);
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading transactions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * AUTO-SAVE: Save all transactions to CSV file immediately
     */
    private void saveTransactions() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(TRANSACTIONS_CSV))) {
            writer.println(TRANSACTIONS_HEADER);

            for (Map.Entry<String, List<Transaction>> entry : userTransactions.entrySet()) {
                String username = entry.getKey();
                for (Transaction transaction : entry.getValue()) {
                    writer.printf("%s,%s,%s,%s,%.2f,%s,%s%n",
                            username,
                            transaction.getDate(),
                            transaction.getCategory(),
                            escapeCSV(transaction.getDescription()),
                            transaction.getAmount(),
                            transaction.getType(),
                            transaction.getId()
                    );
                }
            }
            writer.flush(); // Ensure data is written immediately
        } catch (IOException e) {
            System.err.println("Error saving transactions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load all budgets from CSV file on startup
     */
    private void loadBudgets() {
        if (!Files.exists(Paths.get(BUDGETS_CSV))) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(BUDGETS_CSV))) {
            String line;
            reader.readLine(); // Skip header line

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(DELIMITER, -1);
                if (data.length >= 4) {
                    String username = data[0];
                    String category = data[1];
                    double limit = Double.parseDouble(data[2]);
                    double spent = Double.parseDouble(data[3]);

                    ensureUserExists(username);
                    Budget budget = new Budget(category, limit, spent);
                    userBudgets.get(username).put(category, budget);
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading budgets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * AUTO-SAVE: Save all budgets to CSV file immediately
     */
    private void saveBudgets() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(BUDGETS_CSV))) {
            writer.println(BUDGETS_HEADER);

            for (Map.Entry<String, Map<String, Budget>> userEntry : userBudgets.entrySet()) {
                String username = userEntry.getKey();
                for (Budget budget : userEntry.getValue().values()) {
                    writer.printf("%s,%s,%.2f,%.2f%n",
                            username,
                            budget.getCategory(),
                            budget.getLimit(),
                            budget.getSpent()
                    );
                }
            }
            writer.flush(); // Ensure data is written immediately
        } catch (IOException e) {
            System.err.println("Error saving budgets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- UTILITY METHODS ---

    private String escapeCSV(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }

    private String unescapeCSV(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1).replace("\"\"", "\"");
        }
        return value;
    }
}
