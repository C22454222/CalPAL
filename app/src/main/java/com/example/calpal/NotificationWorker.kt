package com.example.calpal

// Android and Kotlin standard library imports
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.calpal.data.database.AppDatabase
import com.example.calpal.data.entities.Event
import com.example.calpal.data.repository.AppRepository
import java.text.SimpleDateFormat
import java.util.*

/**
 * NotificationWorker is a background worker responsible for checking and sending
 * notifications for upcoming events within the next 24 hours.
 *
 * This worker is designed to run periodically to check user events and trigger
 * notifications for events that are approaching.
 */
class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    // Lazy initialization of AppRepository to manage data operations
    private val appRepository: AppRepository by lazy {
        val db = AppDatabase.getDatabase(context)
        AppRepository(
            db.userSignUpDao(),
            db.userLoginDao(),
            db.noteDao(),
            db.eventDao()
        )
    }

    /**
     * Main worker method that executes the background task.
     * Checks for upcoming events and sends notifications.
     *
     * @return Result of the worker's execution (success or failure)
     */
    override suspend fun doWork(): Result {
        return try {
            checkForUpcomingEvents()
            Result.success() // Worker completed successfully
        } catch (e: Exception) {
            Result.failure() // Worker failed due to an exception
        }
    }

    /**
     * Checks for events within the next 24 hours and sends notifications.
     * Only processes events for the currently logged-in user.
     */
    private suspend fun checkForUpcomingEvents() {
        val userId = getUserId()

        if (userId == -1) return // No logged-in user, exit early

        // Calculate time-related constants
        val currentTimeMillis = System.currentTimeMillis()
        val twentyFourHoursInMillis = 24 * 60 * 60 * 1000

        // Fetch all events for the logged-in user
        val eventsFlow = appRepository.getAllEventsForUser(userId)

        // Collect and process events
        eventsFlow.collect { events ->
            for (event in events) {
                // Parse event date and time
                val eventDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .parse("${event.date} ${event.time}")
                val eventMillis = eventDateTime?.time ?: 0

                // Check if event is within 24 hours and hasn't been notified
                if (eventMillis - currentTimeMillis in 0..twentyFourHoursInMillis && !event.isNotified) {
                    sendEventNotification(event)
                    markEventAsNotified(event)
                }
            }
        }
    }

    /**
     * Retrieves the current user's ID from SharedPreferences.
     *
     * @return User ID or -1 if no user is logged in
     */
    private fun getUserId(): Int {
        val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("userId", -1)
    }

    /**
     * Sends a notification for an upcoming event.
     *
     * @param event The event to create a notification for
     */
    private fun sendEventNotification(event: Event) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Build the notification with event details
        val notification = NotificationCompat.Builder(context, "event_notifications")
            .setContentTitle("Upcoming Event: ${event.name}")
            .setContentText("Event '${event.name}' starts at ${event.time} on the date ${event.date}.")
            .setSmallIcon(R.drawable.ic_event)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Send the notification
        notificationManager.notify(event.id.toInt(), notification)
    }

    /**
     * Marks an event as notified to prevent duplicate notifications.
     *
     * @param event The event to mark as notified
     */
    private suspend fun markEventAsNotified(event: Event) {
        // Update the event's notified status
        event.isNotified = true
        appRepository.updateEvent(event)
    }
}