#include <windows.h>
#include <cstdio>
#include <string>
#include <vector>
#include <fstream>
#include <sstream> // For std::istringstream
#include <iomanip> // For std::fixed, std::setprecision

// --- Global Defines for Control IDs ---
// Login Screen
#define ID_USERNAME_EDIT    101
#define ID_PASSWORD_EDIT    102
#define ID_LOGIN_BUTTON     103
#define ID_SIGNUP_BUTTON    104
#define ID_STATUS_STATIC    105
#define ID_USERNAME_LABEL   106
#define ID_PASSWORD_LABEL   107
#define ID_WELCOME_STATIC   108

// Main Dashboard
#define ID_DASH_EXPENSE_BTN 201
#define ID_DASH_INCOME_BTN  202
#define ID_DASH_HISTORY_BTN 203
#define ID_DASH_ACCOUNT_BTN 204
#define ID_DASH_EXIT_BTN    205

// Expense Module
#define ID_EXP_DESC_LABEL   301
#define ID_EXP_DESC_EDIT    302
#define ID_EXP_AMOUNT_LABEL 303
#define ID_EXP_AMOUNT_EDIT  304
#define ID_EXP_DATE_LABEL   305
#define ID_EXP_DATE_EDIT    306
#define ID_EXP_ADD_BTN      307
#define ID_EXP_UPDATE_BTN   308
#define ID_EXP_SAVE_BTN     309
#define ID_EXP_SEARCH_EDIT  310
#define ID_EXP_SEARCH_BTN   311
#define ID_EXP_LISTBOX      312
#define ID_EXP_NEXT_BTN     313
#define ID_EXP_PREV_BTN     314
#define ID_EXP_BACK_TO_MAIN 315
#define ID_EXP_STATUS_STATIC 316 // For expense module specific messages


// --- Constants ---
#define MAX_LOGIN_ATTEMPTS 3
#define APP_WINDOW_WIDTH   600
#define APP_WINDOW_HEIGHT  500

// --- Global Variables ---
HWND g_hwnd; // Main window handle
HFONT g_hFont; // Custom font handle
HBRUSH g_hBlackBrush; // Black brush for background

// Handles for all controls (will be managed by screen)
// Login Screen Handles
HWND hLoginUsernameEdit, hLoginPasswordEdit, hLoginButton, hLoginSignUpButton, hLoginStatusStatic;
HWND hLoginUsernameLabel, hLoginPasswordLabel, hLoginWelcomeStatic;

// Dashboard Handles
HWND hDashExpenseBtn, hDashIncomeBtn, hDashHistoryBtn, hDashAccountBtn, hDashExitBtn;

// Expense Module Handles
HWND hExpDescLabel, hExpDescEdit, hExpAmountLabel, hExpAmountEdit, hExpDateLabel, hExpDateEdit;
HWND hExpAddBtn, hExpUpdateBtn, hExpSaveBtn, hExpSearchEdit, hExpSearchBtn, hExpListBox;
HWND hExpNextBtn, hExpPrevBtn, hExpBackToMainBtn, hExpStatusStatic;

int g_loginAttempts = 0; // Counter for failed login attempts
int g_currentExpenseIndex = -1; // Current record index in expense module

// --- Data Structures ---
struct Expense {
    std::string description;
    double amount;
    std::string date; // Stored as YYYY-MM-DD string

    std::string toCsvString() const {
        std::stringstream ss;
        ss << "\"" << description << "\"," << std::fixed << std::setprecision(2) << amount << ",\"" << date << "\"";
        return ss.str();
    }
};

std::vector<Expense> g_expenses; // In-memory storage for expenses

// --- Screen Management Enum ---
enum AppScreen {
    SCREEN_LOGIN,
    SCREEN_DASHBOARD,
    SCREEN_EXPENSE_MODULE
    // Add more modules as needed
};

AppScreen g_currentScreen = SCREEN_LOGIN; // Initial screen

// --- Function Prototypes ---
LRESULT CALLBACK WindowProcedure(HWND hwnd, UINT message, WPARAM wParam, LPARAM lParam);
void ShowScreen(AppScreen screen);
void CreateLoginControls(HWND hwnd);
void DestroyLoginControls();
void CreateDashboardControls(HWND hwnd);
void DestroyDashboardControls();
void CreateExpenseModuleControls(HWND hwnd);
void DestroyExpenseModuleControls();

// CSV Utility Functions
bool checkCredentials(const std::string& username, const std::string& password);
bool saveCredentials(const std::string& username, const std::string& password);
void loadExpensesFromCsv();
void saveExpensesToCsv();
void displayExpenseRecord(int index);
void updateExpenseListBox();
void searchExpenses(const std::string& query);


