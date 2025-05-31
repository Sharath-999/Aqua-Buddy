package keyur.diwan.project.waterReminder.fragments

// Import Activity to handle activity result from ringtone picker
import android.app.Activity
// Import TimePickerDialog to let the user select wake-up and sleep times
import android.app.TimePickerDialog
// Import Context to access app resources and services
import android.content.Context
// Import Intent to launch the ringtone picker activity
import android.content.Intent
// Import SharedPreferences to store and retrieve user settings
import android.content.SharedPreferences
// Import RingtoneManager to manage notification ringtones
import android.media.RingtoneManager
// Import Uri to handle ringtone URIs
import android.net.Uri
// Import Bundle to handle saved instance state in onCreateView
import android.os.Bundle
// Import TextUtils to check for empty input fields
import android.text.TextUtils
// Import Log for debugging (e.g., logging selected ringtone URI)
import android.util.Log
// Import LayoutInflater, View, and ViewGroup for fragment view creation
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
// Import Toast to show short messages to the user
import android.widget.Toast
// Import BottomSheetDialogFragment as the base class for this fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
// Import View Binding class for this fragment, auto-generated from bottom_sheet_fragment.xml
import io.github.z3r0c00l_2k.aquadroid.databinding.BottomSheetFragmentBinding
// Import MainActivity to update its values after settings are saved
import keyur.diwan.project.waterReminder.MainActivity
// Import AlarmHelper to schedule/cancel alarms after updating settings
import keyur.diwan.project.waterReminder.helpers.AlarmHelper
// Import SqliteHelper to update the total intake in the database
import keyur.diwan.project.waterReminder.helpers.SqliteHelper
// Import AppUtils for utility functions (e.g., calculateIntake) and SharedPreferences keys
import keyur.diwan.project.waterReminder.utils.AppUtils
// Import DecimalFormat and RoundingMode to format the calculated total intake
import java.math.RoundingMode
import java.text.DecimalFormat
// Import Calendar and Date for time calculations
import java.util.*

// BottomSheetFragment is a dialog fragment that allows the user to update their settings
class BottomSheetFragment(private val mCtx: Context) : BottomSheetDialogFragment() {

    // Nullable View Binding instance for the fragment, auto-generated from bottom_sheet_fragment.xml
    private var _binding: BottomSheetFragmentBinding? = null
    // Non-null accessor for the binding, throws an exception if accessed after onDestroyView
    private val binding get() = _binding!!

    // SharedPreferences instance to store and retrieve user settings
    private lateinit var sharedPref: SharedPreferences
    // String to store the user’s weight input (in kg)
    private var weight = ""
    // String to store the user’s workout time input (in minutes)
    private var workTime = ""
    // String to store the user’s custom water intake target (in ml)
    private var customTarget = ""
    // Long to store the wake-up time timestamp (in milliseconds)
    private var wakeupTime: Long = 0
    // Long to store the sleep time timestamp (in milliseconds)
    private var sleepingTime: Long = 0
    // String to store the notification message
    private var notificMsg = ""
    // Int to store the notification frequency (in minutes)
    private var notificFrequency = 0
    // String to store the current notification ringtone URI
    private var currentToneUri: String? = ""

