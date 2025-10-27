package com.example.budgetbuddy;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;


public class QRCodeGenerator {

    private static final int DEFAULT_SIZE = 300;
    private static final String DEFAULT_FORMAT = "PNG";

    /**
     * Generate a QR code image from text and save it to a file
     *
     * @param text The text to encode in the QR code
     * @param width Width of the QR code image
     * @param height Height of the QR code image
     * @param filePath Path where the QR code image will be saved
     * @throws WriterException If encoding fails
     * @throws IOException If file writing fails
     */
    public static void generateQRCodeImage(String text, int width, int height, String filePath)
            throws WriterException, IOException {

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

        Path path = FileSystems.getDefault().getPath(filePath);
        MatrixToImageWriter.writeToPath(bitMatrix, DEFAULT_FORMAT, path);

        System.out.println("✓ QR Code generated: " + filePath);
    }

    /**
     * Generate a QR code BufferedImage from text (for display in JavaFX)
     *
     * @param text The text to encode in the QR code
     * @param width Width of the QR code image
     * @param height Height of the QR code image
     * @return BufferedImage containing the QR code
     * @throws WriterException If encoding fails
     */
    public static BufferedImage generateQRCodeImage(String text, int width, int height)
            throws WriterException {

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    /**
     * Generate a QR code with default size (300x300)
     *
     * @param text The text to encode
     * @param filePath Path where the QR code will be saved
     * @throws WriterException If encoding fails
     * @throws IOException If file writing fails
     */
    public static void generateQRCodeImage(String text, String filePath)
            throws WriterException, IOException {
        generateQRCodeImage(text, DEFAULT_SIZE, DEFAULT_SIZE, filePath);
    }

    /**
     * Generate a QR code for a user account
     *
     * @param username Username for the account
     * @param qrCode The unique QR code string
     * @param outputDirectory Directory to save the QR code image
     * @return Path to the generated QR code image
     * @throws WriterException If encoding fails
     * @throws IOException If file writing fails
     */
    public static String generateUserQRCode(String username, String qrCode, String outputDirectory)
            throws WriterException, IOException {

        // Create output directory if it doesn't exist
        File dir = new File(outputDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = username + "_QRCode.png";
        String filePath = outputDirectory + File.separator + fileName;

        generateQRCodeImage(qrCode, 400, 400, filePath);

        return filePath;
    }

    /**
     * Generate QR code and return as BufferedImage for immediate display
     *
     * @param qrCode The QR code text to encode
     * @return BufferedImage of the QR code
     */
    public static BufferedImage generateQRCodeForDisplay(String qrCode) {
        try {
            return generateQRCodeImage(qrCode, 300, 300);
        } catch (WriterException e) {
            System.err.println("Error generating QR code: " + e.getMessage());
            return null;
        }
    }
}