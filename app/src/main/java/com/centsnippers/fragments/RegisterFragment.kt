package com.centsnippers.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.centsnippers.R
import com.centsnippers.data.DatabaseHelper
import com.centsnippers.databinding.FragmentRegisterBinding
import com.centsnippers.models.RegisterResult
import com.centsnippers.utils.SessionManager

/**
 * RegisterFragment
 * ------------------
 * Handles user registration logic with input validation,
 * password confirmation, session handling, and onboarding flag setup.
 */
class RegisterFragment : Fragment() {

    // ViewBinding for register layout
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    // DatabaseHelper instance
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        dbHelper = DatabaseHelper(requireContext())

        Log.d("RegisterFragment", "Register screen initialized")

        // Handle register button logic
        binding.registerButton.setOnClickListener {
            handleRegistration()
        }

        return binding.root
    }

    /**
     * handleRegistration()
     * ---------------------
     * Validates input fields, ensures password match,
     * registers user, saves session and onboarding flag.
     */
    private fun handleRegistration() {
        val username = binding.usernameEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

        Log.d("RegisterFragment", "Attempting registration for username=$username")

        // Validate inputs
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            Log.w("RegisterFragment", "Empty fields detected")
            return
        }

        if (password.length < 6) {
            Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            Log.w("RegisterFragment", "Password too short")
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
            Log.w("RegisterFragment", "Password mismatch")
            return
        }

        try {
            val result = dbHelper.registerUser(username, password)

            when (result) {
                is RegisterResult.Success -> {
                    val userId = dbHelper.validateUser(username, password)
                    SessionManager(requireContext()).saveUserSession(userId)
                    Log.i("RegisterFragment", "Registration successful, userId=$userId")

                    // Set onboarding flag for new users
                    val prefs = requireContext().getSharedPreferences("prefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("hasSeenOnboarding", true).apply()
                    Log.d("RegisterFragment", "Onboarding flag set to true")

                    Toast.makeText(requireContext(), "Registration successful!", Toast.LENGTH_SHORT).show()
                    showWelcomeDialog()
                    findNavController().navigate(R.id.dashboardFragment)
                }

                is RegisterResult.Failure -> {
                    Toast.makeText(requireContext(), "Error: ${result.reason}", Toast.LENGTH_LONG).show()
                    Log.e("RegisterFragment", "Registration failed: ${result.reason}")
                }
            }

        } catch (e: Exception) {
            Toast.makeText(context, "An error occurred: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("RegisterFragment", "Exception during registration", e)
        }
    }

    private fun showWelcomeDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Welcome to CentSnippers")
            .setMessage(
                """
            CentSnippers is designed to help you develop better financial habits and work towards saving more each month through mindful spending.

            Before you begin, make sure to create your categories. Each category includes a minimum spend, a target goal, and a maximum spend — these are your personal budgeting boundaries and guide your progress.

            As you log transactions and income, the app will provide visual breakdowns and trends to help you stay on track. You'll also have access to educational resources aimed at improving your budgeting skills over time.

            Start by setting up your categories to get the most out of CentSnippers.
            """.trimIndent()
            )
            .setPositiveButton("Get Started") { dialog, _ ->
                dialog.dismiss()
                findNavController().navigate(R.id.dashboardFragment)
            }
            .setCancelable(false)
            .show()
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("RegisterFragment", "View destroyed and binding cleared")
    }
}
