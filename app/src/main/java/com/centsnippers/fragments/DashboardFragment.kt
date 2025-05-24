// ===============================
// DashboardFragment.kt
// Purpose: Display real-time financial summary (income, expenses, remaining budget)
// ===============================

package com.centsnippers.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.centsnippers.MainActivity
import com.centsnippers.R
import com.centsnippers.adapters.CategorySummaryAdapter
import com.centsnippers.data.DatabaseHelper
import com.centsnippers.databinding.FragmentDashboardBinding
import com.centsnippers.utils.SessionManager
import java.text.SimpleDateFormat
import java.time.LocalDate
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

        // === Init Session + DB ===
        dbHelper = DatabaseHelper(requireContext())
        sessionManager = SessionManager(requireContext())
        userId = sessionManager.getUserId()
        Log.d("DashboardFragment", "USER ID: $userId | ACTION: Fragment launched")

        // === Month Display ===
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val currentMonth = monthFormat.format(Date())
        binding.dashboardTitle.text = "Snapshot for $currentMonth"

        // === Check if user is logged in ===
        if (userId == -1) {
            binding.totalIncomeText.text = "Not logged in"
            Log.e("DashboardFragment", "USER ID: unknown | ACTION: Attempted to access dashboard without login")
            return
        }

        // === Get Transactions & Categories ===
        val transactions = dbHelper.getTransactionsForUser(userId)
        val allCategories = dbHelper.getCategoriesForUser(userId)
        val allTransactions = dbHelper.getTransactionsForUser(userId)
        Log.d("DashboardFragment", "USER ID: $userId | ACTION: Pulled ${transactions.size} transactions, ${allCategories.size} categories")

        // === Map categories to spending summaries ===
        val categorySummaries = allCategories.map { category ->
            val relatedTransactions = allTransactions.filter { it.categoryId == category.id }
            val totalSpent = relatedTransactions.sumOf { it.amount }
            category.copy(
                totalSpent = totalSpent,
                transactionCount = relatedTransactions.size
            )
        }

        // === RecyclerView Setup ===
        // grouped using `with` – no duplicate assignments
        with(binding.summaryRecyclerView) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = CategorySummaryAdapter(categorySummaries)
        }

        // === Set Click to Update Income ===
        binding.totalIncomeText.setOnClickListener {
            Log.d("DashboardFragment", "USER ID: $userId | ACTION: Clicked income label to update income")
            showIncomeDialog()
        }

        // === Calculate Budget Summary ===
        // === Pull active incomes from DB ===
        val incomeList = dbHelper.getIncomesForUser(userId).filter { it.isActive }
        val now = LocalDate.now()

        val income = incomeList
            .filter { it.startDate <= now && (it.endDate == null || it.endDate >= now) }
            .sumOf { it.amount }

        Log.i("DashboardFragment", "User $userId | Calculated income from income table: R$income")

        val expenses = transactions.filter { it.amount < 0 }.sumOf { it.amount } * -1
        val remaining = income - expenses

        Log.i("DashboardFragment", "USER ID: $userId | ACTION: Calculated budget | Income: R$income | Expenses: R$expenses | Remaining: R$remaining")

        // === Show Summary on UI ===
        binding.totalIncomeText.text = "Total Income: R%.2f".format(income)
        binding.totalExpensesText.text = "Total Expenses: R%.2f".format(expenses)
        binding.remainingBudgetText.text = "Remaining Budget: R%.2f".format(remaining)
    }

    private fun loadDashboardData() {
        // used after income is manually updated
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
            R.id.incomeFragment -> {
                findNavController().navigate(R.id.incomeFragment)
                Log.d("DashboardFragment", "User $userId | Navigated to IncomeFragment via top menu")
                true
            }

            R.id.logoutButton -> {
                val sessionManager = SessionManager(requireContext())
                sessionManager.clearSession()
                Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
                Log.i("DashboardFragment", "USER ID: $userId | ACTION: Logged out")
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
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        builder.setView(input)

        builder.setPositiveButton("Save") { _, _ ->
            val value = input.text.toString().toDoubleOrNull()
            if (value != null) {
                sessionManager.saveIncome(value)
                Log.i("DashboardFragment", "USER ID: $userId | ACTION: Income updated to R$value via income dialog")
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
