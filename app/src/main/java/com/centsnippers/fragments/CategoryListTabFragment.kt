package com.centsnippers.fragments

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.centsnippers.adapters.CategoryAdapter
import com.centsnippers.data.DatabaseHelper
import com.centsnippers.databinding.FragmentCategoryListTabBinding
import com.centsnippers.models.CategoryItem
import com.centsnippers.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.*
import com.centsnippers.R
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.widget.EditText
import android.view.LayoutInflater
import android.view.View
import android.util.Log
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import java.time.temporal.ChronoUnit


//interface to convery data from this fragment to categoyr fragment
interface OnCategoryTotalCalculatedListener {
    fun onTotalCalculated(total: Double)
}



class CategoryListTabFragment : Fragment() {

    private var _binding: FragmentCategoryListTabBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var categoryAdapter: CategoryAdapter
    private var userId: Int = -1
    private var editedCategory: CategoryItem? = null
    private var listener: OnCategoryTotalCalculatedListener? = null
    private var lastCalculatedTotal: Double? = null//local variable to parse total on loading app






    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        Log.d("CategoryListTabFragment", "onCreateView() called")
        _binding = FragmentCategoryListTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("CategoryListTabFragment", "onViewCreated() called")
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())
        sessionManager = SessionManager(requireContext())
        userId = sessionManager.getUserId()
        // --- Pull latest date filter from CategoryFragment ---
        (parentFragment as? CategoryFragment)?.let { parent ->
            val start = parent.currentStartDate
            val end = parent.currentEndDate

            if (start != null && end != null) {
                Log.d("CategoryListTabFragment", "Pulling date filter from parent: $start → $end")
                applyDateFilter(start, end)
            } else {
                Log.d("CategoryListTabFragment", " No date filter found in parent — skipping")
            }
        }


        if (userId == -1) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        categoryAdapter = CategoryAdapter(
            mutableListOf(),
            onDeleteClick = { item -> handleDeleteCategory(item) },
            onEditClick = { item -> editCategoryItem(item) }
        )
        categoryAdapter.setDependencies(dbHelper, userId)

        binding.categoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.categoryRecyclerView.adapter = categoryAdapter

        loadCategories()

        val categories = dbHelper.getCategoriesForUser(userId)
        val total = categories.sumOf { it.maxSpend ?: it.goalAmount }
        Log.d("CategoryListTabFragment", "-----------------------load Categories executed and values are: total: ${total}  ----------------  ")
        listener?.onTotalCalculated(total)



    }
    // call the category fragment for global floating button
    fun launchAddDialogFromParentTab() {
        Log.d("CategoryListTabFragment", "launchAddDialogFromParentTab() called by CategoryFragment")
        showAddCategoryDialog()
    }

    fun setOnTotalCalculatedListener(listener: OnCategoryTotalCalculatedListener) {
        this.listener = listener
    }


    /// Loads all categories for the current user from the database and updates the UI.
/// Fetches the list, updates the adapter, calculates the total budget, and displays it in the view.
    private fun loadCategories() {
        // Retrieve all categories associated with the current user
        val categories = dbHelper.getCategoriesForUser(userId)
        // Enrich each category with transaction count
        categories.forEach { category ->
            val spent = dbHelper.getTotalSpentForCategory(category)
            category.totalSpent = spent
            val count = dbHelper.getTransactionCountForCategory(category)
            category.transactionCount = count
        }

        // Update the RecyclerView adapter with the fetched list
        categoryAdapter.updateList(categories)


        Log.d("CategoryListTabFragment", "Category saved — reloading list via loadCategories()")


        val total = categories.sumOf { it.maxSpend ?: it.goalAmount }
        lastCalculatedTotal = total// storing variable locally for remedy lazy load on startup
        listener?.onTotalCalculated(total)


        // Log the total and confirm UI update
        //Log.d("CategoryFragment.loadCategories", "Loaded ${categories.size} categories for user $userId and updated total budget to R%.2f".format(total))
    }

    fun getTotalCategoryBudget(): Double {
        val categories = dbHelper.getCategoriesForUser(userId)
        return categories.sumOf { it.maxSpend ?: it.goalAmount }
    }

    /// Displays a dialog for adding a new category to the user's profile.
