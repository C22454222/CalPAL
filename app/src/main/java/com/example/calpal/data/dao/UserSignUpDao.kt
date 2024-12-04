package com.example.calpal.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.calpal.data.entities.UserSignUp
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for UserSignUp entity.
 * Provides database operations for user sign-up records.
 */
@Dao
interface UserSignUpDao {

    /**
     * Finds a user sign-up record by unique username.
     * @param username User's unique identifier
     * @return UserSignUp record or null if not found
     */
    @Query("SELECT * FROM userSignUp WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): UserSignUp?

    /**
     * Finds a user sign-up record by password hash.
     * @param password Hashed password to search
     * @return UserSignUp record or null if not found
     */
    @Query("SELECT * FROM userSignUp WHERE password_hash = :password LIMIT 1")
    suspend fun findByPassword(password: String): UserSignUp?

    /**
     * Finds a user sign-up record by email address.
     * @param email User's email address
     * @return UserSignUp record or null if not found
     */
    @Query("SELECT * FROM userSignUp WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserSignUp?

    /**
     * Inserts multiple user sign-up records simultaneously.
     * @param userSignUp Variable number of UserSignUp entities
     */
    @Insert
    suspend fun insertAll(vararg userSignUp: UserSignUp)

    /**
     * Retrieves all user sign-up records as a Flow for reactive updates.
     * @return Flow of all UserSignUp records
     */
    @Query("SELECT * FROM userSignUp")
    fun getAll(): Flow<List<UserSignUp>>

    /**
     * Loads user sign-up records by their specific IDs.
     * @param userIds Array of user IDs to retrieve
     * @return Flow of UserSignUp records matching given IDs
     */
    @Query("SELECT * FROM userSignUp WHERE id IN (:userIds)")
    fun loadAllByIds(userIds: IntArray): Flow<List<UserSignUp>>

    /**
     * Finds a user sign-up record by first and last name.
     * @param first First name to match
     * @param last Last name to match
     * @return UserSignUp record or null if not found
     */
    @Query(
        "SELECT * FROM userSignUp WHERE first_name LIKE :first AND " +
                "last_name LIKE :last LIMIT 1"
    )
    suspend fun findByName(first: String, last: String): UserSignUp?

    /**
     * Deletes a specific user sign-up record.
     * @param userSignUp UserSignUp entity to delete
     */
    @Delete
    suspend fun delete(userSignUp: UserSignUp)
}