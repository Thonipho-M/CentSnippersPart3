// IncomeFragment.kt — Enhanced with logging and detailed comments
// Purpose: Displays and manages income entries, including add/edit/delete

package com.centsnippers.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.centsnippers.R
import com.centsnippers.adapters.IncomeAdapter
import com.centsnippers.data.DatabaseHelper
import com.centsnippers.databinding.FragmentIncomeBinding
import com.centsnippers.models.IncomeItem
import com.centsnippers.models.CycleType
import com.centsnippers.utils.SessionManager
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*

class IncomeFragment : Fragment() {

    private var _binding: FragmentIncomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var incomeAdapter: IncomeAdapter

    private var editedIncome: IncomeItem? = null // Used to track editing state
    private var userId: Int = -1


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIncomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("IncomeFragment", "onViewCreated() called")

        dbHelper = DatabaseHelper(requireContext())
        sessionManager = SessionManager(requireContext())
        userId = sessionManager.getUserId()

        if (userId == -1) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Initialize adapter with delete and edit handlers
        incomeAdapter = IncomeAdapter(mutableListOf(),
            onDeleteClick = { position -> deleteIncomeItem(position) },
            onEditClick = { position -> editIncomeItem(position) }
        )

        // Setup RecyclerView
        binding.incomeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.incomeRecyclerView.adapter = incomeAdapter

        // Load initial incomes
        loadIncomes()

        // Floating action button to add new income
        binding.addIncomeFab.setOnClickListener {
            showAddIncomeDialog()
        }
    }

    // Loads all active incomes for the user and displays total income
    private fun loadIncomes() {
        Log.d("IncomeFragment", "loadIncomes() called")

        val incomes = dbHelper.getIncomesForUser(userId).filter { it.isActive }
        incomeAdapter.updateList(incomes)
        binding.incomeRecyclerView.adapter = incomeAdapter

        

        // Get current date and time info
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH) + 1
        val currentYear = now.get(Calendar.YEAR)

        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

