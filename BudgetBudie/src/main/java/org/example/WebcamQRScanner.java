package org.example;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.NotFoundException;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class WebcamQRScanner extends JDialog implements Runnable {
    private Webcam webcam;
    private WebcamPanel webcamPanel;
    private QRAuthenticationManager authManager;
    private volatile boolean running = true;
    private Executor executor = Executors.newSingleThreadExecutor();

    private ScanCompleteCallback callback;

    public interface ScanCompleteCallback {
        void onScanComplete(String csvPath, String username);
    }

    public WebcamQRScanner(JFrame parent, QRAuthenticationManager authManager) {
        super(parent, "Scan QR Code", true);
        this.authManager = authManager;

        initializeUI();
        startScanning();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Title
        JLabel titleLabel = new JLabel("Position QR code in front of camera", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(titleLabel, BorderLayout.NORTH);

        // Webcam panel
        webcam = Webcam.getDefault();
        if (webcam != null) {
            webcam.setViewSize(WebcamResolution.VGA.getSize());
            webcamPanel = new WebcamPanel(webcam);
            webcamPanel.setPreferredSize(WebcamResolution.VGA.getSize());
            add(webcamPanel, BorderLayout.CENTER);
        }

        // Cancel button
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            running = false;
            dispose();
        });
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(getParent());
    }

    private void startScanning() {
        executor.execute(this);
    }

    @Override
    public void run() {
        if (webcam == null) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this,
                        "No webcam available",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                dispose();
            });
            return;
        }

        while (running) {
            try {
                Thread.sleep(100); // Scan every 100ms

                BufferedImage image = webcam.getImage();
                if (image != null) {
                    try {
                        String qrContent = authManager.decodeQRCode(image);
                        String csvPath = authManager.authenticateAndGetCSV(qrContent);

                        if (csvPath != null) {
                            String username = extractUsernameFromPath(csvPath);
                            final String finalCsvPath = csvPath;
                            final String finalUsername = username;

                            SwingUtilities.invokeLater(() -> {
                                if (callback != null) {
                                    callback.onScanComplete(finalCsvPath, finalUsername);
                                }
                                running = false;
                                dispose();
                            });
                            break;
                        }
                    } catch (NotFoundException e) {
                        // No QR code found in this frame, continue scanning
                    }
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void setOnScanComplete(ScanCompleteCallback callback) {
        this.callback = callback;
    }

    private String extractUsernameFromPath(String csvPath) {
        String fileName = csvPath.substring(csvPath.lastIndexOf('/') + 1);
        fileName = fileName.substring(fileName.lastIndexOf('\\') + 1);
        return fileName.replace("_budget.csv", "");
    }

    @Override
    public void dispose() {
        running = false;
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
        super.dispose();
    }
}