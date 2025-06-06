#include <windows.h>
#include <string>
#include <vector>
#include <fstream>
#include <sstream>
#include <iomanip>
#include <ctime>
#include <algorithm>
#include <cstdio>
#include <commctrl.h>
#include <commdlg.h>

// Linker directives for common controls and common dialogs
#pragma comment(lib, "ComCtl32.lib")
#pragma comment(lib, "Comdlg32.lib")
#pragma comment(linker,"/manifestdependency:\"type='win32' name='Microsoft.Windows.Common-Controls' version='6.0.0.0' processorArchitecture='*' publicKeyToken='6595b64144ccf1df' language='*'\"")

// --- Constants ---
const std::string ACCOUNTS_FILE = "accounts.csv";
const std::string ADMIN_USERNAME = "admin";
const std::string ADMIN_PASSWORD = "admin123";
const int MAX_LOGIN_ATTEMPTS = 3;
const double REWARD_RATE = 0.05; // 5% reward

// --- Global UI Element IDs ---
#define IDC_STATIC_TITLE        1001
#define IDC_BTN_REGISTER        1002
#define IDC_BTN_LOGIN           1003
#define IDC_BTN_EXIT            1004

#define IDC_EDIT_USERNAME       1005
#define IDC_EDIT_PASSWORD       1006
#define IDC_BTN_SUBMIT          1007
#define IDC_BTN_CANCEL          1008
#define IDC_STATIC_MESSAGE      1009 // For displaying success/error messages

// User Menu Buttons
#define IDC_BTN_ADD_EXPENSE     1101
#define IDC_BTN_VIEW_BALANCE    1102
#define IDC_BTN_MANAGE_RECORDS  1103
#define IDC_BTN_SET_LIMIT       1104
#define IDC_BTN_REDEEM_REWARDS  1105
#define IDC_BTN_USER_LOGOUT     1106

// Add Expense Screen Controls
#define IDC_STATIC_EXPENSE_NAME     1201
#define IDC_EDIT_EXPENSE_NAME       1202
#define IDC_STATIC_EXPENSE_CATEGORY 1203
#define IDC_COMBO_EXPENSE_CATEGORY  1204
#define IDC_STATIC_EXPENSE_AMOUNT   1205
#define IDC_EDIT_EXPENSE_AMOUNT     1206
#define IDC_BTN_ADD_EXPENSE_SUBMIT  1207
#define IDC_BTN_ADD_EXPENSE_CANCEL  1208

// View Balance Screen Controls
#define IDC_STATIC_BALANCE_INFO     1301
#define IDC_LISTVIEW_EXPENSES       1302
#define IDC_BTN_VIEW_BALANCE_BACK   1303

// Manage Records Screen Controls
#define IDC_LISTVIEW_MANAGE_EXPENSES 1401
#define IDC_BTN_EDIT_EXPENSE         1402
#define IDC_BTN_DELETE_EXPENSE       1403
#define IDC_BTN_MANAGE_RECORDS_BACK  1404
#define IDC_STATIC_MANAGE_MESSAGE    1405 // For messages specific to manage records
#define IDC_BTN_RESTORE_EXPENSE      1406 // New: Restore deleted expense button

// Set Expense Limit Screen Controls
#define IDC_STATIC_CURRENT_LIMIT    1501
#define IDC_EDIT_NEW_LIMIT          1502
#define IDC_BTN_SET_LIMIT_SUBMIT    1503
#define IDC_BTN_SET_LIMIT_CANCEL    1504
#define IDC_STATIC_SET_LIMIT_MESSAGE 1505

// Redeem Rewards Screen Controls
#define IDC_STATIC_REWARD_INFO      1601
#define IDC_EDIT_REDEEM_POINTS      1602
#define IDC_BTN_REDEEM_REWARDS_SUBMIT 1603
#define IDC_BTN_REDEEM_REWARDS_CANCEL 1604
#define IDC_STATIC_REDEEM_MESSAGE   1605

// Admin Menu Buttons
#define IDC_BTN_VIEW_ALL_ACCOUNTS   1701
#define IDC_BTN_VIEW_ALL_EXPENSES   1702

// View All Expenses Screen Controls
#define IDC_STATIC_ALL_EXPENSES_TITLE 1801
#define IDC_EDIT_ALL_EXPENSES_DISPLAY 1802
#define IDC_BTN_ALL_EXPENSES_BACK   1803

// View All Accounts Screen Controls (New)
#define IDC_STATIC_ALL_ACCOUNTS_TITLE 1901
#define IDC_EDIT_ALL_ACCOUNTS_DISPLAY 1902
#define IDC_BTN_ALL_ACCOUNTS_BACK   1903


// --- Global Handles ---
HINSTANCE hInst;
HWND      hWndMain;
HBRUSH    hbrBackground;
COLORREF  crFontColor = RGB(0x00, 0xFF, 0x21); // Green
COLORREF  crBaseColor = RGB(0x00, 0x00, 0x00); // Black

// --- BudgetBuddy Class ---
class BudgetBuddy {
private:
    struct User {
        std::string username;
        std::string password;
        double balance;
        int rewardPoints;
        int dailyExpenseLimit;
        bool isAdmin;
    };

    struct Expense {
        std::string date;
        std::string name;
        std::string category;
        double amount;
        bool isDeleted;
    };

    User currentUser;
    std::vector<Expense> expenses;
    std::string userExpenseFile;

    // Helper functions
    std::string getCurrentDate() {
        time_t now = time(0);
        tm* ltm = localtime(&now);
        char buffer[80];
        strftime(buffer, sizeof(buffer), "%Y-%m-%d", ltm);
        return std::string(buffer);
    }

    bool fileExists(const std::string& filename) {
        std::ifstream file(filename);
        return file.good();
    }

    void createUserFile() {
        std::ofstream file(userExpenseFile, std::ios::app);
        if (!file) {
            MessageBoxA(hWndMain, "Error creating user file!", "File Error", MB_OK | MB_ICONERROR);
        }
        file.close();
    }

    void loadUserData() {
        expenses.clear(); // Clear existing expenses before loading
        std::ifstream file(userExpenseFile);
        if (!file) return;

        std::string line;
        while (getline(file, line)) {
            if (line.empty()) continue;

            Expense exp;
            std::stringstream ss(line);
            std::string field;

            try {
                getline(ss, field, ',');
                exp.date = field;

                getline(ss, field, ',');
                exp.name = field;

                getline(ss, field, ',');
                exp.category = field;

                getline(ss, field, ',');
                exp.amount = std::stod(field);

                getline(ss, field, ',');
                exp.isDeleted = (field == "1");

                expenses.push_back(exp);
            } catch (const std::invalid_argument& e) {
                char errorMsg[512];
                sprintf(errorMsg, "Error loading expense data for user %s: Invalid number format in line '%s'. Error: %s",
                        currentUser.username.c_str(), line.c_str(), e.what());
                MessageBoxA(hWndMain, errorMsg, "Data Load Error", MB_OK | MB_ICONWARNING);
            } catch (const std::out_of_range& e) {
                char errorMsg[512];
                sprintf(errorMsg, "Error loading expense data for user %s: Number out of range in line '%s'. Error: %s",
                        currentUser.username.c_str(), line.c_str(), e.what());
                MessageBoxA(hWndMain, errorMsg, "Data Load Error", MB_OK | MB_ICONWARNING);
            }
        }
        file.close();
    }

    void saveUserData() {
        std::ofstream file(userExpenseFile);
        if (!file) {
            MessageBoxA(hWndMain, "Error saving user data!", "File Error", MB_OK | MB_ICONERROR);
            return;
        }

        for (const auto& exp : expenses) {
            file << exp.date << ","
                 << exp.name << ","
                 << exp.category << ","
                 << exp.amount << ","
                 << (exp.isDeleted ? "1" : "0") << "\n";
        }
        file.close();
    }

    void updateAccountData() {
        std::vector<User> allUsers;
        std::ifstream inFile(ACCOUNTS_FILE);
        if (inFile) {
            std::string line;
            while (getline(inFile, line)) {
                std::stringstream ss(line);
                std::string storedUser, storedPass, field;
                User tempUser;

                try {
                    getline(ss, storedUser, ',');
                    getline(ss, storedPass, ',');
                    tempUser.username = storedUser;
                    tempUser.password = storedPass;

                    getline(ss, field, ','); tempUser.balance = std::stod(field);
                    getline(ss, field, ','); tempUser.rewardPoints = std::stoi(field);
                    getline(ss, field, ','); tempUser.dailyExpenseLimit = std::stoi(field);
                    getline(ss, field, ','); tempUser.isAdmin = (field == "1");

                    if (tempUser.username == currentUser.username) {
                        allUsers.push_back(currentUser); // Replace with updated current user
                    } else {
                        allUsers.push_back(tempUser);
                    }
                } catch (const std::invalid_argument& e) {
                    char errorMsg[512];
                    sprintf(errorMsg, "Error updating account data: Invalid number format in line '%s'. Error: %s",
                            line.c_str(), e.what());
                    MessageBoxA(hWndMain, errorMsg, "Data Update Error", MB_OK | MB_ICONWARNING);
                } catch (const std::out_of_range& e) {
                    char errorMsg[512];
                    sprintf(errorMsg, "Error updating account data: Number out of range in line '%s'. Error: %s",
                            line.c_str(), e.what());
                    MessageBoxA(hWndMain, errorMsg, "Data Update Error", MB_OK | MB_ICONWARNING);
                }
            }
            inFile.close();
        }

        std::ofstream outFile(ACCOUNTS_FILE, std::ios::trunc); // Overwrite file
        if (!outFile) {
            MessageBoxA(hWndMain, "Error updating account data!", "File Error", MB_OK | MB_ICONERROR);
            return;
        }

        for (const auto& user : allUsers) {
            outFile << user.username << ","
                    << user.password << ","
                    << user.balance << ","
                    << user.rewardPoints << ","
                    << user.dailyExpenseLimit << ","
                    << (user.isAdmin ? "1" : "0") << "\n";
        }
        outFile.close();
    }


public:
    User& GetCurrentUser() { return currentUser; }
    void SetCurrentUser(const User& user) { currentUser = user; }
    void ClearCurrentUser() { currentUser = {}; }
    const std::vector<Expense>& GetExpenses() const { return expenses; }

    bool registerUser(const std::string& username, const std::string& password, std::string& message) {
        std::ifstream file(ACCOUNTS_FILE);
        if (file) {
            std::string line;
            while (getline(file, line)) {
                std::stringstream ss(line);
                std::string storedUser;
                getline(ss, storedUser, ',');
                if (storedUser == username) {
                    message = "Username already exists!";
                    file.close();
                    return false;
                }
            }
            file.close();
        }

        User newUser;
        newUser.username = username;
        newUser.password = password;
        newUser.balance = 1000.0;
        newUser.rewardPoints = 0;
        newUser.dailyExpenseLimit = 0;
        newUser.isAdmin = false;

        std::ofstream outFile(ACCOUNTS_FILE, std::ios::app);
        if (!outFile) {
            message = "Error opening accounts file for writing!";
            return false;
        }
        outFile << newUser.username << ","
                << newUser.password << ","
                << newUser.balance << ","
                << newUser.rewardPoints << ","
                << newUser.dailyExpenseLimit << ","
                << "0\n";
        outFile.close();

        userExpenseFile = newUser.username + "_expenses.csv";
        createUserFile();

        currentUser = newUser;
        message = "Registration successful!";
        return true;
    }

