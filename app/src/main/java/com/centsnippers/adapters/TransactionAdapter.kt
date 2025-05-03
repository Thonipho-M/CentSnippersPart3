package com.centsnippers.adapters

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.centsnippers.R
import com.centsnippers.data.DatabaseHelper
import com.centsnippers.models.TransactionItem
import android.net.Uri
import android.util.Log


class TransactionAdapter(
    private var transactionList: MutableList<TransactionItem>,
    private val dbHelper: DatabaseHelper
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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

        // Basic transaction info
        holder.transactionTitle.text = item.title
        holder.transactionAmount.text = "R%.2f".format(item.amount)
        holder.transactionDescription.text = item.description

        val categoryTitle = dbHelper.getCategoriesForUser(item.userId)
            .find { it.id == item.categoryId }?.title ?: "Unknown Category"
        holder.transactionCategory.text = "Category: $categoryTitle"
        holder.transactionDates.text = "${item.startDate} - ${item.endDate}"

        // Image Handling
        if (!item.imageUrl.isNullOrEmpty()) {
            holder.imagePreview.visibility = View.VISIBLE

            val imageUri = item.imageUrl



            Glide.with(holder.imagePreview.context)
                .load(imageUri)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_delete)
                .into(holder.imagePreview)

            holder.imagePreview.setOnClickListener {
                val context = holder.imagePreview.context
                val dialogView =
                    LayoutInflater.from(context).inflate(R.layout.dialog_image_preview, null)
                val fullImageView = dialogView.findViewById<ImageView>(R.id.fullImageView)

                try {
                    Glide.with(context)
                        .load(Uri.parse(imageUri))  //
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_delete)
                        .into(fullImageView)

                    AlertDialog.Builder(context)
                        .setView(dialogView)
                        .setPositiveButton("Close", null)
                        .setNeutralButton("Download") { _, _ ->
                            Toast.makeText(
                                context,
                                "Download not implemented yet",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .show()

                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                    e.printStackTrace()
                }
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
