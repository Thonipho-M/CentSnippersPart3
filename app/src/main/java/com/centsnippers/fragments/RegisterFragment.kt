package com.centsnippers.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.centsnippers.R
import com.centsnippers.data.DatabaseHelper
import com.centsnippers.databinding.FragmentRegisterBinding
import com.centsnippers.utils.SessionManager

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        dbHelper = DatabaseHelper(requireContext())

        // Handle Register Button
        binding.registerButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            // Validate input
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(context, "Password should be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val success = dbHelper.registerUser(username, password)
                if (success) {
                    Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()

                    // Optional: auto-login and go to dashboard
                    val userId = dbHelper.validateUser(username, password)
                    if (userId != -1) {
                        SessionManager(requireContext()).saveUserSession(userId)
                        findNavController().navigate(R.id.action_registerFragment_to_dashboardFragment)
                    } else {
                        // Fallback if something went wrong
                        findNavController().navigate(R.id.action_registerFragment_to_loginFragment2)
                    }

                } else {
                    Toast.makeText(context, "Username already exists", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(context, "An error occurred: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