    // onCreateView is called to create the fragment’s view
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for the fragment using View Binding (bottom_sheet_fragment.xml)
        _binding = BottomSheetFragmentBinding.inflate(inflater, container, false)
        // Return the root view of the fragment
        return binding.root
    }

    // onViewCreated is called after the view is created, used to set up UI elements and listeners
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Call the parent class’s onViewCreated method
        super.onViewCreated(view, savedInstanceState)

        // Check if the device uses 24-hour time format for the TimePickerDialog
        val is24h = android.text.format.DateFormat.is24HourFormat(mCtx)
        // Initialize SharedPreferences to access user settings
        sharedPref = mCtx.getSharedPreferences(AppUtils.USERS_SHARED_PREF, AppUtils.PRIVATE_MODE)

        // Populate the weight input field with the stored value from SharedPreferences
        binding.etWeight.editText!!.setText(sharedPref.getInt(AppUtils.WEIGHT_KEY, 0).toString())
        // Populate the workout time input field with the stored value from SharedPreferences
        binding.etWorkTime.editText!!.setText(sharedPref.getInt(AppUtils.WORK_TIME_KEY, 0).toString())
        // Populate the custom target input field with the stored total intake from SharedPreferences
        binding.etTarget.editText!!.setText(sharedPref.getInt(AppUtils.TOTAL_INTAKE, 0).toString())
        // Populate the notification message input field with the stored value (default message if unset)
        binding.etNotificationText.editText!!.setText(
            sharedPref.getString(
                AppUtils.NOTIFICATION_MSG_KEY,
                "Hey... It's time to  drink some water...."
            )
        )

        // Retrieve the current notification ringtone URI from SharedPreferences (default: system notification sound)
        currentToneUri = sharedPref.getString(
            AppUtils.NOTIFICATION_TONE_URI_KEY,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString()
        )

        // Display the title of the current ringtone in the ringtone input field
        binding.etRingtone.editText!!.setText(
            RingtoneManager.getRingtone(mCtx, Uri.parse(currentToneUri)).getTitle(mCtx)
        )

        // Set a listener for the notification frequency radio group (custom view with positions 0-2)
        binding.radioNotificItervel.setOnClickedButtonListener { _, position ->
            // Set the notification frequency based on the selected position
            notificFrequency = when (position) {
                0 -> 1   // 1 minute
                1 -> 45  // 45 minutes
                2 -> 60  // 60 minutes
                else -> 30 // Default: 30 minutes
            }
        }

        // Retrieve the stored notification frequency (default: 30 minutes)
        notificFrequency = sharedPref.getInt(AppUtils.NOTIFICATION_FREQUENCY_KEY, 30)
        // Set the radio group position based on the stored frequency
        binding.radioNotificItervel.position = when (notificFrequency) {
            1 -> 0   // 1 minute
            45 -> 1  // 45 minutes
            60 -> 2  // 60 minutes
            else -> 0 // Default position (1 minute)
        }

        // Set a click listener for the ringtone input field to launch the ringtone picker
        binding.etRingtone.editText!!.setOnClickListener {
            // Create an Intent to launch the system ringtone picker
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            // Set the ringtone type to notification
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            // Set the title of the ringtone picker dialog
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select ringtone for notifications:")
            // Disable the "Silent" option in the picker
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            // Enable the default notification sound option in the picker
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            // Pass the current ringtone URI to pre-select it in the picker
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentToneUri)
            // Start the ringtone picker activity and expect a result with request code 999
            startActivityForResult(intent, 999)
        }

        // Initialize a Calendar instance to handle time formatting
        val cal = Calendar.getInstance()
        // Retrieve the wake-up time from SharedPreferences (default: a specific timestamp)
        wakeupTime = sharedPref.getLong(AppUtils.WAKEUP_TIME, 1558323000000)
        // Set the Calendar to the wake-up time
        cal.timeInMillis = wakeupTime
        // Display the wake-up time in the input field (format: HH:mm)
        binding.etWakeUpTime.editText!!.setText(String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)))

        // Retrieve the sleep time from SharedPreferences (default: a specific timestamp)
        sleepingTime = sharedPref.getLong(AppUtils.SLEEPING_TIME_KEY, 1558369800000)
        // Set the Calendar to the sleep time
        cal.timeInMillis = sleepingTime
        // Display the sleep time in the input field (format: HH:mm)
        binding.etSleepTime.editText!!.setText(String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)))

        // Set a click listener for the wake-up time input field to show a TimePickerDialog
        binding.etWakeUpTime.editText!!.setOnClickListener {
            // Create a Calendar instance with the current wake-up time
            val calendar = Calendar.getInstance().apply { timeInMillis = wakeupTime }
            // Show a TimePickerDialog to let the user select a wake-up time
            TimePickerDialog(mCtx, { _, hour, minute ->
                // Create a Calendar instance to store the selected time
                val time = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }
                // Update the wake-up time with the selected time
                wakeupTime = time.timeInMillis
                // Display the selected time in the input field (format: HH:mm)
                binding.etWakeUpTime.editText!!.setText(String.format("%02d:%02d", hour, minute))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), is24h).apply {
                // Set the title of the dialog
                setTitle("Select Wakeup Time")
                // Show the dialog
                show()
            }
        }

        // Set a click listener for the sleep time input field to show a TimePickerDialog
        binding.etSleepTime.editText!!.setOnClickListener {
            // Create a Calendar instance with the current sleep time
            val calendar = Calendar.getInstance().apply { timeInMillis = sleepingTime }
            // Show a TimePickerDialog to let the user select a sleep time
            TimePickerDialog(mCtx, { _, hour, minute ->
                // Create a Calendar instance to store the selected time
                val time = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }
                // Update the sleep time with the selected time
                sleepingTime = time.timeInMillis
                // Display the selected time in the input field (format: HH:mm)
                binding.etSleepTime.editText!!.setText(String.format("%02d:%02d", hour, minute))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), is24h).apply {
                // Set the title of the dialog
                setTitle("Select Sleeping Time")
                // Show the dialog
                show()
            }
        }

        // Set a click listener for the "Update" button to save the updated settings
        binding.btnUpdate.setOnClickListener {
            // Retrieve the current total intake from SharedPreferences
            val currentTarget = sharedPref.getInt(AppUtils.TOTAL_INTAKE, 0)
            // Get the user’s weight input
            weight = binding.etWeight.editText!!.text.toString()
            // Get the user’s workout time input
            workTime = binding.etWorkTime.editText!!.text.toString()
            // Get the notification message input
            notificMsg = binding.etNotificationText.editText!!.text.toString()
            // Get the custom target input
            customTarget = binding.etTarget.editText!!.text.toString()

            // Validate the user’s input
            when {
                // Check if the notification message is empty
                TextUtils.isEmpty(notificMsg) -> toast("Please enter a notification message")
                // Check if a notification frequency has been selected
                notificFrequency == 0 -> toast("Please select a notification frequency")
                // Check if the weight field is empty
                TextUtils.isEmpty(weight) -> toast("Please input your weight")
                // Check if the weight is within a valid range (20-200 kg)
                weight.toInt() !in 20..200 -> toast("Please input a valid weight")
                // Check if the workout time field is empty
                TextUtils.isEmpty(workTime) -> toast("Please input your workout time")
                // Check if the workout time is within a valid range (0-500 minutes)
                workTime.toInt() !in 0..500 -> toast("Please input a valid workout time")
                // Check if the custom target field is empty
                TextUtils.isEmpty(customTarget) -> toast("Please input your custom target")
                else -> {
                    // Validate that the sleep time is after the wake-up time
                    val wakeupCal = Calendar.getInstance().apply { timeInMillis = wakeupTime }
                    val sleepCal = Calendar.getInstance().apply { timeInMillis = sleepingTime }

                    // Adjust for sleep time on the next day if it’s before wake-up time
                    if (sleepCal.before(wakeupCal)) {
                        sleepCal.add(Calendar.DAY_OF_MONTH, 1)
                    }

                    // Recalculate timestamps after adjustment
                    wakeupTime = wakeupCal.timeInMillis
                    sleepingTime = sleepCal.timeInMillis

                    // Ensure sleep time is still after wake-up time
                    if (sleepingTime <= wakeupTime) {
                        toast("Sleep time must be after wake-up time")
                        return@setOnClickListener
                    }

                    // Create a SharedPreferences editor to save the updated settings
                    val editor = sharedPref.edit()
                    // Save the weight (in kg)
                    editor.putInt(AppUtils.WEIGHT_KEY, weight.toInt())
                    // Save the workout time (in minutes)
                    editor.putInt(AppUtils.WORK_TIME_KEY, workTime.toInt())
                    // Save the wake-up time (in milliseconds)
                    editor.putLong(AppUtils.WAKEUP_TIME, wakeupTime)
                    // Save the sleep time (in milliseconds)
                    editor.putLong(AppUtils.SLEEPING_TIME_KEY, sleepingTime)
                    // Save the notification message
                    editor.putString(AppUtils.NOTIFICATION_MSG_KEY, notificMsg)
                    // Save the notification frequency (in minutes)
                    editor.putInt(AppUtils.NOTIFICATION_FREQUENCY_KEY, notificFrequency)

                    // Create an instance of SqliteHelper to update the database
                    val sqliteHelper = SqliteHelper(mCtx)

                    // Update the total intake based on user input
                    if (currentTarget != customTarget.toInt()) {
                        // If the custom target has changed, use the custom target
                        editor.putInt(AppUtils.TOTAL_INTAKE, customTarget.toInt())
                        // Update the total intake in the database for the current date
                        sqliteHelper.updateTotalIntake(AppUtils.getCurrentDate()!!, customTarget.toInt())
                        // Calls SqliteHelper.kt to update the totalIntake column
                    } else {
                        // Otherwise, recalculate the total intake based on weight and workout time
                        val totalIntake = AppUtils.calculateIntake(weight.toInt(), workTime.toInt())
                        // Format the total intake to a whole number (ceiling)
                        val df = DecimalFormat("#").apply { roundingMode = RoundingMode.CEILING }
                        val intake = df.format(totalIntake).toInt()
                        // Save the calculated total intake
                        editor.putInt(AppUtils.TOTAL_INTAKE, intake)
                        // Update the total intake in the database for the current date
                        sqliteHelper.updateTotalIntake(AppUtils.getCurrentDate()!!, intake)
                        // Calls SqliteHelper.kt to update the totalIntake column
                    }

                    // Apply the changes to SharedPreferences
                    editor.apply()
                    // Show a toast message to confirm the update
                    toast("Values updated successfully")

                    // Reschedule the alarm with the updated notification frequency
                    AlarmHelper().apply {
                        // Cancel any existing alarm to avoid duplicates
                        cancelAlarm(mCtx)
                        // Schedule a new alarm with the updated frequency
                        setAlarm(mCtx, sharedPref.getInt(AppUtils.NOTIFICATION_FREQUENCY_KEY, 30).toLong())
                        // Calls AlarmHelper.kt to reschedule the alarm
                    }

                    // Dismiss the bottom sheet dialog
                    dismiss()
                    // Update the UI in MainActivity by calling its updateValues method
                    (activity as? MainActivity)?.updateValues()
                }
            }
        }
    }

    // onActivityResult is called when the ringtone picker activity returns a result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Check if the result is OK and the request code matches the ringtone picker (999)
        if (resultCode == Activity.RESULT_OK && requestCode == 999) {
            // Retrieve the selected ringtone URI from the intent
            val uri = data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            // Update the current ringtone URI
            currentToneUri = uri?.toString()
            // Log the selected ringtone URI for debugging
            Log.d("BottomSheetFragment", "Selected ringtone URI: $currentToneUri")
            // If a URI was selected, save it to SharedPreferences and update the UI
            currentToneUri?.let {
                // Save the selected ringtone URI to SharedPreferences
                sharedPref.edit().putString(AppUtils.NOTIFICATION_TONE_URI_KEY, it).apply()
                // Get the ringtone title to display in the input field
                val ringtone = RingtoneManager.getRingtone(mCtx, uri)
                // Update the ringtone input field with the selected ringtone’s title
                binding.etRingtone.editText!!.setText(ringtone.getTitle(mCtx))
            }
        }
    }

    // onDestroyView is called when the fragment’s view is destroyed
    override fun onDestroyView() {
        // Call the parent class’s onDestroyView method
        super.onDestroyView()
        // Set the binding to null to prevent memory leaks
        _binding = null
    }

    // Helper function to show a short toast message
    private fun toast(msg: String) {
        // Show a toast message with the given text
        Toast.makeText(mCtx, msg, Toast.LENGTH_SHORT).show()
    }
}