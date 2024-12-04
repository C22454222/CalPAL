package com.example.calpal.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// The UserSignUp data class represents the user registration details stored in the "userSignUp" table.
@Entity(
    tableName = "userSignUp",
    indices = [
        // Ensures unique values for the email and username fields
        Index(value = ["email"], unique = true),
        Index(value = ["username"], unique = true)
    ]
)
data class UserSignUp(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // Auto-generated unique identifier for each user sign-up entry.

    @ColumnInfo(name = "first_name")
    val firstName: String, // User's first name.

    @ColumnInfo(name = "last_name")
    val lastName: String, // User's last name.

    @ColumnInfo(name = "email")
    val email: String, // User's email address.

    @ColumnInfo(name = "username")
    val username: String, // User's chosen username.

    @ColumnInfo(name = "password_hash")
    val passwordHash: String, // Hashed password for security.

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis() // Timestamp for when the user signed up.
)