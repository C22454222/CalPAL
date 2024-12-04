package com.example.calpal.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// The Event data class represents an event and is stored in the "events" table.
@Entity(
    tableName = "events",
    foreignKeys = [
        // Foreign key relationship with UserLogin to link events to users
        ForeignKey(
            entity = UserLogin::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE // Deletes the event if the associated user login is deleted
        )
    ],
    indices = [Index(value = ["userId"])] // Index the foreign key column for efficient lookups
)
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Auto-generated unique identifier for the event.

    @ColumnInfo(name = "name")
    var name: String, // Name of the event.

    @ColumnInfo(name = "time")
    var time: String, // Event time, stored as a string in HH:mm:ss format.

    @ColumnInfo(name = "location")
    var location: String, // Event location.

    @ColumnInfo(name = "date")
    val date: String, // Event date, stored as a string.

    @ColumnInfo(name = "userId")
    val userId: Int, // Foreign key linking the event to the user who created it.

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(), // Auto-set timestamp for when the event was created.

    @ColumnInfo(name = "isNotified")
    var isNotified: Boolean = false // Flag to indicate if the event has been notified.
)
