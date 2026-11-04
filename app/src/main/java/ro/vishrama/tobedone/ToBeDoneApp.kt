package ro.vishrama.tobedone

import android.app.Application
import ro.vishrama.tobedone.notifications.ReminderScheduler

class ToBeDoneApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannels(this)
        ReminderScheduler.refresh(this)
    }
}
