// Updated GoalsFragment.kt
package com.centsnippers.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.centsnippers.R
import com.centsnippers.data.DatabaseHelper
import com.centsnippers.databinding.FragmentGoalsBinding
import com.centsnippers.models.Goal
import com.centsnippers.utils.SessionManager
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private var userId: Int = -1

    private var minGoal: Double = 0.0
    private var maxGoal: Double = 0.0
    private var actualSpent: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)
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

        binding.addGoalFab.setOnClickListener {
            showGoalDialog()
        }

        calculateMonthlySpending()
    }

    private fun calculateMonthlySpending() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val allTransactions = dbHelper.getTransactionsForUser(userId)
        val filteredTransactions = allTransactions.filter { txn ->
            try {
                val txnDate = sdf.parse(txn.startDate)
                val txnCal = Calendar.getInstance().apply { time = txnDate!! }
                txnCal.get(Calendar.MONTH) == currentMonth &&
                        txnCal.get(Calendar.YEAR) == currentYear
            } catch (e: Exception) {
                false
            }
        }

        actualSpent = filteredTransactions.sumOf { it.amount }
        updateGoalsDisplay()
    }

    private fun updateGoalsDisplay() {
        val df = DecimalFormat("#.00")
        binding.txtMinGoal.text = "Min Goal: R${df.format(minGoal)}"
        binding.txtMaxGoal.text = "Max Goal: R${df.format(maxGoal)}"
        binding.txtActualSpent.text = "Spent This Month: R${df.format(actualSpent)}"

        when {
            actualSpent < minGoal -> {
                binding.txtStatus.apply {
                    text = "You're spending wisely! 🟢"
                    setTextColor(resources.getColor(android.R.color.holo_green_dark))
                }
            }
            actualSpent > maxGoal -> {
                binding.txtStatus.apply {
                    text = "You've exceeded your max budget! 🔴"
                    setTextColor(resources.getColor(android.R.color.holo_red_dark))
                }
            }
            else -> {
                binding.txtStatus.apply {
                    text = "You're within your budget range. ✅"
                    setTextColor(resources.getColor(android.R.color.holo_green_light))
                }
            }
        }
    }

    private fun showGoalDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_set_goals, null)
        val minGoalInput = dialogView.findViewById<EditText>(R.id.inputMinGoal)
        val maxGoalInput = dialogView.findViewById<EditText>(R.id.inputMaxGoal)

        AlertDialog.Builder(requireContext())
            .setTitle("Set Monthly Goals")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val min = minGoalInput.text.toString().toDoubleOrNull()
                val max = maxGoalInput.text.toString().toDoubleOrNull()
                if (min != null && max != null) {
                    minGoal = min
                    maxGoal = max
                    updateGoalsDisplay()
                    Toast.makeText(requireContext(), "Goals Updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Enter valid values", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
