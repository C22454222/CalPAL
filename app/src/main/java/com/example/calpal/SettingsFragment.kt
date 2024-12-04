package com.example.calpal

// Android and Kotlin standard library imports
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * SettingsFragment manages user account settings and preferences.
 * Provides functionality for dark mode toggling, logout, and account deletion.
 */
class SettingsFragment : Fragment() {

    // ViewModel for managing user-related data operations
    private lateinit var mainViewModel: MainViewModel

    /**
     * Creates and configures the settings fragment view.
     * Sets up dark mode switch, logout, and delete account buttons.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate settings fragment layout
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Initialize ViewModel for data management
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        // Configure dark mode toggle switch
        val darkModeSwitch = view.findViewById<SwitchCompat>(R.id.darkModeSwitch)
        darkModeSwitch.isChecked = DarkModePreferences.isDarkModeEnabled(requireContext())
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            DarkModePreferences.setDarkModeEnabled(requireContext(), isChecked)
            DarkModeDelegate.applyDarkMode(requireActivity())
        }

        // Set up logout button
        val logoutButton = view.findViewById<Button>(R.id.button_logout)
        logoutButton.setOnClickListener {
            handleLogout()
        }

        // Set up delete account button
        val deleteAccountButton = view.findViewById<Button>(R.id.button_delete_account)
        deleteAccountButton.setOnClickListener {
            showDeleteConfirmationDialog {
                handleDeleteAccount()
            }
        }

        return view
    }

    /**
     * Handles user logout process.
     * Updates user login status and navigates to login page.
     */
    private fun handleLogout() {
        viewLifecycleOwner.lifecycleScope.launch {
            val loggedInUser = mainViewModel.getLoggedInUser()
            if (loggedInUser != null) {
                loggedInUser.isLoggedIn = false
                mainViewModel.updateUserLogin(loggedInUser) // Update login record
                Toast.makeText(context, getString(R.string.logged_out), Toast.LENGTH_SHORT).show()
                navigateToLoginPage()
            } else {
                Toast.makeText(context, "No user is currently logged in.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Handles complete account deletion.
     * Removes user sign-up and login data from database.
     */
    private fun handleDeleteAccount() {
        viewLifecycleOwner.lifecycleScope.launch {
            val loggedInUser = mainViewModel.getLoggedInUser()
            if (loggedInUser != null) {
                val userSignUp = mainViewModel.getUserSignUpByUsername(loggedInUser.username)
                if (userSignUp != null) {
                    mainViewModel.deleteUserSignUp(userSignUp) // Delete account details
                    mainViewModel.deleteUserLogin(loggedInUser) // Remove login data
                    Toast.makeText(context, "Account deleted successfully.", Toast.LENGTH_SHORT).show()
                    navigateToLoginPage()
                } else {
                    Toast.makeText(context, "Error: Account details not found.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "No user is currently logged in.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Displays a confirmation dialog before account deletion.
     * @param onConfirm Callback function to execute upon user confirmation
     */
    private fun showDeleteConfirmationDialog(onConfirm: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Navigates to the login activity, clearing the current activity stack.
     */
    private fun navigateToLoginPage() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        // Prevent user from navigating back to previous screens
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    /**
     * Manages dark mode preferences via SharedPreferences.
     */
    internal object DarkModePreferences {
        private const val PREF_DARK_MODE = "dark_mode"

        /**
         * Checks if dark mode is currently enabled.
         * @param context Application context
         * @return Boolean indicating dark mode status
         */
        fun isDarkModeEnabled(context: android.content.Context): Boolean {
            val sharedPreferences =
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getBoolean(PREF_DARK_MODE, false)
        }

        /**
         * Sets dark mode preference.
         * @param context Application context
         * @param enabled Dark mode toggle state
         */
        fun setDarkModeEnabled(context: android.content.Context, enabled: Boolean) {
            val sharedPreferences =
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            sharedPreferences.edit().putBoolean(PREF_DARK_MODE, enabled).apply()
        }
    }

    /**
     * Handles application of dark mode across the app.
     */
    internal object DarkModeDelegate {
        /**
         * Applies dark mode setting to the entire activity.
         * @param activity Current fragment activity
         */
        fun applyDarkMode(activity: androidx.fragment.app.FragmentActivity) {
            val isDarkModeEnabled = DarkModePreferences.isDarkModeEnabled(activity)
            val mode = if (isDarkModeEnabled) {
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            } else {
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            }
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode)
        }
    }
}