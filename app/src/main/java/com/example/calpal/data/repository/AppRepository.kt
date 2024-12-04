package com.example.calpal.data.repository

import android.util.Log
import com.example.calpal.data.dao.*
import com.example.calpal.data.entities.*
import kotlinx.coroutines.flow.Flow

/**
 * AppRepository acts as a centralized data access layer for the CalPal application.
 * It coordinates database operations across different DAOs (Data Access Objects).
 *
 * @param userSignUpDao DAO for user sign-up related database operations
 * @param userLoginDao DAO for user login related database operations
 * @param noteDao DAO for note-related database operations
 * @param eventDao DAO for event-related database operations
 */
class AppRepository(
    private val userSignUpDao: UserSignUpDao,
    private val userLoginDao: UserLoginDao,
    private val noteDao: NoteDao,
    private val eventDao: EventDao
) {

    // ----- User SignUp-related operations -----

    /**
     * Finds a user sign-up record by username.
     * @param username The username to search for
     * @return UserSignUp entity or null if not found
     */
    suspend fun findUserSignUpByUsername(username: String): UserSignUp? =
        userSignUpDao.findByUsername(username)

    /**
     * Finds a user sign-up record by email.
     * @param email The email to search for
     * @return UserSignUp entity or null if not found
     */
    suspend fun findUserSignUpByEmail(email: String): UserSignUp? =
        userSignUpDao.findByEmail(email)

    /**
     * Finds a user sign-up record by password.
     * Note: Searching by password directly is generally not a recommended practice.
     * @param password The password to search for
     * @return UserSignUp entity or null if not found
     */
    suspend fun findUserSignUpByPassword(password: String): UserSignUp? =
        userSignUpDao.findByPassword(password)

    /**
     * Inserts multiple user sign-up records.
     * @param userSignUps Variable number of UserSignUp entities to insert
     */
    suspend fun insertUserSignUps(vararg userSignUps: UserSignUp) =
        userSignUpDao.insertAll(*userSignUps)

    /**
     * Deletes a specific user sign-up record.
     * @param userSignUp The UserSignUp entity to delete
     */
    suspend fun deleteUserSignUp(userSignUp: UserSignUp) = userSignUpDao.delete(userSignUp)

    // ----- User Login-related operations -----

    /**
     * Retrieves a user login record by username.
     * @param username The username to search for
     * @return UserLogin entity or null if not found
     */
    suspend fun getUserLoginByUsername(username: String): UserLogin? =
        userLoginDao.findByUsername(username)

    /**
     * Retrieves the currently logged-in user.
     * @return UserLogin entity or null if no user is logged in
     */
    suspend fun getLoggedInUser(): UserLogin? = userLoginDao.getLoggedInUser()

    /**
     * Inserts a new user login record.
     * @param userLogin The UserLogin entity to insert
     */
    suspend fun insertUserLogin(userLogin: UserLogin) =
        userLoginDao.insert(userLogin)

    /**
     * Updates an existing user login record.
     * @param userLogin The UserLogin entity to update
     */
    suspend fun updateUserLogin(userLogin: UserLogin) {
        userLoginDao.updateUserLogin(userLogin)
    }

    /**
     * Deletes a specific user login record.
     * @param userLogin The UserLogin entity to delete
     */
    suspend fun deleteUserLogin(userLogin: UserLogin) =
        userLoginDao.delete(userLogin)

    /**
     * Clears all login records from the database.
     */
    suspend fun clearAllLogins() {
        userLoginDao.clearAllLogins()
    }

    // ----- Event-related operations -----

    /**
     * Inserts a new event record with error handling.
     * @param event The Event entity to insert
     * @throws IllegalStateException if insertion fails
     */
    suspend fun insertEvent(event: Event) = try {
        eventDao.insert(event)
    } catch (e: Exception) {
        throw IllegalStateException("Error inserting event: ${e.message}")
    }

    /**
     * Deletes a specific event record with error handling.
     * @param event The Event entity to delete
     * @throws IllegalStateException if deletion fails
     */
    suspend fun deleteEvent(event: Event) = try {
        eventDao.delete(event)
    } catch (e: Exception) {
        throw IllegalStateException("Error deleting event: ${e.message}")
    }

    /**
     * Retrieves events for a specific date and user.
     * @param date The date to retrieve events for
     * @param userId The ID of the user whose events to retrieve
     * @return Flow of Event lists matching the criteria
     */
    fun getEventsForDateAndUser(date: String, userId: Int): Flow<List<Event>> =
        eventDao.getEventsForDateAndUser(date, userId)

    /**
     * Retrieves all events for a specific user.
     * @param userId The ID of the user whose events to retrieve
     * @return Flow of Event lists for the user
     */
    fun getAllEventsForUser(userId: Int): Flow<List<Event>> {
        return eventDao.getAllEventsForUser(userId)
    }

    /**
     * Retrieves the next upcoming event for a user, considering date and time.
     * @param userId The ID of the user whose next event to retrieve
     * @param currentDate The current date to compare against
     * @param currentTime The current time to compare against
     * @return The next Event or null if no events exist
     */
    suspend fun getNextEventForUserSpec(userId: Int, currentDate: String, currentTime: String): Event? =
        eventDao.getNextEventForUserSpec(userId, currentDate, currentTime)

    suspend fun updateEvent(event: Event) {
        eventDao.updateEvent(event)
    }

    // ----- Note-related operations -----

    /**
     * Inserts a new note record with error logging.
     * @param note The Note entity to insert
     * @throws Exception if insertion fails
     */
    suspend fun insertNote(note: Note) {
        try {
            noteDao.insert(note)
        } catch (e: Exception) {
            Log.e("AppRepository", "Error inserting note: ${e.localizedMessage}")
            throw e
        }
    }

    /**
     * Deletes a specific note record with error logging.
     * @param note The Note entity to delete
     * @throws Exception if deletion fails
     */
    suspend fun deleteNote(note: Note) {
        try {
            noteDao.delete(note)
        } catch (e: Exception) {
            Log.e("AppRepository", "Error deleting note: ${e.localizedMessage}")
            throw e
        }
    }

    /**
     * Retrieves all notes for a specific event.
     * @param eventId The ID of the event to retrieve notes for
     * @return Flow of Note lists associated with the event
     */
    fun getNotesForEvent(eventId: Int): Flow<List<Note>> =
        noteDao.getNotesForEvent(eventId)
}