    bool loginUser(const std::string& username, const std::string& password, std::string& message) {
        if (username == ADMIN_USERNAME && password == ADMIN_PASSWORD) {
            currentUser.username = ADMIN_USERNAME;
            currentUser.password = ADMIN_PASSWORD;
            currentUser.isAdmin = true;
            message = "Admin login successful!";
            return true;
        }

        std::ifstream file(ACCOUNTS_FILE);
        if (file) {
            std::string line;
            bool found = false;
            while (getline(file, line)) {
                std::stringstream ss(line);
                std::string storedUser, storedPass;
                getline(ss, storedUser, ',');
                getline(ss, storedPass, ',');

                if (storedUser == username && storedPass == password) {
                    found = true;
                    currentUser.username = storedUser;
                    currentUser.password = storedPass;

                    std::string field;
                    try {
                        getline(ss, field, ',');
                        currentUser.balance = std::stod(field);

                        getline(ss, field, ',');
                        currentUser.rewardPoints = std::stoi(field);

                        getline(ss, field, ',');
                        currentUser.dailyExpenseLimit = std::stoi(field);

                        getline(ss, field, ',');
                        currentUser.isAdmin = (field == "1");
                    } catch (const std::invalid_argument& e) {
                        char errorMsg[512];
                        sprintf(errorMsg, "Error reading account data for %s: Invalid number format in line '%s'. Error: %s",
                                username.c_str(), line.c_str(), e.what());
                        MessageBoxA(hWndMain, errorMsg, "Login Data Error", MB_OK | MB_ICONWARNING);
                        message = "Corrupted account data. Please contact support.";
                        return false;
                    } catch (const std::out_of_range& e) {
                        char errorMsg[512];
                        sprintf(errorMsg, "Error reading account data for %s: Number out of range in line '%s'. Error: %s",
                                username.c_str(), line.c_str(), e.what());
                        MessageBoxA(hWndMain, errorMsg, "Login Data Error", MB_OK | MB_ICONWARNING);
                        message = "Corrupted account data. Please contact support.";
                        return false;
                    }

                    break;
                }
            }
            file.close();

            if (found) {
                userExpenseFile = currentUser.username + "_expenses.csv";
                loadUserData();
                message = "Login successful!";
                return true;
            }
        }
        message = "Invalid credentials.";
        return false;
    }

    bool addExpense(const std::string& name, const std::string& category, double amount, std::string& message) {
        Expense newExpense;
        newExpense.date = getCurrentDate();
        newExpense.isDeleted = false;
        newExpense.name = name;
        newExpense.category = category;
        newExpense.amount = amount;

        if (newExpense.amount <= 0) {
            message = "Amount must be positive.";
            return false;
        }

        if (newExpense.amount > currentUser.balance) {
            message = "Insufficient balance!";
            return false;
        }

        double todayExpense = 0;
        for (const auto& exp : expenses) {
            if (exp.date == newExpense.date && !exp.isDeleted) {
                todayExpense += exp.amount;
            }
        }

        if (currentUser.dailyExpenseLimit > 0 &&
            (todayExpense + newExpense.amount) > currentUser.dailyExpenseLimit) {
            message = "This expense exceeds your daily limit!";
            return false;
        }

        currentUser.balance -= newExpense.amount;
        currentUser.rewardPoints += static_cast<int>(newExpense.amount * REWARD_RATE);
        expenses.push_back(newExpense);
        saveUserData();
        updateAccountData();

        message = "Expense added successfully!";
        return true;
    }

    bool editExpense(int index, const std::string& newName, const std::string& newCategory, double newAmount, std::string& message) {
        if (index < 0 || index >= expenses.size()) {
            message = "Invalid expense index.";
            return false;
        }
        if (expenses[index].isDeleted) {
            message = "Cannot edit a deleted expense. Please restore it first.";
            return false;
        }
        if (newAmount <= 0) {
            message = "Amount must be positive.";
            return false;
        }

        double oldAmount = expenses[index].amount;
        double balanceChange = newAmount - oldAmount;

        if (currentUser.balance < balanceChange) {
            message = "Insufficient balance for this edit!";
            return false;
        }

        expenses[index].name = newName;
        expenses[index].category = newCategory;
        expenses[index].amount = newAmount;
        currentUser.balance -= balanceChange;
        currentUser.rewardPoints += static_cast<int>(balanceChange * REWARD_RATE);

        saveUserData();
        updateAccountData();
        message = "Expense updated successfully!";
        return true;
    }

    bool deleteExpense(int index, std::string& message) {
        if (index < 0 || index >= expenses.size()) {
            message = "Invalid expense index.";
            return false;
        }
        if (expenses[index].isDeleted) {
            message = "Expense is already deleted.";
            return false;
        }

        expenses[index].isDeleted = true;
        currentUser.balance += expenses[index].amount;
        currentUser.rewardPoints -= static_cast<int>(expenses[index].amount * REWARD_RATE);
        if (currentUser.rewardPoints < 0) currentUser.rewardPoints = 0;

        saveUserData();
        updateAccountData();
        message = "Expense deleted successfully and amount refunded.";
        return true;
    }

    // New function to restore a deleted expense
    bool restoreExpense(int index, std::string& message) {
        if (index < 0 || index >= expenses.size()) {
            message = "Invalid expense index.";
            return false;
        }
        if (!expenses[index].isDeleted) {
            message = "Expense is not deleted.";
            return false;
        }

        // Check if restoring this expense would exceed daily limit (optional, based on policy)
        // For simplicity, we'll assume admin can restore without daily limit check.
        // If a daily limit check is desired, it would be similar to addExpense logic.

        expenses[index].isDeleted = false;
        currentUser.balance -= expenses[index].amount; // Deduct amount again
        currentUser.rewardPoints += static_cast<int>(expenses[index].amount * REWARD_RATE); // Re-add rewards

        // Ensure balance doesn't go negative if it was already low before deletion
        if (currentUser.balance < 0) {
            // This scenario implies the user spent money they didn't have after deletion.
            // A more robust system would prevent this or handle it with an overdraft.
            // For now, we'll just set balance to 0 and warn.
            MessageBoxA(hWndMain, "Warning: Restoring this expense would result in negative balance. Balance set to $0.", "Balance Warning", MB_OK | MB_ICONWARNING);
            currentUser.balance = 0;
        }

        saveUserData();
        updateAccountData();
        message = "Expense restored successfully!";
        return true;
    }

    bool setDailyExpenseLimit(int limit, std::string& message) {
        if (limit < 0) {
            message = "Daily expense limit cannot be negative.";
            return false;
        }
        currentUser.dailyExpenseLimit = limit;
        updateAccountData();
        message = "Daily expense limit set successfully!";
        return true;
    }

    bool redeemRewardPoints(int pointsToRedeem, std::string& message) {
        if (pointsToRedeem <= 0) {
            message = "Please enter a positive number of points to redeem.";
            return false;
        }
        if (pointsToRedeem > currentUser.rewardPoints) {
            message = "Not enough reward points.";
            return false;
        }
        double amountEarned = static_cast<double>(pointsToRedeem);
        currentUser.balance += amountEarned;
        currentUser.rewardPoints -= pointsToRedeem;
        updateAccountData();
        char msgBuf[100];
        sprintf(msgBuf, "Successfully redeemed %d points for $%.2f!", pointsToRedeem, amountEarned);
        message = msgBuf;
        return true;
    }

    void generateReceipt(const Expense& expense) {
        char receiptContent[1024];
        sprintf(receiptContent,
                "::::::::::::::::::::::::::::\n"
                "        RECEIPT           \n"
                "::::::::::::::::::::::::::::\n"
                "Date: %s\n"
                "Name: %s\n"
                "Category: %s\n"
                "Amount: $%.2f\n"
                "Remaining Balance: $%.2f\n"
                "Reward Points Earned: %.2f\n"
                "::::::::::::::::::::::::::::\n",
                expense.date.c_str(),
                expense.name.c_str(),
                expense.category.c_str(),
                expense.amount,
                currentUser.balance,
                expense.amount * REWARD_RATE);

        MessageBoxA(hWndMain, receiptContent, "Expense Receipt", MB_OK | MB_ICONINFORMATION);
    }

    // New: viewAllAccounts now returns a formatted string of all accounts
    std::string viewAllAccounts() {
        std::string allAccountsStr = "::::::::::::::::::::::::::::\n";
        allAccountsStr += "::    ALL ACCOUNTS      ::\n";
        allAccountsStr += "::::::::::::::::::::::::::::\n";

        std::ifstream accountsFile(ACCOUNTS_FILE);
        if (!accountsFile) {
            return "Error: Could not open accounts file to view all accounts.\n";
        }

        std::string line;
        while (getline(accountsFile, line)) {
            std::stringstream ss(line);
            std::string username, password, balanceStr, rewardStr, limitStr, isAdminStr;
            getline(ss, username, ',');
            getline(ss, password, ','); // Password included for display, but be cautious in real apps
            getline(ss, balanceStr, ',');
            getline(ss, rewardStr, ',');
            getline(ss, limitStr, ',');
            getline(ss, isAdminStr, ',');

            allAccountsStr += "Username: " + username + ", Balance: $" + balanceStr +
                              ", Rewards: " + rewardStr + ", Limit: $" + limitStr +
                              ", Admin: " + (isAdminStr == "1" ? "Yes" : "No") + "\n";
        }
        accountsFile.close();

        if (allAccountsStr.length() < 100) {
             allAccountsStr += "\nNo accounts found.\n";
        }

        allAccountsStr += "----------------------------\n";
        return allAccountsStr;
    }

    // Modified: viewAllExpenses now returns a formatted string of all expenses
    std::string viewAllExpenses() {
        std::string allExpensesStr = "::::::::::::::::::::::::::::\n";
        allExpensesStr += "::    ALL EXPENSES      ::\n";
        allExpensesStr += "::::::::::::::::::::::::::::\n";

        std::ifstream accountsFile(ACCOUNTS_FILE);
        if (!accountsFile) {
            return "Error: Could not open accounts file to view all expenses.\n";
        }

        std::string line;
        while (getline(accountsFile, line)) {
            std::stringstream ss(line);
            std::string username;
            getline(ss, username, ',');

            std::string userFile = username + "_expenses.csv";
            std::ifstream expenseFile(userFile);

            if (expenseFile.good()) {
                allExpensesStr += "\n--- Expenses for User: " + username + " ---\n";
                std::string expenseLine;
                while (getline(expenseFile, expenseLine)) {
                    if (expenseLine.empty()) continue;
                    std::stringstream es(expenseLine);
                    std::string date, name, category, amount, isDeleted;
                    getline(es, date, ',');
                    getline(es, name, ',');
                    getline(es, category, ',');
                    getline(es, amount, ',');
                    getline(es, isDeleted, ',');

                    allExpensesStr += "Date: " + date + ", Name: " + name + ", Category: " + category + ", Amount: $" + amount;
                    if (isDeleted == "1") {
                        allExpensesStr += " (DELETED)";
                    }
                    allExpensesStr += "\n";
                }
                expenseFile.close();
            } else {
                allExpensesStr += "\n--- No expense records found for User: " + username + " ---\n";
            }
        }
        accountsFile.close();

        if (allExpensesStr.length() < 100) {
             allExpensesStr += "\nNo expenses found across all users.\n";
        }

        allExpensesStr += "----------------------------\n";
        return allExpensesStr;
    }
};

