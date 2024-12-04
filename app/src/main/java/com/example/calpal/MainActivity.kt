package com.example.calpal

import android.app.NotificationChannel // Required for notification channels on Android O and above
import android.app.NotificationManager // Manages notifications in the system
import android.content.Intent // Used to navigate between activities
import android.os.Build // Helps check the Android OS version
import android.os.Bundle // Carries data between Android components
import androidx.activity.viewModels // Delegates ViewModel instantiation to AndroidX library
import androidx.appcompat.app.AppCompatActivity // Base class for activities
import androidx.fragment.app.Fragment // Base class for UI fragments
import androidx.viewpager2.adapter.FragmentStateAdapter // Adapter for managing fragments in ViewPager2
import androidx.viewpager2.widget.ViewPager2 // Widget for horizontal paging of fragments
import androidx.work.PeriodicWorkRequestBuilder // Schedules periodic tasks with WorkManager
import androidx.work.WorkManager // Manages background tasks in the app
import com.google.android.material.bottomnavigation.BottomNavigationView // Provides a bottom navigation bar
import kotlinx.coroutines.runBlocking // Runs coroutines in a blocking manner
import java.util.concurrent.TimeUnit // Time unit constants for scheduling

// MainActivity serves as the central activity managing fragments and navigation
class MainActivity : AppCompatActivity() {

    // Late initialization for UI components
    private lateinit var viewPager: ViewPager2 // Manages horizontal fragment navigation
    private lateinit var bottomNavigationView: BottomNavigationView // Bottom navigation bar

    // ViewModel for handling data and business logic
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Set the activity layout

        // Create notification channel for event notifications
        createNotificationChannel()

        // Initialize the ViewPager and BottomNavigationView components
        viewPager = findViewById(R.id.view_pager)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Check if a user is logged in; if not, navigate to the login screen
        if (!isUserLoggedIn()) {
            navigateToLogin()
        } else {
            // Set up the ViewPager adapter and default to the HomeFragment
            viewPager.adapter = ViewPagerAdapter(this)
            viewPager.setCurrentItem(0, true) // Default to the first page (HomeFragment)

            // Set up navigation listener for the BottomNavigationView
            bottomNavigationView.setOnItemSelectedListener { item ->
                // Determine the fragment to display based on the selected menu item
                val selectedPage = when (item.itemId) {
                    R.id.nav_home -> 0 // Home page
                    R.id.nav_profile -> 1 // Profile page
                    R.id.nav_notes -> 2 // Notes page
                    R.id.nav_settings -> 3 // Settings page
                    else -> 0 // Default to the home page
                }
                // Change the displayed page in the ViewPager
                viewPager.setCurrentItem(selectedPage, true)
                true // Return true to indicate the event was handled
            }

            // Synchronize BottomNavigationView selection with ViewPager page changes
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    // Mark the corresponding menu item as selected
                    bottomNavigationView.menu.getItem(position).isChecked = true
                }
            })

            // Schedule periodic notifications for events
            scheduleNotificationWorker()
        }
    }

    // Checks if a user is logged in by querying the ViewModel
    private fun isUserLoggedIn(): Boolean {
        return runBlocking {
            val currentUser = mainViewModel.getLoggedInUser() // Retrieve the logged-in user
            currentUser?.isLoggedIn == true // Return true if a user is logged in
        }
    }

    // Navigates to the LoginActivity if no user is logged in
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java) // Intent to start LoginActivity
        startActivity(intent) // Start LoginActivity
        finish() // Finish MainActivity so the user cannot return without logging in
    }

    // Schedules a background worker to send periodic notifications
    private fun scheduleNotificationWorker() {
        // Create a periodic work request that runs every 15 minutes
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
            .build()
        // Enqueue the work request with WorkManager
        WorkManager.getInstance(this).enqueue(workRequest)
    }

    // Creates a notification channel for sending event notifications
    private fun createNotificationChannel() {
        // Only execute this code on Android O (API 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Define the notification channel's properties
            val channel = NotificationChannel(
                "event_notifications", // Unique ID for the channel
                "Event Notifications", // User-friendly name for the channel
                NotificationManager.IMPORTANCE_HIGH // Importance level for the notifications
            ).apply {
                description = "Channel for upcoming event notifications" // Description of the channel
            }
            // Get the system NotificationManager
            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
            // Create the notification channel
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Adapter for managing the fragments in the ViewPager
    private inner class ViewPagerAdapter(activity: AppCompatActivity) :
        FragmentStateAdapter(activity) {

        // Define the total number of fragments
        override fun getItemCount(): Int = 4

        // Return the fragment corresponding to the position
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> HomeFragment() // Home fragment
                1 -> ProfileFragment() // Profile fragment
                2 -> NotesFragment() // Notes fragment
                3 -> SettingsFragment() // Settings fragment
                else -> HomeFragment() // Default to Home fragment
            }
        }
    }
}
