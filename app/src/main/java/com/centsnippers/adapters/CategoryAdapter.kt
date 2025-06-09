package com.centsnippers.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.centsnippers.R
import com.centsnippers.data.DatabaseHelper
import com.centsnippers.models.CategoryItem

class CategoryAdapter(
    private var categoryList: MutableList<CategoryItem>,
    private val onDeleteClick: (CategoryItem) -> Unit,
    private val onEditClick: (CategoryItem) -> Unit

) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private lateinit var dbHelper: DatabaseHelper
    private var userId: Int = -1

    fun setDependencies(helper: DatabaseHelper, uid: Int) {
        dbHelper = helper
        userId = uid
    }

    inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.txtCategoryTitle)
        val description: TextView = view.findViewById(R.id.txtCategoryDescription)
        val amounts: TextView = view.findViewById(R.id.txtCategoryAmounts)
        val remaining: TextView = view.findViewById(R.id.txtRemainingAmount)
        val transactionCount: TextView = view.findViewById(R.id.txtCategoryTransactionCount)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEditCategory)
        val minMax: TextView = view.findViewById(R.id.txtMinMax)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val item = categoryList[position]

        holder.title.text = item.title
        holder.description.text = item.description

        val totalSpent = item.totalSpent
        val remainingAmount = item.goalAmount - totalSpent
        val transactionCount = item.transactionCount

        // Display budget vs spent
        holder.amounts.text = "Goal: R%.2f | Spent: R%.2f".format(item.goalAmount, totalSpent)

        // Status logic
        val overMax = item.maxSpend != null && totalSpent > item.maxSpend!!
        val belowMin = item.minSpend != null && totalSpent < item.minSpend!!

        holder.remaining.text = when {
            overMax -> "Over max limit (R%.2f)".format(item.maxSpend)
            belowMin -> "Below minimum target (R%.2f)".format(item.minSpend)
            remainingAmount < 0 -> "Over goal by R%.2f".format(-remainingAmount)
            remainingAmount > 0 -> "Remaining to goal: R%.2f".format(remainingAmount)
            else -> "Exactly on goal"
        }
        holder.minMax.text = when {
            item.minSpend != null && item.maxSpend != null -> "Min: R%.2f | Max: R%.2f".format(item.minSpend, item.maxSpend)
            item.minSpend != null -> "Min: R%.2f".format(item.minSpend)
            item.maxSpend != null -> "Max: R%.2f".format(item.maxSpend)
            else -> "No min/max set"
        }


        holder.transactionCount.text = "$transactionCount transactions in this category"

        holder.btnDelete.setOnClickListener {
            onDeleteClick(item)
        }

        holder.btnEdit.setOnClickListener {
            onEditClick(item)
        }
    }

    override fun getItemCount(): Int = categoryList.size

    fun updateList(newList: List<CategoryItem>) {
        categoryList.clear()
        categoryList.addAll(newList)
        notifyDataSetChanged()
    }
}
