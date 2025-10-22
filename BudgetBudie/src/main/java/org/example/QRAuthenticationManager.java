package org.example;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;

public class QRAuthenticationManager {
    private static final String USER_MAPPING_FILE = "user_qr_mapping.properties";
    private static final String QR_CODES_DIRECTORY = "qr_codes/";
    private static final String CSV_DIRECTORY = "user_data/";
    private Properties userMappings;

    public QRAuthenticationManager() {
        userMappings = new Properties();
        loadUserMappings();
        createDirectories();
    }

    private void createDirectories() {
        new File(QR_CODES_DIRECTORY).mkdirs();
        new File(CSV_DIRECTORY).mkdirs();
    }

    private void loadUserMappings() {
        File mappingFile = new File(USER_MAPPING_FILE);
        if (mappingFile.exists()) {
            try (FileInputStream fis = new FileInputStream(mappingFile)) {
                userMappings.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveUserMappings() {
        try (FileOutputStream fos = new FileOutputStream(USER_MAPPING_FILE)) {
            userMappings.store(fos, "QR Code to User CSV Mapping");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate a unique QR code for a user
     * @param username User's username
     * @return Path to the generated QR code image
     */
    public String generateUserQRCode(String username) throws WriterException, IOException {
        // Generate unique ID for this user
        String uniqueId = UUID.randomUUID().toString();

        // Store mapping: QR content -> CSV filename
        String csvFileName = username + "_budget.csv";
        userMappings.setProperty(uniqueId, csvFileName);
        saveUserMappings();

        // Generate QR code
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(uniqueId, BarcodeFormat.QR_CODE, 300, 300);

        // Save QR code as PNG
        String qrCodePath = QR_CODES_DIRECTORY + username + "_qr.png";
        Path path = FileSystems.getDefault().getPath(qrCodePath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);

        // Create corresponding CSV file if it doesn't exist
        createUserCSV(csvFileName, username);

        return qrCodePath;
    }

    /**
     * Create a new CSV file for the user with headers
     */
    private void createUserCSV(String csvFileName, String username) {
        File csvFile = new File(CSV_DIRECTORY + csvFileName);
        if (!csvFile.exists()) {
            try (PrintWriter writer = new PrintWriter(csvFile)) {
                // Add CSV headers for budget tracking
                writer.println("Date,Category,Description,Amount,Type");
                writer.println("# Budget Buddy Data for: " + username);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Decode QR code from image
     * @param qrCodeImage The QR code image to decode
     * @return Decoded string content
     */
    public String decodeQRCode(BufferedImage qrCodeImage) throws NotFoundException {
        LuminanceSource source = new BufferedImageLuminanceSource(qrCodeImage);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Result result = new MultiFormatReader().decode(bitmap);
        return result.getText();
    }

    /**
     * Authenticate user and get their CSV file path
     * @param qrContent Content decoded from QR code
     * @return Path to user's CSV file, or null if not found
     */
    public String authenticateAndGetCSV(String qrContent) {
        String csvFileName = userMappings.getProperty(qrContent);
        if (csvFileName != null) {
            String csvPath = CSV_DIRECTORY + csvFileName;
            File csvFile = new File(csvPath);
            if (csvFile.exists()) {
                return csvPath;
            }
        }
        return null;
    }

    /**
     * Get list of all registered users
     */
    public List<String> getRegisteredUsers() {
        List<String> users = new ArrayList<>();
        for (Object value : userMappings.values()) {
            String csvFileName = (String) value;
            String username = csvFileName.replace("_budget.csv", "");
            users.add(username);
        }
        return users;
    }

    /**
     * Delete user and their associated data
     */
    public boolean deleteUser(String username) {
        String csvFileName = username + "_budget.csv";
        String qrCodePath = QR_CODES_DIRECTORY + username + "_qr.png";

        // Find and remove mapping
        String keyToRemove = null;
        for (Map.Entry<Object, Object> entry : userMappings.entrySet()) {
            if (entry.getValue().equals(csvFileName)) {
                keyToRemove = (String) entry.getKey();
                break;
            }
        }

        if (keyToRemove != null) {
            userMappings.remove(keyToRemove);
            saveUserMappings();

            // Delete files
            new File(CSV_DIRECTORY + csvFileName).delete();
            new File(qrCodePath).delete();
            return true;
        }
        return false;
    }
}