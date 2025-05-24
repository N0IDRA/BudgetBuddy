# Budget Buddy - C++ GUI Application

This is a C++ GUI application for a budgeting system, built using the Win32 API. It includes a login system, a main dashboard, and a module for managing beneficiaries.

## Features

### Login System
- Username and password input.
- Maximum of 3 login attempts; displays remaining attempts.
- Application terminates on 3 failed attempts.
- Successful login navigates to the main dashboard.

### GUI Design
- **Base Color:** Black (`#000000`).
- **Font Color:** Green (`#00ff21`).
- **Font:** Attempts to use "HK Modular". If not installed on your system, Windows will substitute with a similar generic font.
- Clean layout with grouped inputs, consistent styling, and functional navigation.

### Main Dashboard
- Provides navigation buttons to different modules (e.g., Beneficiaries).
- Includes an "Exit" button to close the application.

### Beneficiaries Module
- **Data Management:** Stores beneficiary records (ID, Name, Contact, Address).
- **CSV Integration:**
    - Loads existing records from `beneficiaries.csv` on module entry.
    - Saves all current records to `beneficiaries.csv` (overwrites the file) when "Save All" is clicked.
    - Basic error handling for file operations (e.g., file not found).
- **CRUD Operations:**
    - **Add:** Adds a new beneficiary record to the in-memory list. Checks for duplicate IDs.
    - **Update:** Modifies the currently displayed beneficiary record.
    - **Save All:** Persists all changes from memory to the `beneficiaries.csv` file.
- **Navigation:** "Next" and "Previous" buttons to cycle through records.
- **Search:** Find beneficiaries by ID or Name.
- **List View:** Displays all beneficiaries in a list box. Selecting an item from the list populates the form fields.
- **Back Button:** Returns to the main dashboard.

## Setup and Usage Instructions

### Prerequisites
- A C++ compiler (e.g., MinGW GCC for Windows).

### Compilation
1.  Save the provided C++ code as `BudgetBuddy.cpp`.
2.  Open your command prompt or terminal.
3.  Navigate to the directory where you saved `BudgetBuddy.cpp`.
4.  Compile the application using the following command:
    ```bash
    g++ BudgetBuddy.cpp -o BudgetBuddy.exe -luser32 -lgdi32
    ```
    This command compiles the source code and links the necessary Windows libraries.

### Running the Application
1.  After successful compilation, an executable file named `BudgetBuddy.exe` will be created in the same directory.
2.  **Create Data Files:** Before running, create the following empty text files in the same directory as `BudgetBuddy.exe`:
    * `accounts.txt`
    * `beneficiaries.csv`
3.  **Add Sample Data (Optional):**
    * **`accounts.txt`:** Add sample login credentials (e.g., `admin,password`). Each entry should be on a new line.
        ```
        admin,password
        user1,pass123
        ```
    * **`beneficiaries.csv`:** Add sample beneficiary data. Each entry should be on a new line, with fields separated by commas.
        ```
        B001,John Doe,123-456-7890,123 Main St,Some Address
        B002,Jane Smith,987-654-3210,456 Oak Ave,Another Address
        ```
        *Note: If your data contains commas, they will be replaced by semicolons (`;`) during saving/loading for this basic CSV implementation.*
4.  Run the application by double-clicking `BudgetBuddy.exe` or by executing it from the command line:
    ```bash
    .\BudgetBuddy.exe
    ```

### Usage
- **Login Screen:**
    - Enter a username and password.
    - Use "Login" to attempt to log in.
    - Use "Sign Up" to create a new account (username and password cannot be empty).
    - You have 3 attempts to log in. After 3 failed attempts, the application will terminate.
- **Dashboard Screen:**
    - After successful login, you'll see a welcome message and buttons to navigate to different modules.
    - Click "Beneficiaries" to go to the Beneficiaries management module.
    - "Volunteers (Coming Soon)" is a placeholder.
    - "Exit" will close the application.
- **Beneficiaries Module:**
    - **Form Fields:** Enter or modify beneficiary details (ID, Name, Contact, Address).
    - **Add:** Adds a new beneficiary. An ID and Name are required. IDs must be unique.
    - **Update:** Updates the currently displayed beneficiary's details with the values in the form fields.
    - **Save All:** Saves all in-memory beneficiary data to `beneficiaries.csv`. **Crucial for persisting changes!**
    - **< Prev / Next >:** Navigate through existing beneficiaries.
    - **Search:** Enter an ID or Name in the search box and click "Search" to find a specific beneficiary.
    - **List Box:** Displays all loaded beneficiaries. Click an item to load its details into the form.
    - **Back to Dashboard:** Returns to the main dashboard.

## Notes
- This application uses a very basic CSV file for data storage. For production environments, a more robust and secure database solution (e.g., SQLite) and proper password hashing are highly recommended.
- The "HK Modular" font will only display if it is installed on your system. Otherwise, a default system font will be used.
- Date pickers and more advanced UI controls would require integrating the Windows Common Controls Library, which is beyond the scope of this basic Win32 example.
