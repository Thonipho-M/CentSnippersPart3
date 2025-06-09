package com.centsnippers.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
<<<<<<< HEAD
import android.util.Log
=======
>>>>>>> 37c624634abd3be351831d5d7260137885392c23
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
<<<<<<< HEAD
import com.centsnippers.MainActivity
=======
>>>>>>> 37c624634abd3be351831d5d7260137885392c23
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
<<<<<<< HEAD
        // Inflate the layout for this fragment
        _binding = FragmentEducationBinding.inflate(inflater, container, false)
        Log.d("EducationFragment", "View created and binding initialized")
=======
        _binding = FragmentEducationBinding.inflate(inflater, container, false)
>>>>>>> 37c624634abd3be351831d5d7260137885392c23
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
<<<<<<< HEAD
        Log.d("EducationFragment", "onViewCreated triggered")
        super.onViewCreated(view, savedInstanceState)
        // Setup custom back button to return to dashboard and restore nav
        binding.btnBack.setOnClickListener {
            Log.i("EducationFragment", "Back button clicked, navigating back to previous screen")
            parentFragmentManager.popBackStack()
            (requireActivity() as MainActivity).showBottomNavView()
        }

        // Initialize both RecyclerViews
        setupArticleRecyclerView()
        setupVideoRecyclerView()
    }
    /// Prepares and binds article data to the articleRecyclerView
    private fun setupArticleRecyclerView() {
        Log.d("EducationFragment", "Setting up Article RecyclerView")
=======
        super.onViewCreated(view, savedInstanceState)
        setupArticleRecyclerView()
        setupVideoRecyclerView()
    }

    private fun setupArticleRecyclerView() {
>>>>>>> 37c624634abd3be351831d5d7260137885392c23
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
<<<<<<< HEAD
            Log.d("EducationFragment", "Opening article: ${article.title}")
=======
>>>>>>> 37c624634abd3be351831d5d7260137885392c23
            openUrl(article.url)
        }

        binding.articleRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.articleRecyclerView.adapter = articleAdapter
<<<<<<< HEAD
        Log.i("EducationFragment", "Article RecyclerView initialized with ${articles.size} items")
    }

    /// Prepares and binds video data to the videoRecyclerView
    private fun setupVideoRecyclerView() {
        Log.d("EducationFragment", "Setting up Video RecyclerView")
=======
    }

    private fun setupVideoRecyclerView() {
>>>>>>> 37c624634abd3be351831d5d7260137885392c23
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
<<<<<<< HEAD
        Log.i("EducationFragment", "Video RecyclerView initialized with ${videos.size} items")
    }

    /// Launches a URL in an external browser
    private fun openUrl(url: String) {
        Log.d("EducationFragment", "Launching URL: $url")
=======
    }

    private fun openUrl(url: String) {
>>>>>>> 37c624634abd3be351831d5d7260137885392c23
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
<<<<<<< HEAD
        Log.d("EducationFragment", "View destroyed, binding set to null")
=======
>>>>>>> 37c624634abd3be351831d5d7260137885392c23
        _binding = null
    }
}