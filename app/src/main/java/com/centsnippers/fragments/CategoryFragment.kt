package com.centsnippers.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import android.util.Log
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
    private var editedCategory: CategoryItem? = null


    /// Fragment initialization, used to inflate layout and bind components
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    /// Inflates the fragment layout and sets up the view binding reference.
/// Uses FragmentCategoryBinding to access UI elements in a type-safe way and returns the root view.
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate layout using ViewBinding specific to CategoryFragment
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)

        // Return the root view of the binding to be rendered by the system
        return binding.root.also {
            Log.d("CategoryFragment.onCreateView", "Inflated layout and initialized binding in CategoryFragment using function onCreateView")
        }
    }

    /// Initializes view components, user session, and category list after fragment layout is created.
/// Sets up RecyclerView adapter, date pickers, and handles button click logic for filtering and adding categories.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize database helper and session manager
        dbHelper = DatabaseHelper(requireContext())
        sessionManager = SessionManager(requireContext())

        // Fetch logged-in user ID from session
        userId = sessionManager.getUserId()

        // If no user is logged in, show a toast and exit early
        if (userId == -1) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            Log.d("CategoryFragment.onViewCreated", "User not logged in in CategoryFragment using function onViewCreated")
            return
        }

        // Initialize the category adapter with delete callback
        categoryAdapter = CategoryAdapter(
            mutableListOf(),
            onDeleteClick = { item -> handleDeleteCategory(item) },
            onEditClick = { item -> editCategoryItem(item) }
        )




        // Set adapter dependencies: DB helper and user ID
        categoryAdapter.setDependencies(dbHelper, userId)

        // Configure the RecyclerView with linear layout and attach adapter
       binding.categoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.categoryRecyclerView.adapter = categoryAdapter


        // Get current month and year from system calendar
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)

        // Load all categories for the current month
        loadFilteredCategoriesForMonth(currentMonth, currentYear)

        // Set listener for the Floating Action Button to show category dialog
        binding.addCategoryFab.setOnClickListener {
            showAddCategoryDialog()
            Log.d("CategoryFragment.onViewCreated", "FAB clicked - showing category dialog in CategoryFragment using function onViewCreated")
        }


        // Final confirmation of successful setup
        Log.d("CategoryFragment.onViewCreated", "CategoryFragment fully initialized for userId=$userId using function onViewCreated")
    }




    /// Loads all categories for the current user from the database and updates the UI.
/// Fetches the list, updates the adapter, calculates the total budget, and displays it in the view.
    private fun loadCategories() {
        // Retrieve all categories associated with the current user
        val categories = dbHelper.getCategoriesForUser(userId)

        // Update the RecyclerView adapter with the fetched list
        categoryAdapter.updateList(categories)

        // Calculate the total budgeted amount across all categories
        val total = categories.sumOf { it.maxSpend ?: it.goalAmount }



        // Display the total formatted as currency in the corresponding TextView
        binding.totalCategoryTextView.text = "Total Budgeted: R%.2f".format(total)

        // Log the total and confirm UI update
        Log.d("CategoryFragment.loadCategories", "Loaded ${categories.size} categories for user $userId and updated total budget to R%.2f".format(total))
    }

    /// Loads and filters user categories and their transactions for a specific month and year.
