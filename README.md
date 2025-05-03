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

##  How to Run the App

```bash
# 1. Prerequisites
- Android Studio (latest version recommended)
- Android Emulator or physical device with API level 30+

# 2. Clone the Repository
git clone https://github.com/VCSTDN2024/prog7313-part2-centsnippers1.git

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
│
├── adapters/
│   ├── CategoryAdapter.kt
│   ├── CategorySummaryAdapter.kt
│   ├── GoalsAdapter.kt
│   └── TransactionAdapter.kt
│
├── models/
│   ├── CategoryItem.kt
│   ├── Goal.kt
│   └── Transaction.kt
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
