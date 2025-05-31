package keyur.diwan.project.waterReminder

// Import TimePickerDialog to let the user select wake-up and sleep times
import android.app.TimePickerDialog
// Import Context to access system services (e.g., InputMethodManager)
import android.content.Context
// Import Intent to launch MainActivity after user setup
import android.content.Intent
// Import SharedPreferences to store user settings (e.g., weight, wake-up time)
import android.content.SharedPreferences
// Import Build to check the Android API version for status bar customization
import android.os.Build
// Import Bundle to handle saved instance state in onCreate
import android.os.Bundle
// Import Handler to delay actions (e.g., double back press to exit)
import android.os.Handler
// Import TextUtils to check for empty input fields
import android.text.TextUtils
// Import View for Snackbar and input method handling
import android.view.View
// Import InputMethodManager to hide the keyboard
import android.view.inputmethod.InputMethodManager
// Import AppCompatActivity as the base class for this activity
import androidx.appcompat.app.AppCompatActivity
// Import Snackbar to show messages (e.g., validation errors)
import com.google.android.material.snackbar.Snackbar
// Import View Binding class for this activity, auto-generated from activity_init_user_info.xml
import io.github.z3r0c00l_2k.aquadroid.databinding.ActivityInitUserInfoBinding
// Import AppUtils for utility functions (e.g., calculateIntake) and SharedPreferences keys
import keyur.diwan.project.waterReminder.utils.AppUtils
// Import DecimalFormat and RoundingMode to format the calculated total intake
import java.math.RoundingMode
import java.text.DecimalFormat
// Import Calendar and Date for time calculations
import java.util.*

// InitUserInfoActivity collects user information to calculate their daily water intake goal
class InitUserInfoActivity : AppCompatActivity() {

    // View Binding instance to access UI elements (e.g., etWeight, btnContinue) from activity_init_user_info.xml
    private lateinit var binding: ActivityInitUserInfoBinding
    // SharedPreferences instance to store user settings (e.g., weight, wake-up time)
    private lateinit var sharedPref: SharedPreferences

    // String to store the user’s weight input (in kg)
    private var weight: String = ""
    // String to store the user’s workout time input (in minutes)
    private var workTime: String = ""
    // Long to store the wake-up time timestamp (in milliseconds)
    private var wakeupTime: Long = 0
    // Long to store the sleep time timestamp (in milliseconds)
    private var sleepingTime: Long = 0
    // Boolean to track double back press for exiting the activity
    private var doubleBackToExitPressedOnce = false

    // onCreate is called when the activity is first created
    override fun onCreate(savedInstanceState: Bundle?) {
        // Call the parent class’s onCreate method to perform default initialization
        super.onCreate(savedInstanceState)

        // Check if the device uses 24-hour time format for the TimePickerDialog
        val is24h = android.text.format.DateFormat.is24HourFormat(this.applicationContext)

        // Check if the device is running Android 6.0 (API 23) or later
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Set the status bar to light mode (black icons on a light background)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        // Inflate the layout using View Binding (activity_init_user_info.xml) and set it as the content view
        binding = ActivityInitUserInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPreferences to store user settings
        sharedPref = getSharedPreferences(AppUtils.USERS_SHARED_PREF, AppUtils.PRIVATE_MODE)

        // Retrieve the wake-up time from SharedPreferences (default: a specific timestamp)
        wakeupTime = sharedPref.getLong(AppUtils.WAKEUP_TIME, 1558323000000)
        // Retrieve the sleep time from SharedPreferences (default: a specific timestamp)
        sleepingTime = sharedPref.getLong(AppUtils.SLEEPING_TIME_KEY, 1558369800000)

        // Set a click listener for the wake-up time input field to show a TimePickerDialog
        binding.etWakeUpTime.editText?.setOnClickListener {
            // Create a Calendar instance with the current wake-up time
            val calendar = Calendar.getInstance().apply { timeInMillis = wakeupTime }

            // Show a TimePickerDialog to let the user select a wake-up time
            TimePickerDialog(this, { _, hour, minute ->
                // Create a Calendar instance to store the selected time
                val time = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }
                // Update the wake-up time with the selected time
                wakeupTime = time.timeInMillis
                // Display the selected time in the input field (format: HH:mm)
                binding.etWakeUpTime.editText?.setText(String.format("%02d:%02d", hour, minute))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), is24h).apply {
                // Set the title of the dialog
                setTitle("Select Wakeup Time")
                // Show the dialog
                show()
            }
        }

