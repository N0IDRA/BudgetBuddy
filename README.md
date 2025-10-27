# BudgetBuddy: Smart Finance Management App

BudgetBuddy is a comprehensive desktop application built with **JavaFX** designed to help users efficiently track income, log expenses, manage budgets, and visualize financial trends through interactive charts and reports. It features both PIN and QR code authentication for secure, quick logins.

## Key Features

* **Secure Authentication:** Log in using a 4-digit PIN or a quick QR code scan via webcam (using the ZXing and Webcam Capture libraries).
* **Transaction Tracking:** Easily add and view all Income and Expense transactions.
* **Budget Management:** Set spending limits for various categories (e.g., Food, Transportation) and track progress against those limits.
* **Financial Dashboard:** Get a quick overview of total income, total expenses, and current balance.
* **Data Visualization:** Interactive Pie Charts for expense breakdown and Line Charts for monthly financial trends.
* **Data Export:** Export all transaction data to a CSV file for backup or external analysis.
* **Settings:** Change your secure PIN and manage data export options.

## Technologies Used

* **Primary Language:** Java 17+
* **GUI Framework:** JavaFX
* **Authentication/Utility:**
    * `com.google.zxing` (ZXing Core) for QR code decoding.
    * `com.github.sarxos.webcam-capture` for camera access.
    * `com.google.zxing:javase` (for `MatrixToImageWriter`)

## Getting Started

### Prerequisites

* Java Development Kit (JDK) 17 or higher.
* Maven or Gradle (used for dependency management in most IDEs).

### Setup in an IDE (IntelliJ IDEA Recommended)

1.  **Clone the Repository:**
    ```bash
    git clone [Your-GitHub-Repo-URL]
    cd budgetbuddy-app
    ```
2.  **Open Project:** Open the project directory in your IDE as a Maven/Gradle project.
3.  **Add JavaFX and External Dependencies:** Ensure your build file (`pom.xml` for Maven or `build.gradle` for Gradle) includes the necessary JavaFX modules and the external libraries used for QR scanning/generation.

    *Example Maven Dependencies (check version compatibility):*
    ```xml
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>17.0.2</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-fxml</artifactId>
        <version>17.0.2</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-swing</artifactId>
        <version>17.0.2</version>
    </dependency>
    
    <dependency>
        <groupId>com.google.zxing</groupId>
        <artifactId>core</artifactId>
        <version>3.5.3</version>
    </dependency>
    <dependency>
        <groupId>com.google.zxing</groupId>
        <artifactId>javase</artifactId>
        <version>3.5.3</version>
    </dependency>
    
    <dependency>
        <groupId>com.github.sarxos</groupId>
        <artifactId>webcam-capture</artifactId>
        <version>0.3.12</version>
    </dependency>
    ```

4.  **Run the Application:**
    * Run the `Launcher.java` file, which is configured to start the `BudgetBuddyApp`.

### Project Structure

The core logic is divided into the following files within the `com.example.budgetbuddy` package:

| File | Description |
| :--- | :--- |
| `BudgetBuddyApp.java` | Main JavaFX application class, handles all GUI components and navigation. |
| `UserManager.java` | Manages user data, authentication (PIN & QR), and PIN changes. |
| `BudgetManager.java` | Manages all financial data (transactions, budgets) and reporting/calculations. |
| `Transaction.java` | Data model for a single income or expense entry. |
| `Budget.java` | Data model for tracking spending limits per category. |
| `QRCodeGenerator.java` | Utility for generating QR code images. |
| `Launcher.java` | Simple main class required for launching JavaFX applications packaged with an IDE. |

##  Usage

1.  **Register:** Click the "Register here" link on the login screen to create a new user. The system will display a unique QR code for QR login.
2.  **Login:** Use your new username and PIN, or switch to the QR Code tab and scan the code you saved.
3.  **Navigate:** Use the left sidebar to access the **Dashboard**, **Budgets**, **Expenses**, **Income**, **Reports**, and **Settings**.
4.  **Add Data:** Use the **+ Add Expense** or **+ Add Income** buttons to log new transactions.
