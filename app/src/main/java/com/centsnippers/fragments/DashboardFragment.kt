package com.centsnippers.fragments

// Usual imports to handle layout, nav, and utils
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import com.centsnippers.models.CycleType
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

/// Handles UI and data logic for displaying user's financial summary.
/// Loads income, expenses, top categories, and recent transactions for current month.
class DashboardFragment : Fragment() {

    // View binding setup to access XML views
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // Core utility and DB classes for data access and session handling
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private var userId: Int = -1 // Will hold the ID of the logged-in user

    /// Inflates the layout for this fragment using ViewBinding.
    /// Required before interacting with UI elements.
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    /// Called once the view is created. Initializes session, DB, and starts data loading.
    /// If no user is logged in, avoids crash and shows fallback message.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("DashboardFragment.onViewCreated", "DashboardFragment view created, starting setup")

        dbHelper = DatabaseHelper(requireContext())
        sessionManager = SessionManager(requireContext())
        userId = sessionManager.getUserId()
        Log.d("DashboardFragment.onViewCreated", "DashboardFragment using userId=$userId")

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val currentMonth = monthFormat.format(Date())
        binding.dashboardTitle.text = "Snapshot for $currentMonth"

        if (userId == -1) {
            binding.totalIncomeText.text = "Not logged in"
            Log.e("DashboardFragment.onViewCreated", "Attempted dashboard access without valid session")
            return
        }

