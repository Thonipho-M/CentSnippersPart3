// TransactionFragment.kt
package com.centsnippers.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.centsnippers.R
import com.centsnippers.adapters.TransactionAdapter
import com.centsnippers.databinding.FragmentTransactionBinding
import com.centsnippers.models.TransactionItem
import com.centsnippers.utils.FirebaseCategoryService
import com.centsnippers.utils.FirebaseTransactionService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class TransactionFragment : Fragment() {

    private lateinit var binding: FragmentTransactionBinding
    private lateinit var transactionAdapter: TransactionAdapter
    private var fullTransactionList: List<TransactionItem> = listOf()

    private val userId: String? get() = FirebaseAuth.getInstance().currentUser?.uid
    private val storageRef = FirebaseStorage.getInstance().reference

    private var selectedCategoryId: Int = -1
    private var selectedImageUri: Uri? = null
    private lateinit var categoryTitleList: List<String>
    private lateinit var categoryIdList: List<Int>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionAdapter = TransactionAdapter(mutableListOf())
        binding.transactionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.transactionRecyclerView.adapter = transactionAdapter

        setupDatePickers()
        setupSearchField()

        binding.applyFilterButton.setOnClickListener {
            val start = binding.startDateInput.text.toString()
            val end = binding.endDateInput.text.toString()
            if (start.isBlank() || end.isBlank()) {
                Toast.makeText(requireContext(), "Select both dates", Toast.LENGTH_SHORT).show()
            } else {
                filterTransactionsByDate(start, end)
            }
        }

        binding.addTransactionFab.setOnClickListener {
            showAddTransactionDialog()
        }

        loadTransactions()
    }

    private fun loadTransactions() {
        FirebaseTransactionService.getTransactions { list ->
            fullTransactionList = list
            transactionAdapter.updateList(list)

            val total = list.sumOf { it.amount }
            val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
            val year = Calendar.getInstance().get(Calendar.YEAR)
            binding.totalSpentTextView.text = "Total Spent for $monthName $year: R%.2f".format(total)
        }
    }

    private fun setupSearchField() {
        binding.inputSearchTransaction.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s.toString().lowercase(Locale.getDefault())
                val filtered = fullTransactionList.filter {
                    it.title.lowercase(Locale.getDefault()).contains(query) ||
                            it.description.lowercase(Locale.getDefault()).contains(query)
                }
                transactionAdapter.updateList(filtered)
            }
        })
    }

    private fun setupDatePickers() {
        val cal = Calendar.getInstance()
        binding.startDateInput.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                binding.startDateInput.setText("%02d/%02d/%04d".format(d, m + 1, y))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.endDateInput.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                binding.endDateInput.setText("%02d/%02d/%04d".format(d, m + 1, y))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun filterTransactionsByDate(start: String, end: String) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val startDate = sdf.parse(start)
        val endDate = sdf.parse(end)

        val filtered = fullTransactionList.filter {
            try {
                val txnDate = sdf.parse(it.Date)
                txnDate != null && !txnDate.before(startDate) && !txnDate.after(endDate)
            } catch (e: Exception) {
                false
            }
        }
        transactionAdapter.updateList(filtered)

        val total = filtered.sumOf { it.amount }
        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
        val year = Calendar.getInstance().get(Calendar.YEAR)
        binding.totalSpentTextView.text = "Total Spent for $monthName $year: R%.2f".format(total)
    }

    private fun showAddTransactionDialog() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_transaction, null)

        val titleInput = view.findViewById<EditText>(R.id.inputTitle)
        val descInput = view.findViewById<EditText>(R.id.inputDescription)
        val amountInput = view.findViewById<EditText>(R.id.inputAmount)
        val dateInput = view.findViewById<EditText>(R.id.inputDate)
        val categorySpinner = view.findViewById<Spinner>(R.id.spinnerCategory)
        val imagePreview = view.findViewById<ImageView>(R.id.imagePreview)
        val galleryButton = view.findViewById<Button>(R.id.btnGallery)
        val cameraButton = view.findViewById<Button>(R.id.btnCamera)

        dateInput.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                dateInput.setText("%02d/%02d/%04d".format(d, m + 1, y))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        galleryButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        cameraButton.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureLauncher.launch(intent)
        }

        FirebaseCategoryService.getCategoriesForUser { categoryList ->
            categoryTitleList = categoryList.map { it.title }
            categoryIdList = categoryList.map { it.id }

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryTitleList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            categorySpinner.adapter = adapter

            categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    selectedCategoryId = categoryIdList[position]
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Transaction")
            .setView(view)
            .setPositiveButton("Save") { dialog, _ ->
                val title = titleInput.text.toString()
                val desc = descInput.text.toString()
                val amount = amountInput.text.toString().toDoubleOrNull()
                val date = dateInput.text.toString()

                if (title.isBlank() || amount == null || date.isBlank()) {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (selectedImageUri != null) {
                    val imageName = "txn_${System.currentTimeMillis()}.jpg"
                    val imgRef = storageRef.child("transactions/$imageName")
                    imgRef.putFile(selectedImageUri!!)
                        .continueWithTask { task -> imgRef.downloadUrl }
                        .addOnSuccessListener { uri ->
                            saveTransactionToFirebase(title, desc, amount, date, selectedCategoryId, uri.toString())
                        }
                } else {
                    saveTransactionToFirebase(title, desc, amount, date, selectedCategoryId, null)
                }

                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveTransactionToFirebase(
        title: String,
        desc: String,
        amount: Double,
        date: String,
        categoryId: Int,
        imageUrl: String?
    ) {
        val txn = TransactionItem(
            userId = userId ?: "",
            title = title,
            description = desc,
            amount = amount,
            Date = date,
            categoryId = categoryId,
            type = "expense",
            imageUrl = imageUrl
        )

        FirebaseTransactionService.insertTransaction(txn) { success, error ->
            if (success) {
                Toast.makeText(requireContext(), "Transaction added", Toast.LENGTH_SHORT).show()
                loadTransactions()
            } else {
                Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val bitmap = result.data?.extras?.get("data") as? Bitmap
        bitmap?.let {
            val uri = Uri.parse(MediaStore.Images.Media.insertImage(requireContext().contentResolver, it, "Temp", null))
            selectedImageUri = uri
        }
    }
}