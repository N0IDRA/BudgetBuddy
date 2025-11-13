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
            String email = emailField.getText().trim();
            String password = passwordField.getText().trim();
            String confirmPassword = confirmPasswordField.getText().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
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
