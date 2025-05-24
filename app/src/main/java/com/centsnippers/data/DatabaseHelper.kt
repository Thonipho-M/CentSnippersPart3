// ===============================
// DatabaseHelper.kt (Reviewed + Logged + Enhanced)
// Handles: All DB logic for Users, Categories, Transactions
// Logs user actions clearly — including username context
// ===============================

package com.centsnippers.data
import com.centsnippers.models.RegisterResult
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.centsnippers.models.*



class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "DatabaseHelper"
        private const val DATABASE_NAME = "CentSnippers.db"
        private const val DATABASE_VERSION = 2
        // ==============================================
        // USERS TABLE CONSTANTS
        // ==============================================
        private const val TABLE_USERS = "users"
        private const val COL_USER_ID = "id"
        private const val COL_USERNAME = "username"
        private const val COL_PASSWORD = "password"
        // ==============================================
        // CATEGORIIES TABLE CONSTANTS
        // ==============================================
        private const val TABLE_CATEGORIES = "categories"
        private const val COL_CATEGORY_ID = "id"
        private const val COL_CATEGORY_USER_ID = "userId"
        private const val COL_CATEGORY_TITLE = "title"
        private const val COL_CATEGORY_DESCRIPTION = "description"
        private const val COL_CATEGORY_AMOUNT = "amount"
        // ==============================================
        // TRANSACTIONS TABLE CONSTANTS
        // ==============================================
        private const val TABLE_TRANSACTIONS = "transactions"
        private const val COL_TRANSACTION_ID = "id"
        private const val COL_TRANSACTION_USER_ID = "userId"
        private const val COL_TRANSACTION_CATEGORY_ID = "categoryId"
        private const val COL_TRANSACTION_TITLE = "title"
        private const val COL_TRANSACTION_DESCRIPTION = "description"
        private const val COL_TRANSACTION_AMOUNT = "amount"
        private const val COL_TRANSACTION_START = "startDate"
        private const val COL_TRANSACTION_END = "endDate"
        private const val COL_TRANSACTION_IMAGE = "imageUri"

        // ==============================================
        // INCOME TABLE CONSTANTS
        // ==============================================
        private const val TABLE_INCOME = "income"
        private const val COL_INCOME_ID = "id"
        private const val COL_INCOME_USER_ID = "userId"
        private const val COL_INCOME_DESCRIPTION = "description"
        private const val COL_INCOME_AMOUNT = "amount"
        private const val COL_INCOME_CYCLE_TYPE = "cycleType"
        private const val COL_INCOME_CYCLE_START_DAY = "cycleStartDay"
        private const val COL_INCOME_START_DATE = "startDate"
        private const val COL_INCOME_END_DATE = "endDate"
        private const val COL_INCOME_IS_ACTIVE = "isActive"

    }

    // This gets called when the app installs the database for the first time
    override fun onCreate(db: SQLiteDatabase) {
        Log.d(TAG, "Creating database tables")

        val createUsersTable = """
        CREATE TABLE $TABLE_USERS (
            $COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_USERNAME TEXT UNIQUE,
            $COL_PASSWORD TEXT
        );""".trimIndent()

        val createCategoriesTable = """
        CREATE TABLE $TABLE_CATEGORIES (
            $COL_CATEGORY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_CATEGORY_USER_ID INTEGER,
            $COL_CATEGORY_TITLE TEXT,
            $COL_CATEGORY_DESCRIPTION TEXT,
            $COL_CATEGORY_AMOUNT REAL,
            FOREIGN KEY ($COL_CATEGORY_USER_ID) REFERENCES $TABLE_USERS($COL_USER_ID)
        );""".trimIndent()

        val createTransactionsTable = """
        CREATE TABLE $TABLE_TRANSACTIONS (
            $COL_TRANSACTION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_TRANSACTION_USER_ID INTEGER,
            $COL_TRANSACTION_CATEGORY_ID INTEGER,
            $COL_TRANSACTION_TITLE TEXT,
            $COL_TRANSACTION_DESCRIPTION TEXT,
            $COL_TRANSACTION_AMOUNT REAL,
            $COL_TRANSACTION_START TEXT,
            $COL_TRANSACTION_END TEXT,
            $COL_TRANSACTION_IMAGE TEXT,
            FOREIGN KEY ($COL_TRANSACTION_USER_ID) REFERENCES $TABLE_USERS($COL_USER_ID),
            FOREIGN KEY ($COL_TRANSACTION_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COL_CATEGORY_ID)
        );""".trimIndent()

        val createIncomeTable = """
        CREATE TABLE $TABLE_INCOME (
        $COL_INCOME_ID INTEGER PRIMARY KEY AUTOINCREMENT,
        $COL_INCOME_USER_ID INTEGER,
        $COL_INCOME_DESCRIPTION TEXT,
        $COL_INCOME_AMOUNT REAL,
        $COL_INCOME_CYCLE_TYPE TEXT,
        $COL_INCOME_CYCLE_START_DAY INTEGER,
        $COL_INCOME_START_DATE TEXT,
        $COL_INCOME_END_DATE TEXT,
        $COL_INCOME_IS_ACTIVE INTEGER,
        FOREIGN KEY ($COL_INCOME_USER_ID) REFERENCES $TABLE_USERS($COL_USER_ID)
    );
""".trimIndent()

        db.execSQL(createIncomeTable)
        db.execSQL(createUsersTable)
        db.execSQL(createCategoriesTable)
        db.execSQL(createTransactionsTable)
        Log.i(TAG, "Tables created successfully")
    }

    // This is used when the schema version changes
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.w(TAG, "Upgrading DB from $oldVersion to $newVersion. Dropping all tables.")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRANSACTIONS")
        onCreate(db)
    }

    // ==============================================
    // USER FUNCTIONS
    // ==============================================

    // Registers a new user — returns true if success, false if failed (duplicate or error)
    fun registerUser(username: String, password: String): RegisterResult {
        val db = writableDatabase

        // Basic input validation
        if (username.isBlank()) {
            Log.w("DatabaseHelper", "Registration failed — username is blank")
            return RegisterResult.Failure("Username cannot be empty")
        }

        if (password.length < 6) {
            Log.w("DatabaseHelper", "Registration failed — password too short for '$username'")
            return RegisterResult.Failure("Password must be at least 6 characters long")
        }

        // Check for duplicates
        val checkCursor = db.rawQuery(
            "SELECT $COL_USER_ID FROM $TABLE_USERS WHERE $COL_USERNAME=?",
            arrayOf(username)
        )

        if (checkCursor.moveToFirst()) {
            checkCursor.close()
            Log.w("DatabaseHelper", "Registration failed — duplicate username '$username'")
            return RegisterResult.Failure("Username already exists")
        }
        checkCursor.close()

        val values = ContentValues().apply {
            put(COL_USERNAME, username)
            put(COL_PASSWORD, password)
        }

        return try {
            val rowId = db.insertOrThrow(TABLE_USERS, null, values)
            Log.i("DatabaseHelper", "User registered successfully. Username: $username | Row ID: $rowId")
            RegisterResult.Success
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Unexpected error during registration of user '$username'", e)
            RegisterResult.Failure("Unknown error occurred")
        }
    }


    // Validates username/password. Returns user ID if found, otherwise -1
    fun validateUser(username: String, password: String): Int {
        if (username.isBlank() || password.isBlank()) {
            Log.w("DatabaseHelper", "Login failed — empty username or password.")
            return -1
        }

        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT $COL_USER_ID FROM $TABLE_USERS WHERE $COL_USERNAME=? AND $COL_PASSWORD=?",
            arrayOf(username, password)
        )

        val userId = if (cursor.moveToFirst()) cursor.getInt(0) else -1
        cursor.close()

        if (userId != -1) {
            Log.i("DatabaseHelper", "Login successful for user: '$username' (User ID: $userId)")
        } else {
            Log.w("DatabaseHelper", "Login failed for user: '$username' — incorrect credentials")
        }

        return userId
    }


    // ==============================================
    // CATEGORY FUNCTIONS
    // ==============================================

    // Adds a category for a user — title, description, amount limit
    fun insertCategory(category: CategoryItem): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CATEGORY_USER_ID, category.userId)
            put(COL_CATEGORY_TITLE, category.title)
            put(COL_CATEGORY_DESCRIPTION, category.description)
            put(COL_CATEGORY_AMOUNT, category.amount)
        }
        val result = db.insert(TABLE_CATEGORIES, null, values)
        Log.i(TAG, "User ${category.userId} created a new category: '${category.title}' | Insert ID: $result")
        return result > 0
    }

    // Pull all categories for a given user ID
    fun getCategoriesForUser(userId: Int): List<CategoryItem> {
        val db = readableDatabase
        val list = mutableListOf<CategoryItem>()
        val cursor = db.rawQuery("SELECT * FROM $TABLE_CATEGORIES WHERE $COL_CATEGORY_USER_ID=?", arrayOf(userId.toString()))
        if (cursor.moveToFirst()) {
            do {
                list.add(CategoryItem(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CATEGORY_ID)),
                    userId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CATEGORY_USER_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY_TITLE)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY_DESCRIPTION)),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_CATEGORY_AMOUNT))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        Log.d(TAG, "Fetched ${list.size} categories for user $userId")
        return list
    }

    // ==============================================
    // TRANSACTION FUNCTIONS
    // ==============================================

    // Adds a transaction with all fields including optional image
    fun insertTransaction(userId: Int, categoryId: Int, title: String, description: String, amount: Double, startDate: String, endDate: String, imageUri: String?): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TRANSACTION_USER_ID, userId)
            put(COL_TRANSACTION_CATEGORY_ID, categoryId)
            put(COL_TRANSACTION_TITLE, title)
            put(COL_TRANSACTION_DESCRIPTION, description)
            put(COL_TRANSACTION_AMOUNT, amount)
            put(COL_TRANSACTION_START, startDate)
            put(COL_TRANSACTION_END, endDate)
            put(COL_TRANSACTION_IMAGE, imageUri)
        }
        val result = db.insert(TABLE_TRANSACTIONS, null, values)
        Log.i(TAG, "Transaction saved. User: $userId | Title: '$title' | Amount: $amount | Success: ${result > 0}")
        return result > 0
    }

    // Fetch all transactions for a user
    fun getTransactionsForUser(userId: Int): List<TransactionItem> {
        val db = readableDatabase
        val list = mutableListOf<TransactionItem>()
        val cursor = db.rawQuery("SELECT * FROM $TABLE_TRANSACTIONS WHERE $COL_TRANSACTION_USER_ID=?", arrayOf(userId.toString()))
        if (cursor.moveToFirst()) {
            do {
                list.add(TransactionItem(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTION_ID)),
                    userId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTION_USER_ID)),
                    categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTION_CATEGORY_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTION_TITLE)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTION_DESCRIPTION)),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TRANSACTION_AMOUNT)),
                    startDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTION_START)),
                    endDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTION_END)),
                    imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTION_IMAGE))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        Log.d(TAG, "User $userId — fetched ${list.size} transactions")
        return list
    }

    // Remove category by ID (admin or user action)
    fun deleteCategory(categoryId: Int): Boolean {
        val db = writableDatabase
        val result = db.delete(TABLE_CATEGORIES, "$COL_CATEGORY_ID=?", arrayOf(categoryId.toString()))
        Log.i(TAG, "Category $categoryId deleted. Success: ${result > 0}")
        return result > 0
    }

    // Filter transactions between two dates — inclusive or strict
    fun getTransactionsForUserByDate(userId: Int, startDate: String, endDate: String, inclusive: Boolean): List<TransactionItem> {
        val db = readableDatabase
        val list = mutableListOf<TransactionItem>()
        val query = if (inclusive) {
            """SELECT * FROM $TABLE_TRANSACTIONS 
               WHERE $COL_TRANSACTION_USER_ID=? 
               AND ($COL_TRANSACTION_START <= ? AND $COL_TRANSACTION_END >= ?)"""
        } else {
            """SELECT * FROM $TABLE_TRANSACTIONS 
               WHERE $COL_TRANSACTION_USER_ID=? 
               AND $COL_TRANSACTION_START >= ? AND $COL_TRANSACTION_END <= ?"""
        }
        val cursor = db.rawQuery(query, arrayOf(userId.toString(), endDate, startDate))
        if (cursor.moveToFirst()) {
            do {
                list.add(TransactionItem(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTION_ID)),
                    userId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTION_USER_ID)),
                    categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTION_CATEGORY_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTION_TITLE)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTION_DESCRIPTION)),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TRANSACTION_AMOUNT)),
                    startDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTION_START)),
                    endDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTION_END)),
                    imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTION_IMAGE))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        Log.d(TAG, "User $userId — filtered ${list.size} transactions from $startDate to $endDate | Inclusive: $inclusive")
        return list
    }


    // ============================================================
