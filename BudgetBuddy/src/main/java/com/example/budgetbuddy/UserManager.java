package com.example.budgetbuddy;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UserManager {

    private static final String CSV_FILE = "users.csv";
    private static final String HEADER = "username,pin_hash,qr_code,email,created_date";
    private static final String DELIMITER = ",";
    private Map<String, User> users;

    public UserManager() {
        this.users = new HashMap<>();
        loadUsers();

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

        saveUsers();
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
     * **FIXED METHOD:** Changes the PIN for the specified user.
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

    // --- PERSISTENCE METHODS ---

    private void loadUsers() {
        if (!Files.exists(Paths.get(CSV_FILE))) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(CSV_FILE))) {
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
        try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_FILE))) {
            writer.println(HEADER);
            for (User user : users.values()) {
                writer.println(user.toCSV());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- UTILITY METHODS ---

    /**
     * Securely hash the PIN. (Placeholder uses simple return for portability).
     */
    private String hashPin(String pin) {
        // For production, you must use a strong hashing mechanism (e.g., bcrypt, PBKDF2).
        // For a simple Java project matching tutorial styles, we use a basic SHA-256 or return the PIN itself
        // for ease of debugging, or a simple SHA-256 if imports are available.

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
        private String pinHash; // Needs to be mutable for pin changes
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

        // --- NEW/UPDATED METHODS TO FIX ERRORS ---

        // Crucial setter to allow pin changes
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
            // Escape fields that might contain commas
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

            // If the value contains comma, quote, or newline, wrap in quotes
            if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                return "\"" + value.replace("\"", "\"\"") + "\"";
            }

            return value;
        }
        // ... (other methods like toString if they exist)
    }
}