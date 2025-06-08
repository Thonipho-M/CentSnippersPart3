package com.centsnippers.adapters

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.centsnippers.R
import com.centsnippers.models.CycleType
import com.centsnippers.models.IncomeItem
import java.time.format.DateTimeFormatter

class IncomeAdapter(
    private var incomeList: List<IncomeItem>
) : RecyclerView.Adapter<IncomeAdapter.IncomeViewHolder>() {

    class IncomeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val incomeDescription: TextView = itemView.findViewById(R.id.txtIncomeDescription)
        val incomeAmount: TextView = itemView.findViewById(R.id.txtIncomeAmount)
        val incomeCycle: TextView = itemView.findViewById(R.id.txtIncomeCycle)
        val incomeDateRange: TextView = itemView.findViewById(R.id.txtIncomeDateRange)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_income, parent, false)
        return IncomeViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: IncomeViewHolder, position: Int) {
        val item = incomeList[position]

        holder.incomeDescription.text = item.description
        holder.incomeAmount.text = "R%.2f".format(item.amount)

        holder.incomeCycle.text = when (item.cycleType) {
            CycleType.MONTHLY -> "Monthly"
            CycleType.YEARLY -> "Yearly"
            CycleType.ONCE -> "One-time"
        }

        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val start = item.startDate.format(formatter)
        val end = item.endDate?.format(formatter) ?: "Ongoing"

        holder.incomeDateRange.text = "From $start to $end"
    }

    override fun getItemCount(): Int = incomeList.size

    fun updateList(newList: List<IncomeItem>) {
        incomeList = newList
        notifyDataSetChanged()
    }
}
