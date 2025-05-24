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
import com.centsnippers.adapters.TransactionAdapter
import com.centsnippers.data.DatabaseHelper
import com.centsnippers.databinding.FragmentTransactionBinding
import com.centsnippers.models.TransactionItem
import com.centsnippers.utils.SessionManager
import com.centsnippers.models.*
import java.util.*
//Add the top right menu

import android.widget.Toast
import android.content.Intent
import android.util.Log
import java.text.SimpleDateFormat
import androidx.navigation.fragment.findNavController
import com.centsnippers.MainActivity
import java.io.File
import androidx.core.content.FileProvider

// TransactionFragment.kt
// This fragment handles all the logic for viewing, adding, and filtering user transactions.
// It manages image capture, gallery selection, and passes data to the adapter.

class TransactionFragment : Fragment() {

    // Fragment binding + helpers
    private lateinit var binding: FragmentTransactionBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var transactionAdapter: TransactionAdapter

    // Image capture and selection URIs
    private var selectedImageUri: Uri? = null
    private var capturedImageUri: Uri? = null

    // Misc variables
    private var selectedCategoryId: Int = -1
    private var userId: Int = -1
    private lateinit var imagePreview: ImageView

    // Handle gallery image selection
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
        if (::imagePreview.isInitialized && uri != null) {
            Glide.with(this)
                .load(uri)
                .into(imagePreview)
            imagePreview.visibility = View.VISIBLE
            Log.d("TransactionFragment", "Image selected from gallery: $uri")
        }
    }
    // Ask permission for camera use
    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            Log.d("TransactionFragment", "Camera permission granted. Launching camera.")
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to take photos.", Toast.LENGTH_SHORT).show()
            Log.w("TransactionFragment", "Camera permission denied by user.")
        }
    }


    // Handle camera image capture
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && ::imagePreview.isInitialized && capturedImageUri != null) {
            Glide.with(this)
                .load(capturedImageUri)
                .into(imagePreview)
            selectedImageUri = capturedImageUri
            imagePreview.visibility = View.VISIBLE
            Log.d("TransactionFragment", "Camera image captured: $capturedImageUri")
        } else {
            Log.w("TransactionFragment", "Camera capture failed or cancelled")
        }
    }

    // Called when the fragment is being created. Sets up recycler, listeners, and loads user data.
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d("TransactionFragment", "onCreateView called")

        binding = FragmentTransactionBinding.inflate(inflater, container, false)
        dbHelper = DatabaseHelper(requireContext())
        sessionManager = SessionManager(requireContext())
        userId = sessionManager.getUserId()
        setHasOptionsMenu(true)

        if (userId == -1) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            Log.w("TransactionFragment", "userId was -1. Cannot proceed.")
            return binding.root
        }

        transactionAdapter = TransactionAdapter(mutableListOf(), dbHelper)
        binding.transactionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.transactionRecyclerView.adapter = transactionAdapter

        loadTransactions()
        setupDatePickers()

        // Floating action button to open the add transaction dialog
        binding.addTransactionFab.setOnClickListener {
            showAddTransactionDialog()
        }

        // Filter button clicked
        binding.applyFilterButton.setOnClickListener {
            val start = binding.startDateInput.text.toString()
            val end = binding.endDateInput.text.toString()

            if (start.isBlank() || end.isBlank()) {
                Toast.makeText(requireContext(), "Please select both dates", Toast.LENGTH_SHORT).show()
                Log.w("TransactionFragment", "Start or end date blank when filtering")
            } else {
                loadFilteredTransactions(start, end)
            }
        }

        return binding.root
    }

    // Loads all transactions for the logged-in user
    private fun loadTransactions() {
        Log.d("TransactionFragment", "Loading all transactions for user $userId")
        val transactions = dbHelper.getTransactionsForUser(userId)
        transactionAdapter.updateList(transactions)
    }

    // Sets up start and end date pickers
    private fun setupDatePickers() {
        Log.d("TransactionFragment", "Setting up date pickers")
        val calendar = Calendar.getInstance()

        binding.startDateInput.setOnClickListener {
            DatePickerDialog(requireContext(), { _, year, month, day ->
                binding.startDateInput.setText(String.format("%02d/%02d/%04d", day, month + 1, year))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.endDateInput.setOnClickListener {
            DatePickerDialog(requireContext(), { _, year, month, day ->
                binding.endDateInput.setText(String.format("%02d/%02d/%04d", day, month + 1, year))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    // Filters transactions by date range
    private fun loadFilteredTransactions(startDate: String, endDate: String) {
        Log.d("TransactionFragment", "Filtering transactions from $startDate to $endDate")
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val start = sdf.parse(startDate)
        val end = sdf.parse(endDate)

        val allTransactions = dbHelper.getTransactionsForUser(userId)

        val filtered = allTransactions.filter {
            val txnStart = sdf.parse(it.startDate)
            val txnEnd = sdf.parse(it.endDate)
            txnStart != null && txnEnd != null && !txnEnd.before(start) && !txnStart.after(end)
        }

        transactionAdapter.updateList(filtered)
        binding.transactionRecyclerView.adapter?.notifyDataSetChanged()

        val totalSpent = filtered.sumOf { it.amount }
        binding.totalSpentTextView.text = "Total Spent: R%.2f".format(totalSpent)
    }

    // Displays a dialog for creating a new transaction
    private fun showAddTransactionDialog() {
        Log.d("TransactionFragment", "Opening Add Transaction Dialog")
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_transaction, null)

        val titleInput = dialogView.findViewById<EditText>(R.id.inputTitle)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.inputDescription)
        val amountInput = dialogView.findViewById<EditText>(R.id.inputAmount)
        val startDateInput = dialogView.findViewById<EditText>(R.id.inputStartDate)
        val endDateInput = dialogView.findViewById<EditText>(R.id.inputEndDate)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        imagePreview = dialogView.findViewById(R.id.transactionImagePreview)
        val imageButton = dialogView.findViewById<Button>(R.id.selectImageButton)
        val captureImageButton = dialogView.findViewById<Button>(R.id.captureImageButton)

        // Trigger camera
        captureImageButton.setOnClickListener {
            Log.d("TransactionFragment", "Checking camera permission...")
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }


        // Populate categories into dropdown
        val categories = dbHelper.getCategoriesForUser(userId)
        val categoryTitles = categories.map { it.title }
        val categoryIds = categories.map { it.id }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryTitles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCategoryId = categoryIds[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedCategoryId = -1
            }
        }

        // Open gallery to pick an image
        imageButton.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // Setup date pickers for the dialog
        val calendar = Calendar.getInstance()
        startDateInput.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                startDateInput.setText(String.format("%02d/%02d/%04d", d, m + 1, y))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        endDateInput.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                endDateInput.setText(String.format("%02d/%02d/%04d", d, m + 1, y))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Create and show the dialog
        AlertDialog.Builder(requireContext())
            .setTitle("Add Transaction")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = titleInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()
                val amount = amountInput.text.toString().toDoubleOrNull()
                val startDate = startDateInput.text.toString().trim()
                val endDate = endDateInput.text.toString().trim()
                val imageUrl = selectedImageUri?.toString()

                if (title.isNotBlank() && amount != null && selectedCategoryId != -1) {
                    dbHelper.insertTransaction(
                        userId, selectedCategoryId, title, description, amount,
                        startDate, endDate, imageUrl
                    )
                    loadTransactions()
                    Log.i("TransactionFragment", "Transaction saved successfully")
                } else {
                    Toast.makeText(requireContext(), "Please complete all fields", Toast.LENGTH_SHORT).show()
                    Log.w("TransactionFragment", "User tried to save with missing fields")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Helper method to launch the camera after permission is granted
    private fun launchCamera() {
        val imageFile = File(requireContext().getExternalFilesDir(null), "photo_${System.currentTimeMillis()}.jpg")
        capturedImageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", imageFile)
        Log.d("TransactionFragment", "Camera intent URI prepared: $capturedImageUri")
        cameraLauncher.launch(capturedImageUri)
    }


    // Inflates the top-right menu (legacy)
    @Deprecated("Using legacy menu method for now")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.top_right_menu, menu)
        Log.d("TransactionFragment", "Top-right menu inflated")
    }

    // Handles menu item selections (legacy)
    @Deprecated("Using legacy menu method for now")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.logoutButton -> {
                sessionManager.clearSession()
                Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireActivity(), MainActivity::class.java))
                requireActivity().finish()
                Log.i("TransactionFragment", "User logged out")
                true
            }
            R.id.goalsFragment -> {
                Toast.makeText(requireContext(), "Goals clicked", Toast.LENGTH_SHORT).show()
                Log.d("TransactionFragment", "Goals nav triggered")
                true
            }
            R.id.rewardsFragment -> {
                Toast.makeText(requireContext(), "Rewards clicked", Toast.LENGTH_SHORT).show()
                Log.d("TransactionFragment", "Rewards nav triggered")
                true
            }
            R.id.helpPage -> {
                Toast.makeText(requireContext(), "Help clicked", Toast.LENGTH_SHORT).show()
                Log.d("TransactionFragment", "Help nav triggered")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
