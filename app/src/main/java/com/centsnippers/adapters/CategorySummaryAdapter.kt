package com.centsnippers.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.centsnippers.R
import com.centsnippers.models.CategoryItem

class CategorySummaryAdapter(
    private var categoryList: List<CategoryItem>
) : RecyclerView.Adapter<CategorySummaryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.categoryTitleSummary)
        val amount: TextView = view.findViewById(R.id.categorySpentSummary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_summary, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categoryList[position]
        holder.title.text = category.title
        holder.amount.text = "Spent: R%.2f".format(category.totalSpent)
    }

    override fun getItemCount(): Int = categoryList.size

    fun updateList(newList: List<CategoryItem>) {
        categoryList = newList
        notifyDataSetChanged()
    }
}
