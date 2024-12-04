package com.example.calpal.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import com.example.calpal.data.entities.Note
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Note entity.
 * Manages database operations for note records.
 */
@Dao
interface NoteDao {

    /**
     * Inserts a new note into the database.
     * @param note Note entity to insert
     */
    @Insert
    suspend fun insert(note: Note)

    /**
     * Deletes a specific note from the database.
     * @param note Note entity to delete
     */
    @Delete
    suspend fun delete(note: Note)

    /**
     * Updates an existing note in the database.
     * @param note Note entity to update
     */
    @Update
    suspend fun update(note: Note)

    /**
     * Retrieves all notes for a specific event.
     * @param eventId ID of the event to fetch notes for
     * @return Flow of notes sorted by creation time
     */
    @Query("SELECT * FROM notes WHERE eventId = :eventId ORDER BY createdAt DESC")
    fun getNotesForEvent(eventId: Int): Flow<List<Note>>

    /**
     * Retrieves all notes for a specific user's events.
     * @param userId ID of the user to fetch notes for
     * @return Flow of notes sorted by creation time
     */
    @Query("SELECT * FROM notes WHERE eventId IN (SELECT id FROM events WHERE userId = :userId) ORDER BY createdAt DESC")
    fun getNotesForUser(userId: Int): Flow<List<Note>>

    /**
     * Retrieves a specific note by its ID.
     * @param noteId ID of the note to fetch
     * @return Note entity or null if not found
     */
    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    suspend fun getNoteById(noteId: Int): Note?
}