// --- Global Instance of BudgetBuddy ---
static BudgetBuddy budgetBuddyApp;

// --- Screen Management ---
enum AppScreen {
    SCREEN_MAIN_MENU,
    SCREEN_REGISTER,
    SCREEN_LOGIN,
    SCREEN_USER_MENU,
    SCREEN_ADMIN_MENU,
    SCREEN_ADD_EXPENSE,
    SCREEN_VIEW_BALANCE,
    SCREEN_MANAGE_RECORDS,
    SCREEN_SET_LIMIT,
    SCREEN_REDEEM_REWARDS,
    SCREEN_ALL_EXPENSES,
    SCREEN_ALL_ACCOUNTS // New screen for viewing all accounts
};
AppScreen currentScreen = SCREEN_MAIN_MENU;

// Global UI element handles for various screens
// Main Menu
HWND hTitleStatic;
HWND hRegisterButton;
HWND hLoginButton;
HWND hExitButton;

// Register/Login Screen common elements
HWND hUsernameLabel;
HWND hUsernameEdit;
HWND hPasswordLabel;
HWND hPasswordEdit;
HWND hSubmitButton;
HWND hCancelButton;
HWND hMessageStatic;

// User Menu Controls
HWND hUserMenuTitle;
HWND hBtnAddExpense;
HWND hBtnViewBalance;
HWND hBtnManageRecords;
HWND hBtnSetLimit;
HWND hBtnRedeemRewards;
HWND hBtnUserLogout;

// Add Expense Screen Controls
HWND hStaticExpenseName;
HWND hEditExpenseName;
HWND hStaticExpenseCategory;
HWND hComboExpenseCategory;
HWND hStaticExpenseAmount;
HWND hEditExpenseAmount;
HWND hBtnAddExpenseSubmit;
HWND hBtnAddExpenseCancel;

// View Balance Screen Controls
HWND hStaticBalanceInfo;
HWND hListViewExpenses;
HWND hBtnViewBalanceBack;

// Manage Records Screen Controls
HWND hListViewManageExpenses;
HWND hBtnEditExpense;
HWND hBtnDeleteExpense;
HWND hBtnRestoreExpense; // New button handle
HWND hBtnManageRecordsBack;
HWND hStaticManageMessage;

// Set Expense Limit Screen Controls
HWND hStaticCurrentLimit;
HWND hEditNewLimit;
HWND hBtnSetLimitSubmit;
HWND hBtnSetLimitCancel;
HWND hStaticSetLimitMessage;

// Redeem Rewards Screen Controls
HWND hStaticRewardInfo;
HWND hEditRedeemPoints;
HWND hBtnRedeemRewardsSubmit;
HWND hBtnRedeemRewardsCancel;
HWND hStaticRedeemMessage;

// Admin Menu Controls
HWND hBtnViewAllAccounts;
HWND hBtnViewAllExpenses;

// View All Expenses Screen Controls
HWND hStaticAllExpensesTitle;
HWND hEditAllExpensesDisplay;
HWND hBtnAllExpensesBack;

// View All Accounts Screen Controls (New)
HWND hStaticAllAccountsTitle;
HWND hEditAllAccountsDisplay;
HWND hBtnAllAccountsBack;


// Function to hide all controls for a given screen
void HideAllControls() {
    // Main Menu controls
    if (hTitleStatic) ShowWindow(hTitleStatic, SW_HIDE);
    if (hRegisterButton) ShowWindow(hRegisterButton, SW_HIDE);
    if (hLoginButton) ShowWindow(hLoginButton, SW_HIDE);
    if (hExitButton) ShowWindow(hExitButton, SW_HIDE);

    // Register/Login common controls
    if (hUsernameLabel) ShowWindow(hUsernameLabel, SW_HIDE);
    if (hUsernameEdit) ShowWindow(hUsernameEdit, SW_HIDE);
    if (hPasswordLabel) ShowWindow(hPasswordLabel, SW_HIDE);
    if (hPasswordEdit) ShowWindow(hPasswordEdit, SW_HIDE);
    if (hSubmitButton) ShowWindow(hSubmitButton, SW_HIDE);
    if (hCancelButton) ShowWindow(hCancelButton, SW_HIDE);
    if (hMessageStatic) ShowWindow(hMessageStatic, SW_HIDE);

    // User Menu Controls
    if (hUserMenuTitle) ShowWindow(hUserMenuTitle, SW_HIDE);
    if (hBtnAddExpense) ShowWindow(hBtnAddExpense, SW_HIDE);
    if (hBtnViewBalance) ShowWindow(hBtnViewBalance, SW_HIDE);
    if (hBtnManageRecords) ShowWindow(hBtnManageRecords, SW_HIDE);
    if (hBtnSetLimit) ShowWindow(hBtnSetLimit, SW_HIDE);
    if (hBtnRedeemRewards) ShowWindow(hBtnRedeemRewards, SW_HIDE);
    if (hBtnUserLogout) ShowWindow(hBtnUserLogout, SW_HIDE);

    // Add Expense Screen Controls
    if (hStaticExpenseName) ShowWindow(hStaticExpenseName, SW_HIDE);
    if (hEditExpenseName) ShowWindow(hEditExpenseName, SW_HIDE);
    if (hStaticExpenseCategory) ShowWindow(hStaticExpenseCategory, SW_HIDE);
    if (hComboExpenseCategory) ShowWindow(hComboExpenseCategory, SW_HIDE);
    if (hStaticExpenseAmount) ShowWindow(hStaticExpenseAmount, SW_HIDE);
    if (hEditExpenseAmount) ShowWindow(hEditExpenseAmount, SW_HIDE);
    if (hBtnAddExpenseSubmit) ShowWindow(hBtnAddExpenseSubmit, SW_HIDE);
    if (hBtnAddExpenseCancel) ShowWindow(hBtnAddExpenseCancel, SW_HIDE);

    // View Balance Screen Controls
    if (hStaticBalanceInfo) ShowWindow(hStaticBalanceInfo, SW_HIDE);
    if (hListViewExpenses) ShowWindow(hListViewExpenses, SW_HIDE);
    if (hBtnViewBalanceBack) ShowWindow(hBtnViewBalanceBack, SW_HIDE);

    // Manage Records Screen Controls
    if (hListViewManageExpenses) ShowWindow(hListViewManageExpenses, SW_HIDE);
    if (hBtnEditExpense) ShowWindow(hBtnEditExpense, SW_HIDE);
    if (hBtnDeleteExpense) ShowWindow(hBtnDeleteExpense, SW_HIDE);
    if (hBtnRestoreExpense) ShowWindow(hBtnRestoreExpense, SW_HIDE); // Hide new button
    if (hBtnManageRecordsBack) ShowWindow(hBtnManageRecordsBack, SW_HIDE);
    if (hStaticManageMessage) ShowWindow(hStaticManageMessage, SW_HIDE);

    // Set Expense Limit Screen Controls
    if (hStaticCurrentLimit) ShowWindow(hStaticCurrentLimit, SW_HIDE);
    if (hEditNewLimit) ShowWindow(hEditNewLimit, SW_HIDE);
    if (hBtnSetLimitSubmit) ShowWindow(hBtnSetLimitSubmit, SW_HIDE);
    if (hBtnSetLimitCancel) ShowWindow(hBtnSetLimitCancel, SW_HIDE);
    if (hStaticSetLimitMessage) ShowWindow(hStaticSetLimitMessage, SW_HIDE);

    // Redeem Rewards Screen Controls
    if (hStaticRewardInfo) ShowWindow(hStaticRewardInfo, SW_HIDE);
    if (hEditRedeemPoints) ShowWindow(hEditRedeemPoints, SW_HIDE);
    if (hBtnRedeemRewardsSubmit) ShowWindow(hBtnRedeemRewardsSubmit, SW_HIDE);
    if (hBtnRedeemRewardsCancel) ShowWindow(hBtnRedeemRewardsCancel, SW_HIDE);
    if (hStaticRedeemMessage) ShowWindow(hStaticRedeemMessage, SW_HIDE);

    // Admin Menu Controls
    if (hBtnViewAllAccounts) ShowWindow(hBtnViewAllAccounts, SW_HIDE);
    if (hBtnViewAllExpenses) ShowWindow(hBtnViewAllExpenses, SW_HIDE);

    // View All Expenses Screen Controls
    if (hStaticAllExpensesTitle) ShowWindow(hStaticAllExpensesTitle, SW_HIDE);
    if (hEditAllExpensesDisplay) ShowWindow(hEditAllExpensesDisplay, SW_HIDE);
    if (hBtnAllExpensesBack) ShowWindow(hBtnAllExpensesBack, SW_HIDE);

    // View All Accounts Screen Controls
    if (hStaticAllAccountsTitle) ShowWindow(hStaticAllAccountsTitle, SW_HIDE);
    if (hEditAllAccountsDisplay) ShowWindow(hEditAllAccountsDisplay, SW_HIDE);
    if (hBtnAllAccountsBack) ShowWindow(hBtnAllAccountsBack, SW_HIDE);
}

// Function to create and show Main Menu controls
void ShowMainMenuScreen(HWND hWnd) {
    HideAllControls();

    if (!hTitleStatic) {
        hTitleStatic = CreateWindowExA(0, "STATIC", "::    BUDGET BUDDY       ::", WS_CHILD | WS_VISIBLE | SS_CENTER,
            100, 50, 400, 30, hWnd, (HMENU)(LONG_PTR)IDC_STATIC_TITLE, hInst, NULL);
    } else {
        SetWindowTextA(hTitleStatic, "::    BUDGET BUDDY       ::");
        ShowWindow(hTitleStatic, SW_SHOW);
    }

    if (!hRegisterButton) {
        hRegisterButton = CreateWindowExA(0, "BUTTON", "1. Register", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            200, 100, 200, 40, hWnd, (HMENU)(LONG_PTR)IDC_BTN_REGISTER, hInst, NULL);
    } else {
        ShowWindow(hRegisterButton, SW_SHOW);
    }

    if (!hLoginButton) {
        hLoginButton = CreateWindowExA(0, "BUTTON", "2. Login", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            200, 150, 200, 40, hWnd, (HMENU)(LONG_PTR)IDC_BTN_LOGIN, hInst, NULL);
    } else {
        ShowWindow(hLoginButton, SW_SHOW);
    }

    if (!hExitButton) {
        hExitButton = CreateWindowExA(0, "BUTTON", "3. Exit", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            200, 200, 200, 40, hWnd, (HMENU)(LONG_PTR)IDC_BTN_EXIT, hInst, NULL);
    } else {
        ShowWindow(hExitButton, SW_SHOW);
    }

    currentScreen = SCREEN_MAIN_MENU;
    InvalidateRect(hWnd, NULL, TRUE);
}

