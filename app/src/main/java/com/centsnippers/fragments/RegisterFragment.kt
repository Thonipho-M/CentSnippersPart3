package com.centsnippers.fragments
import com.centsnippers.models.RegisterResult
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
                val result = dbHelper.registerUser(username, password)

                when (result) {
                    is RegisterResult.Success -> {
                        //  1. Get user ID immediately after successful registration
                        val userId = dbHelper.validateUser(username, password)

                        //  2. Save login session
                        SessionManager(requireContext()).saveUserSession(userId)

                        Toast.makeText(requireContext(), "Registration successful!", Toast.LENGTH_SHORT).show()

                        // 3. Navigate to dashboard or home screen
                        findNavController().navigate(R.id.dashboardFragment)
                    }

                    is RegisterResult.Failure -> {
                        Toast.makeText(requireContext(), "Error: ${result.reason}", Toast.LENGTH_LONG).show()
                    }
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
