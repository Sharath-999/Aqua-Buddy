package keyur.diwan.project.waterReminder

// Import Android package for checking permissions (e.g., POST_NOTIFICATIONS)
import android.content.pm.PackageManager
// Import for Material Design TextInputLayout, used in the custom intake dialog
import com.google.android.material.textfield.TextInputLayout
// Import Android Manifest class for permission constants
import android.Manifest
// Import for NotificationManager to clear notifications when water is added
import android.app.NotificationManager
// Import for creating Intents to launch other activities (e.g., StatsActivity)
import android.content.Intent
// Import for SharedPreferences to store user settings (e.g., total intake, notification status)
import android.content.SharedPreferences
// Import for Build class to check Android API version (e.g., for notification permissions)
import android.os.Build
// Import for Bundle to handle saved instance state in onCreate
import android.os.Bundle
// Import for Handler to delay actions (e.g., double back press to exit)
import android.os.Handler
// Import for TextUtils to check if user input is empty in the custom dialog
import android.text.TextUtils
// Import for TypedValue to resolve theme attributes (e.g., selectable item background)
import android.util.TypedValue
// Import for LayoutInflater to inflate the custom dialog layout
import android.view.LayoutInflater
// Import for ActivityResultContracts to handle permission request results (e.g., POST_NOTIFICATIONS)
import androidx.activity.result.contract.ActivityResultContracts
// Import for AlertDialog to show the custom intake dialog
import androidx.appcompat.app.AlertDialog
// Import for AppCompatActivity, the base class for this activity
import androidx.appcompat.app.AppCompatActivity
// Import for animation library (YoYo) to animate UI elements (e.g., shake effect)
import com.daimajia.androidanimations.library.Techniques
// Import for YoYo animation library to apply animations (e.g., SlideInDown)
import com.daimajia.androidanimations.library.YoYo
// Import for Material Design Snackbar to show messages (e.g., "Water intake saved")
import com.google.android.material.snackbar.Snackbar
// Import app resources (e.g., drawables like ic_bell, layouts)
import io.github.z3r0c00l_2k.aquadroid.R
// Import the View Binding class for this activity, auto-generated from activity_main.xml
import io.github.z3r0c00l_2k.aquadroid.databinding.ActivityMainBinding
// Import BottomSheetFragment to allow users to update settings (e.g., total intake, ringtone)
import keyur.diwan.project.waterReminder.fragments.BottomSheetFragment
// Import AlarmHelper to schedule/cancel water reminder alarms
import keyur.diwan.project.waterReminder.helpers.AlarmHelper
// Import SqliteHelper to manage the app’s database (e.g., store/retrieve water intake)
import keyur.diwan.project.waterReminder.helpers.SqliteHelper
// Import AppUtils for utility functions (e.g., getCurrentDate) and SharedPreferences keys
import keyur.diwan.project.waterReminder.utils.AppUtils
// Import StatsActivity to launch the stats screen when the stats button is clicked
import keyur.diwan.project.waterReminder.StatsActivity

// MainActivity is the primary screen of the app, showing water intake progress and controls
class MainActivity : AppCompatActivity() {

    // View Binding instance to access UI elements (e.g., tvIntook, fabAdd) from activity_main.xml
    private lateinit var binding: ActivityMainBinding
    // Variable to store the user’s daily total water intake goal (in ml), retrieved from SharedPreferences
    private var totalIntake: Int = 0
    // Variable to store the current water intake for the day (in ml), retrieved from SqliteHelper
    private var inTook: Int = 0
    // SharedPreferences instance to store user settings (e.g., total intake, notification status)
    private lateinit var sharedPref: SharedPreferences
    // SqliteHelper instance to interact with the app’s SQLite database (e.g., store water intake)
    private lateinit var sqliteHelper: SqliteHelper
    // String to store the current date (format: YYYY-MM-DD), used as a key in the database
    private lateinit var dateNow: String
    // Boolean to track whether notifications are enabled or disabled
    private var notificStatus: Boolean = false
    // Nullable Int to store the user’s selected water intake option (e.g., 100ml) before adding
    private var selectedOption: Int? = null
    // Nullable Snackbar to manage the currently displayed Snackbar (e.g., to dismiss it)
    private var snackbar: Snackbar? = null
    // Boolean to track double back press for exiting the app
    private var doubleBackToExitPressedOnce = false

