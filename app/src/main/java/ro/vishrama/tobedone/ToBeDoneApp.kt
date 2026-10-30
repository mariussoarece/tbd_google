package ro.vishrama.tobedone

import android.app.Application
import ro.vishrama.tobedone.data.AppStore
import ro.vishrama.tobedone.notifications.ReminderScheduler

class ToBeDoneApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
        ReminderScheduler.rescheduleAll(this, AppStore.get(this).data.value.dueTasks)
    }
}
