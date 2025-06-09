package com.centsnippers.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.centsnippers.R
import android.text.TextWatcher
import android.text.Editable

import com.centsnippers.adapters.TransactionAdapter
import com.centsnippers.data.DatabaseHelper
import com.centsnippers.databinding.FragmentTransactionBinding
import com.centsnippers.models.TransactionItem
import com.centsnippers.utils.SessionManager
import com.centsnippers.models.*
import java.util.*
import android.widget.Toast
import android.util.Log
import java.text.SimpleDateFormat
import java.io.File
import androidx.core.content.FileProvider
/// Fragment responsible for displaying and managing transactions.
/// Sets up RecyclerView, handles date filters, and supports adding transactions with images.
class TransactionFragment : Fragment() {

    // UI and logic setup
    private lateinit var binding: FragmentTransactionBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var transactionAdapter: TransactionAdapter

    private var selectedType: String = "expense" // default value for safety

    // uris for images (gallery or camera)
    private var selectedImageUri: Uri? = null
    private var capturedImageUri: Uri? = null

    // tracks selected category + user ID
    private var selectedCategoryId: Int = -1
    private var userId: Int = -1

    // Holds the transaction currently being edited (if any)
    private var editedTransaction: TransactionItem? = null

    private var fullTransactionList: List<TransactionItem> = listOf()
// allowing search through all data in transaction
    private var categoryMap: Map<Int, String> = mapOf()
    private var selectedFilterCategoryId: Int = -1

    //filter buttons
    private var isDateFilterVisible = false
    private var isCategoryFilterVisible = false



    // hold preview image widget
    private lateinit var imagePreview: ImageView

    /// Handles image selection from gallery and previews it inside dialog
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
        if (::imagePreview.isInitialized && uri != null) {
            Glide.with(this).load(uri).into(imagePreview)
            imagePreview.visibility = View.VISIBLE
        }
        Log.d("TransactionFragment.imagePickerLauncher", "Image selected from gallery: $uri")
    }

    /// Requests camera permission and launches camera if granted
    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCamera()
            Log.d("TransactionFragment.cameraPermissionLauncher", "Camera permission granted, launching camera")
        } else {
            Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
            Log.d("TransactionFragment.cameraPermissionLauncher", "Camera permission denied")
        }
    }

    /// Launches camera and processes image once taken
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && ::imagePreview.isInitialized && capturedImageUri != null) {
            Glide.with(this).load(capturedImageUri).into(imagePreview)
            selectedImageUri = capturedImageUri
            imagePreview.visibility = View.VISIBLE
            Log.d("TransactionFragment.cameraLauncher", "Photo captured and preview loaded")
        }
    }

    /// Main view initialization for the fragment. Sets up adapter, loads data and listeners.
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTransactionBinding.inflate(inflater, container, false)
        dbHelper = DatabaseHelper(requireContext())
        sessionManager = SessionManager(requireContext())
        userId = sessionManager.getUserId()

        // Handle edge case where user session is missing
        if (userId == -1) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            Log.d("TransactionFragment.onCreateView", "No valid user session found")
            return binding.root
        }

        // Setup transaction list
        transactionAdapter = TransactionAdapter(mutableListOf(), dbHelper) { item ->
            editTransactionItem(item)
        }

        binding.transactionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.transactionRecyclerView.adapter = transactionAdapter

        // ===== AUTO-APPLY CURRENT MONTH FILTER =====
        val calendar = Calendar.getInstance()

// Start of month: 01/MM/yyyy
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.time

// End of month: last day of current month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endOfMonth = calendar.time

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val startDateStr = sdf.format(startOfMonth)
        val endDateStr = sdf.format(endOfMonth)

// Pre-fill the UI fields
        binding.startDateInput.setText(startDateStr)
        binding.endDateInput.setText(endDateStr)

// Trigger the filtered transaction load
        applySearchAndFilters(startDateStr, endDateStr, "")


        binding.inputSearchTransaction.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim().lowercase(Locale.getDefault())
                val start = binding.startDateInput.text.toString()
                val end = binding.endDateInput.text.toString()
                applySearchAndFilters(start, end, query)
            }


        })

        Log.d("TransactionFragment", "Applied default filter for: $startDateStr to $endDateStr")
