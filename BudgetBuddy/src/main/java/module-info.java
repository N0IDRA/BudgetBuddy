module com.example.budgetbuddy {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;

    opens com.example.budgetbuddy to javafx.fxml;
    exports com.example.budgetbuddy;
}