// --- Main Entry Point ---
int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nCmdShow)
{
    const char g_szClassName[] = "BudgetBuddyApp";

    WNDCLASSEX wc;
    MSG Msg;

    // Step 1: Registering the Window Class
    wc.cbSize        = sizeof(WNDCLASSEX);
    wc.style         = 0;
    wc.lpfnWndProc   = WindowProcedure;
    wc.cbClsExtra    = 0;
    wc.cbWndExtra    = 0;
    wc.hInstance     = hInstance;
    wc.hIcon         = LoadIcon(NULL, IDI_APPLICATION);
    wc.hCursor       = LoadCursor(NULL, IDC_ARROW);
    wc.hbrBackground = (HBRUSH)(COLOR_WINDOW+1); // Will be overridden by WM_ERASEBKGND
    wc.lpszMenuName  = NULL;
    wc.lpszClassName = g_szClassName;
    wc.hIconSm       = LoadIcon(NULL, IDI_APPLICATION);

    if(!RegisterClassEx(&wc))
    {
        MessageBox(NULL, "Window Registration Failed!", "Error!", MB_ICONEXCLAMATION | MB_OK);
        return 0;
    }

    // Create custom font (HK Modular is not standard, using a monospace font as a fallback)
    g_hFont = CreateFont(
        18,                         // Height
        0,                          // Width (0 for default aspect ratio)
        0,                          // Escapement
        0,                          // Orientation
        FW_NORMAL,                  // Weight
        FALSE,                      // Italic
        FALSE,                      // Underline
        FALSE,                      // Strikeout
        DEFAULT_CHARSET,            // Character set
        OUT_OUTLINE_PRECIS,         // Output precision
        CLIP_DEFAULT_PRECIS,        // Clipping precision
        CLEARTYPE_QUALITY,          // Quality (ANTIALIASED_QUALITY or CLEARTYPE_QUALITY)
        FIXED_PITCH | FF_MODERN,    // Pitch and Family (Fixed pitch for monospace, modern for generic)
        "Consolas"                  // Font Name (or "Courier New", "Lucida Console")
    );
    // If you have "HK Modular" .ttf file installed, you can try:
    // g_hFont = CreateFont(... "HK Modular");

    g_hBlackBrush = CreateSolidBrush(RGB(0, 0, 0)); // Black brush for background

    // Step 2: Creating the Window
    g_hwnd = CreateWindowEx(
        WS_EX_CLIENTEDGE,
        g_szClassName,
        "Budget Buddy",
        WS_OVERLAPPEDWINDOW | WS_CLIPCHILDREN, // WS_CLIPCHILDREN to prevent flickering
        CW_USEDEFAULT, CW_USEDEFAULT, APP_WINDOW_WIDTH, APP_WINDOW_HEIGHT,
        NULL, NULL, hInstance, NULL);

    if(g_hwnd == NULL)
    {
        MessageBox(NULL, "Window Creation Failed!", "Error!", MB_ICONEXCLAMATION | MB_OK);
        return 0;
    }

    ShowWindow(g_hwnd, nCmdShow);
    UpdateWindow(g_hwnd);

    // Initialize with the login screen
    ShowScreen(SCREEN_LOGIN);

    // Step 3: The Message Loop
    while(GetMessage(&Msg, NULL, 0, 0) > 0)
    {
        TranslateMessage(&Msg);
        DispatchMessage(&Msg);
    }

    // Cleanup
    DeleteObject(g_hFont);
    DeleteObject(g_hBlackBrush);
    return Msg.wParam;
}

