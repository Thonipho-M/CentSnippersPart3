package com.centsnippers.fragments

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.centsnippers.MainActivity
import com.centsnippers.R
import com.centsnippers.adapters.CategorySummaryAdapter
import com.centsnippers.databinding.FragmentDashboardBinding
import com.centsnippers.models.CycleType
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        if (userId == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.dashboardTitle.text = "Snapshot for ${monthFormat.format(Date())}"

        loadDashboardData()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadDashboardData() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val now = Calendar.getInstance()
        val today = now.time

        val startOfMonth = Calendar.getInstance().apply {
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

        FirebaseCategoryService.getCategoriesForUser { allCategories ->
            FirebaseTransactionService.getTransactions { allTransactions ->
                FirebaseIncomeService.getIncomes { allIncomes ->

                    // ✅ Safe check before accessing UI (to prevent crashes after view is destroyed)
                    if (_binding == null || !isAdded) return@getIncomes

                    // === Step 1: Filter incomes ===
                    val income = allIncomes.filter { income ->
                        val start = Date(income.startDate.year - 1900, income.startDate.monthValue - 1, income.startDate.dayOfMonth)
                        val end = income.endDate?.let { Date(it.year - 1900, it.monthValue - 1, it.dayOfMonth) }

                        !start.after(today) && (end == null || !end.before(today)) && income.isActive
                    }.sumOf {
                        when (it.cycleType) {
                            CycleType.ONCE -> it.amount
                            CycleType.MONTHLY -> it.amount
                            CycleType.YEARLY -> it.amount / 12.0
                        }
                    }

                    // === Step 2: Filter transactions ===
                    val filteredTransactions = allTransactions.filter {
                        try {
                            val txnDate = sdf.parse(it.Date)
                            txnDate != null && !txnDate.before(startOfMonth) && !txnDate.after(endOfMonth)
                        } catch (e: Exception) {
                            false
                        }
                    }

                    // === Step 3: Calculate total expenses ===
                    var totalHold = 0.0
                    for (txn in filteredTransactions) {
                        when (txn.type.lowercase(Locale.getDefault())) {
                            "expense" -> totalHold += txn.amount
                            "refund" -> totalHold -= txn.amount
                        }
                    }

                    val remaining = income - totalHold

                    // === Step 4: Show totals in UI ===
                    binding.totalIncomeText.text = "Total Income: R%.2f".format(income)
                    binding.totalExpensesText.text = "Total Expenses: R%.2f".format(totalHold)
                    binding.remainingBudgetText.text = "Remaining Budget: R%.2f".format(remaining)

                    // === Step 5: Top Categories ===
                    val topCategories = allCategories.map { category ->
                        val categoryTxns = filteredTransactions.filter { txn -> txn.categoryId == category.id }
                        val totalSpent = categoryTxns.sumOf { txn -> txn.amount }
                        category.copy(
                            totalSpent = totalSpent,
                            transactionCount = categoryTxns.size
                        )
                    }.sortedByDescending { it.totalSpent }
                        .take(3)

                    binding.topCategoriesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                    binding.topCategoriesRecyclerView.adapter = CategorySummaryAdapter(topCategories)

                    // === Step 6: Recent Transactions ===
                    val recentTxns = filteredTransactions.sortedByDescending {
                        try {
                            sdf.parse(it.Date)?.time ?: 0L
                        } catch (e: Exception) {
                            0L
                        }
                    }.take(3)

                    val recentAdapter = CategorySummaryAdapter(
                        recentTxns.mapNotNull { txn ->
                            val category = allCategories.find { it.id == txn.categoryId }
                            category?.copy(
                                totalSpent = txn.amount,
                                transactionCount = 1,
                                title = "${category.title} (Recent)"
                            )
                        }
                    )

                    binding.recentTransactionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                    binding.recentTransactionsRecyclerView.adapter = recentAdapter
                }
            }
        }
    }

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