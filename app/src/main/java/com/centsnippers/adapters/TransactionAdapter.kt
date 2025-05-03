package com.centsnippers.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.centsnippers.R
import com.centsnippers.models.TransactionItem
import com.centsnippers.data.DatabaseHelper
//for image view alert
import android.app.AlertDialog
import android.widget.Toast


class TransactionAdapter(
    private var transactionList: MutableList<TransactionItem>,
    private val dbHelper: DatabaseHelper  // Needed to fetch category title
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        //intiliazing variables for the fragment
        val transactionTitle: TextView = view.findViewById(R.id.transactionTitle)
        val transactionAmount: TextView = view.findViewById(R.id.transactionAmount)
        val transactionDescription: TextView = view.findViewById(R.id.transactionDescription)
        val transactionCategory: TextView = view.findViewById(R.id.transactionCategory)
        val transactionDates: TextView = view.findViewById(R.id.transactionDates)
        val imagePreview: ImageView = view.findViewById(R.id.imagePreview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val item = transactionList[position]

        // Set title and description
        holder.transactionTitle.text = item.title
        holder.transactionAmount.text = "R%.2f".format(item.amount)
        holder.transactionDescription.text = item.description

        // Fetch category name using the ID
        val categoryTitle = dbHelper.getCategoriesForUser(item.userId)
            .find { it.id == item.categoryId }?.title ?: "Unknown Category"
        holder.transactionCategory.text = "Category: $categoryTitle"

        // Set dates
        holder.transactionDates.text = "${item.startDate} - ${item.endDate}"

        // Handle image
        if (!item.imageUrl.isNullOrEmpty()) {
            holder.imagePreview.visibility = View.VISIBLE
            Glide.with(holder.imagePreview.context)
                .load(item.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_delete)
                .into(holder.imagePreview)
            holder.imagePreview.setOnClickListener {
                val builder = AlertDialog.Builder(holder.imagePreview.context)
                val dialogView = LayoutInflater.from(holder.imagePreview.context).inflate(R.layout.dialog_image_preview, null)
                val fullImage = dialogView.findViewById<ImageView>(R.id.fullImageView)

                dialogView.findViewById<ImageView>(R.id.fullImageView).let { preview ->
                    preview.visibility = View.VISIBLE
                    Glide.with(holder.imagePreview.context)
                        .load(item.imageUrl)
                        .into(preview)
                }
                Glide.with(dialogView)
                    .load(item.imageUrl)
                    .into(fullImage)

                builder.setView(dialogView)
                builder.setPositiveButton("Close", null)
                builder.setNeutralButton("Download") { _, _ ->
                    // Implement download logic here
                    Toast.makeText(holder.imagePreview.context, "Downloading not implemented yet", Toast.LENGTH_SHORT).show()
                }

                builder.show()
            }

        } else {
            holder.imagePreview.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = transactionList.size

    fun updateList(newList: List<TransactionItem>) {
        transactionList.clear()
        transactionList.addAll(newList)
        notifyDataSetChanged()
    }
}
