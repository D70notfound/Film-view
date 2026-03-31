package net.nepuview.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.window.layout.WindowMetricsCalculator
import dagger.hilt.android.AndroidEntryPoint
import net.nepuview.R

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupNavigation()
        observeDestinationChanges()
    }

    private fun setupNavigation() {
        val windowMetrics = WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(this)
        val widthDp = windowMetrics.bounds.width() / resources.displayMetrics.density

        when {
            widthDp >= 840 -> {
                // Tablet: NavigationDrawer
                findViewById<View?>(R.id.bottom_nav)?.visibility = View.GONE
                findViewById<View?>(R.id.navigation_rail)?.visibility = View.GONE
                findViewById<com.google.android.material.navigation.NavigationView?>(R.id.navigation_drawer)?.let {
                    it.visibility = View.VISIBLE
                    it.setupWithNavController(navController)
                }
            }
            widthDp >= 600 -> {
                // Medium: NavigationRail
                findViewById<View?>(R.id.bottom_nav)?.visibility = View.GONE
                findViewById<View?>(R.id.navigation_drawer)?.visibility = View.GONE
                findViewById<com.google.android.material.navigationrail.NavigationRailView?>(R.id.navigation_rail)?.let {
                    it.visibility = View.VISIBLE
                    it.setupWithNavController(navController)
                }
            }
            else -> {
                // Phone: BottomNavigationView
                findViewById<View?>(R.id.navigation_rail)?.visibility = View.GONE
                findViewById<View?>(R.id.navigation_drawer)?.visibility = View.GONE
                findViewById<com.google.android.material.bottomnavigation.BottomNavigationView?>(R.id.bottom_nav)?.let {
                    it.visibility = View.VISIBLE
                    it.setupWithNavController(navController)
                }
            }
        }
    }

    private fun observeDestinationChanges() {
        val fullscreenDests = setOf(
            net.nepuview.R.id.playerFragment,
            net.nepuview.R.id.homeFragment
        )
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val hide = destination.id in fullscreenDests
            val v = if (hide) View.GONE else View.VISIBLE
            findViewById<View?>(R.id.bottom_nav)?.visibility = v
            findViewById<View?>(R.id.navigation_rail)?.visibility = v
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp() || super.onSupportNavigateUp()
}
