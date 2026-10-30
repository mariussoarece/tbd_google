# ToBeDone for Android (Google Play)

Android version of the iOS app [ToBeDone](https://github.com/mariussoarece/tobedone),
written in Kotlin with Jetpack Compose (Android's equivalent of SwiftUI).

## What's in the app

Same six tabs as the iPhone version:

| Tab | What it does |
|---|---|
| **Due** | Tasks with a title, due date and priority (High / Medium / Low). Tap to mark done; incomplete tasks first, sorted by date. A reminder notification is shown at 09:00 on the due date. |
| **Shopping** | Items with price and priority. Tap the cart to mark as bought; shows total cost of what's left, and a "Completed Purchases" list. |
| **Monthly** | Recurring monthly costs with a running total. |
| **Grocery** | Simple list; tap to tick off. |
| **Other** | Tasks with subtasks. Tap the circle to complete, tap the row to open/add subtasks. |
| **Settings** | Theme (Default / Light / Dark) and the currency used for new shopping items (default RON). |

Small Android extras: **swipe left on any row to delete it**, prices accept both `12.5` and `12,5`,
and data is included in Android's backup so it survives a phone change.

All data stays on the phone (`files/tobedone.json`), nothing is sent anywhere.

## Project layout

```
app/src/main/java/ro/vishrama/tobedone/
  MainActivity.kt          app entry (LifeOrderingApp in Swift)
  ToBeDoneApp.kt           startup: notification channel, reschedule reminders
  data/Models.kt           TaskModel, ShoppingItemModel, MonthlyTaskModel, ... 
  data/AppStore.kt         storage (replaces SwiftData)
  notifications/           reminders (replaces NotificationManager / UNUserNotificationCenter)
  ui/MainScreen.kt         bottom tab bar (TabView)
  ui/screens/*.kt          one file per tab
  ui/theme/Theme.kt        colours (yellow from the icon) + light/dark
play-store/icon-512.png    the 512×512 icon Play Console asks for
```

Package name / application ID: `ro.vishrama.tobedone` (same as the iOS bundle ID).
Version: `versionName 2.0`, `versionCode 1`. Target SDK 36 (Android 16), which Google Play
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
  "exact alarm" permission; the notification may arrive a few minutes after 09:00.
- The iOS project had an Apple Watch placeholder; there is no Wear OS version here.
