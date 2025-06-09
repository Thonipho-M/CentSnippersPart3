package com.centsnippers.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.centsnippers.databinding.ItemArticleBinding
import com.centsnippers.models.ArticleItem


class ArticleAdapter(
    private val articles: List<ArticleItem>,
    private val onItemClick: (ArticleItem) -> Unit
) : RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder>() {

    inner class ArticleViewHolder(val binding: ItemArticleBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val binding = ItemArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ArticleViewHolder(binding)
    }

    override fun getItemCount(): Int = articles.size

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val item = articles[position]
        holder.binding.articleTitle.text = item.title
        holder.binding.articleSummary.text = item.summary
        holder.binding.root.setOnClickListener {
            onItemClick(item)
        }
    }
}
