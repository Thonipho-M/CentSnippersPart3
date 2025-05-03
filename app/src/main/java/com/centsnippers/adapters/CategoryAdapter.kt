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
    private val onDeleteClick: (Int) -> Unit
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
        val remainingAmount = item.amount - totalSpent
        val transactionCount = item.transactionCount

        holder.amounts.text = "Budget: R%.2f | Spent: R%.2f".format(item.amount, totalSpent)

        holder.remaining.text = when {
            remainingAmount < 0 -> "Over budget by R%.2f".format(-remainingAmount)
            remainingAmount > 0 -> "Remaining: R%.2f".format(remainingAmount)
            else -> "Exactly on budget"
        }

        holder.transactionCount.text = "$transactionCount transactions in this category"

        holder.btnDelete.setOnClickListener {
            onDeleteClick(position)
        }
    }


    override fun getItemCount(): Int = categoryList.size

    fun updateList(newList: List<CategoryItem>) {
        categoryList.clear()
        categoryList.addAll(newList)
        notifyDataSetChanged()
    }
}
