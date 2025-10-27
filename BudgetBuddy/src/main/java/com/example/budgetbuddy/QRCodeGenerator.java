package com.example.budgetbuddy;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class QRCodeGenerator {

    public static BufferedImage generateQRCodeForDisplay(String text) throws WriterException, IOException {
        int width = 300;
        int height = 300;
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        // This utility class from zxing-j2se converts the BitMatrix to a BufferedImage
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }
}