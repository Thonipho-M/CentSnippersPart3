package com.centsnippers.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.centsnippers.databinding.ItemVideoBinding
import com.centsnippers.models.VideoItem

class VideoAdapter(
    private val videos: List<VideoItem>,
    private val onItemClick: (VideoItem) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    inner class VideoViewHolder(val binding: ItemVideoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun getItemCount(): Int = videos.size

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        with(holder.binding) {
            videoTitle.text = video.title
            Glide.with(root.context)
                .load(video.thumbnailUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop()
                .into(videoThumbnail)

            // Optional accessibility improvement
            videoThumbnail.contentDescription = "Thumbnail for ${video.title}"

            root.setOnClickListener {
                onItemClick(video)
            }
        }
    }
}