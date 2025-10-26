package org.example.budgetbuddies;

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
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BudgetBuddyApp extends Application {

    private UserManager userManager;
    private Stage primaryStage;
    private Webcam webcam;
    private ExecutorService executor;
    private volatile boolean scanning = false;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.userManager = new UserManager();

        showLoginScreen();

        stage.setTitle("BudgetBuddy - Smart Finance Management");
        stage.setWidth(900);
        stage.setHeight(700);
        stage.setOnCloseRequest(e -> cleanup());
        stage.show();
    }

    private void showLoginScreen() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #667eea 0%, #764ba2 100%);");

        VBox centerCard = createLoginCard();
        root.setCenter(centerCard);
        BorderPane.setMargin(centerCard, new Insets(50));

        Scene scene = new Scene(root);

        try {
            String css = getClass().getResource("/org/example/budgetbuddies/style.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.out.println("External CSS not found, using inline styles");
        }

        primaryStage.setScene(scene);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), centerCard);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private VBox createLoginCard() {
        VBox card = new VBox(25);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(500);
        card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 20; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 30, 0, 0, 10); " +
                        "-fx-padding: 40;"
        );

        Label titleLabel = new Label("BudgetBuddy");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 36));
        titleLabel.setStyle("-fx-text-fill: #667eea;");

        Label subtitleLabel = new Label("Secure Login");
        subtitleLabel.setFont(Font.font("System", 16));
        subtitleLabel.setStyle("-fx-text-fill: #636e72;");

        VBox header = new VBox(5, titleLabel, subtitleLabel);
        header.setAlignment(Pos.CENTER);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: transparent;");

        Tab pinTab = new Tab("PIN Login");
        pinTab.setContent(createPinLoginPane());

        Tab qrTab = new Tab("QR Code");
        qrTab.setContent(createQRLoginPane());

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (oldTab == qrTab && scanning) {
                stopScanning();
            }
        });

        tabPane.getTabs().addAll(pinTab, qrTab);
        card.getChildren().addAll(header, tabPane);

        return card;
    }

    private VBox createPinLoginPane() {
        VBox pane = new VBox(20);
        pane.setPadding(new Insets(30, 20, 30, 20));
        pane.setAlignment(Pos.CENTER);

        Label userLabel = new Label("Username");
        userLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        userLabel.setStyle("-fx-text-fill: #2d3436;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.setStyle("-fx-pref-height: 45; -fx-font-size: 14; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #e9ecef; -fx-border-width: 2; -fx-padding: 12 16;");

        VBox userBox = new VBox(8, userLabel, usernameField);

        Label pinLabel = new Label("PIN");
        pinLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        pinLabel.setStyle("-fx-text-fill: #2d3436;");

        PasswordField pinField = new PasswordField();
        pinField.setPromptText("Enter your 4-digit PIN");
        pinField.setStyle("-fx-pref-height: 45; -fx-font-size: 14; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #e9ecef; -fx-border-width: 2; -fx-padding: 12 16;");

        VBox pinBox = new VBox(8, pinLabel, pinField);

        CheckBox rememberMe = new CheckBox("Remember me");
        rememberMe.setStyle("-fx-font-size: 13; -fx-text-fill: #636e72;");

        Button loginBtn = createStyledButton("Login", "#667eea", "#5568d3");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        loginBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String pin = pinField.getText().trim();

            if (username.isEmpty() || pin.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Missing Information", "Please enter both username and PIN.");
                return;
            }

            if (!pin.matches("\\d{4}")) {
                showAlert(Alert.AlertType.WARNING, "Invalid PIN", "PIN must be exactly 4 digits.");
                return;
            }

            if (userManager.authenticate(username, pin)) {
                showDashboard(username);
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or PIN.");
            }
        });

        Hyperlink registerLink = new Hyperlink("Don't have an account? Register here");
        registerLink.setStyle("-fx-font-size: 13; -fx-text-fill: #667eea; -fx-underline: false; -fx-font-weight: 500;");
        registerLink.setOnAction(e -> showRegisterDialog());

        pane.getChildren().addAll(userBox, pinBox, rememberMe, loginBtn, registerLink);

        return pane;
    }

    private VBox createQRLoginPane() {
        VBox pane = new VBox(20);
        pane.setPadding(new Insets(30, 20, 30, 20));
        pane.setAlignment(Pos.CENTER);

        Label infoLabel = new Label("Scan your QR code to login");
        infoLabel.setFont(Font.font("System", 14));
        infoLabel.setStyle("-fx-text-fill: #636e72;");

        StackPane qrDisplay = new StackPane();
        qrDisplay.setStyle("-fx-background-color: linear-gradient(to bottom right, #1a1a1a, #2d2d2d); -fx-background-radius: 15; -fx-border-color: #667eea; -fx-border-radius: 15; -fx-border-width: 3; -fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(118, 75, 162, 0.3), 15, 0, 0, 5);");
        qrDisplay.setPrefSize(360, 270);

        ImageView webcamView = new ImageView();
        webcamView.setFitWidth(320);
        webcamView.setFitHeight(240);
        webcamView.setPreserveRatio(true);
        webcamView.setVisible(false);

        VBox placeholderBox = new VBox(10);
        placeholderBox.setAlignment(Pos.CENTER);
        Label placeholderIcon = new Label("📷");
        placeholderIcon.setFont(Font.font(64));
        Label placeholderText = new Label("Click 'Scan with Camera' below");
        placeholderText.setFont(Font.font("System", 14));
        placeholderText.setStyle("-fx-text-fill: #999999;");
        placeholderBox.getChildren().addAll(placeholderIcon, placeholderText);

        qrDisplay.getChildren().addAll(placeholderBox, webcamView);

        HBox statusBox = new HBox();
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 20; -fx-padding: 8 20;");

        Label statusLabel = new Label("● Ready to scan");
        statusLabel.setStyle("-fx-text-fill: #00d4aa; -fx-font-weight: bold; -fx-font-size: 13;");
        statusBox.getChildren().add(statusLabel);

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button scanBtn = createStyledButton("📷 Scan with Camera", "#764ba2", "#5e3c82");
        scanBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(scanBtn, Priority.ALWAYS);

        Button uploadBtn = createStyledButton("🖼️ Upload QR Image", "#667eea", "#5568d3");
        uploadBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(uploadBtn, Priority.ALWAYS);

        scanBtn.setOnAction(e -> {
            if (!scanning) {
                placeholderBox.setVisible(false);
                webcamView.setVisible(true);
                startScanning(webcamView, statusLabel, statusBox, scanBtn);
                scanBtn.setText("⬛ Stop Camera");
            } else {
                stopScanning();
                webcamView.setVisible(false);
                placeholderBox.setVisible(true);
                statusLabel.setText("● Stopped");
                scanBtn.setText("📷 Scan with Camera");
            }
        });

        uploadBtn.setOnAction(e -> handleQRImageUpload(statusLabel, statusBox));

        buttonBox.getChildren().addAll(scanBtn, uploadBtn);
        pane.getChildren().addAll(infoLabel, qrDisplay, statusBox, buttonBox);

        return pane;
    }

    private void handleQRImageUpload(Label statusLabel, HBox statusBox) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select QR Code Image");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"));

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
                    stopScanning();
                    showDashboard(username);
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

    private Button createStyledButton(String text, String baseColor, String hoverColor) {
        Button btn = new Button(text);
        String baseStyle = String.format("-fx-pref-height: 50; -fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: linear-gradient(135deg, %s 0%%, derive(%s, -10%%) 100%%); -fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand;", baseColor, baseColor);
        btn.setStyle(baseStyle);
        return btn;
    }

    private Button createVideoButtonWithIcon(String text, String baseColor, String hoverColor) {
        Button btn = new Button(text);

        // Try to load custom image icon
        try {
            // Option 1: Load from resources folder
            // Place your image in: src/main/resources/org/example/budgetbuddies/icons/video.png
            String iconPath = "/org/example/budgetbuddies/icons/video.png";
            javafx.scene.image.Image icon = new javafx.scene.image.Image(getClass().getResourceAsStream(iconPath));
            ImageView iconView = new ImageView(icon);
            iconView.setFitWidth(20);
            iconView.setFitHeight(20);
            iconView.setPreserveRatio(true);
            btn.setGraphic(iconView);
            System.out.println("✓ Loaded custom video icon from resources");
        } catch (Exception e) {
            // Option 2: Load from file system (absolute path)
            try {
                // Change this path to your image location
                String filePath = "file:///C:/path/to/your/video-icon.png";
                // Or use relative path: "file:icons/video.png"
                javafx.scene.image.Image icon = new javafx.scene.image.Image(filePath);
                ImageView iconView = new ImageView(icon);
                iconView.setFitWidth(20);
                iconView.setFitHeight(20);
                iconView.setPreserveRatio(true);
                btn.setGraphic(iconView);
                System.out.println("✓ Loaded custom video icon from file system");
            } catch (Exception ex) {
                // Fallback: Use emoji if image not found
                btn.setText("🎬 " + text);
                System.out.println("⚠ Image not found, using emoji. Place image at: src/main/resources/org/example/budgetbuddies/icons/video.png");
            }
        }

        String baseStyle = String.format("-fx-pref-height: 50; -fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: linear-gradient(135deg, %s 0%%, derive(%s, -10%%) 100%%); -fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand; -fx-graphic-text-gap: 8;", baseColor, baseColor);
        btn.setStyle(baseStyle);
        return btn;
    }

    private void startScanning(ImageView webcamView, Label statusLabel, HBox statusBox, Button scanBtn) {
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
                                        showDashboard(username);
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

    private void showRegisterDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Register New Account");
        dialog.setHeaderText("Create your BudgetBuddy account");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField pinField = new PasswordField();
        pinField.setPromptText("4-digit PIN");
        PasswordField confirmPinField = new PasswordField();
        confirmPinField.setPromptText("Confirm PIN");
        TextField emailField = new TextField();
        emailField.setPromptText("Email (optional)");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("PIN:"), 0, 1);
        grid.add(pinField, 1, 1);
        grid.add(new Label("Confirm PIN:"), 0, 2);
        grid.add(confirmPinField, 1, 2);
        grid.add(new Label("Email:"), 0, 3);
        grid.add(emailField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String username = usernameField.getText().trim();
                String pin = pinField.getText().trim();
                String confirmPin = confirmPinField.getText().trim();
                String email = emailField.getText().trim();

                if (username.isEmpty() || pin.isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Missing Information", "Username and PIN required.");
                    return;
                }
                if (!pin.matches("\\d{4}")) {
                    showAlert(Alert.AlertType.WARNING, "Invalid PIN", "PIN must be 4 digits.");
                    return;
                }
                if (!pin.equals(confirmPin)) {
                    showAlert(Alert.AlertType.WARNING, "PIN Mismatch", "PINs do not match.");
                    return;
                }
                if (userManager.registerUser(username, pin, email)) {
                    showQRCodeSuccessDialog(username);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Registration Failed", "Username exists.");
                }
            }
        });
    }

    private void showQRCodeSuccessDialog(String username) {
        String qrCode = userManager.getQRCode(username);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Registration Successful!");
        dialog.setHeaderText("Welcome, " + username + "!");

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30));

        Label successLabel = new Label("✓ Account created!");
        successLabel.setStyle("-fx-font-size: 15; -fx-text-fill: #00d4aa; -fx-font-weight: bold;");

        try {
            BufferedImage qrImage = QRCodeGenerator.generateQRCodeForDisplay(qrCode);
            if (qrImage != null) {
                ImageView qrImageView = new ImageView(SwingFXUtils.toFXImage(qrImage, null));
                qrImageView.setFitWidth(280);
                qrImageView.setFitHeight(280);

                Label qrCodeLabel = new Label(qrCode);
                qrCodeLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 11;");

                Button saveBtn = new Button("💾 Save QR Image");
                saveBtn.setOnAction(e -> {
                    try {
                        String filePath = QRCodeGenerator.generateUserQRCode(username, qrCode, "qr_codes");
                        showAlert(Alert.AlertType.INFORMATION, "Saved", "QR saved to: " + filePath);
                    } catch (Exception ex) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to save: " + ex.getMessage());
                    }
                });

                content.getChildren().addAll(successLabel, qrImageView, qrCodeLabel, saveBtn);
            }
        } catch (Exception e) {
            Label qrCodeLabel = new Label(qrCode);
            content.getChildren().addAll(successLabel, qrCodeLabel);
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
    }

    private void showAccountDialog(String username) {
        String qrCode = userManager.getQRCode(username);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("My Account");
        dialog.setHeaderText("Account: " + username);

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30));

        Label usernameLabel = new Label("👤 Username: " + username);

        try {
            BufferedImage qrImage = QRCodeGenerator.generateQRCodeForDisplay(qrCode);
            if (qrImage != null) {
                ImageView qrImageView = new ImageView(SwingFXUtils.toFXImage(qrImage, null));
                qrImageView.setFitWidth(280);
                qrImageView.setFitHeight(280);

                Button saveBtn = new Button("💾 Save QR");
                saveBtn.setOnAction(e -> {
                    try {
                        String filePath = QRCodeGenerator.generateUserQRCode(username, qrCode, "qr_codes");
                        showAlert(Alert.AlertType.INFORMATION, "Saved", "QR saved to: " + filePath);
                    } catch (Exception ex) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to save");
                    }
                });

                content.getChildren().addAll(usernameLabel, qrImageView, saveBtn);
            }
        } catch (Exception e) {
            content.getChildren().add(usernameLabel);
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showDashboard(String username) {
        stopScanning();

        BorderPane dashboard = new BorderPane();
        dashboard.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa 0%, #e9ecef 100%);");

        HBox topBar = new HBox();
        topBar.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 2 0;");
        topBar.setPadding(new Insets(20, 30, 20, 30));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setSpacing(20);

        Label welcomeLabel = new Label("Welcome, " + username + "! 👋");
        welcomeLabel.setFont(Font.font("System", FontWeight.BOLD, 20));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Video button with custom image icon
        Button videoBtn = createVideoButtonWithIcon("Tutorial", "#e74c3c", "#c0392b");
        videoBtn.setOnAction(e -> playVideo());

        Button accountBtn = createStyledButton("👤 Account", "#667eea", "#5568d3");
        accountBtn.setOnAction(e -> showAccountDialog(username));

        Button logoutBtn = createStyledButton("Logout", "#ff6b6b", "#ee5a6f");
        logoutBtn.setOnAction(e -> showLoginScreen());

        topBar.getChildren().addAll(welcomeLabel, spacer, videoBtn, accountBtn, logoutBtn);

        VBox centerContent = new VBox(30);
        centerContent.setPadding(new Insets(50));
        centerContent.setAlignment(Pos.TOP_CENTER);

        VBox successCard = new VBox(20);
        successCard.setAlignment(Pos.CENTER);
        successCard.setMaxWidth(600);
        successCard.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 50;");

        Label dashLabel = new Label("🎉 Login Successful!");
        dashLabel.setFont(Font.font("System", FontWeight.BOLD, 36));
        dashLabel.setStyle("-fx-text-fill: #667eea;");

        Label infoLabel = new Label("Your dashboard will appear here");
        infoLabel.setFont(Font.font("System", 16));

        successCard.getChildren().addAll(dashLabel, infoLabel);
        centerContent.getChildren().add(successCard);

        dashboard.setTop(topBar);
        dashboard.setCenter(centerContent);

        Scene scene = new Scene(dashboard);
        primaryStage.setScene(scene);

        FadeTransition fade = new FadeTransition(Duration.millis(500), successCard);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void playVideo() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Budget Buddy Tutorial");
        dialog.setHeaderText("📺 How to Use Budget Buddy");

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: white;");

        // Video player setup
        MediaPlayer mediaPlayer = null;
        MediaView mediaView = null;

        try {
            // Load video from resources or file system
            String videoPath = "src/main/resources/org/example/budgetbuddies/videos/videoplayback.mp4";

            // Alternative paths you can try:
            // String videoPath = getClass().getResource("/videos/videoplayback.mp4").toExternalForm();
            // String videoPath = "file:///C:/path/to/videos/videoplayback.mp4";

            Media media = new Media(videoPath);
            mediaPlayer = new MediaPlayer(media);
            mediaView = new MediaView(mediaPlayer);

            mediaView.setFitWidth(640);
            mediaView.setFitHeight(360);
            mediaView.setPreserveRatio(true);

            StackPane videoContainer = new StackPane(mediaView);
            videoContainer.setStyle(
                    "-fx-background-color: black; " +
                            "-fx-background-radius: 15; " +
                            "-fx-border-color: #e74c3c; " +
                            "-fx-border-radius: 15; " +
                            "-fx-border-width: 3;"
            );
            videoContainer.setPrefSize(660, 380);

            Label titleLabel = new Label("Tutorial Video");
            titleLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
            titleLabel.setStyle("-fx-text-fill: #2d3436;");

            // Video controls
            HBox controls = new HBox(15);
            controls.setAlignment(Pos.CENTER);
            controls.setStyle("-fx-padding: 10;");

            MediaPlayer finalMediaPlayer = mediaPlayer;

            Button playBtn = createStyledButton("▶ Play", "#00d4aa", "#00b894");
            Button pauseBtn = createStyledButton("⏸ Pause", "#667eea", "#5568d3");
            Button stopBtn = createStyledButton("⏹ Stop", "#ff6b6b", "#ee5a6f");
            Button replayBtn = createStyledButton("🔄 Replay", "#f39c12", "#e67e22");

            playBtn.setOnAction(e -> finalMediaPlayer.play());
            pauseBtn.setOnAction(e -> finalMediaPlayer.pause());
            stopBtn.setOnAction(e -> {
                finalMediaPlayer.stop();
                finalMediaPlayer.seek(Duration.ZERO);
            });
            replayBtn.setOnAction(e -> {
                finalMediaPlayer.stop();
                finalMediaPlayer.seek(Duration.ZERO);
                finalMediaPlayer.play();
            });

            // Volume control
            HBox volumeBox = new HBox(10);
            volumeBox.setAlignment(Pos.CENTER);

            Label volumeLabel = new Label("🔊 Volume:");
            volumeLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

            Slider volumeSlider = new Slider(0, 1, 0.5);
            volumeSlider.setPrefWidth(150);
            volumeSlider.setShowTickLabels(false);
            volumeSlider.setShowTickMarks(false);
            finalMediaPlayer.setVolume(0.5);
            volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                finalMediaPlayer.setVolume(newVal.doubleValue());
            });

            volumeBox.getChildren().addAll(volumeLabel, volumeSlider);

            controls.getChildren().addAll(playBtn, pauseBtn, stopBtn, replayBtn);

            // Progress bar
            ProgressBar progressBar = new ProgressBar(0);
            progressBar.setPrefWidth(640);
            progressBar.setStyle("-fx-accent: #00d4aa;");

            finalMediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (!finalMediaPlayer.getMedia().getDuration().isUnknown()) {
                    progressBar.setProgress(newTime.toSeconds() /
                            finalMediaPlayer.getMedia().getDuration().toSeconds());
                }
            });

            Label tipLabel = new Label("💡 Video loaded successfully! Use the controls below to play.");
            tipLabel.setWrapText(true);
            tipLabel.setMaxWidth(640);
            tipLabel.setStyle(
                    "-fx-font-size: 12px; " +
                            "-fx-text-fill: #636e72; " +
                            "-fx-background-color: #d4edda; " +
                            "-fx-padding: 10; " +
                            "-fx-background-radius: 8;"
            );

            content.getChildren().addAll(videoContainer, titleLabel, progressBar, controls, volumeBox, tipLabel);

            // Cleanup when dialog closes
            MediaPlayer playerForCleanup = finalMediaPlayer;
            dialog.setOnCloseRequest(e -> {
                if (playerForCleanup != null) {
                    playerForCleanup.stop();
                    playerForCleanup.dispose();
                }
            });

            System.out.println("✓ Video loaded successfully from: " + videoPath);

        } catch (Exception e) {
            // Fallback if video not found
            System.err.println("✗ Could not load video: " + e.getMessage());

            Label errorIcon = new Label("❌");
            errorIcon.setFont(Font.font(72));

            StackPane videoPlaceholder = new StackPane(errorIcon);
            videoPlaceholder.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #1a1a1a, #2d2d2d); " +
                            "-fx-background-radius: 15; " +
                            "-fx-border-color: #e74c3c; " +
                            "-fx-border-radius: 15; " +
                            "-fx-border-width: 3; " +
                            "-fx-padding: 40; " +
                            "-fx-min-width: 500; " +
                            "-fx-min-height: 300;"
            );

            Label titleLabel = new Label("Video Not Found");
            titleLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
            titleLabel.setStyle("-fx-text-fill: #e74c3c;");

            Label errorLabel = new Label(
                    "Could not load video from: videos/videoplayback.mp4\n\n" +
                            "Please check:\n" +
                            "• File exists at correct location\n" +
                            "• File path is correct\n" +
                            "• Video format is supported (MP4, AVI, etc.)\n\n" +
                            "Try these paths:\n" +
                            "• file:videos/videoplayback.mp4\n" +
                            "• file:///C:/full/path/to/videos/videoplayback.mp4\n" +
                            "• Place in resources: src/main/resources/videos/"
            );
            errorLabel.setWrapText(true);
            errorLabel.setMaxWidth(500);
            errorLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #636e72;");

            Button retryBtn = createStyledButton("🔄 Retry", "#667eea", "#5568d3");
            retryBtn.setOnAction(ev -> {
                dialog.close();
                playVideo();
            });

            content.getChildren().addAll(videoPlaceholder, titleLabel, errorLabel, retryBtn);
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 20; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 30, 0, 0, 10);"
        );

        dialog.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}