package com.centsnippers.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.centsnippers.adapters.ArticleAdapter
import com.centsnippers.adapters.VideoAdapter
import com.centsnippers.databinding.FragmentEducationBinding
import com.centsnippers.models.ArticleItem
import com.centsnippers.models.VideoItem

class EducationFragment : Fragment() {

    private var _binding: FragmentEducationBinding? = null
    private val binding get() = _binding!!

    private lateinit var articleAdapter: ArticleAdapter
    private lateinit var videoAdapter: VideoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEducationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupArticleRecyclerView()
        setupVideoRecyclerView()
    }

    private fun setupArticleRecyclerView() {
        val articles = listOf(
            ArticleItem(
                title = "Making a budget",
                summary = "Learn how to split your income into needs, wants, and savings effectively.",
                url = "https://consumer.gov/your-money/making-budget"
            ),
            ArticleItem(
                title = "Budgeting Tips",
                summary = "Start saving for the unexpected with this step-by-step guide.",
                url = "https://www.oldmutual.co.za/articles/7-budgeting-tips-to-help-you-save/"
            )
        )

        articleAdapter = ArticleAdapter(articles) { article ->
            openUrl(article.url)
        }

        binding.articleRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.articleRecyclerView.adapter = articleAdapter
    }

    private fun setupVideoRecyclerView() {
        val videos = listOf(
            VideoItem(
                title = "Whats Really Hindering your Financial Freedom",
                thumbnailUrl = "https://img.youtube.com/vi/F7rksrKSkwQZ4SUX/0.jpg",
                videoUrl = "https://youtu.be/WBUO8uPUEBc?si=F7rksrKSkwQZ4SUX"
            ),
            VideoItem(
                title = "How do I budget with little money",
                thumbnailUrl = "https://img.youtube.com/vi/xb-IuVUpJvEDJXbG.jpg",
                videoUrl = "https://youtu.be/4Eh8QLcB1UQ?si=xb-IuVUpJvEDJXbG"
            )
        )

        videoAdapter = VideoAdapter(videos) { video ->
            openUrl(video.videoUrl)
        }

        binding.videoRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.videoRecyclerView.adapter = videoAdapter
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}