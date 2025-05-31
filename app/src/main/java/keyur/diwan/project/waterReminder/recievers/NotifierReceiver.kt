package keyur.diwan.project.waterReminder.recievers

// Import BroadcastReceiver to handle system broadcasts (e.g., alarm triggers)
import android.content.BroadcastReceiver
// Import Context to access app resources and services
import android.content.Context
// Import Intent to receive the alarm intent and create new intents
import android.content.Intent
// Import SharedPreferences to access user settings (e.g., notification message, sound)
import android.content.SharedPreferences
// Import RingtoneManager to get the default notification sound if needed
import android.media.RingtoneManager
// Import AppUtils for utility functions and SharedPreferences keys
import keyur.diwan.project.waterReminder.utils.AppUtils
// Import AlarmHelper to reschedule the next alarm
import keyur.diwan.project.waterReminder.helpers.AlarmHelper
// Import NotificationHelper to create and show notifications
import keyur.diwan.project.waterReminder.helpers.NotificationHelper

// NotifierReceiver is a BroadcastReceiver that handles alarm triggers for water reminders
class NotifierReceiver : BroadcastReceiver() {

    // onReceive is called when the alarm triggers (scheduled by AlarmHelper.kt)
    override fun onReceive(context: Context, intent: Intent) {
        // Get the SharedPreferences instance to access user settings
        val prefs: SharedPreferences = context.getSharedPreferences(AppUtils.USERS_SHARED_PREF, AppUtils.PRIVATE_MODE)

        // Get the notification message from SharedPreferences (default: "Time to drink water!")
        val message: String? = prefs.getString(AppUtils.NOTIFICATION_MSG_KEY, "Time to drink water!")

        // Get the notification title (hardcoded as "Water Reminder")
        val title = "Water Reminder"

        // Get the notification sound URI from SharedPreferences (default: system notification sound)
        val notificationsTone = prefs.getString(
            AppUtils.NOTIFICATION_TONE_URI_KEY,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString()
        )

        // Create an instance of NotificationHelper to build and show the notification
        val nHelper = NotificationHelper(context)

        // Build the notification with the title, message, and sound URI
        val nBuilder = message?.let {
            nHelper.getNotification(title, it, notificationsTone)
        }

        // Show the notification with ID 100 if the builder is not null
        nBuilder?.let { nHelper.notify(100, it) }

        // Get the notification frequency from the intent (passed by AlarmHelper.kt)
        val frequency = intent.getLongExtra("frequency", 30L)

        // Create an instance of AlarmHelper to reschedule the next alarm
        val alarmHelper = AlarmHelper()

        // Reschedule the next alarm with the same frequency
        alarmHelper.setAlarm(context, frequency)
        // This ensures notifications are repeated at the specified interval
    }
}