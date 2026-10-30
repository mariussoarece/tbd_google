# ToBeDone for Android (Google Play)

Android version of the iOS app [ToBeDone](https://github.com/mariussoarece/tobedone),
written in Kotlin with Jetpack Compose (Android's equivalent of SwiftUI).

## What's in the app

Same as the iPhone version (branch `tobedone` of the iOS repo):

**Home** – date, "Hi, <name> ♥", Settings button, a **Today** card (progress, tasks due,
activities left, things to buy) and three groups, each opening its own screen:

| Group | Screen | What it does |
|---|---|---|
| PLAN | **To Do** | Tasks with title, **due date and time**, priority. Reminder at the due time. Due / Completed lists, export & import. |
| PLAN | **Monthly Expenses** | Monthly costs in your currency, with total. Export & import. |
| PLAN | **Projects** | Projects with subtasks (tick each subtask). Active / Completed lists, export & import. |
| DO | **Shopping** | Items with price and priority. Tap an item → confirm the real price → bought. Totals, completed purchases with date. |
| DO | **Grocery** | Quick list; tap to tick off; bought items below. |
| GROW | **Daily Routine** | Monthly calendar with each day's score (done / total × 10), activities per day, weekly timeline (7 days × 24 h), repeat weekly for N weeks, overlap warning. Reminder when an activity starts. |
| GROW | **Confidence** | Rate 1–10 with an optional note, 30-day chart, 7/30-day averages, best/worst, streak. Daily Routine scores are added automatically. |
| GROW | **Focus Test** | Red-dot test (tap when you blink), best/last result, progress chart, history. |

**Settings** – your name, theme (Default / Light / Dark), currency (same list as iOS),
daily summary notification (default 21:00) and activity-start notifications.

**First launch** – the "Caveat" page asks your name, explains the Focus Test and runs it once, then opens Home.

Export files use the same JSON format as the iPhone app, so you can move lists between the two.
All data stays on the phone (`files/tobedone.json`) and is included in Android's automatic backup.

## Project layout

```
app/src/main/java/ro/vishrama/tobedone/
  MainActivity.kt            app entry (LifeOrderingApp in Swift)
  ToBeDoneApp.kt             startup: notification channels, reschedule reminders
  data/Models.kt             TaskModel, ActivityItem, ConfidenceEntry, FocusResult, ...
  data/AppStore.kt           storage + settings (replaces SwiftData / @AppStorage)
  data/ExportImport.kt       iOS-compatible export/import files
  notifications/             due-time, daily summary and activity-start reminders
  ui/AppRoot.kt              navigation: Home → each screen
  ui/Components.kt           iOS-style building blocks (grouped lists, pickers, buttons)
  ui/screens/HomeScreen.kt   the Home screen
  ui/screens/*.kt            To Do, Monthly, Projects, Shopping, Grocery, Daily Routine,
                             Confidence, Focus Test, Settings, first-launch intro
  ui/theme/Theme.kt          iOS system colours, light/dark
play-store/icon-512.png    the 512×512 icon Play Console asks for
```

Package name / application ID: `ro.vishrama.tobedone` (same as the iOS bundle ID).
Version: `versionName 2.0`, `versionCode 2`. Target SDK 36 (Android 16), which Google Play
requires for new apps and updates; min SDK 26 (Android 8.0).

## 1. Open and run it

1. Install **Android Studio** (latest stable): https://developer.android.com/studio
2. *File → Open* → select this folder. Let Gradle sync (first time downloads a lot; 5–10 min).
   If it offers to upgrade the Android Gradle Plugin or Kotlin, accepting is fine.
3. Plug in an Android phone with USB debugging on, or create an emulator
   (*Device Manager → Create device*), then press **Run ▶**.

## 2. Signing (one time)

Google Play needs the app signed with your **upload key**.

1. In Android Studio: *Build → Generate Signed App Bundle / APK → Android App Bundle → Create new…*
   Save the keystore e.g. as `upload-keystore.jks` in this folder. **Back it up and keep the passwords** —
   you need the same key for every future update.
2. Create `keystore.properties` in this folder (it is git-ignored, never commit it):

   ```properties
   storeFile=upload-keystore.jks
   storePassword=YOUR_STORE_PASSWORD
   keyAlias=upload
   keyPassword=YOUR_KEY_PASSWORD
   ```

## 3. Build the release bundle

```bash
./gradlew bundleRelease          # Windows: gradlew.bat bundleRelease
```

The file to upload is `app/build/outputs/bundle/release/app-release.aab`.

For every new release, increase `versionCode` (1 → 2 → 3 …) and `versionName` in `app/build.gradle.kts`.

## 4. Publish on Google Play

1. Create a developer account at https://play.google.com/console (one-time US$25 fee, identity check).
2. *Create app* → name **ToBeDone**, App, Free.
3. Fill in **App content**: privacy policy URL (you can publish `PRIVACY.md` from this repo,
   e.g. via GitHub Pages), Ads: *No*, Data safety: *no data collected or shared*,
   content rating questionnaire, target audience.
4. **Store listing**: short + full description, the icon from `play-store/icon-512.png`,
   a 1024×500 feature graphic, and at least 2 phone screenshots (take them from the emulator).
5. *Testing → Closed testing* → upload the `.aab`.
   **New personal developer accounts must run a closed test with at least 12 testers for 14 days**
   before they can apply for Production.
6. Then *Production → Create release* → upload the `.aab` → send for review.

## Notes

- Reminders use normal (inexact) alarms, so the app does not need the restricted
  "exact alarm" permission (which Google Play only allows for alarm-clock and calendar apps).
  When the phone is idle a reminder can arrive a few minutes late.
- The iOS backup/restore screen is replaced by Android's own automatic backup plus export/import.
- The iOS project had an Apple Watch placeholder; there is no Wear OS version here.
