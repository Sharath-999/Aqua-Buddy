package keyur.diwan.project.waterReminder.helpers

// Import Notification for creating notifications
import android.app.Notification
// Import NotificationChannel for creating notification channels (Android 8.0+)
import android.app.NotificationChannel
// Import NotificationManager to show notifications
import android.app.NotificationManager
// Import Context to access app resources and services
import android.content.Context
// Import AudioAttributes to configure notification sound settings
import android.media.AudioAttributes
// Import RingtoneManager to get the default notification sound
import android.media.RingtoneManager
// Import Uri to parse the notification sound URI
import android.net.Uri
// Import Build to check the Android API version
import android.os.Build
// Import NotificationCompat for building cross-version compatible notifications
import androidx.core.app.NotificationCompat
// Import app resources (e.g., drawables for notification icon)
import io.github.z3r0c00l_2k.aquadroid.R
// Import AppUtils for SharedPreferences keys and utility functions
import keyur.diwan.project.waterReminder.utils.AppUtils
// Import Calendar and Date for time calculations
import java.util.*

// NotificationHelper manages the creation and display of water reminder notifications
class NotificationHelper(private val context: Context) {

    // Companion object to define notification channel constants
    companion object {
        // Channel ID for the water reminder notification channel
        private const val CHANNEL_ID = "water_reminder_channel"
        // Channel name for the water reminder notification channel
        private const val CHANNEL_NAME = "Water Reminder Notifications"
    }