        // Set a click listener for the sleep time input field to show a TimePickerDialog
        binding.etSleepTime.editText?.setOnClickListener {
            // Create a Calendar instance with the current sleep time
            val calendar = Calendar.getInstance().apply { timeInMillis = sleepingTime }

            // Show a TimePickerDialog to let the user select a sleep time
            TimePickerDialog(this, { _, hour, minute ->
                // Create a Calendar instance to store the selected time
                val time = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }
                // Update the sleep time with the selected time
                sleepingTime = time.timeInMillis
                // Display the selected time in the input field (format: HH:mm)
                binding.etSleepTime.editText?.setText(String.format("%02d:%02d", hour, minute))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), is24h).apply {
                // Set the title of the dialog
                setTitle("Select Sleeping Time")
                // Show the dialog
                show()
            }
        }

        // Set a click listener for the "Continue" button to save user input and proceed
        binding.btnContinue.setOnClickListener { view ->
            // Hide the keyboard when the button is clicked
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.root.windowToken, 0)

            // Get the user’s weight input
            weight = binding.etWeight.editText?.text.toString()
            // Get the user’s workout time input
            workTime = binding.etWorkTime.editText?.text.toString()

            // Validate the user’s input
            when {
                // Check if the weight field is empty
                TextUtils.isEmpty(weight) -> {
                    // Show a Snackbar to prompt the user to enter their weight
                    Snackbar.make(view, "Please input your weight", Snackbar.LENGTH_SHORT).show()
                }
                // Check if the weight is within a valid range (20-200 kg)
                weight.toInt() > 200 || weight.toInt() < 20 -> {
                    // Show a Snackbar if the weight is invalid
                    Snackbar.make(view, "Please input a valid weight", Snackbar.LENGTH_SHORT).show()
                }
                // Check if the workout time field is empty
                TextUtils.isEmpty(workTime) -> {
                    // Show a Snackbar to prompt the user to enter their workout time
                    Snackbar.make(view, "Please input your workout time", Snackbar.LENGTH_SHORT).show()
                }
                // Check if the workout time is within a valid range (0-500 minutes)
                workTime.toInt() > 500 || workTime.toInt() < 0 -> {
                    // Show a Snackbar if the workout time is invalid
                    Snackbar.make(view, "Please input a valid workout time", Snackbar.LENGTH_SHORT).show()
                }
                // If all inputs are valid, save the data and proceed
                else -> {
                    // Create a SharedPreferences editor to save the user’s settings
                    val editor = sharedPref.edit()
                    // Save the weight (in kg)
                    editor.putInt(AppUtils.WEIGHT_KEY, weight.toInt())
                    // Save the workout time (in minutes)
                    editor.putInt(AppUtils.WORK_TIME_KEY, workTime.toInt())
                    // Save the wake-up time (in milliseconds)
                    editor.putLong(AppUtils.WAKEUP_TIME, wakeupTime)
                    // Save the sleep time (in milliseconds)
                    editor.putLong(AppUtils.SLEEPING_TIME_KEY, sleepingTime)
                    // Set the first run flag to false (user has completed onboarding)
                    editor.putBoolean(AppUtils.FIRST_RUN_KEY, false)

                    // Calculate the total daily water intake based on weight and workout time
                    val totalIntake = AppUtils.calculateIntake(weight.toInt(), workTime.toInt())
                    // Format the total intake to a whole number (ceiling)
                    val df = DecimalFormat("#").apply { roundingMode = RoundingMode.CEILING }
                    // Save the total intake (in ml)
                    editor.putInt(AppUtils.TOTAL_INTAKE, df.format(totalIntake).toInt())

                    // Apply the changes to SharedPreferences
                    editor.apply()
                    // Launch MainActivity to start the main app experience
                    startActivity(Intent(this, MainActivity::class.java))
                    // Finish this activity to prevent the user from returning to it
                    finish()
                }
            }
        }
    }

    // onBackPressed is called when the user presses the back button
    override fun onBackPressed() {
        // Check if the back button was pressed once already
        if (doubleBackToExitPressedOnce) {
            // If pressed twice within 1 second, exit the activity by calling the parent method
            super.onBackPressed()
            return
        }

        // Set the flag to indicate the back button was pressed once
        doubleBackToExitPressedOnce = true
        // Show a Snackbar to prompt the user to press back again to exit
        Snackbar.make(binding.root, "Please click BACK again to exit", Snackbar.LENGTH_SHORT).show()

        // Reset the flag after 1 second using a Handler
        Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 1000)
    }
}