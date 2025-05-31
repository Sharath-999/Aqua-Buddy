package keyur.diwan.project.waterReminder.helpers

// Import AlarmManager to schedule and cancel alarms
import android.app.AlarmManager
// Import PendingIntent to create intents for alarm triggers
import android.app.PendingIntent
// Import ComponentName to enable/disable the BootReceiver
import android.content.ComponentName
// Import Context to access system services (e.g., AlarmManager)
import android.content.Context
// Import Intent to create the alarm intent for NotifierReceiver
import android.content.Intent
// Import PackageManager to enable/disable the BootReceiver
import android.content.pm.PackageManager
// Import Build to check the Android API version for alarm scheduling
import android.os.Build
// Import Log for logging alarm scheduling details
import android.util.Log
// Import BootReceiver to enable/disable it for rescheduling alarms on boot
import keyur.diwan.project.waterReminder.recievers.BootReceiver
// Import NotifierReceiver as the target for alarm intents
import keyur.diwan.project.waterReminder.recievers.NotifierReceiver
// Import AppUtils for SharedPreferences keys and utility functions
import keyur.diwan.project.waterReminder.utils.AppUtils
// Import Calendar to handle time calculations for scheduling
import java.util.Calendar
// Import TimeUnit to convert minutes to milliseconds for alarm intervals
import java.util.concurrent.TimeUnit

// AlarmHelper manages scheduling and canceling of water reminder alarms
class AlarmHelper {
    // AlarmManager instance to schedule and cancel alarms
    private var alarmManager: AlarmManager? = null
    // Action string for the alarm intent (used by NotifierReceiver)
    private val ACTION_BD_NOTIFICATION = "io.github.z3r0c00l_2k.aquadroid.NOTIFICATION"