// Function to create and show Register Screen controls
void ShowRegisterScreen(HWND hWnd) {
    HideAllControls();

    if (!hTitleStatic) {
        hTitleStatic = CreateWindowExA(0, "STATIC", "::      REGISTER         ::", WS_CHILD | WS_VISIBLE | SS_CENTER,
            100, 50, 400, 30, hWnd, (HMENU)(LONG_PTR)IDC_STATIC_TITLE, hInst, NULL);
    } else {
        SetWindowTextA(hTitleStatic, "::      REGISTER         ::");
        ShowWindow(hTitleStatic, SW_SHOW);
    }

    if (!hUsernameLabel) {
        hUsernameLabel = CreateWindowExA(0, "STATIC", "Username:", WS_CHILD | WS_VISIBLE,
            150, 100, 100, 20, hWnd, NULL, hInst, NULL);
    } else {
        ShowWindow(hUsernameLabel, SW_SHOW);
    }

    if (!hUsernameEdit) {
        hUsernameEdit = CreateWindowExA(WS_EX_CLIENTEDGE, "EDIT", "", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL,
            260, 100, 200, 25, hWnd, (HMENU)(LONG_PTR)IDC_EDIT_USERNAME, hInst, NULL);
    } else {
        SetWindowTextA(hUsernameEdit, "");
        ShowWindow(hUsernameEdit, SW_SHOW);
    }

    if (!hPasswordLabel) {
        hPasswordLabel = CreateWindowExA(0, "STATIC", "Password:", WS_CHILD | WS_VISIBLE,
            150, 130, 100, 20, hWnd, NULL, hInst, NULL);
    } else {
        ShowWindow(hPasswordLabel, SW_SHOW);
    }

    if (!hPasswordEdit) {
        hPasswordEdit = CreateWindowExA(WS_EX_CLIENTEDGE, "EDIT", "", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL | ES_PASSWORD,
            260, 130, 200, 25, hWnd, (HMENU)(LONG_PTR)IDC_EDIT_PASSWORD, hInst, NULL);
    } else {
        SetWindowTextA(hPasswordEdit, "");
        ShowWindow(hPasswordEdit, SW_SHOW);
    }

    if (!hSubmitButton) {
        hSubmitButton = CreateWindowExA(0, "BUTTON", "Register", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            200, 180, 100, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_SUBMIT, hInst, NULL);
    } else {
        SetWindowTextA(hSubmitButton, "Register");
        ShowWindow(hSubmitButton, SW_SHOW);
    }

    if (!hCancelButton) {
        hCancelButton = CreateWindowExA(0, "BUTTON", "Cancel", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            310, 180, 100, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_CANCEL, hInst, NULL);
    } else {
        ShowWindow(hCancelButton, SW_SHOW);
    }

    if (!hMessageStatic) {
        hMessageStatic = CreateWindowExA(0, "STATIC", "", WS_CHILD | WS_VISIBLE | SS_CENTER,
            100, 220, 400, 20, hWnd, (HMENU)(LONG_PTR)IDC_STATIC_MESSAGE, hInst, NULL);
    } else {
        SetWindowTextA(hMessageStatic, "");
        ShowWindow(hMessageStatic, SW_SHOW);
    }

    currentScreen = SCREEN_REGISTER;
    InvalidateRect(hWnd, NULL, TRUE);
}

// Function to create and show Login Screen controls
void ShowLoginScreen(HWND hWnd) {
    HideAllControls();

    if (!hTitleStatic) {
        hTitleStatic = CreateWindowExA(0, "STATIC", "::        LOGIN          ::", WS_CHILD | WS_VISIBLE | SS_CENTER,
            100, 50, 400, 30, hWnd, (HMENU)(LONG_PTR)IDC_STATIC_TITLE, hInst, NULL);
    } else {
        SetWindowTextA(hTitleStatic, "::        LOGIN          ::");
        ShowWindow(hTitleStatic, SW_SHOW);
    }

    if (!hUsernameLabel) {
        hUsernameLabel = CreateWindowExA(0, "STATIC", "Username:", WS_CHILD | WS_VISIBLE,
            150, 100, 100, 20, hWnd, NULL, hInst, NULL);
    } else {
        ShowWindow(hUsernameLabel, SW_SHOW);
    }

    if (!hUsernameEdit) {
        hUsernameEdit = CreateWindowExA(WS_EX_CLIENTEDGE, "EDIT", "", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL,
            260, 100, 200, 25, hWnd, (HMENU)(LONG_PTR)IDC_EDIT_USERNAME, hInst, NULL);
    } else {
        SetWindowTextA(hUsernameEdit, "");
        ShowWindow(hUsernameEdit, SW_SHOW);
    }

    if (!hPasswordLabel) {
        hPasswordLabel = CreateWindowExA(0, "STATIC", "Password:", WS_CHILD | WS_VISIBLE,
            150, 130, 100, 20, hWnd, NULL, hInst, NULL);
    } else {
        ShowWindow(hPasswordLabel, SW_SHOW);
    }

    if (!hPasswordEdit) {
        hPasswordEdit = CreateWindowExA(WS_EX_CLIENTEDGE, "EDIT", "", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL | ES_PASSWORD,
            260, 130, 200, 25, hWnd, (HMENU)(LONG_PTR)IDC_EDIT_PASSWORD, hInst, NULL);
    } else {
        SetWindowTextA(hPasswordEdit, "");
        ShowWindow(hPasswordEdit, SW_SHOW);
    }

    if (!hSubmitButton) {
        hSubmitButton = CreateWindowExA(0, "BUTTON", "Login", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            200, 180, 100, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_SUBMIT, hInst, NULL);
    } else {
        SetWindowTextA(hSubmitButton, "Login");
        ShowWindow(hSubmitButton, SW_SHOW);
    }

    if (!hCancelButton) {
        hCancelButton = CreateWindowExA(0, "BUTTON", "Cancel", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            310, 180, 100, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_CANCEL, hInst, NULL);
    } else {
        ShowWindow(hCancelButton, SW_SHOW);
    }

    if (!hMessageStatic) {
        hMessageStatic = CreateWindowExA(0, "STATIC", "", WS_CHILD | WS_VISIBLE | SS_CENTER,
            100, 220, 400, 20, hWnd, (HMENU)(LONG_PTR)IDC_STATIC_MESSAGE, hInst, NULL);
    } else {
        SetWindowTextA(hMessageStatic, "");
        ShowWindow(hMessageStatic, SW_SHOW);
    }

    currentScreen = SCREEN_LOGIN;
    InvalidateRect(hWnd, NULL, TRUE);
}

// Function to create and show User Menu controls
void ShowUserMenuScreen(HWND hWnd) {
    HideAllControls();

    char titleBuf[100];
    sprintf(titleBuf, "::      USER MENU (%s)       ::", budgetBuddyApp.GetCurrentUser().username.c_str());

    if (!hUserMenuTitle) {
        hUserMenuTitle = CreateWindowExA(0, "STATIC", titleBuf, WS_CHILD | WS_VISIBLE | SS_CENTER,
            100, 50, 400, 30, hWnd, (HMENU)NULL, hInst, NULL);
    } else {
        SetWindowTextA(hUserMenuTitle, titleBuf);
        ShowWindow(hUserMenuTitle, SW_SHOW);
    }

    if (!hBtnAddExpense) {
        hBtnAddExpense = CreateWindowExA(0, "BUTTON", "1. Add Expense", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            200, 100, 200, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_ADD_EXPENSE, hInst, NULL);
    } else {
        ShowWindow(hBtnAddExpense, SW_SHOW);
    }

    if (!hBtnViewBalance) {
        hBtnViewBalance = CreateWindowExA(0, "BUTTON", "2. View Balance", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            200, 140, 200, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_VIEW_BALANCE, hInst, NULL);
    } else {
        ShowWindow(hBtnViewBalance, SW_SHOW);
    }

    if (!hBtnManageRecords) {
        hBtnManageRecords = CreateWindowExA(0, "BUTTON", "3. Manage Records", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            200, 180, 200, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_MANAGE_RECORDS, hInst, NULL);
    } else {
        ShowWindow(hBtnManageRecords, SW_SHOW);
    }

    if (!hBtnSetLimit) {
        hBtnSetLimit = CreateWindowExA(0, "BUTTON", "4. Set Expense Limit", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            200, 220, 200, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_SET_LIMIT, hInst, NULL);
    } else {
        ShowWindow(hBtnSetLimit, SW_SHOW);
    }

    if (!hBtnRedeemRewards) {
        hBtnRedeemRewards = CreateWindowExA(0, "BUTTON", "5. Redeem Rewards", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            200, 260, 200, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_REDEEM_REWARDS, hInst, NULL);
    } else {
        ShowWindow(hBtnRedeemRewards, SW_SHOW);
    }

    if (!hBtnUserLogout) {
        hBtnUserLogout = CreateWindowExA(0, "BUTTON", "6. Logout", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            200, 300, 200, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_USER_LOGOUT, hInst, NULL);
    } else {
        ShowWindow(hBtnUserLogout, SW_SHOW);
    }

    currentScreen = SCREEN_USER_MENU;
    InvalidateRect(hWnd, NULL, TRUE);
}

// Function to create and show Admin Menu Screen controls
void ShowAdminMenuScreen(HWND hWnd) {
    HideAllControls();

    char titleBuf[100];
    sprintf(titleBuf, "::      ADMIN MENU       ::");

    if (!hTitleStatic) {
        hTitleStatic = CreateWindowExA(0, "STATIC", titleBuf, WS_CHILD | WS_VISIBLE | SS_CENTER,
            100, 50, 400, 30, hWnd, (HMENU)(LONG_PTR)IDC_STATIC_TITLE, hInst, NULL);
    } else {
        SetWindowTextA(hTitleStatic, titleBuf);
        ShowWindow(hTitleStatic, SW_SHOW);
    }

    if (!hBtnViewAllAccounts) {
        hBtnViewAllAccounts = CreateWindowExA(0, "BUTTON", "1. View All Accounts", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            200, 100, 200, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_VIEW_ALL_ACCOUNTS, hInst, NULL);
    } else {
        ShowWindow(hBtnViewAllAccounts, SW_SHOW);
    }

    if (!hBtnViewAllExpenses) {
        hBtnViewAllExpenses = CreateWindowExA(0, "BUTTON", "2. View All Expenses", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            200, 140, 200, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_VIEW_ALL_EXPENSES, hInst, NULL);
    } else {
        ShowWindow(hBtnViewAllExpenses, SW_SHOW);
    }

    if (!hBtnUserLogout) {
        hBtnUserLogout = CreateWindowExA(0, "BUTTON", "3. Logout", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            200, 180, 200, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_USER_LOGOUT, hInst, NULL);
    } else {
        ShowWindow(hBtnUserLogout, SW_SHOW);
    }

    currentScreen = SCREEN_ADMIN_MENU;
    InvalidateRect(hWnd, NULL, TRUE);
}

