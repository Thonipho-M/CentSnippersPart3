package com.centsnippers.fragments

// Usual imports to handle layout, nav, and utils
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
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
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import androidx.core.content.ContextCompat
import com.centsnippers.fragments.*


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
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)


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
        binding.spendingGoalBarChart.clear()

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

        totalHold = 0.0

        val filteredTransactions = allTransactions.filter { txn ->
            try {
                val txnDate = sdf.parse(txn.Date)
                val inRange = txnDate != null && !txnDate.before(startOfMonth.time) && !txnDate.after(endOfMonth.time)

                if (inRange) {
                    when (txn.type.lowercase(Locale.getDefault())) {
                        "expense" -> {
                            totalHold += txn.amount
                            Log.d("DashboardFragment", "Included expense: '${txn.title}' | Amount=R${txn.amount}")
                        }
                        "refund" -> {
                            totalHold -= txn.amount
                            Log.d("DashboardFragment", "Included refund: '${txn.title}' | Amount=-R${txn.amount}")
                        }
                        else -> {
                            Log.d("DashboardFragment", "Ignored txn: '${txn.title}' | Type='${txn.type}'")
                        }
                    }
                }
                inRange
            } catch (e: Exception) {
                Log.e("DashboardFragment", "Error parsing txn date for '${txn.title}'", e)
                false
            }
        }

        Log.i("DashboardFragment", "🧮 FINAL totalHold (totalSpent) = R$totalHold from ${filteredTransactions.size} filtered transactions")


        val remaining = income - totalHold

        Log.i("DashboardFragment.loadDashboardData", "Calculated dashboard: Income=R$income | Expenses=R$netExpense | Remaining=R$remaining")

        binding.totalIncomeText.text = "Total Income: R%.2f".format(income)
        binding.totalExpensesText.text = "Total Expenses: R%.2f".format(totalHold)
        binding.remainingBudgetText.text = "Remaining Budget: R%.2f".format(remaining)



        /// Calculate total category goals to compare spending against
        val totalGoalAmount = allCategories.sumOf { it.goalAmount }
        recalculateChart(totalHold, totalGoalAmount)

/// Calculate total points earned (if any) by subtracting spending from goal
        val pointsEarned = (totalGoalAmount - totalHold).coerceAtLeast(0.0)

/// Check if we are on the 1st day of the next month to reward points
        val calendarNow = Calendar.getInstance()
        val isFirstOfNextMonth = calendarNow.get(Calendar.DAY_OF_MONTH) == 1
        Log.d("DashboardFragment", "📊 Graph Input — totalSpent=$totalHold | totalGoalAmount=$totalGoalAmount")

/// Show user how they're doing and whether they will earn points
        val feedbackMsg = if (totalHold <= totalGoalAmount) {
            if (isFirstOfNextMonth) {
                "🎉 You earned %.0f points from last month!".format(pointsEarned)
            } else {
                "✅ You're on track to earn %.0f points by month end!".format(pointsEarned)
            }
        } else {
            "⚠️ You've exceeded your category goals. Consider adjusting your budget."
        }
        // 🟢 Load bar colors from colors.xml using ContextCompat
        val goalBarColor = ContextCompat.getColor(requireContext(), R.color.sageGreen)

// Calculate the % of budget used
        val spendingRatio = if (totalGoalAmount == 0.0) 0.0 else totalHold / totalGoalAmount

// Determine dynamic color for "Spent" bar based on how close to goal
        val spentBarColor = when {
            spendingRatio <= 0.8 -> ContextCompat.getColor(requireContext(), R.color.successGreen)   // Green
            spendingRatio <= 1.0 -> ContextCompat.getColor(requireContext(), R.color.centYellow)     // Yellow
            else -> ContextCompat.getColor(requireContext(), R.color.errorRed)                       // Red
        }

        // Bar 0: Spent
        val spentEntry = BarEntry(0f, totalHold.toFloat())
        Log.d("DashboardChart", "Creating spentEntry with Y=${spentEntry.y} from totalSpent=$totalHold")

        val spentDataSet = BarDataSet(listOf(spentEntry), "Spent").apply {
            color = spentBarColor
            valueTextSize = 14f
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.black)
        }

// Bar 1: Goal
        val goalEntry = BarEntry(1f, totalGoalAmount.toFloat())
        val goalDataSet = BarDataSet(listOf(goalEntry), "Goal").apply {
            color = goalBarColor
            valueTextSize = 14f
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.black)
        }


// Combine both into one BarData
        val barData = BarData(spentDataSet, goalDataSet)


