package com.centsnippers.adapters

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.RecyclerView
import com.centsnippers.R
import com.centsnippers.models.BudgetItem
import com.centsnippers.fragments.BudgetFragment
import androidx.core.net.toUri


class BudgetAdapter(


    private var budgetList: MutableList<BudgetItem>,
    private val onDelete: (Int) -> Unit,


) : RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder>() {

    inner class BudgetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtBudgetInfo: TextView = view.findViewById(R.id.txtBudgetInfo)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val imagePreview: ImageView = view.findViewById(R.id.imagePreview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_budget, parent, false)
        return BudgetViewHolder(view)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
    // this determines how the budget list will be presented
        val item = budgetList[position]
        holder.txtBudgetInfo.text = "${item.description}: R${item.amount}"

        // Show image if URI is available
        if (!item.imageUrl.isNullOrEmpty()) {
            holder.imagePreview.setImageURI(item.imageUrl.toUri())
            holder.imagePreview.visibility = View.VISIBLE
        } else {
            holder.imagePreview.visibility = View.GONE
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

