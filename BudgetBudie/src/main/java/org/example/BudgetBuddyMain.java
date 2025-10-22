package org.example;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class BudgetBuddyMain extends JFrame {
    private JPanel contentPanel;
    private String currentUserCSV;
    private String currentUsername;

    public BudgetBuddyMain() {
        setTitle("Budget Buddy System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        contentPanel = new JPanel(new BorderLayout());
        add(contentPanel);

        showLoginScreen();
    }

    private void showLoginScreen() {
        contentPanel.removeAll();

        QRLoginPanel loginPanel = new QRLoginPanel(this, (csvPath, username) -> {
            currentUserCSV = csvPath;
            currentUsername = username;
            showMainApplication();
        });

        contentPanel.add(loginPanel);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showMainApplication() {
        contentPanel.removeAll();

        // Create main application panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("Welcome, " + currentUsername + "!");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 20));
        headerPanel.add(welcomeLabel, BorderLayout.WEST);

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> showLoginScreen());
        headerPanel.add(logoutButton, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Content area - Display CSV data
        JTextArea dataArea = new JTextArea();
        dataArea.setEditable(false);
        dataArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Load CSV data
        try {
            StringBuilder content = new StringBuilder();
            content.append("Budget Data from: ").append(currentUserCSV).append("\n");
            content.append("=".repeat(60)).append("\n\n");

            try (BufferedReader reader = new BufferedReader(new FileReader(currentUserCSV))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }

            dataArea.setText(content.toString());
        } catch (IOException e) {
            dataArea.setText("Error loading budget data: " + e.getMessage());
        }

        JScrollPane scrollPane = new JScrollPane(dataArea);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addEntryButton = new JButton("Add Entry");
        JButton viewReportsButton = new JButton("View Reports");
        JButton settingsButton = new JButton("Settings");

        addEntryButton.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Add Entry feature - Connect to your budget entry form"));
        viewReportsButton.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "View Reports feature - Connect to your reports panel"));
        settingsButton.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Settings feature - Connect to your settings panel"));

        buttonPanel.add(addEntryButton);
        buttonPanel.add(viewReportsButton);
        buttonPanel.add(settingsButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        contentPanel.add(mainPanel);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Set system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            BudgetBuddyMain app = new BudgetBuddyMain();
            app.setVisible(true);
        });
    }
}