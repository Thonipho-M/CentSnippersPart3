

Cent Snippers is a personal budgeting Android app designed to help users manage their finances by tracking budgets and goals. It supports user registration and login, allowing users to continue where they left off using a local SQLite database.

## How to Run the App

This guide walks you through setting up and running the app on either a physical Android device or an emulator.

### 1. Prerequisites

- Android Studio installed (latest stable version)
- Android device or emulator with at least API level 30

### 2. Clone the Repository

Run the following in your terminal:

git clone https://github.com/VCSTDN2024/prog7313-part2-centsnippers1.git


### 3. Open the Project in Android Studio

- Open Android Studio
- Click File > Open
- Navigate to the folder that contains `build.gradle` and open it
- Let Gradle sync and the project index

### 4. Run the App

#### Option 1: Using a Physical Device

- Connect your phone via USB
- Enable Developer Options and USB Debugging
- Select your device in the target list
- Press Run

#### Option 2: Using an Emulator

- Create a virtual device (Pixel 4, API 30 or similar)
- Use an image without Google Play if on a lower-end PC
- Launch the emulator and press Run

### 5. Use the App

- Register a new account
- Add budgets and goals
- Exit and re-open the app to verify data persists


Features Implemented


  - Users can create accounts.
  - Login persists via a session manager.
  - Budgets and user information are stored locally using SQLite.
  - Each user has their own budgets and data.
  - Users can add budgets with a category and amount.
  - Bottom navigation bar allows users to switch between Dashboard, Budgets, and Goals.


Tech Stack

| Tool           | Purpose                              |
|----------------|--------------------------------------|
| Android Studio | IDE for development                  |
| Kotlin         | Programming language                 |
| XML            | UI layout files                      |
| SQLite         | Local database storage               |
| ViewBinding    | Access layout views safely           |
| RecyclerView   | Displaying lists (budgets, goals)    |


Project Structure (Simplified)


com.centsnippers/
│
├── fragments/
│   ├── LoginFragment.kt
│   ├── RegisterFragment.kt
│   ├── BudgetFragment.kt
│   ├── GoalsFragment.kt
│   └── DashboardFragment.kt
│
├── adapters/
│   ├── BudgetAdapter.kt
│   └── GoalsAdapter.kt
│
├── models/
│   ├── BudgetItem.kt
│   └── GoalItem.kt
│
├── database/
│   └── DatabaseHelper.kt
│
├── utils/
│   └── SessionManager.kt
│
└── res/
    ├── layout/
    ├── drawable/
    └── values/
```


Author

Built by Thonipho Mavhungu & Braydon Wooley