/// Configure chart appearance
        binding.spendingGoalBarChart.apply {
            data = barData
            description.isEnabled = false
            legend.isEnabled = false
            setScaleEnabled(false)
            setDrawGridBackground(false)
            animateY(800)
            setTouchEnabled(false)
            setDrawValueAboveBar(true)

            /// X Axis: becomes the vertical axis in HorizontalBarChart
            xAxis.apply {
                isEnabled = true
                setDrawGridLines(false)
                setDrawAxisLine(false)
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(listOf("Spent", "Goal"))
                granularity = 1f
                textSize = 14f
            }

            /// Hide Y axes
            axisLeft.isEnabled = false
            axisRight.isEnabled = false

            invalidate() // 🔁 Refresh
        }

        barData.notifyDataChanged() // tell BarData to refresh internal dataset
        binding.spendingGoalBarChart.notifyDataSetChanged() // tell chart to redraw with updated data


/// Push feedback to the TextView
        binding.pointsFeedbackText.text = feedbackMsg


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
    /// Refresh bar chart and message using current expense + goal data
    fun recalculateChart(totalSpent: Double, totalGoal: Double) {
        val context = requireContext()

        // Load colors
        val goalBarColor = ContextCompat.getColor(context, R.color.sageGreen)
        val spendingRatio = if (totalGoal == 0.0) 0.0 else totalSpent / totalGoal
        val spentBarColor = when {
            spendingRatio <= 0.8 -> ContextCompat.getColor(context, R.color.successGreen)
            spendingRatio <= 1.0 -> ContextCompat.getColor(context, R.color.centYellow)
            else -> ContextCompat.getColor(context, R.color.errorRed)
        }

        // Create entries
        val spentEntry = BarEntry(0f, totalSpent.toFloat())
        val goalEntry = BarEntry(1f, totalGoal.toFloat())

        // Datasets
        val spentDataSet = BarDataSet(listOf(spentEntry), "Spent").apply {
            color = spentBarColor
            valueTextSize = 14f
            valueTextColor = ContextCompat.getColor(context, R.color.black)
        }

        val goalDataSet = BarDataSet(listOf(goalEntry), "Goal").apply {
            color = goalBarColor
            valueTextSize = 14f
            valueTextColor = ContextCompat.getColor(context, R.color.black)
        }

        // Combine and format bar chart
        val barData = BarData().apply {
            addDataSet(spentDataSet)
            addDataSet(goalDataSet)
            barWidth = 0.4f
        }

        binding.spendingGoalBarChart.apply {
            data = barData
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(listOf("Spent", "Goal"))
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textSize = 14f
            }

            axisLeft.isEnabled = false
            axisRight.isEnabled = false

            description.isEnabled = false
            legend.isEnabled = false
            animateY(800)
            setTouchEnabled(false)
            setDrawValueAboveBar(true)
            setScaleEnabled(false)

            notifyDataSetChanged()
            invalidate()
        }

        // Update feedback
        val isFirstOfNextMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) == 1
        val points = (totalGoal - totalSpent).coerceAtLeast(0.0)
        val feedback = if (totalSpent <= totalGoal) {
            if (isFirstOfNextMonth) {
                "🎉 You earned %.0f points from last month!".format(points)
            } else {
                "✅ You're on track to earn %.0f points by month end!".format(points)
            }
        } else {
            "⚠️ You've exceeded your category goals. Consider adjusting your budget."
        }

        binding.pointsFeedbackText.text = feedback
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
    /// Sets up spinner to filter dashboard by month
    private fun setupDateSpinner() {
        val spinner: Spinner = binding.dateRangeSpinner
        val calendar = Calendar.getInstance()
        val monthLabels = (0..11).map {
            calendar.set(Calendar.MONTH, it)
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
        }.reversed()

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, monthLabels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(0)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -position) }
                selectedMonth = cal.get(Calendar.MONTH) + 1
                selectedYear = cal.get(Calendar.YEAR)
                Log.d("DashboardFragment", "Spinner selected -> Month=$selectedMonth Year=$selectedYear")
                loadDashboardData()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun showAddTransactionDialog() {
        val transactionFragment = parentFragmentManager.findFragmentByTag("TransactionFragment")
        if (transactionFragment is TransactionFragment) {
            transactionFragment.showAddTransactionDialog()
        } else {
            Toast.makeText(requireContext(), "TransactionFragment not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddIncomeDialog() {
        val incomeFragment = parentFragmentManager.findFragmentByTag("IncomeFragment")
        if (incomeFragment is IncomeFragment) {
            incomeFragment.showAddIncomeDialog()
        } else {
            Toast.makeText(requireContext(), "IncomeFragment not available", Toast.LENGTH_SHORT).show()
        }
    }
    /// Cleans up view binding to prevent memory leaks after fragment is destroyed.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("DashboardFragment.onDestroyView", "View destroyed and binding cleared")
    }
}
