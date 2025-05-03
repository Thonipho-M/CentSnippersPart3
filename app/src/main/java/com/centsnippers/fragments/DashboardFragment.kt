// ===============================
// DashboardFragment.kt
// Purpose: Display real-time financial summary (income, expenses, remaining budget)
// ===============================

package com.centsnippers.fragments
import androidx.recyclerview.widget.LinearLayoutManager
import com.centsnippers.adapters.CategorySummaryAdapter
import android.app.AlertDialog
import android.widget.EditText


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.centsnippers.databinding.FragmentDashboardBinding
import com.centsnippers.data.DatabaseHelper
import com.centsnippers.utils.SessionManager
//Add the top right menu
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import android.content.Intent
import androidx.navigation.fragment.findNavController
import com.centsnippers.MainActivity
import com.centsnippers.R

//for current date

import java.text.SimpleDateFormat
import java.util.*




class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private var userId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        // Initialize helpers
        dbHelper = DatabaseHelper(requireContext())
        sessionManager = SessionManager(requireContext())
        userId = sessionManager.getUserId()

        //set date for current date
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val currentMonth = monthFormat.format(Date())
        binding.dashboardTitle.text = "Snapshot for $currentMonth"


        if (userId == -1) {
            binding.totalIncomeText.text = "Not logged in"
            return
        }

        // Fetch all transactions for the logged-in user
        val transactions = dbHelper.getTransactionsForUser(userId)
// Fetch categories and transactions
        val allCategories = dbHelper.getCategoriesForUser(userId)
        val allTransactions = dbHelper.getTransactionsForUser(userId)

// Map each category to include total spent and transaction count
        val categorySummaries = allCategories.map { category ->
            val relatedTransactions = allTransactions.filter { it.categoryId == category.id }
            val totalSpent = relatedTransactions.sumOf { it.amount }
            category.copy(
                totalSpent = totalSpent,
                transactionCount = relatedTransactions.size
            )
        }

// Update the summary RecyclerView
        binding.summaryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.summaryRecyclerView.adapter = CategorySummaryAdapter(categorySummaries)

        binding.totalIncomeText.setOnClickListener {
            showIncomeDialog()
        }


        // Calculate total income and expenses
        val income = transactions.filter { it.amount > 0 }.sumOf { it.amount }
        val expenses = transactions.filter { it.amount < 0 }.sumOf { it.amount } * -1
        val remaining = income - expenses

        // Update the UI with real-time values
        binding.totalIncomeText.text = "Total Income: R%.2f".format(income)
        binding.totalExpensesText.text = "Total Expenses: R%.2f".format(expenses)
        binding.remainingBudgetText.text = "Remaining Budget: R%.2f".format(remaining)
// Summary Adapter
        val categories = dbHelper.getCategoriesForUser(userId)
        val adapter = CategorySummaryAdapter(categories)
        binding.summaryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.summaryRecyclerView.adapter = CategorySummaryAdapter(categorySummaries)



    }
    private fun loadDashboardData() {
        val income = sessionManager.getIncome()
        val transactions = dbHelper.getTransactionsForUser(userId)
        val expenses = transactions.sumOf { it.amount }
        val remaining = income - expenses

        binding.totalIncomeText.text = "Total Income: R%.2f".format(income)
        binding.totalExpensesText.text = "Expenses: R%.2f".format(expenses)
        binding.remainingBudgetText.text = "Remaining: R%.2f".format(remaining)


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.goalsFragment -> {
                findNavController().navigate(R.id.goalsFragment)
                true
            }
            R.id.rewardsFragment -> {
                findNavController().navigate(R.id.rewardsFragment)
                true
            }
            R.id.helpPage -> {
                findNavController().navigate(R.id.helpPage)
                true
            }
            R.id.logoutButton -> {
                val sessionManager = SessionManager(requireContext())
                sessionManager.clearSession()
                Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.top_right_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun showIncomeDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Enter Your Total Income")

        val input = EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        builder.setView(input)

        builder.setPositiveButton("Save") { _, _ ->
            val value = input.text.toString().toDoubleOrNull()
            if (value != null) {
                sessionManager.saveIncome(value)
                loadDashboardData()
            } else {
                Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