// Function to create and show Add Expense Screen controls
void ShowAddExpenseScreen(HWND hWnd) {
    HideAllControls();

    if (!hTitleStatic) {
        hTitleStatic = CreateWindowExA(0, "STATIC", "::      ADD EXPENSE      ::", WS_CHILD | WS_VISIBLE | SS_CENTER,
            100, 50, 400, 30, hWnd, (HMENU)(LONG_PTR)IDC_STATIC_TITLE, hInst, NULL);
    } else {
        SetWindowTextA(hTitleStatic, "::      ADD EXPENSE      ::");
        ShowWindow(hTitleStatic, SW_SHOW);
    }

    if (!hStaticExpenseName) {
        hStaticExpenseName = CreateWindowExA(0, "STATIC", "Expense Name:", WS_CHILD | WS_VISIBLE,
            100, 100, 120, 20, hWnd, NULL, hInst, NULL);
    } else {
        ShowWindow(hStaticExpenseName, SW_SHOW);
    }

    if (!hEditExpenseName) {
        hEditExpenseName = CreateWindowExA(WS_EX_CLIENTEDGE, "EDIT", "", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL,
            230, 100, 250, 25, hWnd, (HMENU)(LONG_PTR)IDC_EDIT_EXPENSE_NAME, hInst, NULL);
    } else {
        SetWindowTextA(hEditExpenseName, "");
        ShowWindow(hEditExpenseName, SW_SHOW);
    }

    if (!hStaticExpenseCategory) {
        hStaticExpenseCategory = CreateWindowExA(0, "STATIC", "Category:", WS_CHILD | WS_VISIBLE,
            100, 130, 120, 20, hWnd, NULL, hInst, NULL);
    } else {
        ShowWindow(hStaticExpenseCategory, SW_SHOW);
    }

    if (!hComboExpenseCategory) {
        hComboExpenseCategory = CreateWindowExA(0, "COMBOBOX", "", WS_CHILD | WS_VISIBLE | CBS_DROPDOWNLIST | WS_VSCROLL,
            230, 130, 150, 150, hWnd, (HMENU)(LONG_PTR)IDC_COMBO_EXPENSE_CATEGORY, hInst, NULL);
        // Add categories to the combo box
        SendMessageA(hComboExpenseCategory, CB_ADDSTRING, (WPARAM)0, (LPARAM)"Food");
        SendMessageA(hComboExpenseCategory, CB_ADDSTRING, (WPARAM)0, (LPARAM)"Essentials");
        SendMessageA(hComboExpenseCategory, CB_ADDSTRING, (WPARAM)0, (LPARAM)"Clothing");
        SendMessageA(hComboExpenseCategory, CB_ADDSTRING, (WPARAM)0, (LPARAM)"Fees");
        SendMessageA(hComboExpenseCategory, CB_ADDSTRING, (WPARAM)0, (LPARAM)"Transport");
        SendMessageA(hComboExpenseCategory, CB_ADDSTRING, (WPARAM)0, (LPARAM)"Entertainment");
        SendMessageA(hComboExpenseCategory, CB_ADDSTRING, (WPARAM)0, (LPARAM)"Others");
        SendMessageA(hComboExpenseCategory, CB_SETCURSEL, (WPARAM)0, (LPARAM)0); // Select first item
    } else {
        ShowWindow(hComboExpenseCategory, SW_SHOW);
    }

    if (!hStaticExpenseAmount) {
        hStaticExpenseAmount = CreateWindowExA(0, "STATIC", "Amount ($):", WS_CHILD | WS_VISIBLE,
            100, 160, 120, 20, hWnd, NULL, hInst, NULL);
    } else {
        ShowWindow(hStaticExpenseAmount, SW_SHOW);
    }

    if (!hEditExpenseAmount) {
        hEditExpenseAmount = CreateWindowExA(WS_EX_CLIENTEDGE, "EDIT", "", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL | ES_NUMBER,
            230, 160, 100, 25, hWnd, (HMENU)(LONG_PTR)IDC_EDIT_EXPENSE_AMOUNT, hInst, NULL);
    } else {
        SetWindowTextA(hEditExpenseAmount, "");
        ShowWindow(hEditExpenseAmount, SW_SHOW);
    }

    if (!hBtnAddExpenseSubmit) {
        hBtnAddExpenseSubmit = CreateWindowExA(0, "BUTTON", "Add Expense", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            180, 220, 120, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_ADD_EXPENSE_SUBMIT, hInst, NULL);
    } else {
        ShowWindow(hBtnAddExpenseSubmit, SW_SHOW);
    }

    if (!hBtnAddExpenseCancel) {
        hBtnAddExpenseCancel = CreateWindowExA(0, "BUTTON", "Cancel", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            310, 220, 100, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_ADD_EXPENSE_CANCEL, hInst, NULL);
    } else {
        ShowWindow(hBtnAddExpenseCancel, SW_SHOW);
    }

    if (!hMessageStatic) {
        hMessageStatic = CreateWindowExA(0, "STATIC", "", WS_CHILD | WS_VISIBLE | SS_CENTER,
            100, 260, 400, 20, hWnd, (HMENU)(LONG_PTR)IDC_STATIC_MESSAGE, hInst, NULL);
    } else {
        SetWindowTextA(hMessageStatic, "");
        ShowWindow(hMessageStatic, SW_SHOW);
    }

    currentScreen = SCREEN_ADD_EXPENSE;
    InvalidateRect(hWnd, NULL, TRUE);
}

// Function to create and show View Balance Screen controls
void ShowViewBalanceScreen(HWND hWnd) {
    HideAllControls();

    char balanceInfoBuf[512];
    sprintf(balanceInfoBuf,
            "Current Balance: $%.2f\n"
            "Reward Points: %d\n"
            "Daily Expense Limit: $%d",
            budgetBuddyApp.GetCurrentUser().balance,
            budgetBuddyApp.GetCurrentUser().rewardPoints,
            budgetBuddyApp.GetCurrentUser().dailyExpenseLimit);

    if (!hStaticBalanceInfo) {
        hStaticBalanceInfo = CreateWindowExA(0, "STATIC", balanceInfoBuf, WS_CHILD | WS_VISIBLE,
            50, 50, 500, 60, hWnd, (HMENU)(LONG_PTR)IDC_STATIC_BALANCE_INFO, hInst, NULL);
    } else {
        SetWindowTextA(hStaticBalanceInfo, balanceInfoBuf);
        ShowWindow(hStaticBalanceInfo, SW_SHOW);
    }

    if (!hListViewExpenses) {
        hListViewExpenses = CreateWindowExA(
            WS_EX_CLIENTEDGE,
            WC_LISTVIEWA,
            "",
            WS_CHILD | WS_VISIBLE | LVS_REPORT | LVS_SINGLESEL | LVS_SHOWSELALWAYS,
            50, 120, 500, 200,
            hWnd,
            (HMENU)(LONG_PTR)IDC_LISTVIEW_EXPENSES,
            hInst,
            NULL
        );

        // Add columns to the ListView
        LVCOLUMNA lvc;
        lvc.mask = LVCF_FMT | LVCF_WIDTH | LVCF_TEXT | LVCF_SUBITEM;

        lvc.iSubItem = 0;
        lvc.pszText = (LPSTR)"Date";
        lvc.cx = 90;
        lvc.fmt = LVCFMT_LEFT;
        ListView_InsertColumn(hListViewExpenses, 0, &lvc);

        lvc.iSubItem = 1;
        lvc.pszText = (LPSTR)"Name";
        lvc.cx = 150;
        lvc.fmt = LVCFMT_LEFT;
        ListView_InsertColumn(hListViewExpenses, 1, &lvc);

        lvc.iSubItem = 2;
        lvc.pszText = (LPSTR)"Category";
        lvc.cx = 90;
        lvc.fmt = LVCFMT_LEFT;
        ListView_InsertColumn(hListViewExpenses, 2, &lvc);

        lvc.iSubItem = 3;
        lvc.pszText = (LPSTR)"Amount ($)";
        lvc.cx = 80;
        lvc.fmt = LVCFMT_RIGHT;
        ListView_InsertColumn(hListViewExpenses, 3, &lvc);

        lvc.iSubItem = 4;
        lvc.pszText = (LPSTR)"Deleted";
        lvc.cx = 60;
        lvc.fmt = LVCFMT_CENTER;
        ListView_InsertColumn(hListViewExpenses, 4, &lvc);

    } else {
        ListView_DeleteAllItems(hListViewExpenses);
        ShowWindow(hListViewExpenses, SW_SHOW);
    }

    // Populate ListView with expense data
    const auto& expenses = budgetBuddyApp.GetExpenses();
    for (size_t i = 0; i < expenses.size(); ++i) {
        const auto& exp = expenses[i];
        if (!exp.isDeleted) {
            LVITEMA lvItem;
            ZeroMemory(&lvItem, sizeof(lvItem));
            lvItem.mask = LVIF_TEXT;
            lvItem.iItem = ListView_GetItemCount(hListViewExpenses);
            lvItem.iSubItem = 0;
            lvItem.pszText = (LPSTR)exp.date.c_str();
            ListView_InsertItem(hListViewExpenses, &lvItem);

            ListView_SetItemText(hListViewExpenses, lvItem.iItem, 1, (LPSTR)exp.name.c_str());
            ListView_SetItemText(hListViewExpenses, lvItem.iItem, 2, (LPSTR)exp.category.c_str());
            char amountBuf[32];
            sprintf(amountBuf, "%.2f", exp.amount);
            ListView_SetItemText(hListViewExpenses, lvItem.iItem, 3, amountBuf);
            ListView_SetItemText(hListViewExpenses, lvItem.iItem, 4, (LPSTR)(exp.isDeleted ? "Yes" : "No"));
        }
    }

    if (!hBtnViewBalanceBack) {
        hBtnViewBalanceBack = CreateWindowExA(0, "BUTTON", "Back to Menu", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            250, 330, 100, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_VIEW_BALANCE_BACK, hInst, NULL);
    } else {
        ShowWindow(hBtnViewBalanceBack, SW_SHOW);
    }

    currentScreen = SCREEN_VIEW_BALANCE;
    InvalidateRect(hWnd, NULL, TRUE);
}