// insertIncome()
// Purpose: Inserts a new income record into the income table
// ============================================================
    fun insertIncome(income: IncomeItem): Boolean {
        val db = writableDatabase

        // Prepare data for insertion into the income table
        val values = ContentValues().apply {
            put(COL_INCOME_USER_ID, income.userId)
            put(COL_INCOME_DESCRIPTION, income.description)
            put(COL_INCOME_AMOUNT, income.amount)
            put(COL_INCOME_CYCLE_TYPE, income.cycleType.name)
            put(COL_INCOME_CYCLE_START_DAY, income.cycleStartDay)
            put(COL_INCOME_START_DATE, income.startDate.toString())
            put(COL_INCOME_END_DATE, income.endDate?.toString())
            put(COL_INCOME_IS_ACTIVE, if (income.isActive) 1 else 0)
        }

        val result = db.insert(TABLE_INCOME, null, values)

        // Log the result for this action
        Log.i(TAG, "User ${income.userId} | Created income: '${income.description}' | Amount: R${income.amount} | Cycle: ${income.cycleType} | InsertID: $result")

        return result > 0
    }
    // ===================================================================
// getIncomesForUser()
// Purpose: Fetch all income records for a specific user from DB
// ===================================================================
    fun getIncomesForUser(userId: Int): List<IncomeItem> {
        val db = readableDatabase
        val list = mutableListOf<IncomeItem>()

        // SQL query to fetch all incomes for the user
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_INCOME WHERE $COL_INCOME_USER_ID=?",
            arrayOf(userId.toString())
        )

        // Loop through and convert each row to an IncomeItem object
        if (cursor.moveToFirst()) {
            do {
                val income = IncomeItem(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_INCOME_ID)),
                    userId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_INCOME_USER_ID)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COL_INCOME_DESCRIPTION)),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_INCOME_AMOUNT)),
                    cycleType = CycleType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COL_INCOME_CYCLE_TYPE))),
                    cycleStartDay = cursor.getInt(cursor.getColumnIndexOrThrow(COL_INCOME_CYCLE_START_DAY)),
                    startDate = java.time.LocalDate.parse(cursor.getString(cursor.getColumnIndexOrThrow(COL_INCOME_START_DATE))),
                    endDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_INCOME_END_DATE))?.let { java.time.LocalDate.parse(it) },
                    isActive = cursor.getInt(cursor.getColumnIndexOrThrow(COL_INCOME_IS_ACTIVE)) == 1
                )
                list.add(income)
            } while (cursor.moveToNext())
        }

        cursor.close()

        // 🪵 Log how many incomes we pulled and for who
        Log.d(TAG, "User $userId | Retrieved ${list.size} incomes from DB")

        return list
    }
    // ============================================================