/// Collects title, description, and amount inputs, validates them, and saves to DB on confirmation.
    private fun showAddCategoryDialog(isEdit: Boolean = false) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null)

        val titleInput = dialogView.findViewById<EditText>(R.id.inputCategoryTitle)
        val descInput = dialogView.findViewById<EditText>(R.id.inputCategoryDescription)
        val goalInput = dialogView.findViewById<EditText>(R.id.inputGoalAmount)
        val minInput = dialogView.findViewById<EditText>(R.id.inputMinSpend)
        val maxInput = dialogView.findViewById<EditText>(R.id.inputMaxSpend)
        val colorLayout = dialogView.findViewById<LinearLayout>(R.id.colorPickerLayout)

        // Define available bar colors
        val availableColors = listOf("#F44336", "#4CAF50", "#2196F3", "#FF9800", "#9C27B0", "#00BCD4")

        // Set default or edit-selected color
        var selectedColorHex = editedCategory?.colorHex ?: availableColors[0]

        // Dynamically inflate the color picker
        colorLayout.removeAllViews()
        for (color in availableColors) {
            val circle = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                    setMargins(12, 8, 12, 8)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(color))
                    setStroke(4, if (color == selectedColorHex) Color.BLACK else Color.TRANSPARENT)
                }
                tag = color
                setOnClickListener {
                    selectedColorHex = color
                    for (i in 0 until colorLayout.childCount) {
                        val v = colorLayout.getChildAt(i)
                        (v.background as GradientDrawable).setStroke(
                            4,
                            if ((v.tag as String) == selectedColorHex) Color.BLACK else Color.TRANSPARENT
                        )
                    }
                }
            }
            colorLayout.addView(circle)
        }

        // Pre-fill fields if editing
        if (isEdit && editedCategory != null) {
            titleInput.setText(editedCategory!!.title)
            descInput.setText(editedCategory!!.description)
            goalInput.setText(editedCategory!!.goalAmount.toString())
            minInput.setText(editedCategory!!.minSpend?.toString() ?: "")
            maxInput.setText(editedCategory!!.maxSpend?.toString() ?: "")
            Log.d("CategoryFragment", "Pre-filling dialog with category ID=${editedCategory!!.id}")
        }

        // Build dialog
        AlertDialog.Builder(requireContext())
            .setTitle(if (isEdit) "Edit Category" else "Add Category")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = titleInput.text.toString().trim()
                val description = descInput.text.toString().trim()
                val goalAmount = goalInput.text.toString().trim().toDoubleOrNull()
                val minSpend = minInput.text.toString().trim().toDoubleOrNull()
                val maxSpend = maxInput.text.toString().trim().toDoubleOrNull()

                if (title.isNotBlank() && goalAmount != null) {
                    if (minSpend != null && minSpend > goalAmount) {
                        Toast.makeText(requireContext(), "Min spend must be < goal", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    if (maxSpend != null && maxSpend < goalAmount) {
                        Toast.makeText(requireContext(), "Max spend must be > goal", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val categoryItem = CategoryItem(
                        id = editedCategory?.id ?: 0,
                        userId = userId,
                        title = title,
                        description = description,
                        goalAmount = goalAmount,
                        minSpend = minSpend,
                        maxSpend = maxSpend,
                        colorHex = selectedColorHex
                    )

                    val success = if (isEdit) {
                        dbHelper.updateCategory(categoryItem)
                    } else {
                        dbHelper.insertCategory(categoryItem)
                    }

                    if (success) {
                        Toast.makeText(requireContext(), "Category ${if (isEdit) "updated" else "added"}", Toast.LENGTH_SHORT).show()
                        Log.i("CategoryFragment", "Category saved: ${categoryItem.title}")
                        loadCategories()
                    } else {
                        Toast.makeText(requireContext(), "Failed to save", Toast.LENGTH_SHORT).show()
                    }
                    editedCategory = null
                } else {
                    Toast.makeText(requireContext(), "Missing title or goal", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }



    /// Deletes a category from the database based on an item
/// Validates index, retrieves the correct category, deletes it by ID, and reloads the updated list.
    private fun handleDeleteCategory(item: CategoryItem) {
        val categoryId = item.id
        val transactionCount = dbHelper.getTransactionCountForCategory(item)

        Log.d("CategoryFragment", "Attempting to delete category ID=$categoryId | Title='${item.title}' | Linked transactions=$transactionCount")

        // BLOCK deletion if there are linked transactions
        if (transactionCount > 0) {
            AlertDialog.Builder(requireContext())
                .setTitle("Cannot Delete Category")
                .setMessage("This category has $transactionCount linked transaction(s). Reassign or remove them before deleting.")
                .setPositiveButton("OK", null)
                .show()

            Log.i("CategoryFragment", "Blocked deletion for category ID=$categoryId due to $transactionCount transaction(s)")
            return
        }

        // ALLOW deletion with confirmation
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete the category '${item.title}'?")
            .setPositiveButton("Yes") { _, _ ->
                val success = dbHelper.deleteCategory(item)
                if (success) {
                    Toast.makeText(requireContext(), "Category deleted", Toast.LENGTH_SHORT).show()
                    Log.i("CategoryFragment", "Successfully deleted category ID=$categoryId | Title='${item.title}'")
                    loadCategories() // refresh list
                } else {
                    Toast.makeText(requireContext(), "Failed to delete category", Toast.LENGTH_SHORT).show()
                    Log.w("CategoryFragment", "Deletion failed for category ID=$categoryId | Title='${item.title}'")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    /// Prepares the selected category for editing by storing its reference and showing the input dialog.
/// Sets the global editedCategory variable and passes isEdit=true to the dialog loader.
    private fun editCategoryItem(item: CategoryItem) {
        editedCategory = item
        Log.d("CategoryFragment", "Preparing edit for category ID=${item.id} | Title='${item.title}'")
        showAddCategoryDialog(isEdit = true)
    }



/// Applies date-based filtering to show only categories with transactions in that range

    @RequiresApi(Build.VERSION_CODES.O)
    fun applyDateFilter(startDateStr: String, endDateStr: String) {
        Log.d("CategoryListTabFragment", "applyDateFilter() called with $startDateStr to $endDateStr")
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
            Log.e("CategoryListTabFragment", "Invalid date inputs: $startDateStr / $endDateStr")
            return
        }

        // Fetch all transactions once for efficiency
        val allTransactions = dbHelper.getTransactionsForUser(userId)

        // Fetch all categories
        val allCategories = dbHelper.getCategoriesForUser(userId)

        // Filter: keep categories that have ≥1 txn in date range
        val filtered = allCategories.filter { category ->
            allTransactions.any { txn ->
                txn.categoryId == category.id && try {
                    val txnDate = sdf.parse(txn.Date)
                    Log.d("CategoryListTabFragment", "Txn='${txn.title}' | RawDate='${txn.Date}' | Parsed=$txnDate -----------------------")

                    txnDate != null && !txnDate.before(start) && !txnDate.after(end)

                } catch (e: Exception) {
                    Log.e("CategoryListTabFragment", "Failed to parse txn date for '${txn.title}'", e)
                    false
                }
            }
        }

        // Enrich each remaining category
        filtered.forEach { category ->
            val txns = allTransactions.filter { txn ->
                txn.categoryId == category.id && try {
                    val txnDate = sdf.parse(txn.Date)
                    txnDate != null && !txnDate.before(start) && !txnDate.after(end)
                } catch (e: Exception) {
                    false
                }
            }

            category.totalSpent = txns.sumOf { it.amount }
            category.transactionCount = txns.size

            Log.d("CategoryListTabFragment", "✔ Category='${category.title}' | Txns=${txns.size} | Total=R${category.totalSpent}")
        }


        // Update UI
        categoryAdapter.updateList(filtered)
        val total = filtered.sumOf { it.totalSpent }
        listener?.onTotalCalculated(total)

        Log.d("CategoryListTabFragment", "Filtered ${filtered.size} categories for range $startDateStr to $endDateStr")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
