package com.centsnippers.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.centsnippers.R
import com.centsnippers.adapters.CategoryAdapter
import com.centsnippers.databinding.FragmentCategoryBinding
import com.centsnippers.utils.FirebaseCategoryService
import com.centsnippers.models.CategoryItem
import com.centsnippers.MainActivity
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class CategoryFragment : Fragment() {

    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var categoryAdapter: CategoryAdapter
    private val firebaseUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (firebaseUserId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        categoryAdapter = CategoryAdapter(mutableListOf()) { item ->
            deleteCategoryItem(item)
        }

        binding.categoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.categoryRecyclerView.adapter = categoryAdapter

        setupDatePickers()

        binding.addCategoryFab.setOnClickListener {
            showAddCategoryDialog()
        }

        binding.applyFilterButton.setOnClickListener {
            Toast.makeText(requireContext(), "Date filtering will be added later", Toast.LENGTH_SHORT).show()
        }

        loadCategories()
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

    private fun loadCategories() {
        FirebaseCategoryService.getCategoriesForUser { categoryList ->
            categoryAdapter.updateList(categoryList.toMutableList())
            val total = categoryList.sumOf { it.amount }
            binding.totalCategoryTextView.text = "Total Budgeted: R%.2f".format(total)
        }
    }

    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.inputCategoryTitle)
        val descInput = dialogView.findViewById<EditText>(R.id.inputCategoryDescription)
        val amountInput = dialogView.findViewById<EditText>(R.id.inputCategoryAmount)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Category")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = titleInput.text.toString().trim()
                val description = descInput.text.toString().trim()
                val amount = amountInput.text.toString().trim().toDoubleOrNull()

                if (title.isNotBlank() && amount != null && firebaseUserId != null) {
                    val categoryItem = CategoryItem(
                        id = 0,
                        userId = firebaseUserId!!,
                        title = title,
                        description = description,
                        amount = amount
                    )
                    FirebaseCategoryService.insertCategory(categoryItem) { success, error ->
                        if (success) {
                            Toast.makeText(requireContext(), "Category added", Toast.LENGTH_SHORT).show()
                            loadCategories()
                        } else {
                            Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCategoryItem(item: CategoryItem) {
        // Optional: implement delete in FirebaseCategoryService
        Toast.makeText(requireContext(), "Delete not implemented yet for ${item.title}", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.top_right_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logoutButton -> {
                FirebaseAuth.getInstance().signOut()
                Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireActivity(), MainActivity::class.java))
                requireActivity().finish()
                return true
            }
            R.id.goalsFragment -> {
                findNavController().navigate(R.id.goalsFragment)
                return true
            }
            R.id.rewardsFragment -> {
                findNavController().navigate(R.id.rewardsFragment)
                return true
            }
            R.id.helpPage -> {
                findNavController().navigate(R.id.helpPage)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
