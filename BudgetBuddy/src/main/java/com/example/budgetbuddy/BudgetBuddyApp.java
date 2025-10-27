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
import javafx.scene.shape.Circle;

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
        stage.setWidth(1200);
        stage.setHeight(800);
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

        Label titleLabel = new Label("💰 BudgetBuddy");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 36));
        titleLabel.setStyle("-fx-text-fill: #667eea;");

        Label subtitleLabel = new Label("Secure Login");
        subtitleLabel.setFont(Font.font("System", 16));
        subtitleLabel.setStyle("-fx-text-fill: #636e72;");

        VBox header = new VBox(5, titleLabel, subtitleLabel);
        header.setAlignment(Pos.CENTER);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

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

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.setStyle("-fx-pref-height: 45;");

        PasswordField pinField = new PasswordField();
        pinField.setPromptText("Enter your 4-digit PIN");
        pinField.setStyle("-fx-pref-height: 45;");

        Button loginBtn = createStyledButton("Login", "#667eea");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        loginBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String pin = pinField.getText().trim();

            if (username.isEmpty() || pin.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Missing Information", "Please enter both username and PIN.");
                return;
            }

            if (userManager.authenticate(username, pin)) {
                currentUser = username;
                showDashboard(username);
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or PIN.");
            }
        });

        Hyperlink registerLink = new Hyperlink("Don't have an account? Register here");
        registerLink.setOnAction(e -> showRegisterDialog());

        pane.getChildren().addAll(usernameField, pinField, loginBtn, registerLink);
        return pane;
    }

    private VBox createQRLoginPane() {
        VBox pane = new VBox(20);
        pane.setPadding(new Insets(30, 20, 30, 20));
        pane.setAlignment(Pos.CENTER);

        ImageView webcamView = new ImageView();
        webcamView.setFitWidth(320);
        webcamView.setFitHeight(240);

        Button scanBtn = createStyledButton("📷 Scan with Camera", "#764ba2");
        scanBtn.setOnAction(e -> {
            if (!scanning) {
                startScanning(webcamView, scanBtn);
            } else {
                stopScanning();
            }
        });

        pane.getChildren().addAll(webcamView, scanBtn);
        return pane;
    }

    private void startScanning(ImageView webcamView, Button scanBtn) {
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
                                        currentUser = username;
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

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField usernameField = new TextField();
        PasswordField pinField = new PasswordField();
        PasswordField confirmPinField = new PasswordField();
        TextField emailField = new TextField();

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

                if (!pin.equals(confirmPin)) {
                    showAlert(Alert.AlertType.WARNING, "PIN Mismatch", "PINs do not match.");
                    return;
                }
                if (userManager.registerUser(username, pin, emailField.getText())) {
                    showQRCodeDialog(username);
                }
            }
        });
    }

    private void showQRCodeDialog(String username) {
        String qrCode = userManager.getQRCode(username);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Registration Successful!");

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30));

        try {
            BufferedImage qrImage = QRCodeGenerator.generateQRCodeForDisplay(qrCode);
            if (qrImage != null) {
                ImageView qrImageView = new ImageView(SwingFXUtils.toFXImage(qrImage, null));
                qrImageView.setFitWidth(280);
                qrImageView.setFitHeight(280);
                content.getChildren().add(qrImageView);
            }
        } catch (Exception e) {
            content.getChildren().add(new Label("QR: " + qrCode));
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
    }

    private void showDashboard(String username) {
        stopScanning();

        BorderPane dashboard = new BorderPane();
        dashboard.setStyle("-fx-background-color: #f5f7fa;");

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
        topBar.setStyle("-fx-background-color: white; -fx-padding: 15 30;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("💰 BudgetBuddy");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        titleLabel.setStyle("-fx-text-fill: #667eea;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userLabel = new Label("👤 " + username);
        userLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));

        Button logoutBtn = createStyledButton("Logout", "#ff6b6b");
        logoutBtn.setOnAction(e -> showLoginScreen());

        topBar.getChildren().addAll(titleLabel, spacer, userLabel, logoutBtn);
        return topBar;
    }

    private VBox createSideNavigation() {
        VBox sideNav = new VBox(10);
        sideNav.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-min-width: 200;");

        Button overviewBtn = createNavButton("📊 Overview");
        Button budgetsBtn = createNavButton("💼 Budgets");
        Button expensesBtn = createNavButton("💸 Expenses");
        Button incomeBtn = createNavButton("💵 Income");
        Button reportsBtn = createNavButton("📈 Reports");
        Button settingsBtn = createNavButton("⚙️ Settings");

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

        sideNav.getChildren().addAll(overviewBtn, budgetsBtn, expensesBtn, incomeBtn, reportsBtn, settingsBtn);
        return sideNav;
    }

    private Button createNavButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: #2d3436; " +
                        "-fx-font-size: 14; " +
                        "-fx-padding: 12 15; " +
                        "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle() + "-fx-background-color: #f0f0f0;"));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().replace("-fx-background-color: #f0f0f0;", "")));
        return btn;
    }

    private void showOverview(StackPane contentArea, String username) {
        VBox overview = new VBox(20);
        overview.setPadding(new Insets(20));

        Label titleLabel = new Label("Financial Overview");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));

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
                "-fx-background-color: white; " +
                        "-fx-background-radius: 15; " +
                        "-fx-padding: 30; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
        );
        card.setPrefWidth(250);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #636e72;");

        Label amountLabel = new Label(amount);
        amountLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        amountLabel.setStyle("-fx-text-fill: " + color + ";");

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
        chartBox.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20;");

        Label title = new Label("Budget Progress");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));

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
        tableBox.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20;");

        Label title = new Label("Recent Transactions");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));

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

        TableColumn<Transaction, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDescription()));

        TableColumn<Transaction, String> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.format("₱%.2f", data.getValue().getAmount())
        ));
        amountCol.setPrefWidth(100);

        TableColumn<Transaction, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(80);
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("🗑️");
            {
                deleteBtn.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10; -fx-font-size: 12;");
                deleteBtn.setOnAction(e -> {
                    // Check if the cell is not empty
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        Transaction transaction = getTableView().getItems().get(getIndex());
                        budgetManager.deleteTransaction(currentUser, transaction.getId());
                        // Refresh the current view after deletion
                        getTableView().getItems().remove(transaction);
                        // Refresh dashboard overview data as well
                        StackPane contentArea = (StackPane) ((BorderPane) primaryStage.getScene().getRoot()).getCenter();
                        if (contentArea.getChildren().get(0) instanceof VBox) {
                            if (((VBox) contentArea.getChildren().get(0)).getChildren().get(0) instanceof Label) {
                                Label title = (Label) ((VBox) contentArea.getChildren().get(0)).getChildren().get(0);
                                if (title.getText().equals("Expenses")) {
                                    showExpenses(contentArea);
                                } else if (title.getText().equals("Income")) {
                                    showIncome(contentArea);
                                }
                                // A quick refresh for the dashboard to update totals
                                showDashboard(currentUser);
                                // Then switch back to the correct view to show the table
                                if (title.getText().equals("Expenses")) {
                                    showExpenses(contentArea);
                                } else if (title.getText().equals("Income")) {
                                    showIncome(contentArea);
                                }
                            }
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteBtn);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        table.getColumns().addAll(dateCol, categoryCol, descCol, amountCol, actionCol);
        table.setItems(FXCollections.observableArrayList(transactions));

        return table;
    } // Closing brace for createTransactionTable
} // Closing brace for BudgetBuddyApp