// ===============================
// DatabaseHelper.kt
// Updated: Refactor Budget -> Category, add Transactions table
// ===============================

package com.centsnippers.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.centsnippers.models.CategoryItem
import com.centsnippers.models.TransactionItem

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "CentSnippers.db"
        private const val DATABASE_VERSION = 1

        // USERS TABLE
        private const val TABLE_USERS = "users"
        private const val COL_USER_ID = "id"
        private const val COL_USERNAME = "username"
        private const val COL_PASSWORD = "password"

        // CATEGORIES TABLE
        private const val TABLE_CATEGORIES = "categories"
        private const val COL_CATEGORY_ID = "id"
        private const val COL_CATEGORY_USER_ID = "userId"
        private const val COL_CATEGORY_TITLE = "title"
        private const val COL_CATEGORY_DESCRIPTION = "description"
        private const val COL_CATEGORY_AMOUNT = "amount"

        // TRANSACTIONS TABLE
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
    }

    override fun onCreate(db: SQLiteDatabase) {
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

        db.execSQL(createUsersTable)
        db.execSQL(createCategoriesTable)
        db.execSQL(createTransactionsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRANSACTIONS")
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

    fun validateUser(username: String, password: String): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT $COL_USER_ID FROM $TABLE_USERS WHERE $COL_USERNAME=? AND $COL_PASSWORD=?", arrayOf(username, password))
        val userId = if (cursor.moveToFirst()) cursor.getInt(0) else -1
        cursor.close()
        return userId
    }

    // =============== CATEGORY FUNCTIONS ===============

    fun insertCategory(category: CategoryItem): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CATEGORY_USER_ID, category.userId)
            put(COL_CATEGORY_TITLE, category.title)
            put(COL_CATEGORY_DESCRIPTION, category.description)
            put(COL_CATEGORY_AMOUNT, category.amount)
        }
        return db.insert(TABLE_CATEGORIES, null, values) > 0
    }

    fun getCategoriesForUser(userId: Int): List<CategoryItem> {
        val db = readableDatabase
        val list = mutableListOf<CategoryItem>()
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_CATEGORIES WHERE $COL_CATEGORY_USER_ID=?",
            arrayOf(userId.toString())
        )
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    CategoryItem(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CATEGORY_ID)),
                        userId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CATEGORY_USER_ID)),
                        title = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY_TITLE)),
                        description = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY_DESCRIPTION)),
                        amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_CATEGORY_AMOUNT))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }


    // =============== TRANSACTION FUNCTIONS ===============

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
        return db.insert(TABLE_TRANSACTIONS, null, values) > 0
    }

    fun getTransactionsForUser(userId: Int): List<TransactionItem> {
        val db = readableDatabase
        val list = mutableListOf<TransactionItem>()
        val cursor = db.rawQuery("SELECT * FROM $TABLE_TRANSACTIONS WHERE $COL_TRANSACTION_USER_ID=?", arrayOf(userId.toString()))
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    TransactionItem(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTION_ID)),
                        userId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTION_USER_ID)),
                        categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTION_CATEGORY_ID)),
                        title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTION_TITLE)),
                        description = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTION_DESCRIPTION)),
                        amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TRANSACTION_AMOUNT)),
                        startDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTION_START)),
                        endDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTION_END)),
                        imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTION_IMAGE))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
    fun deleteCategory(categoryId: Int): Boolean {
        val db = writableDatabase
        return db.delete(TABLE_CATEGORIES, "$COL_CATEGORY_ID=?", arrayOf(categoryId.toString())) > 0
    }
    fun getTransactionsForUserByDate(userId: Int, startDate: String, endDate: String, inclusive: Boolean): List<TransactionItem> {
        val db = readableDatabase
        val list = mutableListOf<TransactionItem>()
        val query = if (inclusive) {
            """SELECT * FROM transactions 
           WHERE userId=? 
           AND (startDate <= ? AND endDate >= ?)"""
        } else {
            """SELECT * FROM transactions 
           WHERE userId=? 
           AND startDate >= ? AND endDate <= ?"""
        }
        val cursor = db.rawQuery(query, arrayOf(userId.toString(), endDate, startDate))
        if (cursor.moveToFirst()) {
            do {
                list.add(TransactionItem(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    userId = cursor.getInt(cursor.getColumnIndexOrThrow("userId")),
                    categoryId = cursor.getInt(cursor.getColumnIndexOrThrow("categoryId")),
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                    startDate = cursor.getString(cursor.getColumnIndexOrThrow("startDate")),
                    endDate = cursor.getString(cursor.getColumnIndexOrThrow("endDate")),
                    imageUrl = cursor.getString(cursor.getColumnIndexOrThrow("imageUri"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }



}