    // Function to schedule a repeating alarm for water reminders
    fun setAlarm(context: Context, notificationFrequency: Long) {
        // Convert the notification frequency from minutes to milliseconds
        val notificationFrequencyMs = TimeUnit.MINUTES.toMillis(notificationFrequency)
        // Get the AlarmManager system service
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Get SharedPreferences to access wake-up and sleep times
        val prefs = context.getSharedPreferences(AppUtils.USERS_SHARED_PREF, AppUtils.PRIVATE_MODE)
        // Retrieve the wake-up time timestamp (in milliseconds)
        val startTimestamp = prefs.getLong(AppUtils.WAKEUP_TIME, 0)
        // Retrieve the sleep time timestamp (in milliseconds)
        val stopTimestamp = prefs.getLong(AppUtils.SLEEPING_TIME_KEY, 0)

        // Check if wake-up or sleep time is not set (0L means unset)
        if (startTimestamp == 0L || stopTimestamp == 0L) {
            // Log an error if wake-up or sleep time is not set, and exit the function
            Log.e("AlarmHelper", "Cannot schedule alarm: Wake-up or sleep time not set")
            return
        }

        // Create an Intent for NotifierReceiver to handle the alarm trigger
        val alarmIntent = Intent(context, NotifierReceiver::class.java).apply {
            // Set the action for the intent (used by NotifierReceiver)
            action = ACTION_BD_NOTIFICATION
            // Pass the notification frequency as an extra to reschedule the next alarm
            putExtra("frequency", notificationFrequency)
        }

        // Create a PendingIntent for the alarm (immutable for Android 12+)
        val pendingAlarmIntent = getImmutablePendingIntent(context, alarmIntent)

        // Get the current time using Calendar
        val now = Calendar.getInstance()
        // Create a Calendar instance for the wake-up time
        val startCal = Calendar.getInstance().apply { timeInMillis = startTimestamp }
        // Create a Calendar instance for the sleep time
        val stopCal = Calendar.getInstance().apply { timeInMillis = stopTimestamp }

        // Set the wake-up time to the same date as the current time for comparison
        startCal.set(Calendar.YEAR, now.get(Calendar.YEAR))
        startCal.set(Calendar.MONTH, now.get(Calendar.MONTH))
        startCal.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))

        // Set the sleep time to the same date as the current time for comparison
        stopCal.set(Calendar.YEAR, now.get(Calendar.YEAR))
        stopCal.set(Calendar.MONTH, now.get(Calendar.MONTH))
        stopCal.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))

        // If sleep time is before wake-up time, it means sleep time is on the next day
        if (stopCal.before(startCal)) {
            // Add one day to the sleep time to correct the comparison
            stopCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Get the current time in milliseconds
        val currentTime = now.timeInMillis
        // Default next trigger time to 1 minute from now (for testing if logic fails)
        var nextTriggerTime = now.timeInMillis + 60000

        // Determine the next alarm trigger time based on the current time
        if (currentTime >= startCal.timeInMillis && currentTime <= stopCal.timeInMillis) {
            // If current time is within the wake-up/sleep window, schedule the next alarm after the frequency
            nextTriggerTime = currentTime + notificationFrequencyMs
        } else if (currentTime < startCal.timeInMillis) {
            // If current time is before wake-up, schedule the alarm at the wake-up time
            nextTriggerTime = startCal.timeInMillis
        } else {
            // If current time is after sleep time, schedule for the next day's wake-up time
            startCal.add(Calendar.DAY_OF_MONTH, 1)
            nextTriggerTime = startCal.timeInMillis
        }

        // Log the alarm scheduling details (frequency and trigger time)
        Log.i("AlarmHelper", "Setting Alarm Interval to: $notificationFrequency minutes, scheduled at: ${Calendar.getInstance().apply { timeInMillis = nextTriggerTime }.time}")

        // Schedule the alarm using AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For Android 6.0+ (API 23), use setExactAndAllowWhileIdle to ensure precise timing in Doze mode
            alarmManager?.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, // Wake the device if asleep
                nextTriggerTime,         // Time to trigger the alarm
                pendingAlarmIntent       // PendingIntent to trigger NotifierReceiver
            )
        } else {
            // For older Android versions, use setExact for precise timing
            alarmManager?.setExact(
                AlarmManager.RTC_WAKEUP, // Wake the device if asleep
                nextTriggerTime,         // Time to trigger the alarm
                pendingAlarmIntent       // PendingIntent to trigger NotifierReceiver
            )
        }

        // Enable the BootReceiver to reschedule alarms after a device reboot
        val receiver = ComponentName(context, BootReceiver::class.java)
        context.packageManager.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, // Enable the BootReceiver
            PackageManager.DONT_KILL_APP                    // Don’t kill the app while enabling
        )
        // BootReceiver.kt will reschedule the alarm on device boot
    }

    // Function to cancel the scheduled alarm
    fun cancelAlarm(context: Context) {
        // Get the AlarmManager system service
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Create an Intent for NotifierReceiver to match the scheduled alarm
        val alarmIntent = Intent(context, NotifierReceiver::class.java).apply {
            // Set the same action as the scheduled alarm
            action = ACTION_BD_NOTIFICATION
        }

        // Create a PendingIntent to cancel the alarm (immutable for Android 12+)
        val pendingAlarmIntent = getImmutablePendingIntent(context, alarmIntent)
        // Cancel the alarm using AlarmManager
        alarmManager?.cancel(pendingAlarmIntent)
        // Cancel the PendingIntent itself
        pendingAlarmIntent.cancel()

        // Disable the BootReceiver since alarms are canceled
        val receiver = ComponentName(context, BootReceiver::class.java)
        val pm = context.packageManager
        pm.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, // Disable the BootReceiver
            PackageManager.DONT_KILL_APP                     // Don’t kill the app while disabling
        )
        // Log that the alarm has been canceled
        Log.i("AlarmHelper", "Cancelling alarms")
    }

    // Function to check if an alarm is already scheduled
    fun checkAlarm(context: Context): Boolean {
        // Create an Intent for NotifierReceiver to match the scheduled alarm
        val alarmIntent = Intent(context, NotifierReceiver::class.java).apply {
            // Set the same action as the scheduled alarm
            action = ACTION_BD_NOTIFICATION
        }

        // Set flags to check for an existing PendingIntent (immutable for Android 12+)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        else
            PendingIntent.FLAG_NO_CREATE

        // Check if a PendingIntent exists for the alarm
        val pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, flags)
        // Return true if the PendingIntent exists (alarm is scheduled), false otherwise
        return pendingIntent != null
        // Called by MainActivity.kt to avoid scheduling duplicate alarms
    }

    // Helper function to create an immutable PendingIntent for the alarm
    private fun getImmutablePendingIntent(context: Context, intent: Intent): PendingIntent {
        // Set flags for the PendingIntent (immutable for Android 12+)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else
            PendingIntent.FLAG_UPDATE_CURRENT

        // Create and return the PendingIntent for NotifierReceiver
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }
}