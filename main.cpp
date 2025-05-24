#include <windows.h>
#include <cstdio>
#include <string>
#include <vector>
#include <fstream>
#include <sstream> // For std::stringstream
#include <algorithm> // For std::remove_if

// --- Global Defines ---
#define ID_USERNAME_EDIT 101
#define ID_PASSWORD_EDIT 102
#define ID_LOGIN_BUTTON 103
#define ID_SIGNUP_BUTTON 104
#define ID_STATUS_STATIC 105
#define ID_WELCOME_STATIC 106
#define ID_USERNAME_LABEL 107
#define ID_PASSWORD_LABEL 108

#define ID_DASHBOARD_BENEFICIARIES_BUTTON 201
#define ID_DASHBOARD_VOLUNTEERS_BUTTON 202 // Placeholder for other modules
#define ID_DASHBOARD_EXIT_BUTTON 203

#define ID_BENEFICIARY_ID_LABEL 301
#define ID_BENEFICIARY_ID_EDIT 302
#define ID_BENEFICIARY_NAME_LABEL 303
#define ID_BENEFICIARY_NAME_EDIT 304
#define ID_BENEFICIARY_CONTACT_LABEL 305
#define ID_BENEFICIARY_CONTACT_EDIT 306
#define ID_BENEFICIARY_ADDRESS_LABEL 307
#define ID_BENEFICIARY_ADDRESS_EDIT 308
#define ID_BENEFICIARY_ADD_BUTTON 309
#define ID_BENEFICIARY_UPDATE_BUTTON 310
#define ID_BENEFICIARY_SAVE_ALL_BUTTON 311
#define ID_BENEFICIARY_NEXT_BUTTON 312
#define ID_BENEFICIARY_PREVIOUS_BUTTON 313
#define ID_BENEFICIARY_SEARCH_EDIT 314
#define ID_BENEFICIARY_SEARCH_BUTTON 315
#define ID_BENEFICIARY_LIST_BOX 316
#define ID_BENEFICIARY_BACK_BUTTON 317
#define ID_BENEFICIARY_MODULE_STATUS_STATIC 318
#define ID_BENEFICIARY_CURRENT_RECORD_STATIC 319 // To show current record details

#define MAX_LOGIN_ATTEMPTS 3 // Maximum allowed login attempts

// --- Global Variables for GUI Elements ---
static HWND hUsernameEdit, hPasswordEdit, hLoginButton, hSignUpButton, hStatusStatic, hWelcomeStatic;
static HWND hUsernameLabel, hPasswordLabel;

static HWND hDashboardBeneficiariesButton, hDashboardVolunteersButton, hDashboardExitButton;

static HWND hBeneficiaryIdLabel, hBeneficiaryIdEdit;
static HWND hBeneficiaryNameLabel, hBeneficiaryNameEdit;
static HWND hBeneficiaryContactLabel, hBeneficiaryContactEdit;
static HWND hBeneficiaryAddressLabel, hBeneficiaryAddressEdit;
static HWND hBeneficiaryAddButton, hBeneficiaryUpdateButton, hBeneficiarySaveAllButton;
static HWND hBeneficiaryNextButton, hBeneficiaryPreviousButton;
static HWND hBeneficiarySearchEdit, hBeneficiarySearchButton;
static HWND hBeneficiaryListBox;
static HWND hBeneficiaryBackButton;
static HWND hBeneficiaryModuleStatusStatic;
static HWND hBeneficiaryCurrentRecordStatic;

// --- Global State Variables ---
enum AppScreen {
    SCREEN_LOGIN,
    SCREEN_DASHBOARD,
    SCREEN_BENEFICIARIES,
    // Add other modules here
};
static AppScreen currentScreen = SCREEN_LOGIN;
static int loginAttempts = 0;
static HBRUSH hbrBackground = CreateSolidBrush(RGB(0, 0, 0)); // Black background brush
static HFONT hAppFont; // Custom font handle

// --- Data Structures ---
struct Beneficiary {
    std::string id;
    std::string name;
    std::string contact;
    std::string address;

    // Convert Beneficiary to CSV string
    std::string toCsvString() const {
        std::string s_id = id;
        std::string s_name = name;
        std::string s_contact = contact;
        std::string s_address = address;

        // Escape commas in fields if necessary (basic escaping for simplicity)
        // In a real app, proper CSV escaping (e.g., double quotes) is needed.
        std::replace(s_id.begin(), s_id.end(), ',', ';');
        std::replace(s_name.begin(), s_name.end(), ',', ';');
        std::replace(s_contact.begin(), s_contact.end(), ',', ';');
        std::replace(s_address.begin(), s_address.end(), ',', ';');

        return s_id + "," + s_name + "," + s_contact + "," + s_address;
    }
};

