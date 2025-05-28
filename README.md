# Budget Buddy System

The Budget Buddy system is a simple desktop application developed in C++ using the Windows API (Win32) for managing personal finances. It allows users to track expenses, view balances, set daily spending limits, redeem reward points, and manage their expense records. Additionally, it now includes an administrative system for overviewing all user accounts and expenses, and the ability to restore previously deleted expense records.

## Features

### User Features
* **User Registration & Login:** Create and log in to individual user accounts.
* **Add Expense:** Record daily expenses with details like name, category, and amount.
* **View Balance:** Check current account balance, reward points, and daily expense limit.
* **Manage Records:** View, edit, delete, and **restore** expense records.
* **Set Expense Limit:** Define a daily spending limit to help control expenditures.
* **Redeem Rewards:** Convert accumulated reward points into balance.
* **Receipt Generation:** Get a pop-up receipt after adding an expense.

### Admin Features
* **Admin Login:** A dedicated admin login (`admin`/`admin123`) to access administrative functionalities.
* **View All Accounts:** See a list of all registered user accounts, their balances, reward points, and daily limits.
* **View All Expenses:** Access a consolidated list of all expenses recorded by all users, including deleted ones.
* **Restore Deleted Records:** On the "Manage Records" screen, users can restore expenses previously marked as deleted. This deducts the expense amount from their current balance and re-adds the corresponding reward points.

## How to Compile and Run

### Prerequisites
* A C++ compiler (e.g., MinGW, g++)
* Windows SDK (usually included with Visual Studio, or can be installed separately for MinGW)

### Compilation Steps

1.  **Save the Code:** Save the provided C++ code as `main.cpp`.
2.  **Compile:** Open a terminal or command prompt and navigate to the directory where you saved `main.cpp`. Use your C++ compiler to compile the code.

    **For g++ (MinGW):**
    ```bash
    g++ main.cpp -o BudgetBuddy.exe -luser32 -lgdi32 -lComCtl32 -lComdlg32
    ```

    * `-o BudgetBuddy.exe`: Specifies the output executable name.
    * `-luser32`, `-lgdi32`, `-lComCtl32`, `-lComdlg32`: Link against necessary Windows libraries for UI, common controls, and common dialogs.

    **For Visual Studio (via Developer Command Prompt):**
    ```bash
    cl main.cpp /Fe:BudgetBuddy.exe /link user32.lib gdi32.lib ComCtl32.lib Comdlg32.lib
    ```

    * `/Fe:BudgetBuddy.exe`: Specifies the output executable name.
    * `/link user32.lib gdi32.lib ComCtl32.lib Comdlg32.lib`: Links the required libraries.

### Running the Application
1.  **Execute:** After successful compilation, run the executable:
    ```bash
    ./BudgetBuddy.exe
    ```
    or simply double-click `BudgetBuddy.exe` in your file explorer.

## Usage

### Getting Started
1.  **Register:** On the main menu, click "Register" to create a new user account.
2.  **Login:** Use your registered credentials or the `admin`/`admin123` credentials to log in.

### User Functions
* **Add Expense:** Enter expense details and click "Add Expense".
* **View Balance:** Check your financial overview.
* **Manage Records:** Select an expense from the list to "Edit", "Delete", or "Restore" it.
* **Set Limit:** Enter a positive integer to set your daily spending limit. Enter 0 to remove the limit.
* **Redeem Rewards:** Enter the number of points you wish to redeem.

### Admin Functions
* After logging in as `admin`, select "View All Accounts" to see user account details or "View All Expenses" for a comprehensive list of all recorded expenses across all users.

## Data Storage

The application stores user account information in `accounts.csv` and individual user expenses in separate `.csv` files (e.g., `username_expenses.csv`). These files are created automatically in the same directory as the executable.

---
