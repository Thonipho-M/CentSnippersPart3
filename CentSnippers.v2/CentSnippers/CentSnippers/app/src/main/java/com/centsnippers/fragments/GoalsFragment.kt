package com.centsnippers.fragments

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.centsnippers.R
import com.centsnippers.databinding.FragmentGoalsBinding
import com.centsnippers.models.Goal
import com.centsnippers.adapters.GoalsAdapter

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    private val goalsList = mutableListOf<Goal>()
    private lateinit var adapter: GoalsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the RecyclerView and Adapter
        adapter = GoalsAdapter(goalsList)
        binding.goalsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.goalsRecyclerView.adapter = adapter

        // Set up the floating action button to add a new goal
        binding.addGoalFab.setOnClickListener {
            showAddGoalDialog()
        }
    }

    // Shows a dialog to add a new goal
    private fun showAddGoalDialog() {
        val inputTitle = EditText(requireContext()).apply { hint = "Goal Title" }
        val inputAmount = EditText(requireContext()).apply {
            hint = "Target Amount"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 0)
            addView(inputTitle)
            addView(inputAmount)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add New Goal")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val title = inputTitle.text.toString()
                val amount = inputAmount.text.toString().toDoubleOrNull()

                if (title.isNotBlank() && amount != null) {
                    goalsList.add(Goal(title, amount))
                    adapter.notifyItemInserted(goalsList.size - 1)
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