static std::vector<Beneficiary> beneficiaries;
static int currentBeneficiaryIndex = -1; // -1 indicates no record selected/displayed

// --- Function Prototypes ---
LRESULT CALLBACK WindowProcedure(HWND hwnd, UINT message, WPARAM wParam, LPARAM lParam);
bool checkCredentials(const std::string& username, const std::string& password);
bool saveCredentials(const std::string& username, const std::string& password);
void ShowScreen(HWND hwnd, AppScreen screen);
void LoadBeneficiariesFromCSV();
void SaveBeneficiariesToCSV();
void DisplayCurrentBeneficiary();
void UpdateBeneficiaryListBox(HWND hListBox);
void ClearBeneficiaryForm();

// --- Main Entry Point ---
int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nCmdShow)
{
    const char g_szClassName[] = "BudgetBuddyApp";

    WNDCLASSEX wc;
    HWND hwnd;
    MSG Msg;

    // Create the font
    // Note: "HK Modular" is not a standard system font. For it to work, it must be installed
    // on the user's system. We're requesting it by name. If not found, Windows will substitute.
    // We'll use a generic modern font family as a fallback.
    hAppFont = CreateFont(
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
        CLEARTYPE_QUALITY,          // Quality (or ANTIALIASED_QUALITY)
        FF_MODERN | FIXED_PITCH,    // Pitch and family (FF_MODERN for a generic modern font)
        "HK Modular"                // Font face name
    );


    // Step 1: Registering the Window Class
    wc.cbSize        = sizeof(WNDCLASSEX);
    wc.style         = CS_HREDRAW | CS_VREDRAW; // Redraw on resize
    wc.lpfnWndProc   = WindowProcedure;
    wc.cbClsExtra    = 0;
    wc.cbWndExtra    = 0;
    wc.hInstance     = hInstance;
    wc.hIcon         = LoadIcon(NULL, IDI_APPLICATION);
    wc.hCursor       = LoadCursor(NULL, IDC_ARROW);
    wc.hbrBackground = hbrBackground; // Use our black brush
    wc.lpszMenuName  = NULL;
    wc.lpszClassName = g_szClassName;
    wc.hIconSm       = LoadIcon(NULL, IDI_APPLICATION);

    if (!RegisterClassEx(&wc)) {
        MessageBox(NULL, "Window Registration Failed!", "Error", MB_ICONEXCLAMATION | MB_OK);
        return 0;
    }

    // Step 2: Creating the Window
    hwnd = CreateWindowEx(
        WS_EX_CLIENTEDGE,
        g_szClassName,
        "Budget Buddy - C++ GUI",
        WS_OVERLAPPEDWINDOW | WS_VISIBLE, // Ensure it's visible initially
        CW_USEDEFAULT, CW_USEDEFAULT, 800, 600, // Larger window for dashboard/modules
        NULL, NULL, hInstance, NULL);

    if (hwnd == NULL) {
        MessageBox(NULL, "Window Creation Failed!", "Error", MB_ICONEXCLAMATION | MB_OK);
        return 0;
    }

    ShowWindow(hwnd, nCmdShow);
    UpdateWindow(hwnd);

    // Step 3: The Message Loop
    while(GetMessage(&Msg, NULL, 0, 0) > 0)
    {
        TranslateMessage(&Msg);
        DispatchMessage(&Msg);
    }

    // Clean up
    DeleteObject(hbrBackground);
    DeleteObject(hAppFont);

    return Msg.wParam;
}

