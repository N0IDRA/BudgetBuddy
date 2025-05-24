# Budget Buddy - C++ GUI Budgeting System

This is a simple budgeting application developed in C++ using the Win32 API. It features a login system, a main dashboard, and an expense tracking module with data persistence via CSV files.

## Download

      *Open Bash.
      * Input "./BudgetBuddy.exe"

## Features

* **Login System:**
    * User authentication with username and password.
    * Account creation (Sign Up) functionality.
    * Maximum of 3 login attempts; the application terminates on failure.
    * Successful login navigates to the main dashboard.
* **Main Dashboard:**
    * Navigation to different modules (Expense, Income, History, Account, Exit).
* **Expense Module:**
    * Form inputs for Description, Amount, and Date (YYYY-MM-DD).
    * Add new expense records.
    * Navigate through existing records (Next/Previous).
    * Update selected expense entries.
    * Save all current expense data to `expenses.csv`.
    * Search expenses by description or date.
    * Displays a list of all current expense entries.
* **Data Storage:**
    * Login credentials are saved in `accounts.csv`.
    * Expense records are saved in `expenses.csv`.
    * Data is loaded into memory when modules are accessed and saved back to CSV.
* **GUI Design:**
    * Dark theme with bright green text, inspired by the "Budget Buddy Storyboard.pdf".
    * Uses a monospace font for a "modular" look.

## Setup and Usage

### Prerequisites

* A C++ compiler (e.g., MinGW-w64 GCC, Visual Studio).

### Compilation

1.  **Save the Source Code:** Save the provided C++ code as `BudgetBuddy.cpp` in a directory of your choice.
2.  **Create CSV Files:** In the *same directory* as `BudgetBuddy.cpp`, create two empty text files:
    * `accounts.csv`
    * `expenses.csv`
    (The application will create these if they don't exist, but it's good practice to have them ready.)
3.  **Compile:**
    * **Using MinGW (Recommended):**
        Open a command prompt or Git Bash, navigate to your directory, and run:
        ```bash
        g++ BudgetBuddy.cpp -o BudgetBuddy.exe -luser32 -lgdi32
        ```
    * **Using Visual Studio:**
        1.  Open Visual Studio.
        2.  Go to `File > New > Project`.
        3.  Select `Empty Project` (C++).
        4.  Give your project a name (e.g., `BudgetBuddy`).
        5.  Right-click on `Source Files` in the Solution Explorer, go to `Add > Existing Item...`, and select `BudgetBuddy.cpp`.
        6.  Go to `Build > Build Solution` (or press `Ctrl+Shift+B`).

### Running the Application

After successful compilation, an executable file named `BudgetBuddy.exe` will be created in your project directory (e.g., `your_project_name/Debug/BudgetBuddy.exe` for Visual Studio, or the same directory as `BudgetBuddy.cpp` for MinGW).

Double-click `BudgetBuddy.exe` to run the application.

### Usage Instructions

1.  **Login Screen:**
    * **Sign Up:** If you're a new user, enter a desired Username and Password, then click "SIGN UP". A message will confirm success or indicate if the username already exists.
    * **Log In:** Enter your registered Username and Password, then click "LOG IN".
    * You have 3 attempts. After 3 failed attempts, the application will close.
2.  **Main Dashboard:**
    * Click "EXPENSE" to enter the Expense Tracking module.
    * Other buttons are placeholders for future modules.
    * Click "EXIT" to close the application.
3.  **Expense Module:**
    * **Add Expense:** Enter a Description, Amount (numeric), and Date (YYYY-MM-DD). Click "Add".
    * **Navigate:** Use "Previous" and "Next" to cycle through your added expenses.
    * **Update Expense:** Modify the fields for the currently displayed expense, then click "Update".
    * **Save All:** Click "Save All" to write all expenses currently in memory to `expenses.csv`. **Note: This overwrites the existing `expenses.csv` with the current in-memory data.**
    * **Search:** Enter a keyword in the search box and click "Search" to filter the list by description or date.
    * **List Box:** Click on an item in the list box to load its details into the input fields.
    * **Back to Main:** Click "Back to Main" to return to the Dashboard.

## Notes

* **Security:** The login system stores passwords in plain text in `accounts.csv`. **This is highly insecure and should NOT be used for any real-world application.** For production, proper password hashing and secure database solutions are mandatory.
* **Font:** The application attempts to use a monospace font for a "modular" look. If you have the "HK Modular" font installed on your system, you can uncomment and try to use `CreateFont(... "HK Modular")` in `WinMain` for a more exact match.
* **Error Handling:** Basic error handling is in place for file operations and input validation. More robust error handling could be implemented for a production application.
* **Extensibility:** The structure allows for easy addition of more modules (Income, History, etc.) by creating new `Create...Controls` and `Destroy...Controls` functions and adding them to the `ShowScreen` logic.
