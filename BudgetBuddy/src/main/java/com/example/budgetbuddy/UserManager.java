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
    private static final String SAVED_CREDENTIALS_FILE = "saved_credentials.csv";
    private static final String USERS_HEADER = "username,pin_hash,qr_code,created_date,profile_picture,balance,income,expenses,savings_goal,reward_points,last_updated";
    private static final String SAVED_CREDS_HEADER = "identifier,pin_hash,last_login";
    private static final String DELIMITER = ",";

    private Map<String, User> users;
    private Map<String, SavedCredential> savedCredentials;

    public UserManager() {
        this.users = new HashMap<>();
        this.savedCredentials = new HashMap<>();

        loadUsers();
        loadSavedCredentials();

        if (users.isEmpty()) {
            registerUser("testuser", "1234");
        }
    }

    public boolean registerUser(String username, String pin) {
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

        User user = new User(username, pinHash, qrCode, createdDate, "", 0.0, 0.0, 0.0, 0.0, 0, new Date().toString());
        users.put(username, user);

        saveUsers();
        System.out.println("User registered successfully: " + username);
        return true;
    }

    public boolean authenticate(String username, String pin) {
        User user = users.get(username);
        if (user == null) {
            return false;
        }

        String enteredPinHash = hashPin(pin);
        return user.getPinHash().equals(enteredPinHash);
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

    public boolean updateProfilePicture(String username, String picturePath) {
        User user = users.get(username);
        if (user != null) {
            user.setProfilePicture(picturePath);
            saveUsers();
            return true;
        }
        return false;
    }

    public String getProfilePicture(String username) {
        User user = users.get(username);
        return user != null ? user.getProfilePicture() : "";
    }

    // --- SAVED CREDENTIALS METHODS ---

    public void saveCredentials(String username, String pin) {
        String pinHash = hashPin(pin);
        String lastLogin = new Date().toString();

        SavedCredential cred = new SavedCredential(username, pinHash, lastLogin);
        savedCredentials.put(username, cred);
        saveSavedCredentials();
    }

    public void removeSavedCredentials(String username) {
        savedCredentials.remove(username);
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

    public boolean authenticateWithSaved(String username) {
        SavedCredential saved = savedCredentials.get(username);
        if (saved == null) {
            return false;
        }

        User user = users.get(username);
        if (user == null) {
            return false;
        }

        return user.getPinHash().equals(saved.pinHash);
    }

    // --- USER ACCOUNT DATA METHODS ---

    public UserAccount getUserAccount(String username) {
        User user = users.get(username);
        if (user == null) {
            return null;
        }
        return new UserAccount(user);
    }

    public void updateBalance(String username, double balance) {
        User user = users.get(username);
        if (user != null) {
            user.setBalance(balance);
            user.setLastUpdated(new Date().toString());
            saveUsers();
        }
    }

    public void updateIncome(String username, double income) {
        User user = users.get(username);
        if (user != null) {
            user.setIncome(income);
            user.setLastUpdated(new Date().toString());
            saveUsers();
        }
    }

    public void updateExpenses(String username, double expenses) {
        User user = users.get(username);
        if (user != null) {
            user.setExpenses(expenses);
            user.setLastUpdated(new Date().toString());
            saveUsers();
        }
    }

    public void updateSavingsGoal(String username, double savingsGoal) {
        User user = users.get(username);
        if (user != null) {
            user.setSavingsGoal(savingsGoal);
            user.setLastUpdated(new Date().toString());
            saveUsers();
        }
    }

    public void updateUserAccount(String username, double balance, double income, double expenses, double savingsGoal) {
        User user = users.get(username);
        if (user != null) {
            user.setBalance(balance);
            user.setIncome(income);
            user.setExpenses(expenses);
            user.setSavingsGoal(savingsGoal);
            user.setLastUpdated(new Date().toString());
            saveUsers();
        }
    }

    public int getRewardPoints(String username) {
        User user = users.get(username);
        return user != null ? user.getRewardPoints() : 0;
    }

    public void setRewardPoints(String username, int points) {
        User user = users.get(username);
        if (user != null) {
            user.setRewardPoints(points);
            saveUsers();
        }
    }

    public void addRewardPoints(String username, int points) {
        User user = users.get(username);
        if (user != null) {
            user.setRewardPoints(user.getRewardPoints() + points);
            saveUsers();
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
                if (data.length >= 11) {
                    String username = data[0];
                    String pinHash = data[1];
                    String qrCode = data[2];
                    String createdDate = unescapeCSV(data[3]);
                    String profilePicture = unescapeCSV(data[4]);
                    double balance = Double.parseDouble(data[5]);
                    double income = Double.parseDouble(data[6]);
                    double expenses = Double.parseDouble(data[7]);
                    double savingsGoal = Double.parseDouble(data[8]);
                    int rewardPoints = Integer.parseInt(data[9]);
                    String lastUpdated = unescapeCSV(data[10]);

                    User user = new User(username, pinHash, qrCode, createdDate, profilePicture,
                            balance, income, expenses, savingsGoal, rewardPoints, lastUpdated);
                    users.put(username, user);
                }
            }
        } catch (IOException | NumberFormatException e) {
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
                    savedCredentials.put(identifier, cred);
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

    private static String escapeCSV(String value) {
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

    // --- NESTED USER CLASS ---

    private static class User {
        public final String username;
        private String pinHash;
        public final String qrCode;
        public final String createdDate;
        private String profilePicture;
        private double balance;
        private double income;
        private double expenses;
        private double savingsGoal;
        private int rewardPoints;
        private String lastUpdated;

        public User(String username, String pinHash, String qrCode, String createdDate,
                    String profilePicture, double balance, double income, double expenses,
                    double savingsGoal, int rewardPoints, String lastUpdated) {
            this.username = username;
            this.pinHash = pinHash;
            this.qrCode = qrCode;
            this.createdDate = createdDate;
            this.profilePicture = profilePicture != null ? profilePicture : "";
            this.balance = balance;
            this.income = income;
            this.expenses = expenses;
            this.savingsGoal = savingsGoal;
            this.rewardPoints = rewardPoints;
            this.lastUpdated = lastUpdated;
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

        public double getBalance() {
            return balance;
        }

        public void setBalance(double balance) {
            this.balance = balance;
        }

        public double getIncome() {
            return income;
        }

        public void setIncome(double income) {
            this.income = income;
        }

        public double getExpenses() {
            return expenses;
        }

        public void setExpenses(double expenses) {
            this.expenses = expenses;
        }

        public double getSavingsGoal() {
            return savingsGoal;
        }

        public void setSavingsGoal(double savingsGoal) {
            this.savingsGoal = savingsGoal;
        }

        public int getRewardPoints() {
            return rewardPoints;
        }

        public void setRewardPoints(int rewardPoints) {
            this.rewardPoints = rewardPoints;
        }

        public String getLastUpdated() {
            return lastUpdated;
        }

        public void setLastUpdated(String lastUpdated) {
            this.lastUpdated = lastUpdated;
        }

        public String toCSV() {
            return String.format("%s,%s,%s,%s,%s,%.2f,%.2f,%.2f,%.2f,%d,%s",
                    username,
                    pinHash,
                    qrCode,
                    escapeCSV(createdDate),
                    escapeCSV(profilePicture),
                    balance,
                    income,
                    expenses,
                    savingsGoal,
                    rewardPoints,
                    escapeCSV(lastUpdated));
        }

        private String escapeCSV(String value) {
            return UserManager.escapeCSV(value);
        }
    }

    // --- NESTED USER ACCOUNT CLASS (Wrapper for compatibility) ---

    public static class UserAccount {
        private final User user;

        public UserAccount(User user) {
            this.user = user;
        }

        public String getUsername() {
            return user.username;
        }

        public double getBalance() {
            return user.getBalance();
        }

        public double getIncome() {
            return user.getIncome();
        }

        public double getExpenses() {
            return user.getExpenses();
        }

        public double getSavingsGoal() {
            return user.getSavingsGoal();
        }

        public String getLastUpdated() {
            return user.getLastUpdated();
        }

        public void setBalance(double balance) {
            user.setBalance(balance);
        }

        public void setIncome(double income) {
            user.setIncome(income);
        }

        public void setExpenses(double expenses) {
            user.setExpenses(expenses);
        }

        public void setSavingsGoal(double savingsGoal) {
            user.setSavingsGoal(savingsGoal);
        }

        public void setLastUpdated(String lastUpdated) {
            user.setLastUpdated(lastUpdated);
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
            return UserManager.escapeCSV(value);
        }
    }
}
