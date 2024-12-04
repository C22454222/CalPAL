package com.example.calpal

import android.Manifest // Import for accessing GPS permission constants
import android.annotation.SuppressLint // Suppress warnings for specific lint checks
import android.content.pm.PackageManager // Used to check permission status
import android.location.Geocoder // Converts GPS coordinates into human-readable addresses
import android.location.Location // Represents a geographical location
import android.location.LocationListener // Interface for receiving location updates
import android.location.LocationManager // Manages location updates from the device
import android.os.Build // Provides information about the OS version
import android.os.Bundle // Used to pass data between Android components
import android.view.LayoutInflater // Inflates layout XML into views
import android.view.View // Represents the UI components in Android
import android.view.ViewGroup // Parent class for UI components in a layout
import android.widget.* // UI components like TextView, Button, etc.
import androidx.annotation.RequiresApi // Annotation to enforce API-level constraints
import androidx.appcompat.app.AppCompatActivity // Base class for activities
import androidx.core.app.ActivityCompat // Utilities for requesting permissions
import androidx.core.content.ContextCompat // Utilities for accessing app resources
import androidx.fragment.app.Fragment // Base class for fragments
import androidx.fragment.app.activityViewModels // ViewModel sharing across fragments
import androidx.lifecycle.lifecycleScope // Lifecycle-aware coroutine scope
import com.example.calpal.data.entities.Event // Importing Event data class
import kotlinx.coroutines.Dispatchers // For dispatching coroutine contexts
import kotlinx.coroutines.launch // Coroutine builder
import kotlinx.coroutines.withContext // Switching coroutine contexts
import java.text.SimpleDateFormat // For date formatting
import java.time.LocalDate // For representing dates
import java.time.LocalTime // For representing time
import java.time.format.DateTimeFormatter // For formatting date/time
import java.util.* // Utilities like Locale

class HomeFragment : Fragment(), LocationListener {

    // UI components
    private lateinit var welcomeTextView: TextView
    private lateinit var nextEventContainer: LinearLayout
    private lateinit var mLocationText: TextView
    private lateinit var locality: TextView
    private lateinit var locationListView: ListView

    private val mainViewModel: MainViewModel by activityViewModels()
    private var locationManager: LocationManager? = null

    // Location update parameters
    private val minTime: Long = 500 // Minimum time interval for updates (500ms)
    private val minDistance: Float = 1f // Minimum distance for updates (1 meter)

    private val locationAddresses: ArrayList<String> = ArrayList()
    private lateinit var adapter: ArrayAdapter<String>

    companion object {
        private const val MY_PERMISSION_GPS = 1
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize UI components
        welcomeTextView = rootView.findViewById(R.id.welcome_text)
        nextEventContainer = rootView.findViewById(R.id.nextEventContainer)
        mLocationText = rootView.findViewById(R.id.location)
        locality = rootView.findViewById(R.id.locality)
        locationListView = rootView.findViewById(R.id.location_list)

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, locationAddresses)
        locationListView.adapter = adapter

        loadWelcomeMessage()
        loadNextEvent()
        setUpLocation()

