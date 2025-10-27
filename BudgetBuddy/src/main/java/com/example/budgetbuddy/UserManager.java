package com.example.budgetbuddy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserManager {
    // Stores: username -> UserDetails (PIN, QR Code, Email)
    private final Map<String, Map<String, String>> users;

    public UserManager() {
        this.users = new HashMap<>();
        // Seed initial data for testing
        registerUser("testuser", "1234", "test@example.com");
    }

    public boolean registerUser(String username, String pin, String email) {
        if (users.containsKey(username)) {
            return false; // User already exists
        }
        if (!pin.matches("\\d{4}")) {
            // PIN must be 4 digits
            return false;
        }

        Map<String, String> details = new HashMap<>();
        details.put("pin", pin);
        details.put("email", email);
        // The QR code is a unique identifier tied to the user
        details.put("qrCode", UUID.randomUUID().toString());

        users.put(username, details);
        return true;
    }

    public boolean authenticate(String username, String pin) {
        if (users.containsKey(username)) {
            return users.get(username).get("pin").equals(pin);
        }
        return false;
    }

    public String authenticateQR(String qrCode) {
        for (Map.Entry<String, Map<String, String>> entry : users.entrySet()) {
            if (entry.getValue().get("qrCode").equals(qrCode)) {
                return entry.getKey(); // Return username
            }
        }
        return null;
    }

    public String getQRCode(String username) {
        return users.getOrDefault(username, Map.of()).get("qrCode");
    }

    public boolean changePin(String username, String newPin) {
        if (users.containsKey(username) && newPin.matches("\\d{4}")) {
            users.get(username).put("pin", newPin);
            return true;
        }
        return false;
    }
}