// ===========================================

        setupDatePickers()

        // Button: Add new transaction
        binding.applyFilterButton.setOnClickListener {
            val start = binding.startDateInput.text.toString()
            val end = binding.endDateInput.text.toString()
            val query = binding.inputSearchTransaction.text.toString().trim().lowercase(Locale.getDefault())
            Log.d("TransactionFragment", "Button clicked")
            if (start.isBlank() || end.isBlank()) {
                Toast.makeText(requireContext(), "Pick both dates", Toast.LENGTH_SHORT).show()
            } else {
                applySearchAndFilters(start, end, query)
            }
        }

        binding.addTransactionFab.setOnClickListener {
            Log.d("TransactionFragment", "FAB clicked")
            showAddTransactionDialog()

            Toast.makeText(requireContext(), "Add Transaction FAB clicked", Toast.LENGTH_SHORT).show()
        }
        val categories = dbHelper.getCategoriesForUser(userId)
        categoryMap = categories.associateBy({ it.id }, { it.title.lowercase(Locale.getDefault()) })

        val categoryTitles = mutableListOf("All Categories")
        val categoryIds = mutableListOf(-1)
        categories.forEach {
            categoryTitles.add(it.title)
            categoryIds.add(it.id)
        }

        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryTitles)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategoryFilter.adapter = categoryAdapter

        binding.spinnerCategoryFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedFilterCategoryId = categoryIds[position]
                triggerCombinedFilter()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedFilterCategoryId = -1
            }
        }
        binding.toggleDateFilterButton.setOnClickListener {
            isDateFilterVisible = !isDateFilterVisible
            binding.filterBar.visibility = if (isDateFilterVisible) View.VISIBLE else View.GONE
            binding.toggleDateFilterButton.text = if (isDateFilterVisible) "Hide Date Filter" else "Date Filter"

            if (!isDateFilterVisible) {
                resetDateFiltersToDefault()
            }
        }

        binding.toggleCategoryFilterButton.setOnClickListener {
            isCategoryFilterVisible = !isCategoryFilterVisible
            binding.categoryFilterBar.visibility = if (isCategoryFilterVisible) View.VISIBLE else View.GONE
            binding.toggleCategoryFilterButton.text = if (isCategoryFilterVisible) "Hide Category Filter" else "Category Filter"

            if (!isCategoryFilterVisible) {
                selectedFilterCategoryId = -1
                binding.spinnerCategoryFilter.setSelection(0) // Reset to "All Categories"
                triggerCombinedFilter()
            }
        }

        binding.hideDateFilterButton.setOnClickListener {
            binding.toggleDateFilterButton.performClick()
        }

        binding.hideCategoryFilterButton.setOnClickListener {
            binding.toggleCategoryFilterButton.performClick()
        }





        Log.d("TransactionFragment.onCreateView", "TransactionFragment view created and initialized")
        return binding.root
    }

    /// Loads all user transactions from the database and displays in RecyclerView.
    private fun loadTransactions() {
        val transactions = dbHelper.getTransactionsForUser(userId)
        transactionAdapter.updateList(transactions)

        Log.d("TransactionFragment.loadTransactions", "Loaded ${transactions.size} transactions for user $userId")
    }

    /// Enables the calendar date picker inputs for filtering transactions.
    private fun setupDatePickers() {
        val cal = Calendar.getInstance()

        // Start date picker setup
        binding.startDateInput.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                binding.startDateInput.setText("%02d/%02d/%04d".format(d, m + 1, y))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // End date picker setup
        binding.endDateInput.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                binding.endDateInput.setText("%02d/%02d/%04d".format(d, m + 1, y))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        Log.d("TransactionFragment.setupDatePickers", "Date pickers initialized")
    }

    /// Re-applies all active filters: search query, date range, category ID
    private fun triggerCombinedFilter() {
        Log.d("TransactionFragment.triggerCombinedFilter", "Re-evaluating all filters...")

        // Step 1: Extract current values from inputs
        val startDateStr = binding.startDateInput.text.toString().trim()
        val endDateStr = binding.endDateInput.text.toString().trim()
        val searchQuery = binding.inputSearchTransaction.text.toString().trim().lowercase(Locale.getDefault())

        // Step 2: Validate required date fields
        if (startDateStr.isBlank() || endDateStr.isBlank()) {
            Log.w("TransactionFragment.triggerCombinedFilter", "Start or End date is blank — skipping filter")
            Toast.makeText(requireContext(), "Date range is invalid", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Step 3: Parse date range safely
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val startDate = sdf.parse(startDateStr)
            val endDate = sdf.parse(endDateStr)

            if (startDate == null || endDate == null) {
                Log.w("TransactionFragment.triggerCombinedFilter", "Failed to parse dates")
                return
            }

            // Step 4: Pull all transactions from DB
            val allTransactions = dbHelper.getTransactionsForUser(userId)
            val allCategories = dbHelper.getCategoriesForUser(userId)
            categoryMap = allCategories.associateBy({ it.id }, { it.title.lowercase(Locale.getDefault()) })

            // Step 5: Apply filters together
            val filtered = allTransactions.filter { txn ->
                try {
                    val txnDate = sdf.parse(txn.Date)
                    val isInRange = txnDate != null && !txnDate.before(startDate) && !txnDate.after(endDate)

                    val matchesSearch = txn.title.lowercase(Locale.getDefault()).contains(searchQuery)
                            || txn.description.lowercase(Locale.getDefault()).contains(searchQuery)
                            || (categoryMap[txn.categoryId]?.contains(searchQuery) == true)

                    val matchesCategory = selectedFilterCategoryId == -1 || txn.categoryId == selectedFilterCategoryId

                    isInRange && matchesSearch && matchesCategory
                } catch (e: Exception) {
                    Log.e("TransactionFragment.triggerCombinedFilter", "Error filtering txn '${txn.title}'", e)
                    false
                }
            }

            // Step 6: Update UI
            fullTransactionList = filtered
            transactionAdapter.updateList(filtered)

            val calendar = Calendar.getInstance()
            val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
            val year = calendar.get(Calendar.YEAR)
            val total = filtered.sumOf { it.amount }

            binding.totalSpentTextView.text = "Total Spent for $monthName $year: R%.2f".format(total)

            Log.i("TransactionFragment.triggerCombinedFilter", "Final filter applied | ${filtered.size} transactions shown")

        } catch (e: Exception) {
            Log.e("TransactionFragment.triggerCombinedFilter", "Unexpected crash during filtering", e)
        }
    }

    private fun resetDateFiltersToDefault() {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // 1st of current month
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start = sdf.format(cal.time)
        binding.startDateInput.setText(start)

        // End of current month
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        val end = sdf.format(cal.time)
        binding.endDateInput.setText(end)

        triggerCombinedFilter()
    }


    /// Applies date range filtering + text search in a single pass.
/// Called whenever user updates date or search input.
    private fun applySearchAndFilters(startDate: String, endDate: String, query: String) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val start = sdf.parse(startDate)
        val end = sdf.parse(endDate)

        val all = dbHelper.getTransactionsForUser(userId)
        val categories = dbHelper.getCategoriesForUser(userId)
        categoryMap = categories.associateBy({ it.id }, { it.title.lowercase(Locale.getDefault()) })

        val filtered = all.filter {
            try {
                val txnDate = sdf.parse(it.Date)
                val dateValid = txnDate != null && !txnDate.before(start) && !txnDate.after(end)

                val titleMatch = it.title.lowercase(Locale.getDefault()).contains(query)
                val descMatch = it.description.lowercase(Locale.getDefault()).contains(query)
                val categoryMatch = categoryMap[it.categoryId]?.contains(query) == true

                dateValid && (titleMatch || descMatch || categoryMatch)
            } catch (e: Exception) {
                Log.e("TransactionFragment", "Error filtering transaction '${it.title}'", e)
                false
            }
        }

        fullTransactionList = filtered
        transactionAdapter.updateList(filtered)

        // Update summary text
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
        val total = filtered.sumOf { it.amount }
        binding.totalSpentTextView.text = "Total Spent for $monthName $currentYear: R%.2f".format(total)

        Log.d("TransactionFragment", "Search+Filter applied: ${filtered.size} transactions shown")
    }


    /// Filters transactions within the selected date range and updates the list.
    private fun loadFilteredTransactions(startDate: String, endDate: String) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val start = sdf.parse(startDate)
        val end = sdf.parse(endDate)

        val all = dbHelper.getTransactionsForUser(userId)
        val categories = dbHelper.getCategoriesForUser(userId)
        categoryMap = categories.associateBy({ it.id }, { it.title.lowercase(Locale.getDefault()) })

        val filtered = all.filter {
            try {
                val txnDate = sdf.parse(it.Date)
                txnDate != null && !txnDate.before(start) && !txnDate.after(end)
            } catch (e: Exception) {
                Log.e("TransactionFragment.loadFilteredTransactions", "Error parsing date for txn: ${it.Date}", e)
                false
            }
        }
        fullTransactionList = filtered // store full list for search


        transactionAdapter.updateList(filtered)
        binding.transactionRecyclerView.adapter?.notifyDataSetChanged()

        // Get current month and year from system calendar
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
        val total = filtered.sumOf { it.amount }
        binding.totalSpentTextView.text = "Total Spent for $monthName $currentYear: R%.2f".format(total)

        Log.d("TransactionFragment.loadFilteredTransactions", "Filtered ${filtered.size} transactions from ${all.size} total")
    }
    /// Initiates editing of a transaction's category only
    private fun editTransactionItem(item: TransactionItem) {
        // Store item in fragment memory
        editedTransaction = item

        Log.d("TransactionFragment.editTransactionItem", "Editing transaction ID=${item.id} | Only category is editable")

        // Open the dialog in edit mode
        showAddTransactionDialog(isEdit = true)
    }


    /// Shows a form dialog allowing users to add a new transaction (with image support).
    /// Dynamically fills dropdowns from DB, lets user pick/capture image, and inserts the transaction on confirmation.
    fun showAddTransactionDialog(isEdit: Boolean = false) {
        // Inflate custom dialog layout
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_transaction, null)

        // Get references to input fields and spinners
        val titleInput = dialogView.findViewById<EditText>(R.id.inputTitle)
        val typeSpinner = dialogView.findViewById<Spinner>(R.id.spinnerTransactionType)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.inputDescription)
        val amountInput = dialogView.findViewById<EditText>(R.id.inputAmount)
        val dateInput = dialogView.findViewById<EditText>(R.id.inputStartDate)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val confirmCheckbox = dialogView.findViewById<CheckBox>(R.id.checkboxConfirmFields)


        // Image preview and buttons for gallery or camera
        imagePreview = dialogView.findViewById(R.id.transactionImagePreview)
        val imageButton = dialogView.findViewById<Button>(R.id.selectImageButton)
        val captureButton = dialogView.findViewById<Button>(R.id.captureImageButton)

        // Populate transaction type spinner
        val typeOptions = listOf("Expense", "Refund")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, typeOptions)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = typeAdapter

        // Populate category spinner from database
        val categories = dbHelper.getCategoriesForUser(userId)
        categoryMap = categories.associateBy({ it.id }, { it.title.lowercase(Locale.getDefault()) })

        val categoryTitles = categories.map { it.title }
        val categoryIds = categories.map { it.id }
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryTitles)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        // Handle category selection
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCategoryId = categoryIds[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>) { selectedCategoryId = -1 }
        }

        // Handle transaction type selection
        typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedType = parent.getItemAtPosition(position).toString().lowercase(Locale.getDefault())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedType = "expense" // fallback value
            }
        }

        // Launch gallery image picker
        imageButton.setOnClickListener { imagePickerLauncher.launch("image/*") }

        // Launch camera (after permission)
        captureButton.setOnClickListener { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) }
        if (isEdit && editedTransaction != null) {
            val txn = editedTransaction!!

            titleInput.setText(txn.title)
            descriptionInput.setText(txn.description)
            amountInput.setText(txn.amount.toString())
            dateInput.setText(txn.Date)

            // Show image if available
            if (!txn.imageUrl.isNullOrBlank()) {
                imagePreview.visibility = View.VISIBLE
                Glide.with(this).load(txn.imageUrl).into(imagePreview)
                selectedImageUri = Uri.parse(txn.imageUrl)
            }

            // Set selected transaction type in spinner
            val typeIndex = typeOptions.indexOfFirst { it.equals(txn.type, ignoreCase = true) }
            if (typeIndex >= 0) typeSpinner.setSelection(typeIndex)

            // Disable everything except category spinner
            titleInput.isEnabled = false
            descriptionInput.isEnabled = false
            amountInput.isEnabled = false
            dateInput.isEnabled = false
            typeSpinner.isEnabled = false
            imageButton.isEnabled = false
            captureButton.isEnabled = false
        }

        // Date picker for transaction date
        val cal = Calendar.getInstance()
        dateInput.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                dateInput.setText("%02d/%02d/%04d".format(d, m + 1, y))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Show dialog and handle Save/Cancel
        val builder = AlertDialog.Builder(requireContext())
            .setTitle(if (isEdit) "Edit Transaction Category" else "Add Transaction")
            .setView(dialogView)
            .setPositiveButton("Save", null) // must be null to override later
            .setNegativeButton("Cancel", null)

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val title = titleInput.text.toString().trim()
            val type = selectedType
            val desc = descriptionInput.text.toString().trim()
            val amount = amountInput.text.toString().toDoubleOrNull()
            val date = dateInput.text.toString().trim()
            val img = selectedImageUri?.toString()

            // Confirm checkbox must be ticked
            if (!confirmCheckbox.isChecked) {
                Toast.makeText(requireContext(), "Please confirm all fields before saving", Toast.LENGTH_SHORT).show()
                Log.d("TransactionFragment", "Save blocked: confirmation checkbox not ticked")
                return@setOnClickListener
            }

            // Field validation
            if (title.isNotBlank() && amount != null && selectedCategoryId != -1 && date.isNotBlank()) {
                if (isEdit && editedTransaction != null) {
                    // Call updateTransaction() (already implemented)
                    // Only update category for the existing transaction
                    val success = dbHelper.updateTransactionCategory(
                        transactionId = editedTransaction!!.id,
                        newCategoryId = selectedCategoryId
                    )

                    if (success) {
                        Log.d("TransactionFragment", "Transaction category updated successfully for ID=${editedTransaction!!.id}")
                        Toast.makeText(requireContext(), "Transaction category updated", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("TransactionFragment", "Failed to update category for ID=${editedTransaction!!.id}")
                        Toast.makeText(requireContext(), "Failed to update transaction", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    // Insert transaction
                    dbHelper.insertTransaction(userId, selectedCategoryId, type, title, desc, amount, date, img)
                }

                loadTransactions()
                dialog.dismiss()
                Log.d("TransactionFragment", "Transaction saved successfully")
            } else {
                Toast.makeText(requireContext(), "Please fill out all fields correctly", Toast.LENGTH_SHORT).show()
                Log.d("TransactionFragment", "Save blocked: invalid input")
            }
        }

    }
    /// Launches camera intent and prepares a file destination to store the captured image.
    /// Stores image URI in memory and fires the camera launcher with that path.
    private fun launchCamera() {
        // Create file location for new image with a unique timestamp
        val file = File(requireContext().getExternalFilesDir(null), "photo_${System.currentTimeMillis()}.jpg")

        // Convert file to content URI using FileProvider
        capturedImageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)

        // Trigger camera with URI
        cameraLauncher.launch(capturedImageUri)

        Log.d("TransactionFragment.launchCamera", "Camera launched in TransactionFragment using function launchCamera with file: ${file.absolutePath}")
    }

}

