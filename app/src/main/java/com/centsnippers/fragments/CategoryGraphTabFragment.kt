package com.centsnippers.fragments

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.centsnippers.databinding.FragmentCategoryGraphTabBinding

/**
 * CategoryGraphTabFragment is responsible for rendering a visual
 * representation of category spending using graphs (planned).
 * This fragment will eventually display the same category data
 * shown in Tab 1 but in a graphical/chart format.
 */
class CategoryGraphTabFragment : Fragment() {

    // -------------------- Binding Setup --------------------

    // ViewBinding object for accessing layout views in a type-safe way
    private var _binding: FragmentCategoryGraphTabBinding? = null

    // Safe getter for binding, guarantees non-null after onCreateView
    private val binding get() = _binding!!

    // -------------------- Fragment Lifecycle --------------------

    /**
     * Inflates the layout and binds the ViewBinding object.
     * This is where we prepare the base UI for the graph view.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using ViewBinding for this fragment
        _binding = FragmentCategoryGraphTabBinding.inflate(inflater, container, false)

        // Log that the view is being created
        Log.d("CategoryGraphTabFragment.onCreateView", "Graph tab layout inflated successfully")

        // Return the root view to be rendered
        return binding.root
    }

    /**
     * Called once the view is created. Ideal for setting up logic,
     * listeners, future graph drawing, and observing shared data.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Log lifecycle point reached
        Log.d("CategoryGraphTabFragment.onViewCreated", "Graph tab UI view created")

        // Placeholder for now — show a static label
        setupPlaceholderUI()
    }

    // -------------------- UI Setup --------------------

    /**
     * Temporary placeholder while the actual graph rendering logic
     * is still under development.
     */
    private fun setupPlaceholderUI() {
        // Set the text of the placeholder view
        binding.graphPlaceholderText.text = "Graph view coming soon..."

        // Optional: style or animate this later
        Log.i("CategoryGraphTabFragment.setupPlaceholderUI", "Graph placeholder set in UI")
    }

    // -------------------- Cleanup --------------------

    /**
     * Called when the fragment’s view is destroyed. Clears the binding.
     */
    override fun onDestroyView() {
        super.onDestroyView()

        // Nullify binding reference to prevent memory leaks
        _binding = null

        // Log destruction for debugging
        Log.d("CategoryGraphTabFragment.onDestroyView", "View destroyed and binding set to null")
    }
}