// --- Window Procedure ---
LRESULT CALLBACK WindowProcedure(HWND hwnd, UINT message, WPARAM wParam, LPARAM lParam)
{
    switch(message)
    {
        case WM_CTLCOLORSTATIC: // For static text controls
        case WM_CTLCOLOREDIT:   // For edit controls
        {
            HDC hdcStatic = (HDC)wParam;
            SetTextColor(hdcStatic, RGB(0, 255, 33)); // Bright Green
            SetBkMode(hdcStatic, TRANSPARENT); // Make background transparent
            return (LRESULT)g_hBlackBrush; // Return black brush for background
        }

        case WM_ERASEBKGND: // Custom background drawing
        {
            RECT rc;
            GetClientRect(hwnd, &rc);
            FillRect((HDC)wParam, &rc, g_hBlackBrush);
            return 1; // Indicate that background has been erased
        }

        case WM_CREATE:
            // Controls will be created by ShowScreen based on initial state
            break;

        case WM_COMMAND:
            {
                // Common variables for input
                char usernameBuf[50], passwordBuf[50], tempBuf[256];
                std::string username, password, tempStr;

                switch (LOWORD(wParam))
                {
                    // --- Login Screen Commands ---
                    case ID_LOGIN_BUTTON:
                        GetWindowText(hLoginUsernameEdit, usernameBuf, 50);
                        GetWindowText(hLoginPasswordEdit, passwordBuf, 50);
                        username = usernameBuf;
                        password = passwordBuf;

                        if (checkCredentials(username, password)) {
                            g_loginAttempts = 0; // Reset attempts on success
                            SetWindowText(hLoginStatusStatic, ""); // Clear status
                            char welcomeMsg[100];
                            sprintf(welcomeMsg, "Welcome, %s!", username.c_str());
                            SetWindowText(hLoginWelcomeStatic, welcomeMsg);

                            // Hide login controls, show welcome, then transition to dashboard
                            ShowWindow(hLoginUsernameEdit, SW_HIDE);
                            ShowWindow(hLoginPasswordEdit, SW_HIDE);
                            ShowWindow(hLoginButton, SW_HIDE);
                            ShowWindow(hLoginSignUpButton, SW_HIDE);
                            ShowWindow(hLoginUsernameLabel, SW_HIDE);
                            ShowWindow(hLoginPasswordLabel, SW_HIDE);
                            ShowWindow(hLoginStatusStatic, SW_HIDE);
                            ShowWindow(hLoginWelcomeStatic, SW_SHOW);

                            // Transition to dashboard after a short delay (simulated)
                            // In a real app, you might use a timer or separate thread for this
                            // For simplicity, we'll just transition directly.
                            ShowScreen(SCREEN_DASHBOARD);

                        } else {
                            g_loginAttempts++; // Increment failed attempt counter
                            char statusMsg[100];
                            sprintf(statusMsg, "Invalid Username or Password. Attempts left: %d", MAX_LOGIN_ATTEMPTS - g_loginAttempts);
                            SetWindowText(hLoginStatusStatic, statusMsg);

                            if (g_loginAttempts >= MAX_LOGIN_ATTEMPTS) {
                                SetWindowText(hLoginStatusStatic, "Maximum attempts reached. Terminating.");
                                // Give a moment for the message to show, then quit
                                SetTimer(hwnd, 1, 1500, NULL); // Timer ID 1, 1.5 seconds
                            }
                        }
                        break;

                    case ID_SIGNUP_BUTTON:
                        GetWindowText(hLoginUsernameEdit, usernameBuf, 50);
                        GetWindowText(hLoginPasswordEdit, passwordBuf, 50);
                        username = usernameBuf;
                        password = passwordBuf;

                        if (username.empty() || password.empty()) {
                            SetWindowText(hLoginStatusStatic, "Username and Password cannot be empty.");
                        } else if (saveCredentials(username, password)) {
                            SetWindowText(hLoginStatusStatic, "Account created successfully!");
                            SetWindowText(hLoginUsernameEdit, "");
                            SetWindowText(hLoginPasswordEdit, "");
                            g_loginAttempts = 0; // Reset attempts after successful sign-up
                        } else {
                            SetWindowText(hLoginStatusStatic, "Username already exists or file error.");
                        }
                        break;

                    // --- Dashboard Commands ---
                    case ID_DASH_EXPENSE_BTN:
                        ShowScreen(SCREEN_EXPENSE_MODULE);
                        break;
                    case ID_DASH_INCOME_BTN:
                        SetWindowText(hLoginStatusStatic, "Income Module (Not Implemented Yet)"); // Use login status for temp msg
                        break;
                    case ID_DASH_HISTORY_BTN:
                        SetWindowText(hLoginStatusStatic, "History Module (Not Implemented Yet)");
                        break;
                    case ID_DASH_ACCOUNT_BTN:
                        SetWindowText(hLoginStatusStatic, "Account Module (Not Implemented Yet)");
                        break;
                    case ID_DASH_EXIT_BTN:
                        PostQuitMessage(0);
                        break;

                    // --- Expense Module Commands ---
                    case ID_EXP_ADD_BTN:
                        {
                            GetWindowText(hExpDescEdit, tempBuf, sizeof(tempBuf));
                            std::string desc = tempBuf;
                            GetWindowText(hExpAmountEdit, tempBuf, sizeof(tempBuf));
                            std::string amountStr = tempBuf;
                            GetWindowText(hExpDateEdit, tempBuf, sizeof(tempBuf));
                            std::string dateStr = tempBuf;

                            if (desc.empty() || amountStr.empty() || dateStr.empty()) {
                                SetWindowText(hExpStatusStatic, "All fields must be filled.");
                                break;
                            }

                            double amountVal;
                            try {
                                amountVal = std::stod(amountStr);
                            } catch (const std::invalid_argument& e) {
                                SetWindowText(hExpStatusStatic, "Invalid amount. Enter a number.");
                                break;
                            } catch (const std::out_of_range& e) {
                                SetWindowText(hExpStatusStatic, "Amount too large.");
                                break;
                            }

                            // Basic date format validation (YYYY-MM-DD)
                            if (dateStr.length() != 10 || dateStr[4] != '-' || dateStr[7] != '-') {
                                SetWindowText(hExpStatusStatic, "Date format: YYYY-MM-DD");
                                break;
                            }

                            Expense newExp;
                            newExp.description = desc;
                            newExp.amount = amountVal;
                            newExp.date = dateStr;

                            g_expenses.push_back(newExp);
                            SetWindowText(hExpStatusStatic, "Expense added.");
                            SetWindowText(hExpDescEdit, "");
                            SetWindowText(hExpAmountEdit, "");
                            SetWindowText(hExpDateEdit, "");
                            updateExpenseListBox();
                            g_currentExpenseIndex = g_expenses.size() - 1; // Select the newly added item
                            displayExpenseRecord(g_currentExpenseIndex);
                        }
                        break;

                    case ID_EXP_UPDATE_BTN:
                        if (g_currentExpenseIndex != -1 && g_currentExpenseIndex < g_expenses.size()) {
                            GetWindowText(hExpDescEdit, tempBuf, sizeof(tempBuf));
                            std::string desc = tempBuf;
                            GetWindowText(hExpAmountEdit, tempBuf, sizeof(tempBuf));
                            std::string amountStr = tempBuf;
                            GetWindowText(hExpDateEdit, tempBuf, sizeof(tempBuf));
                            std::string dateStr = tempBuf;

                            if (desc.empty() || amountStr.empty() || dateStr.empty()) {
                                SetWindowText(hExpStatusStatic, "All fields must be filled to update.");
                                break;
                            }

                            double amountVal;
                            try {
                                amountVal = std::stod(amountStr);
                            } catch (const std::invalid_argument& e) {
                                SetWindowText(hExpStatusStatic, "Invalid amount. Enter a number.");
                                break;
                            } catch (const std::out_of_range& e) {
                                SetWindowText(hExpStatusStatic, "Amount too large.");
                                break;
                            }

                            if (dateStr.length() != 10 || dateStr[4] != '-' || dateStr[7] != '-') {
                                SetWindowText(hExpStatusStatic, "Date format: YYYY-MM-DD");
                                break;
                            }

                            g_expenses[g_currentExpenseIndex].description = desc;
                            g_expenses[g_currentExpenseIndex].amount = amountVal;
                            g_expenses[g_currentExpenseIndex].date = dateStr;
                            SetWindowText(hExpStatusStatic, "Expense updated.");
                            updateExpenseListBox();
                            // Re-select the updated item in the listbox
                            SendMessage(hExpListBox, LB_SETCURSEL, g_currentExpenseIndex, 0);
                        } else {
                            SetWindowText(hExpStatusStatic, "No expense selected to update.");
                        }
                        break;

                    case ID_EXP_SAVE_BTN:
                        saveExpensesToCsv();
                        SetWindowText(hExpStatusStatic, "Expenses saved to expenses.csv");
                        break;

                    case ID_EXP_SEARCH_BTN:
                        GetWindowText(hExpSearchEdit, tempBuf, sizeof(tempBuf));
                        searchExpenses(tempBuf);
                        break;

                    case ID_EXP_NEXT_BTN:
                        if (!g_expenses.empty()) {
                            g_currentExpenseIndex = (g_currentExpenseIndex + 1) % g_expenses.size();
                            displayExpenseRecord(g_currentExpenseIndex);
                            SendMessage(hExpListBox, LB_SETCURSEL, g_currentExpenseIndex, 0);
                        } else {
                            SetWindowText(hExpStatusStatic, "No expenses to navigate.");
                        }
                        break;

                    case ID_EXP_PREV_BTN:
                        if (!g_expenses.empty()) {
                            g_currentExpenseIndex = (g_currentExpenseIndex - 1 + g_expenses.size()) % g_expenses.size();
                            displayExpenseRecord(g_currentExpenseIndex);
                            SendMessage(hExpListBox, LB_SETCURSEL, g_currentExpenseIndex, 0);
                        } else {
                            SetWindowText(hExpStatusStatic, "No expenses to navigate.");
                        }
                        break;

                    case ID_EXP_BACK_TO_MAIN:
                        ShowScreen(SCREEN_DASHBOARD);
                        break;

                    case ID_EXP_LISTBOX:
                        if (HIWORD(wParam) == LBN_SELCHANGE) {
                            g_currentExpenseIndex = SendMessage((HWND)lParam, LB_GETCURSEL, 0, 0);
                            displayExpenseRecord(g_currentExpenseIndex);
                        }
                        break;
                }
            }
            break;

        case WM_TIMER:
            if (wParam == 1) { // Timer for termination after failed login attempts
                KillTimer(hwnd, 1);
                PostQuitMessage(0);
            }
            break;

        case WM_DESTROY:
            PostQuitMessage(0);
            break;

        default:
            return DefWindowProc(hwnd, message, wParam, lParam);
    }
    return 0;
}

