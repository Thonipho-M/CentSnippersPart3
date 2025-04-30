package com.centsnippers.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.centsnippers.R
import com.centsnippers.databinding.FragmentDashboardBinding


class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // Example data - this will be fetched from the database later
    private val totalIncome = 8000
    private val totalExpenses = 5400

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show the values
        binding.totalIncomeText.text = "Total Income: R$totalIncome"
        binding.totalExpensesText.text = "Total Expenses: R$totalExpenses"

        val remaining = totalIncome - totalExpenses
        binding.remainingBudgetText.text = "Remaining Budget: R$remaining"

        // Basic overspending check
        if (remaining < 0) {
            binding.overspendingText.text = "Overspending: R${-remaining}"
        } else {
            binding.overspendingText.text = "Overspending: None"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}