        loadDashboardData()
    }

    /// Loads all dashboard data: active incomes, expenses, and transaction breakdown.
    /// Filters data to current month and updates UI accordingly.
    private fun loadDashboardData() {
        val netExpense = 0.0
        var totalHold = 0.0
        Log.d("DashboardFragment.loadDashboardData", "Started loading dashboard data for user $userId")

        val allTransactions = dbHelper.getTransactionsForUser(userId)
        val allCategories = dbHelper.getCategoriesForUser(userId)
        val incomeList = dbHelper.getIncomesForUser(userId).filter { it.isActive }

        val now = Calendar.getInstance()
        val today = now.time
        val currentMonth = now.get(Calendar.MONTH) + 1
        val currentYear = now.get(Calendar.YEAR)

        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        Log.d("DashboardFragment.loadDashboardData", "Current date=$today | Month=$currentMonth | Year=$currentYear | Active incomes=${incomeList.size}")

        val income = incomeList.sumOf { incomeItem ->
            try {
                val endDate = incomeItem.endDate
                Log.w("DEBUG_DATE", "Raw endDate=${incomeItem.endDate} | startDate=${incomeItem.startDate}")

                val endDateSql: java.sql.Date? = try {
                    endDate?.let { java.sql.Date.valueOf(it.toString()) }
                } catch (e: Exception) {
                    Log.e("DashboardFragment", "Invalid endDate for '${incomeItem.description}'", e)
                    null
                }

                val startCal = Calendar.getInstance().apply {
                    time = java.sql.Date.valueOf(incomeItem.startDate.toString())
                }

                val incomeStartMonth = startCal.get(Calendar.MONTH) + 1
                val incomeStartYear = startCal.get(Calendar.YEAR)

                val isActiveInMonth = endDateSql == null || endDateSql >= startOfMonth.time
                if (!isActiveInMonth) {
                    Log.d("DashboardFragment", "Income '${incomeItem.description}' is NOT active in current month")
                    return@sumOf 0.0
                }

                when (incomeItem.cycleType) {
                    CycleType.MONTHLY -> {
                        val isOnceOff = incomeItem.endDate != null && incomeItem.startDate == incomeItem.endDate
                        val isThisMonth = incomeStartMonth == currentMonth && incomeStartYear == currentYear

                        val monthlyAmount = when {
                            isOnceOff && isThisMonth -> {
                                Log.d("DashboardFragment", "Detected once-off income: '${incomeItem.description}'")
                                incomeItem.amount
                            }
                            incomeStartYear < currentYear || (incomeStartYear == currentYear && incomeStartMonth <= currentMonth) -> {
                                incomeItem.amount
                            }
                            else -> 0.0
                        }

                        Log.d("DashboardFragment", "Income '${incomeItem.description}' applied monthly amount: R$monthlyAmount")
                        monthlyAmount
                    }

                    CycleType.YEARLY -> {
                        val isEligible = incomeStartYear < currentYear || (incomeStartYear == currentYear && incomeStartMonth <= currentMonth)
                        val yearlyPortion = if (isEligible) incomeItem.amount / 12 else 0.0
                        Log.d("DashboardFragment", "Income '${incomeItem.description}' applied yearly portion: R$yearlyPortion")
                        yearlyPortion
                    }
                }
            } catch (e: Exception) {
                Log.e("DashboardFragment", "Error evaluating income '${incomeItem.description}'", e)
                0.0
            }
        }


        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val filteredTransactions = allTransactions.filter {
            try {
                val txnDate = sdf.parse(it.Date)
                val isInRange = txnDate != null && !txnDate.before(startOfMonth.time) && !txnDate.after(endOfMonth.time)
                var holder = 0.0
                if (isInRange) {
                    when (it.type.lowercase(Locale.getDefault())) {
                        "expense" -> holder += it.amount
                        "refund" -> holder -= it.amount
                    }
                    totalHold += holder
                }
                isInRange
            } catch (e: Exception) {
                Log.e("DashboardFragment.loadDashboardData", "Error parsing txn date for '${it.title}'", e)
                false
            }
        }

        val remaining = income - totalHold

        Log.i("DashboardFragment.loadDashboardData", "Calculated dashboard: Income=R$income | Expenses=R$netExpense | Remaining=R$remaining")

        binding.totalIncomeText.text = "Total Income: R%.2f".format(income)
        binding.totalExpensesText.text = "Total Expenses: R%.2f".format(totalHold)
        binding.remainingBudgetText.text = "Remaining Budget: R%.2f".format(remaining)

        val categorySummaries = allCategories.map { category ->
            val txns = filteredTransactions.filter { it.categoryId == category.id }
            val totalSpent = txns.sumOf { it.amount }
            Log.d("DashboardFragment.loadDashboardData", "Category '${category.title}' | Spent=R$totalSpent | Count=${txns.size}")
            category.copy(totalSpent = totalSpent, transactionCount = txns.size)
        }.sortedByDescending { it.totalSpent }.take(3)

        binding.topCategoriesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.topCategoriesRecyclerView.adapter = CategorySummaryAdapter(categorySummaries)

        val recentTxns = filteredTransactions.sortedByDescending {
            try {
                sdf.parse(it.Date)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }.take(3)

        val recentAdapter = CategorySummaryAdapter(
            recentTxns.map { txn ->
                val category = allCategories.find { it.id == txn.categoryId }
                category?.copy(
                    totalSpent = txn.amount,
                    transactionCount = 1,
                    title = "${category?.title ?: "Unknown"} (Recent Trans.)"
                ) ?: allCategories.first()
            }
        )

        binding.recentTransactionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recentTransactionsRecyclerView.adapter = recentAdapter
    }


    /// Handles top-right menu item clicks for navigation and actions.
    /// Supports Help, Goals, Rewards, Income, and Logout operations.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d("DashboardFragment.onOptionsItemSelected", "Menu item clicked: ${item.itemId}")

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
                Log.d("DashboardFragment.onOptionsItemSelected", "User $userId navigating to IncomeFragment via menu")
                true
            }
            R.id.logoutButton -> {
                sessionManager.clearSession()
                Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
                Log.i("DashboardFragment.onOptionsItemSelected", "User $userId logged out")
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /// Inflates the top-right menu items into the app bar.
    /// Menu contains options for income, help, logout, etc.
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.top_right_menu, menu)
        Log.d("DashboardFragment.onCreateOptionsMenu", "Top-right menu created")
        super.onCreateOptionsMenu(menu, inflater)
    }

    /// Cleans up view binding to prevent memory leaks after fragment is destroyed.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("DashboardFragment.onDestroyView", "View destroyed and binding cleared")
    }
}
