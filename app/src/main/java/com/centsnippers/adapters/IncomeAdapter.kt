// ================================================================
// IncomeAdapter.kt — Adapter for RecyclerView
// Purpose: Binds a list of income items to RecyclerView inside IncomeFragment
// Enhanced: Includes delete/edit handling, updateList, and dynamic badge formatting
// ================================================================

package com.centsnippers.adapters

import android.view.LayoutInflater
import android.view.View
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.centsnippers.R
import com.centsnippers.data.DatabaseHelper
import com.centsnippers.models.CycleType
import com.centsnippers.models.IncomeItem
import java.time.format.DateTimeFormatter

class IncomeAdapter(
    private var incomeList: MutableList<IncomeItem>, // Dynamic list of income entries
    private val onDeleteClick: (IncomeItem) -> Unit, // Delete handler callback
    private val onEditClick: (IncomeItem) -> Unit    // Edit handler callback
) : RecyclerView.Adapter<IncomeAdapter.IncomeViewHolder>() {

    private lateinit var dbHelper: DatabaseHelper
    private var userId: Int = -1

    // Sets database helper and user ID
    fun setDependencies(helper: DatabaseHelper, uid: Int) {
        dbHelper = helper
        userId = uid
    }

    // ViewHolder holds views for a single income card
    inner class IncomeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val desc: TextView = view.findViewById(R.id.incomeDescription)
        val amount: TextView = view.findViewById(R.id.incomeAmount)
        val cycle: TextView = view.findViewById(R.id.incomeCycle)
        val dates: TextView = view.findViewById(R.id.incomeDates)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEditIncome)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteIncome)
    }

    // Inflates item_income layout and returns a view holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_income, parent, false)
        Log.d("IncomeFragment", "onViewCreated() called")
        return IncomeViewHolder(view)
    }

    // Binds income data to each card item view
    override fun onBindViewHolder(holder: IncomeViewHolder, position: Int) {
        val item = incomeList[position] // Get income item at current position

        holder.desc.text = item.description // Set description text
        holder.amount.text = "R%.2f".format(item.amount) // Set formatted amount

        val isOnceOff = item.endDate != null && item.startDate == item.endDate // Identify once-off items

        // Determine and display cycle type string
        holder.cycle.text = when {
            isOnceOff -> "Once-off payment"
            item.cycleType == CycleType.MONTHLY -> "Cycle: Monthly (Day ${item.cycleStartDay})"
            item.cycleType == CycleType.YEARLY -> {
                val monthDay = item.startDate.format(DateTimeFormatter.ofPattern("dd MMMM"))
                "Cycle: Yearly ($monthDay)"
            }
            else -> "Cycle: ${item.cycleType.name} (Day ${item.cycleStartDay})"
        }

        // Format and show start/end dates
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val start = item.startDate.format(formatter)
        val end = item.endDate?.format(formatter) ?: "Ongoing"
        holder.dates.text = "From $start to $end"

        // ===============================
        // Badge Style Logic
        // ===============================
        val badge = holder.itemView.findViewById<TextView>(R.id.incomeTag) // Grab tag badge view

        val badgeLabel: String
        val badgeColor: Int

        val context = holder.itemView.context

        val isOnceOffv = item.endDate != null && item.startDate == item.endDate

        if (isOnceOffv) {
            badgeLabel = "ONCE-OFF"
            badgeColor = R.color.orange_800
        } else if (item.cycleType == CycleType.MONTHLY) {
            badgeLabel = "MONTHLY"
            badgeColor = R.color.teal_700
        } else if (item.cycleType == CycleType.YEARLY) {
            badgeLabel = "YEARLY"
            badgeColor = R.color.purple_700
        } else {
            badgeLabel = item.cycleType.name
            badgeColor = R.color.black // Fallback for unknown types
        }


        // Apply visual updates to badge
        badge.text = badgeLabel
        badge.setBackgroundColor(ContextCompat.getColor(context, badgeColor))

        // Assign onClick callbacks to buttons
        holder.btnDelete.setOnClickListener { onDeleteClick(item) }
        holder.btnEdit.setOnClickListener { onEditClick(item) }
    }

    // Returns total number of income entries
    override fun getItemCount(): Int = incomeList.size

    // Force a full refresh by creating new references
    fun updateList(newList: List<IncomeItem>) {
        incomeList = newList.toMutableList() // Replace the entire list object
        notifyDataSetChanged()               // Force redraw of all ViewHolders
    }

}
