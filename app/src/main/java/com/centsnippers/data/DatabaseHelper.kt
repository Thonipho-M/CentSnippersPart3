package com.centsnippers.data


import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.centsnippers.models.BudgetItem

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "CentSnippers.db"
        private const val DATABASE_VERSION = 1

        // Users table
        private const val TABLE_USERS = "users"
        private const val COL_USER_ID = "id"
        private const val COL_USERNAME = "username"
        private const val COL_PASSWORD = "password"

        // Budgets table
        private const val TABLE_BUDGETS = "budgets"
        private const val COL_BUDGET_ID = "id"
        private const val COL_BUDGET_USER_ID = "userId"
        private const val COL_BUDGET_CATEGORY = "category"
        private const val COL_BUDGET_AMOUNT = "amount"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = """
        CREATE TABLE users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT UNIQUE,
            password TEXT
        );
    """.trimIndent()

        val createBudgetsTable = """
        CREATE TABLE budgets (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            userId INTEGER,
            category TEXT,
            amount REAL,
            FOREIGN KEY (userId) REFERENCES users(id)
        );
    """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createBudgetsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BUDGETS")
        onCreate(db)
    }

    // =============== USER FUNCTIONS ===============

    fun registerUser(username: String, password: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_USERNAME, username)
            put(COL_PASSWORD, password)
        }
        return try {
            db.insertOrThrow(TABLE_USERS, null, values) > 0
        } catch (e: Exception) {
            false
        }
    }

    fun loginUser(username: String, password: String): Int {
        val db = readableDatabase
        val query = "SELECT $COL_USER_ID FROM $TABLE_USERS WHERE $COL_USERNAME=? AND $COL_PASSWORD=?"
        val cursor = db.rawQuery(query, arrayOf(username, password))

        return if (cursor.moveToFirst()) {
            val userId = cursor.getInt(0)
            cursor.close()
            userId
        } else {
            cursor.close()
            -1
        }
    }

    fun validateUser(username: String, password: String): Int {
        val db = this.readableDatabase
        val query = "SELECT $COL_USER_ID FROM $TABLE_USERS WHERE $COL_USERNAME=? AND $COL_PASSWORD=?"
        val cursor = db.rawQuery(query, arrayOf(username, password))

        var userId = -1
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_ID))
        }

        cursor.close()
        db.close()
        return userId // Returns -1 if not found
    }


    // =============== BUDGET FUNCTIONS ===============

    fun insertBudget(budgetItem: BudgetItem): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_BUDGET_USER_ID, budgetItem.userId)
            put(COL_BUDGET_CATEGORY, budgetItem.category)
            put(COL_BUDGET_AMOUNT, budgetItem.amount)
        }
        return db.insert(TABLE_BUDGETS, null, values) > 0
    }

    fun getBudgetsForUser(userId: Int): List<BudgetItem> {
        val db = readableDatabase
        val list = mutableListOf<BudgetItem>()
        val query = "SELECT * FROM $TABLE_BUDGETS WHERE $COL_BUDGET_USER_ID=?"
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val budget = BudgetItem(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_BUDGET_ID)),
                    userId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_BUDGET_USER_ID)),
                    category = cursor.getString(cursor.getColumnIndexOrThrow(COL_BUDGET_CATEGORY)),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_BUDGET_AMOUNT))
                )
                list.add(budget)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deleteBudget(budgetId: Int): Boolean {
        val db = writableDatabase
        return db.delete(TABLE_BUDGETS, "$COL_BUDGET_ID=?", arrayOf(budgetId.toString())) > 0
    }
}
