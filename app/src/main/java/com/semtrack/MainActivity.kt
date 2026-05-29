package com.semtrack

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {
    
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        val appBarLayout = findViewById<AppBarLayout>(R.id.appbar_layout)
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        navView.itemIconTintList = null

        // Handle window insets so the toolbar doesn't overlap with the status bar
        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        // Set up the toolbar as the action bar
        setSupportActionBar(toolbar)

        // Add the hamburger icon to the toolbar to open/close the drawer
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, 
            R.string.navigation_drawer_open, 
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Handle navigation item clicks
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tasks -> {
                    toolbar.title = "Tasks"
                    replaceFragment(TasksFragment())
                }
                R.id.nav_attendance -> {
                    toolbar.title = "Attendance"
                    replaceFragment(AttendanceFragment())
                }
                R.id.nav_evaluations -> {
                    toolbar.title = "Evaluations"
                    replaceFragment(EvaluationsFragment())
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        // Load the Tasks fragment by default on startup
        if (savedInstanceState == null) {
            navView.setCheckedItem(R.id.nav_tasks)
            toolbar.title = "Tasks"
            replaceFragment(TasksFragment())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}