// Calculates total income using same logic as dashboard
        val total = incomes.sumOf { incomeItem ->
            try {
                val endDate = incomeItem.endDate

                // Optional: debug raw dates
                Log.d("IncomeFragment", "Evaluating income: '${incomeItem.description}' | start=${incomeItem.startDate} | end=$endDate")

                // Convert startDate to Calendar for extracting month/year
                val startCal = Calendar.getInstance().apply {
                    time = java.sql.Date.valueOf(incomeItem.startDate.toString())
                }
                val startMonth = startCal.get(Calendar.MONTH) + 1
                val startYear = startCal.get(Calendar.YEAR)

                // Convert endDate to sql.Date for safe comparison
                val endDateSql: java.sql.Date? = try {
                    endDate?.let { java.sql.Date.valueOf(it.toString()) }
                } catch (e: Exception) {
                    Log.e("IncomeFragment", "Invalid endDate for '${incomeItem.description}': $endDate", e)
                    null
                }

                // Check if this income is active in the current month
                val isActiveInMonth = endDateSql == null || endDateSql >= startOfMonth.time
                if (!isActiveInMonth) {
                    Log.d("IncomeFragment", "Income '${incomeItem.description}' is NOT active in current month")
                    return@sumOf 0.0
                }

                // Apply proper logic depending on cycle
                when (incomeItem.cycleType) {
                    CycleType.MONTHLY -> {
                        val isOnceOff = incomeItem.endDate != null && incomeItem.startDate == incomeItem.endDate
                        val isThisMonth = startMonth == currentMonth && startYear == currentYear

                        val result = when {
                            isOnceOff && isThisMonth -> incomeItem.amount
                            startYear < currentYear || (startYear == currentYear && startMonth <= currentMonth) -> incomeItem.amount
                            else -> 0.0
                        }

                        Log.d("IncomeFragment", "Applied monthly income: R$result for '${incomeItem.description}'")
                        result
                    }

                    CycleType.YEARLY -> {
                        val eligible = startYear < currentYear || (startYear == currentYear && startMonth <= currentMonth)
                        val result = if (eligible) incomeItem.amount / 12 else 0.0
                        Log.d("IncomeFragment", "Applied yearly income: R$result for '${incomeItem.description}'")
                        result
                    }
                }

            } catch (e: Exception) {
                Log.e("IncomeFragment", "Error calculating income '${incomeItem.description}'", e)
                0.0
            }
        }



        binding.totalIncomeThisMonth.text = "Total Income: R%.2f".format(total)
        Log.i("IncomeFragment", "User $userId | Reloaded incomes | Active count: ${incomes.size} | Total this month: R$total")
    }

    // Shows the Add/Edit Income dialog
    fun showAddIncomeDialog(isEdit: Boolean = false) {
        Log.d("IncomeFragment", "showAddIncomeDialog() called | isEdit=$isEdit")

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_income, null)

        val descInput = dialogView.findViewById<EditText>(R.id.inputIncomeDescription)
        val amountInput = dialogView.findViewById<EditText>(R.id.inputIncomeAmount)
        val cycleTypeSpinner = dialogView.findViewById<Spinner>(R.id.spinnerCycleType)
        val startDateInput = dialogView.findViewById<EditText>(R.id.inputStartDate)
        val endDateInput = dialogView.findViewById<EditText>(R.id.inputEndDate)
        val onceOffCheckbox = dialogView.findViewById<CheckBox>(R.id.checkboxOnceOff)
        val layoutRecurringFields = dialogView.findViewById<LinearLayout>(R.id.layoutRecurringFields)
        val clearEndDateButton = dialogView.findViewById<ImageButton>(R.id.btnClearEndDate)


        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val now = Calendar.getInstance()

        val cycleOptions = CycleType.values().map { it.name }
        cycleTypeSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cycleOptions)

        // Setup date pickers
        val openDatePicker = { target: EditText ->
            DatePickerDialog(requireContext(), { _, y, m, d ->
                target.setText(String.format("%02d/%02d/%04d", d, m + 1, y))
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
        }
        startDateInput.setOnClickListener { openDatePicker(startDateInput) }
        // Launch date picker on click
        endDateInput.setOnClickListener {
            val now = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                endDateInput.setText("%02d/%02d/%04d".format(d, m + 1, y))
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
        }

// Clear the end date on button click
        clearEndDateButton.setOnClickListener {
            endDateInput.setText("")
            Log.d("IncomeDialog", "User cleared optional end date field")
        }

        // Toggle recurring fields on checkbox
        onceOffCheckbox.setOnCheckedChangeListener { _, isChecked ->
            layoutRecurringFields.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        // Pre-fill fields if editing
        if (isEdit && editedIncome != null) {
            Log.d("IncomeFragment", "Prefilling fields for income ID=${editedIncome!!.id}")
            descInput.setText(editedIncome!!.description)
            amountInput.setText(editedIncome!!.amount.toString())
            cycleTypeSpinner.setSelection(CycleType.values().indexOf(editedIncome!!.cycleType))
            startDateInput.setText(sdf.format(Date.from(editedIncome!!.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())))
            editedIncome!!.endDate?.let {
                endDateInput.setText(sdf.format(Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant())))

            }
            onceOffCheckbox.isChecked = editedIncome!!.cycleType == CycleType.MONTHLY && editedIncome!!.endDate != null && editedIncome!!.startDate == editedIncome!!.endDate

        }

        // Save income
        AlertDialog.Builder(requireContext())
            .setTitle(if (isEdit) "Edit Income" else "Add Income")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val desc = descInput.text.toString().trim()
                val amount = amountInput.text.toString().toDoubleOrNull()
                val isOnceOff = onceOffCheckbox.isChecked
                val startDate = sdf.parse(startDateInput.text.toString())?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()

                val endDate = if (isOnceOff) startDate else endDateInput.text.toString().takeIf { it.isNotBlank() }?.let {
                    sdf.parse(it)?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
                }

                val cycle = if (isOnceOff) CycleType.MONTHLY else CycleType.valueOf(cycleTypeSpinner.selectedItem.toString())
                val cycleStart = startDate?.dayOfMonth

                if (desc.isNotBlank() && amount != null && cycleStart != null && startDate != null) {
                    Log.d("IncomeFragment", "DEBUG: isEdit=$isEdit | editedIncome?.id=${editedIncome?.id}")

                    val income = IncomeItem(
                        id = editedIncome?.id ?: 0,
                        userId = userId,
                        description = desc,
                        amount = amount,
                        cycleType = cycle,
                        cycleStartDay = cycleStart,
                        startDate = startDate,
                        endDate = endDate,
                        isActive = true
                    )
                    Log.d("IncomeFragment", "Saving income with ID=${income.id}")


                    if (isEdit && editedIncome != null) {
                        dbHelper.updateIncome(income)
                        editedIncome = null
                        Log.i("IncomeFragment", "Updated income ID=${income.id}")
                    } else {
                        dbHelper.insertIncome(income)
                        Log.i("IncomeFragment", "Inserted new income")
                    }

                    loadIncomes()
                } else {
                    Toast.makeText(requireContext(), "Please complete all required fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Confirms and deletes an income item
    private fun deleteIncomeItem(item: IncomeItem) {
        editedIncome = null // Clear any previous edit state

        AlertDialog.Builder(requireContext())
            .setTitle("Delete Income")
            .setMessage("Are you sure you want to delete '${item.description}'?")
            .setPositiveButton("Yes") { _, _ ->
                val success = dbHelper.deleteIncome(item, endNow = true)
                if (success) {
                    Toast.makeText(requireContext(), "Income deleted", Toast.LENGTH_SHORT).show()
                    Log.i("IncomeFragment", "Soft-deleted income ID=${item.id}")
                    loadIncomes()
                } else {
                    Toast.makeText(requireContext(), "Failed to delete income", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Loads the selected income for editing
    private fun editIncomeItem(item: IncomeItem) {
        editedIncome = item
        Log.d("IncomeFragment", "Preparing edit for income ID=${item.id}")
        showAddIncomeDialog(isEdit = true)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("IncomeFragment", "onDestroyView() called")
    }
}