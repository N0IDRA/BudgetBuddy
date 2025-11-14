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

    private final Map<String, Map<String, Budget>> userBudgets;

    private final Map<String, List<Transaction>> userTransactions;

    private Map<String, Double> targetSavings = new HashMap<>();


    public BudgetManager() {
        this.userBudgets = new HashMap<>();
        this.userTransactions = new HashMap<>();

        loadTransactions();
        loadBudgets();

        if (!userTransactions.containsKey("testuser") || userTransactions.get("testuser").isEmpty()) {
            seedTestData("testuser");
        }
    }

    private final Map<String, Double> userTargetSavings = new HashMap<>();



    public void setTargetSavings(String username, double target) {
        targetSavings.put(username, target);
        saveData();
    }

    public double getTargetSavings(String username) {
        return targetSavings.getOrDefault(username, 0.0);
    }

    public void saveData() {
        saveBudgets();       
        saveTransactions();  
    }

    private void seedTestData(String username) {
        userBudgets.put(username, new HashMap<>());
        userTransactions.put(username, new ArrayList<>());

        addExpense(username, "2025-10-25", "Food", "Dinner with friends", 1500.00);
        addExpense(username, "2025-10-26", "Transportation", "Gas refill", 800.00);
        addExpense(username, "2025-09-10", "Entertainment", "Movie tickets", 500.00);
        addIncome(username, "2025-10-01", "Monthly pay", 50000.00);

        addBudget(username, "Food", 3000.00);
        addBudget(username, "Transportation", 2000.00);
    }

    private void ensureUserExists(String username) {
        userBudgets.putIfAbsent(username, new HashMap<>());
        userTransactions.putIfAbsent(username, new ArrayList<>());
    }


    private void addTransaction(String username, String date, String category, String description, double amount, String type) {
        ensureUserExists(username);
        Transaction transaction = new Transaction(date, category, description, amount, type);
        userTransactions.get(username).add(transaction);

        if (type.equals("Expense")) {
            Budget budget = userBudgets.get(username).get(category);
            if (budget != null) {
                budget.recordExpense(amount);
            }
        }

        saveTransactions();
        if (type.equals("Expense")) {
            saveBudgets();
        }
    }

    public void addExpense(String username, String date, String category, String description, double amount) {
        addTransaction(username, date, category, description, amount, "Expense");
    }

    public void addIncome(String username, String date, String description, double amount) {
        addTransaction(username, date, "Income", description, amount, "Income");
    }

    public void removeExpense(String username, Transaction transaction) {
        List<Transaction> transactions = userTransactions.get(username);
        if (transactions == null) return;

        if (transaction.getType().equals("Expense")) {
            Budget budget = userBudgets.get(username).get(transaction.getCategory());
            if (budget != null) {
                budget.removeExpense(transaction.getAmount());
            }
        }
        transactions.remove(transaction);

        saveTransactions();
        if (transaction.getType().equals("Expense")) {
            saveBudgets();
        }
    }

    public void deleteTransaction(String username, String transactionId) {
        List<Transaction> transactions = userTransactions.get(username);
        if (transactions == null) return;

        transactions.stream()
                .filter(t -> t.getId().equals(transactionId))
                .findFirst()
                .ifPresent(t -> {
                    if (t.getType().equals("Expense")) {
                        Budget budget = userBudgets.get(username).get(t.getCategory());
                        if (budget != null) {
                            budget.removeExpense(t.getAmount());
                        }
                    }
                    transactions.remove(t);

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

    public void addBudget(String username, String category, double limit) {
        ensureUserExists(username);

        double currentSpent = getExpenses(username).stream()
                .filter(t -> t.getCategory().equals(category))
                .mapToDouble(Transaction::getAmount)
                .sum();

        Budget budget = new Budget(category, limit, currentSpent);
        userBudgets.get(username).put(category, budget);
        saveBudgets();
    }

    public void updateBudgetLimit(String username, String category, double newLimit) {
        Map<String, Budget> budgets = userBudgets.get(username);
        if (budgets != null && budgets.containsKey(category)) {
            budgets.get(category).setLimit(newLimit);
            saveBudgets();
        }
    }

    public void deleteBudget(String username, String category) {
        Map<String, Budget> budgets = userBudgets.get(username);
        if (budgets != null) {
            budgets.remove(category);
            saveBudgets();
        }
    }

    public Map<String, Budget> getUserBudgets(String username) {
        ensureUserExists(username);
        return userBudgets.get(username);
    }

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

        Map<String, Double> monthlyData = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> {
                            return t.getDate().substring(0, 7);
                        },
                        Collectors.summingDouble(Transaction::getAmount)
                ));

        return monthlyData.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        entry -> {
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

    public double checkAndGrantBudgetRewards(String username, UserManager userManager) {
        Map<String, Budget> budgets = userBudgets.getOrDefault(username, Collections.emptyMap());

        double totalPointsEarned = 0.0;

        for (Budget budget : budgets.values()) {
            String category = budget.getCategory();
            double spent = budget.getSpent();
            double limit = budget.getLimit();

            if (spent < limit) {
                double amountSaved = limit - spent;
                double points = amountSaved / 100.0;
                totalPointsEarned += points;

                System.out.println(String.format(
                        "Category: %s | Saved: %.2f | Points Earned: %.2f",
                        category, amountSaved, points));
            }
        }

        if (totalPointsEarned > 0) {
            userManager.addRewardPoints(username, (int) totalPointsEarned);
            System.out.println(String.format(
                    "%s earned %.2f reward points! (Total budgets checked: %d)",
                    username, totalPointsEarned, budgets.size()));
        } else {
            System.out.println(String.format(
                    "%s did not earn any reward points. (Over budget or no budgets)",
                    username));
        }

        return totalPointsEarned;
    }

    public BudgetAdherenceSummary getBudgetAdherenceSummary(String username) {
        Map<String, Budget> budgets = userBudgets.getOrDefault(username, Collections.emptyMap());

        int budgetsUnderLimit = 0;
        double totalSavings = 0.0;
        List<String> budgetStatus = new ArrayList<>();

        for (Budget budget : budgets.values()) {
            double spent = budget.getSpent();
            double limit = budget.getLimit();
            double savings = Math.max(0, limit - spent);
            double percentage = (spent / limit) * 100;

            budgetStatus.add(String.format("%s: %.2f/%.2f (%.1f%%)",
                    budget.getCategory(), spent, limit, percentage));

            if (spent <= limit) {
                budgetsUnderLimit++;
                totalSavings += savings;
            }
        }

        return new BudgetAdherenceSummary(
                budgets.size(),
                budgetsUnderLimit,
                totalSavings,
                budgetStatus
        );
    }

    public double removeExpenseAndCalculatePointAdjustment(String username, Transaction transaction) {
        BudgetAdherenceSummary oldSummary = getBudgetAdherenceSummary(username);
        double oldPoints = Math.floor(oldSummary.totalSavings / 100);

        removeExpense(username, transaction);


        BudgetAdherenceSummary newSummary = getBudgetAdherenceSummary(username);
        double newPoints = Math.floor(newSummary.totalSavings / 100);


        return oldPoints - newPoints;
    }

    public boolean exportUserData(String username, String filePath) {
        ensureUserExists(username);
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("Date,Type,Category,Description,Amount");

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

        saveTransactions();
        saveBudgets();

        ensureUserExists(username);
    }

    private void loadTransactions() {
        if (!Files.exists(Paths.get(TRANSACTIONS_CSV))) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(TRANSACTIONS_CSV))) {
            String line;
            reader.readLine(); 

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
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error saving transactions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadBudgets() {
        if (!Files.exists(Paths.get(BUDGETS_CSV))) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(BUDGETS_CSV))) {
            String line;
            reader.readLine(); 

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
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error saving budgets: " + e.getMessage());
            e.printStackTrace();
        }
    }


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

    public static class BudgetAdherenceSummary {
        public final int totalBudgets;
        public final int budgetsUnderLimit;
        public final double totalSavings;
        public final List<String> budgetStatuses;

        public BudgetAdherenceSummary(int totalBudgets, int budgetsUnderLimit,
                                      double totalSavings, List<String> budgetStatuses) {
            this.totalBudgets = totalBudgets;
            this.budgetsUnderLimit = budgetsUnderLimit;
            this.totalSavings = totalSavings;
            this.budgetStatuses = budgetStatuses;
        }
    }
}
