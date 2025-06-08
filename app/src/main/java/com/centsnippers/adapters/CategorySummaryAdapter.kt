package com.centsnippers.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.centsnippers.R
import com.centsnippers.models.CategoryItem

class CategorySummaryAdapter(
    private val categoryList: List<CategoryItem>
) : RecyclerView.Adapter<CategorySummaryAdapter.SummaryViewHolder>() {

    /// ViewHolder class holds the layout views for each summary card
    inner class SummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.txtCategorySummaryTitle)
        val value: TextView = itemView.findViewById(R.id.txtCategorySummaryValue)
    }

    /// Inflates the layout for each summary item (item_category_summary.xml)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_summary, parent, false)
        return SummaryViewHolder(view)
    }

    /// Binds each category's name and total spent amount to the views
    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        val item = categoryList[position]
        holder.title.text = item.title
        holder.value.text = "Spent: R%.2f".format(item.totalSpent)
    }

    /// Returns the total number of items to display
    override fun getItemCount(): Int = categoryList.size
}
