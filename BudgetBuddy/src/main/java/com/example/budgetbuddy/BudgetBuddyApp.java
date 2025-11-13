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
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.media.Media;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.scene.chart.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.animation.*;
import java.io.File;
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
    private Circle profilePictureCircle;
    private int rewardPoints = 0;
    private Map<String, Integer> userRewardPoints = new HashMap<>();
    private MediaPlayer loginMediaPlayer;
    private MediaView loginMediaView;


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
        StackPane root = new StackPane();

        try {
            if (loginMediaPlayer == null) {
                String videoPath = getClass().getResource("/videos/AuroraBorealis.mp4").toExternalForm();
                Media media = new Media(videoPath);
                loginMediaPlayer = new MediaPlayer(media);
                loginMediaView = new MediaView(loginMediaPlayer);

                loginMediaView.setPreserveRatio(false);
                loginMediaView.fitWidthProperty().bind(root.widthProperty());
                loginMediaView.fitHeightProperty().bind(root.heightProperty());

                loginMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                loginMediaPlayer.setMute(true);
                loginMediaPlayer.play();
            }

            root.getChildren().add(loginMediaView);

        } catch (Exception e) {
            System.err.println("Video not found, using fallback background: " + e.getMessage());
            root.setStyle("-fx-background-color: linear-gradient(to bottom right, #001a1a, #003d3d, #00ffcc);");
        }

        Rectangle overlay = new Rectangle();
        overlay.setFill(Color.rgb(0, 0, 0, 0.45));
        overlay.widthProperty().bind(root.widthProperty());
        overlay.heightProperty().bind(root.heightProperty());
        root.getChildren().add(overlay);

        VBox loginCard = createModernLoginCard();
        root.getChildren().add(loginCard);
        StackPane.setAlignment(loginCard, Pos.CENTER_LEFT);
        StackPane.setMargin(loginCard, new Insets(0, 0, 0, 80));

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(1200), loginCard);
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

        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("BUDGET BUDDY");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 50));
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

        signInTab.setPrefWidth(170);
        signUpTab.setPrefWidth(170);

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
            return "-fx-background-color: #00ffcc; -fx-text-fill: #021a1a; -fx-font-weight: bold; -fx-font-size: 14; -fx-padding: 12 40; -fx-background-radius: 25; -fx-border-width: 0;";
        } else {
            return "-fx-background-color: transparent; -fx-text-fill: #00ffcc; -fx-font-weight: bold; -fx-font-size: 14; -fx-padding: 12 40; -fx-background-radius: 25; -fx-border-color: #00ffcc; -fx-border-width: 2; -fx-border-radius: 25;";
        }
    }

    private int loginAttempts = 0;
    private int lockoutCount = 0;
    private boolean isLocked = false;
    private Timeline lockoutTimer;

    private VBox createSignInPane() {
        VBox pane = new VBox(17);
        pane.setAlignment(Pos.CENTER);
        pane.setPadding(new Insets(30));

        Label emailLabel = new Label("Username");
        emailLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13;");

        TextField emailField = new TextField();
        emailField.setPromptText("Enter your Username");
        emailField.setStyle(getInputFieldStyle());

        VBox emailBox = new VBox(12, emailLabel, emailField);

        Label passwordLabel = new Label("Pin (4 digits)");
        passwordLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13;");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your 4-digit Pin");
        passwordField.setStyle(getInputFieldStyle());

        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d{0,4}")) {
                passwordField.setText(oldVal);
            }
        });

        VBox passwordBox = new VBox(12, passwordLabel, passwordField);

        Label lockoutLabel = new Label();
        lockoutLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12; -fx-font-weight: bold;");
        lockoutLabel.setVisible(false);

        Label warningLabel = new Label();
        warningLabel.setStyle("-fx-text-fill: #ffaa00; -fx-font-size: 11; -fx-font-style: italic;");
        warningLabel.setVisible(false);

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
            if (isLocked) return;

            String email = emailField.getText().trim();
            String password = passwordField.getText().trim();

            if (email.isEmpty() || password.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Missing Information", "Please enter both username and pin.");
                return;
            }

            if (!password.matches("\\d{4}")) {
                showAlert(Alert.AlertType.WARNING, "Invalid Pin", "Pin must be exactly 4 digits.");
                return;
            }

            if (userManager.authenticate(email, password)) {
                currentUser = email;
                loginAttempts = 0;
                lockoutCount = 0;
                warningLabel.setVisible(false);
                showDashboard(email);
            } else {
                loginAttempts++;

                if (loginAttempts >= 3) {
                    lockoutCount++;
                    int lockoutDuration = 8 + ((lockoutCount - 1) * 4);

                    if (lockoutCount > 3) {
                        showAlert(Alert.AlertType.ERROR, "Maximum Attempts Exceeded", "The application will now close for security reasons.");
                        Timeline closeTimer = new Timeline(new KeyFrame(Duration.seconds(2), event -> Platform.exit()));
                        closeTimer.play();
                        return;
                    }

                    isLocked = true;
                    loginAttempts = 0;
                    confirmButton.setDisable(true);
                    emailField.setDisable(true);
                    passwordField.setDisable(true);
                    signInQRButton.setDisable(true);

                    lockoutLabel.setVisible(true);

                    if (lockoutCount > 0) {
                        warningLabel.setVisible(true);
                        int remainingAttempts = 4 - lockoutCount;
                        warningLabel.setText("⚠ Warning: " + remainingAttempts + " more lockout(s) until system closes.");
                    }

                    final int[] secondsRemaining = {lockoutDuration};
                    lockoutLabel.setText("Too many failed attempts. Wait " + secondsRemaining[0] + " seconds.");

                    if (lockoutTimer != null) lockoutTimer.stop();

                    lockoutTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
                        secondsRemaining[0]--;
                        if (secondsRemaining[0] > 0) {
                            lockoutLabel.setText("Too many failed attempts. Wait " + secondsRemaining[0] + " seconds.");
                        } else {
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
                    showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or pin. Attempt " + loginAttempts + " of 3.");
                }
            }
        });

        exitButton.setOnAction(e -> Platform.exit());

        actionButtons.getChildren().addAll(exitButton, confirmButton);
        mainButtons.getChildren().addAll(signInQRButton, actionButtons);

        Label quoteLabel = new Label("\"Beware of little expenses; a small leak will sink a great ship.\"\n -Benjamin Franklin");
        quoteLabel.setStyle("-fx-text-fill: #00ffcc; -fx-font-size: 11px; -fx-text-alignment: center; -fx-wrap-text: true;");
        quoteLabel.setWrapText(true);
        quoteLabel.setMaxWidth(380);
        quoteLabel.setAlignment(Pos.CENTER);

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

        TextField nameField = new TextField();
        nameField.setPromptText("Username");
        nameField.setStyle(getInputFieldStyle());

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("4-digit Pin");
        passwordField.setStyle(getInputFieldStyle());

        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d{0,4}")) passwordField.setText(oldVal);
        });

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm 4-digit Pin");
        confirmPasswordField.setStyle(getInputFieldStyle());

        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d{0,4}")) confirmPasswordField.setText(oldVal);
        });

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button backButton = createActionButton("Back", "#666666", true);
        Button confirmButton = createActionButton("Confirm", "#00ffcc", false);

        backButton.setPrefWidth(120);
        confirmButton.setPrefWidth(120);

        backButton.setOnAction(e -> {
            StackPane contentArea = (StackPane) pane.getParent();
            if (contentArea != null && contentArea.getChildren().size() >= 2) {
                contentArea.getChildren().get(0).setVisible(true);
                pane.setVisible(false);
            }
        });

        confirmButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String password = passwordField.getText().trim();
            String confirmPassword = confirmPasswordField.getText().trim();

            if (name.isEmpty() || password.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Missing Information", "Please fill in all fields.");
                return;
            }

            if (!password.matches("\\d{4}")) {
                showAlert(Alert.AlertType.WARNING, "Invalid Pin", "Pin must be exactly 4 digits.");
                return;
            }

            if (!password.equals(confirmPassword)) {
                showAlert(Alert.AlertType.WARNING, "Pin Mismatch", "Pins do not match.");
                return;
            }

            if (userManager.registerUser(name, password)) {
                showQRCodeSuccessDialog(name);
            } else {
                showAlert(Alert.AlertType.ERROR, "Registration Failed", "Username already exists.");
            }
        });

        buttonBox.getChildren().addAll(backButton, confirmButton);

        pane.getChildren().addAll(headerBox, nameField, passwordField, confirmPasswordField, buttonBox);
        return pane;
    }

    private void showQRCodeLogin() {
        Stage qrStage = new Stage();
        qrStage.setTitle("QR Code Login");
        qrStage.initOwner(primaryStage);

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: #021a1a;");

        Label titleLabel = new Label("Scan QR Code to Login");
        titleLabel.setStyle("-fx-font-size: 18; -fx-text-fill: #00ffcc; -fx-font-weight: bold;");

        ImageView webcamView = new ImageView();
        webcamView.setFitWidth(480);
        webcamView.setFitHeight(360);
        webcamView.setStyle("-fx-border-color: #00ffcc; -fx-border-width: 2;");

        Label statusLabel = new Label("Initializing camera...");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14;");

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button uploadBtn = createActionButton("Upload QR Image", "#667eea", false);
        Button cancelBtn = createActionButton("Cancel", "#ff6b6b", true);

        uploadBtn.setOnAction(e -> {
            stopScanning();
            handleQRImageUpload(qrStage);
        });

        cancelBtn.setOnAction(e -> {
            stopScanning();
            qrStage.close();
        });

        buttonBox.getChildren().addAll(uploadBtn, cancelBtn);
        content.getChildren().addAll(titleLabel, webcamView, statusLabel, buttonBox);

        Scene scene = new Scene(content, 600, 550);
        qrStage.setScene(scene);
        qrStage.setOnCloseRequest(e -> stopScanning());
        qrStage.show();

        new Thread(() -> startQRScanning(webcamView, qrStage, statusLabel)).start();
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

        // Auto-limit to 4 digits ONLY
        pinField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d{0,4}")) {
                pinField.setText(oldVal);
            }
        });

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button exitButton = createActionButton("Exit", "#ff6b6b", true);
        Button confirmButton = createActionButton("Confirm", "#00ffcc", false);

        exitButton.setPrefWidth(120);
        confirmButton.setPrefWidth(120);

        confirmButton.setOnAction(e -> {
            String pin = pinField.getText().trim();

            // Validate: must be exactly 4 digits
            if (!pin.matches("\\d{4}")) {
                showAlert(Alert.AlertType.WARNING, "Invalid PIN", "PIN must be exactly 4 digits.");
                return;
            }

            // Check authentication
            if (userManager.authenticate(username, pin)) {
                currentUser = username;

                // Close both windows
                pinStage.close();
                if (parentStage != null) parentStage.close();

                showDashboard(username);
            } else {
                // Wrong PIN → Show error + reopen PIN dialog
                showAlert(Alert.AlertType.ERROR, "Invalid PIN", "The PIN you entered is incorrect.");
                pinStage.close();

                // Reopen same PIN Dialog (same behavior as your Dialog code)
                Platform.runLater(() -> showPINDialog(username, parentStage));
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
                Platform.runLater(() -> {
                    statusLabel.setText("No webcam detected!");
                    statusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 14;");
                });
                return;
            }

            webcam.setViewSize(WebcamResolution.VGA.getSize());
            webcam.open();

            scanning = true;
            executor = Executors.newSingleThreadExecutor();

            executor.submit(() -> {
                Platform.runLater(() -> statusLabel.setText("Scanning for QR code..."));

                while (scanning && webcam.isOpen()) {
                    try {
                        BufferedImage image = webcam.getImage();
                        if (image != null) {
                            Platform.runLater(() -> {
                                Image fxImage = SwingFXUtils.toFXImage(image, null);
                                webcamView.setImage(fxImage);
                            });

                            try {
                                LuminanceSource source = new BufferedImageLuminanceSource(image);
                                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                                Result result = new MultiFormatReader().decode(bitmap);
                                String qrCode = result.getText();

                                String username = userManager.authenticateQR(qrCode);
                                if (username != null) {
                                    scanning = false;
                                    Platform.runLater(() -> {
                                        stopScanning();
                                        showPINDialog(username, parentStage);
                                    });
                                    break;
                                }
                            } catch (NotFoundException e) {
                                // No QR code found, continue scanning
                            }
                        }

                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                statusLabel.setText("Error: " + e.getMessage());
                statusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 14;");
            });
            e.printStackTrace();
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
        return "-fx-background-color: white; -fx-text-fill: #021a1a; -fx-font-size: 14; -fx-padding: 12 15; -fx-background-radius: 8; -fx-border-radius: 8; -fx-pref-height: 45;";
    }

    private Button createActionButton(String text, String color, boolean isSecondary) {
        Button btn = new Button(text);
        if (isSecondary) {
            btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13; -fx-padding: 12 25; -fx-background-radius: 8; -fx-cursor: hand;");
        } else {
            btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: #021a1a; -fx-font-weight: bold; -fx-font-size: 13; -fx-padding: 12 25; -fx-background-radius: 8; -fx-cursor: hand;");
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

    // ============ RECEIPT SCANNER METHODS ============

    private void showReceiptScanDialog() {
        stopScanning();

        Stage receiptStage = new Stage();
        receiptStage.setTitle("Scan Receipt QR Code");
        receiptStage.initOwner(primaryStage);

        VBox content = new VBox(25);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40));
        content.setStyle("-fx-background-color: #021a1a; -fx-background-radius: 20;");

        Label titleLabel = new Label("RECEIPT SCANNER");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 32));
        titleLabel.setStyle("-fx-text-fill: white; -fx-text-alignment: center;");

        Label instructionLabel = new Label("Upload or scan a receipt QR code");
        instructionLabel.setStyle("-fx-text-fill: #00ffcc; -fx-font-size: 14;");

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
        Label placeholderIcon = new Label("📄");
        placeholderIcon.setFont(Font.font(72));
        Label placeholderText = new Label("Position receipt QR code in frame");
        placeholderText.setStyle("-fx-text-fill: #666666; -fx-font-size: 14;");
        placeholderBox.getChildren().addAll(placeholderIcon, placeholderText);

        cameraArea.getChildren().addAll(placeholderBox, webcamView);
        webcamView.setVisible(false);

        Label statusLabel = new Label("Ready to scan");
        statusLabel.setStyle("-fx-text-fill: #00ffcc; -fx-font-size: 13;");

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button scanButton = createActionButton("Start Camera", "#00ffcc", false);
        Button uploadButton = createActionButton("Upload Image", "#00ffcc", false);
        Button backButton = createActionButton("Back", "#666666", true);

        scanButton.setPrefWidth(150);
        uploadButton.setPrefWidth(150);

        scanButton.setOnAction(e -> {
            if (!scanning) {
                placeholderBox.setVisible(false);
                webcamView.setVisible(true);
                startReceiptQRScanning(webcamView, receiptStage, statusLabel);
                scanButton.setText("Stop Camera");
                statusLabel.setText("Scanning... Point receipt QR at camera");
            } else {
                stopScanning();
                webcamView.setVisible(false);
                placeholderBox.setVisible(true);
                scanButton.setText("Start Camera");
                statusLabel.setText("Scanning stopped");
            }
        });

        uploadButton.setOnAction(e -> handleReceiptQRImageUpload(receiptStage, statusLabel));

        backButton.setOnAction(e -> {
            stopScanning();
            receiptStage.close();
        });

        buttonBox.getChildren().addAll(scanButton, uploadButton);

        content.getChildren().addAll(
                titleLabel,
                instructionLabel,
                cameraArea,
                statusLabel,
                buttonBox,
                backButton
        );

        Scene scene = new Scene(content, 600, 750);
        receiptStage.setScene(scene);
        receiptStage.setOnCloseRequest(e -> stopScanning());
        receiptStage.showAndWait();
    }

    private void startReceiptQRScanning(ImageView webcamView, Stage parentStage, Label statusLabel) {
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
                                String qrData = result.getText();
                                scanning = false;

                                Platform.runLater(() -> {
                                    stopScanning();
                                    processReceiptQR(qrData, parentStage, statusLabel);
                                });
                            } catch (NotFoundException ex) {
                                // Continue scanning
                            }
                            reader.reset();
                        }
                        Thread.sleep(100);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Camera Error", "Failed to start camera: " + ex.getMessage());
        }
    }

    private void handleReceiptQRImageUpload(Stage parentStage, Label statusLabel) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Receipt QR Code Image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif")
        );

        java.io.File selectedFile = fileChooser.showOpenDialog(parentStage);

        if (selectedFile != null) {
            try {
                BufferedImage bufferedImage = javax.imageio.ImageIO.read(selectedFile);
                if (bufferedImage == null) throw new Exception("Could not read image file");

                String qrData = ReceiptQRScanner.scanQRFromImage(bufferedImage);
                processReceiptQR(qrData, parentStage, statusLabel);
            } catch (NotFoundException ex) {
                showAlert(Alert.AlertType.ERROR, "No QR Code Found", "No QR code detected in image.");
                statusLabel.setText("No QR code found in image");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to read QR code: " + ex.getMessage());
                statusLabel.setText("Error reading image");
            }
        }
    }

    private void processReceiptQR(String qrData, Stage parentStage, Label statusLabel) {
        ReceiptQRScanner.ReceiptData receipt = ReceiptQRScanner.parseReceiptQR(qrData);

        if (receipt != null) {
            statusLabel.setText("Receipt detected! Review details...");
            showReceiptConfirmationDialog(receipt, parentStage);
        } else {
            showAlert(Alert.AlertType.ERROR, "Invalid Receipt",
                    "The QR code does not contain valid receipt data.\n\n" +
                            "Expected format: Merchant|Date|Amount|Category|Items");
            statusLabel.setText("Invalid receipt QR code");
        }
    }

    private void showReceiptConfirmationDialog(ReceiptQRScanner.ReceiptData receipt, Stage parentStage) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Confirm Receipt Details");
        dialog.initOwner(parentStage);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        Label infoLabel = new Label("Review and edit receipt details:");
        infoLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        grid.add(infoLabel, 0, 0, 2, 1);

        DatePicker datePicker = new DatePicker();
        try {
            datePicker.setValue(LocalDate.parse(receipt.getDate()));
        } catch (Exception e) {
            datePicker.setValue(LocalDate.now());
        }

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll(ReceiptQRScanner.getExpenseCategories());
        categoryCombo.setValue(receipt.getCategory());

        TextField merchantField = new TextField(receipt.getMerchant());
        TextField amountField = new TextField(String.format("%.2f", receipt.getAmount()));
        TextArea itemsArea = new TextArea(receipt.getItems());
        itemsArea.setPrefRowCount(3);
        itemsArea.setPromptText("Items (optional)");

        grid.add(new Label("Merchant:"), 0, 1);
        grid.add(merchantField, 1, 1);
        grid.add(new Label("Date:"), 0, 2);
        grid.add(datePicker, 1, 2);
        grid.add(new Label("Category:"), 0, 3);
        grid.add(categoryCombo, 1, 3);
        grid.add(new Label("Amount:"), 0, 4);
        grid.add(amountField, 1, 4);
        grid.add(new Label("Items:"), 0, 5);
        grid.add(itemsArea, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String date = datePicker.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);
                    String category = categoryCombo.getValue();
                    String merchant = merchantField.getText().trim();
                    double amount = Double.parseDouble(amountField.getText().trim());
                    String items = itemsArea.getText().trim();

                    String description = merchant;
                    if (!items.isEmpty()) {
                        description += " - " + items;
                    }

                    budgetManager.addExpense(currentUser, date, category, description, amount);

                    parentStage.close();
                    showAlert(Alert.AlertType.INFORMATION, "Success",
                            String.format("Receipt added successfully!\n\n%s\n₱%.2f", description, amount));

                    StackPane contentArea = (StackPane) ((BorderPane) primaryStage.getScene().getRoot()).getCenter();
                    showExpenses(contentArea);
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Amount", "Please enter a valid amount.");
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to add expense: " + e.getMessage());
                }
            }
        });
    }

    // ============ END RECEIPT SCANNER METHODS ============


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
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: #002f2f; -fx-font-weight: bold; -fx-font-size: 14; -fx-padding: 10 25; -fx-background-radius: 25; -fx-effect: dropshadow(gaussian, rgba(0,255,195,0.25), 10, 0, 0, 0); -fx-cursor: hand;");
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
        dashboard.setStyle("-fx-background-color: linear-gradient(to bottom right, #003d3d, #00bfa5);");

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
        topBar.setStyle("-fx-background-color: rgba(2, 26, 26, 0.95); -fx-effect: dropshadow(gaussian, rgba(0, 255, 204, 0.4), 40, 0, 0, 0); -fx-padding: 15 13;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("BUDGET BUDDY");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: #00ffc3;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        profilePictureCircle = createProfilePictureCircle(username);

        Label userLabel = new Label(username);
        userLabel.setStyle("-fx-text-fill: #f0f0f0; -fx-font-size: 14; -fx-font-weight: bold;");

        // Reward Points Display
        int currentPoints = userManager.getRewardPoints(username);
        userRewardPoints.put(username, currentPoints);

        Label rewardLabel = new Label("🏆 " + currentPoints + " pts");
        rewardLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 14; -fx-font-weight: bold;");

        HBox userInfo = new HBox(12, profilePictureCircle, userLabel, rewardLabel);
        userInfo.setAlignment(Pos.CENTER_LEFT);

        Button logoutBtn = createStyledButton("Logout", "#ff6b6b");
        logoutBtn.setOnAction(e -> showLoginScreen());

        topBar.getChildren().addAll(titleLabel, spacer, userInfo, logoutBtn);
        return topBar;
    }

    private Circle createProfilePictureCircle(String username) {
        Circle circle = new Circle(25);
        String profilePicture = userManager.getProfilePicture(username);

        if (profilePicture != null && !profilePicture.isEmpty() && !profilePicture.startsWith("#")) {
            try {
                File file = new File(profilePicture);
                if (file.exists()) {
                    Image img = new Image(file.toURI().toString());
                    circle.setFill(new ImagePattern(img));
                } else {
                    circle.setFill(Color.web(getDefaultProfileColor(username)));
                }
            } catch (Exception e) {
                circle.setFill(Color.web(getDefaultProfileColor(username)));
            }
        } else if (profilePicture != null && profilePicture.startsWith("#")) {
            circle.setFill(Color.web(profilePicture));
        } else {
            String defaultColor = getDefaultProfileColor(username);
            userManager.updateProfilePicture(username, defaultColor);
            circle.setFill(Color.web(defaultColor));
        }

        circle.setStroke(Color.web("#00ffcc"));
        circle.setStrokeWidth(2.5);
        circle.setStyle("-fx-cursor: hand;");
        circle.setOnMouseClicked(e -> showProfilePictureDialog(username));

        return circle;
    }

    private String getDefaultProfileColor(String username) {
        String[] colors = {"#FF6B6B", "#4ECDC4", "#45B7D1", "#FFA07A", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E2", "#F8B739", "#52B788", "#E07A5F", "#81B29A"};
        int hash = Math.abs(username.hashCode());
        return colors[hash % colors.length];
    }

    private void showProfilePictureDialog(String username) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Change Profile Picture");
        dialog.initOwner(primaryStage);

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: #021a1a;");

        Label titleLabel = new Label("Customize Your Profile");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold;");

        // Current profile picture preview
        Circle previewCircle = new Circle(60);
        String currentPfp = userManager.getProfilePicture(username);
        if (currentPfp != null && !currentPfp.isEmpty() && !currentPfp.startsWith("#")) {
            try {
                File file = new File(currentPfp);
                if (file.exists()) {
                    Image img = new Image(file.toURI().toString());
                    previewCircle.setFill(new ImagePattern(img));
                } else {
                    previewCircle.setFill(Color.web(getDefaultProfileColor(username)));
                }
            } catch (Exception e) {
                previewCircle.setFill(Color.web(getDefaultProfileColor(username)));
            }
        } else if (currentPfp != null && currentPfp.startsWith("#")) {
            previewCircle.setFill(Color.web(currentPfp));
        } else {
            previewCircle.setFill(Color.web(getDefaultProfileColor(username)));
        }
        previewCircle.setStroke(Color.web("#00ffcc"));
        previewCircle.setStrokeWidth(3);

        Label subtitleLabel = new Label("Choose a color:");
        subtitleLabel.setStyle("-fx-text-fill: #00ffcc; -fx-font-size: 14;");

        // Store the selected image path
        final String[] selectedImagePath = {null};

        // Color picker grid
        GridPane colorGrid = new GridPane();
        colorGrid.setHgap(10);
        colorGrid.setVgap(10);
        colorGrid.setAlignment(Pos.CENTER);

        String[] colors = {
                "#FF6B6B", "#4ECDC4", "#45B7D1", "#FFA07A",
                "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E2",
                "#F8B739", "#52B788", "#E07A5F", "#81B29A",
                "#FF85A1", "#6C5CE7", "#00D2D3", "#FFEAA7"
        };

        int col = 0;
        int row = 0;
        for (String color : colors) {
            Circle colorCircle = new Circle(20);
            colorCircle.setFill(Color.web(color));
            colorCircle.setStroke(Color.web("#00ffcc"));
            colorCircle.setStrokeWidth(2);
            colorCircle.setStyle("-fx-cursor: hand;");

            colorCircle.setOnMouseClicked(e -> {
                previewCircle.setFill(Color.web(color));
                selectedImagePath[0] = null; // Clear any selected image
            });

            colorCircle.setOnMouseEntered(e -> {
                colorCircle.setScaleX(1.2);
                colorCircle.setScaleY(1.2);
            });
            colorCircle.setOnMouseExited(e -> {
                colorCircle.setScaleX(1.0);
                colorCircle.setScaleY(1.0);
            });

            colorGrid.add(colorCircle, col, row);

            col++;
            if (col >= 4) {
                col = 0;
                row++;
            }
        }

        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #00ffcc;");

        Label orLabel = new Label("Or upload an image:");
        orLabel.setStyle("-fx-text-fill: #00ffcc; -fx-font-size: 14;");

        Button uploadButton = createActionButton("📁 Upload Image", "#667eea", false);
        uploadButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Profile Picture");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
            );

            File selectedFile = fileChooser.showOpenDialog(dialog.getOwner());
            if (selectedFile != null) {
                try {
                    // Create profile_pictures directory if it doesn't exist
                    File profilePicsDir = new File("profile_pictures");
                    if (!profilePicsDir.exists()) {
                        profilePicsDir.mkdirs();
                    }

                    // Copy file to profile_pictures directory
                    String extension = selectedFile.getName().substring(selectedFile.getName().lastIndexOf("."));
                    File destFile = new File(profilePicsDir, username + "_profile" + extension);

                    java.nio.file.Files.copy(
                            selectedFile.toPath(),
                            destFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    );

                    // Store the path and update preview
                    selectedImagePath[0] = destFile.getAbsolutePath();
                    Image img = new Image(destFile.toURI().toString());
                    previewCircle.setFill(new ImagePattern(img));
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to load image: " + ex.getMessage());
                }
            }
        });

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button cancelBtn = createActionButton("Cancel", "#666666", true);
        Button saveBtn = createActionButton("Save", "#00ffcc", false);

        cancelBtn.setOnAction(e -> dialog.close());
        saveBtn.setOnAction(e -> {
            // Check if an image was uploaded
            if (selectedImagePath[0] != null) {
                // Save the image path
                userManager.updateProfilePicture(username, selectedImagePath[0]);

                // Update the profile picture in the top bar
                try {
                    Image img = new Image(new File(selectedImagePath[0]).toURI().toString());
                    profilePictureCircle.setFill(new ImagePattern(img));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                showAlert(Alert.AlertType.INFORMATION, "Image Saved",
                        "Profile picture updated! Your image has been set.");
                dialog.close();
            } else if (previewCircle.getFill() instanceof Color) {
                // Save the color
                Color selectedColor = (Color) previewCircle.getFill();
                String colorHex = String.format("#%02X%02X%02X",
                        (int) (selectedColor.getRed() * 255),
                        (int) (selectedColor.getGreen() * 255),
                        (int) (selectedColor.getBlue() * 255));

                userManager.updateProfilePicture(username, colorHex);

                // Update the profile picture in the top bar
                profilePictureCircle.setFill(Color.web(colorHex));

                showAlert(Alert.AlertType.INFORMATION, "Color Saved",
                        "Profile picture color updated successfully!");
                dialog.close();
            } else {
                // Image pattern from existing profile picture
                showAlert(Alert.AlertType.WARNING, "No Changes",
                        "Please select a color or upload an image.");
            }
        });

        buttonBox.getChildren().addAll(cancelBtn, saveBtn);

        content.getChildren().addAll(
                titleLabel,
                previewCircle,
                subtitleLabel,
                colorGrid,
                separator,
                orLabel,
                uploadButton,
                buttonBox
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);

        dialog.showAndWait();
        // Profile picture dialog (keeping original implementation)
        // Implementation remains same as original
    }

    private VBox createSideNavigation() {
        VBox sideNav = new VBox(50);
        sideNav.setAlignment(Pos.CENTER);
        sideNav.setPadding(new Insets(30));
        sideNav.setPrefHeight(Double.MAX_VALUE);
        sideNav.setStyle("-fx-background-color: rgba(0,47,47,0.8); -fx-padding: 15 30; -fx-min-width: 230; -fx-max-width: 230;");

        Button overviewBtn = createNavButton("📊 Overview");
        Button budgetsBtn = createNavButton("💼 Budgets");
        Button expensesBtn = createNavButton("💸 Expenses");
        Button incomeBtn = createNavButton("💵 Income");
        Button reportsBtn = createNavButton("📈 Reports");
        Button rewardsBtn = createNavButton("🏆 Rewards");
        Button settingsBtn = createNavButton("⚙ Settings");

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

        rewardsBtn.setOnAction(e -> {
            StackPane contentArea = (StackPane) ((BorderPane) primaryStage.getScene().getRoot()).getCenter();
            showRewards(contentArea);
        });

        settingsBtn.setOnAction(e -> {
            StackPane contentArea = (StackPane) ((BorderPane) primaryStage.getScene().getRoot()).getCenter();
            showSettings(contentArea);
        });

        sideNav.getChildren().addAll(overviewBtn, budgetsBtn, expensesBtn, incomeBtn, reportsBtn, rewardsBtn, settingsBtn);
        return sideNav;
    }

    private Button createNavButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #E0E0E0; -fx-font-size: 15; -fx-font-weight: bold; -fx-padding: 12 18; -fx-cursor: hand; -fx-background-radius: 30;");

        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: rgba(0,255,195,0.25); -fx-text-fill: #00ffc3; -fx-font-size: 15; -fx-font-weight: bold; -fx-padding: 12 18; -fx-cursor: hand; -fx-background-radius: 30;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #E0E0E0; -fx-font-size: 15; -fx-font-weight: bold; -fx-padding: 12 18; -fx-cursor: hand; -fx-background-radius: 30;"));

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
        card.setStyle("-fx-background-color: rgba(0,47,47,0.9); -fx-background-radius: 20; -fx-padding: 30; -fx-effect: dropshadow(gaussian, rgba(0,255,200,0.25), 15, 0, 0, 0);");
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
        chartBox.setStyle("-fx-background-color: rgba(0,47,47,0.9); -fx-background-radius: 15; -fx-padding: 20;");

        Label title = new Label("Budget Progress");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.setStyle("-fx-text-fill: white;");

        BarChart<String, Number> barChart = createBudgetBarChart();
        barChart.setPrefSize(350, 300);

        Platform.runLater(() -> {
            // chart background area
            barChart.lookupAll(".chart-plot-background").forEach(n ->
                    n.setStyle("-fx-background-color: white; -fx-border-color: transparent;"));

            // white text for title, labels, tick labels, and legend
            barChart.lookupAll(".chart-title").forEach(n -> n.setStyle("-fx-text-fill: white;"));
            barChart.lookupAll(".axis-label").forEach(n -> n.setStyle("-fx-text-fill: white;"));
            barChart.lookupAll(".axis-tick-label").forEach(n -> n.setStyle("-fx-text-fill: white;"));
            barChart.lookupAll(".chart-legend-item").forEach(n -> n.setStyle("-fx-text-fill: white;"));
        });

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
        tableBox.setStyle("-fx-background-color: rgba(0,47,47,0.9); -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,255,200,0.25), 15, 0, 0, 0);");

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
        amountCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.format("₱%.2f", data.getValue().getAmount())));

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

        Button addTargetBtn = createStyledButton("🎯 Set Target Savings", "#667eea");
        addTargetBtn.setOnAction(e -> showTargetSavingsDialog());

        HBox buttonGroup = new HBox(10);
        buttonGroup.getChildren().addAll(addBudgetBtn, addTargetBtn);

        header.getChildren().addAll(titleLabel, spacer, buttonGroup);

        VBox budgetsList = new VBox(15);
        Map<String, Budget> budgets = budgetManager.getUserBudgets(currentUser);

        for (Budget budget : budgets.values()) {
            budgetsList.getChildren().add(createBudgetCard(budget));
        }

        // Display Target Savings if set
        double targetSavings = budgetManager.getTargetSavings(currentUser);
        if (targetSavings > 0) {
            budgetsList.getChildren().add(0, createTargetSavingsCard(targetSavings));
        }

        ScrollPane scrollPane = new ScrollPane(budgetsList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        budgetsView.getChildren().addAll(header, scrollPane);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(budgetsView);
    }

    private VBox createTargetSavingsCard(double targetSavings) {
        VBox card = new VBox(15);
        // Emerald gradient + soft glow
        card.setStyle(
                "-fx-background-color: linear-gradient(to right, #063c2d, #007a4d, #00c389);"
                        + "-fx-background-radius: 15; -fx-padding: 20;"
                        + "-fx-effect: dropshadow(gaussian, rgba(0,255,150,0.22), 14, 0, 0, 2);"
        );

        double totalIncome = budgetManager.getTotalIncome(currentUser);
        double totalExpenses = budgetManager.getTotalExpenses(currentUser);
        double currentSavings = Math.max(0, totalIncome - totalExpenses);
        double progress = (targetSavings <= 0) ? 0 : (currentSavings / targetSavings) * 100;

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("🎯 Target Savings Goal");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setStyle("-fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label amountLabel = new Label(String.format("₱%.2f / ₱%.2f", currentSavings, targetSavings));
        amountLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 16));
        amountLabel.setStyle("-fx-text-fill: white;");

        topRow.getChildren().addAll(titleLabel, spacer, amountLabel);

        ProgressBar progressBar = new ProgressBar(Math.min((targetSavings <= 0 ? 0 : currentSavings / targetSavings), 1.0));
        progressBar.setPrefHeight(14);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: linear-gradient(#00ffd1, #00c389);"); // emerald accent

        Label percentageLabel = new Label(String.format("%.1f%% achieved", Math.min(progress, 100)));
        percentageLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #e6fff8; -fx-font-weight: bold;");

        card.getChildren().addAll(topRow, progressBar, percentageLabel);
        return card;
    }


    private void showTargetSavingsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Set Target Savings");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        Label infoLabel = new Label("Set your monthly or overall savings target:");
        infoLabel.setStyle("-fx-font-size: 14;");

        TextField targetField = new TextField();
        targetField.setPromptText("Enter target amount");

        double currentTarget = budgetManager.getTargetSavings(currentUser);
        if (currentTarget > 0) {
            targetField.setText(String.valueOf(currentTarget));
        }

        grid.add(infoLabel, 0, 0, 2, 1);
        grid.add(new Label("Target Amount:"), 0, 1);
        grid.add(targetField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    double target = Double.parseDouble(targetField.getText().trim());
                    if (target > 0) {
                        budgetManager.setTargetSavings(currentUser, target);
                        updateUserAccountData();
                        showAlert(Alert.AlertType.INFORMATION, "Target Set", String.format("Target savings set to ₱%.2f", target));
                        StackPane contentArea = (StackPane) ((BorderPane) primaryStage.getScene().getRoot()).getCenter();
                        showBudgets(contentArea);
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Invalid Amount", "Target must be greater than 0.");
                    }
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Please enter a valid amount.");
                }
            }
        });
    }

    // Lightweight mapping from category -> gradient color string
    private String getGradientForCategory(String category) {
        if (category == null) return "linear-gradient(to right, #0f5132, #20c997)"; // default

        switch (category.toLowerCase()) {
            case "food":
                return "linear-gradient(to right, #ff7a7a, #ffb199)"; // coral
            case "transportation":
                return "linear-gradient(to right, #4a90e2, #00b4ff)"; // blue
            case "entertainment":
                return "linear-gradient(to right, #d16ba5, #f7797d)"; // pink/purple
            case "shopping":
                return "linear-gradient(to right, #667eea, #764ba2)"; // purple
            case "bills":
                return "linear-gradient(to right, #ffb86b, #ff6b6b)"; // orange/red
            case "healthcare":
                return "linear-gradient(to right, #06b6a4, #00c389)"; // emerald
            case "education":
                return "linear-gradient(to right, #8ad1ff, #5fb1ff)"; // light blue
            default:
                return "linear-gradient(to right, #0f5132, #20c997)"; // emerald default
        }
    }


    private VBox createBudgetCard(Budget budget) {
        VBox card = new VBox(20);
        String gradient = getGradientForCategory(budget.getCategory());
        card.setStyle(
                "-fx-background-color: " + gradient + ";"
                        + "-fx-background-radius: 15; -fx-padding: 25;"
                        + "-fx-effect: dropshadow(gaussian, rgba(0,255,150,0.35), 18, 0, 0, 2);"
        );


        HBox topRow = new HBox(20);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label categoryLabel = new Label(budget.getCategory());
        categoryLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        categoryLabel.setStyle("-fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label amountLabel = new Label(String.format("₱%.2f / ₱%.2f", budget.getSpent(), budget.getLimit()));
        amountLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 18));
        amountLabel.setStyle("-fx-text-fill: white;");

        topRow.getChildren().addAll(categoryLabel, spacer, amountLabel);

        double percentage = (budget.getSpent() / budget.getLimit()) * 100;
        double progressValue = Math.min(budget.getSpent() / budget.getLimit(), 1.0);

        //Progress Bar
        ProgressBar progressBar = new ProgressBar(progressValue);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(15);
        progressBar.setStyle("-fx-accent: #00ff9d;");   // Neon Emerald

        // Percentage Label (with warnings)
        Label percentageLabel = new Label();
        percentageLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        if (percentage >= 100) {
            percentageLabel.setText(String.format("⚠ %.1f%% - BUDGET EXCEEDED!", percentage));
            percentageLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #ff6b6b; -fx-font-weight: bold;");
        } else if (percentage >= 90) {
            percentageLabel.setText(String.format("⚠ %.1f%% - Near limit!", percentage));
            percentageLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #ff9500; -fx-font-weight: bold;");
        } else {
            percentageLabel.setText(String.format("%.1f%% used", percentage));
            percentageLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #eafff5; -fx-font-weight: bold;");
        }

        card.getChildren().addAll(topRow, progressBar, percentageLabel);
        return card;
    }

    private String getCategoryColor(String category) {
        switch (category.toLowerCase()) {
            case "food":
                return "-fx-background-color: linear-gradient(to right, #ff6b6b, #ff8e8e);"; // Red-ish
            case "transportation":
                return "-fx-background-color: linear-gradient(to right, #4dabf7, #1c7ed6);"; // Blue
            case "entertainment":
                return "-fx-background-color: linear-gradient(to right, #f783ac, #da4a91);"; // Pink/Purple
            case "shopping":
                return "-fx-background-color: linear-gradient(to right, #ffa94d, #ff922b);"; // Orange
            case "bills":
                return "-fx-background-color: linear-gradient(to right, #845ef7, #7048e8);"; // Violet
            case "healthcare":
                return "-fx-background-color: linear-gradient(to right, #20c997, #12b886);"; // Emerald Green
            case "education":
                return "-fx-background-color: linear-gradient(to right, #74c0fc, #4dabf7);"; // Light blue
            case "other":
            default:
                return "-fx-background-color: linear-gradient(to right, #0f5132, #198754, #20c997);"; // Default emerald
        }
    }


    private void showAddBudgetDialog() {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); -fx-background-radius: 15; -fx-padding: 25; -fx-effect: dropshadow(gaussian, rgba(102,126,234,0.4), 10, 0, 0, 2);");

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Budget");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Food", "Transportation", "Entertainment", "Shopping", "Bills", "Healthcare", "Education", "Other");
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

    private void checkBudgetWarning(String category, double newAmount) {
        Map<String, Budget> budgets = budgetManager.getUserBudgets(currentUser);
        Budget budget = budgets.get(category);

        if (budget != null) {
            double newTotal = budget.getSpent() + newAmount;
            double percentage = (newTotal / budget.getLimit()) * 100;

            if (newTotal > budget.getLimit()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Budget Limit Exceeded");
                alert.setHeaderText("⚠ Warning: Budget Exceeded!");
                alert.setContentText(String.format("This expense will exceed your %s budget!\n\nBudget: ₱%.2f\nCurrent: ₱%.2f\nNew Total: ₱%.2f (%.1f%%)\n\nDo you want to continue?",
                        category, budget.getLimit(), budget.getSpent(), newTotal, percentage));

                ButtonType continueBtn = new ButtonType("Continue Anyway");
                ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(continueBtn, cancelBtn);

                alert.showAndWait().ifPresent(response -> {
                    if (response == cancelBtn) {
                        throw new RuntimeException("Transaction cancelled");
                    }
                });
            } else if (percentage >= 90) {
                showAlert(Alert.AlertType.WARNING, "Approaching Budget Limit",
                        String.format("⚠ You're at %.1f%% of your %s budget (₱%.2f / ₱%.2f)",
                                percentage, category, newTotal, budget.getLimit()));
            }
        }
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

        // Buttons
        Button scanReceiptBtn = createStyledButton("📄 Scan Receipt", "#667eea");
        scanReceiptBtn.setOnAction(e -> showReceiptScanDialog());

        Button addExpenseBtn = createStyledButton("+ Add Expense", "#ff6b6b");
        addExpenseBtn.setOnAction(e -> showAddTransactionDialog("Expense"));

        // Grouped Buttons
        HBox buttonGroup = new HBox(10, scanReceiptBtn, addExpenseBtn);
        buttonGroup.setAlignment(Pos.CENTER_RIGHT);

        // Add title, spacer, then buttonGroup to header (each node added once)
        header.getChildren().addAll(titleLabel, spacer, buttonGroup);

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
        amountCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.format("₱%.2f", data.getValue().getAmount())));

        TableColumn<Transaction, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("🗑️");

            {
                deleteBtn.setOnAction(e -> {
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        Transaction transaction = getTableView().getItems().get(getIndex());
                        budgetManager.deleteTransaction(currentUser, transaction.getId());
                        getTableView().getItems().remove(transaction);
                        updateUserAccountData();
                        // BUG FIX: DO NOT check rewards on deletion
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

        if (type.equals("Income")) {
            categoryCombo.getItems().addAll("Salary", "Business", "Investment", "Gift", "Other");
        } else {
            categoryCombo.getItems().addAll("Food", "Transportation", "Entertainment", "Shopping", "Bills", "Healthcare", "Education", "Other");
        }

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
                        try {
                            checkBudgetWarning(category, amount);
                            budgetManager.addExpense(currentUser, date, category, description, amount);
                            checkRewardEligibility();
                            updateUserAccountData(); //Updates CSV on User
                        } catch (RuntimeException ex) {
                            return; // User cancelled
                        }
                    } else {
                        budgetManager.addIncome(currentUser, date, description, amount);
                        updateUserAccountData();
                    }

                    StackPane contentArea = (StackPane) ((BorderPane) primaryStage.getScene().getRoot()).getCenter();
                    if (type.equals("Expense")) {
                        showExpenses(contentArea);
                    } else {
                        showIncome(contentArea);
                    }
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Invalid amount entered.");
                }
            }
        });
    }
    private void checkRewardEligibility() {
        Map<String, Budget> budgets = budgetManager.getUserBudgets(currentUser);

        // If no budgets exist, can't earn rewards yet
        if (budgets.isEmpty()) {
            return;
        }

        int budgetsUnderLimit = 0;
        int totalBudgets = budgets.size();

        StringBuilder budgetStatus = new StringBuilder("Budget Status:\n");
        for (Budget budget : budgets.values()) {
            double percentage = (budget.getSpent() / budget.getLimit()) * 100;
            boolean underLimit = budget.getSpent() <= budget.getLimit();

            budgetStatus.append(String.format("• %s: ₱%.2f / ₱%.2f (%.1f%%) %s\n",
                    budget.getCategory(),
                    budget.getSpent(),
                    budget.getLimit(),
                    percentage,
                    underLimit ? "✓" : "✗"));

            if (underLimit) {
                budgetsUnderLimit++;
            }
        }

        // Debug output
        System.out.println("\n=== REWARD CHECK ===");
        System.out.println("Total Budgets: " + totalBudgets);
        System.out.println("Budgets Under Limit: " + budgetsUnderLimit);
        System.out.println(budgetStatus.toString());

        if (budgetsUnderLimit == totalBudgets) {
            // Get current points before adding
            int pointsBefore = userManager.getRewardPoints(currentUser);

            // Add points to UserManager (persists to file)
            userManager.addRewardPoints(currentUser, 10);

            // Sync local map with UserManager
            int pointsAfter = userManager.getRewardPoints(currentUser);
            userRewardPoints.put(currentUser, pointsAfter);

            System.out.println("Points Before: " + pointsBefore);
            System.out.println("Points After: " + pointsAfter);
            System.out.println("===================\n");

            // Update UI
            updateTopBarRewardPoints();

            // Show detailed reward notification
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("🏆 Reward Earned!");
            alert.setHeaderText("Congratulations! You stayed within all your budgets!");
            alert.setContentText(String.format(
                    "%s\n+10 Reward Points Earned!\n\nPrevious Points: %d\nNew Total: %d points",
                    budgetStatus.toString(),
                    pointsBefore,
                    pointsAfter
            ));
            alert.showAndWait();
        } else {
            System.out.println("❌ Not all budgets under limit - no reward earned");
            System.out.println("===================\n");
        }
    }

    private void updateTopBarRewardPoints() {
        // FIX: Always sync from UserManager (source of truth)
        int currentPoints = userManager.getRewardPoints(currentUser);
        userRewardPoints.put(currentUser, currentPoints);

        BorderPane dashboard = (BorderPane) primaryStage.getScene().getRoot();
        HBox topBar = (HBox) dashboard.getTop();

        for (javafx.scene.Node node : topBar.getChildren()) {
            if (node instanceof HBox) {
                HBox userInfo = (HBox) node;
                for (javafx.scene.Node child : userInfo.getChildren()) {
                    if (child instanceof Label) {
                        Label label = (Label) child;
                        if (label.getText().startsWith("🏆")) {
                            label.setText("🏆 " + currentPoints + " pts");
                        }
                    }
                }
            }
        }
    }

    private void showRewards(StackPane contentArea) {
        VBox rewardsView = new VBox(25);
        rewardsView.setPadding(new Insets(20));

        Label titleLabel = new Label("🏆 Rewards System");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleLabel.setStyle("-fx-text-fill: White;");

        //  FIX: Always get fresh points from UserManager
        int currentPoints = userManager.getRewardPoints(currentUser);
        userRewardPoints.put(currentUser, currentPoints);

        // Points Display Card
        VBox pointsCard = new VBox(15);
        pointsCard.setAlignment(Pos.CENTER);
        pointsCard.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FFA500); -fx-background-radius: 20; -fx-padding: 30; -fx-effect: dropshadow(gaussian, rgba(255,215,0,0.4), 15, 0, 0, 2);");

        Label pointsTitle = new Label("Your Reward Points");
        pointsTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        pointsTitle.setStyle("-fx-text-fill: white;");

        //  FIX: Use fresh points value
        Label pointsAmount = new Label(String.valueOf(currentPoints));
        pointsAmount.setFont(Font.font("System", FontWeight.BOLD, 48));
        pointsAmount.setStyle("-fx-text-fill: white;");

        Label pointsSubtitle = new Label("Keep staying within budget to earn more!");
        pointsSubtitle.setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-size: 13;");

        pointsCard.getChildren().addAll(pointsTitle, pointsAmount, pointsSubtitle);

        // How to Earn Section
        VBox howToEarn = new VBox(15);
        howToEarn.setStyle("-fx-background-color: rgba(0,47,47,0.9); -fx-background-radius: 15; -fx-padding: 25;");

        Label howToTitle = new Label("💡 How to Earn Rewards");
        howToTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        howToTitle.setStyle("-fx-text-fill: white;");

        VBox earningMethods = new VBox(10);
        earningMethods.getChildren().addAll(
                createRewardInfoLabel("✓ Stay within ALL budget limits: +10 points per transaction"),
                createRewardInfoLabel("✓ Achieve your savings target: +50 points"),
                createRewardInfoLabel("✓ Maintain budgets for a full week: +25 points"),
                createRewardInfoLabel("✓ Maintain budgets for a full month: +100 points")
        );

        howToEarn.getChildren().addAll(howToTitle, earningMethods);

        // Redeem Section
        VBox redeemSection = new VBox(15);
        redeemSection.setStyle("-fx-background-color: rgba(0,47,47,0.9); -fx-background-radius: 15; -fx-padding: 25;");

        Label redeemTitle = new Label("🎁 Redeem Rewards");
        redeemTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        redeemTitle.setStyle("-fx-text-fill: white;");

        GridPane redeemGrid = new GridPane();
        redeemGrid.setHgap(15);
        redeemGrid.setVgap(15);

        redeemGrid.add(createRewardOption("Budget Buddy Premium", "100 pts", 100), 0, 0);
        redeemGrid.add(createRewardOption("Financial Tips eBook", "50 pts", 50), 1, 0);
        redeemGrid.add(createRewardOption("Custom Budget Template", "75 pts", 75), 0, 1);
        redeemGrid.add(createRewardOption("Priority Support", "150 pts", 150), 1, 1);

        redeemSection.getChildren().addAll(redeemTitle, redeemGrid);

        ScrollPane scrollPane = new ScrollPane(new VBox(20, titleLabel, pointsCard, howToEarn, redeemSection));
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        rewardsView.getChildren().add(scrollPane);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(rewardsView);
    }

    private Label createRewardInfoLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #b0f0e0; -fx-font-size: 14;");
        label.setWrapText(true);
        return label;
    }

    private VBox createRewardOption(String name, String cost, int pointCost) {
        VBox option = new VBox(10);
        option.setAlignment(Pos.CENTER);
        option.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; -fx-cursor: hand;");
        option.setPrefWidth(200);

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        nameLabel.setStyle("-fx-text-fill: #021a1a;");
        nameLabel.setWrapText(true);
        nameLabel.setAlignment(Pos.CENTER);

        Label costLabel = new Label(cost);
        costLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        costLabel.setStyle("-fx-text-fill: #FFD700;");

        Button redeemBtn = new Button("Redeem");
        redeemBtn.setStyle("-fx-background-color: #00d4aa; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        redeemBtn.setOnAction(e -> {
            // FIX: Get fresh points from UserManager
            int currentPoints = userManager.getRewardPoints(currentUser);

            if (currentPoints >= pointCost) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Redeem Reward");
                confirm.setHeaderText("Redeem " + name + "?");
                confirm.setContentText("This will cost " + cost + ". Continue?");

                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        int newPoints = currentPoints - pointCost;

                        // FIX: Save to UserManager (persists to file)
                        userManager.setRewardPoints(currentUser, newPoints);

                        //  FIX: Update local map
                        userRewardPoints.put(currentUser, newPoints);

                        // Update top bar display
                        updateTopBarRewardPoints();

                        showAlert(Alert.AlertType.INFORMATION, "Reward Redeemed!",
                                "You have successfully redeemed: " + name + "\n\nRemaining Points: " + newPoints);

                        // Refresh the rewards view
                        StackPane contentArea = (StackPane) ((BorderPane) primaryStage.getScene().getRoot()).getCenter();
                        showRewards(contentArea);
                    }
                });
            } else {
                showAlert(Alert.AlertType.WARNING, "Insufficient Points",
                        "You need " + pointCost + " points to redeem this reward.\nYou currently have " + currentPoints + " points.");
            }
        });

        option.getChildren().addAll(nameLabel, costLabel, redeemBtn);

        option.setOnMouseEntered(e -> option.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 12; -fx-padding: 20; -fx-cursor: hand;"));
        option.setOnMouseExited(e -> option.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; -fx-cursor: hand;"));

        return option;
    }

    private void showReports(StackPane contentArea) {
        VBox reportsView = new VBox(20);
        reportsView.setPadding(new Insets(20));

        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Reports & Analytics");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleLabel.setStyle("-fx-text-fill: White;");

        // Time period selector
        ComboBox<String> periodSelector = new ComboBox<>();
        periodSelector.getItems().addAll("Daily", "Weekly", "Monthly");
        periodSelector.setValue("Monthly");
        periodSelector.setStyle("-fx-font-size: 14;");

        header.getChildren().addAll(titleLabel, periodSelector);

        LineChart<String, Number> lineChart = createMonthlyTrendChart();

        periodSelector.setOnAction(e -> {
            String period = periodSelector.getValue();
            LineChart<String, Number> newChart;

            switch(period) {
                case "Daily":
                    newChart = createDailyTrendChart();
                    break;
                case "Weekly":
                    newChart = createWeeklyTrendChart();
                    break;
                default:
                    newChart = createMonthlyTrendChart();
            }

            reportsView.getChildren().set(1, newChart);
        });

        reportsView.getChildren().addAll(header, lineChart);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(reportsView);
    }

    private LineChart<String, Number> createDailyTrendChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Amount (₱)");
        yAxis.setStyle("-fx-tick-label-fill: white; -fx-label-fill: white;");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Income vs. Expenses (Daily - Last 7 Days)");
        lineChart.setPrefHeight(500);
        lineChart.setStyle("-fx-text-fill: white;");

        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName("Income");

        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName("Expenses");

        // Get transactions from last 7 days
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String displayDate = date.format(DateTimeFormatter.ofPattern("MM/dd"));

            double dailyIncome = budgetManager.getIncome(currentUser).stream()
                    .filter(t -> t.getDate().equals(dateStr))
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            double dailyExpense = budgetManager.getExpenses(currentUser).stream()
                    .filter(t -> t.getDate().equals(dateStr))
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            incomeSeries.getData().add(new XYChart.Data<>(displayDate, dailyIncome));
            expenseSeries.getData().add(new XYChart.Data<>(displayDate, dailyExpense));
        }

        lineChart.getData().addAll(incomeSeries, expenseSeries);
        return lineChart;
    }

    private LineChart<String, Number> createWeeklyTrendChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Amount (₱)");
        yAxis.setStyle("-fx-tick-label-fill: white; -fx-label-fill: white;");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Income vs. Expenses (Weekly - Last 4 Weeks)");
        lineChart.setPrefHeight(500);
        lineChart.setStyle("-fx-text-fill: white;");

        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName("Income");

        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName("Expenses");

        LocalDate today = LocalDate.now();
        for (int i = 3; i >= 0; i--) {
            LocalDate weekStart = today.minusWeeks(i);
            LocalDate weekEnd = weekStart.plusDays(6);

            String weekLabel = "Week " + (4 - i);

            double weeklyIncome = budgetManager.getIncome(currentUser).stream()
                    .filter(t -> {
                        LocalDate tDate = LocalDate.parse(t.getDate());
                        return !tDate.isBefore(weekStart) && !tDate.isAfter(weekEnd);
                    })
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            double weeklyExpense = budgetManager.getExpenses(currentUser).stream()
                    .filter(t -> {
                        LocalDate tDate = LocalDate.parse(t.getDate());
                        return !tDate.isBefore(weekStart) && !tDate.isAfter(weekEnd);
                    })
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            incomeSeries.getData().add(new XYChart.Data<>(weekLabel, weeklyIncome));
            expenseSeries.getData().add(new XYChart.Data<>(weekLabel, weeklyExpense));
        }

        lineChart.getData().addAll(incomeSeries, expenseSeries);
        return lineChart;
    }

    private LineChart<String, Number> createMonthlyTrendChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Amount (₱)");
        yAxis.setStyle("-fx-tick-label-fill: white; -fx-label-fill: white;");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Income vs. Expenses (Monthly)");
        lineChart.setPrefHeight(500);
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

        VBox profileSection = new VBox(10);
        Label profileTitle = new Label("Profile Picture");
        profileTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        profileTitle.setStyle("-fx-text-fill: White;");

        HBox profileBox = new HBox(15);
        profileBox.setAlignment(Pos.CENTER_LEFT);

        Circle settingsProfileCircle = createProfilePictureCircle(currentUser);

        Button changeProfileBtn = createStyledButton("Change Profile Picture", "#667eea");

