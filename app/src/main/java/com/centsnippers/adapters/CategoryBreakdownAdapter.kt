package com.centsnippers.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.centsnippers.R
import com.centsnippers.models.CategoryItem

class CategoryBreakdownAdapter(
    private val categories: List<CategoryItem>
) : RecyclerView.Adapter<CategoryBreakdownAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.txtCategoryBreakdownTitle)
        val spent: TextView = view.findViewById(R.id.txtCategoryBreakdownSpent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_breakdown, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val item = categories[position]
        holder.title.text = item.title
        holder.spent.text = "Spent: R%.2f".format(item.totalSpent)
    }

    override fun getItemCount(): Int = categories.size
}

