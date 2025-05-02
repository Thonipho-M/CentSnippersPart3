package com.centsnippers.fragments

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.centsnippers.adapters.BudgetAdapter
import com.centsnippers.data.DatabaseHelper
import com.centsnippers.databinding.FragmentBudgetBinding
import com.centsnippers.models.BudgetItem
import com.centsnippers.utils.SessionManager
import com.centsnippers.MainActivity
import com.centsnippers.R

class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var budgetAdapter: BudgetAdapter
    private var userId: Int = -1
    private var selectedImageUri: Uri? = null
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
        dialogImageView?.setImageURI(uri)
        dialogImageView?.visibility = View.VISIBLE
    }
    private var dialogImageView: ImageView? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize database and session manager
        dbHelper = DatabaseHelper(requireContext())
        sessionManager = SessionManager(requireContext())
        userId = sessionManager.getUserId()

        // If no user is logged in, show a message
        if (userId == -1) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Setup RecyclerView for budget list
        budgetAdapter = BudgetAdapter(mutableListOf(),
            onDelete = { position -> deleteBudgetItem(position) }
        )
        binding.budgetRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.budgetRecyclerView.adapter = budgetAdapter

        // Load budgets for the current user
        loadBudgets()

        // Handle add button click
        binding.addBudgetFab.setOnClickListener {
            showAddBudgetDialog()
        }

        // Handle logout button
        binding.logoutButton.setOnClickListener {
            sessionManager.clearSession()
            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireActivity(), MainActivity::class.java))
            requireActivity().finish()
        }
    }

    // Loads budgets from the database and updates the UI
    private fun loadBudgets() {
        val budgets = dbHelper.getBudgetsForUser(userId)
        budgetAdapter.updateList(budgets)
        val total = budgets.sumOf { it.amount }
        binding.totalBudgetTextView.text = "Total: R%.2f".format(total)
    }

    // Show a dialog to enter a new budget item
    private fun showAddBudgetDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_budget, null)
        val editCategory = dialogView.findViewById<EditText>(R.id.editCategory)
        val editAmount = dialogView.findViewById<EditText>(R.id.editAmount)
        val editDescription = dialogView.findViewById<EditText>(R.id.editDescription)
        val buttonSelectImage = dialogView.findViewById<Button>(R.id.buttonSelectImage)
        dialogImageView = dialogView.findViewById(R.id.selectedImageView)


        AlertDialog.Builder(requireContext())
            .setTitle("Add Budget")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val category = editCategory.text.toString().trim()
                val amount = editAmount.text.toString().trim().toDoubleOrNull()
                val description = editDescription.text.toString().trim()
                val imageUrl=selectedImageUri?.toString()

                if (category.isNotEmpty() && amount != null) {
                    val budgetItem = BudgetItem(0, userId, category, amount, description, imageUrl)
                    dbHelper.insertBudget(budgetItem)
                    loadBudgets()
                    selectedImageUri = null
                } else {
                    Toast.makeText(requireContext(), "Invalid input", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Deletes a budget item and refreshes list
    private fun deleteBudgetItem(position: Int) {
        val budgetList = dbHelper.getBudgetsForUser(userId)
        if (position in budgetList.indices) {
            val item = budgetList[position]
            dbHelper.deleteBudget(item.id)
            loadBudgets()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}


