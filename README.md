<!-- 🎥 Demo Video -->
<!-- https://youtu.be/cVHNUFpoiaM-->

# Cent Snippers

Cent Snippers is a personal budgeting Android app developed using Kotlin and SQLite. It empowers users to take control of their finances through categorized budgeting, monthly goals, and real-time dashboard insights—all with persistent local storage.

```
Main Features:
--------------
✔ User registration & login with session persistence  
✔ Add income and set monthly minimum & maximum spending goals  
✔ Track spending in categorized budgets with visual summaries  
✔ View filtered totals by month or date range  
✔ Edit income dynamically via dashboard  
✔ Local SQLite storage ensures offline functionality  
✔ Clean UI using CardViews, RecyclerViews, and Dialogs 

 ```
## Recent Updates (June 2025)

### 👤 Login & Register System Upgrade
- Fully redesigned login and register UIs using `TextInputLayout` for clean, modern input handling
- Register screen now includes `Confirm Password` logic with full validation and error messaging
- After a successful registration, users see a one-time onboarding dialog explaining how to use the app
- Sessions remain persistent using a lightweight `SessionManager`

### 📊 Category Graph Tab Finalized
- Pie chart system now visualizes:
  - Total Spend per Category
  - Adjusted Goal Amounts (based on filter period)
  - Adjusted Min/Max Spend (only if all values are present)
- Custom legend replaces default chart legends
- Charts auto-hide if any category is missing required data
- Friendly message shown when pie chart is skipped due to incomplete values

### 📅 Dynamic Filter Logic for Date Ranges
- Start/End date defaults to current month
- Date range filter applies across both tabs: List + Graph
- Filter button actively re-applies the logic
- `CategoryFragment` now stores and passes `currentStartDate` and `currentEndDate` centrally

### 🧠 Adjusted Spend Logic
- If user selects a multi-month period (e.g., 45 days), goals auto-scale
- Formula: `adjusted = goalAmount * ((days - 30) / 100 + 1)`
- Applied to `goalAmount`, `minSpend`, and `maxSpend`

### 💬 Summary Budget Label
- Total Category budget label updates dynamically:
  - Default: “Total Budgeted for June 2025”
  - Filtered: “Total Budgeted per selected period: Rxxxx.xx”

### 📚 Education Hub Added
- New `EducationFragment` helps users learn better financial habits
- Includes scrollable lists of:
  - 📖 Curated budgeting articles (Investopedia, NerdWallet)
  - 📺 Embedded YouTube videos with thumbnail previews
- Clicking opens external link in browser
- Dark-themed design with Glide-powered thumbnail loading
- Fully integrated into the top-right menu under “Learn”

---

```

##  How to Run the App

```bash
# 1. Prerequisites
- Android Studio (latest version recommended)
- Android Emulator or physical device with API level 30+

# 2. Clone the Repository
git clone https://github.com/Thonipho-M/CentSnippersPart3.git
Branch workspace

# 3. Open in Android Studio
- Open Android Studio
- Click: File > Open > Navigate to the project folder
- Let Gradle sync

# 4. Run the App
- Either connect a device (USB debugging enabled)
- Or launch an emulator (Pixel 4 API 30 suggested)
- Press "Run"
```

---

##  How to Use the App

```text
1. Register a new user account
2. Go to Dashboard → Click income to set monthly income
3. Navigate to Goals → Set monthly min/max spending limits
4. Add category budgets and create transactions
5. Use filters to analyze spending trends
6. Visual insights update dynamically in Dashboard & Goals
```

---

## 💻 Tech Stack

| Technology       | Purpose                               |
|------------------|----------------------------------------|
| Kotlin           | Main programming language              |
| Android Studio   | IDE used for development               |
| SQLite           | Local database for data persistence    |
| ViewBinding      | Type-safe view access                  |
| RecyclerView     | Display dynamic category & goal lists  |
| CardView         | Stylish UI containers                  |
| XML              | UI layout design                       |

---

##  Project Structure

```
com.centsnippers/
│
├── fragments/
│   ├── LoginFragment.kt
│   ├── RegisterFragment.kt
│   ├── DashboardFragment.kt
│   ├── CategoryFragment.kt
│   ├── GoalsFragment.kt
│   └── TransactionFragment.kt
│   └── IncomeFragment.kt
│   └── EducationFragment.kt
│
├── adapters/
│   ├── CategoryAdapter.kt
│   ├── CategorySummaryAdapter.kt
│   ├── GoalsAdapter.kt
│   └── TransactionAdapter.kt
│   └── IncomeAdapter.kt
│   └── EducationAdapter.kt
│
├── models/
│   ├── CategoryItem.kt
│   ├── Goal.kt
│   └── TransactionItem.kt
│   └── IncomeItem.kt
│   └── VideoItem.kt
│   └── ArticleItem.kt
│
├── utils/
│   └── SessionManager.kt
│
├── data/
│   └── DatabaseHelper.kt
│
└── res/
    ├── layout/
    ├── drawable/
    └── values/
```

---

##  Authors

```
Thonipho Mavhungu  
Braydon Wooley
```

---

##  Notes

- Built as part of the PROG7313 Application Development module.
- App follows local data model using SQLite — no external server dependencies.
- Designed to function offline and retain data between sessions.