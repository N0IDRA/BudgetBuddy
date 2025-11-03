package com.example.budgetbuddy;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Handles scanning and parsing QR codes from receipts.
 * Supported format (pipe-delimited):
 * MERCHANT|DATE|AMOUNT|CATEGORY|ITEMS
 * Example: "Jollibee|2025-11-02|250.50|Food|Chickenjoy, Fries"
 */
public class ReceiptQRScanner {

    public static class ReceiptData {
        private String merchant;
        private String date;
        private double amount;
        private String category;
        private String items;

        public ReceiptData(String merchant, String date, double amount, String category, String items) {
            this.merchant = merchant;
            this.date = date;
            this.amount = amount;
            this.category = category;
            this.items = items;
        }

        public String getMerchant() { return merchant; }
        public String getDate() { return date; }
        public double getAmount() { return amount; }
        public String getCategory() { return category; }
        public String getItems() { return items; }

        public String getDescription() {
            return merchant + (items != null && !items.isEmpty() ? " - " + items : "");
        }
    }


    public static ReceiptData parseReceiptQR(String qrData) {
        try {
            // Remove any whitespace and split by pipe
            String[] parts = qrData.trim().split("\\|");

            if (parts.length >= 3) {
                String merchant = parts[0].trim();

                // Parse date
                String date;
                try {
                    date = parts[1].trim();
                    // Validate date format
                    LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (Exception e) {
                    date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
                }

                // Parse amount
                double amount;
                try {
                    amount = Double.parseDouble(parts[2].trim().replace(",", ""));
                } catch (NumberFormatException e) {
                    return null; // Invalid amount
                }

                // Parse category (default to Shopping if not provided)
                String category = parts.length > 3 ? parts[3].trim() : "Shopping";

                // Parse items (optional)
                String items = parts.length > 4 ? parts[4].trim() : "";

                return new ReceiptData(merchant, date, amount, category, items);
            }
        } catch (Exception e) {
            System.err.println("Error parsing receipt QR: " + e.getMessage());
        }

        return null; // Parsing failed
    }

    /**
     * Scan QR code from BufferedImage
     */
    public static String scanQRFromImage(BufferedImage image) throws NotFoundException {
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Result result = new MultiFormatReader().decode(bitmap);
        return result.getText();
    }

    /**
     * Get available expense categories
     */
    public static String[] getExpenseCategories() {
        return new String[]{
                "Food", "Transportation", "Entertainment",
                "Shopping", "Bills", "Healthcare", "Other"
        };
    }

    /**
     * Generate a sample receipt QR code string (for testing)
     */
    public static String generateSampleReceiptQR(String merchant, double amount, String category) {
        return String.format("%s|%s|%.2f|%s",
                merchant,
                LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                amount,
                category
        );
    }
}