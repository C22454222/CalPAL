package com.example.calpal

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.calpal.data.entities.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

// Define a Fragment to display the profile with event functionality.
class ProfileFragment : Fragment() {

    // Get a reference to the shared ViewModel (used across multiple fragments).
    private val mainViewModel: MainViewModel by activityViewModels()

    // Store the selected date as a string.
    private var selectedDate: String = ""
    private lateinit var eventsContainer: LinearLayout

    @RequiresApi(Build.VERSION_CODES.TIRAMISU) // Ensure compatibility with newer APIs.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the fragment layout.
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Reference to the container for displaying events.
        eventsContainer = view.findViewById(R.id.eventsContainer)

        // Set up the calendar and add event button functionalities.
        setupCalendarView(view)
        setupAddEventButton(view)

        // Request notification permissions if necessary.
        requestNotificationPermissions()

        return view
    }

    // Retrieve the user ID stored in shared preferences.
    private fun getUserId(): Int {
        val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("userId", -1)
    }

    // Configure the CalendarView to handle date selections.
    private fun setupCalendarView(view: View) {
        val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // Update the selected date when a new date is chosen.
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth) // Calendar months are 0-indexed.
            selectedDate = formatDateToString(calendar.timeInMillis) // Format the date to a string.
            fetchEventsForDate(selectedDate) // Fetch events for the selected date.
        }
    }

    // Configure the Add Event button behavior.
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setupAddEventButton(view: View) {
        view.findViewById<Button>(R.id.btnAddEvent)?.setOnClickListener {
            val userId = getUserId()
            when {
                userId == -1 -> {
                    // Notify the user to log in if no user ID is found.
                    Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show()
                }
                selectedDate.isNotEmpty() -> {
                    // Show the dialog to add a new event.
                    showAddEventDialog(selectedDate)
                }
                else -> {
                    // Prompt the user to select a date if none is selected.
                    Toast.makeText(requireContext(), "Please select a date first", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Fetch events for the selected date and display them.
    private fun fetchEventsForDate(date: String) {
        val userId = getUserId()
        if (userId == -1) {
            // Prompt the user to log in if not authenticated.
            Toast.makeText(requireContext(), "Please log in to view events", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Get events from the ViewModel and sort them.
                mainViewModel.getEventsForDateAndUser(date, userId).collectLatest { events ->
                    val sortedEvents = events.sortedBy { event ->
                        val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            .parse("${event.date} ${event.time}")?.time ?: 0L
                        dateTime
                    }
                    withContext(Dispatchers.Main) {
                        displayEvents(sortedEvents) // Display sorted events.
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error fetching events", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error fetching events: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Display the list of events or show a placeholder if no events exist.
    @SuppressLint("SetTextI18n")
    private fun displayEvents(events: List<Event>) {
        eventsContainer.removeAllViews() // Clear the container.

        if (events.isEmpty()) {
            // Show a message if there are no events for the date.
            val noEventsText = TextView(context).apply {
                text = "No events for this date"
                textSize = 25f
                setPadding(16, 16, 16, 16)
            }
            eventsContainer.addView(noEventsText)
        } else {
            // Add a card for each event to the container.
            events.forEach { event ->
                val eventCard = createEventCard(event)
                eventsContainer.addView(eventCard)
            }
        }
    }

    // Create and populate a card layout for an event.
    @SuppressLint("SetTextI18n", "MissingInflatedId", "InflateParams")
    private fun createEventCard(event: Event): View {
        val eventCard = LayoutInflater.from(context).inflate(R.layout.event_cards, null)

        // Populate the card with event details.
        val tvEventName = eventCard.findViewById<TextView>(R.id.tvEventName)
        val tvEventDate = eventCard.findViewById<TextView>(R.id.tvEventDate)
        val tvEventTime = eventCard.findViewById<TextView>(R.id.tvEventTime)
        val tvEventLocation = eventCard.findViewById<TextView>(R.id.tvEventLocation)
        val removeButton = eventCard.findViewById<Button>(R.id.btnRemoveEvent)

        tvEventName.text = event.name
        tvEventDate.text = "Date: ${event.date}"
        tvEventTime.text = "Time: ${event.time}"
        tvEventLocation.text = "Location: ${event.location}"

        // Configure the remove button for the event.
        removeButton.setOnClickListener {
            val userId = getUserId()
            if (userId == -1) {
                Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show()
            } else {
                confirmRemoveEvent(event)
            }
        }

        return eventCard
    }

    // Show a confirmation dialog before deleting an event.
    private fun confirmRemoveEvent(event: Event) {
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to remove the event '${event.name}'?")
            .setPositiveButton("Yes") { _, _ -> removeEvent(event) }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    // Delete the event from the database.
    private fun removeEvent(event: Event) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                mainViewModel.deleteEvent(event)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Event removed successfully", Toast.LENGTH_SHORT).show()
                    fetchEventsForDate(selectedDate) // Refresh the event list.
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error removing event", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error removing event: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Convert a timestamp to a formatted date string.
    private fun formatDateToString(millis: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
    }

    // Display the dialog to add a new event.
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun showAddEventDialog(date: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_event, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Add Event")
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val eventNameEditText = dialogView.findViewById<EditText>(R.id.eventName)
        val eventTimeEditText = dialogView.findViewById<EditText>(R.id.eventTime)
        val eventLocationEditText = dialogView.findViewById<EditText>(R.id.eventLocation)
        val saveEventButton = dialogView.findViewById<Button>(R.id.saveEvent)

        saveEventButton.setOnClickListener {
            val eventName = eventNameEditText?.text?.toString()?.trim() ?: ""
            val eventTime = eventTimeEditText?.text?.toString()?.trim() ?: ""
            val eventLocation = eventLocationEditText?.text?.toString()?.trim() ?: ""

            // Ensure all fields are filled and the time format is valid.
            when {
                eventName.isEmpty() || eventTime.isEmpty() || eventLocation.isEmpty() -> {
                    Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
                !isValidTimeFormat(eventTime) -> {
                    Toast.makeText(requireContext(), "Invalid time format. Please use HH:mm (24-hour format).", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val userId = getUserId()
                    if (userId == -1) {
                        Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        saveEvent(Event(name = eventName, time = eventTime, location = eventLocation, date = date, userId = userId))
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    // Validate if a time string matches HH:mm format.
    private fun isValidTimeFormat(time: String): Boolean {
        val timePattern = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$"
        return time.matches(timePattern.toRegex())
    }

    // Save the event to the database.
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun saveEvent(event: Event) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val conflictingEvent = mainViewModel.getNextEventForUserSpec(event.userId, event.date, event.time)

                if (conflictingEvent != null && conflictingEvent.date == event.date && conflictingEvent.time == event.time) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "An event is already scheduled at this time.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // Insert the new event.
                mainViewModel.insertEvent(event)

                withContext(Dispatchers.Main) {
                    fetchEventsForDate(selectedDate)
                    Toast.makeText(requireContext(), "Event added successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error saving event", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error saving event: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Request notification permissions from the user.
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermissions() {
        val sharedPrefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val hasRequestedPermission = sharedPrefs.getBoolean("notifications_permission_requested", false)

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                Log.d("debug", "Notification permission already granted")
            }
            !hasRequestedPermission -> {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                sharedPrefs.edit().putBoolean("notifications_permission_requested", true).apply()
            }
            else -> {
                Log.d("debug", "Notification permission previously requested")
            }
        }
    }

    // Handle the result of the permission request.
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("debug", "Notification permission granted")
        } else {
            Log.d("debug", "Notification permission denied")
        }
    }
}