    // NotificationManager instance to show notifications
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Init block to set up the notification channel when the class is instantiated
    init {
        // Check if the device is running Android 8.0 (API 26) or later
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Get SharedPreferences to retrieve the user’s selected notification sound
            val prefs = context.getSharedPreferences(AppUtils.USERS_SHARED_PREF, AppUtils.PRIVATE_MODE)
            // Retrieve the sound URI (default: system notification sound)
            val soundUri = prefs.getString(
                AppUtils.NOTIFICATION_TONE_URI_KEY,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString()
            )

            // Create a NotificationChannel for water reminders
            val channel = NotificationChannel(
                CHANNEL_ID,                    // Channel ID
                CHANNEL_NAME,                  // Channel name (visible to the user)
                NotificationManager.IMPORTANCE_DEFAULT // Importance level (default: shows notification, plays sound)
            ).apply {
                // Enable vibration for the notification
                enableVibration(true)
                // Configure the notification sound
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION) // Sound type: sonification
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)              // Usage: notification
                    .build()
                // Set the sound URI if it exists
                soundUri?.let {
                    try {
                        // Parse the sound URI and set it for the channel
                        setSound(Uri.parse(it), audioAttributes)
                    } catch (e: Exception) {
                        // If parsing fails, fall back to the default notification sound
                        setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes)
                    }
                } ?: run {
                    // If no sound URI is set, use the default notification sound
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes)
                }
            }
            // Create the notification channel
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Function to build a notification with the given title, message, and sound
    fun getNotification(title: String, message: String, soundUri: String?): Notification {
        // Create a NotificationCompat.Builder for cross-version compatibility
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell) // Set the notification icon (bell icon)
            .setContentTitle(title)           // Set the notification title (e.g., "Water Reminder")
            .setContentText(message)          // Set the notification message (e.g., "Time to drink water!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Set priority (default: shows notification)
            .setAutoCancel(true)              // Dismiss the notification when tapped

        // Set the notification sound for Android versions below 8.0 (API 26)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Check if a sound URI is provided
            soundUri?.let {
                try {
                    // Parse the sound URI and set it for the notification
                    val sound = Uri.parse(it)
                    builder.setSound(sound)
                } catch (e: Exception) {
                    // If parsing fails, fall back to the default notification sound
                    builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                }
            } ?: run {
                // If no sound URI is set, use the default notification sound
                builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            }
        }

        // Build and return the notification
        return builder.build()
        // Called by NotifierReceiver.kt to create a notification when an alarm triggers
    }

    // Function to show a notification with the given ID
    fun notify(id: Int, notification: Notification) {
        // Check if a notification should be shown (based on wake-up/sleep times and intake progress)
        if (shallNotify()) {
            // Show the notification using NotificationManager
            notificationManager.notify(id, notification)
        }
        // Called by NotifierReceiver.kt to display the notification
    }

    // Function to determine if a notification should be shown
    private fun shallNotify(): Boolean {
        // Get SharedPreferences to access user settings
        val prefs = context.getSharedPreferences(AppUtils.USERS_SHARED_PREF, AppUtils.PRIVATE_MODE)
        // Create an instance of SqliteHelper to access the database
        val sqliteHelper = SqliteHelper(context)

        // Retrieve the wake-up time timestamp (in milliseconds)
        val startTimestamp = prefs.getLong(AppUtils.WAKEUP_TIME, 0)
        // Retrieve the sleep time timestamp (in milliseconds)
        val stopTimestamp = prefs.getLong(AppUtils.SLEEPING_TIME_KEY, 0)
        // Retrieve the total intake goal (in ml)
        val totalIntake = prefs.getInt(AppUtils.TOTAL_INTAKE, 0)

        // If wake-up time, sleep time, or total intake is not set, don’t notify
        if (startTimestamp == 0L || stopTimestamp == 0L || totalIntake == 0) {
            return false
        }

        // Calculate the current intake percentage (intake / totalIntake * 100)
        val percent = sqliteHelper.getIntook(AppUtils.getCurrentDate()!!) * 100 / totalIntake
        // Calls SqliteHelper.kt to get the current intake for the day

        // Get the current time as a Date object
        val now = Calendar.getInstance().time
        // Convert the wake-up time to a Date object
        val start = Date(startTimestamp)
        // Convert the sleep time to a Date object
        val stop = Date(stopTimestamp)

        // Calculate the seconds passed since the wake-up time
        val passedSeconds = compareTimes(now, start)
        // Calculate the total seconds between wake-up and sleep times
        val totalSeconds = compareTimes(stop, start)

        // Calculate the target percentage of water that should have been consumed by now
        val currentTarget = passedSeconds.toFloat() / totalSeconds.toFloat() * 100f

        // Check if the current time is within the wake-up/sleep window (Do Not Disturb off)
        val doNotDisturbOff = passedSeconds >= 0 && compareTimes(now, stop) <= 0
        // Notify if Do Not Disturb is off and the current intake percentage is less than the target
        val notify = doNotDisturbOff && (percent < currentTarget)

        // Return true if a notification should be shown, false otherwise
        return notify
    }

    // Helper function to compare two times and return the difference in milliseconds
    private fun compareTimes(currentTime: Date, timeToRun: Date): Long {
        // Create a Calendar instance for the current time
        val currentCal = Calendar.getInstance()
        currentCal.time = currentTime

        // Create a Calendar instance for the time to compare (wake-up or sleep)
        val runCal = Calendar.getInstance()
        runCal.time = timeToRun
        // Set the date to match the current date for accurate comparison
        runCal.set(Calendar.DAY_OF_MONTH, currentCal.get(Calendar.DAY_OF_MONTH))
        runCal.set(Calendar.MONTH, currentCal.get(Calendar.MONTH))
        runCal.set(Calendar.YEAR, currentCal.get(Calendar.YEAR))

        // Get SharedPreferences to access wake-up and sleep times
        val prefs = context.getSharedPreferences(AppUtils.USERS_SHARED_PREF, AppUtils.PRIVATE_MODE)
        // If the time to compare is the sleep time and it’s before the current time
        if (timeToRun == Date(prefs.getLong(AppUtils.SLEEPING_TIME_KEY, 0)) &&
            runCal.timeInMillis < currentCal.timeInMillis &&
            runCal.timeInMillis < Calendar.getInstance().apply {
                timeInMillis = prefs.getLong(AppUtils.WAKEUP_TIME, 0)
            }.timeInMillis
        ) {
            // Add one day to the sleep time to correct the comparison
            runCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Return the difference in milliseconds (current time - time to run)
        return currentCal.timeInMillis - runCal.timeInMillis
    }
}