/// Retrieves all categories and transactions from the DB, filters transactions by date, updates each category's total spent,
/// and updates the total view with a summary of monthly spending.
    private fun loadFilteredCategoriesForMonth(month: Int, year: Int) {
        // Fetch all categories for the current user
        val allCategories = dbHelper.getCategoriesForUser(userId)

        // Fetch all transactions for the current user
        val allTransactions = dbHelper.getTransactionsForUser(userId)

        // Filter transactions to only include those from the selected month and year
        val filteredTransactions = allTransactions.filter { txn ->
            try {
                // Parse transaction date using expected format
                val txnDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(txn.Date)

                // Convert date to Calendar for easier extraction
                val txnCal = Calendar.getInstance().apply { time = txnDate!! }

                // Extract transaction's month and year
                val txnMonth = txnCal.get(Calendar.MONTH) + 1
                val txnYear = txnCal.get(Calendar.YEAR)

                // Only include if it matches the requested month and year
                txnMonth == month && txnYear == year
            } catch (e: Exception) {
                Log.e("CategoryFragment.loadFilteredCategoriesForMonth", "Error parsing date for txn: ${txn.Date}", e)
                false // Skip if parsing fails
            }
        }

// Map over categories to recalculate totals based on filtered transactions
        val updatedCategories = allCategories.map { category ->

            // Step 1: Get transactions assigned to this category
            val txns = filteredTransactions.filter { it.categoryId == category.id }

            // Step 2: Calculate totalSpent by applying type-aware logic
            var totalSpent = 0.0

            txns.forEach { txn ->
                when (txn.type.lowercase(Locale.getDefault())) {
                    "expense" -> totalSpent += txn.amount     // Expenses increase total spent
                    "refund"  -> totalSpent -= txn.amount     // Refunds reduce total spent
                    else -> Log.w("CategoryFragment", "Ignored txn type '${txn.type}' for category '${category.title}'")
                }
                Log.d("CategoryFragment", "Txn '${txn.title}' | Type=${txn.type} | Amount=${txn.amount} | Running totalSpent=${totalSpent}")
            }

            // Step 3: Return a new copy of the category with updated values
            category.copy(
                totalSpent = totalSpent,
                transactionCount = txns.size
            )
        }


        // Refresh the adapter with the updated category list
        categoryAdapter.updateList(updatedCategories)

        // Calculate total amount spent in the filtered categories
        val totalSpentFiltered = updatedCategories.sumOf { it.totalSpent }


        // Convert month number to full month name for display
        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Calendar.getInstance().apply {
            set(Calendar.MONTH, month - 1)
        }.time)

        // Update UI to show full budget for the given month
        binding.totalCategoryTextView.text =
            "Total Budget for $monthName $year: R%.2f".format(updatedCategories.sumOf { it.goalAmount })

        Log.d("CategoryFragment.loadFilteredCategoriesForMonth", "Filtered and updated categories for $month/$year in CategoryFragment using function loadFilteredCategoriesForMonth")
    }

    /// Displays a dialog for adding a new category to the user's profile.
/// Collects title, description, and amount inputs, validates them, and saves to DB on confirmation.
    private fun showAddCategoryDialog(isEdit: Boolean = false) {
        // Inflate the custom layout for the category input dialog
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null)



        // Get references to the input fields inside the dialog layout
        val titleInput = dialogView.findViewById<EditText>(R.id.inputCategoryTitle)
        val descInput = dialogView.findViewById<EditText>(R.id.inputCategoryDescription)
        val goalInput = dialogView.findViewById<EditText>(R.id.inputGoalAmount)
        val minInput = dialogView.findViewById<EditText>(R.id.inputMinSpend)
        val maxInput = dialogView.findViewById<EditText>(R.id.inputMaxSpend)


        // Pre-fill fields if editing an existing category
        if (isEdit && editedCategory != null) {
            titleInput.setText(editedCategory!!.title)
            descInput.setText(editedCategory!!.description)
            goalInput.setText(editedCategory!!.goalAmount.toString())
            minInput.setText(editedCategory!!.minSpend?.toString() ?: "")
            maxInput.setText(editedCategory!!.maxSpend?.toString() ?: "")
            Log.d("CategoryFragment", "Pre-filling dialog with category ID=${editedCategory!!.id}")
        }
        // Build and display the AlertDialog for adding a category
        AlertDialog.Builder(requireContext())
            .setTitle("Add Category") // Dialog title
            .setView(dialogView) // Set the custom view with input fields
            .setPositiveButton("Save") { _, _ ->
                val title = titleInput.text.toString().trim()
                val description = descInput.text.toString().trim()
                val goalAmount = goalInput.text.toString().trim().toDoubleOrNull()
                val minSpend = minInput.text.toString().trim().toDoubleOrNull()
                val maxSpend = maxInput.text.toString().trim().toDoubleOrNull()

                if (title.isNotBlank() && goalAmount != null) {
                    val categoryItem = CategoryItem(
                        id = editedCategory?.id ?: 0,
                        userId = userId,
                        title = title,
                        description = description,
                        goalAmount = goalAmount,
                        minSpend = minSpend,
                        maxSpend = maxSpend
                    )


                    val success = if (isEdit) {
                        dbHelper.updateCategory(categoryItem)
                    } else {
                        dbHelper.insertCategory(categoryItem)
                    }

                    if (success) {
                        Toast.makeText(requireContext(), "Category ${if (isEdit) "updated" else "added"}", Toast.LENGTH_SHORT).show()
                        Log.i("CategoryFragment", "Category ${if (isEdit) "updated" else "added"}: ${categoryItem.title}")
                        loadCategories()
                    } else {
                        Toast.makeText(requireContext(), "Failed to save category", Toast.LENGTH_SHORT).show()
                        Log.w("CategoryFragment", "Failed to save category: ${categoryItem.title}")
                    }

                    editedCategory = null // Reset edit mode after save
                } else {
                    Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    Log.w("CategoryFragment", "Validation failed for saving category — title or amount invalid")
                }
            }

            .setNegativeButton("Cancel", null) // Cancel button closes dialog without action
            .show() // Display the dialog
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
                val success = dbHelper.deleteCategory(categoryId)
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
