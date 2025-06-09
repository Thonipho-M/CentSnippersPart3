package com.centsnippers.adapters

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.centsnippers.R
import com.centsnippers.data.DatabaseHelper
import com.centsnippers.models.TransactionItem
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
/// Adapter for displaying a list of TransactionItems in a RecyclerView.
/// Binds each transaction to a custom layout, handles image preview logic, and supports image download.
class TransactionAdapter(
    private var transactionList: MutableList<TransactionItem>,
    private val dbHelper: DatabaseHelper,
    private val onEditClick: (TransactionItem) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    /// ViewHolder class that holds references to UI components for a single transaction item
    inner class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val transactionTitle: TextView = view.findViewById(R.id.transactionTitle)
        val transactionAmount: TextView = view.findViewById(R.id.transactionAmount)
        val transactionDescription: TextView = view.findViewById(R.id.transactionDescription)
        val transactionCategory: TextView = view.findViewById(R.id.transactionCategory)
        val transactionDates: TextView = view.findViewById(R.id.transactionDates)
        val imagePreview: ImageView = view.findViewById(R.id.imagePreview)
        val btnEditTransaction: ImageButton = view.findViewById(R.id.btnEditTransaction)

    }

    /// Inflates a transaction item layout and returns a ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        // Inflate the transaction item layout from XML
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)

        // Log creation of a new ViewHolder
        Log.d("TransactionAdapter.onCreateViewHolder", "Creating ViewHolder in TransactionAdapter using function onCreateViewHolder")

        // Return the ViewHolder instance
        return TransactionViewHolder(view)
    }

    /// Binds a transaction item to the UI, including data population and image handling
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        // Get the current transaction item
        val item = transactionList[position]

        // Bind transaction data to UI fields
        holder.transactionTitle.text = item.title
        holder.transactionAmount.text = "R%.2f".format(item.amount)
        holder.transactionDescription.text = item.description

        // Fetch and bind the category title
        val categoryTitle = dbHelper.getCategoriesForUser(item.userId)
            .find { it.id == item.categoryId }?.title ?: "Unknown Category"
        holder.transactionCategory.text = "Category: $categoryTitle"
        holder.transactionDates.text = "${item.Date}"

        // Handle image display if URL is available
        if (!item.imageUrl.isNullOrEmpty()) {
            holder.imagePreview.visibility = View.VISIBLE
            val imageUri = item.imageUrl

            // Load the image preview using Glide
            Glide.with(holder.imagePreview.context)
                .load(imageUri)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_delete)
                .into(holder.imagePreview)

            // On image click, open a dialog to show full-size image
            holder.imagePreview.setOnClickListener {
                val context = holder.imagePreview.context
                val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_image_preview, null)
                val fullImageView = dialogView.findViewById<ImageView>(R.id.fullImageView)

                try {
                    // Load the full-size image into dialog
                    Glide.with(context)
                        .load(Uri.parse(imageUri))
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_delete)
                        .into(fullImageView)

                    // Show the dialog with "Close" and "Download" options
                    AlertDialog.Builder(context)
                        .setView(dialogView)
                        .setPositiveButton("Close", null)
                        .setNeutralButton("Download") { _, _ ->
                            downloadImage(context, Uri.parse(imageUri), item.title)
                        }
                        .show()

                } catch (e: Exception) {
                    // Show error message and log exception
                    Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("TransactionAdapter.onBindViewHolder", "Image load failed in TransactionAdapter using function onBindViewHolder: ${e.message}")
                    e.printStackTrace()
                }
            }
        } else {
            // Hide image view if no image exists
            holder.imagePreview.visibility = View.GONE
        }
        holder.btnEditTransaction.setOnClickListener {
            onEditClick(item)
            Log.d("TransactionAdapter", "Edit button clicked for transaction ID=${item.id}")
        }

        // Set background color based on transaction type
        val typeIndicator = holder.itemView.findViewById<View>(R.id.typeIndicator)

        when (item.type.lowercase(Locale.getDefault())) {
            "expense" -> typeIndicator.setBackgroundColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
            "transfer" -> typeIndicator.setBackgroundColor(holder.itemView.context.getColor(android.R.color.holo_blue_dark))
            "refund" -> typeIndicator.setBackgroundColor(holder.itemView.context.getColor(android.R.color.holo_orange_light))
            else -> typeIndicator.setBackgroundColor(holder.itemView.context.getColor(android.R.color.darker_gray))
        }

        // Log binding operation
        Log.d("TransactionAdapter.onBindViewHolder", "Bound transaction ${item.title} in TransactionAdapter using function onBindViewHolder")
    }

    /// Returns the number of transaction items in the list
    override fun getItemCount(): Int {
        // Log current item count
        Log.d("TransactionAdapter.getItemCount", "Returning transaction list size: ${transactionList.size} in TransactionAdapter using function getItemCount")
        return transactionList.size
    }

    /// Updates the adapter with a new list of transactions
    fun updateList(newList: List<TransactionItem>) {
        // Clear old list
        transactionList.clear()

        // Add all new transactions
        transactionList.addAll(newList)

        // Notify the RecyclerView to refresh the UI
        notifyDataSetChanged()

        // Log list update
        Log.d("TransactionAdapter.updateList", "Updated transaction list with ${newList.size} items in TransactionAdapter using function updateList")
    }

    /// Downloads an image from the given URI and saves it to external storage (with Q+ support)
    private fun downloadImage(context: Context, uri: Uri, title: String) {
        try {
            // Open input stream from URI
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)

            // Generate timestamped file name
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${title}_$timestamp.jpg"

            // Save the image depending on Android version
            val savedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for scoped storage (Android Q and above)
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CentSnippers")
                }

                val imageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                imageUri?.let { uriOut ->
                    context.contentResolver.openOutputStream(uriOut)?.use { output ->
                        inputStream?.copyTo(output)
                    }
                }
                imageUri
            } else {
                // Use legacy external storage path for older Android versions
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "CentSnippers").apply { if (!exists()) mkdirs() }
                val outFile = File(appDir, fileName)

                FileOutputStream(outFile).use { output ->
                    inputStream?.copyTo(output)
                }

                Uri.fromFile(outFile)
            }

            // Show success or failure message
            if (savedUri != null) {
                Toast.makeText(context, "Image downloaded successfully", Toast.LENGTH_SHORT).show()
                Log.i("TransactionAdapter.downloadImage", "Image downloaded to: $savedUri in TransactionAdapter using function downloadImage")
            } else {
                throw Exception("File save failed")
            }

        } catch (e: Exception) {
            // Handle and log errors during download
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("TransactionAdapter.downloadImage", "Download failed in TransactionAdapter using function downloadImage: ${e.message}")
            e.printStackTrace()
        }
    }
}
