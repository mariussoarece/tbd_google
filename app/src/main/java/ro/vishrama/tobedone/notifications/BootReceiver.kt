package ro.vishrama.tobedone.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Alarms are cleared on reboot and app update, so put the reminders back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> ReminderScheduler.refresh(context)
        }
    }
}