// Function to create and show Manage Records Screen controls
void ShowManageRecordsScreen(HWND hWnd) {
    HideAllControls();

    if (!hTitleStatic) {
        hTitleStatic = CreateWindowExA(0, "STATIC", "::    MANAGE RECORDS     ::", WS_CHILD | WS_VISIBLE | SS_CENTER,
            100, 30, 400, 30, hWnd, (HMENU)(LONG_PTR)IDC_STATIC_TITLE, hInst, NULL);
    } else {
        SetWindowTextA(hTitleStatic, "::    MANAGE RECORDS     ::");
        ShowWindow(hTitleStatic, SW_SHOW);
    }

    if (!hListViewManageExpenses) {
        hListViewManageExpenses = CreateWindowExA(
            WS_EX_CLIENTEDGE,
            WC_LISTVIEWA,
            "",
            WS_CHILD | WS_VISIBLE | LVS_REPORT | LVS_SINGLESEL | LVS_SHOWSELALWAYS,
            50, 70, 500, 200,
            hWnd,
            (HMENU)(LONG_PTR)IDC_LISTVIEW_MANAGE_EXPENSES,
            hInst,
            NULL
        );

        // Add columns
        LVCOLUMNA lvc;
        lvc.mask = LVCF_FMT | LVCF_WIDTH | LVCF_TEXT | LVCF_SUBITEM;

        lvc.iSubItem = 0; lvc.pszText = (LPSTR)"ID"; lvc.cx = 30; lvc.fmt = LVCFMT_LEFT;
        ListView_InsertColumn(hListViewManageExpenses, 0, &lvc);
        lvc.iSubItem = 1; lvc.pszText = (LPSTR)"Date"; lvc.cx = 90; lvc.fmt = LVCFMT_LEFT;
        ListView_InsertColumn(hListViewManageExpenses, 1, &lvc);
        lvc.iSubItem = 2; lvc.pszText = (LPSTR)"Name"; lvc.cx = 120; lvc.fmt = LVCFMT_LEFT;
        ListView_InsertColumn(hListViewManageExpenses, 2, &lvc);
        lvc.iSubItem = 3; lvc.pszText = (LPSTR)"Category"; lvc.cx = 80; lvc.fmt = LVCFMT_LEFT;
        ListView_InsertColumn(hListViewManageExpenses, 3, &lvc);
        lvc.iSubItem = 4; lvc.pszText = (LPSTR)"Amount ($)"; lvc.cx = 80; lvc.fmt = LVCFMT_RIGHT;
        ListView_InsertColumn(hListViewManageExpenses, 4, &lvc);
        lvc.iSubItem = 5; lvc.pszText = (LPSTR)"Deleted"; lvc.cx = 60; lvc.fmt = LVCFMT_CENTER;
        ListView_InsertColumn(hListViewManageExpenses, 5, &lvc);

    } else {
        ListView_DeleteAllItems(hListViewManageExpenses);
        ShowWindow(hListViewManageExpenses, SW_SHOW);
    }

    // Populate ListView
    const auto& expenses = budgetBuddyApp.GetExpenses();
    for (size_t i = 0; i < expenses.size(); ++i) {
        const auto& exp = expenses[i];
        LVITEMA lvItem;
        ZeroMemory(&lvItem, sizeof(lvItem));
        lvItem.mask = LVIF_TEXT;
        lvItem.iItem = ListView_GetItemCount(hListViewManageExpenses);
        lvItem.iSubItem = 0;
        char idBuf[10];
        sprintf(idBuf, "%d", (int)i); // Use index as ID
        lvItem.pszText = idBuf;
        ListView_InsertItem(hListViewManageExpenses, &lvItem);

        ListView_SetItemText(hListViewManageExpenses, lvItem.iItem, 1, (LPSTR)exp.date.c_str());
        ListView_SetItemText(hListViewManageExpenses, lvItem.iItem, 2, (LPSTR)exp.name.c_str());
        ListView_SetItemText(hListViewManageExpenses, lvItem.iItem, 3, (LPSTR)exp.category.c_str());
        char amountBuf[32];
        sprintf(amountBuf, "%.2f", exp.amount);
        ListView_SetItemText(hListViewManageExpenses, lvItem.iItem, 4, amountBuf);
        ListView_SetItemText(hListViewManageExpenses, lvItem.iItem, 5, (LPSTR)(exp.isDeleted ? "Yes" : "No"));
    }

    // Buttons
    if (!hBtnEditExpense) {
        hBtnEditExpense = CreateWindowExA(0, "BUTTON", "Edit Selected", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            50, 280, 120, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_EDIT_EXPENSE, hInst, NULL);
    } else {
        ShowWindow(hBtnEditExpense, SW_SHOW);
    }

    if (!hBtnDeleteExpense) {
        hBtnDeleteExpense = CreateWindowExA(0, "BUTTON", "Delete Selected", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            180, 280, 120, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_DELETE_EXPENSE, hInst, NULL);
    } else {
        ShowWindow(hBtnDeleteExpense, SW_SHOW);
    }

    if (!hBtnRestoreExpense) { // New: Restore button
        hBtnRestoreExpense = CreateWindowExA(0, "BUTTON", "Restore Selected", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            310, 280, 120, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_RESTORE_EXPENSE, hInst, NULL);
    } else {
        ShowWindow(hBtnRestoreExpense, SW_SHOW);
    }

    if (!hBtnManageRecordsBack) {
        hBtnManageRecordsBack = CreateWindowExA(0, "BUTTON", "Back to Menu", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            440, 280, 100, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_MANAGE_RECORDS_BACK, hInst, NULL);
    } else {
        ShowWindow(hBtnManageRecordsBack, SW_SHOW);
    }

    if (!hStaticManageMessage) {
        hStaticManageMessage = CreateWindowExA(0, "STATIC", "", WS_CHILD | WS_VISIBLE | SS_CENTER,
            50, 320, 500, 20, hWnd, (HMENU)(LONG_PTR)IDC_STATIC_MANAGE_MESSAGE, hInst, NULL);
    } else {
        SetWindowTextA(hStaticManageMessage, "");
        ShowWindow(hStaticManageMessage, SW_SHOW);
    }

    currentScreen = SCREEN_MANAGE_RECORDS;
    InvalidateRect(hWnd, NULL, TRUE);
}

// Function to create and show Set Limit Screen controls
void ShowSetLimitScreen(HWND hWnd) {
    HideAllControls();

    char titleBuf[100];
    sprintf(titleBuf, "::    SET EXPENSE LIMIT    ::");

    if (!hTitleStatic) {
        hTitleStatic = CreateWindowExA(0, "STATIC", titleBuf, WS_CHILD | WS_VISIBLE | SS_CENTER,
            100, 50, 400, 30, hWnd, (HMENU)(LONG_PTR)IDC_STATIC_TITLE, hInst, NULL);
    } else {
        SetWindowTextA(hTitleStatic, titleBuf);
        ShowWindow(hTitleStatic, SW_SHOW);
    }

    char currentLimitBuf[100];
    sprintf(currentLimitBuf, "Current Daily Limit: $%d", budgetBuddyApp.GetCurrentUser().dailyExpenseLimit);
    if (!hStaticCurrentLimit) {
        hStaticCurrentLimit = CreateWindowExA(0, "STATIC", currentLimitBuf, WS_CHILD | WS_VISIBLE,
            150, 100, 300, 20, hWnd, (HMENU)(LONG_PTR)IDC_STATIC_CURRENT_LIMIT, hInst, NULL);
    } else {
        SetWindowTextA(hStaticCurrentLimit, currentLimitBuf);
        ShowWindow(hStaticCurrentLimit, SW_SHOW);
    }

    if (!hEditNewLimit) {
        hEditNewLimit = CreateWindowExA(WS_EX_CLIENTEDGE, "EDIT", "", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL | ES_NUMBER,
            200, 130, 200, 25, hWnd, (HMENU)(LONG_PTR)IDC_EDIT_NEW_LIMIT, hInst, NULL);
    } else {
        SetWindowTextA(hEditNewLimit, "");
        ShowWindow(hEditNewLimit, SW_SHOW);
    }

    if (!hBtnSetLimitSubmit) {
        hBtnSetLimitSubmit = CreateWindowExA(0, "BUTTON", "Set Limit", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            200, 180, 100, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_SET_LIMIT_SUBMIT, hInst, NULL);
    } else {
        ShowWindow(hBtnSetLimitSubmit, SW_SHOW);
    }

    if (!hBtnSetLimitCancel) {
        hBtnSetLimitCancel = CreateWindowExA(0, "BUTTON", "Cancel", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            310, 180, 100, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_SET_LIMIT_CANCEL, hInst, NULL);
    } else {
        ShowWindow(hBtnSetLimitCancel, SW_SHOW);
    }

    if (!hStaticSetLimitMessage) {
        hStaticSetLimitMessage = CreateWindowExA(0, "STATIC", "", WS_CHILD | WS_VISIBLE | SS_CENTER,
            100, 220, 400, 20, hWnd, (HMENU)(LONG_PTR)IDC_STATIC_SET_LIMIT_MESSAGE, hInst, NULL);
    } else {
        SetWindowTextA(hStaticSetLimitMessage, "");
        ShowWindow(hStaticSetLimitMessage, SW_SHOW);
    }

    currentScreen = SCREEN_SET_LIMIT;
    InvalidateRect(hWnd, NULL, TRUE);
}

// Function to create and show Redeem Rewards Screen controls
void ShowRedeemRewardsScreen(HWND hWnd) {
    HideAllControls();

    char titleBuf[100];
    sprintf(titleBuf, "::    REDEEM REWARDS     ::");

    if (!hTitleStatic) {
        hTitleStatic = CreateWindowExA(0, "STATIC", titleBuf, WS_CHILD | WS_VISIBLE | SS_CENTER,
            100, 50, 400, 30, hWnd, (HMENU)(LONG_PTR)IDC_STATIC_TITLE, hInst, NULL);
    } else {
        SetWindowTextA(hTitleStatic, titleBuf);
        ShowWindow(hTitleStatic, SW_SHOW);
    }

    char rewardInfoBuf[100];
    sprintf(rewardInfoBuf, "Current Reward Points: %d", budgetBuddyApp.GetCurrentUser().rewardPoints);
    if (!hStaticRewardInfo) {
        hStaticRewardInfo = CreateWindowExA(0, "STATIC", rewardInfoBuf, WS_CHILD | WS_VISIBLE,
            150, 100, 300, 20, hWnd, (HMENU)(LONG_PTR)IDC_STATIC_REWARD_INFO, hInst, NULL);
    } else {
        SetWindowTextA(hStaticRewardInfo, rewardInfoBuf);
        ShowWindow(hStaticRewardInfo, SW_SHOW);
    }

    if (!hEditRedeemPoints) {
        hEditRedeemPoints = CreateWindowExA(WS_EX_CLIENTEDGE, "EDIT", "", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL | ES_NUMBER,
            200, 130, 200, 25, hWnd, (HMENU)(LONG_PTR)IDC_EDIT_REDEEM_POINTS, hInst, NULL);
    } else {
        SetWindowTextA(hEditRedeemPoints, "");
        ShowWindow(hEditRedeemPoints, SW_SHOW);
    }

    if (!hBtnRedeemRewardsSubmit) {
        hBtnRedeemRewardsSubmit = CreateWindowExA(0, "BUTTON", "Redeem Points", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            200, 180, 120, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_REDEEM_REWARDS_SUBMIT, hInst, NULL);
    } else {
        ShowWindow(hBtnRedeemRewardsSubmit, SW_SHOW);
    }

    if (!hBtnRedeemRewardsCancel) {
        hBtnRedeemRewardsCancel = CreateWindowExA(0, "BUTTON", "Cancel", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            330, 180, 100, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_REDEEM_REWARDS_CANCEL, hInst, NULL);
    } else {
        ShowWindow(hBtnRedeemRewardsCancel, SW_SHOW);
    }

    if (!hStaticRedeemMessage) {
        hStaticRedeemMessage = CreateWindowExA(0, "STATIC", "", WS_CHILD | WS_VISIBLE | SS_CENTER,
            100, 220, 400, 20, hWnd, (HMENU)(LONG_PTR)IDC_STATIC_REDEEM_MESSAGE, hInst, NULL);
    } else {
        SetWindowTextA(hStaticRedeemMessage, "");
        ShowWindow(hStaticRedeemMessage, SW_SHOW);
    }

    currentScreen = SCREEN_REDEEM_REWARDS;
    InvalidateRect(hWnd, NULL, TRUE);
}

