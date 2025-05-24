package com.centsnippers.models

// Used to give proper feedback after registration attempts
sealed class RegisterResult {
    object Success : RegisterResult()
    data class Failure(val reason: String) : RegisterResult()
}
