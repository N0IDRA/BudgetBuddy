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
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.control.Separator;
import javafx.animation.*;

import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BudgetBuddyApp extends Application {

    private UserManager userManager;
    private BudgetManager budgetManager;
    private Stage primaryStage;
    private Webcam webcam;
    private ExecutorService executor;
    private volatile boolean scanning = false;
    private String currentUser;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.userManager = new UserManager();
        this.budgetManager = new BudgetManager();

        showLoginScreen();

        stage.setTitle("BudgetBuddy - Smart Finance Management");
        stage.setWidth(1100);
        stage.setHeight(800);
        stage.setOnCloseRequest(e -> cleanup());
        stage.show();
    }

    private void showLoginScreen() {
        // Main container with aurora background
        StackPane root = new StackPane();

        // Background with aurora image
        try {
            // Try to load aurora background image
            ImageView bgImage = new ImageView(new Image(getClass().getResourceAsStream("/aurora_bg.jpg")));
            bgImage.setPreserveRatio(false);
            bgImage.fitWidthProperty().bind(root.widthProperty());
            bgImage.fitHeightProperty().bind(root.heightProperty());
            root.getChildren().add(bgImage);
        } catch (Exception e) {
            // Fallback to gradient if image not found
            root.setStyle("-fx-background-color: linear-gradient(to bottom right, #001a1a 0%, #003d3d 50%, #00ffcc 100%);");
        }

        // Create the login card
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
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_CENTER);
        card.setMaxWidth(500);
        card.setMaxHeight(650);
        card.setStyle(
                "-fx-background-color: rgba(2, 26, 26, 0.95); " +
                        "-fx-background-radius: 30; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 255, 204, 0.4), 40, 0, 0, 0); " +
                        "-fx-padding: 40 50;"
        );

        // Title section
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("BUDGET BUDDY");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 50));
        titleLabel.setStyle("-fx-text-fill: white; -fx-text-alignment: center;");

        Label welcomeLabel = new Label("Welcome, Buddy!");
        welcomeLabel.setFont(Font.font("System", FontWeight.NORMAL, 18));
        welcomeLabel.setStyle("-fx-text-fill: #00ffcc;");

        header.getChildren().addAll(titleLabel, welcomeLabel);

        // Tab buttons
        HBox tabButtons = new HBox(0);
        tabButtons.setAlignment(Pos.CENTER);
        tabButtons.setMaxWidth(400);

        Button signInTab = createTabButton("Sign-In", true);
        Button signUpTab = createTabButton("Sign-Up", false);

        signInTab.setPrefWidth(170);
        signUpTab.setPrefWidth(170);

        tabButtons.getChildren().addAll(signInTab, signUpTab);

        // Content area (will switch between sign-in and sign-up)
        StackPane contentArea = new StackPane();
        contentArea.setMinHeight(400);

        VBox signInPane = createSignInPane();
        VBox signUpPane = createSignUpPane();

        contentArea.getChildren().add(signInPane);
        signUpPane.setVisible(false);
        contentArea.getChildren().add(signUpPane);

        // Tab switching logic
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
                    "-fx-background-radius: 25; " +
                    "-fx-border-width: 0;";
        } else {
            return "-fx-background-color: transparent; " +
                    "-fx-text-fill: #00ffcc; " +
                    "-fx-font-weight: bold; " +
                    "-fx-font-size: 14; " +
                    "-fx-padding: 12 40; " +
                    "-fx-background-radius: 25; " +
                    "-fx-border-color: #00ffcc; " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 25;";
        }
    }
    private int loginAttempts = 0;
    private int lockoutCount = 0; // Track how many times user has been locked out
    private boolean isLocked = false;
    private Timeline lockoutTimer;

    private VBox createSignInPane() {
        VBox pane = new VBox(17);
        pane.setAlignment(Pos.CENTER);
        pane.setPadding(new Insets(30));

        // Email field
        Label emailLabel = new Label("Username");
        emailLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13;");

        TextField emailField = new TextField();
        emailField.setPromptText("Enter your Username");
        emailField.setStyle(getInputFieldStyle());

        VBox emailBox = new VBox(12, emailLabel, emailField);

        // Password field
        Label passwordLabel = new Label("Pin");
        passwordLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13;");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your Pin");
        passwordField.setStyle(getInputFieldStyle());

        VBox passwordBox = new VBox(12, passwordLabel, passwordField);

        // Lockout message label
        Label lockoutLabel = new Label();
        lockoutLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12; -fx-font-weight: bold;");
        lockoutLabel.setVisible(false);

        // Warning label for escalating lockouts
        Label warningLabel = new Label();
        warningLabel.setStyle("-fx-text-fill: #ffaa00; -fx-font-size: 11; -fx-font-style: italic;");
        warningLabel.setVisible(false);

        // Buttons
        Button signInQRButton = createActionButton("Sign-In using QR-Code", "#00ffcc", false);
        signInQRButton.setPrefWidth(230);
        signInQRButton.setOnAction(e -> showQRCodeLogin());

        VBox mainButtons = new VBox(15);
        mainButtons.setAlignment(Pos.CENTER);

        HBox actionButtons = new HBox(15);
        actionButtons.setAlignment(Pos.CENTER);

        Button exitButton = createActionButton("Exit", "#ff6b6b", true);
        Button confirmButton = createActionButton("Confirm", "#00ffcc", false);

        exitButton.setPrefWidth(120);
        confirmButton.setPrefWidth(120);

        confirmButton.setOnAction(e -> {
            if (isLocked) {
                return; // Prevent action if locked
            }

            String email = emailField.getText().trim();
            String password = passwordField.getText().trim();

            if (email.isEmpty() || password.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Missing Information", "Please enter both email and password.");
                return;
            }

            if (userManager.authenticate(email, password)) {
                currentUser = email;
                loginAttempts = 0; // Reset attempts on successful login
                lockoutCount = 0; // Reset lockout count on successful login
                warningLabel.setVisible(false);
                showDashboard(email);
            } else {
                loginAttempts++;

                if (loginAttempts >= 3) {
                    lockoutCount++; // Increment lockout count

                    // Calculate lockout duration: 8, 12, 16, 20, 24, 28, 32 seconds
                    int lockoutDuration = 8 + ((lockoutCount - 1) * 4);

                    // If we've reached the 32-second lockout and user still fails, close app
                    if (lockoutCount > 3) {
                        showAlert(Alert.AlertType.ERROR, "Maximum Attempts Exceeded",
                                "The application will now close for security reasons.");

                        // Add a brief delay before closing
                        Timeline closeTimer = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
                            Platform.exit();
                        }));
                        closeTimer.play();
                        return;
                    }

                    // Lock the login
                    isLocked = true;
                    loginAttempts = 0; // Reset attempts for next round
                    confirmButton.setDisable(true);
                    emailField.setDisable(true);
                    passwordField.setDisable(true);
                    signInQRButton.setDisable(true);

                    lockoutLabel.setVisible(true);

                    // Show warning if lockout count is increasing
                    if (lockoutCount > 0) {
                        warningLabel.setVisible(true);
                        int remainingAttempts = 4 - lockoutCount;
                        warningLabel.setText("⚠ Warning: Lockout time increased! ");
                        warningLabel.setText(remainingAttempts + " more lockout(s) until system closes.");
                    }

                    // Create countdown timer
                    final int[] secondsRemaining = {lockoutDuration};
                    lockoutLabel.setText("Too many failed attempts. Please wait " + secondsRemaining[0] + " seconds.");

                    if (lockoutTimer != null) {
                        lockoutTimer.stop();
                    }

                    lockoutTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
                        secondsRemaining[0]--;
                        if (secondsRemaining[0] > 0) {
                            lockoutLabel.setText("Too many failed attempts. Please wait " + secondsRemaining[0] + " seconds.");
                        } else {
                            // Unlock
                            isLocked = false;
                            confirmButton.setDisable(false);
                            emailField.setDisable(false);
                            passwordField.setDisable(false);
                            signInQRButton.setDisable(false);
                            lockoutLabel.setVisible(false);
                            lockoutTimer.stop();
                        }
                    }));
                    lockoutTimer.setCycleCount(lockoutDuration);
                    lockoutTimer.play();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Login Failed",
                            "Invalid email or password. Attempt " + loginAttempts + " of 3.");
                }
            }
        });

        exitButton.setOnAction(e -> Platform.exit());

        actionButtons.getChildren().addAll(exitButton, confirmButton);
        mainButtons.getChildren().addAll(signInQRButton, actionButtons);

        // Quote at bottom
        Label quoteLabel = new Label(
                "\"Beware of little expenses; a small leak will sink a great ship.\"\n -Benjamin Franklin"
        );
        quoteLabel.setStyle(
                "-fx-text-fill: #00ffcc;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-family: 'Segoe UI';" +
                        "-fx-text-alignment: center;" +
                        "-fx-wrap-text: true;"
        );
        quoteLabel.setWrapText(true);
        quoteLabel.setMaxWidth(380);
        quoteLabel.setAlignment(Pos.CENTER);
        VBox.setMargin(quoteLabel, new Insets(20, 0, 10, 0));
        FadeTransition fadeInQuote = new FadeTransition(Duration.seconds(1.5), quoteLabel);
        fadeInQuote.setFromValue(0);
        fadeInQuote.setToValue(1);
        fadeInQuote.setDelay(Duration.seconds(0.3));
        fadeInQuote.play();


        pane.getChildren().addAll(emailBox, passwordBox, lockoutLabel, warningLabel, mainButtons, quoteLabel);
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

        // Full Name field
        TextField nameField = new TextField();
        nameField.setPromptText("Full Name/Username");
        nameField.setStyle(getInputFieldStyle());

        // Email field
        TextField emailField = new TextField();
        emailField.setPromptText("Email Address");
        emailField.setStyle(getInputFieldStyle());

        // Password field
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Pin");
        passwordField.setStyle(getInputFieldStyle());

        // Confirm Password field
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm Pin");
        confirmPasswordField.setStyle(getInputFieldStyle());

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button backButton = createActionButton("Back", "#666666", true);
        Button confirmButton = createActionButton("Confirm", "#00ffcc", false);

        backButton.setPrefWidth(120);
        confirmButton.setPrefWidth(120);

        // Fixed back button - now it actually works -JD
        backButton.setOnAction(e -> {
            // Get parent to switch tabs
            StackPane contentArea = (StackPane) pane.getParent();
            if (contentArea != null && contentArea.getChildren().size() >= 2) {
                contentArea.getChildren().get(0).setVisible(true);  // Show sign-in
                pane.setVisible(false);  // Hide sign-up
            }
        });

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
                showAlert(Alert.AlertType.WARNING, "Pin Mismatch", "Pin do not match.");
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

    private void showQRCodeLogin() {
        // Stop any existing scanning first
        stopScanning();

        // Create QR Code login dialog
        Stage qrStage = new Stage();
        qrStage.setTitle("QR Code Login");
        qrStage.initOwner(primaryStage);

        VBox content = new VBox(25);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40));
        content.setStyle("-fx-background-color: #021a1a; -fx-background-radius: 20;");

        Label titleLabel = new Label("BUDGET BUDDY");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 36));
        titleLabel.setStyle("-fx-text-fill: white; -fx-text-alignment: center;");

        Label scanLabel = new Label("Ready to scan QR Code");
        scanLabel.setStyle("-fx-text-fill: #00ffcc; -fx-font-size: 14;");

        // Camera preview area
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

        // Webcam view
        ImageView webcamView = new ImageView();
        webcamView.setFitWidth(380);
        webcamView.setFitHeight(285);
        webcamView.setPreserveRatio(true);

        // Placeholder
        VBox placeholderBox = new VBox(15);
        placeholderBox.setAlignment(Pos.CENTER);
        Label placeholderIcon = new Label("📷");
        placeholderIcon.setFont(Font.font(72));
        Label placeholderText = new Label("Position QR code in frame");
        placeholderText.setStyle("-fx-text-fill: #666666; -fx-font-size: 14;");
        placeholderBox.getChildren().addAll(placeholderIcon, placeholderText);

        cameraArea.getChildren().addAll(placeholderBox, webcamView);
        webcamView.setVisible(false);

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button scanButton = createActionButton("Start Cam", "#00ffcc", false);
        Button uploadButton = createActionButton("Upload QR", "#00ffcc", false);
        Button backButton = createActionButton("Back", "#666666", true);

        scanButton.setPrefWidth(150);
        uploadButton.setPrefWidth(150);

        scanButton.setOnAction(e -> {
            if (!scanning) {
                placeholderBox.setVisible(false);
                webcamView.setVisible(true);
                startQRScanning(webcamView, qrStage, scanLabel);
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

        uploadButton.setOnAction(e -> handleQRImageUpload(qrStage));

        backButton.setOnAction(e -> {
            stopScanning();
            qrStage.close();
        });

        buttonBox.getChildren().addAll(scanButton, uploadButton);

        // Quote
        Label quoteLabel = new Label(
                "\"Beware of little expenses; a small leak will sink a great ship.\"\n -Benjamin Franklin"
        );
        quoteLabel.setStyle(
                "-fx-text-fill: #00ffcc;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-family: 'Segoe UI';" +
                        "-fx-text-alignment: center;" +
                        "-fx-wrap-text: true;"
        );
        quoteLabel.setWrapText(true);
        quoteLabel.setMaxWidth(380);
        quoteLabel.setAlignment(Pos.CENTER);
        VBox.setMargin(quoteLabel, new Insets(20, 0, 10, 0));
        FadeTransition fadeInQuote = new FadeTransition(Duration.seconds(1.5), quoteLabel);
        fadeInQuote.setFromValue(0);
        fadeInQuote.setToValue(1);
        fadeInQuote.setDelay(Duration.seconds(0.3));
        fadeInQuote.play();


        content.getChildren().addAll(titleLabel, scanLabel, cameraArea, buttonBox, backButton, quoteLabel);

        Scene scene = new Scene(content, 600, 800);
        qrStage.setScene(scene);
        qrStage.setOnCloseRequest(e -> stopScanning());
        qrStage.showAndWait();
    }

    private void showPINDialog(String username, Stage parentStage) {
        Stage pinStage = new Stage();
        pinStage.setTitle("Enter PIN");
        pinStage.initOwner(parentStage);

        VBox content = new VBox(25);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40));
        content.setStyle("-fx-background-color: #021a1a; -fx-background-radius: 20;");

        Label titleLabel = new Label("BUDGET BUDDY");
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
                // Verify PIN matches user's PIN
                if (userManager.authenticate(username, pin)) {
                    currentUser = username;
                    pinStage.close();
                    if (parentStage != null) {
                        parentStage.close();
                    }
                    showDashboard(username);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Invalid PIN", "The PIN you entered is incorrect.");
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Invalid PIN", "PIN must be exactly 4 digits.");
            }
        });

        exitButton.setOnAction(e -> pinStage.close());

        buttonBox.getChildren().addAll(exitButton, confirmButton);

        Label quoteLabel = new Label(
                "\"Beware of little expenses; a small leak will sink a great ship.\"\n -Benjamin Franklin"
        );
        quoteLabel.setStyle(
                "-fx-text-fill: #00ffcc;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-family: 'Segoe UI';" +
                        "-fx-text-alignment: center;" +
                        "-fx-wrap-text: true;"
        );
        quoteLabel.setWrapText(true);
        quoteLabel.setMaxWidth(380);
        quoteLabel.setAlignment(Pos.CENTER);
        VBox.setMargin(quoteLabel, new Insets(20, 0, 10, 0));
        FadeTransition fadeInQuote = new FadeTransition(Duration.seconds(1.5), quoteLabel);
        fadeInQuote.setFromValue(0);
        fadeInQuote.setToValue(1);
        fadeInQuote.setDelay(Duration.seconds(0.3));
        fadeInQuote.play();


        content.getChildren().addAll(titleLabel, pinLabel, pinField, buttonBox, quoteLabel);

        Scene scene = new Scene(content, 500, 500);
        pinStage.setScene(scene);
        pinStage.showAndWait();
    }

    private void startQRScanning(ImageView webcamView, Stage parentStage, Label statusLabel) {
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
                                        statusLabel.setText("QR Code detected! Enter your PIN.");
                                        showPINDialog(username, parentStage);
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

    private void handleQRImageUpload(Stage parentStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select QR Code Image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif")
        );

        java.io.File selectedFile = fileChooser.showOpenDialog(parentStage);

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
                    showPINDialog(username, parentStage);
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
        return btn;
    }

    private void showQRCodeSuccessDialog(String username) {
        String qrCode = userManager.getQRCode(username);
        Stage successStage = new Stage();
        successStage.setTitle("Registration Successful!");
        successStage.initOwner(primaryStage);

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

                Button okBtn = createActionButton("OK", "#00ffcc", false);
                okBtn.setOnAction(e -> successStage.close());

                content.getChildren().addAll(successLabel, welcomeLabel, qrImageView, saveBtn, okBtn);
            }
        } catch (Exception e) {
            content.getChildren().addAll(successLabel, welcomeLabel, new Label("QR: " + qrCode));
        }

        Scene scene = new Scene(content, 450, 600);
        successStage.setScene(scene);
        successStage.showAndWait();
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

    private Button createStyledButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: " + color + "; " +
                        "-fx-text-fill: #002f2f; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14; " +
                        "-fx-padding: 10 25; " +
                        "-fx-background-radius: 25; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,255,195,0.25), 10, 0, 0, 0); " +
                        "-fx-cursor: hand;"
        );

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

    private void showDashboard(String username) {
        stopScanning();

        BorderPane dashboard = new BorderPane();
        dashboard.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #003d3d, #00bfa5);"
        );


        HBox topBar = createTopBar(username);
        dashboard.setTop(topBar);

        VBox sideNav = createSideNavigation();
        dashboard.setLeft(sideNav);

        StackPane contentArea = new StackPane();
        contentArea.setPadding(new Insets(20));
        dashboard.setCenter(contentArea);

        showOverview(contentArea, username);

        Scene scene = new Scene(dashboard, 1200, 800);
        primaryStage.setScene(scene);
    }

    private HBox createTopBar(String username) {
        HBox topBar = new HBox(20);
        topBar.setStyle(
                "-fx-background-color: rgba(2, 26, 26, 0.95); "
                        + "-fx-effect: dropshadow(gaussian, rgba(0, 255, 204, 0.4), 40, 0, 0, 0); "
                        + "-fx-padding: 15 13;"
        );

        topBar.setAlignment(Pos.CENTER_LEFT);


        Label titleLabel = new Label("BUDGET BUDDY");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: #00ffc3; -fx-text-alignment: center;");



        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userLabel = new Label("👤 " + username);
        userLabel.setStyle("-fx-text-fill: #f0f0f0;");

        Button logoutBtn = createStyledButton("Logout", "#ff6b6b");
        logoutBtn.setOnAction(e -> showLoginScreen());

        topBar.getChildren().addAll(titleLabel, spacer, userLabel, logoutBtn);
        return topBar;
    }

    private VBox createSideNavigation() {
        VBox sideNav = new VBox(12);
        sideNav.setAlignment(Pos.CENTER);
        sideNav.setPadding(new Insets(30));
        sideNav.setPrefHeight(Double.MAX_VALUE);
        sideNav.setStyle(
                "-fx-background-color: rgba(0,47,47,0.8);  "
                        + "-fx-background-color: rgba(0,47,47,0.8); -fx-padding: 15 30; "
                        + "-fx-padding: 25;"
                        + "-fx-min-width: 230;"
                        + "-fx-max-width: 230;"
        );


        Button overviewBtn = createNavButton("📊 Overview");
        Button budgetsBtn = createNavButton("💼 Budgets");
        Button expensesBtn = createNavButton("💸 Expenses");
        Button incomeBtn = createNavButton("💵 Income");
        Button reportsBtn = createNavButton("📈 Reports");
        Button settingsBtn = createNavButton("⚙️ Settings");

        // --- Navigation Actions ---
        overviewBtn.setOnAction(e -> {
            StackPane contentArea = (StackPane) ((BorderPane) primaryStage.getScene().getRoot()).getCenter();
            showOverview(contentArea, currentUser);
        });

        budgetsBtn.setOnAction(e -> {
            StackPane contentArea = (StackPane) ((BorderPane) primaryStage.getScene().getRoot()).getCenter();
            showBudgets(contentArea);
        });

        expensesBtn.setOnAction(e -> {
            StackPane contentArea = (StackPane) ((BorderPane) primaryStage.getScene().getRoot()).getCenter();
            showExpenses(contentArea);
        });

        incomeBtn.setOnAction(e -> {
            StackPane contentArea = (StackPane) ((BorderPane) primaryStage.getScene().getRoot()).getCenter();
            showIncome(contentArea);
        });

        reportsBtn.setOnAction(e -> {
            StackPane contentArea = (StackPane) ((BorderPane) primaryStage.getScene().getRoot()).getCenter();
            showReports(contentArea);
        });

        settingsBtn.setOnAction(e -> {
            StackPane contentArea = (StackPane) ((BorderPane) primaryStage.getScene().getRoot()).getCenter();
            showSettings(contentArea);
        });

        sideNav.getChildren().addAll(
                overviewBtn,
                budgetsBtn,
                expensesBtn,
                incomeBtn,
                reportsBtn,
                settingsBtn
        );

        return sideNav;
    }

    private Button createNavButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-text-fill: #E0E0E0;"
                        + "-fx-font-size: 15;"
                        + "-fx-font-weight: bold;"
                        + "-fx-padding: 12 18;"
                        + "-fx-cursor: hand;"
                        + "-fx-background-radius: 30;"
        );

        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: rgba(0,255,195,0.25);"
                        + "-fx-text-fill: #00ffc3;"
                        + "-fx-font-size: 15;"
                        + "-fx-font-weight: bold;"
                        + "-fx-padding: 12 18;"
                        + "-fx-cursor: hand;"
                        + "-fx-background-radius: 30;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-text-fill: #E0E0E0;"
                        + "-fx-font-size: 15;"
                        + "-fx-font-weight: bold;"
                        + "-fx-padding: 12 18;"
                        + "-fx-cursor: hand;"
                        + "-fx-background-radius: 30;"
        ));

        return btn;
    }

    private void showOverview(StackPane contentArea, String username) {
        VBox overview = new VBox(20);
        overview.setPadding(new Insets(20));

        Label titleLabel = new Label("Financial Overview");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleLabel.setStyle("-fx-text-fill: White;");

        HBox summaryCards = createSummaryCards();
        HBox charts = new HBox(20);
        charts.getChildren().addAll(createExpenseChart(), createBudgetProgressChart());
        VBox recentTransactions = createRecentTransactionsTable();

        overview.getChildren().addAll(titleLabel, summaryCards, charts, recentTransactions);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(overview);

        FadeTransition fade = new FadeTransition(Duration.millis(300), overview);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private HBox createSummaryCards() {
        HBox cards = new HBox(20);
        double totalIncome = budgetManager.getTotalIncome(currentUser);
        double totalExpenses = budgetManager.getTotalExpenses(currentUser);
        double balance = totalIncome - totalExpenses;

        cards.getChildren().addAll(
                createSummaryCard("Total Income", String.format("₱%.2f", totalIncome), "#00d4aa"),
                createSummaryCard("Total Expenses", String.format("₱%.2f", totalExpenses), "#ff6b6b"),
                createSummaryCard("Balance", String.format("₱%.2f", balance), "#667eea")
        );
        return cards;
    }

    private VBox createSummaryCard(String title, String amount, String color) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setStyle(
                "-fx-background-color: rgba(0,47,47,0.9); "
                        + "-fx-background-radius: 20; "
                        + "-fx-padding: 30; "
                        + "-fx-effect: dropshadow(gaussian, rgba(0,255,200,0.25), 15, 0, 0, 0);"

        );
        card.setPrefWidth(250);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #b0f0e0;");



        Label amountLabel = new Label(amount);
        amountLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        amountLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");

        card.getChildren().addAll(titleLabel, amountLabel);
        return card;
    }

    private VBox createExpenseChart() {
        VBox chartBox = new VBox(10);
        chartBox.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20;");

        Label title = new Label("Expenses by Category");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));


        PieChart pieChart = new PieChart();
        pieChart.setLegendVisible(false);
        pieChart.setPrefSize(350, 300);

        Map<String, Double> expenses = budgetManager.getExpensesByCategory(currentUser);
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        for (Map.Entry<String, Double> entry : expenses.entrySet()) {
            pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }

        pieChart.setData(pieData);
        chartBox.getChildren().addAll(title, pieChart);
        return chartBox;
    }

    private VBox createBudgetProgressChart() {
        VBox chartBox = new VBox(10);
        chartBox.setStyle("-fx-background-color: rgba(0,47,47,0.9); "
                + "-fx-background-radius: 15; -fx-padding: 20;");


        Label title = new Label("Budget Progress");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.setStyle("-fx-text-fill: White;");

        BarChart<String, Number> barChart = createBudgetBarChart();
        barChart.setPrefSize(350, 300);

        chartBox.getChildren().addAll(title, barChart);
        return chartBox;
    }

    private BarChart<String, Number> createBudgetBarChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<String, Budget> budgets = budgetManager.getUserBudgets(currentUser);
        for (Map.Entry<String, Budget> entry : budgets.entrySet()) {
            Budget budget = entry.getValue();
            series.getData().add(new XYChart.Data<>(budget.getCategory(), budget.getSpent()));
        }

        barChart.getData().add(series);
        return barChart;
    }

    private VBox createRecentTransactionsTable() {
        VBox tableBox = new VBox(15);
        tableBox.setStyle("-fx-background-color: rgba(0,47,47,0.9); "
                + "-fx-background-radius: 15; -fx-padding: 20; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,255,200,0.25), 15, 0, 0, 0);");


        Label title = new Label("Recent Transactions");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.setStyle("-fx-text-fill: White;");

        TableView<Transaction> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Transaction, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDate()));

        TableColumn<Transaction, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCategory()));

        TableColumn<Transaction, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDescription()));

        TableColumn<Transaction, String> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.format("₱%.2f", data.getValue().getAmount())
        ));

        table.getColumns().addAll(dateCol, categoryCol, descCol, amountCol);
        table.setItems(FXCollections.observableArrayList(budgetManager.getRecentTransactions(currentUser, 10)));

        tableBox.getChildren().addAll(title, table);
        return tableBox;
    }

    private void showBudgets(StackPane contentArea) {
        VBox budgetsView = new VBox(20);
        budgetsView.setPadding(new Insets(20));

        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("My Budgets");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleLabel.setStyle("-fx-text-fill: White;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBudgetBtn = createStyledButton("+ Add Budget", "#00d4aa");
        addBudgetBtn.setOnAction(e -> showAddBudgetDialog());

        header.getChildren().addAll(titleLabel, spacer, addBudgetBtn);

        VBox budgetsList = new VBox(15);
        Map<String, Budget> budgets = budgetManager.getUserBudgets(currentUser);

        for (Budget budget : budgets.values()) {
            budgetsList.getChildren().add(createBudgetCard(budget));
        }

        ScrollPane scrollPane = new ScrollPane(budgetsList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        budgetsView.getChildren().addAll(header, scrollPane);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(budgetsView);
    }

    private VBox createBudgetCard(Budget budget) {
        VBox card = new VBox(15);
        card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 15; " +
                        "-fx-padding: 25; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
        );

        HBox topRow = new HBox(20);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label categoryLabel = new Label(budget.getCategory());
        categoryLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label amountLabel = new Label(String.format("₱%.2f / ₱%.2f", budget.getSpent(), budget.getLimit()));
        amountLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 16));

        topRow.getChildren().addAll(categoryLabel, spacer, amountLabel);

        ProgressBar progressBar = new ProgressBar(budget.getSpent() / budget.getLimit());
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(12);

        double percentage = (budget.getSpent() / budget.getLimit()) * 100;
        Label percentageLabel = new Label(String.format("%.1f%% used", percentage));
        percentageLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #636e72;");

        card.getChildren().addAll(topRow, progressBar, percentageLabel);
        return card;
    }

    private void showAddBudgetDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Budget");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Food", "Transportation", "Entertainment", "Shopping", "Bills", "Other");
        categoryCombo.setPromptText("Select category");

        TextField limitField = new TextField();
        limitField.setPromptText("Budget limit");

        grid.add(new Label("Category:"), 0, 0);
        grid.add(categoryCombo, 1, 0);
        grid.add(new Label("Limit:"), 0, 1);
        grid.add(limitField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String category = categoryCombo.getValue();
                    double limit = Double.parseDouble(limitField.getText());
                    budgetManager.addBudget(currentUser, category, limit);
                    StackPane contentArea = (StackPane) ((BorderPane) primaryStage.getScene().getRoot()).getCenter();
                    showBudgets(contentArea);
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Invalid input");
                }
            }
        });
    }

    private void showExpenses(StackPane contentArea) {
        VBox expensesView = new VBox(20);
        expensesView.setPadding(new Insets(20));

        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Expenses");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleLabel.setStyle("-fx-text-fill: White;");


        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addExpenseBtn = createStyledButton("+ Add Expense", "#ff6b6b");
        addExpenseBtn.setOnAction(e -> showAddTransactionDialog("Expense"));

        header.getChildren().addAll(titleLabel, spacer, addExpenseBtn);

        TableView<Transaction> table = createTransactionTable(budgetManager.getExpenses(currentUser));

        expensesView.getChildren().addAll(header, table);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(expensesView);
    }

    private void showIncome(StackPane contentArea) {
        VBox incomeView = new VBox(20);
        incomeView.setPadding(new Insets(20));

        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Income");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleLabel.setStyle("-fx-text-fill: White;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addIncomeBtn = createStyledButton("+ Add Income", "#00d4aa");
        addIncomeBtn.setOnAction(e -> showAddTransactionDialog("Income"));

        header.getChildren().addAll(titleLabel, spacer, addIncomeBtn);

        TableView<Transaction> table = createTransactionTable(budgetManager.getIncome(currentUser));

        incomeView.getChildren().addAll(header, table);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(incomeView);
    }

    private TableView<Transaction> createTransactionTable(List<Transaction> transactions) {
        TableView<Transaction> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: white; -fx-background-radius: 15;");

        TableColumn<Transaction, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDate()));

        TableColumn<Transaction, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCategory()));

        TableColumn<Transaction, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDescription()));

        TableColumn<Transaction, String> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.format("₱%.2f", data.getValue().getAmount())
        ));

        TableColumn<Transaction, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("🗑️");

            {
                deleteBtn.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setOnAction(e -> {
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        Transaction transaction = getTableView().getItems().get(getIndex());
                        budgetManager.deleteTransaction(currentUser, transaction.getId());
                        getTableView().getItems().remove(transaction);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
                setAlignment(Pos.CENTER);
            }
        });

        table.getColumns().addAll(dateCol, categoryCol, descCol, amountCol, actionCol);
        table.setItems(FXCollections.observableArrayList(transactions));
        return table;
    }

    private void showAddTransactionDialog(String type) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add " + type);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        DatePicker datePicker = new DatePicker(LocalDate.now());
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Food", "Transportation", "Entertainment", "Shopping", "Bills", "Salary", "Gift", "Other");
        TextField descriptionField = new TextField();
        TextField amountField = new TextField();

        grid.add(new Label("Date:"), 0, 0);
        grid.add(datePicker, 1, 0);
        grid.add(new Label("Category:"), 0, 1);
        grid.add(categoryCombo, 1, 1);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(descriptionField, 1, 2);
        grid.add(new Label("Amount:"), 0, 3);
        grid.add(amountField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String date = datePicker.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);
                    String category = categoryCombo.getValue();
                    String description = descriptionField.getText();
                    double amount = Double.parseDouble(amountField.getText());

                    if (type.equals("Expense")) {
                        budgetManager.addExpense(currentUser, date, category, description, amount);
                    } else {
                        budgetManager.addIncome(currentUser, date, category, amount);
                    }

                    StackPane contentArea = (StackPane) ((BorderPane) primaryStage.getScene().getRoot()).getCenter();
                    if (type.equals("Expense")) {
                        showExpenses(contentArea);
                    } else {
                        showIncome(contentArea);
                    }
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Invalid amount entered.");
                }
            }
        });
    }

    private void showReports(StackPane contentArea) {
        VBox reportsView = new VBox(20);
        reportsView.setPadding(new Insets(20));

        Label titleLabel = new Label("Reports & Analytics");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleLabel.setStyle("-fx-text-fill: White;");

        LineChart<String, Number> lineChart = createMonthlyTrendChart();

        reportsView.getChildren().addAll(titleLabel, lineChart);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(reportsView);
    }

    private LineChart<String, Number> createMonthlyTrendChart() {


        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Amount (₱)");
        yAxis.lookup(".axis-label");
        yAxis.setStyle("-fx-tick-label-fill: white; -fx-label-fill: white;");


        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Income vs. Expenses");
        lineChart.setPrefHeight(500);
        lineChart.lookup(".chart-title");
        lineChart.setStyle("-fx-text-fill: white;");

        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName("Income");

        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName("Expenses");

        Map<String, Double> monthlyIncome = budgetManager.getMonthlyIncome(currentUser);
        Map<String, Double> monthlyExpenses = budgetManager.getMonthlyExpenses(currentUser);

        Set<String> allMonths = new TreeSet<>();
        allMonths.addAll(monthlyIncome.keySet());
        allMonths.addAll(monthlyExpenses.keySet());

        for (String month : allMonths) {
            incomeSeries.getData().add(new XYChart.Data<>(month, monthlyIncome.getOrDefault(month, 0.0)));
            expenseSeries.getData().add(new XYChart.Data<>(month, monthlyExpenses.getOrDefault(month, 0.0)));
        }

        lineChart.getData().addAll(incomeSeries, expenseSeries);
        return lineChart;
    }

    private void showSettings(StackPane contentArea) {
        VBox settingsView = new VBox(20);
        settingsView.setPadding(new Insets(20));

        Label titleLabel = new Label("Settings");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleLabel.setStyle("-fx-text-fill: White;");

        VBox pinChangeSection = new VBox(10);
        Label pinTitle = new Label("Change PIN");
        pinTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        pinTitle.setStyle("-fx-text-fill: White;");


        PasswordField newPinField = new PasswordField();
        newPinField.setPromptText("Enter new 4-digit PIN");

        Button changePinBtn = createStyledButton("Update PIN", "#667eea");
        changePinBtn.setOnAction(e -> {
            String newPin = newPinField.getText();
            if (newPin.matches("\\d{4}")) {
                if (userManager.changePin(currentUser, newPin)) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "PIN updated successfully!");
                    newPinField.clear();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to update PIN.");
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Invalid PIN", "PIN must be exactly 4 digits.");
            }
        });

        pinChangeSection.getChildren().addAll(pinTitle, newPinField, changePinBtn);

        VBox exportSection = new VBox(10);
        Label exportTitle = new Label("Export Data");
        exportTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        exportTitle.setStyle("-fx-text-fill: White;");

        Button exportBtn = createStyledButton("Export to CSV", "#00d4aa");
        exportBtn.setOnAction(e -> {
            String filePath = System.getProperty("user.home") + "/BudgetBuddy_Export.csv";
            if (budgetManager.exportUserData(currentUser, filePath)) {
                showAlert(Alert.AlertType.INFORMATION, "Export Complete", "Data exported to: " + filePath);
            } else {
                showAlert(Alert.AlertType.ERROR, "Export Failed", "Could not export data.");
            }
        });

        exportSection.getChildren().addAll(exportTitle, exportBtn);

        settingsView.getChildren().addAll(titleLabel, pinChangeSection, exportSection);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(settingsView);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

