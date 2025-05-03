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
import java.util.*
//Add the top right menu
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import android.content.Intent
import java.text.SimpleDateFormat
import androidx.navigation.fragment.findNavController
import com.centsnippers.MainActivity

class TransactionFragment : Fragment() {

    private lateinit var binding: FragmentTransactionBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var transactionAdapter: TransactionAdapter

    private var selectedImageUri: Uri? = null
    private var selectedCategoryId: Int = -1
    private var userId: Int = -1

    private lateinit var imagePreview: ImageView

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
        if (::imagePreview.isInitialized && uri != null) {
            Glide.with(this)
                .load(uri)
                .into(imagePreview)
            imagePreview.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransactionBinding.inflate(inflater, container, false)
        dbHelper = DatabaseHelper(requireContext())
        sessionManager = SessionManager(requireContext())
        userId = sessionManager.getUserId()
        setHasOptionsMenu(true)

        if (userId == -1) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return binding.root
        }

        transactionAdapter = TransactionAdapter(mutableListOf(), dbHelper)
        binding.transactionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.transactionRecyclerView.adapter = transactionAdapter

        loadTransactions()
        setupDatePickers()


        binding.addTransactionFab.setOnClickListener {
            showAddTransactionDialog()
        }
        binding.applyFilterButton.setOnClickListener {
            val start = binding.startDateInput.text.toString()
            val end = binding.endDateInput.text.toString()

            if (start.isBlank() || end.isBlank()) {
                Toast.makeText(requireContext(), "Please select both dates", Toast.LENGTH_SHORT).show()
            } else {
                loadFilteredTransactions(start, end)
            }
        }

        binding.transactionRecyclerView.adapter?.notifyDataSetChanged()

        return binding.root
    }

    private fun loadTransactions() {
        val transactions = dbHelper.getTransactionsForUser(userId)
        transactionAdapter.updateList(transactions)
        val total = transactions.sumOf { it.amount }
        binding.totalTransactionTextView.text = "Total: R%.2f".format(total)
    }
    private fun setupDatePickers() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

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
    private fun loadFilteredTransactions(startDate: String, endDate: String) {
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
    }



    private fun showAddTransactionDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_transaction, null)

        val titleInput = dialogView.findViewById<EditText>(R.id.inputTitle)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.inputDescription)
        val amountInput = dialogView.findViewById<EditText>(R.id.inputAmount)
        val startDateInput = dialogView.findViewById<EditText>(R.id.inputStartDate)
        val endDateInput = dialogView.findViewById<EditText>(R.id.inputEndDate)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        imagePreview = dialogView.findViewById(R.id.transactionImagePreview)
        val imageButton = dialogView.findViewById<Button>(R.id.selectImageButton)

        // Populate spinner with category names
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

        imageButton.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

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
                } else {
                    Toast.makeText(requireContext(), "Please complete all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    @Deprecated("Using legacy menu method for now")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.top_right_menu, menu)
    }

    @Deprecated("Using legacy menu method for now")
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
                Toast.makeText(requireContext(), "Goals clicked", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.rewardsFragment -> {
                Toast.makeText(requireContext(), "Rewards clicked", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.helpPage -> {
                Toast.makeText(requireContext(), "Help clicked", Toast.LENGTH_SHORT).show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
