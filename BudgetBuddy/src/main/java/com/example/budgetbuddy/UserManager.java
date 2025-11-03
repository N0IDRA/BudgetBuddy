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
    private static final String SAVED_CREDENTIALS_FILE = "saved_credentials.csv";
    private static final String USERS_HEADER = "username,pin_hash,qr_code,email,created_date,profile_picture";
    private static final String ACCOUNTS_HEADER = "username,balance,income,expenses,savings_goal,last_updated";
    private static final String SAVED_CREDS_HEADER = "identifier,pin_hash,last_login";
    private static final String DELIMITER = ",";
    private Map<String, User> users;
    private Map<String, User> usersByEmail;
    private Map<String, UserAccount> userAccounts;
    private Map<String, SavedCredential> savedCredentials;

    public UserManager() {
        this.users = new HashMap<>();
        this.usersByEmail = new HashMap<>();
        this.userAccounts = new HashMap<>();
        this.savedCredentials = new HashMap<>();
        loadUsers();
        loadUserAccounts();
        loadSavedCredentials();

        if (users.isEmpty()) {
            registerUser("testuser", "1234", "test@example.com");
        }
    }

    public boolean registerUser(String username, String pin, String email) {
        if (users.containsKey(username)) {
            System.out.println("User already exists: " + username);
            return false;
        }

        if (email != null && !email.isEmpty() && usersByEmail.containsKey(email.toLowerCase())) {
            System.out.println("Email already exists: " + email);
            return false;
        }

        if (!pin.matches("\\d{4}")) {
            System.out.println("PIN must be exactly 4 digits.");
            return false;
        }

        String pinHash = hashPin(pin);
        String qrCode = UUID.randomUUID().toString();
        String createdDate = new Date().toString();

        User user = new User(username, pinHash, qrCode, email, createdDate, "");
        users.put(username, user);

        if (email != null && !email.isEmpty()) {
            usersByEmail.put(email.toLowerCase(), user);
        }

        UserAccount account = new UserAccount(username, 0.0, 0.0, 0.0, 0.0, new Date().toString());
        userAccounts.put(username, account);

        saveUsers();
        saveUserAccounts();
        System.out.println("User registered successfully: " + username);
        return true;
    }

    public boolean authenticate(String identifier, String pin) {
        User user = getUserByIdentifier(identifier);
        if (user == null) {
            return false;
        }

        String enteredPinHash = hashPin(pin);
        return user.getPinHash().equals(enteredPinHash);
    }

    private User getUserByIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return null;
        }

        if (users.containsKey(identifier)) {
            return users.get(identifier);
        }

        return usersByEmail.get(identifier.toLowerCase());
    }

    public String getUsernameFromIdentifier(String identifier) {
        User user = getUserByIdentifier(identifier);
        return user != null ? user.username : null;
    }

    public String authenticateQR(String qrCode) {
        for (User user : users.values()) {
            if (user.getQrCode().equals(qrCode)) {
                return user.username;
            }
        }
        return null;
    }

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

        user.setPinHash(newPinHash);
        saveUsers();

        return true;
    }

    public String getQRCode(String username) {
        User user = users.get(username);
        return user != null ? user.getQrCode() : null;
    }

    // --- PROFILE PICTURE METHODS ---

    /**
     * Update user's profile picture path
     */
    public boolean updateProfilePicture(String username, String picturePath) {
        User user = users.get(username);
        if (user != null) {
            user.setProfilePicture(picturePath);
            saveUsers();
            return true;
        }
        return false;
    }

    /**
     * Get user's profile picture path
     */
    public String getProfilePicture(String username) {
        User user = users.get(username);
        return user != null ? user.getProfilePicture() : "";
    }

    // --- SAVED CREDENTIALS METHODS ---

    public void saveCredentials(String identifier, String pin) {
        String pinHash = hashPin(pin);
        String lastLogin = new Date().toString();

        SavedCredential cred = new SavedCredential(identifier, pinHash, lastLogin);
        savedCredentials.put(identifier.toLowerCase(), cred);
        saveSavedCredentials();
    }

    public void removeSavedCredentials(String identifier) {
        savedCredentials.remove(identifier.toLowerCase());
        saveSavedCredentials();
    }

    public List<String> getSavedCredentialIdentifiers() {
        List<String> identifiers = new ArrayList<>(savedCredentials.keySet());
        identifiers.sort((a, b) -> {
            SavedCredential credB = savedCredentials.get(b);
            SavedCredential credA = savedCredentials.get(a);
            return credB.lastLogin.compareTo(credA.lastLogin);
        });
        return identifiers;
    }

    public boolean authenticateWithSaved(String identifier) {
        SavedCredential saved = savedCredentials.get(identifier.toLowerCase());
        if (saved == null) {
            return false;
        }

        User user = getUserByIdentifier(identifier);
        if (user == null) {
            return false;
        }

        return user.getPinHash().equals(saved.pinHash);
    }

    // --- USER ACCOUNT DATA METHODS ---

    public UserAccount getUserAccount(String username) {
        return userAccounts.get(username);
    }

    public void updateBalance(String username, double balance) {
        UserAccount account = userAccounts.get(username);
        if (account != null) {
            account.setBalance(balance);
            account.setLastUpdated(new Date().toString());
            saveUserAccounts();
        }
    }

    public void updateIncome(String username, double income) {
        UserAccount account = userAccounts.get(username);
        if (account != null) {
            account.setIncome(income);
            account.setLastUpdated(new Date().toString());
            saveUserAccounts();
        }
    }

    public void updateExpenses(String username, double expenses) {
        UserAccount account = userAccounts.get(username);
        if (account != null) {
            account.setExpenses(expenses);
            account.setLastUpdated(new Date().toString());
            saveUserAccounts();
        }
    }

    public void updateSavingsGoal(String username, double savingsGoal) {
        UserAccount account = userAccounts.get(username);
        if (account != null) {
            account.setSavingsGoal(savingsGoal);
            account.setLastUpdated(new Date().toString());
            saveUserAccounts();
        }
    }

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
                    String profilePicture = data.length > 5 ? unescapeCSV(data[5]) : "";

                    User user = new User(username, pinHash, qrCode, email, createdDate, profilePicture);
                    users.put(username, user);

                    if (email != null && !email.isEmpty()) {
                        usersByEmail.put(email.toLowerCase(), user);
                    }
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

    private void loadSavedCredentials() {
        if (!Files.exists(Paths.get(SAVED_CREDENTIALS_FILE))) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(SAVED_CREDENTIALS_FILE))) {
            String line;
            reader.readLine(); // Skip header line
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(DELIMITER, -1);
                if (data.length >= 3) {
                    String identifier = data[0];
                    String pinHash = data[1];
                    String lastLogin = unescapeCSV(data[2]);

                    SavedCredential cred = new SavedCredential(identifier, pinHash, lastLogin);
                    savedCredentials.put(identifier.toLowerCase(), cred);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveSavedCredentials() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(SAVED_CREDENTIALS_FILE))) {
            writer.println(SAVED_CREDS_HEADER);
            for (SavedCredential cred : savedCredentials.values()) {
                writer.println(cred.toCSV());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- UTILITY METHODS ---

    private String hashPin(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            return pin;
        }
    }

    private String unescapeCSV(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
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
        private String profilePicture;

        public User(String username, String pinHash, String qrCode, String email, String createdDate, String profilePicture) {
            this.username = username;
            this.pinHash = pinHash;
            this.qrCode = qrCode;
            this.email = email != null ? email : "";
            this.createdDate = createdDate;
            this.profilePicture = profilePicture != null ? profilePicture : "";
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

        public void setProfilePicture(String profilePicture) {
            this.profilePicture = profilePicture != null ? profilePicture : "";
        }

        public String getProfilePicture() {
            return profilePicture;
        }

        public String toCSV() {
            String escapedEmail = escapeCSV(email);
            String escapedDate = escapeCSV(createdDate);
            String escapedPicture = escapeCSV(profilePicture);

            return String.format("%s,%s,%s,%s,%s,%s",
                    username,
                    pinHash,
                    qrCode,
                    escapedEmail,
                    escapedDate,
                    escapedPicture);
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

    // --- NESTED SAVED CREDENTIAL CLASS ---

    private static class SavedCredential {
        public final String identifier;
        public final String pinHash;
        public final String lastLogin;

        public SavedCredential(String identifier, String pinHash, String lastLogin) {
            this.identifier = identifier;
            this.pinHash = pinHash;
            this.lastLogin = lastLogin;
        }

        public String toCSV() {
            return String.format("%s,%s,%s",
                    identifier,
                    pinHash,
                    escapeCSV(lastLogin));
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