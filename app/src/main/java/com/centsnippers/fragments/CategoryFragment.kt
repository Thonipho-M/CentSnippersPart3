package com.centsnippers.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.centsnippers.adapters.CategoryTabAdapter
import com.centsnippers.databinding.FragmentCategoryBinding
import com.google.android.material.tabs.TabLayoutMediator

/**
 * CategoryFragment sets up a tabbed view containing two tabs:
 * 1. CategoryListTabFragment - showing the category list
 * 2. CategoryGraphTabFragment - placeholder for graph view
 */
class CategoryFragment : Fragment() {

    // -------------------- Binding Setup --------------------

    // ViewBinding object for accessing layout views in a type-safe way
    private var _binding: FragmentCategoryBinding? = null

    // Safe getter for binding, guarantees non-null after onCreateView
    private val binding get() = _binding!!

    // -------------------- Fragment Lifecycle --------------------

    /**
     * Inflates the layout using FragmentCategoryBinding and returns the root view.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the binding for this fragment
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        val tab1Fragment = childFragmentManager.findFragmentByTag("f0") as? CategoryListTabFragment



        // Log layout inflation
        Log.d("CategoryFragment.onCreateView", "Inflated layout and initialized binding")

        // Return the root view
        return binding.root
    }

    /**
     * Called after the view has been created. Initializes tabs and connects them to the adapter.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabAdapter = CategoryTabAdapter(this)
        binding.viewPager.adapter = tabAdapter

        //  Always attach listener when view is created
        Log.d("CategoryFragment", "Attempting to find tab1Fragment (f0)")
        // 🔁 Delayed callback to give ViewPager2 time to create the fragments
        Handler(Looper.getMainLooper()).postDelayed({
            val tab1Fragment = childFragmentManager.findFragmentByTag("f0")

            if (tab1Fragment == null) {
                Log.w("CategoryFragment", "Delayed: tab1Fragment is STILL NULL")
            } else {
                Log.d("CategoryFragment", "Delayed: tab1Fragment FOUND — setting listener")
                (tab1Fragment as? CategoryListTabFragment)?.setOnTotalCalculatedListener(object : OnCategoryTotalCalculatedListener {
                    override fun onTotalCalculated(total: Double) {
                        binding.totalCategoryTextView.text = "Total Budgeted per month: R%.2f".format(total)
                        Log.d("CategoryFragment", "Total received from tab1 (after delay): R$total")
                    }
                })
            }
        }, 300) // Wait 300ms (safe default)


        // 🔘 Now the FAB
        binding.addCategoryFab.setOnClickListener {
            Log.d("CategoryFragment", "Global FAB clicked")

            val tab1 = childFragmentManager.findFragmentByTag("f0")
            if (tab1 is CategoryListTabFragment) {
                tab1.launchAddDialogFromParentTab()
            } else {
                Log.w("CategoryFragment", "Tab 1 not found on FAB click")
            }
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "List"
                1 -> "Graph"
                else -> "Tab ${position + 1}"
            }
        }.attach()

        Log.d("CategoryFragment.onViewCreated", "TabLayout and ViewPager2 initialized")
    }



    /**
     * Nullifies the binding when the view is destroyed to avoid memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("CategoryFragment.onDestroyView", "Binding set to null on destroy")
    }
}