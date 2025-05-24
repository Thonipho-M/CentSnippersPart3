// ================================================================
// IncomeAdapter.kt — Adapter for RecyclerView
// Purpose: Binds a list of income items to RecyclerView inside IncomeFragment
// Author's POV: Helps present the user with each income they’ve created
// ================================================================

package com.centsnippers.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.centsnippers.R
import com.centsnippers.models.*

class IncomeAdapter(private val incomeList: List<IncomeItem>) :
    RecyclerView.Adapter<IncomeAdapter.IncomeViewHolder>() {

    // ============================================================
    // ViewHolder class — binds layout to individual data items
    // POV: Holds views for each row to display income
    // ============================================================
    class IncomeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val desc: TextView = itemView.findViewById(R.id.incomeDescription)
        val amount: TextView = itemView.findViewById(R.id.incomeAmount)
    }

    // ============================================================
    // onCreateViewHolder()
    // POV: Inflates the layout file for one row item
    // ============================================================
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_income, parent, false)
        return IncomeViewHolder(view)
    }

    // ============================================================
    // onBindViewHolder()
    // POV: Populates the income row with the correct data
    // ============================================================
    override fun onBindViewHolder(holder: IncomeViewHolder, position: Int) {
        val income = incomeList[position]
        holder.desc.text = income.description
        holder.amount.text = "R%.2f".format(income.amount)
    }

    // ============================================================
    // getItemCount()
    // POV: Returns total number of rows (income items) to display
    // ============================================================
    override fun getItemCount(): Int = incomeList.size
}
