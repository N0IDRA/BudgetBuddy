package com.example.budgetbuddy;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.scene.chart.*;
import javafx.collections.FXCollections;
import javafx.stage.FileChooser;
import javafx.scene.shape.Circle;

import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.LocalDate;
import java.util.*;

public class BudgetBuddyApp extends Application {

    private UserManager userManager;
    private BudgetManager budgetManager;
    private Stage primaryStage;
    private Webcam webcam;
    private ExecutorService executor;
    private volatile boolean scanning = false;
    private String currentUser;
    private Dialog<ButtonType> activeDialog;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.userManager = new UserManager();
        this.budgetManager = new BudgetManager();

        showLoginScreen();

        stage.setTitle("BudgetBuddy - Smart Finance Management");
        stage.setWidth(1200);
        stage.setHeight(800);
        stage.setOnCloseRequest(e -> cleanup());
        stage.show();
    }

    // ==================== LOGIN SCREEN ====================
    private void showLoginScreen() {
        StackPane root = new StackPane();

        try {
            ImageView bgImage = new ImageView(new Image(getClass().getResourceAsStream("/aurora_bg.jpg")));
            bgImage.setPreserveRatio(false);
            bgImage.fitWidthProperty().bind(root.widthProperty());
            bgImage.fitHeightProperty().bind(root.heightProperty());
            root.getChildren().add(bgImage);
        } catch (Exception e) {
            root.setStyle("-fx-background-color: linear-gradient(to bottom right, #001a1a 0%, #003d3d 50%, #00ffcc 100%);");
        }

        VBox loginCard = createModernLoginCard();
        root.getChildren().add(loginCard);
        StackPane.setAlignment(loginCard, Pos.CENTER_LEFT);
        StackPane.setMargin(loginCard, new Insets(0, 0, 0, 80));

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), loginCard);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private VBox createModernLoginCard() {
        VBox card = new VBox(30);
        card.setAlignment(Pos.TOP_CENTER);
        card.setMaxWidth(500);
        card.setMaxHeight(650);
        card.setStyle(
                "-fx-background-color: rgba(2, 26, 26, 0.95); " +
                        "-fx-background-radius: 30; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 255, 204, 0.4), 40, 0, 0, 0); " +
                        "-fx-padding: 40 50;"
        );

        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("BUDGET\nBUDDY");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 48));
        titleLabel.setStyle("-fx-text-fill: white; -fx-text-alignment: center;");

        Label welcomeLabel = new Label("Welcome, Buddy!");
        welcomeLabel.setFont(Font.font("System", FontWeight.NORMAL, 18));
        welcomeLabel.setStyle("-fx-text-fill: #00ffcc;");

        header.getChildren().addAll(titleLabel, welcomeLabel);

        HBox tabButtons = new HBox(0);
        tabButtons.setAlignment(Pos.CENTER);
        tabButtons.setMaxWidth(400);

        Button signInTab = createTabButton("Sign-In", true);
        Button signUpTab = createTabButton("Sign-Up", false);

        signInTab.setPrefWidth(200);
        signUpTab.setPrefWidth(200);

        tabButtons.getChildren().addAll(signInTab, signUpTab);

        StackPane contentArea = new StackPane();
        contentArea.setMinHeight(400);

        VBox signInPane = createSignInPane();
        VBox signUpPane = createSignUpPane();

        contentArea.getChildren().add(signInPane);
        signUpPane.setVisible(false);
        contentArea.getChildren().add(signUpPane);

        signInTab.setOnAction(e -> {
            signInTab.setStyle(getTabStyle(true));
            signUpTab.setStyle(getTabStyle(false));
            signInPane.setVisible(true);
            signUpPane.setVisible(false);
        });

        signUpTab.setOnAction(e -> {
            signUpTab.setStyle(getTabStyle(true));
            signInTab.setStyle(getTabStyle(false));
            signUpPane.setVisible(true);
            signInPane.setVisible(false);
        });

        card.getChildren().addAll(header, tabButtons, contentArea);
        return card;
    }

    private Button createTabButton(String text, boolean active) {
        Button btn = new Button(text);
        btn.setStyle(getTabStyle(active));
        btn.setCursor(javafx.scene.Cursor.HAND);
        return btn;
    }

    private String getTabStyle(boolean active) {
        if (active) {
            return "-fx-background-color: #00ffcc; " +
                    "-fx-text-fill: #021a1a; " +
                    "-fx-font-weight: bold; " +
                    "-fx-font-size: 14; " +
                    "-fx-padding: 12 40; " +
                    "-fx-background-radius: 25 25 0 0; " +
                    "-fx-border-width: 0;";
        } else {
            return "-fx-background-color: transparent; " +
                    "-fx-text-fill: #00ffcc; " +
                    "-fx-font-weight: bold; " +
                    "-fx-font-size: 14; " +
                    "-fx-padding: 12 40; " +
                    "-fx-background-radius: 25 25 0 0; " +
                    "-fx-border-color: #00ffcc; " +
                    "-fx-border-width: 2 2 0 2; " +
                    "-fx-border-radius: 25 25 0 0;";
        }
    }

    private VBox createSignInPane() {
        VBox pane = new VBox(20);
        pane.setAlignment(Pos.CENTER);
        pane.setPadding(new Insets(20));

        Label emailLabel = new Label("Email Address");
        emailLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13;");

        TextField emailField = new TextField();
        emailField.setPromptText("Enter your email");
        emailField.setStyle(getInputFieldStyle());

        VBox emailBox = new VBox(8, emailLabel, emailField);

        Label passwordLabel = new Label("Password");
        passwordLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13;");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.setStyle(getInputFieldStyle());

        VBox passwordBox = new VBox(8, passwordLabel, passwordField);

        Button signInQRButton = createActionButton("Sign-In using QR-Code", "#00ffcc", false);
        signInQRButton.setPrefWidth(220);
        signInQRButton.setOnAction(e -> showQRCodeLogin());

        HBox actionButtons = new HBox(15);
        actionButtons.setAlignment(Pos.CENTER);

        Button exitButton = createActionButton("Exit", "#ff6b6b", true);
        Button confirmButton = createActionButton("Confirm", "#00ffcc", false);

        exitButton.setPrefWidth(120);
        confirmButton.setPrefWidth(120);

        confirmButton.setOnAction(e -> {
            String email = emailField.getText().trim();
            String password = passwordField.getText().trim();

            if (email.isEmpty() || password.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Missing Information", "Please enter both email and password.");
                return;
            }

            if (userManager.authenticate(email, password)) {
                currentUser = email;
                showMainDashboard(email);
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid email or password.");
            }
        });

        exitButton.setOnAction(e -> Platform.exit());

        actionButtons.getChildren().addAll(exitButton, confirmButton);

        Label quoteLabel = new Label("\"Beware of little expenses; a small leak will sink a great ship.\"\nBenjamin Franklin");
        quoteLabel.setStyle("-fx-text-fill: #00ffcc; -fx-font-size: 11; -fx-text-alignment: center;");
        quoteLabel.setWrapText(true);
        quoteLabel.setMaxWidth(350);

        pane.getChildren().addAll(emailBox, passwordBox, signInQRButton, actionButtons, quoteLabel);
        return pane;
    }

    private VBox createSignUpPane() {
        VBox pane = new VBox(15);
        pane.setAlignment(Pos.CENTER);
        pane.setPadding(new Insets(20));

        Label titleLabel = new Label("Create your Account");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");

        Label subtitleLabel = new Label("Join us in unleashing your financial Journey");
        subtitleLabel.setStyle("-fx-text-fill: #00ffcc; -fx-font-size: 12;");

        VBox headerBox = new VBox(5, titleLabel, subtitleLabel);
        headerBox.setAlignment(Pos.CENTER);

        TextField nameField = new TextField();
        nameField.setPromptText("Full Name/Username");
        nameField.setStyle(getInputFieldStyle());

        TextField emailField = new TextField();
        emailField.setPromptText("Email Address");
        emailField.setStyle(getInputFieldStyle());

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setStyle(getInputFieldStyle());

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm Password");
        confirmPasswordField.setStyle(getInputFieldStyle());

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button backButton = createActionButton("Back", "#666666", true);
        Button confirmButton = createActionButton("Confirm", "#00ffcc", false);

        backButton.setPrefWidth(120);
        confirmButton.setPrefWidth(120);

        confirmButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String password = passwordField.getText().trim();
            String confirmPassword = confirmPasswordField.getText().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Missing Information", "Please fill in all fields.");
                return;
            }

            if (!password.equals(confirmPassword)) {
                showAlert(Alert.AlertType.WARNING, "Password Mismatch", "Passwords do not match.");
                return;
            }

            if (userManager.registerUser(name, password, email)) {
                showQRCodeSuccessDialog(name);
            } else {
                showAlert(Alert.AlertType.ERROR, "Registration Failed", "Username or email already exists.");
            }
        });

        buttonBox.getChildren().addAll(backButton, confirmButton);

        pane.getChildren().addAll(headerBox, nameField, emailField, passwordField, confirmPasswordField, buttonBox);
        return pane;
    }

    // ==================== QR CODE SCANNING ====================
    private void showQRCodeLogin() {
        activeDialog = new Dialog<>();
        activeDialog.setTitle("QR Code Login");

        VBox content = new VBox(25);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40));
        content.setStyle("-fx-background-color: #021a1a; -fx-background-radius: 20;");

        Label titleLabel = new Label("BUDGET\nBUDDY");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 36));
        titleLabel.setStyle("-fx-text-fill: white; -fx-text-alignment: center;");

        Label scanLabel = new Label("Scanning QR Code...");
        scanLabel.setStyle("-fx-text-fill: #00ffcc; -fx-font-size: 14;");

        StackPane cameraArea = new StackPane();
        cameraArea.setStyle(
                "-fx-background-color: #1a1a1a; " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-color: #00ffcc; " +
                        "-fx-border-radius: 15; " +
                        "-fx-border-width: 3; " +
                        "-fx-min-width: 400; " +
                        "-fx-min-height: 300;"
        );

        ImageView webcamView = new ImageView();
        webcamView.setFitWidth(380);
        webcamView.setFitHeight(285);
        webcamView.setPreserveRatio(true);

        VBox placeholderBox = new VBox(15);
        placeholderBox.setAlignment(Pos.CENTER);
        Label placeholderIcon = new Label("👤");
        placeholderIcon.setFont(Font.font(72));
        Label placeholderText = new Label("Position QR code in frame");
        placeholderText.setStyle("-fx-text-fill: #666666; -fx-font-size: 14;");
        placeholderBox.getChildren().addAll(placeholderIcon, placeholderText);

        cameraArea.getChildren().addAll(placeholderBox, webcamView);
        webcamView.setVisible(false);

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button scanButton = createActionButton("Scan With Camera", "#00ffcc", false);
        Button uploadButton = createActionButton("Upload QR Image", "#00ffcc", false);
        Button backButton = createActionButton("Back", "#666666", true);

        scanButton.setPrefWidth(150);
        uploadButton.setPrefWidth(150);

        scanButton.setOnAction(e -> {
            if (!scanning) {
                placeholderBox.setVisible(false);
                webcamView.setVisible(true);
                startQRScanning(webcamView, activeDialog);
                scanButton.setText("Stop Camera");
                scanLabel.setText("Scanning... Point QR at camera");
            } else {
                stopScanning();
                webcamView.setVisible(false);
                placeholderBox.setVisible(true);
                scanButton.setText("Scan With Camera");
                scanLabel.setText("Scanning stopped");
            }
        });

        uploadButton.setOnAction(e -> handleQRImageUpload(activeDialog));

        backButton.setOnAction(e -> {
            stopScanning();
            activeDialog.close();
        });

        buttonBox.getChildren().addAll(scanButton, uploadButton);

        Label quoteLabel = new Label("\"Beware of little expenses; a small leak will sink a great ship.\"\nBenjamin Franklin");
        quoteLabel.setStyle("-fx-text-fill: #00ffcc; -fx-font-size: 10; -fx-text-alignment: center;");
        quoteLabel.setWrapText(true);
        quoteLabel.setMaxWidth(400);

        content.getChildren().addAll(titleLabel, scanLabel, cameraArea, buttonBox, backButton, quoteLabel);

        activeDialog.getDialogPane().setContent(content);
        activeDialog.getDialogPane().setStyle("-fx-background-color: #021a1a;");
        activeDialog.getDialogPane().getButtonTypes().clear();

        activeDialog.setOnCloseRequest(e -> stopScanning());
        activeDialog.showAndWait();
    }

    private void startQRScanning(ImageView webcamView, Dialog<ButtonType> dialog) {
        try {
            webcam = Webcam.getDefault();
            if (webcam == null) {
                showAlert(Alert.AlertType.ERROR, "No Webcam", "No webcam detected.");
                return;
            }

            webcam.setViewSize(WebcamResolution.VGA.getSize());
            webcam.open();
            scanning = true;

            executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                MultiFormatReader reader = new MultiFormatReader();
                while (scanning && webcam.isOpen()) {
                    try {
                        BufferedImage image = webcam.getImage();
                        if (image != null) {
                            Platform.runLater(() -> webcamView.setImage(SwingFXUtils.toFXImage(image, null)));

                            LuminanceSource source = new BufferedImageLuminanceSource(image);
                            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                            try {
                                Result result = reader.decode(bitmap);
                                String qrCode = result.getText();
                                scanning = false;

                                Platform.runLater(() -> {
                                    String username = userManager.authenticateQR(qrCode);
                                    if (username != null) {
                                        stopScanning();
                                        dialog.close();
                                        showPINDialog(username);
                                    } else {
                                        showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid QR code");
                                        scanning = true;
                                    }
                                });
                            } catch (NotFoundException ex) {
                                // Continue scanning
                            }
                            reader.reset();
                        }
                        Thread.sleep(100);
                    } catch (Exception ex) {
                        // Handle error
                    }
                }
            });
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Camera Error", "Failed to start camera: " + ex.getMessage());
        }
    }

    private void handleQRImageUpload(Dialog<ButtonType> dialog) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select QR Code Image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif")
        );

        java.io.File selectedFile = fileChooser.showOpenDialog(primaryStage);

        if (selectedFile != null) {
            try {
                BufferedImage bufferedImage = javax.imageio.ImageIO.read(selectedFile);
                if (bufferedImage == null) throw new Exception("Could not read image file");

                LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                Result result = new MultiFormatReader().decode(bitmap);
                String qrCode = result.getText();

                String username = userManager.authenticateQR(qrCode);
                if (username != null) {
                    dialog.close();
                    showPINDialog(username);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid QR code");
                }
            } catch (NotFoundException ex) {
                showAlert(Alert.AlertType.ERROR, "No QR Code Found", "No QR code detected in image.");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to read QR code: " + ex.getMessage());
            }
        }
    }

    private void showPINDialog(String username) {
        Dialog<ButtonType> pinDialog = new Dialog<>();
        pinDialog.setTitle("Enter PIN");

        VBox content = new VBox(25);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40));
        content.setStyle("-fx-background-color: #021a1a; -fx-background-radius: 20;");

        Label titleLabel = new Label("BUDGET\nBUDDY");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 36));
        titleLabel.setStyle("-fx-text-fill: white; -fx-text-alignment: center;");

        Label pinLabel = new Label("Enter PIN");
        pinLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");

        PasswordField pinField = new PasswordField();
        pinField.setPromptText("Enter 4-digit PIN");
        pinField.setStyle(getInputFieldStyle());
        pinField.setMaxWidth(300);

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button exitButton = createActionButton("Exit", "#ff6b6b", true);
        Button confirmButton = createActionButton("Confirm", "#00ffcc", false);

        exitButton.setPrefWidth(120);
        confirmButton.setPrefWidth(120);

        confirmButton.setOnAction(e -> {
            String pin = pinField.getText().trim();
            if (pin.matches("\\d{4}")) {
                currentUser = username;
                pinDialog.close();
                showMainDashboard(username);
            } else {
                showAlert(Alert.AlertType.WARNING, "Invalid PIN", "PIN must be exactly 4 digits.");
            }
        });

        exitButton.setOnAction(e -> pinDialog.close());

        buttonBox.getChildren().addAll(exitButton, confirmButton);

        Label quoteLabel = new Label("\"Beware of little expenses; a small leak will sink a great ship.\"\nBenjamin Franklin");
        quoteLabel.setStyle("-fx-text-fill: #00ffcc; -fx-font-size: 10; -fx-text-alignment: center;");
        quoteLabel.setWrapText(true);
        quoteLabel.setMaxWidth(400);

        content.getChildren().addAll(titleLabel, pinLabel, pinField, buttonBox, quoteLabel);

        pinDialog.getDialogPane().setContent(content);
        pinDialog.getDialogPane().setStyle("-fx-background-color: #021a1a;");
        pinDialog.getDialogPane().getButtonTypes().clear();

        pinDialog.showAndWait();
    }

    private void showQRCodeSuccessDialog(String username) {
        String qrCode = userManager.getQRCode(username);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Registration Successful!");

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: #021a1a;");

        Label successLabel = new Label("✓ Account created!");
        successLabel.setStyle("-fx-font-size: 18; -fx-text-fill: #00ffcc; -fx-font-weight: bold;");

        Label welcomeLabel = new Label("Welcome, " + username + "!");
        welcomeLabel.setStyle("-fx-font-size: 16; -fx-text-fill: white;");

        try {
            BufferedImage qrImage = QRCodeGenerator.generateQRCodeForDisplay(qrCode);
            if (qrImage != null) {
                ImageView qrImageView = new ImageView(SwingFXUtils.toFXImage(qrImage, null));
                qrImageView.setFitWidth(280);
                qrImageView.setFitHeight(280);

                Button saveBtn = createActionButton("💾 Save QR Image", "#00ffcc", false);
                saveBtn.setOnAction(e -> {
                    try {
                        String filePath = QRCodeGenerator.generateUserQRCode(username, qrCode, "qr_codes");
                        showAlert(Alert.AlertType.INFORMATION, "Saved", "QR saved to: " + filePath);
                    } catch (Exception ex) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to save: " + ex.getMessage());
                    }
                });

                content.getChildren().addAll(successLabel, welcomeLabel, qrImageView, saveBtn);
            }
        } catch (Exception e) {
            content.getChildren().addAll(successLabel, welcomeLabel, new Label("QR: " + qrCode));
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: #021a1a;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
    }

    // ==================== MAIN DASHBOARD ====================
    private void showMainDashboard(String username) {
        BorderPane mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: linear-gradient(to bottom right, #1a4d3e 0%, #0d2621 100%);");

        // Left sidebar
        VBox sidebar = createSidebar(username);
        mainLayout.setLeft(sidebar);

        // Center content area
        StackPane centerArea = new StackPane();
        mainLayout.setCenter(centerArea);

        // Right history panel
        VBox historyPanel = createHistoryPanel();
        mainLayout.setRight(historyPanel);

        // Show dashboard by default
        showDashboardContent(centerArea);

        Scene scene = new Scene(mainLayout, 1400, 800);
        primaryStage.setScene(scene);

        FadeTransition fade = new FadeTransition(Duration.millis(500), mainLayout);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private VBox createSidebar(String username) {
        VBox sidebar = new VBox(20);
        sidebar.setStyle("-fx-background-color: #0a1f1a; -fx-padding: 30 20;");
        sidebar.setPrefWidth(250);

        // Logo and profile
        VBox profile = new VBox(15);
        profile.setAlignment(Pos.CENTER);

        Label logo = new Label("BUDGET\nBUDDY");
        logo.setFont(Font.font("System", FontWeight.BOLD, 28));
        logo.setStyle("-fx-text-fill: white; -fx-text-alignment: center;");

        Circle avatar = new Circle(50);
        avatar.setStyle("-fx-fill: #cccccc;");

        Label welcomeLabel = new Label("Welcome Back,\n" + username);
        welcomeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13; -fx-text-alignment: center;");
        welcomeLabel.setWrapText(true);

        profile.getChildren().addAll(logo, avatar, welcomeLabel);

        // Menu buttons
        VBox menu = new VBox(10);
        menu.setAlignment(Pos.CENTER);

        Button dashboardBtn = createMenuButton("Dashboard", true);
        Button budgetingBtn = createMenuButton("Start Budgeting", false);
        Button redeemBtn = createMenuButton("Redeem", false);
        Button helpBtn = createMenuButton("Help Info", false);

        StackPane centerArea = (StackPane) ((BorderPane) primaryStage.getScene().getRoot()).getCenter();

        dashboardBtn.setOnAction(e -> {
            resetMenuButtons(dashboardBtn, budgetingBtn, redeemBtn, helpBtn);
            dashboardBtn.setStyle(createMenuButton("Dashboard", true).getStyle());
            showDashboardContent(centerArea);
        });

        budgetingBtn.setOnAction(e -> {
            resetMenuButtons(dashboardBtn, budgetingBtn, redeemBtn, helpBtn);
            budgetingBtn.setStyle(createMenuButton("Start Budgeting", true).getStyle());
            showBudgetingContent(centerArea);
        });

        redeemBtn.setOnAction(e -> {
            resetMenuButtons(dashboardBtn, budgetingBtn, redeemBtn, helpBtn);
            redeemBtn.setStyle(createMenuButton("Redeem", true).getStyle());
            showRedeemContent(centerArea);
        });

        helpBtn.setOnAction(e -> {
            resetMenuButtons(dashboardBtn, budgetingBtn, redeemBtn, helpBtn);
            helpBtn.setStyle(createMenuButton("Help Info", true).getStyle());
            showHelpContent(centerArea);
        });

        menu.getChildren().addAll(dashboardBtn, budgetingBtn, redeemBtn, helpBtn);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button exitBtn = createActionButton("Exit", "#c0392b", true);
        exitBtn.setPrefWidth(180);
        exitBtn.setOnAction(e -> showLoginScreen());

        sidebar.getChildren().addAll(profile, menu, spacer, exitBtn);
        return sidebar;
    }

    private Button createMenuButton(String text, boolean active) {
        Button btn = new Button(text);
        btn.setPrefWidth(200);
        btn.setPrefHeight(45);
        btn.setAlignment(Pos.CENTER_LEFT);

        if (active) {
            btn.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-text-fill: #0a1f1a; " +
                            "-fx-font-size: 14; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-radius: 8; " +
                            "-fx-padding: 10 20; " +
                            "-fx-border-color: white; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 8; " +
                            "-fx-cursor: hand;"
            );
        } else {
            btn.setStyle(
                    "-fx-background-color: transparent; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 14; " +
                            "-fx-background-radius: 8; " +
                            "-fx-padding: 10 20; " +
                            "-fx-border-color: transparent; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 8; " +
                            "-fx-cursor: hand;"
            );
        }
        return btn;
    }

    private void resetMenuButtons(Button... buttons) {
        for (Button btn : buttons) {
            btn.setStyle(createMenuButton(btn.getText(), false).getStyle());
        }
    }

    private VBox createHistoryPanel() {
        VBox panel = new VBox(20);
        panel.setStyle("-fx-background-color: rgba(10, 31, 26, 0.8); -fx-padding: 20;");
        panel.setPrefWidth(300);

        Label historyTitle = new Label("History");
        historyTitle.setFont(Font.font("System", FontWeight.BOLD, 22));
        historyTitle.setStyle("-fx-text-fill: white; -fx-background-color: #0a1f1a; -fx-padding: 15; -fx-background-radius: 10;");
        historyTitle.setMaxWidth(Double.MAX_VALUE);
        historyTitle.setAlignment(Pos.CENTER);

        // Expenses Chart
        VBox expensesBox = new VBox(10);
        expensesBox.setStyle("-fx-background-color: rgba(26, 77, 62, 0.6); -fx-padding: 15; -fx-background-radius: 10;");

        Label expensesLabel = new Label("EXPENSES");
        expensesLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;");

        PieChart miniPie = new PieChart();
        miniPie.setPrefSize(200, 200);
        miniPie.setLegendVisible(false);
        miniPie.getData().addAll(
                new PieChart.Data("Food", 35),
                new PieChart.Data("Shopping", 25),
                new PieChart.Data("Bills", 20),
                new PieChart.Data("Transport", 15),
                new PieChart.Data("Others", 5)
        );

        expensesBox.getChildren().addAll(expensesLabel, miniPie);

        // Spending Overview
        VBox spendingBox = new VBox(10);
        spendingBox.setStyle("-fx-background-color: #0a1f1a; -fx-padding: 15; -fx-background-radius: 10;");

        HBox spendingHeader = new HBox();
        spendingHeader.setAlignment(Pos.CENTER_LEFT);
        Label spendingLabel = new Label("Spending Overview");
        spendingLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label monthLabel = new Label("8 Month");
        monthLabel.setStyle("-fx-text-fill: #00ffcc; -fx-font-size: 10; -fx-background-color: rgba(0, 255, 204, 0.2); -fx-padding: 3 8; -fx-background-radius: 5;");
        spendingHeader.getChildren().addAll(spendingLabel, spacer, monthLabel);

        Label totalLabel = new Label("Total Expense");
        totalLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11;");

        Label amountLabel = new Label("$167,467.00");
        amountLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20; -fx-font-weight: bold;");

        // Color bar
        HBox colorBar = new HBox(0);
        colorBar.setPrefHeight(8);
        colorBar.setStyle("-fx-background-radius: 4;");

        Region bar1 = new Region();
        bar1.setStyle("-fx-background-color: #3498db; -fx-background-radius: 4 0 0 4;");
        bar1.setPrefWidth(50);

        Region bar2 = new Region();
        bar2.setStyle("-fx-background-color: #00d4aa;");
        bar2.setPrefWidth(60);

        Region bar3 = new Region();
        bar3.setStyle("-fx-background-color: #9b59b6;");
        bar3.setPrefWidth(30);

        Region bar4 = new Region();
        bar4.setStyle("-fx-background-color: #e74c3c;");
        bar4.setPrefWidth(20);

        Region bar5 = new Region();
        bar5.setStyle("-fx-background-color: #95e1d3; -fx-background-radius: 0 4 4 0;");
        bar5.setPrefWidth(40);

        colorBar.getChildren().addAll(bar1, bar2, bar3, bar4, bar5);

        // Legend
        VBox legendBox = new VBox(8);
        legendBox.getChildren().addAll(
                createLegendItem("Medical Equipment", "$21,000", "#3498db"),
                createLegendItem("Rental Cost", "$13,000", "#00d4aa"),
                createLegendItem("Supplies", "11,200", "#9b59b6"),
                createLegendItem("Promotion Cost", "$11,390", "#e74c3c"),
                createLegendItem("Others", "$18,389", "#95e1d3")
        );

        spendingBox.getChildren().addAll(spendingHeader, totalLabel, amountLabel, colorBar, legendBox);

        panel.getChildren().addAll(historyTitle, expensesBox, spendingBox);
        return panel;
    }

    private HBox createLegendItem(String label, String amount, String color) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);

        Region colorDot = new Region();
        colorDot.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 3;");
        colorDot.setPrefSize(8, 8);

        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 10;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label amountLabel = new Label(amount);
        amountLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10; -fx-font-weight: bold;");

        item.getChildren().addAll(colorDot, nameLabel, spacer, amountLabel);
        return item;
    }

    // ==================== CONTENT SCREENS ====================
    private void showDashboardContent(StackPane centerArea) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Dashboard");
        title.setFont(Font.font("System", FontWeight.BOLD, 32));
        title.setStyle("-fx-text-fill: white; -fx-background-color: rgba(10, 31, 26, 0.8); -fx-padding: 20; -fx-background-radius: 15;");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);

        Label welcomeMsg = new Label("Welcome to your financial dashboard!\nTrack your expenses and manage your budget effectively.");
        welcomeMsg.setStyle("-fx-text-fill: #00ffcc; -fx-font-size: 16; -fx-text-alignment: center;");
        welcomeMsg.setWrapText(true);

        content.getChildren().addAll(title, welcomeMsg);

        centerArea.getChildren().clear();
        centerArea.getChildren().add(content);
    }

    private void showBudgetingContent(StackPane centerArea) {
        VBox content = new VBox(25);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Start Budgeting");
        title.setFont(Font.font("System", FontWeight.BOLD, 28));
        title.setStyle("-fx-text-fill: white; -fx-background-color: rgba(10, 31, 26, 0.9); -fx-padding: 15; -fx-background-radius: 12;");
        title.setMaxWidth(500);
        title.setAlignment(Pos.CENTER);

        // Set Expense input
        VBox expenseBox = new VBox(10);
        expenseBox.setAlignment(Pos.CENTER);
        Label expenseLabel = new Label("Set Expense");
        expenseLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");
        TextField expenseField = new TextField();
        expenseField.setPromptText("Enter amount");
        expenseField.setStyle("-fx-pref-width: 400; -fx-pref-height: 45; -fx-font-size: 14; -fx-background-radius: 10; -fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-prompt-text-fill: #999;");
        expenseBox.getChildren().addAll(expenseLabel, expenseField);

        // Limit input
        VBox limitBox = new VBox(10);
        limitBox.setAlignment(Pos.CENTER);
        Label limitLabel = new Label("Limit");
        limitLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");
        TextField limitField = new TextField();
        limitField.setPromptText("Set budget limit");
        limitField.setStyle("-fx-pref-width: 400; -fx-pref-height: 45; -fx-font-size: 14; -fx-background-radius: 10; -fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-prompt-text-fill: #999;");
        limitBox.getChildren().addAll(limitLabel, limitField);

        // Category buttons
        GridPane categoryGrid = new GridPane();
        categoryGrid.setHgap(20);
        categoryGrid.setVgap(20);
        categoryGrid.setAlignment(Pos.CENTER);

        Button foodBtn = createCategoryButton("> Food");
        Button entertainmentBtn = createCategoryButton("> Entertainment");
        Button clothingBtn = createCategoryButton("> Clothing");
        Button feesBtn = createCategoryButton("> Fees");
        Button transportBtn = createCategoryButton("> Transportation");
        Button othersBtn = createCategoryButton("> Others");

        categoryGrid.add(foodBtn, 0, 0);
        categoryGrid.add(entertainmentBtn, 1, 0);
        categoryGrid.add(clothingBtn, 0, 1);
        categoryGrid.add(feesBtn, 1, 1);
        categoryGrid.add(transportBtn, 0, 2);
        categoryGrid.add(othersBtn, 1, 2);

        // Text area for notes
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Add notes or description...");
        notesArea.setPrefHeight(100);
        notesArea.setMaxWidth(500);
        notesArea.setStyle("-fx-font-size: 13; -fx-background-radius: 10; -fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-prompt-text-fill: #999;");

        // Action buttons
        HBox actionButtons = new HBox(20);
        actionButtons.setAlignment(Pos.CENTER);

        Button backBtn = createActionButton("Back", "#999999", true);
        Button confirmBtn = createActionButton("Confirm", "#00ffcc", false);

        backBtn.setPrefWidth(150);
        confirmBtn.setPrefWidth(150);

        backBtn.setOnAction(e -> showDashboardContent(centerArea));
        confirmBtn.setOnAction(e -> {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Budget saved successfully!");
        });

        actionButtons.getChildren().addAll(backBtn, confirmBtn);

        content.getChildren().addAll(title, expenseBox, limitBox, categoryGrid, notesArea, actionButtons);

        centerArea.getChildren().clear();
        centerArea.getChildren().add(content);
    }

    private Button createCategoryButton(String text) {
        Button btn = new Button(text);
        btn.setPrefSize(200, 50);
        btn.setStyle(
                "-fx-background-color: linear-gradient(to right, #1a4d3e, #2d6a56); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 15; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 12; " +
                        "-fx-cursor: hand; " +
                        "-fx-border-color: #00ffcc; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 12;"
        );
        btn.setOnMouseEntered(e -> btn.setOpacity(0.8));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    private void showRedeemContent(StackPane centerArea) {
        VBox content = new VBox(25);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Redeem");
        title.setFont(Font.font("System", FontWeight.BOLD, 28));
        title.setStyle("-fx-text-fill: white; -fx-background-color: rgba(10, 31, 26, 0.9); -fx-padding: 15; -fx-background-radius: 12;");
        title.setMaxWidth(500);
        title.setAlignment(Pos.CENTER);

        // Points input
        VBox pointsBox = new VBox(10);
        pointsBox.setAlignment(Pos.CENTER);
        Label pointsLabel = new Label("Points");
        pointsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");
        TextField pointsField = new TextField();
        pointsField.setPromptText("Enter your points");
        pointsField.setStyle("-fx-pref-width: 400; -fx-pref-height: 45; -fx-font-size: 14; -fx-background-radius: 10; -fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-prompt-text-fill: #999;");
        pointsBox.getChildren().addAll(pointsLabel, pointsField);

        Label rewardsLabel = new Label("Rewards");
        rewardsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold;");

        // Rewards grid
        GridPane rewardsGrid = new GridPane();
        rewardsGrid.setHgap(20);
        rewardsGrid.setVgap(20);
        rewardsGrid.setAlignment(Pos.CENTER);

        Button reward1 = createRewardButton("100PHP Starbucks Gift Card", "Points: 100 | QTY: 10");
        Button reward2 = createRewardButton("100PHP SM Gift Card", "Points: 100 | QTY: 100");
        Button reward3 = createRewardButton("100PHP Ritwal Gift Card", "Points: 100 | QTY: 100");
        Button reward4 = createRewardButton("100PHP Ayala Gift Card", "Points: 100 | QTY: 100");
        Button reward5 = createRewardButton("1000PHP Vikings Gift Card", "Points: 1000 | QTY: 5");
        Button reward6 = createRewardButton("100Hp Gcash", "Points: 100 | QTY: 10");
        Button reward7 = createRewardButton("1000PHP SM Gift Card", "Points: 1000 | QTY: 5");
        Button reward8 = createRewardButton("1000Hp Gcash", "Points: 1000 | QTY: 5");

        rewardsGrid.add(reward1, 0, 0);
        rewardsGrid.add(reward2, 1, 0);
        rewardsGrid.add(reward3, 0, 1);
        rewardsGrid.add(reward4, 1, 1);
        rewardsGrid.add(reward5, 0, 2);
        rewardsGrid.add(reward6, 1, 2);
        rewardsGrid.add(reward7, 0, 3);
        rewardsGrid.add(reward8, 1, 3);

        // Action buttons
        HBox actionButtons = new HBox(20);
        actionButtons.setAlignment(Pos.CENTER);

        Button backBtn = createActionButton("Back", "#999999", true);
        Button confirmBtn = createActionButton("Confirm", "#00ffcc", false);

        backBtn.setPrefWidth(150);
        confirmBtn.setPrefWidth(150);

        backBtn.setOnAction(e -> showDashboardContent(centerArea));
        confirmBtn.setOnAction(e -> {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Reward redeemed successfully!");
        });

        actionButtons.getChildren().addAll(backBtn, confirmBtn);

        ScrollPane scrollPane = new ScrollPane();
        VBox scrollContent = new VBox(20, pointsBox, rewardsLabel, rewardsGrid, actionButtons);
        scrollContent.setAlignment(Pos.TOP_CENTER);
        scrollPane.setContent(scrollContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        content.getChildren().addAll(title, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        centerArea.getChildren().clear();
        centerArea.getChildren().add(content);
    }

    private Button createRewardButton(String title, String details) {
        Button btn = new Button();
        VBox btnContent = new VBox(5);
        btnContent.setAlignment(Pos.CENTER);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(180);
        titleLabel.setAlignment(Pos.CENTER);

        Label detailsLabel = new Label(details);
        detailsLabel.setStyle("-fx-text-fill: #00ffcc; -fx-font-size: 10;");

        btnContent.getChildren().addAll(titleLabel, detailsLabel);

        btn.setGraphic(btnContent);
        btn.setPrefSize(220, 80);
        btn.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #1a4d3e, #0d2621); " +
                        "-fx-background-radius: 12; " +
                        "-fx-cursor: hand; " +
                        "-fx-border-color: #00ffcc; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 12; " +
                        "-fx-padding: 10;"
        );
        btn.setOnMouseEntered(e -> btn.setOpacity(0.8));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    private void showHelpContent(StackPane centerArea) {
        VBox content = new VBox(25);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Help");
        title.setFont(Font.font("System", FontWeight.BOLD, 28));
        title.setStyle("-fx-text-fill: white; -fx-background-color: rgba(10, 31, 26, 0.9); -fx-padding: 15; -fx-background-radius: 12;");
        title.setMaxWidth(600);
        title.setAlignment(Pos.CENTER);

        VBox helpContent = new VBox(20);
        helpContent.setStyle("-fx-background-color: rgba(26, 77, 62, 0.6); -fx-padding: 30; -fx-background-radius: 15;");
        helpContent.setMaxWidth(700);

        Label welcomeLabel = new Label("Welcome to BudgetBuddy!");
        welcomeLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        welcomeLabel.setStyle("-fx-text-fill: #00ffcc;");

        Label intro = new Label("Your personal finance management companion. Here's how to use the system:");
        intro.setStyle("-fx-text-fill: white; -fx-font-size: 14;");
        intro.setWrapText(true);

        VBox instructions = new VBox(15);

        instructions.getChildren().addAll(
                createHelpItem("1. Dashboard", "View your financial overview, total expenses, and budget summaries at a glance."),
                createHelpItem("2. Start Budgeting", "Set your expense limits for different categories like Food, Entertainment, Clothing, Transportation, and more. Track your spending against your budget."),
                createHelpItem("3. Redeem Rewards", "Earn points by staying within your budget! Redeem points for gift cards from popular stores like Starbucks, SM, Vikings, and get Gcash rewards."),
                createHelpItem("4. History Panel", "Check the right sidebar to see your expense breakdown by category and monthly spending trends."),
                createHelpItem("5. Tips", "• Set realistic budgets\n• Review your spending regularly\n• Save consistently\n• Redeem rewards to stay motivated!")
        );

        Label quoteLabel = new Label("\"Beware of little expenses; a small leak will sink a great ship.\" - Benjamin Franklin");
        quoteLabel.setStyle("-fx-text-fill: #00ffcc; -fx-font-size: 12; -fx-font-style: italic;");
        quoteLabel.setWrapText(true);

        helpContent.getChildren().addAll(welcomeLabel, intro, instructions, quoteLabel);

        // Action buttons
        HBox actionButtons = new HBox(20);
        actionButtons.setAlignment(Pos.CENTER);

        Button backBtn = createActionButton("Back", "#999999", true);
        Button confirmBtn = createActionButton("Confirm", "#00ffcc", false);

        backBtn.setPrefWidth(150);
        confirmBtn.setPrefWidth(150);

        backBtn.setOnAction(e -> showDashboardContent(centerArea));
        confirmBtn.setOnAction(e -> showDashboardContent(centerArea));

        actionButtons.getChildren().addAll(backBtn, confirmBtn);

        ScrollPane scrollPane = new ScrollPane();
        VBox scrollContent = new VBox(20, helpContent, actionButtons);
        scrollContent.setAlignment(Pos.TOP_CENTER);
        scrollPane.setContent(scrollContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        content.getChildren().addAll(title, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        centerArea.getChildren().clear();
        centerArea.getChildren().add(content);
    }

    private VBox createHelpItem(String title, String description) {
        VBox item = new VBox(5);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        titleLabel.setStyle("-fx-text-fill: white;");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 13;");
        descLabel.setWrapText(true);

        item.getChildren().addAll(titleLabel, descLabel);
        return item;
    }

    // ==================== HELPER METHODS ====================
    private String getInputFieldStyle() {
        return "-fx-background-color: white; " +
                "-fx-text-fill: #021a1a; " +
                "-fx-font-size: 14; " +
                "-fx-padding: 12 15; " +
                "-fx-background-radius: 8; " +
                "-fx-border-radius: 8; " +
                "-fx-pref-height: 45;";
    }

    private Button createActionButton(String text, String color, boolean isSecondary) {
        Button btn = new Button(text);
        if (isSecondary) {
            btn.setStyle(
                    "-fx-background-color: " + color + "; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold; " +
                            "-fx-font-size: 13; " +
                            "-fx-padding: 12 25; " +
                            "-fx-background-radius: 8; " +
                            "-fx-cursor: hand;"
            );
        } else {
            btn.setStyle(
                    "-fx-background-color: " + color + "; " +
                            "-fx-text-fill: #021a1a; " +
                            "-fx-font-weight: bold; " +
                            "-fx-font-size: 13; " +
                            "-fx-padding: 12 25; " +
                            "-fx-background-radius: 8; " +
                            "-fx-cursor: hand;"
            );
        }
        btn.setOnMouseEntered(e -> btn.setOpacity(0.8));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void stopScanning() {
        scanning = false;
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
            webcam = null;
        }
    }

    private void cleanup() {
        stopScanning();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
