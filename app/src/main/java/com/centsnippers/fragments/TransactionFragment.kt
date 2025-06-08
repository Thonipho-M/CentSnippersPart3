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
import com.centsnippers.databinding.FragmentTransactionBinding
import com.centsnippers.models.TransactionItem
import com.centsnippers.utils.FirebaseTransactionService
import java.text.SimpleDateFormat
import java.util.*

class TransactionFragment : Fragment() {

    private lateinit var binding: FragmentTransactionBinding
    private lateinit var transactionAdapter: TransactionAdapter
    private var fullTransactionList: List<TransactionItem> = listOf()

    // Required for Firebase Auth checks
    private val userId: String? = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTransactionBinding.inflate(inflater, container, false)

        // Setup RecyclerView and Adapter
        transactionAdapter = TransactionAdapter(mutableListOf())
        binding.transactionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.transactionRecyclerView.adapter = transactionAdapter

        // Setup UI listeners
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
            Toast.makeText(requireContext(), "Add Transaction Dialog Coming Soon", Toast.LENGTH_SHORT).show()
            // You can launch a dialog or navigate to a new screen to add a transaction
        }

        // Load all transactions from Firebase
        loadTransactions()
        return binding.root
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
}
