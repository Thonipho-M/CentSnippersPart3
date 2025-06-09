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
import android.widget.EditText
import android.view.LayoutInflater
import android.view.View
import android.util.Log

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
        _binding = FragmentCategoryListTabBinding.inflate(inflater, container, false)
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



    /// Loads all categories for the current user from the database and updates the UI.
/// Fetches the list, updates the adapter, calculates the total budget, and displays it in the view.
    private fun loadCategories() {
        // Retrieve all categories associated with the current user
        val categories = dbHelper.getCategoriesForUser(userId)

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
                    // Enforce validation rules for min and max spend vs. goal amount
                    if (goalAmount != null) {
                        if (minSpend != null && minSpend >= goalAmount) {
                            Toast.makeText(requireContext(), "CATEGORY CAN NOT BE CREATED: Min spend must be less than goal amount", Toast.LENGTH_SHORT).show()
                            Log.w("CategoryListTabFragment", "Validation failed: minSpend=$minSpend >= goalAmount=$goalAmount")
                            return@setPositiveButton
                        }

                        if (maxSpend != null && maxSpend <= goalAmount) {
                            Toast.makeText(requireContext(), "CATEGORY CAN NOT BE CREATED: Max spend must be greater than goal amount", Toast.LENGTH_SHORT).show()
                            Log.w("CategoryListTabFragment", "Validation failed: maxSpend=$maxSpend <= goalAmount=$goalAmount")
                            return@setPositiveButton
                        }
                    }



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

    fun setOnTotalCalculatedListener(callback: OnCategoryTotalCalculatedListener) {
        listener = callback
        // If we already calculated it, send it again
        lastCalculatedTotal?.let {
            listener?.onTotalCalculated(it)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
