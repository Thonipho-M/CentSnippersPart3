//Sessionmanager
package com.centsnippers.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("CentSnippersSession", Context.MODE_PRIVATE)

    fun saveUserSession(uid: String) {
        prefs.edit().putString("userId", uid).apply()
    }

    fun getUserId(): String {
        return prefs.getString("userId", "") ?: ""
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return getUserId().isNotEmpty()
    }
}