        return rootView
    }

    // Load the welcome message asynchronously
    @SuppressLint("SetTextI18n")
    private fun loadWelcomeMessage() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val loggedInUser = mainViewModel.getLoggedInUser()
                val username = loggedInUser?.username ?: "User"

                withContext(Dispatchers.Main) {
                    welcomeTextView.text = "Welcome back $username"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    welcomeTextView.text = "Welcome back!"
                }
            }
        }
    }

    // Load the next event asynchronously
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadNextEvent() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val userId = mainViewModel.getLoggedInUser()?.id ?: return@launch

                val nextEvent = mainViewModel.getNextEventForUserSpec(userId, currentDate, currentTime)

                withContext(Dispatchers.Main) {
                    nextEventContainer.removeAllViews()
                    if (nextEvent != null) {
                        val normalizedDate = normalizeDate(nextEvent.date)
                        val normalizedTime = normalizeTime(nextEvent.time)
                        val normalizedEvent = nextEvent.copy(date = normalizedDate, time = normalizedTime)
                        nextEventContainer.addView(createEventCard(normalizedEvent))
                    } else {
                        nextEventContainer.addView(createNoEventsView())
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    nextEventContainer.removeAllViews()
                    nextEventContainer.addView(createErrorView())
                }
            }
        }
    }

    // Normalize the date string to a consistent format
    @RequiresApi(Build.VERSION_CODES.O)
    private fun normalizeDate(date: String): String {
        return try {
            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
            val parsedDate = LocalDate.parse(date, inputFormatter)
            parsedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } catch (e: Exception) {
            date
        }
    }

    // Normalize the time string to a consistent format
    @RequiresApi(Build.VERSION_CODES.O)
    private fun normalizeTime(time: String): String {
        return try {
            val inputFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
            val parsedTime = LocalTime.parse(time, inputFormatter)
            parsedTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            time
        }
    }

    // Set up GPS location tracking
    private fun setUpLocation() {
        locationManager = requireContext().getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager

        val sharedPreferences = requireContext().getSharedPreferences("AppPreferences", AppCompatActivity.MODE_PRIVATE)
        val locationPermissionAsked = sharedPreferences.getBoolean("LocationPermissionAsked", false)

        if (!locationPermissionAsked) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSION_GPS
                )
                sharedPreferences.edit().putBoolean("LocationPermissionAsked", true).apply()
            } else {
                startLocationUpdates()
            }
        } else {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startLocationUpdates()
            } else {
                Toast.makeText(requireContext(), "Location permission denied. Please enable it in settings.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Start receiving location updates
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        locationManager?.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            minTime,
            minDistance,
            this
        )
    }

    // Callback for when the location changes
    @SuppressLint("SetTextI18n")
    override fun onLocationChanged(location: Location) {
        val latestLocation = "Latitude: ${location.latitude}\nLongitude: ${location.longitude}"
        mLocationText.text = "GPS Location:\n$latestLocation"

        try {
            val geo = Geocoder(requireContext(), Locale.getDefault())
            val addressList = geo.getFromLocation(location.latitude, location.longitude, 1)

            val addressString = if (!addressList.isNullOrEmpty()) {
                val address = addressList[0]
                // Safely concatenate address fields
                listOfNotNull(
                    address.featureName,
                    address.thoroughfare,
                    address.locality,
                    address.adminArea,
                    address.countryName
                ).joinToString(", ")
            } else {
                "Lat: ${location.latitude}, Lng: ${location.longitude}"
            }

            locality.text = addressString
            updateLocationList(addressString)
        } catch (e: Exception) {
            e.printStackTrace()
            val fallbackAddress = "Lat: ${location.latitude}, Lng: ${location.longitude}"
            locality.text = fallbackAddress
            updateLocationList(fallbackAddress)
        }
    }


    // Update the location history list
    private fun updateLocationList(address: String) {
        locationAddresses.add(address)
        adapter.notifyDataSetChanged()
    }

    // Handle location permission result
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == MY_PERMISSION_GPS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                Toast.makeText(requireContext(), "Permission denied. Enable it in settings.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Create a card view for an event
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n", "InflateParams")
    private fun createEventCard(event: Event): View {
        val eventCard = LayoutInflater.from(context).inflate(R.layout.event_cards, null)

        val tvEventName = eventCard.findViewById<TextView>(R.id.tvEventName)
        val tvEventDate = eventCard.findViewById<TextView>(R.id.tvEventDate)
        val tvEventTime = eventCard.findViewById<TextView>(R.id.tvEventTime)
        val tvEventLocation = eventCard.findViewById<TextView>(R.id.tvEventLocation)
        val btnRemoveEvent = eventCard.findViewById<Button>(R.id.btnRemoveEvent)

        tvEventName.text = event.name
        tvEventDate.text = "Date: ${event.date}"
        tvEventTime.text = "Time: ${event.time}"
        tvEventLocation.text = "Location: ${event.location}"

        btnRemoveEvent.setOnClickListener {
            confirmRemoveEvent(event)
        }

        return eventCard
    }

    // Create a view for when no events are found
    @SuppressLint("SetTextI18n")
    private fun createNoEventsView(): View {
        return TextView(context).apply {
            text = "No upcoming events."
            textSize = 25f
        }
    }

    // Create a view for when an error occurs
    @SuppressLint("SetTextI18n")
    private fun createErrorView(): View {
        return TextView(context).apply {
            text = "Failed to load events. Please try again."
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
        }
    }

    // Prompt user to confirm event removal
    @RequiresApi(Build.VERSION_CODES.O)
    private fun confirmRemoveEvent(event: Event) {
        val dialogBuilder = android.app.AlertDialog.Builder(requireContext())
        dialogBuilder.setTitle("Remove Event")
        dialogBuilder.setMessage("Are you sure you want to remove this event?")
        dialogBuilder.setPositiveButton("Yes") { _, _ ->
            lifecycleScope.launch(Dispatchers.IO) {
                mainViewModel.deleteEvent(event)
                loadNextEvent()
            }
        }
        dialogBuilder.setNegativeButton("No", null)
        dialogBuilder.create().show()
    }
}