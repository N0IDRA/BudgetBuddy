package org.example;

import com.github.sarxos.webcam.Webcam;
import com.google.zxing.NotFoundException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class QRLoginPanel extends JPanel {
    private QRAuthenticationManager authManager;
    private JButton scanQRButton;
    private JButton uploadQRButton;
    private JButton registerNewUserButton;
    private JLabel statusLabel;
    private JFrame parentFrame;

    // Callback for successful login
    private LoginCallback loginCallback;

    public interface LoginCallback {
        void onLoginSuccess(String csvFilePath, String username);
    }

    public QRLoginPanel(JFrame parentFrame, LoginCallback callback) {
        this.parentFrame = parentFrame;
        this.authManager = new QRAuthenticationManager();
        this.loginCallback = callback;

        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title panel
        JPanel titlePanel = new JPanel();
        JLabel titleLabel = new JLabel("Budget Buddy - QR Login");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);

        // Center panel with buttons
        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Scan QR button (using webcam)
        scanQRButton = new JButton("Scan QR Code with Webcam");
        scanQRButton.setPreferredSize(new Dimension(300, 50));
        scanQRButton.addActionListener(this::handleScanQR);
        centerPanel.add(scanQRButton, gbc);

        gbc.gridy++;
        // Upload QR image button
        uploadQRButton = new JButton("Upload QR Code Image");
        uploadQRButton.setPreferredSize(new Dimension(300, 50));
        uploadQRButton.addActionListener(this::handleUploadQR);
        centerPanel.add(uploadQRButton, gbc);

        gbc.gridy++;
        // Register new user button
        registerNewUserButton = new JButton("Register New User");
        registerNewUserButton.setPreferredSize(new Dimension(300, 50));
        registerNewUserButton.addActionListener(this::handleRegisterUser);
        centerPanel.add(registerNewUserButton, gbc);

        add(centerPanel, BorderLayout.CENTER);

        // Status label
        statusLabel = new JLabel("Please scan or upload your QR code to login", SwingConstants.CENTER);
        statusLabel.setForeground(Color.GRAY);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void handleScanQR(ActionEvent e) {
        // Check if webcam is available
        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            JOptionPane.showMessageDialog(this,
                    "No webcam detected. Please use 'Upload QR Code Image' instead.",
                    "Webcam Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Create webcam scanner dialog
        SwingUtilities.invokeLater(() -> {
            WebcamQRScanner scanner = new WebcamQRScanner(parentFrame, authManager);
            scanner.setOnScanComplete((csvPath, username) -> {
                if (csvPath != null) {
                    statusLabel.setText("Login successful! Welcome " + username);
                    statusLabel.setForeground(Color.GREEN);
                    loginCallback.onLoginSuccess(csvPath, username);
                } else {
                    statusLabel.setText("QR code not recognized. Please try again.");
                    statusLabel.setForeground(Color.RED);
                }
            });
            scanner.setVisible(true);
        });
    }

    private void handleUploadQR(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select QR Code Image");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image files", "jpg", "jpeg", "png", "gif"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                BufferedImage qrImage = ImageIO.read(selectedFile);
                String qrContent = authManager.decodeQRCode(qrImage);
                String csvPath = authManager.authenticateAndGetCSV(qrContent);

                if (csvPath != null) {
                    String username = extractUsernameFromPath(csvPath);
                    statusLabel.setText("Login successful! Welcome " + username);
                    statusLabel.setForeground(Color.GREEN);
                    loginCallback.onLoginSuccess(csvPath, username);
                } else {
                    statusLabel.setText("QR code not recognized. Please register first.");
                    statusLabel.setForeground(Color.RED);
                }
            } catch (IOException | NotFoundException ex) {
                statusLabel.setText("Failed to read QR code. Please try another image.");
                statusLabel.setForeground(Color.RED);
                ex.printStackTrace();
            }
        }
    }

    private void handleRegisterUser(ActionEvent e) {
        String username = JOptionPane.showInputDialog(this,
                "Enter username for new account:",
                "Register New User",
                JOptionPane.PLAIN_MESSAGE);

        if (username != null && !username.trim().isEmpty()) {
            username = username.trim();
            try {
                String qrPath = authManager.generateUserQRCode(username);

                // Show success dialog with QR code
                JDialog dialog = new JDialog(parentFrame, "Registration Successful", true);
                dialog.setLayout(new BorderLayout(10, 10));

                JLabel messageLabel = new JLabel(
                        "<html><center>User registered successfully!<br>" +
                                "Save this QR code to login:<br>" +
                                qrPath + "</center></html>",
                        SwingConstants.CENTER);
                dialog.add(messageLabel, BorderLayout.NORTH);

                // Display QR code
                BufferedImage qrImage = ImageIO.read(new File(qrPath));
                JLabel qrLabel = new JLabel(new ImageIcon(qrImage));
                dialog.add(qrLabel, BorderLayout.CENTER);

                JButton closeButton = new JButton("Close");
                closeButton.addActionListener(ev -> dialog.dispose());
                JPanel buttonPanel = new JPanel();
                buttonPanel.add(closeButton);
                dialog.add(buttonPanel, BorderLayout.SOUTH);

                dialog.pack();
                dialog.setLocationRelativeTo(this);
                dialog.setVisible(true);

                statusLabel.setText("User '" + username + "' registered successfully!");
                statusLabel.setForeground(Color.GREEN);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to register user: " + ex.getMessage(),
                        "Registration Error",
                        JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private String extractUsernameFromPath(String csvPath) {
        String fileName = new File(csvPath).getName();
        return fileName.replace("_budget.csv", "");
    }
}