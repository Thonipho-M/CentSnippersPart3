package com.centsnippers

// These are Android imports that give us access to core functionality
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.centsnippers.R
import com.centsnippers.databinding.ActivityMainBinding
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.net.Uri
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
// App-specific imports for session management, database, UI, etc.
import com.centsnippers.utils.SessionManager
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.centsnippers.adapters.TransactionAdapter
import com.centsnippers.data.DatabaseHelper
import com.centsnippers.databinding.FragmentTransactionBinding
import com.centsnippers.models.TransactionItem
import com.centsnippers.models.*
import java.util.*

class MainActivity : AppCompatActivity() {

    // sets up ViewBinding to be able to work with views easier and type-safe
    private lateinit var binding: ActivityMainBinding
//On(when) activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Find the navigation container (NavHostFragment) in the layout
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        // Get the navigation controller which lets us move between fragments
        val navController = navHostFragment.navController

        // This logs which fragment we're navigating to. Helps track bugs with nav not working
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            android.util.Log.d("NavTracker", "Navigated to: ${destination.label} (${destination.id})")
        }

        // This hides the bottom nav bar on the Login and Register screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.registerFragment -> {
                    // Hide bottom menu when user is logging in or registering
                    binding.bottomNavView.visibility = View.GONE
                }
                else -> {
                    // Show bottom menu on all other screens
                    binding.bottomNavView.visibility = View.VISIBLE
                }
            }
        }

        // This defines which screens are considered "top level" in the app
        // (prevents back button from appearing unnecessarily)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.categoryFragment,
                R.id.transactionFragment,
                R.id.dashboardFragment,
                R.id.goalsFragment,
            )
        )

        // This connects the bottom nav menu (bottom_nav_menu.xml) to the nav graph
        binding.bottomNavView.setupWithNavController(navController)
    }

    // This function creates the top-right menu in the toolbar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu using the top_right_menu.xml
        menuInflater.inflate(R.menu.top_right_menu, menu)
        return true
    }

    // This handles clicks on top-right menu items
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Get the nav controller again (safe way to access nav from this activity)
        val navController = findNavController(R.id.nav_host_fragment)

        return when (item.itemId) {
            // When "Income" menu item is clicked
            R.id.incomeFragment -> {
                navController.navigate(R.id.incomeFragment)
                true
            }
            // When "Goals" menu item is clicked
            R.id.goalsFragment -> {
                navController.navigate(R.id.goalsFragment)
                true
            }
            // When "Rewards" is clicked
            R.id.rewardsFragment -> {
                navController.navigate(R.id.rewardsFragment)
                true
            }
            // When "Help" is clicked
            R.id.helpPage -> {
                navController.navigate(R.id.helpPage)
                true
            }
            // When "Learn" is clicked
            R.id.educationFragment -> {
                navController.navigate(R.id.educationFragment)
                true
            }
            // Logout button clears the session and goes back to login screen
            R.id.logoutButton -> {
                // Clear saved user ID and other login info
                SessionManager(this).clearSession()
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()

                // Navigate back to login fragment after logout
                val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHost.navController

                navController.navigate(R.id.loginFragment)
                true
            }

            // Fallback if no menu item matches
            else -> super.onOptionsItemSelected(item)
        }
    }
}
