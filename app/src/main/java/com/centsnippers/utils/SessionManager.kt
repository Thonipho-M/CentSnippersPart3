package com.centsnippers.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    fun saveUserSession(userId: Int) {
        val editor = prefs.edit()
        editor.putBoolean("is_logged_in", true)
        editor.putInt("user_id", userId)
        editor.apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    fun getUserId(): Int {
        return prefs.getInt("user_id", -1)
    }

    fun clearSession() {
        prefs.edit() { clear() }
    }
    fun saveIncome(income: Double) {
        val editor = prefs.edit()
        editor.putFloat("USER_INCOME", income.toFloat())
        editor.apply()
    }

    fun getIncome(): Double {
        return prefs.getFloat("USER_INCOME", 0.0f).toDouble()
    }

}
