package com.centsnippers.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.centsnippers.adapters.CategoryTabAdapter
import com.centsnippers.databinding.FragmentCategoryBinding
import com.google.android.material.tabs.TabLayoutMediator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.app.DatePickerDialog
import android.widget.Toast


/**
 * CategoryFragment sets up a tabbed view containing two tabs:
 * 1. CategoryListTabFragment - showing the category list
 * 2. CategoryGraphTabFragment - placeholder for graph view
 */
class CategoryFragment : Fragment(), OnCategoryTotalCalculatedListener {


    // -------------------- Binding Setup --------------------

    // ViewBinding object for accessing layout views in a type-safe way
    private var _binding: FragmentCategoryBinding? = null

    // Safe getter for binding, guarantees non-null after onCreateView
    private val binding get() = _binding!!

    private var isFilterVisible = false


    var currentStartDate: String? = null
    var currentEndDate: String? = null



    // -------------------- Fragment Lifecycle --------------------

    /**
     * Inflates the layout using FragmentCategoryBinding and returns the root view.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the binding for this fragment
        Log.d("CategoryFragment", "onCreateView() called")
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        val tab1Fragment = childFragmentManager.findFragmentByTag("f0") as? CategoryListTabFragment



        // Log layout inflation
        Log.d("CategoryFragment.onCreateView", "Inflated layout and initialized binding")

        // Return the root view
        return binding.root
    }

    /**
     * Called after the view has been created. Initializes tabs and connects them to the adapter.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("CategoryFragment", "onViewCreated() called")
        val tabAdapter = CategoryTabAdapter(this)
        binding.viewPager.adapter = tabAdapter
        Handler(Looper.getMainLooper()).postDelayed({
            val tab1 = childFragmentManager.findFragmentByTag("f0") as? CategoryListTabFragment
            if (tab1 != null) {
                Log.d("CategoryFragment", "Setting OnCategoryTotalCalculatedListener")
                tab1.setOnTotalCalculatedListener(this@CategoryFragment)
            } else {
                Log.w("CategoryFragment", "Tab1 (List) not ready — cannot set listener")
            }

            // 🔁 Only now broadcast initial filter
            currentStartDate?.let { start ->
                currentEndDate?.let { end ->
                    Log.d("CategoryFragment", "Delayed filter broadcast: $start → $end")
                    broadcastDateFilter(start, end)
                }
            }
        }, 300)

        // Set up tab titles
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "List"
                1 -> "BREAKDOWN"
                else -> "Tab ${position + 1}"
            }
        }.attach()

        Log.d("CategoryFragment", "TabLayout and ViewPager2 initialized")
// Delay broadcasting to give ViewPager time to instantiate tabs
        Handler(Looper.getMainLooper()).postDelayed({
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val start = binding.startDateInput.text.toString()
            val end = binding.endDateInput.text.toString()

            Log.d("CategoryFragment", "🔁 Delayed filter broadcast: $start → $end")
            broadcastDateFilter(start, end)
        }, 200)

        // Global FAB action
        binding.addCategoryFab.setOnClickListener {
            Log.d("CategoryFragment", "Global FAB clicked")
            val tab1 = childFragmentManager.findFragmentByTag("f0")
            if (tab1 is CategoryListTabFragment) {
                tab1.launchAddDialogFromParentTab()
            } else {
                Log.w("CategoryFragment", "Tab 1 not found on FAB click")
            }
        }

        //listener for filter button
        binding.toggleFilterButton.setOnClickListener {
            isFilterVisible = !isFilterVisible

            if (isFilterVisible) {
                binding.filterBar.visibility = View.VISIBLE
                binding.toggleFilterButton.text = "Hide Filter"
            } else {
                binding.filterBar.visibility = View.GONE
                binding.toggleFilterButton.text = "Show Filter"
                resetDateFiltersToDefault()
            }
        }




        // Setup input listeners for date pickers only (no broadcast)
        setupDatePickersAndDefaultRange()
        binding.applyFilterButton.setOnClickListener {
            val start = binding.startDateInput.text.toString().trim()
            val end = binding.endDateInput.text.toString().trim()

            Log.d("CategoryFragment", "Manual filter button clicked with: $start → $end")

            if (start.isNotBlank() && end.isNotBlank()) {
                broadcastDateFilter(start, end)
            } else {
                Log.w("CategoryFragment", "Cannot apply filter: start or end date blank")
            }
        }


    }

    override fun onTotalCalculated(total: Double) {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val defaultStart = sdf.format(calendar.apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }.time)

        val defaultEnd = sdf.format(calendar.apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }.time)

        val usedDefaultRange = currentStartDate == defaultStart && currentEndDate == defaultEnd

        val formattedText = if (usedDefaultRange) {
            val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
            val year = calendar.get(Calendar.YEAR)

            "Total Budgeted for $monthName $year: R%.2f".format(total)
        } else {
            "Total Budgeted per selected period: R%.2f".format(total)
        }

        binding.totalCategoryTextView.text = formattedText
        Log.d("CategoryFragment", "Updated total text: $formattedText")
    }



    // setting filters based on date
    private fun setupDatePickersAndDefaultRange() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Set to 1st of the current month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startDateStr = sdf.format(calendar.time)
        binding.startDateInput.setText(startDateStr)

        // Set to end of the current month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endDateStr = sdf.format(calendar.time)
        binding.endDateInput.setText(endDateStr)

        Log.d("CategoryFragment", "Autofilled date range: $startDateStr to $endDateStr")

        // Hook up date pickers
        binding.startDateInput.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                val date = "%02d/%02d/%04d".format(d, m + 1, y)
                binding.startDateInput.setText(date)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.endDateInput.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                val date = "%02d/%02d/%04d".format(d, m + 1, y)
                binding.endDateInput.setText(date)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

    }

    /// Sends the selected date range to both tab fragments (list + graph)
    private fun broadcastDateFilter(startDateStr: String, endDateStr: String) {
        Log.d("CategoryFragment", "Storing + broadcasting date filter: $startDateStr → $endDateStr")

        currentStartDate = startDateStr
        currentEndDate = endDateStr

        val tab1 = childFragmentManager.findFragmentByTag("f0") as? CategoryListTabFragment
        val tab2 = childFragmentManager.findFragmentByTag("f1") as? CategoryGraphTabFragment

        tab1?.applyDateFilter(startDateStr, endDateStr)
        tab2?.applyDateFilter(startDateStr, endDateStr)
    }


    /**
     * Nullifies the binding when the view is destroyed to avoid memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("CategoryFragment.onDestroyView", "Binding set to null on destroy")
    }

    private fun resetDateFiltersToDefault() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Start = 1st of current month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startDateStr = sdf.format(calendar.time)
        binding.startDateInput.setText(startDateStr)

        // End = last day of current month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endDateStr = sdf.format(calendar.time)
        binding.endDateInput.setText(endDateStr)

        Toast.makeText(requireContext(), "Filter reset to current month", Toast.LENGTH_SHORT).show()
        broadcastDateFilter(startDateStr, endDateStr)
    }

}