// updateIncome()
// Purpose: Updates an existing income by ending the old one and inserting a new one
// Used when users get a raise, new job terms, or change cycle.
// ============================================================
    fun updateIncome(oldIncome: IncomeItem, newIncome: IncomeItem): Boolean {
        val db = writableDatabase
        val today = java.time.LocalDate.now().toString()

        // === Step 1: Close the old income
        val updateValues = ContentValues().apply {
            put(COL_INCOME_END_DATE, today)
            put(COL_INCOME_IS_ACTIVE, 0)
        }

        val rowsAffected = db.update(
            TABLE_INCOME,
            updateValues,
            "$COL_INCOME_ID=?",
            arrayOf(oldIncome.id.toString())
        )

        Log.i(TAG, "User ${oldIncome.userId} | Closed income: '${oldIncome.description}' | OldID: ${oldIncome.id} | Rows affected: $rowsAffected")

        // === Step 2: Insert the new income record (start today)
        val newRecord = newIncome.copy(startDate = java.time.LocalDate.now())
        val inserted = insertIncome(newRecord)

        Log.i(TAG, "User ${newIncome.userId} | Inserted updated income: '${newIncome.description}' | New Amount: R${newIncome.amount} | Success: $inserted")

        return rowsAffected > 0 && inserted
    }
    // ============================================================
// deleteIncome()
// Purpose: Soft-deletes income by setting endDate and isActive = false
// Used when user leaves a job or stops receiving a specific income
// ============================================================
    fun deleteIncome(incomeId: Int, endNow: Boolean): Boolean {
        val db = writableDatabase
        val today = java.time.LocalDate.now().toString()

        // If endNow = true, this month is deactivated. Else, starts from next.
        val endDate = if (endNow) today else java.time.LocalDate.now().plusDays(1).toString()

        val values = ContentValues().apply {
            put(COL_INCOME_END_DATE, endDate)
            put(COL_INCOME_IS_ACTIVE, 0)
        }

        val result = db.update(
            TABLE_INCOME,
            values,
            "$COL_INCOME_ID=?",
            arrayOf(incomeId.toString())
        )

        Log.w(TAG, "Income $incomeId | Soft-deleted | End now: $endNow | Effective end date: $endDate | Result: $result")
        return result > 0
    }

}