// Function to show all expenses
void ShowAllExpensesScreen(HWND hWnd) {
    HideAllControls();

    if (!hStaticAllExpensesTitle) {
        hStaticAllExpensesTitle = CreateWindowExA(0, "STATIC", "::    ALL EXPENSES      ::", WS_CHILD | WS_VISIBLE | SS_CENTER,
            100, 30, 400, 30, hWnd, (HMENU)(LONG_PTR)IDC_STATIC_ALL_EXPENSES_TITLE, hInst, NULL);
    } else {
        SetWindowTextA(hStaticAllExpensesTitle, "::    ALL EXPENSES      ::");
        ShowWindow(hStaticAllExpensesTitle, SW_SHOW);
    }

    if (!hEditAllExpensesDisplay) {
        hEditAllExpensesDisplay = CreateWindowExA(WS_EX_CLIENTEDGE, "EDIT", "",
            WS_CHILD | WS_VISIBLE | WS_BORDER | ES_MULTILINE | ES_AUTOVSCROLL | ES_READONLY | WS_VSCROLL,
            50, 70, 500, 250, hWnd, (HMENU)(LONG_PTR)IDC_EDIT_ALL_EXPENSES_DISPLAY, hInst, NULL);
    } else {
        ShowWindow(hEditAllExpensesDisplay, SW_SHOW);
    }

    std::string allExpensesContent = budgetBuddyApp.viewAllExpenses();
    SetWindowTextA(hEditAllExpensesDisplay, allExpensesContent.c_str());


    if (!hBtnAllExpensesBack) {
        hBtnAllExpensesBack = CreateWindowExA(0, "BUTTON", "Back to Admin Menu", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            220, 330, 160, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_ALL_EXPENSES_BACK, hInst, NULL);
    } else {
        ShowWindow(hBtnAllExpensesBack, SW_SHOW);
    }

    currentScreen = SCREEN_ALL_EXPENSES;
    InvalidateRect(hWnd, NULL, TRUE);
}

// New function to show all accounts
void ShowAllAccountsScreen(HWND hWnd) {
    HideAllControls();

    if (!hStaticAllAccountsTitle) {
        hStaticAllAccountsTitle = CreateWindowExA(0, "STATIC", "::    ALL ACCOUNTS      ::", WS_CHILD | WS_VISIBLE | SS_CENTER,
            100, 30, 400, 30, hWnd, (HMENU)(LONG_PTR)IDC_STATIC_ALL_ACCOUNTS_TITLE, hInst, NULL);
    } else {
        SetWindowTextA(hStaticAllAccountsTitle, "::    ALL ACCOUNTS      ::");
        ShowWindow(hStaticAllAccountsTitle, SW_SHOW);
    }

    if (!hEditAllAccountsDisplay) {
        hEditAllAccountsDisplay = CreateWindowExA(WS_EX_CLIENTEDGE, "EDIT", "",
            WS_CHILD | WS_VISIBLE | WS_BORDER | ES_MULTILINE | ES_AUTOVSCROLL | ES_READONLY | WS_VSCROLL,
            50, 70, 500, 250, hWnd, (HMENU)(LONG_PTR)IDC_EDIT_ALL_ACCOUNTS_DISPLAY, hInst, NULL);
    } else {
        ShowWindow(hEditAllAccountsDisplay, SW_SHOW);
    }

    std::string allAccountsContent = budgetBuddyApp.viewAllAccounts();
    SetWindowTextA(hEditAllAccountsDisplay, allAccountsContent.c_str());


    if (!hBtnAllAccountsBack) {
        hBtnAllAccountsBack = CreateWindowExA(0, "BUTTON", "Back to Admin Menu", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            220, 330, 160, 30, hWnd, (HMENU)(LONG_PTR)IDC_BTN_ALL_ACCOUNTS_BACK, hInst, NULL);
    } else {
        ShowWindow(hBtnAllAccountsBack, SW_SHOW);
    }

    currentScreen = SCREEN_ALL_ACCOUNTS;
    InvalidateRect(hWnd, NULL, TRUE);
}


