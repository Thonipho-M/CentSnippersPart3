package com.centsnippers.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.centsnippers.fragments.CategoryListTabFragment
import com.centsnippers.fragments.CategoryGraphTabFragment
import android.util.Log

/**
 * CategoryTabAdapter is responsible for supplying the two tab fragments
 * to the ViewPager2 inside CategoryFragment.
 */
class CategoryTabAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    // Number of tabs to manage
    override fun getItemCount(): Int = 2

    /**
     * Called by ViewPager2 to instantiate the correct fragment based on index.
     */
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                Log.d("CategoryTabAdapter", "Creating fragment at position 0: CategoryListTabFragment")
                CategoryListTabFragment()
            }
            1 -> {
                Log.d("CategoryTabAdapter", "Creating fragment at position 1: CategoryGraphTabFragment")
                CategoryGraphTabFragment()
            }
            else -> {
                Log.e("CategoryTabAdapter", "Invalid position $position")
                throw IndexOutOfBoundsException("Invalid tab index $position")
            }
        }
    }
}
