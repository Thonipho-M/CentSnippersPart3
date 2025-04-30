package com.centsnippers.fragments


import android.widget.LinearLayout
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.centsnippers.adapters.BudgetAdapter
import com.centsnippers.databinding.FragmentBudgetBinding
import com.centsnippers.models.BudgetItem

class BudgetFragment : Fragment() {

    // ViewBinding setup
    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!

    // Adapter and budget data list
    private lateinit var adapter: BudgetAdapter
    private val budgetList = mutableListOf<BudgetItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate layout using ViewBinding
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up spinner for budget reset cycle
        val cycles = listOf("Monthly", "Weekly", "Yearly")
        val adapterCycle = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, cycles)
        binding.spinnerCycle.adapter = adapterCycle

        // Initialize the RecyclerView and Adapter
        adapter = BudgetAdapter(budgetList,
            onEdit = { position -> showEditDialog(position) },
            onDelete = { position -> deleteBudgetItem(position) }
        )

        binding.recyclerBudgets.adapter = adapter
        binding.recyclerBudgets.layoutManager = LinearLayoutManager(requireContext())

        // Handle the "Add Budget" button
        binding.btnAddBudget.setOnClickListener {
            val category = binding.inputCategory.text.toString()
            val amount = binding.inputAmount.text.toString()
            val cycle = binding.spinnerCycle.selectedItem.toString()

            if (category.isNotBlank() && amount.isNotBlank()) {
                val newItem = BudgetItem(category, amount, cycle)
                budgetList.add(newItem)
                adapter.notifyItemInserted(budgetList.size - 1)
                updateTotalBudget()
                clearInputs()
            }
        }
    }

    private fun updateTotalBudget() {
        val total = budgetList.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        binding.textTotalBudget.text = "Total Budget: R%.2f".format(total)
    }


    // Show an AlertDialog to edit a budget item
    private fun showEditDialog(position: Int) {
        val item = budgetList[position]

        val dialogView = LayoutInflater.from(requireContext()).inflate(null, null)
        val inputCategory = EditText(requireContext())
        val inputAmount = EditText(requireContext())
        inputCategory.hint = "Category"
        inputAmount.hint = "Amount"
        inputCategory.setText(item.category)
        inputAmount.setText(item.amount)

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 0)
            addView(inputCategory)
            addView(inputAmount)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Budget")
            .setView(layout)
            .setPositiveButton("Update") { _, _ ->
                item.category = inputCategory.text.toString()
                item.amount = inputAmount.text.toString()
                adapter.notifyItemChanged(position)
                updateTotalBudget()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Delete item from list
    private fun deleteBudgetItem(position: Int) {
        budgetList.removeAt(position)
        adapter.notifyItemRemoved(position)
        updateTotalBudget()
    }

    // Reset input fields
    private fun clearInputs() {
        binding.inputCategory.text?.clear()
        binding.inputAmount.text?.clear()
        binding.spinnerCycle.setSelection(0)
    }

    // Clean up binding to prevent memory leaks
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