// --- Window Procedure ---
LRESULT CALLBACK WndProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam) {
    switch (message) {
        case WM_CREATE: {
            hWndMain = hWnd;
            hbrBackground = CreateSolidBrush(crBaseColor);
            ShowMainMenuScreen(hWnd);
            break;
        }

        case WM_COMMAND: {
            int wmId = LOWORD(wParam);
            switch (wmId) {
                case IDC_BTN_REGISTER:
                    ShowRegisterScreen(hWnd);
                    break;
                case IDC_BTN_LOGIN:
                    ShowLoginScreen(hWnd);
                    break;
                case IDC_BTN_EXIT:
                    DestroyWindow(hWnd);
                    break;

                case IDC_BTN_SUBMIT: {
                    char usernameBuf[256];
                    char passwordBuf[256];
                    char messageBuf[512];

                    GetWindowTextA(hUsernameEdit, usernameBuf, sizeof(usernameBuf));
                    GetWindowTextA(hPasswordEdit, passwordBuf, sizeof(passwordBuf));

                    if (strlen(usernameBuf) == 0 || strlen(passwordBuf) == 0) {
                        SetWindowTextA(hMessageStatic, "Username and password cannot be empty.");
                        break;
                    }

                    std::string username = usernameBuf;
                    std::string password = passwordBuf;
                    std::string messageText;

                    if (currentScreen == SCREEN_REGISTER) {
                        if (budgetBuddyApp.registerUser(username, password, messageText)) {
                            sprintf(messageBuf, "Registration successful! Loading...");
                            SetWindowTextA(hMessageStatic, messageBuf);
                            Sleep(1000);
                            if (budgetBuddyApp.GetCurrentUser().isAdmin) {
                                ShowAdminMenuScreen(hWnd);
                            } else {
                                ShowUserMenuScreen(hWnd);
                            }
                        } else {
                            sprintf(messageBuf, "%s", messageText.c_str());
                            SetWindowTextA(hMessageStatic, messageBuf);
                        }
                    } else if (currentScreen == SCREEN_LOGIN) {
                        static int loginAttempts = 0;
                        if (budgetBuddyApp.loginUser(username, password, messageText)) {
                            sprintf(messageBuf, "Login successful! Loading...");
                            SetWindowTextA(hMessageStatic, messageBuf);
                            loginAttempts = 0;
                            Sleep(1000);
                            if (budgetBuddyApp.GetCurrentUser().isAdmin) {
                                ShowAdminMenuScreen(hWnd);
                            } else {
                                ShowUserMenuScreen(hWnd);
                            }
                        } else {
                            loginAttempts++;
                            sprintf(messageBuf, "%s Attempts left: %d", messageText.c_str(), MAX_LOGIN_ATTEMPTS - loginAttempts);
                            SetWindowTextA(hMessageStatic, messageBuf);

                            if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
                                SetWindowTextA(hMessageStatic, "Maximum attempts reached. Returning to Login.");
                                MessageBoxA(hWnd, "Maximum login attempts reached. Returning to main menu.", "Login Failed", MB_OK | MB_ICONWARNING);
                                ShowMainMenuScreen(hWnd);
                                loginAttempts = 0;
                            }
                        }
                    }
                    break;
                }
                case IDC_BTN_CANCEL:
                    ShowMainMenuScreen(hWnd);
                    break;

                case IDC_BTN_ADD_EXPENSE:
                    ShowAddExpenseScreen(hWnd);
                    break;
                case IDC_BTN_VIEW_BALANCE:
                    ShowViewBalanceScreen(hWnd);
                    break;
                case IDC_BTN_MANAGE_RECORDS:
                    ShowManageRecordsScreen(hWnd);
                    break;
                case IDC_BTN_SET_LIMIT:
                    ShowSetLimitScreen(hWnd);
                    break;
                case IDC_BTN_REDEEM_REWARDS:
                    ShowRedeemRewardsScreen(hWnd);
                    break;
                case IDC_BTN_USER_LOGOUT:
                    budgetBuddyApp.ClearCurrentUser();
                    ShowMainMenuScreen(hWnd);
                    break;

                case IDC_BTN_ADD_EXPENSE_SUBMIT: {
                    char nameBuf[256];
                    char amountBuf[64];
                    char categoryBuf[64];
                    char messageBuf[512];

                    GetWindowTextA(hEditExpenseName, nameBuf, sizeof(nameBuf));
                    GetWindowTextA(hEditExpenseAmount, amountBuf, sizeof(amountBuf));

                    int selectedCategoryIndex = SendMessageA(hComboExpenseCategory, CB_GETCURSEL, (WPARAM)0, (LPARAM)0);
                    if (selectedCategoryIndex != CB_ERR) {
                        SendMessageA(hComboExpenseCategory, CB_GETLBTEXT, (WPARAM)selectedCategoryIndex, (LPARAM)categoryBuf);
                    } else {
                        sprintf(messageBuf, "Please select an expense category.");
                        SetWindowTextA(hMessageStatic, messageBuf);
                        break;
                    }

                    if (strlen(nameBuf) == 0 || strlen(amountBuf) == 0) {
                        SetWindowTextA(hMessageStatic, "Expense name and amount cannot be empty.");
                        break;
                    }

                    double amount = 0.0;
                    try {
                        amount = std::stod(amountBuf);
                    } catch (const std::invalid_argument& e) {
                        SetWindowTextA(hMessageStatic, "Invalid amount format. Please enter a number.");
                        break;
                    } catch (const std::out_of_range& e) {
                        SetWindowTextA(hMessageStatic, "Amount too large or too small.");
                        break;
                    }

                    std::string expenseName = nameBuf;
                    std::string expenseCategory = categoryBuf;
                    std::string messageText;

                    if (budgetBuddyApp.addExpense(expenseName, expenseCategory, amount, messageText)) {
                        sprintf(messageBuf, "%s", messageText.c_str());
                        SetWindowTextA(hMessageStatic, messageBuf);
                        budgetBuddyApp.generateReceipt(budgetBuddyApp.GetExpenses().back());
                        SetWindowTextA(hEditExpenseName, "");
                        SetWindowTextA(hEditExpenseAmount, "");
                        SendMessageA(hComboExpenseCategory, CB_SETCURSEL, (WPARAM)0, (LPARAM)0);
                    } else {
                        sprintf(messageBuf, "%s", messageText.c_str());
                        SetWindowTextA(hMessageStatic, messageBuf);
                    }
                    break;
                }
                case IDC_BTN_ADD_EXPENSE_CANCEL:
                    ShowUserMenuScreen(hWnd);
                    break;

                case IDC_BTN_VIEW_BALANCE_BACK:
                    ShowUserMenuScreen(hWnd);
                    break;

                case IDC_BTN_EDIT_EXPENSE: {
                    int selectedIndex = ListView_GetNextItem(hListViewManageExpenses, -1, LVNI_SELECTED);
                    if (selectedIndex == -1) {
                        SetWindowTextA(hStaticManageMessage, "Please select an expense to edit.");
                        break;
                    }

                    const auto& expenses = budgetBuddyApp.GetExpenses();
                    if (selectedIndex >= expenses.size()) {
                        SetWindowTextA(hStaticManageMessage, "Error: Invalid expense selected.");
                        break;
                    }

                    const auto& selectedExpense = expenses[selectedIndex];

                    char newNameBuf[256];
                    char newCategoryBuf[64];
                    char newAmountBuf[64];
                    char messageBuf[512];
                    std::string messageText;

                    strcpy(newNameBuf, selectedExpense.name.c_str());
                    strcpy(newCategoryBuf, selectedExpense.category.c_str());
                    sprintf(newAmountBuf, "%.2f", selectedExpense.amount);

                    char confirmEditMsg[1024];
                    sprintf(confirmEditMsg,
                            "Confirm Edit:\n"
                            "Old Name: %s -> New Name: %s\n"
                            "Old Category: %s -> New Category: %s\n"
                            "Old Amount: %.2f -> New Amount: %s\n\n"
                            "Proceed with edit?",
                            selectedExpense.name.c_str(), newNameBuf,
                            selectedExpense.category.c_str(), newCategoryBuf,
                            selectedExpense.amount, newAmountBuf);

                    if (MessageBoxA(hWnd, confirmEditMsg, "Confirm Expense Edit", MB_YESNO | MB_ICONQUESTION) == IDNO) {
                        SetWindowTextA(hStaticManageMessage, "Edit cancelled.");
                        break;
                    }

                    double newAmount = 0.0;
                    try {
                        newAmount = std::stod(newAmountBuf);
                    } catch (const std::invalid_argument& e) {
                        SetWindowTextA(hStaticManageMessage, "Invalid amount format for edit. Please enter a number.");
                        break;
                    } catch (const std::out_of_range& e) {
                        SetWindowTextA(hStaticManageMessage, "Amount for edit too large or too small.");
                        break;
                    }

                    if (budgetBuddyApp.editExpense(selectedIndex, newNameBuf, newCategoryBuf, newAmount, messageText)) {
                        sprintf(messageBuf, "%s", messageText.c_str());
                        SetWindowTextA(hStaticManageMessage, messageBuf);
                        ShowManageRecordsScreen(hWnd);
                    } else {
                        sprintf(messageBuf, "%s", messageText.c_str());
                        SetWindowTextA(hStaticManageMessage, messageBuf);
                    }
                    break;
                }
                case IDC_BTN_DELETE_EXPENSE: {
                    int selectedIndex = ListView_GetNextItem(hListViewManageExpenses, -1, LVNI_SELECTED);
                    if (selectedIndex == -1) {
                        SetWindowTextA(hStaticManageMessage, "Please select an expense to delete.");
                        break;
                    }
                    char messageBuf[512];
                    std::string messageText;

                    if (MessageBoxA(hWnd, "Are you sure you want to delete this expense? (Amount will be refunded)", "Confirm Delete", MB_YESNO | MB_ICONQUESTION) == IDNO) {
                        SetWindowTextA(hStaticManageMessage, "Deletion cancelled.");
                        break;
                    }

                    if (budgetBuddyApp.deleteExpense(selectedIndex, messageText)) {
                        sprintf(messageBuf, "%s", messageText.c_str());
                        SetWindowTextA(hStaticManageMessage, messageBuf);
                        ShowManageRecordsScreen(hWnd);
                    } else {
                        sprintf(messageBuf, "%s", messageText.c_str());
                        SetWindowTextA(hStaticManageMessage, messageBuf);
                    }
                    break;
                }
                case IDC_BTN_RESTORE_EXPENSE: { // New: Restore expense logic
                    int selectedIndex = ListView_GetNextItem(hListViewManageExpenses, -1, LVNI_SELECTED);
                    if (selectedIndex == -1) {
                        SetWindowTextA(hStaticManageMessage, "Please select an expense to restore.");
                        break;
                    }
                    char messageBuf[512];
                    std::string messageText;

                    const auto& expenses = budgetBuddyApp.GetExpenses();
                    if (selectedIndex >= expenses.size() || !expenses[selectedIndex].isDeleted) {
                        SetWindowTextA(hStaticManageMessage, "Selected expense is not deleted or invalid.");
                        break;
                    }

                    if (MessageBoxA(hWnd, "Are you sure you want to restore this expense? (Amount will be deducted from balance)", "Confirm Restore", MB_YESNO | MB_ICONQUESTION) == IDNO) {
                        SetWindowTextA(hStaticManageMessage, "Restoration cancelled.");
                        break;
                    }

                    if (budgetBuddyApp.restoreExpense(selectedIndex, messageText)) {
                        sprintf(messageBuf, "%s", messageText.c_str());
                        SetWindowTextA(hStaticManageMessage, messageBuf);
                        ShowManageRecordsScreen(hWnd);
                    } else {
                        sprintf(messageBuf, "%s", messageText.c_str());
                        SetWindowTextA(hStaticManageMessage, messageBuf);
                    }
                    break;
                }
                case IDC_BTN_MANAGE_RECORDS_BACK:
                    ShowUserMenuScreen(hWnd);
                    break;

                case IDC_BTN_SET_LIMIT_SUBMIT: {
                    char limitBuf[64];
                    char messageBuf[512];
                    GetWindowTextA(hEditNewLimit, limitBuf, sizeof(limitBuf));

                    if (strlen(limitBuf) == 0) {
                        SetWindowTextA(hStaticSetLimitMessage, "Please enter a limit.");
                        break;
                    }

                    int newLimit = 0;
                    try {
                        newLimit = std::stoi(limitBuf);
                    } catch (const std::invalid_argument& e) {
                        SetWindowTextA(hStaticSetLimitMessage, "Invalid limit format. Please enter an integer.");
                        break;
                    } catch (const std::out_of_range& e) {
                        SetWindowTextA(hStaticSetLimitMessage, "Limit value too large or too small.");
                        break;
                    }

                    std::string messageText;
                    if (budgetBuddyApp.setDailyExpenseLimit(newLimit, messageText)) {
                        sprintf(messageBuf, "%s", messageText.c_str());
                        SetWindowTextA(hStaticSetLimitMessage, messageBuf);
                        char currentLimitBuf[100];
                        sprintf(currentLimitBuf, "Current Daily Limit: $%d", budgetBuddyApp.GetCurrentUser().dailyExpenseLimit);
                        SetWindowTextA(hStaticCurrentLimit, currentLimitBuf);
                        SetWindowTextA(hEditNewLimit, "");
                    } else {
                        sprintf(messageBuf, "%s", messageText.c_str());
                        SetWindowTextA(hStaticSetLimitMessage, messageBuf);
                    }
                    break;
                }
                case IDC_BTN_SET_LIMIT_CANCEL:
                    ShowUserMenuScreen(hWnd);
                    break;

                case IDC_BTN_REDEEM_REWARDS_SUBMIT: {
                    char pointsBuf[64];
                    char messageBuf[512];
                    GetWindowTextA(hEditRedeemPoints, pointsBuf, sizeof(pointsBuf));

                    if (strlen(pointsBuf) == 0) {
                        SetWindowTextA(hStaticRedeemMessage, "Please enter points to redeem.");
                        break;
                    }

                    int pointsToRedeem = 0;
                    try {
                        pointsToRedeem = std::stoi(pointsBuf);
                    } catch (const std::invalid_argument& e) {
                        SetWindowTextA(hStaticRedeemMessage, "Invalid points format. Please enter an integer.");
                        break;
                    } catch (const std::out_of_range& e) {
                        SetWindowTextA(hStaticRedeemMessage, "Points value too large or too small.");
                        break;
                    }

                    std::string messageText;
                    if (budgetBuddyApp.redeemRewardPoints(pointsToRedeem, messageText)) {
                        sprintf(messageBuf, "%s", messageText.c_str());
                        SetWindowTextA(hStaticRedeemMessage, messageBuf);
                        char rewardInfoBuf[100];
                        sprintf(rewardInfoBuf, "Current Reward Points: %d", budgetBuddyApp.GetCurrentUser().rewardPoints);
                        SetWindowTextA(hStaticRewardInfo, rewardInfoBuf);
                        SetWindowTextA(hEditRedeemPoints, "");
                    } else {
                        sprintf(messageBuf, "%s", messageText.c_str());
                        SetWindowTextA(hStaticRedeemMessage, messageBuf);
                    }
                    break;
                }
                case IDC_BTN_REDEEM_REWARDS_CANCEL:
                    ShowUserMenuScreen(hWnd);
                    break;

                case IDC_BTN_VIEW_ALL_ACCOUNTS: // Admin function
                    ShowAllAccountsScreen(hWnd);
                    break;

                case IDC_BTN_VIEW_ALL_EXPENSES: // Admin function
                    ShowAllExpensesScreen(hWnd);
                    break;

                case IDC_BTN_ALL_EXPENSES_BACK: // Back button for all expenses screen
                    ShowAdminMenuScreen(hWnd);
                    break;

                case IDC_BTN_ALL_ACCOUNTS_BACK: // Back button for all accounts screen
                    ShowAdminMenuScreen(hWnd);
                    break;

                default:
                    break;
            }
            break;
        }

        case WM_CTLCOLORSTATIC: {
            HDC hdcStatic = (HDC)wParam;
            SetTextColor(hdcStatic, crFontColor);
            SetBkColor(hdcStatic, crBaseColor);
            return (LRESULT)hbrBackground;
        }

        case WM_CTLCOLOREDIT: {
            HDC hdcEdit = (HDC)wParam;
            SetTextColor(hdcEdit, crFontColor);
            SetBkColor(hdcEdit, crBaseColor);
            return (LRESULT)hbrBackground;
        }

        case WM_CTLCOLORBTN: {
            HDC hdcButton = (HDC)wParam;
            SetTextColor(hdcButton, crFontColor);
            SetBkColor(hdcButton, crBaseColor);
            return (LRESULT)hbrBackground;
        }

        case WM_PAINT: {
            PAINTSTRUCT ps;
            HDC hdc = BeginPaint(hWnd, &ps);
            FillRect(hdc, &ps.rcPaint, hbrBackground);
            EndPaint(hWnd, &ps);
            break;
        }

        case WM_DESTROY: {
            DeleteObject(hbrBackground);
            PostQuitMessage(0);
            break;
        }

        default:
            return DefWindowProcA(hWnd, message, wParam, lParam);
    }
    return 0;
}

// --- WinMain Entry Point ---
int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nCmdShow) {
    hInst = hInstance;

    INITCOMMONCONTROLSEX icc;
    icc.dwSize = sizeof(icc);
    icc.dwICC = ICC_STANDARD_CLASSES | ICC_LISTVIEW_CLASSES | ICC_BAR_CLASSES;
    InitCommonControlsEx(&icc);

    WNDCLASSEX wc = {0};
    wc.cbSize        = sizeof(WNDCLASSEX);
    wc.lpfnWndProc   = WndProc;
    wc.hInstance     = hInstance;
    wc.hCursor       = LoadCursorA(NULL, IDC_ARROW);
    wc.hbrBackground = (HBRUSH)(COLOR_WINDOW + 1);
    wc.lpszClassName = "BudgetBuddyClass";
    wc.hIcon         = LoadIconA(NULL, IDI_APPLICATION);

    if (!RegisterClassExA(&wc)) {
        MessageBoxA(NULL, "Window Registration Failed!", "Error", MB_ICONERROR | MB_OK);
        return 0;
    }

    hWndMain = CreateWindowExA(
        WS_EX_APPWINDOW | WS_EX_WINDOWEDGE,
        "BudgetBuddyClass",
        "Budget Buddy",
        WS_OVERLAPPEDWINDOW | WS_CLIPCHILDREN,
        CW_USEDEFAULT, CW_USEDEFAULT,
        600, 400,
        NULL,
        NULL,
        hInstance,
        NULL
    );

    if (!hWndMain) {
        MessageBoxA(NULL, "Window Creation Failed!", "Error", MB_ICONERROR | MB_OK);
        return 0;
    }

    ShowWindow(hWndMain, nCmdShow);
    UpdateWindow(hWndMain);

    MSG msg;
    while (GetMessageA(&msg, NULL, 0, 0)) {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    }

    return (int)msg.wParam;
}
