# Cent Snippers

## Recent Updates (June 2025)

### 🔁 Category Model Overhaul
- Replaced single `amount` field with:
  - `goalAmount`: Main budget goal
  - `minSpend`: Optional minimum target
  - `maxSpend`: Optional maximum limit
- UI updated to reflect all three values where relevant
- Backwards-compatible fallback logic for empty min/max

### 🛠️ Category UI Restoration
- Restored RecyclerView list with improved `item_category.xml`
- Adapter now reflects goal vs. spend, min/max warnings
- FloatingActionButton reconnected and stable
- Logging improved for click actions, add/edit/delete operations

### 📊 Graph Integration Prep
- MPAndroidChart added to project for upcoming bar graphs
- Initial setup for horizontal category comparison charts underway

### ✅ Logging and Testing Improvements
- Log statements added for FABs, RecyclerViews, and critical click handlers
- Confirmed stable UI and navigation between fragments
- Incomplete graphs are hidden until feature completion

---

## Application Overview

Cent Snippers is a personal budgeting Android app developed using Kotlin and SQLite. It empowers users to take control of their finances through categorized budgeting, recurring income management, and real-time dashboards—all stored offline on the device.

### Main Features
- User registration and login with persistent session tracking
- Create and manage incomes with support for monthly or yearly recurrence
- Record categorized transactions with summaries per budget
- Filter dashboard views by current month or date range
- Real-time calculation of totals and remaining budget
- Local SQLite storage for fully offline capability
- Clean and responsive UI using CardViews, RecyclerViews, and Dialogs

---

## Feature Status

| Module        | Functionality Summary                                                                  | Status                     |
|---------------|-----------------------------------------------------------------------------------------|-----------------------------|
| Income        | Delete, edit, once-off logic, UI updates                                               | Functional, testing pending |
| Transaction   | Category edit, confirmation dialog, dynamic search, deletion warning                   | Functional, testing pending |
| Category      | New model (goal, min, max), restored UI, adapter updated, graph area in progress       | Mostly functional           |
| Dashboard     | Loads filtered income, categories, transactions + total logic                          | Functional, testing graphs  |

---

## Environment and Setup

### Prerequisites
- Android Studio (latest version recommended)
- Android Emulator or physical Android device (API level 30 or above)

### How to Run the App

```bash
# Clone the Repository
git clone https://github.com/VCSTDN2024/prog7313-part2-centsnippers1.git

# Open in Android Studio
- File > Open > Locate the project folder
- Allow Gradle to sync and resolve dependencies

# Run
- Connect a device or start an emulator
- Press Run or Shift + F10