// FIX: Properly call showProfilePictureDialog when button is clicked
        changeProfileBtn.setOnAction(e -> {
            showProfilePictureDialog(currentUser);

            // FIX: After dialog closes, update the circle in settings view
            Platform.runLater(() -> {
                String profilePicture = userManager.getProfilePicture(currentUser);
                if (profilePicture != null && !profilePicture.isEmpty() && !profilePicture.startsWith("#")) {
                    try {
                        File file = new File(profilePicture);
                        if (file.exists()) {
                            Image img = new Image(file.toURI().toString());
                            settingsProfileCircle.setFill(new ImagePattern(img));
                        } else {
                            settingsProfileCircle.setFill(Color.web(getDefaultProfileColor(currentUser)));
                        }
                    } catch (Exception ex) {
                        settingsProfileCircle.setFill(Color.web(getDefaultProfileColor(currentUser)));
                    }
                } else if (profilePicture != null && profilePicture.startsWith("#")) {
                    settingsProfileCircle.setFill(Color.web(profilePicture));
                } else {
                    settingsProfileCircle.setFill(Color.web(getDefaultProfileColor(currentUser)));
                }
            });
        });

        profileBox.getChildren().addAll(settingsProfileCircle, changeProfileBtn);
        profileSection.getChildren().addAll(profileTitle, profileBox);

        VBox pinChangeSection = new VBox(10);
        Label pinTitle = new Label("Change PIN");
        pinTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        pinTitle.setStyle("-fx-text-fill: White;");

        PasswordField newPinField = new PasswordField();
        newPinField.setPromptText("Enter new 4-digit PIN");
        newPinField.setMaxWidth(300);

        newPinField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d{0,4}")) newPinField.setText(oldVal);
        });

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

        settingsView.getChildren().addAll(titleLabel, profileSection, pinChangeSection, exportSection);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(settingsView);
    }

    private void updateUserAccountData() {
        if (currentUser != null) {
            double totalIncome = budgetManager.getTotalIncome(currentUser);
            double totalExpenses = budgetManager.getTotalExpenses(currentUser);
            double balance = totalIncome - totalExpenses;
            double targetSavings = budgetManager.getTargetSavings(currentUser);

            userManager.updateUserAccount(currentUser, balance, totalIncome, totalExpenses, targetSavings);

        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
