package org.example.budgetbuddies;

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
    }
    
    /**
     * Register a new user with username, PIN, and optional email
     */
    public boolean registerUser(String username, String pin, String email) {
        if (users.containsKey(username)) {
            System.out.println("User already exists: " + username);
            return false;
        }
        
        String pinHash = hashPin(pin);
        String qrCode = generateQRCode(username);
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
        User user = users.get(username);
        if (user == null) {
            System.out.println("User not found: " + username);
            return false;
        }
        
        String pinHash = hashPin(pin);
        boolean authenticated = pinHash.equals(user.getPinHash());
        
        if (authenticated) {
            System.out.println("Authentication successful for: " + username);
        } else {
            System.out.println("Authentication failed for: " + username);
        }
        
        return authenticated;
    }
    
    /**
     * Authenticate user with QR code
     */
    public String authenticateQR(String qrCode) {
        for (Map.Entry<String, User> entry : users.entrySet()) {
            if (entry.getValue().getQrCode().equals(qrCode)) {
                System.out.println("QR authentication successful for: " + entry.getKey());
                return entry.getKey();
            }
        }
        System.out.println("QR authentication failed for code: " + qrCode);
        return null;
    }
    
    /**
     * Get QR code for a specific user
     */
    public String getQRCode(String username) {
        User user = users.get(username);
        return user != null ? user.getQrCode() : null;
    }
    
    /**
     * Get all registered usernames (for debugging)
     */
    public Set<String> getAllUsernames() {
        return users.keySet();
    }
    
    /**
     * Load users from CSV file
     */
    private void loadUsers() {
        File file = new File(CSV_FILE);
        
        if (!file.exists()) {
            System.out.println("CSV file does not exist. Creating new file on first registration.");
            return;
        }
        
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            
            String line = br.readLine(); // Skip header
            
            if (line == null || !line.startsWith("username")) {
                System.out.println("Invalid CSV header. Expected: " + HEADER);
                return;
            }
            
            int lineNumber = 1;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }
                
                String[] parts = parseCsvLine(line);
                
                if (parts.length >= 5) {
                    try {
                        String username = parts[0].trim();
                        String pinHash = parts[1].trim();
                        String qrCode = parts[2].trim();
                        String email = parts[3].trim();
                        String createdDate = parts[4].trim();
                        
                        if (username.isEmpty() || pinHash.isEmpty() || qrCode.isEmpty()) {
                            System.out.println("Skipping invalid entry at line " + lineNumber);
                            continue;
                        }
                        
                        User user = new User(username, pinHash, qrCode, email, createdDate);
                        users.put(username, user);
                    } catch (Exception e) {
                        System.err.println("Error parsing line " + lineNumber + ": " + e.getMessage());
                    }
                } else {
                    System.out.println("Skipping malformed line " + lineNumber + ": insufficient columns");
                }
            }
            
            System.out.println("✓ Loaded " + users.size() + " user(s) from CSV");
            
        } catch (IOException e) {
            System.err.println("Error loading users from CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Save all users to CSV file
     */
    private void saveUsers() {
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(CSV_FILE), StandardCharsets.UTF_8))) {
            
            // Write header
            bw.write(HEADER);
            bw.newLine();
            
            // Write each user
            for (User user : users.values()) {
                bw.write(user.toCSV());
                bw.newLine();
            }
            
            System.out.println("✓ Saved " + users.size() + " user(s) to CSV");
            
        } catch (IOException e) {
            System.err.println("Error saving users to CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Parse a CSV line handling quoted fields and commas within quotes
     */
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(field.toString());
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }
        
        result.add(field.toString());
        return result.toArray(new String[0]);
    }
    
    /**
     * Hash a PIN using SHA-256
     */
    private String hashPin(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pin.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
    
    /**
     * Generate a unique QR code for a user
     */
    private String generateQRCode(String username) {
        // Generate a unique QR code based on username and timestamp
        String data = username + "_" + System.currentTimeMillis() + "_" + Math.random();
        String hash = hashPin(data);
        return "QR-" + hash.substring(0, 16).toUpperCase();
    }
    
    /**
     * Export users to a backup file
     */
    public boolean exportBackup(String filename) {
        try {
            Files.copy(Paths.get(CSV_FILE), Paths.get(filename));
            System.out.println("Backup created: " + filename);
            return true;
        } catch (IOException e) {
            System.err.println("Error creating backup: " + e.getMessage());
            return false;
        }
    }
    
    // Inner class to represent a User
    private static class User {
        private final String username;
        private final String pinHash;
        private final String qrCode;
        private final String email;
        private final String createdDate;
        
        public User(String username, String pinHash, String qrCode, String email, String createdDate) {
            this.username = username;
            this.pinHash = pinHash;
            this.qrCode = qrCode;
            this.email = email != null ? email : "";
            this.createdDate = createdDate;
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
        
        @Override
        public String toString() {
            return String.format("User{username='%s', qrCode='%s', email='%s', created='%s'}", 
                username, qrCode, email, createdDate);
        }
    }
}