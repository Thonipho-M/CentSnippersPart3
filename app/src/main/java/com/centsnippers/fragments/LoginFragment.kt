package com.centsnippers.fragments

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.centsnippers.R
import com.centsnippers.data.DatabaseHelper
import com.centsnippers.databinding.FragmentLoginBinding
import com.centsnippers.utils.SessionManager

/**
 * LoginFragment
 * ----------------
 * Handles user login UI and logic, displays welcome message,
 * and redirects to RegisterFragment or DashboardFragment.
 */
class LoginFragment : Fragment() {

    // ViewBinding for login layout
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // DatabaseHelper instance
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the view using ViewBinding
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        dbHelper = DatabaseHelper(requireContext())

        // Set up listeners for login and register navigation
        setupListeners()

        Log.d("LoginFragment", "Login screen initialized")
        return binding.root
    }

    /**
     * setupListeners()
     * -----------------
     * Binds onClick events to Login and Register buttons
     */
    private fun setupListeners() {
        binding.buttonLogin.setOnClickListener {
            handleLogin()
        }

        binding.textRegisterRedirect.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
            Log.i("LoginFragment", "Navigating to RegisterFragment")
        }
    }

    /**
     * handleLogin()
     * ---------------
     * Validates input, checks credentials, saves session, navigates to Dashboard on success
     */
    private fun handleLogin() {
        val username = binding.editTextUsername.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        Log.d("LoginFragment", "Login attempt for username=$username")

        // Input validation
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            Log.w("LoginFragment", "Missing username or password")
            return
        }

        // Validate credentials
        val userId = dbHelper.validateUser(username, password)
        if (userId != -1) {
            SessionManager(requireContext()).saveUserSession(userId)
            Log.i("LoginFragment", "Login successful. Session started for userId=$userId")
            findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
        } else {
            Toast.makeText(requireContext(), "Invalid credentials", Toast.LENGTH_SHORT).show()
            Log.e("LoginFragment", "Login failed. Invalid credentials for $username")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("LoginFragment", "View destroyed and binding cleared")
    }
}
