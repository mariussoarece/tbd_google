package ro.vishrama.tobedone.ui.screens

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.vishrama.tobedone.data.AppStore
import ro.vishrama.tobedone.data.CURRENCIES
import ro.vishrama.tobedone.data.ThemeSetting
import ro.vishrama.tobedone.ui.Chip
import ro.vishrama.tobedone.ui.DetailScreen
import ro.vishrama.tobedone.ui.FieldRow
import ro.vishrama.tobedone.ui.GroupCard
import ro.vishrama.tobedone.ui.GroupRow
import ro.vishrama.tobedone.ui.PickerRow
import ro.vishrama.tobedone.ui.RowDivider
import ro.vishrama.tobedone.ui.SectionFooter
import ro.vishrama.tobedone.ui.SectionHeader
import ro.vishrama.tobedone.ui.Segmented
import ro.vishrama.tobedone.ui.TimePickDialog
import ro.vishrama.tobedone.ui.formatTime
import ro.vishrama.tobedone.ui.theme.IOS
import java.time.LocalTime

@Composable
fun SettingsScreen(store: AppStore, onBack: () -> Unit) {
    val s by store.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(s.userName) }
    var pickTime by rememberSaveable { mutableStateOf(false) }
    val notificationsOff = !NotificationManagerCompat.from(context).areNotificationsEnabled()

    DetailScreen("Settings", Color.Gray, onBack) {
        item { SectionHeader("Your name") }
        item {
            GroupCard {
                FieldRow(name, { name = it; store.setUserName(it.trim()) }, "Enter your name")
            }
        }

        item { SectionHeader("Appearance") }
        item {
            GroupCard {
                GroupRow {
                    Segmented(ThemeSetting.entries, s.theme, { it.label }, Modifier.weight(1f)) { store.setTheme(it) }
                }
            }
        }

        item { SectionHeader("Currency") }
        item {
            GroupCard {
                PickerRow("Currency", s.currency, CURRENCIES, { it }) { store.setCurrency(it) }
            }
        }

        item { SectionHeader("Notifications") }
        item {
            GroupCard {
                GroupRow {
                    Text("Daily summary", fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = s.dailySummaryEnabled, onCheckedChange = { store.setDailySummary(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = IOS.Green),
                    )
                }
                if (s.dailySummaryEnabled) {
                    RowDivider()
                    GroupRow {
                        Text("Time", fontSize = 17.sp, modifier = Modifier.weight(1f))
                        Chip(formatTime(LocalTime.of(s.reminderHour, s.reminderMinute))) { pickTime = true }
                    }
                }
                RowDivider()
                GroupRow {
                    Text("When an activity starts", fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = s.activityStartEnabled, onCheckedChange = { store.setActivityStart(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = IOS.Green),
                    )
                }
                if (notificationsOff) {
                    RowDivider()
                    GroupRow(onClick = {
                        context.startActivity(
                            Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }) {
                        Text("Turn On Notifications in Phone Settings", color = IOS.Blue, fontSize = 17.sp)
                    }
                }
            }
        }
        item {
            SectionFooter(
                if (notificationsOff) "Notifications are turned off for ToBeDone. Turn them on in your phone's settings."
                else "The daily summary lists the tasks and Daily Routine activities you haven't finished. If everything is done, no notification is sent."
            )
        }

        item { SectionHeader("Data & Backup") }
        item {
            GroupCard {
                GroupRow { Text("Everything is stored only on this phone.", fontSize = 17.sp) }
            }
        }
        item {
            SectionFooter(
                "Your lists are included in Android's automatic backup to your Google account, so they come back " +
                    "when you set up a new phone. You can also export and import lists from the To Do, Monthly, " +
                    "Projects, Shopping and Daily Routine screens — the files work with the iPhone app too."
            )
        }
    }

    if (pickTime) {
        TimePickDialog(LocalTime.of(s.reminderHour, s.reminderMinute), { pickTime = false }) {
            store.setReminderTime(it.hour, it.minute)
        }
    }
}
