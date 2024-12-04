package com.example.calpal

// Android and Kotlin standard library imports
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
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
import com.example.calpal.data.entities.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * NotesFragment is responsible for managing events and notes in the CalPal app.
 * It handles displaying events, adding notes to events, and removing events/notes.
 */
class NotesFragment : Fragment() {

    // UI components for displaying events and notes
    private lateinit var eventContainer: LinearLayout
    private lateinit var notesContainer: LinearLayout
    private lateinit var addNoteButton: Button
    private lateinit var notesSectionTitle: TextView

    // ViewModel for managing data operations
    private val mainViewModel: MainViewModel by activityViewModels()

    // Currently selected event ID for note operations
    private var selectedEventId: Int = 0

    // List to store events for the current user
    private val eventList = mutableListOf<Event>()

    /**
     * Creates and returns the view hierarchy for the fragment.
     * Initializes UI components and sets up event listeners.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the fragment's layout
        val view = inflater.inflate(R.layout.fragment_notes, container, false)

        // Initialize UI components from the inflated view
        eventContainer = view.findViewById(R.id.event_container)
        notesContainer = view.findViewById(R.id.notesContainer)
        addNoteButton = view.findViewById(R.id.add_note_button)
        notesSectionTitle = view.findViewById(R.id.notesSectionTitle)

        // Initially hide notes-related views until an event is selected
        notesSectionTitle.visibility = View.GONE
        notesContainer.visibility = View.GONE

        // Set up listener for adding a new note
        addNoteButton.setOnClickListener { showAddNoteDialog() }

        // Load user's events
        loadUserEvents()

        return view
    }

    /**
     * Retrieves the current user's ID from SharedPreferences.
     * @return User ID or -1 if not found
     */
    private fun getUserId(): Int {
        val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("userId", -1).also {
            Log.d("NotesFragment", "Retrieved User ID: $it")
        }
    }

