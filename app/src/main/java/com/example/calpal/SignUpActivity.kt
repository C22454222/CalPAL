package com.example.calpal

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.calpal.data.entities.UserSignUp
import kotlinx.coroutines.launch
import java.security.MessageDigest

// This activity handles user registration functionality.
class SignUpActivity : AppCompatActivity() {

    // Declare a reference to the ViewModel that interacts with the database.
    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Initialize the ViewModel using the ViewModelProvider.
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // Link the UI components in the XML layout to Kotlin variables.
        val firstNameField: EditText = findViewById(R.id.editText_firstName)
        val lastNameField: EditText = findViewById(R.id.editText_lastName)
        val emailField: EditText = findViewById(R.id.editText_email)
        val usernameField: EditText = findViewById(R.id.editText_username)
        val passwordField: EditText = findViewById(R.id.editText_password)
        val signUpButton: Button = findViewById(R.id.button_signup)
        val loginLink: TextView = findViewById(R.id.textView_login)

        // Set a click listener for the sign-up button to handle user registration.
        signUpButton.setOnClickListener {
            // Extract and trim user input values.
            val firstName = firstNameField.text.toString().trim()
            val lastName = lastNameField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val username = usernameField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            // Check if any input fields are empty, and notify the user if so.
            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Stop further execution.
            }

            // Validate email format and notify the user if invalid.
            if (!isValidEmail(email)) {
                Toast.makeText(this, "Invalid email format!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Stop further execution.
            }

            // Validate password length and notify the user if it is too short.
            if (password.length < 8) {
                Toast.makeText(this, "Password must be at least 8 characters long!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Stop further execution.
            }

            // Launch a coroutine to perform database operations on a background thread.
            lifecycleScope.launch {
                try {
                    // Check if the username or email is already registered.
                    val existingUserByUsername = mainViewModel.getUserSignUpByUsername(username)
                    val existingUserByEmail = mainViewModel.getUserSignUpByEmail(email)

                    // Notify the user if the username is already taken.
                    if (existingUserByUsername != null) {
                        Toast.makeText(this@SignUpActivity, "Username is already taken!", Toast.LENGTH_SHORT).show()
                        return@launch // Exit coroutine early.
                    }

                    // Notify the user if the email is already registered.
                    if (existingUserByEmail != null) {
                        Toast.makeText(this@SignUpActivity, "Email is already registered!", Toast.LENGTH_SHORT).show()
                        return@launch // Exit coroutine early.
                    }

                    // Hash the password securely before saving it to the database.
                    val hashedPassword = hashPassword(password)

                    // Create a new `UserSignUp` object with the provided information.
                    val userSignUp = UserSignUp(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        username = username,
                        passwordHash = hashedPassword
                    )

                    // Insert the new user record into the database.
                    mainViewModel.insertUserSignUps(userSignUp)

                    // Notify the user of successful registration and finish the activity.
                    Toast.makeText(this@SignUpActivity, "Sign-up successful!", Toast.LENGTH_SHORT).show()
                    finish() // Close the activity and return to the previous screen.
                } catch (e: Exception) {
                    // Handle any errors during the sign-up process.
                    Toast.makeText(this@SignUpActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Set a click listener for the login link to navigate to the login screen.
        loginLink.setOnClickListener {
            // Create an intent to start the `LoginActivity`.
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent) // Start the login activity.
            finish() // Close the sign-up activity to avoid navigation back.
        }
    }

    // A utility function to hash the password securely using SHA-256.
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256") // Get the SHA-256 digest algorithm.
        val hashedBytes = digest.digest(password.toByteArray()) // Hash the password.
        // Convert the hashed bytes to a hexadecimal string.
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }

    // A utility function to validate the email format using Android's built-in patterns.
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches() // Returns true if the email matches the pattern.
    }
}


