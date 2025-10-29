package com.example.budgetbuddy;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UserManager {

    private static final String USERS_CSV_FILE = "users.csv";
    private static final String ACCOUNTS_CSV_FILE = "user_accounts.csv";
    private static final String USERS_HEADER = "username,pin_hash,qr_code,email,created_date";
    private static final String ACCOUNTS_HEADER = "username,balance,income,expenses,savings_goal,last_updated";
    private static final String DELIMITER = ",";
    private Map<String, User> users;
    private Map<String, UserAccount> userAccounts;

    public UserManager() {
        this.users = new HashMap<>();
        this.userAccounts = new HashMap<>();
        loadUsers();
        loadUserAccounts();

        // Ensure a test user exists if the file is empty (for first run)
        if (users.isEmpty()) {
            registerUser("testuser", "1234", "test@example.com");
        }
    }

    /**
     * Register a new user with username, PIN, and optional email
     */
    public boolean registerUser(String username, String pin, String email) {
        if (users.containsKey(username)) {
            System.out.println("User already exists: " + username);
            return false;
        }
        if (!pin.matches("\\d{4}")) {
            System.out.println("PIN must be exactly 4 digits.");
            return false;
        }

        String pinHash = hashPin(pin);
        String qrCode = UUID.randomUUID().toString();
        String createdDate = new Date().toString();

        User user = new User(username, pinHash, qrCode, email, createdDate);
        users.put(username, user);

        // Create a new account for the user with default values
        UserAccount account = new UserAccount(username, 0.0, 0.0, 0.0, 0.0, new Date().toString());
        userAccounts.put(username, account);

        saveUsers();
        saveUserAccounts();
        System.out.println("User registered successfully: " + username);
        return true;
    }

    /**
     * Authenticate user with username and PIN
     */
    public boolean authenticate(String username, String pin) {
        if (!users.containsKey(username)) {
            return false;
        }
        User user = users.get(username);
        String enteredPinHash = hashPin(pin);
        return user.getPinHash().equals(enteredPinHash);
    }

    /**
     * Authenticate user using a scanned QR code string
     */
    public String authenticateQR(String qrCode) {
        for (User user : users.values()) {
            if (user.getQrCode().equals(qrCode)) {
                return user.username;
            }
        }
        return null;
    }

    /**
     * Changes the PIN for the specified user.
     */
    public boolean changePin(String username, String newPin) {
        if (!users.containsKey(username)) {
            return false;
        }

        if (!newPin.matches("\\d{4}")) {
            System.err.println("New PIN is not exactly 4 digits.");
            return false;
        }

        User user = users.get(username);
        String newPinHash = hashPin(newPin);

        // Update the PIN hash and save the change
        user.setPinHash(newPinHash);
        saveUsers();

        return true;
    }

    public String getQRCode(String username) {
        User user = users.get(username);
        return user != null ? user.getQrCode() : null;
    }

    // --- USER ACCOUNT DATA METHODS ---

    /**
     * Get the account data for a user
     */
    public UserAccount getUserAccount(String username) {
        return userAccounts.get(username);
    }

    /**
     * Update user account balance
     */
    public void updateBalance(String username, double balance) {
        UserAccount account = userAccounts.get(username);
        if (account != null) {
            account.setBalance(balance);
            account.setLastUpdated(new Date().toString());
            saveUserAccounts();
        }
    }

    /**
     * Update user income
     */
    public void updateIncome(String username, double income) {
        UserAccount account = userAccounts.get(username);
        if (account != null) {
            account.setIncome(income);
            account.setLastUpdated(new Date().toString());
            saveUserAccounts();
        }
    }

    /**
     * Update user expenses
     */
    public void updateExpenses(String username, double expenses) {
        UserAccount account = userAccounts.get(username);
        if (account != null) {
            account.setExpenses(expenses);
            account.setLastUpdated(new Date().toString());
            saveUserAccounts();
        }
    }

    /**
     * Update user savings goal
     */
    public void updateSavingsGoal(String username, double savingsGoal) {
        UserAccount account = userAccounts.get(username);
        if (account != null) {
            account.setSavingsGoal(savingsGoal);
            account.setLastUpdated(new Date().toString());
            saveUserAccounts();
        }
    }

    /**
     * Update all account data at once
     */
    public void updateUserAccount(String username, double balance, double income, double expenses, double savingsGoal) {
        UserAccount account = userAccounts.get(username);
        if (account != null) {
            account.setBalance(balance);
            account.setIncome(income);
            account.setExpenses(expenses);
            account.setSavingsGoal(savingsGoal);
            account.setLastUpdated(new Date().toString());
            saveUserAccounts();
        }
    }

    // --- PERSISTENCE METHODS ---

    private void loadUsers() {
        if (!Files.exists(Paths.get(USERS_CSV_FILE))) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_CSV_FILE))) {
            String line;
            reader.readLine(); // Skip header line
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(DELIMITER, -1);
                if (data.length >= 5) {
                    String username = data[0];
                    String pinHash = data[1];
                    String qrCode = data[2];
                    String email = unescapeCSV(data[3]);
                    String createdDate = unescapeCSV(data[4]);

                    User user = new User(username, pinHash, qrCode, email, createdDate);
                    users.put(username, user);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveUsers() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(USERS_CSV_FILE))) {
            writer.println(USERS_HEADER);
            for (User user : users.values()) {
                writer.println(user.toCSV());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadUserAccounts() {
        if (!Files.exists(Paths.get(ACCOUNTS_CSV_FILE))) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(ACCOUNTS_CSV_FILE))) {
            String line;
            reader.readLine(); // Skip header line
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(DELIMITER, -1);
                if (data.length >= 6) {
                    String username = data[0];
                    double balance = Double.parseDouble(data[1]);
                    double income = Double.parseDouble(data[2]);
                    double expenses = Double.parseDouble(data[3]);
                    double savingsGoal = Double.parseDouble(data[4]);
                    String lastUpdated = unescapeCSV(data[5]);

                    UserAccount account = new UserAccount(username, balance, income, expenses, savingsGoal, lastUpdated);
                    userAccounts.put(username, account);
                }
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void saveUserAccounts() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(ACCOUNTS_CSV_FILE))) {
            writer.println(ACCOUNTS_HEADER);
            for (UserAccount account : userAccounts.values()) {
                writer.println(account.toCSV());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- UTILITY METHODS ---

    /**
     * Securely hash the PIN.
     */
    private String hashPin(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            // Fallback for environments without SHA-256 support
            return pin;
        }
    }

    private String unescapeCSV(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            // Remove surrounding quotes and handle escaped quotes
            value = value.substring(1, value.length() - 1).replace("\"\"", "\"");
        }
        return value;
    }

    // --- NESTED USER CLASS ---

    private static class User {
        public final String username;
        private String pinHash;
        public final String qrCode;
        public final String email;
        public final String createdDate;

        public User(String username, String pinHash, String qrCode, String email, String createdDate) {
            this.username = username;
            this.pinHash = pinHash;
            this.qrCode = qrCode;
            this.email = email != null ? email : "";
            this.createdDate = createdDate;
        }

        public void setPinHash(String pinHash) {
            this.pinHash = pinHash;
        }

        public String getPinHash() {
            return pinHash;
        }

        public String getQrCode() {
            return qrCode;
        }

        public String toCSV() {
            String escapedEmail = escapeCSV(email);
            String escapedDate = escapeCSV(createdDate);

            return String.format("%s,%s,%s,%s,%s",
                    username,
                    pinHash,
                    qrCode,
                    escapedEmail,
                    escapedDate);
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
    }

    // --- NESTED USER ACCOUNT CLASS ---

    public static class UserAccount {
        private final String username;
        private double balance;
        private double income;
        private double expenses;
        private double savingsGoal;
        private String lastUpdated;

        public UserAccount(String username, double balance, double income, double expenses, double savingsGoal, String lastUpdated) {
            this.username = username;
            this.balance = balance;
            this.income = income;
            this.expenses = expenses;
            this.savingsGoal = savingsGoal;
            this.lastUpdated = lastUpdated;
        }

        // Getters
        public String getUsername() {
            return username;
        }

        public double getBalance() {
            return balance;
        }

        public double getIncome() {
            return income;
        }

        public double getExpenses() {
            return expenses;
        }

        public double getSavingsGoal() {
            return savingsGoal;
        }

        public String getLastUpdated() {
            return lastUpdated;
        }

        // Setters
        public void setBalance(double balance) {
            this.balance = balance;
        }

        public void setIncome(double income) {
            this.income = income;
        }

        public void setExpenses(double expenses) {
            this.expenses = expenses;
        }

        public void setSavingsGoal(double savingsGoal) {
            this.savingsGoal = savingsGoal;
        }

        public void setLastUpdated(String lastUpdated) {
            this.lastUpdated = lastUpdated;
        }

        public String toCSV() {
            return String.format("%s,%.2f,%.2f,%.2f,%.2f,%s",
                    username,
                    balance,
                    income,
                    expenses,
                    savingsGoal,
                    escapeCSV(lastUpdated));
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
    }
}