    /**
     * Loads all events for the current user asynchronously.
     * Sorts events chronologically and displays them in the event container.
     */
    private fun loadUserEvents() {
        lifecycleScope.launch(Dispatchers.IO) {
            val userId = getUserId()
            if (userId == -1) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Please log in to view events", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            Log.d("NotesFragment", "Fetching all events for user ID: $userId")
            try {
                // Collect events from ViewModel and update UI
                mainViewModel.getAllEventsForUser(userId).collectLatest { events ->
                    withContext(Dispatchers.Main) {
                        eventList.clear()
                        // Sort events by date and time
                        eventList.addAll(events.sortedBy {
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                .parse("${it.date} ${it.time}")
                        })
                        displayEvents(eventList)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to load events: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Displays events in the event container.
     * Shows a message if no events are available.
     */
    @SuppressLint("SetTextI18n", "InflateParams")
    private fun displayEvents(events: List<Event>) {
        eventContainer.removeAllViews()

        if (events.isEmpty()) {
            // Display message if no events
            eventContainer.addView(TextView(context).apply {
                text = "No events available"
                textSize = 25f
                setPadding(16, 16, 16, 16)
            })
        } else {
            // Create and add event cards for each event
            events.forEach { event -> eventContainer.addView(createEventCard(event)) }
        }
    }

    /**
     * Creates a custom view for an event with details and interaction buttons.
     * @param event The event to display
     * @return A view representing the event card
     */
    @SuppressLint("InflateParams", "SetTextI18n")
    private fun createEventCard(event: Event): View {
        val eventCard = LayoutInflater.from(context).inflate(R.layout.event_cards, null)

        with(eventCard) {
            // Populate event details
            findViewById<TextView>(R.id.tvEventName).text = event.name
            findViewById<TextView>(R.id.tvEventDate).text = "Date: ${event.date}"
            findViewById<TextView>(R.id.tvEventTime).text = "Time: ${event.time}"
            findViewById<TextView>(R.id.tvEventLocation).text = "Location: ${event.location}"

            // Set up event removal button
            findViewById<Button>(R.id.btnRemoveEvent).setOnClickListener { confirmRemoveEvent(event) }

            // Set up event selection to load notes
            setOnClickListener {
                selectedEventId = event.id.toInt()
                loadNotesForEvent(selectedEventId)
            }
        }

        return eventCard
    }

    /**
     * Loads notes for a specific event asynchronously.
     * @param eventId The ID of the event to load notes for
     */
    private fun loadNotesForEvent(eventId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Collect and display notes for the event
                mainViewModel.getNotesForEvent(eventId).collectLatest { notes ->
                    withContext(Dispatchers.Main) {
                        displayNotes(notes)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to load notes", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Displays notes for a selected event.
     * Shows a message if no notes are available.
     */
    @SuppressLint("SetTextI18n")
    private fun displayNotes(notes: List<Note>) {
        notesContainer.removeAllViews()
        if (notes.isEmpty()) {
            // Display message if no notes
            notesContainer.addView(TextView(context).apply {
                text = "No notes available for this event."
                textSize = 25f
                setPadding(16, 16, 16, 16)
            })
        } else {
            // Create and add note cards for each note
            notes.forEach { note ->
                val noteCard = createNoteCard(note)
                notesContainer.addView(noteCard)
            }
        }

        // Make notes-related views visible
        notesSectionTitle.visibility = View.VISIBLE
        notesContainer.visibility = View.VISIBLE
        addNoteButton.visibility = View.VISIBLE
    }

    /**
     * Creates a custom view for a note with content and removal button.
     * @param note The note to display
     * @return A view representing the note card
     */
    @SuppressLint("InflateParams")
    private fun createNoteCard(note: Note): View {
        val noteCard = LayoutInflater.from(context).inflate(R.layout.note_cards, null)

        val tvNoteContent = noteCard.findViewById<TextView>(R.id.tvNoteContent)
        val btnRemoveNote = noteCard.findViewById<Button>(R.id.btnRemoveNote)

        // Set the note content
        tvNoteContent.text = note.content

        // Handle note removal
        btnRemoveNote.setOnClickListener {
            removeNoteFromEvent(note)
        }

        return noteCard
    }

    /**
     * Removes a specific note from an event asynchronously.
     * @param note The note to be removed
     */
    private fun removeNoteFromEvent(note: Note) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Delete note through ViewModel
                mainViewModel.deleteNote(note)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Note removed successfully", Toast.LENGTH_SHORT).show()
                    loadNotesForEvent(selectedEventId)  // Refresh notes list
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error removing note", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Shows a dialog for adding a new note to the selected event.
     */
    private fun showAddNoteDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_note, null)
        val noteEditText = dialogView.findViewById<EditText>(R.id.note_content_edit_text)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Note")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val noteContent = noteEditText.text.toString().trim()
                if (noteContent.isNotEmpty()) {
                    addNoteToEvent(noteContent)
                } else {
                    Toast.makeText(requireContext(), "Please enter some text", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    /**
     * Adds a new note to the selected event asynchronously.
     * @param noteContent The content of the note to be added
     */
    private fun addNoteToEvent(noteContent: String) {
        if (selectedEventId == 0) {
            Toast.makeText(requireContext(), "Please select an event", Toast.LENGTH_SHORT).show()
            return
        }

        val newNote = Note(eventId = selectedEventId, content = noteContent)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Insert note through ViewModel
                mainViewModel.insertNote(newNote)
                loadNotesForEvent(selectedEventId)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error saving note", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Shows a confirmation dialog before removing an event.
     * @param event The event to be removed
     */
    private fun confirmRemoveEvent(event: Event) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to remove the event '${event.name}'?")
            .setPositiveButton("Yes") { _, _ -> removeEvent(event) }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    /**
     * Removes a specific event asynchronously.
     * @param event The event to be removed
     */
    private fun removeEvent(event: Event) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Delete event through ViewModel
                mainViewModel.deleteEvent(event)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Event removed successfully", Toast.LENGTH_SHORT).show()
                    loadUserEvents()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error removing event: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
