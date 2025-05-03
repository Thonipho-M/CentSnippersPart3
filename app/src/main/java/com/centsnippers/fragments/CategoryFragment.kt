package com.centsnippers.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.centsnippers.R
import com.centsnippers.adapters.CategoryAdapter
import com.centsnippers.data.DatabaseHelper
import com.centsnippers.databinding.FragmentCategoryBinding
import com.centsnippers.models.CategoryItem
import com.centsnippers.utils.SessionManager
import com.centsnippers.MainActivity
import androidx.navigation.fragment.findNavController
import java.text.SimpleDateFormat
import java.util.*

class CategoryFragment : Fragment() {

    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var categoryAdapter: CategoryAdapter
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())
        sessionManager = SessionManager(requireContext())
        userId = sessionManager.getUserId()

        if (userId == -1) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        categoryAdapter = CategoryAdapter(mutableListOf()) { position -> deleteCategoryItem(position) }
        categoryAdapter.setDependencies(dbHelper, userId)
        binding.categoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.categoryRecyclerView.adapter = categoryAdapter

        setupMonthYearSpinner()
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1  // 1-based (Jan = 1)
        val currentYear = calendar.get(Calendar.YEAR)

        loadFilteredCategoriesForMonth(currentMonth, currentYear)


        binding.addCategoryFab.setOnClickListener {
            showAddCategoryDialog()
        }

//Listener to load filtered items
        binding.monthYearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = parent.getItemAtPosition(position).toString()  // e.g. "April 2025"
                val parts = selected.split(" ")
                if (parts.size == 2) {
                    val monthName = parts[0]
                    val year = parts[1].toIntOrNull() ?: return

                    // Convert month name to Calendar index
                    val monthIndex = try {
                        SimpleDateFormat("MMMM", Locale.getDefault()).parse(monthName)?.let {
                            Calendar.getInstance().apply { time = it }.get(Calendar.MONTH) + 1
                        } ?: return
                    } catch (e: Exception) {
                        return
                    }

                    loadFilteredCategoriesForMonth(monthIndex, year)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No-op
            }
        }


    }


    private fun setupMonthYearSpinner() {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val months = mutableListOf<String>()

        for (i in 0 until 24) { // Past 24 months
            months.add(sdf.format(calendar.time))
            calendar.add(Calendar.MONTH, -1)
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.monthYearSpinner.adapter = adapter
    }


    private fun loadMonthlyFilteredCategories(month: Int) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()

        val allCategories = dbHelper.getCategoriesForUser(userId)
        val allTransactions = dbHelper.getTransactionsForUser(userId)

        val filteredTransactions = allTransactions.filter { txn ->
            try {
                val txnDate = sdf.parse(txn.startDate)
                calendar.time = txnDate
                (calendar.get(Calendar.MONTH) + 1) == month
            } catch (e: Exception) {
                false
            }
        }

        val updatedCategories = allCategories.map { category ->
            val txns = filteredTransactions.filter { it.categoryId == category.id }
            category.copy(
                totalSpent = txns.sumOf { it.amount },
                transactionCount = txns.size
            )
        }

        categoryAdapter.updateList(updatedCategories)

        val filteredTotal = updatedCategories.sumOf { it.amount }
        binding.totalCategoryTextView.text = "Filtered Total Budget: R%.2f".format(filteredTotal)
    }

    private fun loadCategories() {
        val categories = dbHelper.getCategoriesForUser(userId)
        categoryAdapter.updateList(categories)
        val total = categories.sumOf { it.amount }
        binding.totalCategoryTextView.text = "Total Budgeted: R%.2f".format(total)
    }
    private fun loadFilteredCategoriesForMonth(month: Int, year: Int) {
        val allCategories = dbHelper.getCategoriesForUser(userId)
        val allTransactions = dbHelper.getTransactionsForUser(userId)

        val filteredTransactions = allTransactions.filter { txn ->
            try {
                val txnDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(txn.startDate)
                val txnCal = Calendar.getInstance().apply { time = txnDate!! }
                val txnMonth = txnCal.get(Calendar.MONTH) + 1  // Calendar.MONTH is 0-based
                val txnYear = txnCal.get(Calendar.YEAR)

                txnMonth == month && txnYear == year
            } catch (e: Exception) {
                false
            }

        }


        val updatedCategories = allCategories.map { category ->
            val txns = filteredTransactions.filter { it.categoryId == category.id }
            category.copy(
                totalSpent = txns.sumOf { it.amount },
                transactionCount = txns.size
            )
        }

        categoryAdapter.updateList(updatedCategories)

        // Optional: display month name from integer
        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Calendar.getInstance().apply {
            set(Calendar.MONTH, month - 1)
        }.time)

        binding.totalCategoryTextView.text =
            "Total Budget for $monthName $year: R%.2f".format(updatedCategories.sumOf { it.amount })
        binding.categoryRecyclerView.invalidate()
        binding.categoryRecyclerView.adapter?.notifyDataSetChanged()


        Toast.makeText(requireContext(), "Filtered count: ${updatedCategories.size}", Toast.LENGTH_SHORT).show()

    }


    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.inputCategoryTitle)
        val descInput = dialogView.findViewById<EditText>(R.id.inputCategoryDescription)
        val amountInput = dialogView.findViewById<EditText>(R.id.inputCategoryAmount)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Category")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = titleInput.text.toString().trim()
                val description = descInput.text.toString().trim()
                val amount = amountInput.text.toString().trim().toDoubleOrNull()

                if (title.isNotBlank() && amount != null) {
                    val categoryItem = CategoryItem(0, userId, title, description, amount)
                    dbHelper.insertCategory(categoryItem)
                    loadCategories()
                } else {
                    Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCategoryItem(position: Int) {
        val categoryList = dbHelper.getCategoriesForUser(userId)
        if (position in categoryList.indices) {
            val item = categoryList[position]
            dbHelper.deleteCategory(item.id)
            loadCategories()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.top_right_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logoutButton -> {
                sessionManager.clearSession()
                Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireActivity(), MainActivity::class.java))
                requireActivity().finish()
                return true
            }
            R.id.goalsFragment -> {
                findNavController().navigate(R.id.goalsFragment)
                return true
            }
            R.id.rewardsFragment -> {
                findNavController().navigate(R.id.rewardsFragment)
                return true
            }
            R.id.helpPage -> {
                findNavController().navigate(R.id.helpPage)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
