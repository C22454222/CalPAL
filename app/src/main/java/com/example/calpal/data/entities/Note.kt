package com.example.calpal.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// The Note data class represents a note associated with an event, stored in the "notes" table.
@Entity(
    tableName = "notes",
    foreignKeys = [ForeignKey(
        entity = Event::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE // Deletes the note if the associated event is deleted
    )],
    indices = [Index(value = ["eventId"])] // Index the foreign key column for efficient lookups
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // Auto-generated unique identifier for the note.

    @ColumnInfo(name = "eventId")  // Custom column name for the foreign key to Event
    val eventId: Int, // Foreign key linking the note to an event.

    @ColumnInfo(name = "content")  // Custom column name for the note's content
    val content: String, // Text content of the note.

    @ColumnInfo(name = "createdAt")  // Custom column name for the timestamp when the note was created
    val createdAt: Long = System.currentTimeMillis() // Auto-set timestamp for when the note is created.
)