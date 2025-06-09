package com.centsnippers.fragments

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.centsnippers.databinding.FragmentCategoryGraphTabBinding
import com.centsnippers.models.CategoryItem
import com.centsnippers.utils.SessionManager
import com.centsnippers.data.DatabaseHelper
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import com.github.mikephil.charting.formatter.ValueFormatter

import android.widget.EditText
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.util.Locale
import java.time.temporal.ChronoUnit



/**
 * CategoryGraphTabFragment is responsible for rendering a visual
 * representation of category spending using graphs (planned).
 * This fragment will eventually display the same category data
 * shown in Tab 1 but in a graphical/chart format.
 */
class CategoryGraphTabFragment : Fragment() {

    private var _binding: FragmentCategoryGraphTabBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var chart: CombinedChart
    private var userId: Int = -1

    // -------------------- Fragment Lifecycle --------------------

    /**
     * Inflates the layout and binds the ViewBinding object.
     * This is where we prepare the base UI for the graph view.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        Log.d("CategoryGraphTabFragment", "onCreateView() called")
        _binding = FragmentCategoryGraphTabBinding.inflate(inflater, container, false)
        return binding.root
    }
    /**
     * Initializes the graph tab fragment after the view is created.
     * Sets up the database, loads user data, and begins pie chart population.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("CategoryGraphTabFragment", "onViewCreated() called")
        // --- Initialize session and database helper classes ---
        sessionManager = SessionManager(requireContext())
        dbHelper = DatabaseHelper(requireContext())
        userId = sessionManager.getUserId()
// --- Pull latest date filter from CategoryFragment ---
        (parentFragment as? CategoryFragment)?.let { parent ->
            val start = parent.currentStartDate
            val end = parent.currentEndDate

            if (start != null && end != null) {
                Log.d("CategoryGraphTabFragment", "Pulling date filter from parent: $start → $end")
                applyDateFilter(start, end)
            } else {
                Log.d("CategoryGraphTabFragment", "⚠No date filter found in parent — skipping")
            }
        }

        // --- Check if user is logged in ---
        if (userId == -1) {
            Log.w("CategoryGraphTabFragment", "No logged in user found | userId=$userId")
            return
        }

        // --- Begin loading and processing category data ---
        Log.d("CategoryGraphTabFragment", "User found | userId=$userId | Starting to load pie chart data")
        loadCategoryData()
    }

    /// Filters categories by transactions within date range and redraws all pie charts
    @RequiresApi(Build.VERSION_CODES.O)
    fun applyDateFilter(startDateStr: String, endDateStr: String) {


        Log.d("CategoryGraphTabFragment", "applyDateFilter() called with $startDateStr to $endDateStr")
        if (!::dbHelper.isInitialized) {
            Log.w("CategoryListTabFragment", "applyDateFilter() called before dbHelper init — skipping")
            return
        }

        if (userId == -1) {
            Log.w("CategoryListTabFragment", "User ID invalid in applyDateFilter — skipping")
            return
        }
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val start = sdf.parse(startDateStr)
        val end = sdf.parse(endDateStr)
        val daysBetween = ChronoUnit.DAYS.between(start.toInstant(), end.toInstant()) + 1
        val dayFactor = ((daysBetween - 30).toDouble() / 100) + 1
        Log.d("CategoryGraphTabFragment", "daysBetween=$daysBetween | dayFactor=$dayFactor")
        if (start == null || end == null) {
            Log.e("CategoryGraphTabFragment", "Invalid date inputs: $startDateStr / $endDateStr")
            return
        }

        Log.d("CategoryGraphTabFragment", "Applying date filter: $startDateStr → $endDateStr")

        val allTransactions = dbHelper.getTransactionsForUser(userId)
        val allCategories = dbHelper.getCategoriesForUser(userId)

        // Only keep categories that have at least one txn in date range
        val filtered = allCategories.filter { category ->
            allTransactions.any { txn ->
                txn.categoryId == category.id && try {
                    val txnDate = sdf.parse(txn.Date)
                    txnDate != null && !txnDate.before(start) && !txnDate.after(end)
                } catch (e: Exception) {
                    Log.e("CategoryGraphTabFragment", "Failed to parse txn date for '${txn.title}'", e)
                    false
                }
            }
        }

        if (filtered.isEmpty()) {
            Log.w("CategoryGraphTabFragment", "No categories matched date range — charts will show no data")
        }

        // Update totalSpent before passing to charts
        filtered.forEach { cat ->
            cat.totalSpent = dbHelper.getTotalSpentForCategory(cat)
        }

        // Redraw all four pie charts using filtered list
        setupPieChart(
            pieChart = binding.pieChartActualSpend,
            legendLayout = binding.legendActualSpend,
            data = filtered.map { PieDataItem(it.title, it.totalSpent, it.colorHex) },
            title = "Total Spent by Category"
        )

        setupPieChart(
            pieChart = binding.pieChartGoalAmount,
            legendLayout = binding.legendGoalAmount,
            data = filtered.map { val adjustedGoal = it.goalAmount * dayFactor
                PieDataItem("${it.title} (Adjusted)", adjustedGoal, it.colorHex) },
            title = "Goal Amounts"
        )

        val hasMissingMin = filtered.any { it.minSpend == null }

        if (hasMissingMin) {
            binding.pieChartMaxSpend.clear()
            binding.legendMaxSpend.removeAllViews()
            binding.MINemptyChartMessage.visibility = View.VISIBLE
            Log.w("CategoryGraphTabFragment", "Skipping Max Spend chart due to missing data")
        } else {

            val adjustedMinData = filtered.map {
                val value2 = it.minSpend ?: return@map null
                PieDataItem("${it.title} (Adjusted)", value2, it.colorHex)
            }?.filterNotNull() ?: emptyList()
            setupPieChart(
                pieChart = binding.pieChartMaxSpend,
                legendLayout = binding.legendMaxSpend,
                data = adjustedMinData,
                title = "Maximum Spend Limits"
            )
            binding.MINemptyChartMessage.visibility = View.GONE
        }
        val hasMissingMax = filtered.any { it.maxSpend == null }

        if (hasMissingMax) {
            binding.pieChartMinSpend.clear()
            binding.legendMinSpend.removeAllViews()
            binding.MAXemptyChartMessage.visibility = View.VISIBLE
            Log.w("CategoryGraphTabFragment", "Skipping Min Spend chart due to missing data")
        } else {
            val adjustedMaxData = filtered?.map {
                val value = it.maxSpend ?: return@map null
                PieDataItem("${it.title} (Adjusted)", value, it.colorHex)
            }?.filterNotNull() ?: emptyList()
            setupPieChart(
                pieChart = binding.pieChartMinSpend,
                legendLayout = binding.legendMinSpend,
                data = adjustedMaxData,
                title = "Minimum Spend Targets"
            )
            binding.MAXemptyChartMessage.visibility = View.GONE
        }


        Log.i("CategoryGraphTabFragment", "Filtered charts updated for $startDateStr to $endDateStr")
    }


    /**
     * Loads all user categories and populates the 4 pie charts:
     * - Total Spent
     * - Goal Amount
     * - Min Spend
     * - Max Spend
     * Each chart shows category percentages and updates a legend below it.
     */
    private fun loadCategoryData() {
        Log.d("CategoryGraphTabFragment", "Loading category data for userId=$userId")

        // Step 1: Fetch all categories from DB
        val categories = dbHelper.getCategoriesForUser(userId)
        if (categories.isEmpty()) {
            Log.w("CategoryGraphTabFragment", "No categories found for user $userId")
            return
        }

        // Step 2: Update totalSpent for each category
        categories.forEach { category ->
            category.totalSpent = dbHelper.getTotalSpentForCategory(category)
        }

        // Step 3: Setup each chart
        setupPieChart(
            pieChart = binding.pieChartActualSpend,
            legendLayout  = binding.legendActualSpend,
            data = categories.map { PieDataItem(it.title, it.totalSpent, it.colorHex) },
            title = "Total Spent by Category"
        )

        setupPieChart(
            pieChart = binding.pieChartGoalAmount,
            legendLayout  = binding.legendGoalAmount as LinearLayout,
            data = categories.map { PieDataItem(it.title, it.goalAmount, it.colorHex) },
            title = "Goal Amounts"
        )

        setupPieChart(
            pieChart = binding.pieChartMinSpend,
            legendLayout  = binding.legendMinSpend as LinearLayout,
            data = categories.filter { it.minSpend != null }
                .map { PieDataItem(it.title, it.minSpend!!, it.colorHex) },
            title = "Minimum Spend Targets"
        )

        setupPieChart(
            pieChart = binding.pieChartMaxSpend,
            legendLayout = binding.legendMaxSpend as LinearLayout,
            data = categories.filter { it.maxSpend != null }
                .map { PieDataItem(it.title, it.maxSpend!!, it.colorHex) },
            title = "Maximum Spend Limits"
        )

        Log.d("CategoryGraphTabFragment", "Pie charts successfully loaded")
    }

