// ===================================================
// IncomeFragment.kt — Income View
// Purpose: Displays all income sources for a user.
// Shows current total income, allows add/edit/remove
// ===================================================

package com.centsnippers.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.centsnippers.data.DatabaseHelper
import com.centsnippers.databinding.FragmentIncomeBinding
import com.centsnippers.utils.SessionManager
import com.centsnippers.adapters.IncomeAdapter
import java.time.LocalDate

class IncomeFragment : Fragment() {

    private var _binding: FragmentIncomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private var userId: Int = -1

    // ===============================================
    // onCreateView()
    // Sets up binding and layout inflating
    // ===============================================
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    // ===============================================
    // onViewCreated()
    // Initializes user data, loads income records,
    // calculates current month's total income
    // ===============================================
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // === Init Session + DB ===
        dbHelper = DatabaseHelper(requireContext())
        sessionManager = SessionManager(requireContext())
        userId = sessionManager.getUserId()

        if (userId == -1) {
            Log.e("IncomeFragment", "User not logged in — aborting income screen")
            return
        }

        Log.d("IncomeFragment", "User $userId | Loaded IncomeFragment")

        // === Setup RecyclerView ===
        binding.incomeRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // === Load income list from DB ===
        val incomeList = dbHelper.getIncomesForUser(userId).filter { it.isActive }
        // === Handle Add Income FAB Click ===
        binding.addIncomeFab.setOnClickListener {
            Log.d("IncomeFragment", "User $userId | Clicked Add Income FAB")
            // We'll trigger dialog creation here in the next step
            showAddIncomeDialog()
        }

        // === Display income list in RecyclerView ===
        binding.incomeRecyclerView.adapter = IncomeAdapter(incomeList)

        // === Calculate income total for this month ===
        val now = LocalDate.now()
        val totalIncome = incomeList
            .filter { it.startDate <= now && (it.endDate == null || it.endDate >= now) }
            .sumOf { it.amount }

        // === Show total + log ===
        binding.totalIncomeThisMonth.text = "Total Income: R%.2f".format(totalIncome)
        Log.i("IncomeFragment", "User $userId | Loaded ${incomeList.size} active incomes | Total: R$totalIncome")
    }

    // ===============================================
    // onDestroyView()
    // Clears binding reference to avoid memory leaks
    // ===============================================
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun showAddIncomeDialog() {
        // TODO: Build income entry dialog
        Log.d("IncomeFragment", "User $userId | Triggered add income dialog (UI not yet implemented)")
    }

}
