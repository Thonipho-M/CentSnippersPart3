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

class TransactionAdapter(
    private val transactions: MutableList<TransactionItem>
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.txtTransactionTitle)
        val description: TextView = view.findViewById(R.id.txtTransactionDescription)
        val amount: TextView = view.findViewById(R.id.txtTransactionAmount)
        val dateRange: TextView = view.findViewById(R.id.txtTransactionDateRange)
        val image: ImageView = view.findViewById(R.id.transactionImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val item = transactions[position]

        holder.title.text = item.title
        holder.description.text = item.description
        holder.amount.text = "R%.2f".format(item.amount)
        holder.dateRange.text = "From ${item.startDate} to ${item.endDate}"

        if (!item.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView)
                .load(item.imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.placeholder_image)
        }
    }

    override fun getItemCount(): Int = transactions.size

    fun updateList(newList: List<TransactionItem>) {
        transactions.clear()
        transactions.addAll(newList)
        notifyDataSetChanged()
    }
}
