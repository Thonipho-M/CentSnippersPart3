package com.centsnippers.adapters



import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.centsnippers.databinding.ItemBudgetBinding
import com.centsnippers.models.BudgetItem

class BudgetAdapter(
    private val items: MutableList<BudgetItem>,
    private val onEdit: (Int) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder>() {

    inner class BudgetViewHolder(val binding: ItemBudgetBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val binding = ItemBudgetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BudgetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            txtBudgetInfo.text = "${item.category} - R${item.amount} [${item.cycle}]"

            // Call the edit function passed from the fragment
            btnEdit.setOnClickListener {
                onEdit(position)
            }

            // Call the delete function passed from the fragment
            btnDelete.setOnClickListener {
                onDelete(position)
            }
        }
    }

    override fun getItemCount() = items.size
}
