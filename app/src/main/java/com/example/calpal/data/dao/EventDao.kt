package com.example.calpal.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.calpal.data.entities.Event
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Event entity.
 * Manages database operations for event records.
 */
@Dao
interface EventDao {

    /**
     * Inserts a new event into the database.
     * @param event Event entity to insert
     */
    @Insert
    suspend fun insert(event: Event)

    /**
     * Deletes a specific event from the database.
     * @param event Event entity to delete
     */
    @Delete
    suspend fun delete(event: Event)

    /**
     * Updates an existing event in the database.
     * @param event Event entity to update
     */
    @Update
    suspend fun updateEvent(event: Event)

    /**
     * Retrieves events for a specific user and date.
     * @param date Date to fetch events for
     * @param userId ID of the user to fetch events for
     * @return Flow of events sorted by time
     */
    @Query("SELECT * FROM events WHERE date = :date AND userId = :userId ORDER BY time ASC")
    fun getEventsForDateAndUser(date: String, userId: Int): Flow<List<Event>>

    /**
     * Retrieves the next event for a user from a given date.
     * @param currentDate Starting date to search from
     * @param userId ID of the user to fetch next event for
     * @return Next Event or null if no events exist
     */
    @Query("SELECT * FROM events WHERE userId = :userId AND date >= :currentDate ORDER BY date, time LIMIT 1")
    suspend fun getNextEventForUser(currentDate: String, userId: Int): Event?

    /**
     * Retrieves events for a specific date.
     * @param date Date to fetch events for
     * @return Flow of events sorted by time
     */
    @Query("SELECT * FROM events WHERE date = :date ORDER BY time ASC")
    fun getEventsForDate(date: String): Flow<List<Event>>

    /**
     * Retrieves all events for a specific user.
     * @param userId ID of the user to fetch events for
     * @return Flow of all user events
     */
    @Query("SELECT * FROM events WHERE userId = :userId")
    fun getAllEventsForUser(userId: Int): Flow<List<Event>>

    /**
     * Retrieves events for a user on a specific date and time.
     * @param userId ID of the user
     * @param date Date to fetch events for
     * @param time Time to start searching from
     * @return Flow of events matching criteria
     */
    @Query("SELECT * FROM events WHERE userId = :userId AND date = :date AND time >= :time ORDER BY time ASC")
    fun getEventsByUserIdDateAndTime(userId: Int, date: String, time: String): Flow<List<Event>>

    /**
     * Retrieves the next specific event for a user from current date and time.
     * @param userId ID of the user
     * @param currentDate Current date
     * @param currentTime Current time
     * @return Next Event or null if no events exist
     */
    @Query("""
    SELECT * FROM events 
    WHERE userId = :userId 
    AND (
        date > :currentDate OR 
        (date = :currentDate AND time >= :currentTime)
    ) 
    ORDER BY date ASC, time ASC 
    LIMIT 1
""")
    suspend fun getNextEventForUserSpec(userId: Int, currentDate: String, currentTime: String): Event?
}