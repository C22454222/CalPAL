package com.example.calpal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calpal.data.database.AppDatabase
import com.example.calpal.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.calpal.data.entities.*
import kotlinx.coroutines.flow.Flow

/**
 * MainViewModel serves as the central ViewModel for the CalPal application.
 * It manages data operations for user sign-ups, logins, events, and notes.
 *
 * Uses AndroidViewModel to provide application context and lifecycle-aware
 * coroutine management for database operations.
 *
 * @param application The Android application context
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Repository for handling all data access and persistence operations
    private val appRepository: AppRepository

    /**
     * Initialization block that sets up the database and repository.
     * Creates database instance and initializes repository with DAOs for
     * different entity types.
     */
    init {
        // Get database instance for the application
        val db = AppDatabase.getDatabase(application)

        // Create repository with DAOs for different entity types
        appRepository = AppRepository(
            db.userSignUpDao(),
            db.userLoginDao(),
            db.noteDao(),
            db.eventDao()
        )
    }

    // ----- Sign Up related methods -----

    /**
     * Inserts one or more user sign-up records into the database.
     * Runs on IO dispatcher to avoid blocking the main thread.
     *
     * @param userSignUps Variable number of UserSignUp entities to insert
     */
    fun insertUserSignUps(vararg userSignUps: UserSignUp) {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.insertUserSignUps(*userSignUps)
        }
    }

    /**
     * Deletes a specific user sign-up record from the database.
     *
     * @param userSignUp The UserSignUp entity to be deleted
     */
    fun deleteUserSignUp(userSignUp: UserSignUp) {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.deleteUserSignUp(userSignUp)
        }
    }

    /**
     * Finds a user sign-up record by username.
     *
     * @param username The username to search for
     * @return UserSignUp entity or null if not found
     */
    suspend fun getUserSignUpByUsername(username: String): UserSignUp? {
        return appRepository.findUserSignUpByUsername(username)
    }

    /**
     * Finds a user sign-up record by email.
     *
     * @param email The email to search for
     * @return UserSignUp entity or null if not found
     */
    suspend fun getUserSignUpByEmail(email: String): UserSignUp? {
        return appRepository.findUserSignUpByEmail(email)
    }

    /**
     * Finds a user sign-up record by password.
     * Note: Searching by password directly is generally not a recommended practice.
     *
     * @param password The password to search for
     * @return UserSignUp entity or null if not found
     */
    suspend fun getUserSignUpByPassword(password: String): UserSignUp? {
        return appRepository.findUserSignUpByPassword(password)
    }

    // ----- Login related methods -----

    /**
     * Retrieves a user login record by username.
     *
     * @param username The username to search for
     * @return UserLogin entity or null if not found
     */
    suspend fun getUserLoginByUsername(username: String): UserLogin? {
        return appRepository.getUserLoginByUsername(username)
    }

    /**
     * Retrieves the currently logged-in user.
     *
     * @return UserLogin entity of the current user or null if no user is logged in
     */
    suspend fun getLoggedInUser(): UserLogin? {
        return appRepository.getLoggedInUser()
    }

    /**
     * Inserts a new user login record.
     * Runs on IO dispatcher to avoid blocking the main thread.
     *
     * @param userLogin The UserLogin entity to insert
     */
    fun insertUserLogin(userLogin: UserLogin) {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.insertUserLogin(userLogin)
        }
    }

    /**
     * Updates an existing user login record.
     * Runs on IO dispatcher to avoid blocking the main thread.
     *
     * @param userLogin The UserLogin entity to update
     */
    fun updateUserLogin(userLogin: UserLogin) {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.updateUserLogin(userLogin)
        }
    }

    /**
     * Deletes a specific user login record.
     * Runs on IO dispatcher to avoid blocking the main thread.
     *
     * @param userLogin The UserLogin entity to delete
     */
    fun deleteUserLogin(userLogin: UserLogin) {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.deleteUserLogin(userLogin)
        }
    }

    /**
     * Clears all login records from the database.
     * Runs on IO dispatcher to avoid blocking the main thread.
     */
    fun clearAllLogins() {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.clearAllLogins()
        }
    }

    // ----- Event related methods -----

    /**
     * Inserts a new event record.
     * Runs on IO dispatcher to avoid blocking the main thread.
     *
     * @param event The Event entity to insert
     */
    fun insertEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.insertEvent(event)
        }
    }

    /**
     * Deletes a specific event record.
     * Runs on IO dispatcher to avoid blocking the main thread.
     *
     * @param event The Event entity to delete
     */
    fun deleteEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.deleteEvent(event)
        }
    }

    /**
     * Retrieves events for a specific date and user.
     * Returns a Flow of event lists to support reactive programming.
     *
     * @param date The date to retrieve events for
     * @param userId The ID of the user whose events to retrieve
     * @return Flow<List<Event>> containing events matching the criteria
     */
    fun getEventsForDateAndUser(date: String, userId: Int): Flow<List<Event>> {
        return appRepository.getEventsForDateAndUser(date, userId)
    }

    /**
     * Retrieves all events for a specific user.
     * Returns a Flow of event lists to support reactive programming.
     *
     * @param userId The ID of the user whose events to retrieve
     * @return Flow<List<Event>> containing all events for the user
     */
    fun getAllEventsForUser(userId: Int): Flow<List<Event>> {
        return appRepository.getAllEventsForUser(userId)
    }

    /**
     * Retrieves the next upcoming event for a user, considering both date and time.
     *
     * @param userId The ID of the user whose next event to retrieve
     * @param currentDate The current date to compare against
     * @param currentTime The current time to compare against
     * @return The next Event or null if no events exist
     */
    suspend fun getNextEventForUserSpec(userId: Int, currentDate: String, currentTime: String): Event? {
        return appRepository.getNextEventForUserSpec(userId, currentDate, currentTime)
    }

    // ----- Note related methods -----

    /**
     * Inserts a new note record.
     * Runs on IO dispatcher to avoid blocking the main thread.
     *
     * @param note The Note entity to insert
     */
    fun insertNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.insertNote(note)
        }
    }

    /**
     * Deletes a specific note record.
     * Runs on IO dispatcher to avoid blocking the main thread.
     *
     * @param note The Note entity to delete
     */
    fun deleteNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.deleteNote(note)
        }
    }

    /**
     * Retrieves notes associated with a specific event.
     * Returns a Flow of note lists to support reactive programming.
     *
     * @param eventId The ID of the event to retrieve notes for
     * @return Flow<List<Note>> containing notes for the specified event
     */
    fun getNotesForEvent(eventId: Int): Flow<List<Note>> {
        return appRepository.getNotesForEvent(eventId)
    }
}