// --- Window Procedure ---
LRESULT CALLBACK WindowProcedure(HWND hwnd, UINT message, WPARAM wParam, LPARAM lParam)
{
    switch(message)
    {
        case WM_CREATE:
            // --- Login Screen Controls ---
            // Username Label
            hUsernameLabel = CreateWindow("STATIC", "Username:", WS_CHILD,
                         250, 150, 100, 25, hwnd, (HMENU)ID_USERNAME_LABEL, NULL, NULL);
            // Username Edit Box
            hUsernameEdit = CreateWindow("EDIT", "", WS_CHILD | WS_BORDER,
                                         360, 150, 180, 25, hwnd, (HMENU)ID_USERNAME_EDIT, NULL, NULL);

            // Password Label
            hPasswordLabel = CreateWindow("STATIC", "Password:", WS_CHILD,
                         250, 190, 100, 25, hwnd, (HMENU)ID_PASSWORD_LABEL, NULL, NULL);
            // Password Edit Box (with ES_PASSWORD style for masked input)
            hPasswordEdit = CreateWindow("EDIT", "", WS_CHILD | WS_BORDER | ES_PASSWORD,
                                         360, 190, 180, 25, hwnd, (HMENU)ID_PASSWORD_EDIT, NULL, NULL);

            // Login Button
            hLoginButton = CreateWindow("BUTTON", "Login", WS_CHILD | WS_VISIBLE,
                                        280, 240, 100, 30, hwnd, (HMENU)ID_LOGIN_BUTTON, NULL, NULL);

            // Sign Up Button
            hSignUpButton = CreateWindow("BUTTON", "Sign Up", WS_CHILD | WS_VISIBLE,
                                         400, 240, 100, 30, hwnd, (HMENU)ID_SIGNUP_BUTTON, NULL, NULL);

            // Status Static Text
            hStatusStatic = CreateWindow("STATIC", "", WS_CHILD | SS_CENTER,
                                        200, 290, 400, 50, hwnd, (HMENU)ID_STATUS_STATIC, NULL, NULL);

            // Welcome Static Text (initially hidden)
            hWelcomeStatic = CreateWindow("STATIC", "", WS_CHILD | SS_CENTER,
                                          200, 200, 400, 50, hwnd, (HMENU)ID_WELCOME_STATIC, NULL, NULL);

            // --- Dashboard Screen Controls ---
            hDashboardBeneficiariesButton = CreateWindow("BUTTON", "Beneficiaries", WS_CHILD,
                                                         300, 150, 200, 50, hwnd, (HMENU)ID_DASHBOARD_BENEFICIARIES_BUTTON, NULL, NULL);
            hDashboardVolunteersButton = CreateWindow("BUTTON", "Volunteers (Coming Soon)", WS_CHILD,
                                                       300, 220, 200, 50, hwnd, (HMENU)ID_DASHBOARD_VOLUNTEERS_BUTTON, NULL, NULL);
            hDashboardExitButton = CreateWindow("BUTTON", "Exit", WS_CHILD,
                                                300, 350, 200, 50, hwnd, (HMENU)ID_DASHBOARD_EXIT_BUTTON, NULL, NULL);

            // --- Beneficiaries Module Controls ---
            // Labels and Edit Boxes
            hBeneficiaryIdLabel = CreateWindow("STATIC", "ID:", WS_CHILD, 50, 50, 80, 25, hwnd, (HMENU)ID_BENEFICIARY_ID_LABEL, NULL, NULL);
            hBeneficiaryIdEdit = CreateWindow("EDIT", "", WS_CHILD | WS_BORDER, 140, 50, 150, 25, hwnd, (HMENU)ID_BENEFICIARY_ID_EDIT, NULL, NULL);

            hBeneficiaryNameLabel = CreateWindow("STATIC", "Name:", WS_CHILD, 50, 80, 80, 25, hwnd, (HMENU)ID_BENEFICIARY_NAME_LABEL, NULL, NULL);
            hBeneficiaryNameEdit = CreateWindow("EDIT", "", WS_CHILD | WS_BORDER, 140, 80, 150, 25, hwnd, (HMENU)ID_BENEFICIARY_NAME_EDIT, NULL, NULL);

            hBeneficiaryContactLabel = CreateWindow("STATIC", "Contact:", WS_CHILD, 50, 110, 80, 25, hwnd, (HMENU)ID_BENEFICIARY_CONTACT_LABEL, NULL, NULL);
            hBeneficiaryContactEdit = CreateWindow("EDIT", "", WS_CHILD | WS_BORDER, 140, 110, 150, 25, hwnd, (HMENU)ID_BENEFICIARY_CONTACT_EDIT, NULL, NULL);

            hBeneficiaryAddressLabel = CreateWindow("STATIC", "Address:", WS_CHILD, 50, 140, 80, 25, hwnd, (HMENU)ID_BENEFICIARY_ADDRESS_LABEL, NULL, NULL);
            hBeneficiaryAddressEdit = CreateWindow("EDIT", "", WS_CHILD | WS_BORDER | ES_MULTILINE | ES_AUTOVSCROLL, 140, 140, 150, 70, hwnd, (HMENU)ID_BENEFICIARY_ADDRESS_EDIT, NULL, NULL);

            // Buttons
            hBeneficiaryAddButton = CreateWindow("BUTTON", "Add", WS_CHILD, 50, 230, 80, 30, hwnd, (HMENU)ID_BENEFICIARY_ADD_BUTTON, NULL, NULL);
            hBeneficiaryUpdateButton = CreateWindow("BUTTON", "Update", WS_CHILD, 140, 230, 80, 30, hwnd, (HMENU)ID_BENEFICIARY_UPDATE_BUTTON, NULL, NULL);
            hBeneficiarySaveAllButton = CreateWindow("BUTTON", "Save All", WS_CHILD, 230, 230, 80, 30, hwnd, (HMENU)ID_BENEFICIARY_SAVE_ALL_BUTTON, NULL, NULL);

            hBeneficiaryPreviousButton = CreateWindow("BUTTON", "< Prev", WS_CHILD, 50, 270, 80, 30, hwnd, (HMENU)ID_BENEFICIARY_PREVIOUS_BUTTON, NULL, NULL);
            hBeneficiaryNextButton = CreateWindow("BUTTON", "Next >", WS_CHILD, 140, 270, 80, 30, hwnd, (HMENU)ID_BENEFICIARY_NEXT_BUTTON, NULL, NULL);

            hBeneficiarySearchEdit = CreateWindow("EDIT", "", WS_CHILD | WS_BORDER, 50, 310, 150, 25, hwnd, (HMENU)ID_BENEFICIARY_SEARCH_EDIT, NULL, NULL);
            hBeneficiarySearchButton = CreateWindow("BUTTON", "Search", WS_CHILD, 210, 310, 80, 25, hwnd, (HMENU)ID_BENEFICIARY_SEARCH_BUTTON, NULL, NULL);

            hBeneficiaryListBox = CreateWindow("LISTBOX", "", WS_CHILD | WS_BORDER | WS_VSCROLL | LBS_NOTIFY,
                                               350, 50, 400, 300, hwnd, (HMENU)ID_BENEFICIARY_LIST_BOX, NULL, NULL);

            hBeneficiaryBackButton = CreateWindow("BUTTON", "Back to Dashboard", WS_CHILD,
                                                  50, 350, 200, 30, hwnd, (HMENU)ID_BENEFICIARY_BACK_BUTTON, NULL, NULL);

            hBeneficiaryModuleStatusStatic = CreateWindow("STATIC", "", WS_CHILD | SS_CENTER,
                                                          50, 390, 700, 25, hwnd, (HMENU)ID_BENEFICIARY_MODULE_STATUS_STATIC, NULL, NULL);

            // Set font for all controls
            SendMessage(hUsernameLabel, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hUsernameEdit, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hPasswordLabel, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hPasswordEdit, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hLoginButton, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hSignUpButton, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hStatusStatic, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hWelcomeStatic, WM_SETFONT, (WPARAM)hAppFont, TRUE);

            SendMessage(hDashboardBeneficiariesButton, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hDashboardVolunteersButton, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hDashboardExitButton, WM_SETFONT, (WPARAM)hAppFont, TRUE);

            SendMessage(hBeneficiaryIdLabel, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hBeneficiaryIdEdit, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hBeneficiaryNameLabel, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hBeneficiaryNameEdit, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hBeneficiaryContactLabel, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hBeneficiaryContactEdit, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hBeneficiaryAddressLabel, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hBeneficiaryAddressEdit, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hBeneficiaryAddButton, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hBeneficiaryUpdateButton, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hBeneficiarySaveAllButton, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hBeneficiaryNextButton, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hBeneficiaryPreviousButton, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hBeneficiarySearchEdit, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hBeneficiarySearchButton, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hBeneficiaryListBox, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hBeneficiaryBackButton, WM_SETFONT, (WPARAM)hAppFont, TRUE);
            SendMessage(hBeneficiaryModuleStatusStatic, WM_SETFONT, (WPARAM)hAppFont, TRUE);


            // Initial screen setup
            ShowScreen(hwnd, SCREEN_LOGIN);
            break;

        case WM_COMMAND:
            {
                // Get text from username and password fields for login/signup
                char usernameBuf[50];
                char passwordBuf[50];
                GetWindowText(hUsernameEdit, usernameBuf, 50);
                GetWindowText(hPasswordEdit, passwordBuf, 50);
                std::string username = usernameBuf;
                std::string password = passwordBuf;

                // Get text from beneficiary fields
                char idBuf[50], nameBuf[100], contactBuf[100], addressBuf[256];
                GetWindowText(hBeneficiaryIdEdit, idBuf, 50);
                GetWindowText(hBeneficiaryNameEdit, nameBuf, 100);
                GetWindowText(hBeneficiaryContactEdit, contactBuf, 100);
                GetWindowText(hBeneficiaryAddressEdit, addressBuf, 256);
                std::string b_id = idBuf;
                std::string b_name = nameBuf;
                std::string b_contact = contactBuf;
                std::string b_address = addressBuf;

                switch(LOWORD(wParam))
                {
                    // --- Login Screen Commands ---
                    case ID_LOGIN_BUTTON:
                        if (checkCredentials(username, password)) {
                            loginAttempts = 0; // Reset attempts on successful login
                            SetWindowText(hStatusStatic, ""); // Clear status
                            char welcomeMsg[100];
                            sprintf(welcomeMsg, "Welcome, %s!", username.c_str());
                            SetWindowText(hWelcomeStatic, welcomeMsg);
                            ShowScreen(hwnd, SCREEN_DASHBOARD); // Navigate to dashboard
                        } else {
                            loginAttempts++; // Increment failed attempt counter
                            char statusMsg[100];
                            sprintf(statusMsg, "Invalid Username or Password. Attempts left: %d", MAX_LOGIN_ATTEMPTS - loginAttempts);
                            SetWindowText(hStatusStatic, statusMsg);

                            if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
                                SetWindowText(hStatusStatic, "Too many failed attempts. Terminating.");
                                PostQuitMessage(0); // Terminate the application
                            }
                        }
                        break;

                    case ID_SIGNUP_BUTTON:
                        if (username.empty() || password.empty()) {
                            SetWindowText(hStatusStatic, "Username and Password cannot be empty.");
                        } else if (saveCredentials(username, password)) {
                            SetWindowText(hStatusStatic, "Account created successfully!");
                            SetWindowText(hUsernameEdit, "");
                            SetWindowText(hPasswordEdit, "");
                            loginAttempts = 0; // Reset attempts after a successful sign-up
                        } else {
                            SetWindowText(hStatusStatic, "Username already exists or file error.");
                        }
                        break;

                    // --- Dashboard Screen Commands ---
                    case ID_DASHBOARD_BENEFICIARIES_BUTTON:
                        ShowScreen(hwnd, SCREEN_BENEFICIARIES);
                        LoadBeneficiariesFromCSV();
                        UpdateBeneficiaryListBox(hBeneficiaryListBox);
                        ClearBeneficiaryForm();
                        SetWindowText(hBeneficiaryModuleStatusStatic, "Beneficiaries module loaded.");
                        break;
                    case ID_DASHBOARD_VOLUNTEERS_BUTTON:
                        SetWindowText(hStatusStatic, "Volunteers module is coming soon!");
                        break;
                    case ID_DASHBOARD_EXIT_BUTTON:
                        PostQuitMessage(0);
                        break;

                    // --- Beneficiaries Module Commands ---
                    case ID_BENEFICIARY_ADD_BUTTON:
                        if (b_id.empty() || b_name.empty()) {
                            SetWindowText(hBeneficiaryModuleStatusStatic, "ID and Name cannot be empty.");
                        } else {
                            // Check for duplicate ID
                            bool idExists = false;
                            for (const auto& b : beneficiaries) {
                                if (b.id == b_id) {
                                    idExists = true;
                                    break;
                                }
                            }
                            if (idExists) {
                                SetWindowText(hBeneficiaryModuleStatusStatic, "Beneficiary with this ID already exists.");
                            } else {
                                beneficiaries.push_back({b_id, b_name, b_contact, b_address});
                                UpdateBeneficiaryListBox(hBeneficiaryListBox);
                                ClearBeneficiaryForm();
                                SetWindowText(hBeneficiaryModuleStatusStatic, "Beneficiary added. Remember to Save All.");
                            }
                        }
                        break;

                    case ID_BENEFICIARY_UPDATE_BUTTON:
                        if (currentBeneficiaryIndex != -1 && currentBeneficiaryIndex < beneficiaries.size()) {
                            beneficiaries[currentBeneficiaryIndex] = {b_id, b_name, b_contact, b_address};
                            UpdateBeneficiaryListBox(hBeneficiaryListBox);
                            SetWindowText(hBeneficiaryModuleStatusStatic, "Beneficiary updated. Remember to Save All.");
                        } else {
                            SetWindowText(hBeneficiaryModuleStatusStatic, "No beneficiary selected to update.");
                        }
                        break;

                    case ID_BENEFICIARY_SAVE_ALL_BUTTON:
                        SaveBeneficiariesToCSV();
                        SetWindowText(hBeneficiaryModuleStatusStatic, "All beneficiaries saved to CSV.");
                        break;

                    case ID_BENEFICIARY_NEXT_BUTTON:
                        if (!beneficiaries.empty()) {
                            currentBeneficiaryIndex = (currentBeneficiaryIndex + 1) % beneficiaries.size();
                            DisplayCurrentBeneficiary();
                            SetWindowText(hBeneficiaryModuleStatusStatic, "Next beneficiary.");
                        } else {
                            SetWindowText(hBeneficiaryModuleStatusStatic, "No beneficiaries to navigate.");
                        }
                        break;

                    case ID_BENEFICIARY_PREVIOUS_BUTTON:
                        if (!beneficiaries.empty()) {
                            currentBeneficiaryIndex = (currentBeneficiaryIndex - 1 + beneficiaries.size()) % beneficiaries.size();
                            DisplayCurrentBeneficiary();
                            SetWindowText(hBeneficiaryModuleStatusStatic, "Previous beneficiary.");
                        } else {
                            SetWindowText(hBeneficiaryModuleStatusStatic, "No beneficiaries to navigate.");
                        }
                        break;

                    case ID_BENEFICIARY_SEARCH_BUTTON:
                        {
                            char searchBuf[100];
                            GetWindowText(hBeneficiarySearchEdit, searchBuf, 100);
                            std::string searchTerm = searchBuf;
                            if (searchTerm.empty()) {
                                SetWindowText(hBeneficiaryModuleStatusStatic, "Please enter a search term.");
                                break;
                            }

                            int foundIndex = -1;
                            for (int i = 0; i < beneficiaries.size(); ++i) {
                                if (beneficiaries[i].id == searchTerm || beneficiaries[i].name.find(searchTerm) != std::string::npos) {
                                    foundIndex = i;
                                    break;
                                }
                            }

                            if (foundIndex != -1) {
                                currentBeneficiaryIndex = foundIndex;
                                DisplayCurrentBeneficiary();
                                SetWindowText(hBeneficiaryModuleStatusStatic, "Beneficiary found.");
                            } else {
                                SetWindowText(hBeneficiaryModuleStatusStatic, "Beneficiary not found.");
                                ClearBeneficiaryForm();
                            }
                        }
                        break;

                    case ID_BENEFICIARY_LIST_BOX:
                        if (HIWORD(wParam) == LBN_SELCHANGE) {
                            int selectedIndex = SendMessage((HWND)lParam, LB_GETCURSEL, 0, 0);
                            if (selectedIndex != LB_ERR) {
                                currentBeneficiaryIndex = selectedIndex;
                                DisplayCurrentBeneficiary();
                                SetWindowText(hBeneficiaryModuleStatusStatic, "Beneficiary selected from list.");
                            }
                        }
                        break;

                    case ID_BENEFICIARY_BACK_BUTTON:
                        ShowScreen(hwnd, SCREEN_DASHBOARD);
                        SetWindowText(hBeneficiaryModuleStatusStatic, ""); // Clear module status
                        beneficiaries.clear(); // Clear in-memory data when leaving module
                        currentBeneficiaryIndex = -1;
                        break;
                }
            }
            break;

        case WM_CTLCOLORSTATIC: // Handle static text control colors
            {
                HDC hdcStatic = (HDC)wParam;
                SetTextColor(hdcStatic, RGB(0, 255, 33)); // Font Color: #00ff21
                SetBkMode(hdcStatic, TRANSPARENT); // Make background transparent
                return (LRESULT)hbrBackground; // Use our black background brush
            }
            break;

        case WM_CTLCOLOREDIT: // Handle edit control colors (background)
            {
                HDC hdcEdit = (HDC)wParam;
                SetTextColor(hdcEdit, RGB(0, 255, 33)); // Font Color: #00ff21
                SetBkColor(hdcEdit, RGB(0, 0, 0)); // Background Color: #000000
                return (LRESULT)hbrBackground; // Return our black brush
            }
            break;

        case WM_CTLCOLORBTN: // Handle button colors (background)
            {
                HDC hdcBtn = (HDC)wParam;
                SetTextColor(hdcBtn, RGB(0, 255, 33)); // Font Color: #00ff21
                SetBkColor(hdcBtn, RGB(50, 50, 50)); // A slightly lighter black for button background
                return (LRESULT)CreateSolidBrush(RGB(50, 50, 50)); // Return a brush for button background
            }
            break;

        case WM_CTLCOLORLISTBOX: // Handle ListBox colors
            {
                HDC hdcListBox = (HDC)wParam;
                SetTextColor(hdcListBox, RGB(0, 255, 33)); // Font Color: #00ff21
                SetBkColor(hdcListBox, RGB(0, 0, 0)); // Background Color: #000000
                return (LRESULT)hbrBackground; // Return our black brush
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

// --- Helper Functions ---

// Function to check credentials in a file
bool checkCredentials(const std::string& username, const std::string& password) {
    std::ifstream file("accounts.txt");
    if (!file.is_open()) {
        // Create an empty accounts.txt if it doesn't exist
        std::ofstream outFile("accounts.txt");
        outFile.close();
        return false; // No accounts yet
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

// Function to save new credentials to a file
bool saveCredentials(const std::string& username, const std::string& password) {
    // Check if username already exists
    std::ifstream inFile("accounts.txt");
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
    std::ofstream outFile("accounts.txt", std::ios::app); // Open in append mode
    if (outFile.is_open()) {
        outFile << username << "," << password << "\n";
        outFile.close();
        return true;
    }
    return false; // Error opening file
}

// Function to show/hide controls based on the current screen
void ShowScreen(HWND hwnd, AppScreen screen) {
    currentScreen = screen;

    // Hide all controls first
    ShowWindow(hUsernameLabel, SW_HIDE);
    ShowWindow(hUsernameEdit, SW_HIDE);
    ShowWindow(hPasswordLabel, SW_HIDE);
    ShowWindow(hPasswordEdit, SW_HIDE);
    ShowWindow(hLoginButton, SW_HIDE);
    ShowWindow(hSignUpButton, SW_HIDE);
    ShowWindow(hStatusStatic, SW_HIDE);
    ShowWindow(hWelcomeStatic, SW_HIDE);

    ShowWindow(hDashboardBeneficiariesButton, SW_HIDE);
    ShowWindow(hDashboardVolunteersButton, SW_HIDE);
    ShowWindow(hDashboardExitButton, SW_HIDE);

    ShowWindow(hBeneficiaryIdLabel, SW_HIDE);
    ShowWindow(hBeneficiaryIdEdit, SW_HIDE);
    ShowWindow(hBeneficiaryNameLabel, SW_HIDE);
    ShowWindow(hBeneficiaryNameEdit, SW_HIDE);
    ShowWindow(hBeneficiaryContactLabel, SW_HIDE);
    ShowWindow(hBeneficiaryContactEdit, SW_HIDE);
    ShowWindow(hBeneficiaryAddressLabel, SW_HIDE);
    ShowWindow(hBeneficiaryAddressEdit, SW_HIDE);
    ShowWindow(hBeneficiaryAddButton, SW_HIDE);
    ShowWindow(hBeneficiaryUpdateButton, SW_HIDE);
    ShowWindow(hBeneficiarySaveAllButton, SW_HIDE);
    ShowWindow(hBeneficiaryNextButton, SW_HIDE);
    ShowWindow(hBeneficiaryPreviousButton, SW_HIDE);
    ShowWindow(hBeneficiarySearchEdit, SW_HIDE);
    ShowWindow(hBeneficiarySearchButton, SW_HIDE);
    ShowWindow(hBeneficiaryListBox, SW_HIDE);
    ShowWindow(hBeneficiaryBackButton, SW_HIDE);
    ShowWindow(hBeneficiaryModuleStatusStatic, SW_HIDE);
    // ShowWindow(hBeneficiaryCurrentRecordStatic, SW_HIDE); // This control is not used yet for display

    // Show controls for the current screen
    switch (currentScreen) {
        case SCREEN_LOGIN:
            ShowWindow(hUsernameLabel, SW_SHOW);
            ShowWindow(hUsernameEdit, SW_SHOW);
            ShowWindow(hPasswordLabel, SW_SHOW);
            ShowWindow(hPasswordEdit, SW_SHOW);
            ShowWindow(hLoginButton, SW_SHOW);
            ShowWindow(hSignUpButton, SW_SHOW);
            ShowWindow(hStatusStatic, SW_SHOW);
            SetWindowText(hUsernameEdit, ""); // Clear fields
            SetWindowText(hPasswordEdit, "");
            SetWindowText(hStatusStatic, ""); // Clear status
            break;
        case SCREEN_DASHBOARD:
            ShowWindow(hWelcomeStatic, SW_SHOW); // Keep welcome message visible on dashboard
            ShowWindow(hDashboardBeneficiariesButton, SW_SHOW);
            ShowWindow(hDashboardVolunteersButton, SW_SHOW);
            ShowWindow(hDashboardExitButton, SW_SHOW);
            break;
        case SCREEN_BENEFICIARIES:
            ShowWindow(hBeneficiaryIdLabel, SW_SHOW);
            ShowWindow(hBeneficiaryIdEdit, SW_SHOW);
            ShowWindow(hBeneficiaryNameLabel, SW_SHOW);
            ShowWindow(hBeneficiaryNameEdit, SW_SHOW);
            ShowWindow(hBeneficiaryContactLabel, SW_SHOW);
            ShowWindow(hBeneficiaryContactEdit, SW_SHOW);
            ShowWindow(hBeneficiaryAddressLabel, SW_SHOW);
            ShowWindow(hBeneficiaryAddressEdit, SW_SHOW);
            ShowWindow(hBeneficiaryAddButton, SW_SHOW);
            ShowWindow(hBeneficiaryUpdateButton, SW_SHOW);
            ShowWindow(hBeneficiarySaveAllButton, SW_SHOW);
            ShowWindow(hBeneficiaryNextButton, SW_SHOW);
            ShowWindow(hBeneficiaryPreviousButton, SW_SHOW);
            ShowWindow(hBeneficiarySearchEdit, SW_SHOW);
            ShowWindow(hBeneficiarySearchButton, SW_SHOW);
            ShowWindow(hBeneficiaryListBox, SW_SHOW);
            ShowWindow(hBeneficiaryBackButton, SW_SHOW);
            ShowWindow(hBeneficiaryModuleStatusStatic, SW_SHOW);
            break;
    }
}

// Load beneficiaries from CSV file
void LoadBeneficiariesFromCSV() {
    beneficiaries.clear(); // Clear existing data
    currentBeneficiaryIndex = -1;

    std::ifstream file("beneficiaries.csv");
    if (!file.is_open()) {
        // Create an empty file if it doesn't exist
        std::ofstream outFile("beneficiaries.csv");
        outFile.close();
        SetWindowText(hBeneficiaryModuleStatusStatic, "beneficiaries.csv not found. Created new file.");
        return;
    }

    std::string line;
    while (std::getline(file, line)) {
        std::stringstream ss(line);
        std::string segment;
        Beneficiary b;

        // Basic CSV parsing: Assumes no commas within fields for simplicity
        // If fields can contain commas, proper CSV parsing with quotes is needed.
        if (std::getline(ss, segment, ',')) b.id = segment;
        if (std::getline(ss, segment, ',')) b.name = segment;
        if (std::getline(ss, segment, ',')) b.contact = segment;
        if (std::getline(ss, segment, ',')) b.address = segment;

        beneficiaries.push_back(b);
    }
    file.close();
    SetWindowText(hBeneficiaryModuleStatusStatic, "Beneficiaries loaded from CSV.");
    if (!beneficiaries.empty()) {
        currentBeneficiaryIndex = 0;
        DisplayCurrentBeneficiary();
    } else {
        ClearBeneficiaryForm();
    }
}

// Save beneficiaries to CSV file (overwrites existing file)
void SaveBeneficiariesToCSV() {
    std::ofstream file("beneficiaries.csv");
    if (!file.is_open()) {
        SetWindowText(hBeneficiaryModuleStatusStatic, "Error: Could not open beneficiaries.csv for writing.");
        return;
    }

    for (const auto& b : beneficiaries) {
        file << b.toCsvString() << "\n";
    }
    file.close();
}

// Display the current beneficiary's details in the form fields
void DisplayCurrentBeneficiary() {
    if (currentBeneficiaryIndex != -1 && currentBeneficiaryIndex < beneficiaries.size()) {
        const Beneficiary& b = beneficiaries[currentBeneficiaryIndex];
        SetWindowText(hBeneficiaryIdEdit, b.id.c_str());
        SetWindowText(hBeneficiaryNameEdit, b.name.c_str());
        SetWindowText(hBeneficiaryContactEdit, b.contact.c_str());
        SetWindowText(hBeneficiaryAddressEdit, b.address.c_str());
    } else {
        ClearBeneficiaryForm();
    }
}

// Update the ListBox with current beneficiaries
void UpdateBeneficiaryListBox(HWND hListBox) {
    SendMessage(hListBox, LB_RESETCONTENT, 0, 0); // Clear existing items
    for (const auto& b : beneficiaries) {
        std::string listItem = b.id + " - " + b.name;
        SendMessage(hListBox, LB_ADDSTRING, 0, (LPARAM)listItem.c_str());
    }
    if (currentBeneficiaryIndex != -1 && currentBeneficiaryIndex < beneficiaries.size()) {
        SendMessage(hListBox, LB_SETCURSEL, currentBeneficiaryIndex, 0);
    }
}

// Clear the beneficiary form fields
void ClearBeneficiaryForm() {
    SetWindowText(hBeneficiaryIdEdit, "");
    SetWindowText(hBeneficiaryNameEdit, "");
    SetWindowText(hBeneficiaryContactEdit, "");
    SetWindowText(hBeneficiaryAddressEdit, "");
    currentBeneficiaryIndex = -1;
}