    // Simple structure for pie chart entries
    data class PieDataItem(val label: String, val value: Double, val colorHex: String)

    /**
     * Configures and displays a PieChart using category data.
     * Also updates a legend TextView with actual values.
     */
    private fun setupPieChart(
        pieChart: com.github.mikephil.charting.charts.PieChart,
        legendLayout: LinearLayout,
        data: List<PieDataItem>,
        title: String
    ) {
        Log.d("CategoryGraphTabFragment", "Setting up pie chart: $title")

        // Step 1: Filter out entries with zero or negative values
        val filtered = data.filter { it.value > 0 }
        if (filtered.isEmpty()) {
            Log.w("CategoryGraphTabFragment", "No data to show in chart: $title")
            pieChart.clear()
            legendLayout.removeAllViews()

            val emptyText = TextView(requireContext()).apply {
                text = title // Use the title passed into the function, e.g. "Max Goals Missing"
                setTextColor(Color.RED)
                setTextSize(15f)
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            legendLayout.addView(emptyText)
            return
        }


        // Step 2: Convert to PieEntry and Colors
        val entries = filtered.map { PieEntry(it.value.toFloat(), it.label) }
        val colors = filtered.map { android.graphics.Color.parseColor(it.colorHex) }

        // Step 3: Create PieDataSet
        val dataSet = PieDataSet(entries, title).apply {
            this.colors = colors
            valueTextSize = 12f
            valueTextColor = android.graphics.Color.WHITE
            sliceSpace = 2f
        }

        // Step 4: Create PieData
        val pieData = PieData(dataSet)

        // Show values like "45%" instead of just 45
        pieData.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}%"
            }
        })


        // Step 5: Configure PieChart visuals
        pieChart.apply {
            this.data = pieData
            description.isEnabled = false
            legend.isEnabled = false
            setUsePercentValues(true)
            setEntryLabelColor(android.graphics.Color.WHITE)
            setEntryLabelTextSize(12f)
            setHoleColor(android.graphics.Color.TRANSPARENT)
            invalidate() // refresh
        }

        // Step 6: Build and update legend
        legendLayout.removeAllViews()

        filtered.forEach { item ->
            val legendItem = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }

            val colorDot = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(24, 24).apply {
                    setMargins(10, 10, 10, 10)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(item.colorHex))
                }
            }


            val text = TextView(requireContext()).apply {
                val adjusted = item.label.contains("(Adjusted)")
                val labelText = if (adjusted) {
                    "${item.label}: R%.2f".format(item.value)
                } else {
                    "${item.label}: R%.2f".format(item.value)
                }
                text = labelText
                setTextColor(Color.DKGRAY)
                setTextSize(14f)
            }


            legendItem.addView(colorDot)
            legendItem.addView(text)
            (legendLayout).addView(legendItem)
        }

        Log.i("CategoryGraphTabFragment", "Chart \"$title\" loaded with ${filtered.size} entries")

    }

    private fun setTextColor(dkgray: Any) {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}