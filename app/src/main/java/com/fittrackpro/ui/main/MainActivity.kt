package com.fittrackpro.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.fittrackpro.R
import com.fittrackpro.databinding.ActivityMainBinding
import com.fittrackpro.ui.auth.AuthActivity
import com.fittrackpro.ui.tracking.LiveTrackingActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupFab()
        observeViewModel()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        // Handle navigation destination changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_dashboard,
                R.id.navigation_activities,
                R.id.navigation_training,
                R.id.navigation_social,
                R.id.navigation_profile -> {
                    showBottomNav()
                }
                else -> {
                    // Keep bottom nav visible for nested destinations
                }
            }
        }
    }

    private fun setupFab() {
        binding.fabStartTracking.setOnClickListener {
            // Launch live tracking activity
            val intent = Intent(this, LiveTrackingActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        // Observe user state, notifications, etc.
        viewModel.isUserLoggedIn.observe(this) { isLoggedIn ->
            if (!isLoggedIn) {
                // Navigate to auth screen
                val intent = Intent(this, AuthActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    private fun showBottomNav() {
        binding.bottomNavigation.visibility = android.view.View.VISIBLE
        binding.fabStartTracking.visibility = android.view.View.VISIBLE
    }

    private fun hideBottomNav() {
        binding.bottomNavigation.visibility = android.view.View.GONE
        binding.fabStartTracking.visibility = android.view.View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
