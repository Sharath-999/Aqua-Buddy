package keyur.diwan.project.waterReminder.recievers

// Import BroadcastReceiver to handle system broadcasts (e.g., device boot)
import android.content.BroadcastReceiver
// Import Context to access app resources and services
import android.content.Context
// Import Intent to receive the boot completed intent
import android.content.Intent
// Import AlarmHelper to reschedule the alarm after a reboot
import keyur.diwan.project.waterReminder.helpers.AlarmHelper
// Import AppUtils for SharedPreferences keys and utility functions
import keyur.diwan.project.waterReminder.utils.AppUtils

// BootReceiver reschedules the water reminder alarm after a device reboot
class BootReceiver : BroadcastReceiver() {
    // Create an instance of AlarmHelper to manage alarm scheduling
    private val alarm = AlarmHelper()

    // onReceive is called when the system broadcasts an intent (e.g., BOOT_COMPLETED)
    override fun onReceive(context: Context?, intent: Intent?) {
        // Check if the context and intent are not null, and the intent action is not null
        if (intent != null && intent.action != null) {
            // Check if the intent action is BOOT_COMPLETED (device has finished booting)
            if (intent.action == "android.intent.action.BOOT_COMPLETED") {
                // Get SharedPreferences to access user settings
                val prefs = context!!.getSharedPreferences(AppUtils.USERS_SHARED_PREF, AppUtils.PRIVATE_MODE)
                // Retrieve the notification frequency (in minutes, default: 60 minutes)
                val notificationFrequency = prefs.getInt(AppUtils.NOTIFICATION_FREQUENCY_KEY, 60)
                // Retrieve the notification status (default: true)
                val notificationsNewMessage = prefs.getBoolean("notifications_new_message", true)
                // Cancel any existing alarm to avoid duplicates
                alarm.cancelAlarm(context)
                // Check if notifications are enabled
                if (notificationsNewMessage) {
                    // Reschedule the alarm with the stored frequency
                    alarm.setAlarm(context, notificationFrequency.toLong())
                    // Calls AlarmHelper.kt to schedule the alarm
                }
            }
        }
    }
}