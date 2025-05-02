package com.centsnippers.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.RecyclerView
import com.centsnippers.R
import com.centsnippers.models.BudgetItem
import com.centsnippers.fragments.BudgetFragment


class BudgetAdapter(


    private var budgetList: MutableList<BudgetItem>,  // Now mutable
    private val onDelete: (Int) -> Unit,
    private val onPhoto: (Int) -> Unit

) : RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder>() {

    inner class BudgetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtBudgetInfo: TextView = view.findViewById(R.id.txtBudgetInfo)
        val btnPhoto: ImageButton = view.findViewById(R.id.btnPhoto)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_budget, parent, false)
        return BudgetViewHolder(view)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
    // this determines how the budget list will be presented
        val item = budgetList[position]
        holder.txtBudgetInfo.text = "${item.description}: R${item.amount}"


        holder.btnPhoto.setOnClickListener {
            onPhoto(position)
        }

        holder.btnDelete.setOnClickListener {
            onDelete(position)
        }
    }

    override fun getItemCount(): Int = budgetList.size

    // This function allows the list to be updated and the view refreshed
    fun updateList(newList: List<BudgetItem>) {
        budgetList.clear()
        budgetList.addAll(newList)
        notifyDataSetChanged()
    }
}

