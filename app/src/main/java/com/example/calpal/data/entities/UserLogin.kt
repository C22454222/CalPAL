package com.example.calpal.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// The UserLogin data class represents the user login details stored in the "userLogin" table.
@Entity(
    tableName = "userLogin",
    indices = [
        // Ensures unique username values for user login
        Index(value = ["username"], unique = true)
    ],
    foreignKeys = [
        // Creates a foreign key relationship with the UserSignUp table based on the user ID
        ForeignKey(
            entity = UserSignUp::class,
            parentColumns = ["id"],
            childColumns = ["userSignUpId"],
            onDelete = ForeignKey.CASCADE // Deletes the login record if the associated sign-up record is deleted
        )
    ]
)
data class UserLogin(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // Auto-generated unique identifier for each login entry.

    @ColumnInfo(name = "username")
    val username: String, // Username associated with the login.

    @ColumnInfo(name = "password_hash")
    val passwordHash: String, // Hashed password for authentication.

    @ColumnInfo(name = "userSignUpId")
    val userSignUpId: Int, // Foreign key to the UserSignUp entity.

    @ColumnInfo(name = "login_bool")
    var isLoggedIn: Boolean = false // Flag to indicate whether the user is currently logged in.
)