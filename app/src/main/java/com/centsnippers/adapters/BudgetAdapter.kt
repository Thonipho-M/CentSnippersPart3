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
import com.bumptech.glide.Glide



class BudgetAdapter(


    private var budgetList: MutableList<BudgetItem>,
    private val onDelete: (Int) -> Unit


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
        val item = budgetList[position]
        holder.txtBudgetInfo.text = "${item.description}: R${item.amount}"

        // Load image using Glide
        if (!item.imageUrl.isNullOrEmpty()) {
            holder.imagePreview.visibility = View.VISIBLE
            Glide.with(holder.imagePreview.context)
                .load(item.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery) // optional: show while loading
                .error(android.R.drawable.ic_delete) // optional: show if loading fails
                .into(holder.imagePreview)
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