    // Launcher for requesting POST_NOTIFICATIONS permission (required on Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // If permission is granted, proceed to set up the alarm for reminders
            setupAlarmIfNeeded()
        } else {
            // If permission is denied, show a Snackbar to inform the user
            Snackbar.make(binding.root, "Notification permission is required to send reminders", Snackbar.LENGTH_LONG).show()
        }
    }

    // onCreate is called when the activity is first created
    override fun onCreate(savedInstanceState: Bundle?) {
        // Call the parent class’s onCreate method to perform default initialization
        super.onCreate(savedInstanceState)

        // Inflate the layout using View Binding (activity_main.xml) and set it as the content view
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPreferences to store user settings (e.g., total intake, notification status)
        sharedPref = getSharedPreferences(AppUtils.USERS_SHARED_PREF, AppUtils.PRIVATE_MODE)
        // Initialize SqliteHelper to interact with the app’s SQLite database
        sqliteHelper = SqliteHelper(this)

        // Retrieve the total intake goal from SharedPreferences (default is 0 if not set)
        totalIntake = sharedPref.getInt(AppUtils.TOTAL_INTAKE, 0)

        // Check if this is the first run of the app (stored in SharedPreferences)
        if (sharedPref.getBoolean(AppUtils.FIRST_RUN_KEY, true)) {
            // If it’s the first run, launch WalkThroughActivity to show the onboarding screen
            startActivity(Intent(this, WalkThroughActivity::class.java))
            // Finish this activity to prevent the user from returning to it
            finish()
        } else if (totalIntake <= 0) {
            // If total intake is not set (≤ 0), launch InitUserInfoActivity to prompt user setup
            startActivity(Intent(this, InitUserInfoActivity::class.java))
            // Finish this activity to prevent the user from returning to it
            finish()
        }

        // Get the current date (format: YYYY-MM-DD) using AppUtils, used as a key in the database
        dateNow = AppUtils.getCurrentDate()!!

        // Request notification permission if needed (for Android 13+)
        requestNotificationPermission()
    }

    // Function to request POST_NOTIFICATIONS permission (required on Android 13+)
    private fun requestNotificationPermission() {
        // Check if the device is running Android 13 (Tiramisu) or later
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if the POST_NOTIFICATIONS permission is already granted
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // If not granted, launch the permission request using the launcher
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // If permission is already granted, proceed to set up the alarm
                setupAlarmIfNeeded()
            }
        } else {
            // For Android 12 and below, notifications are enabled by default, so set up the alarm
            setupAlarmIfNeeded()
        }
    }

    // Function to set up the alarm for water reminders if notifications are enabled
    private fun setupAlarmIfNeeded() {
        // Retrieve the notification status from SharedPreferences (default is true)
        notificStatus = sharedPref.getBoolean(AppUtils.NOTIFICATION_STATUS_KEY, true)
        // Create an instance of AlarmHelper to manage alarms
        val alarm = AlarmHelper()
        // Check if an alarm is already set and if notifications are enabled
        if (!alarm.checkAlarm(this) && notificStatus) {
            // If no alarm is set and notifications are enabled, update the UI to show the bell icon
            binding.btnNotific.setImageDrawable(getDrawable(R.drawable.ic_bell))
            // Set the alarm using AlarmHelper, with the frequency from SharedPreferences (default 30 minutes)
            alarm.setAlarm(this, sharedPref.getInt(AppUtils.NOTIFICATION_FREQUENCY_KEY, 30).toLong())
            // AlarmHelper.kt schedules a repeating alarm that triggers NotifierReceiver.kt
        }
    }

    // Function to update the inTook and totalIntake values and refresh the UI
    fun updateValues() {
        // Retrieve the latest total intake from SharedPreferences
        totalIntake = sharedPref.getInt(AppUtils.TOTAL_INTAKE, 0)
        // Retrieve the current water intake for the day from SqliteHelper.kt
        inTook = sqliteHelper.getIntook(dateNow)
        // Update the UI with the latest values by calling setUIChange
        setUIChange(inTook, totalIntake)
        // This function is called by BottomSheetFragment.kt after the user updates settings
    }

    // Function to update the UI with the current intake and total intake values
    private fun setUIChange(inTook: Int, totalIntake: Int) {
        // Apply a slide-in animation to the intake TextView (tvIntook) using YoYo library
        YoYo.with(Techniques.SlideInDown)
            .duration(500)
            .playOn(binding.tvIntook)

        // Update the intake TextView (tvIntook) with the current intake value
        binding.tvIntook.text = "$inTook"
        // Update the total intake TextView (tvTotalIntake) with the total intake goal
        binding.tvTotalIntake.text = "/$totalIntake ml"
    }

    // onStart is called when the activity becomes visible to the user
    override fun onStart() {
        // Call the parent class’s onStart method to perform default behavior
        super.onStart()

        // Create a TypedValue object to resolve theme attributes
        val outValue = TypedValue()
        // Resolve the selectableItemBackground attribute for button backgrounds
        applicationContext.theme.resolveAttribute(
            android.R.attr.selectableItemBackground,
            outValue,
            true
        )

        // Set the notification button icon based on the notification status
        binding.btnNotific.setImageDrawable(
            if (notificStatus) getDrawable(R.drawable.ic_bell) else getDrawable(R.drawable.ic_bell_disabled)
        )

        // Initialize the database entry for the current date with 0 intake and the total intake goal
        sqliteHelper.addAll(dateNow, 0, totalIntake)
        // Calls SqliteHelper.kt to ensure the database has an entry for today

        // Update the inTook and totalIntake values and refresh the UI
        updateValues()
        // Calls setUIChange to ensure the UI reflects the latest values

        // Set a click listener for the menu button to open BottomSheetFragment
        binding.btnMenu.setOnClickListener {
            // Create an instance of BottomSheetFragment, passing the current context
            val bottomSheetFragment = BottomSheetFragment(this)
            // Show the BottomSheetFragment using the FragmentManager
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
            // BottomSheetFragment.kt allows the user to update settings (e.g., total intake, ringtone)
        }

// Set a click listener for the floating action button (fabAdd) to add water intake
        binding.fabAdd.setOnClickListener {
            // Check if the user has selected an intake option (e.g., 100ml)
            if (selectedOption != null) {
                // Check if the current intake is within 100% of the total intake goal
                if ((inTook * 100 / totalIntake) <= 100) {
                    // Add the selected amount to the database using SqliteHelper.kt
                    if (sqliteHelper.addIntook(dateNow, selectedOption!!) > 0) {
                        // If the database update is successful, update the inTook variable
                        inTook += selectedOption!!
                        // Update the UI with the new intake value
                        setUIChange(inTook, totalIntake)

                        // Show a Snackbar to confirm the intake was saved
                        Snackbar.make(it, "Your water intake was saved...!!", Snackbar.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    // If the intake exceeds the total intake goal, show a message
                    Snackbar.make(it, "You already achieved the goal", Snackbar.LENGTH_SHORT).show()
                    // Reset the intake to 0 in the database using SqliteHelper.kt
                    sqliteHelper.resetIntook(dateNow)
                    // Update the inTook variable to 0
                    inTook = 0
                    // Update the UI to reflect the reset intake
                    setUIChange(inTook, totalIntake)
                }

                // Clear the selected option after adding intake
                selectedOption = null
                // Reset the custom option text to "Custom"
                binding.tvCustom.text = "Custom"
                // Reset the background of all option buttons to the default background
                resetOptionButtons(outValue.resourceId)
                // Clear all notifications using NotificationManager
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
                // This interacts with NotificationHelper.kt indirectly by clearing notifications
            } else {
                // If no option is selected, apply a shake animation to the cardView
                YoYo.with(Techniques.Shake).duration(700).playOn(binding.cardView)
                // Show a Snackbar to prompt the user to select an option
                Snackbar.make(it, "Please select an option", Snackbar.LENGTH_SHORT).show()
            }
        }

        // Set a click listener for the notification toggle button (btnNotific)
        binding.btnNotific.setOnClickListener {
            // Toggle the notification status (enable/disable)
            notificStatus = !notificStatus
            // Save the new notification status to SharedPreferences
            sharedPref.edit().putBoolean(AppUtils.NOTIFICATION_STATUS_KEY, notificStatus).apply()

            // Create an instance of AlarmHelper to manage alarms
            val alarm = AlarmHelper()
            // Check if notifications are enabled
            if (notificStatus) {
                // If enabled, update the UI to show the bell icon
                binding.btnNotific.setImageDrawable(getDrawable(R.drawable.ic_bell))
                // Show a Snackbar to confirm notifications are enabled
                Snackbar.make(it, "Notification Enabled..", Snackbar.LENGTH_SHORT).show()
                // Set the alarm using AlarmHelper.kt with the frequency from SharedPreferences
                alarm.setAlarm(this, sharedPref.getInt(AppUtils.NOTIFICATION_FREQUENCY_KEY, 30).toLong())
            } else {
                // If disabled, update the UI to show the disabled bell icon
                binding.btnNotific.setImageDrawable(getDrawable(R.drawable.ic_bell_disabled))
                // Show a Snackbar to confirm notifications are disabled
                Snackbar.make(it, "Notification Disabled..", Snackbar.LENGTH_SHORT).show()
                // Cancel the alarm using AlarmHelper.kt
                alarm.cancelAlarm(this)
            }
        }

        // Set a click listener for the stats button (btnStats) to launch StatsActivity
        binding.btnStats.setOnClickListener {
            // Create an Intent to launch StatsActivity
            val intent = Intent(this, StatsActivity::class.java)
            // Add the current intake as an extra to the Intent
            intent.putExtra("intook", inTook)
            // Add the total intake as an extra to the Intent
            intent.putExtra("totalIntake", totalIntake)
            // Start StatsActivity to show water intake statistics
            startActivity(intent)
            // StatsActivity.kt uses the extras to display intake statistics
        }

        // Set up click listeners for predefined intake option buttons (50ml, 100ml, etc.)
        setupOptionButton(binding.op50ml, 50, outValue.resourceId)
        setupOptionButton(binding.op100ml, 100, outValue.resourceId)
        setupOptionButton(binding.op150ml, 150, outValue.resourceId)
        setupOptionButton(binding.op200ml, 200, outValue.resourceId)
        setupOptionButton(binding.op250ml, 250, outValue.resourceId)

        // Set a click listener for the custom intake option button (opCustom)
        binding.opCustom.setOnClickListener {
            // Dismiss any existing Snackbar
            snackbar?.dismiss()
            // Inflate the custom input dialog layout (custom_input_dialog.xml)
            val li = LayoutInflater.from(this)
            val promptsView = li.inflate(R.layout.custom_input_dialog, null)

            // Create an AlertDialog with the custom layout
            val alertDialogBuilder = AlertDialog.Builder(this).setView(promptsView)

            // Get the TextInputLayout from the dialog layout to retrieve user input
            val userInput = promptsView.findViewById<TextInputLayout>(R.id.etCustomInput)

            // Set the positive button ("OK") behavior for the dialog
            alertDialogBuilder.setPositiveButton("OK") { dialog, _ ->
                // Get the user’s input from the TextInputLayout
                val inputText = userInput.editText!!.text.toString()
                // Check if the input is not empty
                if (!TextUtils.isEmpty(inputText)) {
                    // Update the custom option text to show the entered value (e.g., "500 ml")
                    binding.tvCustom.text = "$inputText ml"
                    // Set the selected option to the entered value (converted to Int)
                    selectedOption = inputText.toInt()
                }
            }.setNegativeButton("Cancel") { dialog, _ ->
                // Set the negative button ("Cancel") to dismiss the dialog
                dialog.cancel()
            }

            // Create and show the AlertDialog
            alertDialogBuilder.create().show()
            // Reset the background of all option buttons to the default background
            resetOptionButtons(outValue.resourceId)
            // Highlight the custom option button with a selected background
            binding.opCustom.background = getDrawable(R.drawable.option_select_bg)
        }
    }

    // Function to set up a click listener for an intake option button (e.g., 50ml, 100ml)
    private fun setupOptionButton(view: android.view.View, amount: Int, defaultBg: Int) {
        // Set a click listener for the given view (option button)
        view.setOnClickListener {
            // Dismiss any existing Snackbar
            snackbar?.dismiss()
            // Set the selected option to the predefined amount (e.g., 50ml)
            selectedOption = amount
            // Reset the background of all option buttons to the default background
            resetOptionButtons(defaultBg)
            // Highlight the clicked button with a selected background
            view.background = getDrawable(R.drawable.option_select_bg)
        }
    }

    // Function to reset the background of all option buttons to the default background
    private fun resetOptionButtons(defaultBg: Int) {
        // Set the background of the 50ml option button to the default background
        binding.op50ml.background = getDrawable(defaultBg)
        // Set the background of the 100ml option button to the default background
        binding.op100ml.background = getDrawable(defaultBg)
        // Set the background of the 150ml option button to the default background
        binding.op150ml.background = getDrawable(defaultBg)
        // Set the background of the 200ml option button to the default background
        binding.op200ml.background = getDrawable(defaultBg)
        // Set the background of the 250ml option button to the default background
        binding.op250ml.background = getDrawable(defaultBg)
        // Set the background of the custom option button to the default background
        binding.opCustom.background = getDrawable(defaultBg)
    }

    // onBackPressed is called when the user presses the back button
    override fun onBackPressed() {
        // Check if the back button was pressed once already
        if (doubleBackToExitPressedOnce) {
            // If pressed twice within 1 second, exit the app by calling the parent method
            super.onBackPressed()
            return
        }

        // Set the flag to indicate the back button was pressed once
        this.doubleBackToExitPressedOnce = true
        // Show a Snackbar to prompt the user to press back again to exit
        Snackbar.make(
            this.window.decorView.findViewById(android.R.id.content),
            "Please click BACK again to exit",
            Snackbar.LENGTH_SHORT
        ).show()

        // Reset the flag after 1 second using a Handler
        Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 1000)
    }
}


