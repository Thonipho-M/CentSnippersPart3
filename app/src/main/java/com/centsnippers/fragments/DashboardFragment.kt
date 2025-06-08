package com.centsnippers.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.centsnippers.MainActivity
import com.centsnippers.R
import com.centsnippers.adapters.CategorySummaryAdapter
import com.centsnippers.databinding.FragmentDashboardBinding
import com.centsnippers.models.CategoryItem
import com.centsnippers.models.TransactionItem
import com.centsnippers.utils.FirebaseCategoryService
import com.centsnippers.utils.FirebaseIncomeService
import com.centsnippers.utils.FirebaseTransactionService
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val userId get() = auth.currentUser?.uid

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        if (userId == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Show current month in the title
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.dashboardTitle.text = "Snapshot for ${monthFormat.format(Date())}"

        loadDashboardData()
    }

    /// Loads income, transactions, and categories from Firestore and updates UI
    private fun loadDashboardData() {
        FirebaseTransactionService.getTransactions { allTransactions ->
            FirebaseCategoryService.getCategories { allCategories ->
                FirebaseIncomeService.getActiveIncomes { incomeList ->

                    val calendar = Calendar.getInstance()
                    val startOfMonth = calendar.apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                    val endOfMonth = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.time

                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                    val filteredTransactions = allTransactions.filter {
                        val txnDate = try {
                            sdf.parse(it.Date)
                        } catch (e: Exception) {
                            null
                        }
                        txnDate != null && !txnDate.before(startOfMonth) && !txnDate.after(endOfMonth)
                    }

                    // Calculate total income (monthly adjusted)
                    val income = incomeList.sumOf { it.getMonthlyAmount() }

                    // Calculate expenses & remaining
                    val totalSpent = filteredTransactions.sumOf {
                        when (it.type.lowercase()) {
                            "expense" -> it.amount
                            "refund" -> -it.amount
                            else -> 0.0
                        }
                    }
                    val remaining = income - totalSpent

                    binding.totalIncomeText.text = "Total Income: R%.2f".format(income)
                    binding.totalExpensesText.text = "Total Expenses: R%.2f".format(totalSpent)
                    binding.remainingBudgetText.text = "Remaining Budget: R%.2f".format(remaining)

                    // ---- Top Categories ----
                    val categorySummaries = allCategories.map { category ->
                        val txns = filteredTransactions.filter { it.categoryId == category.id }
                        val spent = txns.sumOf { it.amount }
                        category.copy(
                            totalSpent = spent,
                            transactionCount = txns.size
                        )
                    }.sortedByDescending { it.totalSpent }.take(3)

                    binding.topCategoriesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                    binding.topCategoriesRecyclerView.adapter = CategorySummaryAdapter(categorySummaries)

                    // ---- Recent Transactions ----
                    val recentTxns = filteredTransactions.sortedByDescending {
                        try {
                            sdf.parse(it.Date)?.time ?: 0L
                        } catch (e: Exception) {
                            0L
                        }
                    }.take(3)

                    val recentSummaries = recentTxns.mapNotNull { txn ->
                        allCategories.find { it.id == txn.categoryId }?.copy(
                            title = "${txn.title} (${txn.type})",
                            totalSpent = txn.amount,
                            transactionCount = 1
                        )
                    }

                    binding.recentTransactionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                    binding.recentTransactionsRecyclerView.adapter = CategorySummaryAdapter(recentSummaries)
                }
            }
        }
    }

    /// Top-right menu logic
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.top_right_menu, menu)
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
                true
            }
            R.id.logoutButton -> {
                FirebaseAuth.getInstance().signOut()
                Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

