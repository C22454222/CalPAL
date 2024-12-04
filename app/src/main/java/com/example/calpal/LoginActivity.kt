package com.example.calpal

import android.content.Context // Provides access to application-specific resources and classes
import android.content.Intent // Used to start new activities or pass data between components
import android.os.Bundle // Used to pass data between Android components
import android.widget.EditText // Input field for user text input
import android.widget.TextView // Text component to display text on the screen
import android.widget.Toast // Displays brief messages as a popup
import androidx.appcompat.app.AppCompatActivity // Base class for activities
import androidx.lifecycle.ViewModelProvider // Provides ViewModel instances
import androidx.lifecycle.lifecycleScope // Lifecycle-aware coroutine scope
import com.example.calpal.data.entities.UserLogin // Data class representing a user login
import kotlinx.coroutines.launch // Used to launch coroutines
import java.security.MessageDigest // Provides cryptographic hash functions

// LoginActivity handles the user login functionality
class LoginActivity : AppCompatActivity() {

    // UI components for user input and interaction
    private lateinit var usernameEditText: EditText // Input field for username
    private lateinit var passwordEditText: EditText // Input field for password
    private lateinit var loginButton: TextView // Button for submitting the login
    private lateinit var mainViewModel: MainViewModel // ViewModel for handling data operations

    // Called when the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // Set the layout for the activity

        // Initialize UI components
        usernameEditText = findViewById(R.id.editText_username)
        passwordEditText = findViewById(R.id.editText_password)
        loginButton = findViewById(R.id.button_login)

        // Initialize the ViewModel for managing data operations
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // Set a click listener for the login button
        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim() // Get trimmed username input
            val password = passwordEditText.text.toString().trim() // Get trimmed password input

            // Check if username or password is empty and show a Toast message
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    this@LoginActivity,
                    "Username and password are required",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener // Exit the click listener
            }

            // Launch a coroutine to handle database operations asynchronously
            lifecycleScope.launch {
                // Fetch the user with the given username from the database
                val user = mainViewModel.getUserSignUpByUsername(username)

                // Check if the user exists
                if (user == null) {
                    // Show a message if the username is not found
                    Toast.makeText(
                        this@LoginActivity,
                        "Username does not exist",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch // Exit the coroutine
                }

                // Hash the entered password for comparison
                val hashedPassword = hashPassword(password)

                // Compare the hashed password with the stored hash
                if (user.passwordHash == hashedPassword) {
                    // Clear all previous login states
                    mainViewModel.clearAllLogins()

                    // Check if there is an existing login entry for this user
                    val existingLogin = mainViewModel.getUserLoginByUsername(username)
                    if (existingLogin != null) {
                        // Update the existing login entry to mark the user as logged in
                        existingLogin.isLoggedIn = true
                        mainViewModel.updateUserLogin(existingLogin)
                    } else {
                        // Create a new login entry for the user
                        val newUserLogin = UserLogin(
                            username = user.username,
                            passwordHash = user.passwordHash,
                            userSignUpId = user.id,
                            isLoggedIn = true
                        )
                        mainViewModel.insertUserLogin(newUserLogin) // Save the new login entry
                    }

                    // Store the user ID in shared preferences for persistence
                    val sharedPreferences =
                        getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    sharedPreferences.edit()
                        .putInt("userId", user.id)
                        .apply()

                    // Show a success message and navigate to the MainActivity
                    Toast.makeText(
                        this@LoginActivity,
                        "You've signed in correctly",
                        Toast.LENGTH_SHORT
                    ).show()
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Close the LoginActivity
                } else {
                    // Show an error message if the password is incorrect
                    Toast.makeText(this@LoginActivity, "Incorrect password", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        // Set a click listener for the "Sign Up" link to navigate to the SignUpActivity
        findViewById<TextView>(R.id.textView_signup).setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    // Helper function to hash a password using SHA-256
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256") // Get the SHA-256 message digest instance
        val hashedBytes = digest.digest(password.toByteArray()) // Hash the password into bytes
        // Convert the bytes into a hexadecimal string and return
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }
}