//
//import android.Manifest.permission.POST_NOTIFICATIONS
//import android.app.NotificationManager
//import android.content.Intent
//import android.content.SharedPreferences
//import android.content.pm.PackageManager
//import android.os.Build
//import android.os.Bundle
//import android.os.Handler
//import android.text.TextUtils
//import android.util.TypedValue
//import android.view.LayoutInflater
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import com.daimajia.androidanimations.library.Techniques
//import com.daimajia.androidanimations.library.YoYo
//import com.google.android.material.snackbar.Snackbar
//import com.google.android.material.textfield.TextInputLayout
//import io.github.z3r0c00l_2k.aquadroid.Manifest
//import io.github.z3r0c00l_2k.aquadroid.R
//import io.github.z3r0c00l_2k.aquadroid.databinding.ActivityMainBinding
//import keyur.diwan.project.waterReminder.fragments.BottomSheetFragment
//import keyur.diwan.project.waterReminder.helpers.AlarmHelper
//import keyur.diwan.project.waterReminder.helpers.SqliteHelper
//import keyur.diwan.project.waterReminder.utils.AppUtils
//import keyur.diwan.project.waterReminder.StatsActivity
//
//class MainActivity : AppCompatActivity() {
//
//    private val notificationPermissionLauncher = registerForActivityResult(
//        ActivityResultContracts.RequestPermission()
//    ) { isGranted: Boolean ->
//        if (isGranted) {
//            // Permission granted, proceed with alarm setup
//            setupAlarmIfNeeded()
//        } else {
//            // Permission denied, show a message to the user
//            Snackbar.make(binding.root, "Notification permission is required to send reminders", Snackbar.LENGTH_LONG).show()
//        }
//    }
//
//    private fun setupAlarmIfNeeded() {
//        notificStatus = sharedPref.getBoolean(AppUtils.NOTIFICATION_STATUS_KEY, true)
//        val alarm = AlarmHelper()
//        if (!alarm.checkAlarm(this) && notificStatus) {
//            binding.btnNotific.setImageDrawable(getDrawable(R.drawable.ic_bell))
//            alarm.setAlarm(this, sharedPref.getInt(AppUtils.NOTIFICATION_FREQUENCY_KEY, 30).toLong())
//        }
//    }
//
//    private fun requestNotificationPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
//            } else {
//                // Permission already granted, proceed with alarm setup
//                setupAlarmIfNeeded()
//            }
//        } else {
//            // For Android 12 and below, notifications are enabled by default
//            setupAlarmIfNeeded()
//        }
//    }
//
//
//
//    private lateinit var binding: ActivityMainBinding
//
//    private var totalIntake: Int = 0
//    private var inTook: Int = 0
//    private lateinit var sharedPref: SharedPreferences
//    private lateinit var sqliteHelper: SqliteHelper
//    private lateinit var dateNow: String
//    private var notificStatus: Boolean = false
//    private var selectedOption: Int? = null
//    private var snackbar: Snackbar? = null
//    private var doubleBackToExitPressedOnce = false
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        sharedPref = getSharedPreferences(AppUtils.USERS_SHARED_PREF, AppUtils.PRIVATE_MODE)
//        sqliteHelper = SqliteHelper(this)
//
//        totalIntake = sharedPref.getInt(AppUtils.TOTAL_INTAKE, 0)
//
//        if (sharedPref.getBoolean(AppUtils.FIRST_RUN_KEY, true)) {
//            startActivity(Intent(this, WalkThroughActivity::class.java))
//            finish()
//        } else if (totalIntake <= 0) {
//            startActivity(Intent(this, InitUserInfoActivity::class.java))
//            finish()
//        }
//
//        dateNow = AppUtils.getCurrentDate()!!
//
//        //notification part
//        requestNotificationPermission()
//    }
//
//    fun updateValues() {
//        totalIntake = sharedPref.getInt(AppUtils.TOTAL_INTAKE, 0)
//        inTook = sqliteHelper.getIntook(dateNow)
//        //setWaterLevel(inTook, totalIntake)
//    }
//
//private fun setWaterLevel(inTook: Int, totalIntake: Int) {
//    YoYo.with(Techniques.SlideInDown)
//        .duration(500)
//        .playOn(binding.tvIntook)
//
//    binding.tvIntook.text = "$inTook"
//    binding.tvTotalIntake.text = "/$totalIntake ml"
//
//    if ((inTook * 100 / totalIntake) > 140) {
//        Snackbar.make(binding.root, "You achieved the goal", Snackbar.LENGTH_SHORT).show()
//    }
//}
//
//    override fun onStart() {
//        super.onStart()
//
//        val outValue = TypedValue()
//        applicationContext.theme.resolveAttribute(
//            android.R.attr.selectableItemBackground,
//            outValue,
//            true
//        )
//
//        notificStatus = sharedPref.getBoolean(AppUtils.NOTIFICATION_STATUS_KEY, true)
//        val alarm = AlarmHelper()
//
//        if (!alarm.checkAlarm(this) && notificStatus) {
//            binding.btnNotific.setImageDrawable(getDrawable(R.drawable.ic_bell))
//            alarm.setAlarm(this, sharedPref.getInt(AppUtils.NOTIFICATION_FREQUENCY_KEY, 30).toLong())
//        }
//
//        binding.btnNotific.setImageDrawable(
//            if (notificStatus) getDrawable(R.drawable.ic_bell) else getDrawable(R.drawable.ic_bell_disabled)
//        )
//
//        sqliteHelper.addAll(dateNow, 0, totalIntake)
//
//        updateValues()
//
//        binding.btnMenu.setOnClickListener {
//            val bottomSheetFragment = BottomSheetFragment(this)
//            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
//        }
//
//        binding.fabAdd.setOnClickListener {
//            if (selectedOption != null) {
//                if ((inTook * 100 / totalIntake) <= 140) {
//                    if (sqliteHelper.addIntook(dateNow, selectedOption!!) > 0) {
//                        inTook += selectedOption!!
//                        setWaterLevel(inTook, totalIntake)
//
//                        Snackbar.make(it, "Your water intake was saved...!!", Snackbar.LENGTH_SHORT)
//                            .show()
//                    }
//                } else {
//                    Snackbar.make(it, "You already achieved the goal", Snackbar.LENGTH_SHORT).show()
//                }
//
//                selectedOption = null
//                binding.tvCustom.text = "Custom"
//                resetOptionButtons(outValue.resourceId)
//
//                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
//            } else {
//                YoYo.with(Techniques.Shake).duration(700).playOn(binding.cardView)
//                Snackbar.make(it, "Please select an option", Snackbar.LENGTH_SHORT).show()
//            }
//        }
//
//        binding.btnNotific.setOnClickListener {
//            notificStatus = !notificStatus
//            sharedPref.edit().putBoolean(AppUtils.NOTIFICATION_STATUS_KEY, notificStatus).apply()
//
//            if (notificStatus) {
//                binding.btnNotific.setImageDrawable(getDrawable(R.drawable.ic_bell))
//                Snackbar.make(it, "Notification Enabled..", Snackbar.LENGTH_SHORT).show()
//                alarm.setAlarm(this, sharedPref.getInt(AppUtils.NOTIFICATION_FREQUENCY_KEY, 30).toLong())
//            } else {
//                binding.btnNotific.setImageDrawable(getDrawable(R.drawable.ic_bell_disabled))
//                Snackbar.make(it, "Notification Disabled..", Snackbar.LENGTH_SHORT).show()
//                alarm.cancelAlarm(this)
//            }
//        }
//
//        binding.btnStats.setOnClickListener {
//            val intent = Intent(this, StatsActivity::class.java)
//            intent.putExtra("intook", inTook)
//            intent.putExtra("totalIntake", totalIntake)
//            startActivity(intent)
//        }
//
//        setupOptionButton(binding.op50ml, 50, outValue.resourceId)
//        setupOptionButton(binding.op100ml, 100, outValue.resourceId)
//        setupOptionButton(binding.op150ml, 150, outValue.resourceId)
//        setupOptionButton(binding.op200ml, 200, outValue.resourceId)
//        setupOptionButton(binding.op250ml, 250, outValue.resourceId)
//
//        binding.opCustom.setOnClickListener {
//            snackbar?.dismiss()
//            val li = LayoutInflater.from(this)
//            val promptsView = li.inflate(R.layout.custom_input_dialog, null)
//
//            val alertDialogBuilder = AlertDialog.Builder(this).setView(promptsView)
//
//            val userInput = promptsView.findViewById<TextInputLayout>(R.id.etCustomInput)
//
//            alertDialogBuilder.setPositiveButton("OK") { dialog, _ ->
//                val inputText = userInput.editText!!.text.toString()
//                if (!TextUtils.isEmpty(inputText)) {
//                    binding.tvCustom.text = "$inputText ml"
//                    selectedOption = inputText.toInt()
//                }
//            }.setNegativeButton("Cancel") { dialog, _ ->
//                dialog.cancel()
//            }
//
//            alertDialogBuilder.create().show()
//            resetOptionButtons(outValue.resourceId)
//            binding.opCustom.background = getDrawable(R.drawable.option_select_bg)
//        }
//    }
//
//    private fun setupOptionButton(view: android.view.View, amount: Int, defaultBg: Int) {
//        view.setOnClickListener {
//            snackbar?.dismiss()
//            selectedOption = amount
//            resetOptionButtons(defaultBg)
//            view.background = getDrawable(R.drawable.option_select_bg)
//        }
//    }
//
//    private fun resetOptionButtons(defaultBg: Int) {
//        binding.op50ml.background = getDrawable(defaultBg)
//        binding.op100ml.background = getDrawable(defaultBg)
//        binding.op150ml.background = getDrawable(defaultBg)
//        binding.op200ml.background = getDrawable(defaultBg)
//        binding.op250ml.background = getDrawable(defaultBg)
//        binding.opCustom.background = getDrawable(defaultBg)
//    }
//
//    override fun onBackPressed() {
//        if (doubleBackToExitPressedOnce) {
//            super.onBackPressed()
//            return
//        }
//
//        this.doubleBackToExitPressedOnce = true
//        Snackbar.make(
//            this.window.decorView.findViewById(android.R.id.content),
//            "Please click BACK again to exit",
//            Snackbar.LENGTH_SHORT
//        ).show()
//
//        Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 1000)
//    }
//}