// --- Screen Management Functions ---
void ShowScreen(AppScreen screen) {
    // Destroy controls of the current screen
    switch (g_currentScreen) {
        case SCREEN_LOGIN: DestroyLoginControls(); break;
        case SCREEN_DASHBOARD: DestroyDashboardControls(); break;
        case SCREEN_EXPENSE_MODULE: DestroyExpenseModuleControls(); break;
    }

    g_currentScreen = screen; // Update current screen

    // Create controls for the new screen
    switch (g_currentScreen) {
        case SCREEN_LOGIN: CreateLoginControls(g_hwnd); break;
        case SCREEN_DASHBOARD: CreateDashboardControls(g_hwnd); break;
        case SCREEN_EXPENSE_MODULE: CreateExpenseModuleControls(g_hwnd); break;
    }
    UpdateWindow(g_hwnd); // Redraw the window
}

// --- Control Creation Functions ---
void CreateLoginControls(HWND hwnd) {
    // Username Label
    hLoginUsernameLabel = CreateWindow("STATIC", "USERNAME:", WS_CHILD | WS_VISIBLE,
                         100, 100, 80, 25, hwnd, (HMENU)ID_USERNAME_LABEL, GetModuleHandle(NULL), NULL);
    SendMessage(hLoginUsernameLabel, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    // Username Edit Box
    hLoginUsernameEdit = CreateWindow("EDIT", "", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL,
                                         190, 100, 150, 25, hwnd, (HMENU)ID_USERNAME_EDIT, GetModuleHandle(NULL), NULL);
    SendMessage(hLoginUsernameEdit, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    // Password Label
    hLoginPasswordLabel = CreateWindow("STATIC", "PASSWORD:", WS_CHILD | WS_VISIBLE,
                         100, 140, 80, 25, hwnd, (HMENU)ID_PASSWORD_LABEL, GetModuleHandle(NULL), NULL);
    SendMessage(hLoginPasswordLabel, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    // Password Edit Box (with ES_PASSWORD style for masked input)
    hLoginPasswordEdit = CreateWindow("EDIT", "", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_PASSWORD | ES_AUTOHSCROLL,
                                         190, 140, 150, 25, hwnd, (HMENU)ID_PASSWORD_EDIT, GetModuleHandle(NULL), NULL);
    SendMessage(hLoginPasswordEdit, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    // Login Button
    hLoginButton = CreateWindow("BUTTON", "LOG IN", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                                        100, 190, 100, 30, hwnd, (HMENU)ID_LOGIN_BUTTON, GetModuleHandle(NULL), NULL);
    SendMessage(hLoginButton, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    // Sign Up Button
    hLoginSignUpButton = CreateWindow("BUTTON", "SIGN UP", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                                         240, 190, 100, 30, hwnd, (HMENU)ID_SIGNUP_BUTTON, GetModuleHandle(NULL), NULL);
    SendMessage(hLoginSignUpButton, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    // Status Static Text
    hLoginStatusStatic = CreateWindow("STATIC", "", WS_CHILD | WS_VISIBLE | SS_CENTER,
                                        50, 240, 350, 50, hwnd, (HMENU)ID_STATUS_STATIC, GetModuleHandle(NULL), NULL);
    SendMessage(hLoginStatusStatic, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    // Welcome Static Text (initially hidden)
    hLoginWelcomeStatic = CreateWindow("STATIC", "BUDGET BUDDY", WS_CHILD | SS_CENTER,
                                          50, 150, 400, 50, hwnd, (HMENU)ID_WELCOME_STATIC, GetModuleHandle(NULL), NULL);
    SendMessage(hLoginWelcomeStatic, WM_SETFONT, (WPARAM)g_hFont, TRUE);
}

void DestroyLoginControls() {
    DestroyWindow(hLoginUsernameEdit);
    DestroyWindow(hLoginPasswordEdit);
    DestroyWindow(hLoginButton);
    DestroyWindow(hLoginSignUpButton);
    DestroyWindow(hLoginStatusStatic);
    DestroyWindow(hLoginUsernameLabel);
    DestroyWindow(hLoginPasswordLabel);
    DestroyWindow(hLoginWelcomeStatic);
}

void CreateDashboardControls(HWND hwnd) {
    // Hide welcome message if it was shown from login
    ShowWindow(hLoginWelcomeStatic, SW_HIDE);

    // Dashboard Title
    HWND hDashTitle = CreateWindow("STATIC", "BUDGET BUDDY - DASHBOARD", WS_CHILD | WS_VISIBLE | SS_CENTER,
                                   50, 50, APP_WINDOW_WIDTH - 100, 30, hwnd, NULL, GetModuleHandle(NULL), NULL);
    SendMessage(hDashTitle, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    // Expense Button
    hDashExpenseBtn = CreateWindow("BUTTON", "EXPENSE", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                                   (APP_WINDOW_WIDTH / 2) - 100, 120, 200, 40, hwnd, (HMENU)ID_DASH_EXPENSE_BTN, GetModuleHandle(NULL), NULL);
    SendMessage(hDashExpenseBtn, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    // Income Button
    hDashIncomeBtn = CreateWindow("BUTTON", "INCOME", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                                  (APP_WINDOW_WIDTH / 2) - 100, 170, 200, 40, hwnd, (HMENU)ID_DASH_INCOME_BTN, GetModuleHandle(NULL), NULL);
    SendMessage(hDashIncomeBtn, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    // History Button
    hDashHistoryBtn = CreateWindow("BUTTON", "HISTORY", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                                   (APP_WINDOW_WIDTH / 2) - 100, 220, 200, 40, hwnd, (HMENU)ID_DASH_HISTORY_BTN, GetModuleHandle(NULL), NULL);
    SendMessage(hDashHistoryBtn, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    // Account Button
    hDashAccountBtn = CreateWindow("BUTTON", "ACCOUNT", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                                   (APP_WINDOW_WIDTH / 2) - 100, 270, 200, 40, hwnd, (HMENU)ID_DASH_ACCOUNT_BTN, GetModuleHandle(NULL), NULL);
    SendMessage(hDashAccountBtn, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    // Exit Button
    hDashExitBtn = CreateWindow("BUTTON", "EXIT", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                                (APP_WINDOW_WIDTH / 2) - 100, 320, 200, 40, hwnd, (HMENU)ID_DASH_EXIT_BTN, GetModuleHandle(NULL), NULL);
    SendMessage(hDashExitBtn, WM_SETFONT, (WPARAM)g_hFont, TRUE);
}

void DestroyDashboardControls() {
    // Find and destroy all child windows that belong to the dashboard
    // This is a more robust way to destroy controls if you don't keep all HWNDs in static vars
    HWND hChild = GetWindow(g_hwnd, GW_CHILD);
    while (hChild != NULL) {
        int id = GetDlgCtrlID(hChild);
        if (id >= 200 && id < 300) { // IDs for dashboard controls are in this range
            DestroyWindow(hChild);
        }
        hChild = GetNextWindow(hChild, GW_HWNDNEXT);
    }
    // Explicitly destroy the ones we kept track of
    DestroyWindow(hDashExpenseBtn);
    DestroyWindow(hDashIncomeBtn);
    DestroyWindow(hDashHistoryBtn);
    DestroyWindow(hDashAccountBtn);
    DestroyWindow(hDashExitBtn);
    // Note: The title static control was not stored in a global HWND, so it needs to be destroyed by iterating or by ID
    // For simplicity, I'll assume the main title will be destroyed by the general child window destruction.
}

void CreateExpenseModuleControls(HWND hwnd) {
    loadExpensesFromCsv(); // Load data when entering the module

    // Labels and Edit Boxes
    hExpDescLabel = CreateWindow("STATIC", "Description:", WS_CHILD | WS_VISIBLE,
                                 30, 30, 80, 25, hwnd, (HMENU)ID_EXP_DESC_LABEL, GetModuleHandle(NULL), NULL);
    SendMessage(hExpDescLabel, WM_SETFONT, (WPARAM)g_hFont, TRUE);
    hExpDescEdit = CreateWindow("EDIT", "", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL,
                                120, 30, 150, 25, hwnd, (HMENU)ID_EXP_DESC_EDIT, GetModuleHandle(NULL), NULL);
    SendMessage(hExpDescEdit, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    hExpAmountLabel = CreateWindow("STATIC", "Amount:", WS_CHILD | WS_VISIBLE,
                                   30, 70, 80, 25, hwnd, (HMENU)ID_EXP_AMOUNT_LABEL, GetModuleHandle(NULL), NULL);
    SendMessage(hExpAmountLabel, WM_SETFONT, (WPARAM)g_hFont, TRUE);
    hExpAmountEdit = CreateWindow("EDIT", "", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_NUMBER, // ES_NUMBER for numeric input
                                  120, 70, 150, 25, hwnd, (HMENU)ID_EXP_AMOUNT_EDIT, GetModuleHandle(NULL), NULL);
    SendMessage(hExpAmountEdit, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    hExpDateLabel = CreateWindow("STATIC", "Date (YYYY-MM-DD):", WS_CHILD | WS_VISIBLE,
                                 30, 110, 130, 25, hwnd, (HMENU)ID_EXP_DATE_LABEL, GetModuleHandle(NULL), NULL);
    SendMessage(hExpDateLabel, WM_SETFONT, (WPARAM)g_hFont, TRUE);
    hExpDateEdit = CreateWindow("EDIT", "", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL,
                                170, 110, 100, 25, hwnd, (HMENU)ID_EXP_DATE_EDIT, GetModuleHandle(NULL), NULL);
    SendMessage(hExpDateEdit, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    // Buttons
    hExpAddBtn = CreateWindow("BUTTON", "Add", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                              30, 160, 80, 30, hwnd, (HMENU)ID_EXP_ADD_BTN, GetModuleHandle(NULL), NULL);
    SendMessage(hExpAddBtn, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    hExpUpdateBtn = CreateWindow("BUTTON", "Update", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                                 120, 160, 80, 30, hwnd, (HMENU)ID_EXP_UPDATE_BTN, GetModuleHandle(NULL), NULL);
    SendMessage(hExpUpdateBtn, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    hExpSaveBtn = CreateWindow("BUTTON", "Save All", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                               210, 160, 80, 30, hwnd, (HMENU)ID_EXP_SAVE_BTN, GetModuleHandle(NULL), NULL);
    SendMessage(hExpSaveBtn, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    // Search
    hExpSearchEdit = CreateWindow("EDIT", "Search...", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL,
                                  30, 210, 180, 25, hwnd, (HMENU)ID_EXP_SEARCH_EDIT, GetModuleHandle(NULL), NULL);
    SendMessage(hExpSearchEdit, WM_SETFONT, (WPARAM)g_hFont, TRUE);
    hExpSearchBtn = CreateWindow("BUTTON", "Search", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                                 220, 210, 70, 25, hwnd, (HMENU)ID_EXP_SEARCH_BTN, GetModuleHandle(NULL), NULL);
    SendMessage(hExpSearchBtn, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    // ListBox for entries
    hExpListBox = CreateWindow("LISTBOX", "", WS_CHILD | WS_VISIBLE | WS_BORDER | WS_VSCROLL | LBS_NOTIFY,
                               300, 30, 270, 300, hwnd, (HMENU)ID_EXP_LISTBOX, GetModuleHandle(NULL), NULL);
    SendMessage(hExpListBox, WM_SETFONT, (WPARAM)g_hFont, TRUE);
    updateExpenseListBox(); // Populate listbox with loaded data

    // Navigation Buttons
    hExpPrevBtn = CreateWindow("BUTTON", "Previous", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                               30, 250, 80, 30, hwnd, (HMENU)ID_EXP_PREV_BTN, GetModuleHandle(NULL), NULL);
    SendMessage(hExpPrevBtn, WM_SETFONT, (WPARAM)g_hFont, TRUE);
    hExpNextBtn = CreateWindow("BUTTON", "Next", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                               120, 250, 80, 30, hwnd, (HMENU)ID_EXP_NEXT_BTN, GetModuleHandle(NULL), NULL);
    SendMessage(hExpNextBtn, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    // Back to Main Button
    hExpBackToMainBtn = CreateWindow("BUTTON", "Back to Main", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                                     (APP_WINDOW_WIDTH / 2) - 75, APP_WINDOW_HEIGHT - 80, 150, 30, hwnd, (HMENU)ID_EXP_BACK_TO_MAIN, GetModuleHandle(NULL), NULL);
    SendMessage(hExpBackToMainBtn, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    // Status Static Text for Expense Module
    hExpStatusStatic = CreateWindow("STATIC", "", WS_CHILD | WS_VISIBLE | SS_CENTER,
                                    30, 290, 260, 50, hwnd, (HMENU)ID_EXP_STATUS_STATIC, GetModuleHandle(NULL), NULL);
    SendMessage(hExpStatusStatic, WM_SETFONT, (WPARAM)g_hFont, TRUE);

    // Display the first record if available
    if (!g_expenses.empty()) {
        g_currentExpenseIndex = 0;
        displayExpenseRecord(g_currentExpenseIndex);
        SendMessage(hExpListBox, LB_SETCURSEL, g_currentExpenseIndex, 0);
    } else {
        g_currentExpenseIndex = -1;
        SetWindowText(hExpStatusStatic, "No expenses found. Add new ones!");
    }
}

void DestroyExpenseModuleControls() {
    DestroyWindow(hExpDescLabel);
    DestroyWindow(hExpDescEdit);
    DestroyWindow(hExpAmountLabel);
    DestroyWindow(hExpAmountEdit);
    DestroyWindow(hExpDateLabel);
    DestroyWindow(hExpDateEdit);
    DestroyWindow(hExpAddBtn);
    DestroyWindow(hExpUpdateBtn);
    DestroyWindow(hExpSaveBtn);
    DestroyWindow(hExpSearchEdit);
    DestroyWindow(hExpSearchBtn);
    DestroyWindow(hExpListBox);
    DestroyWindow(hExpNextBtn);
    DestroyWindow(hExpPrevBtn);
    DestroyWindow(hExpBackToMainBtn);
    DestroyWindow(hExpStatusStatic);
    g_expenses.clear(); // Clear in-memory data when leaving module
}

// --- CSV Utility Implementations ---
bool checkCredentials(const std::string& username, const std::string& password) {
    std::ifstream file("accounts.csv");
    if (!file.is_open()) {
        // File doesn't exist, no accounts to check against
        return false;
    }
    std::string line;
    while (std::getline(file, line)) {
        size_t commaPos = line.find(',');
        if (commaPos != std::string::npos) {
            std::string storedUsername = line.substr(0, commaPos);
            std::string storedPassword = line.substr(commaPos + 1);
            if (username == storedUsername && password == storedPassword) {
                file.close();
                return true;
            }
        }
    }
    file.close();
    return false;
}

bool saveCredentials(const std::string& username, const std::string& password) {
    // Check if username already exists
    std::ifstream inFile("accounts.csv");
    std::string line;
    while (std::getline(inFile, line)) {
        size_t commaPos = line.find(',');
        if (commaPos != std::string::npos) {
            std::string storedUsername = line.substr(0, commaPos);
            if (username == storedUsername) {
                inFile.close();
                return false; // Username already exists
            }
        }
    }
    inFile.close();

    // Append new credentials
    std::ofstream outFile("accounts.csv", std::ios::app); // Open in append mode
    if (outFile.is_open()) {
        outFile << username << "," << password << "\n";
        outFile.close();
        return true;
    }
    return false; // Error opening file
}

void loadExpensesFromCsv() {
    g_expenses.clear(); // Clear existing data
    std::ifstream file("expenses.csv");
    if (!file.is_open()) {
        // File doesn't exist, no expenses to load. Not an error, just empty.
        return;
    }

    std::string line;
    while (std::getline(file, line)) {
        std::stringstream ss(line);
        std::string segment;
        Expense exp;

        // Read description (might contain commas, so handle quotes)
        if (std::getline(ss, segment, ',')) {
            if (segment.length() > 1 && segment.front() == '"' && segment.back() == '"') {
                exp.description = segment.substr(1, segment.length() - 2);
            } else {
                exp.description = segment;
            }
        } else continue; // Malformed line

        // Read amount
        if (std::getline(ss, segment, ',')) {
            try {
                exp.amount = std::stod(segment);
            } catch (...) { continue; } // Malformed amount
        } else continue;

        // Read date
        if (std::getline(ss, segment)) { // Read till end of line
            if (segment.length() > 1 && segment.front() == '"' && segment.back() == '"') {
                exp.date = segment.substr(1, segment.length() - 2);
            } else {
                exp.date = segment;
            }
        } else continue;

        g_expenses.push_back(exp);
    }
    file.close();
}

void saveExpensesToCsv() {
    std::ofstream file("expenses.csv");
    if (!file.is_open()) {
        // Handle error, e.g., show message
        SetWindowText(hExpStatusStatic, "Error: Could not open expenses.csv for writing.");
        return;
    }

    for (const auto& exp : g_expenses) {
        file << exp.toCsvString() << "\n";
    }
    file.close();
}

void displayExpenseRecord(int index) {
    if (index >= 0 && index < g_expenses.size()) {
        const Expense& exp = g_expenses[index];
        SetWindowText(hExpDescEdit, exp.description.c_str());
        char amountStr[50];
        sprintf(amountStr, "%.2f", exp.amount);
        SetWindowText(hExpAmountEdit, amountStr);
        SetWindowText(hExpDateEdit, exp.date.c_str());
        SetWindowText(hExpStatusStatic, ""); // Clear status
    } else {
        SetWindowText(hExpDescEdit, "");
        SetWindowText(hExpAmountEdit, "");
        SetWindowText(hExpDateEdit, "");
        SetWindowText(hExpStatusStatic, "No record selected or out of bounds.");
    }
}

void updateExpenseListBox() {
    SendMessage(hExpListBox, LB_RESETCONTENT, 0, 0); // Clear current items
    for (const auto& exp : g_expenses) {
        std::string displayStr = exp.description + " - $" + std::to_string(static_cast<long long>(exp.amount * 100) / 100.0) + " (" + exp.date + ")";
        SendMessage(hExpListBox, LB_ADDSTRING, 0, (LPARAM)displayStr.c_str());
    }
}

void searchExpenses(const std::string& query) {
    if (query.empty()) {
        updateExpenseListBox(); // Show all if query is empty
        SetWindowText(hExpStatusStatic, "Search cleared.");
        return;
    }

    SendMessage(hExpListBox, LB_RESETCONTENT, 0, 0);
    bool found = false;
    for (size_t i = 0; i < g_expenses.size(); ++i) {
        const Expense& exp = g_expenses[i];
        // Simple case-insensitive search by description or date
        std::string lowerDesc = exp.description;
        std::string lowerDate = exp.date;
        std::string lowerQuery = query;

        std::transform(lowerDesc.begin(), lowerDesc.end(), lowerDesc.begin(), ::tolower);
        std::transform(lowerDate.begin(), lowerDate.end(), lowerDate.begin(), ::tolower);
        std::transform(lowerQuery.begin(), lowerQuery.end(), lowerQuery.begin(), ::tolower);


        if (lowerDesc.find(lowerQuery) != std::string::npos || lowerDate.find(lowerQuery) != std::string::npos) {
            std::string displayStr = exp.description + " - $" + std::to_string(static_cast<long long>(exp.amount * 100) / 100.0) + " (" + exp.date + ")";
            SendMessage(hExpListBox, LB_ADDSTRING, 0, (LPARAM)displayStr.c_str());
            found = true;
        }
    }

    if (!found) {
        SetWindowText(hExpStatusStatic, "No matching expenses found.");
    } else {
        SetWindowText(hExpStatusStatic, "Search complete.");
    }
}
