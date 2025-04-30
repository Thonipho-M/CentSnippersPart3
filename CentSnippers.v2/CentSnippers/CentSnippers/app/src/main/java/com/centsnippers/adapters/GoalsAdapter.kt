package com.centsnippers.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.centsnippers.R
import com.centsnippers.models.Goal

class GoalsAdapter(private val goalsList: List<Goal>) : RecyclerView.Adapter<GoalsAdapter.GoalViewHolder>() {

    class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.goalTitleTextView)
        val amountTextView: TextView = itemView.findViewById(R.id.goalAmountTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_goal, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goalsList[position]
        holder.titleTextView.text = goal.title
        holder.amountTextView.text = "Target: R${goal.amount}"
    }

    override fun getItemCount(): Int = goalsList.size
}
