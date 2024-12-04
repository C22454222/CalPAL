package com.example.calpal.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.calpal.data.entities.UserLogin

/**
 * Data Access Object for UserLogin entity.
 * Provides database operations for user login records.
 */
@Dao
interface UserLoginDao {

    /**
     * Retrieves a user login record by username.
     * @param username User's unique identifier
     * @return UserLogin record or null if not found
     */
    @Query("SELECT * FROM userLogin WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): UserLogin?

    /**
     * Retrieves the currently active logged-in user.
     * @return UserLogin record of logged-in user or null
     */
    @Query("SELECT * FROM userLogin WHERE login_bool = 1 LIMIT 1")
    suspend fun getLoggedInUser(): UserLogin?

    /**
     * Updates an existing user login record.
     * @param userLogin UserLogin entity to update
     */
    @Update
    suspend fun updateUserLogin(userLogin: UserLogin)

    /**
     * Inserts a new user login record.
     * @param userLogin UserLogin entity to insert
     */
    @Insert
    suspend fun insert(userLogin: UserLogin)

    /**
     * Deletes a specific user login record.
     * @param userLogin UserLogin entity to delete
     */
    @Delete
    suspend fun delete(userLogin: UserLogin)

    /**
     * Clears all login records by resetting login status.
     */
    @Query("UPDATE userLogin SET login_bool = 0")
    suspend fun clearAllLogins()
}