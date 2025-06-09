package com.centsnippers.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.centsnippers.R
import com.centsnippers.data.DatabaseHelper
import com.centsnippers.models.CategoryItem
import com.github.mikephil.charting.components.*
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.*

class CategoryAdapter(
    private var categoryList: MutableList<CategoryItem>,
    private val onDeleteClick: (CategoryItem) -> Unit,
    private val onEditClick: (CategoryItem) -> Unit

) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private lateinit var dbHelper: DatabaseHelper
    private var userId: Int = -1

    fun setDependencies(helper: DatabaseHelper, uid: Int) {
        dbHelper = helper
        userId = uid
    }

    inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.txtCategoryTitle)
        val description: TextView = view.findViewById(R.id.txtCategoryDescription)
        val amounts: TextView = view.findViewById(R.id.txtCategoryAmounts)
        val remaining: TextView = view.findViewById(R.id.txtRemainingAmount)
        val transactionCount: TextView = view.findViewById(R.id.txtCategoryTransactionCount)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEditCategory)
        //val minMax: TextView = view.findViewById(R.id.txtMinMax)
        val barChart: com.github.mikephil.charting.charts.HorizontalBarChart = view.findViewById(R.id.barChartCategory)


    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val item = categoryList[position]

        holder.title.text = item.title
        holder.description.text = item.description

        val totalSpent = item.totalSpent
        val remainingAmount = item.goalAmount - totalSpent
        val transactionCount = item.transactionCount

        // Display budget vs spent
        holder.amounts.text = "Goal: R%.2f | Spent: R%.2f".format(item.goalAmount, totalSpent)

        // Status logic
        val overMax = item.maxSpend != null && totalSpent > item.maxSpend!!
        val belowMin = item.minSpend != null && totalSpent < item.minSpend!!
        val remainingText: String
        val remainingColor: Int

        when {
            overMax -> {
                remainingText = "Over max limit (R%.2f)".format(item.maxSpend)
                remainingColor = android.R.color.holo_red_dark
            }
            belowMin -> {
                remainingText = "Below min target (R%.2f)".format(item.minSpend)
                remainingColor = android.R.color.holo_green_light
            }
            remainingAmount < 0 -> {
                remainingText = "Over goal by R%.2f".format(-remainingAmount)
                remainingColor = android.R.color.holo_red_light
            }
            remainingAmount > 0 -> {
                remainingText = "Remaining to goal: R%.2f".format(remainingAmount)
                remainingColor = android.R.color.holo_green_dark
            }
            else -> {
                remainingText = "Exactly on goal"
                remainingColor = android.R.color.black
            }
        }

        holder.remaining.text = remainingText
        holder.remaining.setTextColor(holder.itemView.context.getColor(remainingColor))



        holder.transactionCount.text = "$transactionCount transactions"
        setupCategoryChart(holder.barChart, item)

        holder.btnDelete.setOnClickListener {
            onDeleteClick(item)
        }

        holder.btnEdit.setOnClickListener {
            onEditClick(item)
        }
    }

    override fun getItemCount(): Int = categoryList.size

    fun updateList(newList: List<CategoryItem>) {
        categoryList.clear()
        categoryList.addAll(newList)
        notifyDataSetChanged()
    }


    // ========================
// setupCategoryChart()
// Purpose: Configure the compact bar graph in item_category
// Visual: Shows total spent as a bar, with goal/min/max as lines
// ========================
    private fun setupCategoryChart(chart: com.github.mikephil.charting.charts.HorizontalBarChart, item: CategoryItem) {
        // Extract core values as floats for chart input
        val spent = item.totalSpent.toFloat()
        val goal = item.goalAmount.toFloat()
        val min = item.minSpend?.toFloat()
        val max = item.maxSpend?.toFloat()

        // Create a single bar representing the total amount spent
        val spentEntries = listOf(BarEntry(0f, spent))

        // Define how that bar should look
        val spentDataSet = BarDataSet(spentEntries, "Spent").apply {
            color = Color.parseColor(item.colorHex ?: "#4CAF50") // fallback to green // Bar color
            valueTextSize = 10f // Unused (we'll hide values anyway)
        }

        // Bundle the dataset into a BarData object
        val data = BarData(spentDataSet)

        // Attach the data to the chart
        chart.data = data

        // === Configure limit lines (goal, min, max) ===

        // Define a blue line representing the spending goal
        val goalLine = LimitLine(goal, "").apply {
            lineColor = chart.context.getColor(android.R.color.holo_blue_dark)
            lineWidth = 2f
        }

        // Access the left Y axis (horizontal bar charts use left as X scale)
        val leftAxis = chart.axisLeft

        // Remove any old lines from the chart to avoid stacking
        leftAxis.removeAllLimitLines()

        // Add the goal limit line
        leftAxis.addLimitLine(goalLine)

        // If minSpend is set, draw a green line
        min?.let {
            leftAxis.addLimitLine(LimitLine(it, "").apply {
                lineColor = chart.context.getColor(android.R.color.holo_green_dark)
                lineWidth = 1.5f
            })
        }

        // If maxSpend is set, draw a red line
        max?.let {
            leftAxis.addLimitLine(LimitLine(it, "").apply {
                lineColor = chart.context.getColor(android.R.color.holo_red_dark)
                lineWidth = 1.5f
            })

        }


        // === Configure axis scaling ===

        // Determine the upper bound of the axis:
        // Use maxSpend if available, otherwise 130% of goal
        val upperLimit = max ?: (goal * 1.3f)

        // Apply axis scaling rules
        leftAxis.axisMinimum = 0f                                  // Always start from zero
        leftAxis.axisMaximum = maxOf(upperLimit + 10f, 50f)        // Add padding for headroom
        leftAxis.setDrawLabels(false)                              // Hide axis labels
        leftAxis.setDrawGridLines(false)                           // Remove grid
        leftAxis.setDrawAxisLine(false)                            // Remove axis line

        // === Disable all unnecessary features ===

        chart.axisRight.isEnabled = false       // No secondary axis
        chart.xAxis.isEnabled = false           // Disable X axis (categories)
        chart.legend.isEnabled = false          // Hide legend
        chart.description.isEnabled = false     // Hide chart description
        chart.setTouchEnabled(false)            // No interaction
        chart.setScaleEnabled(false)            // Disable zoom

        // Hide the value label on top of the bar
        spentDataSet.setDrawValues(false)

        // Add a smooth vertical animation when rendering
        chart.animateY(600)

        // Final draw pass
        chart.invalidate()
    }


}
