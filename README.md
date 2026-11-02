# Storytel Reading Log

Native Android app for a personal Storytel listening and reading history. Android 8+ (API 26), Java 17, Android Gradle Plugin 8.7.3, Gradle 8.9 and Android SDK 35.

## Features

- Automatic audiobook discovery and start-date logging using Android notification access.
- Local SQLite storage; no server, analytics or Internet permission. Android backup disabled.
- Book title, author, format, optional series, start and finish dates.
- Separate timestamped comments on books and series.
- Search by title, author or series; sort by title, author, series or start date; filter by reading status.
- Manual correction, date pickers, deletion confirmation and series autocomplete.

## Automatic capture

Install the app, choose **Set up automatic listening log**, and enable its notification access in Android settings. Start playback in Storytel. Only `grit.storytel.app` and `com.storytel.app` notifications are processed. Media metadata is preferred, falling back to notification title/text. A playing media session is required when its state is available; otherwise an ongoing notification is accepted. Active notifications are inspected when the listener connects. The setup dialog shows the last detected book.

Repeated notifications for the same title and author do not create duplicate books. The first observed date becomes its start date. Already-finished books are not automatically restarted. Correct metadata manually when notification text contains a narrator or chapter rather than book details. A different title or author may create a separate entry. Detection cannot recover activity before access was enabled. Dates are local calendar dates; comments use UTC.

Series membership, ebook reading, whole-book completion and re-reading sessions cannot be reliably derived from playback notifications in this version. Enter those details manually. Notification removal, playback pauses and chapter endings never mark a book finished. No Storytel API or account credentials are used. Device testing with the installed Storytel version is required to verify its metadata format and package ID. Battery restrictions or revoked notification access can interrupt capture.

## Diagnostic export

Use **Export debug log** on the home screen to save a JSON Lines file through Android's document picker. It includes Storytel notification extras (text/scalars and type names for complex objects), media title/author/artist/album, playback state and position, capture/skip decisions, database record IDs, lifecycle events and error stack traces. Each record has a UTC timestamp. Only the two recognized Storytel packages are logged. Tokens, images and intents are not serialized. Two private rotating files retain approximately 2 MiB in total; old events are discarded. **Set up automatic listening log → Clear debug log** deletes this diagnostic history without deleting books.

The export contains personal reading metadata. Review it before sharing. To investigate a problem, clear the log, reproduce it in Storytel, then export it and include what you expected, the approximate time and your Storytel version. A debug log is not a backup of the reading database.

## Build instructions

Open this directory in Android Studio with JDK 17 and install Android SDK 35. Use an installed Gradle 8.9 to build, or generate the wrapper with `gradle wrapper --gradle-version 8.9` first. This initial source does not include a downloaded wrapper binary.

```sh
bash scripts/test.sh
gradle lint assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions installs the toolchain, runs validation and lint, builds the app, and uploads a debug APK artifact. Debug APKs are for development.

## Private repository

This project must remain private. To create and push from an authenticated GitHub CLI:

```sh
gh repo create storytel-reading-log --private --source=. --remote=origin --push
```

No books or personal reading history are checked into Git. Uninstalling the app removes its local history; export/restore is not implemented yet.

## Device acceptance checks

1. Enable notification access and play a Storytel book; confirm a book and today's start date appear.
2. Pause/resume and change chapters; check that no duplicate or false completion appears. Inspect the title and author against Storytel.
3. Reopen the app and restart the device; verify persistence and continued capture.
4. Edit dates and series, add comments to both, search and sort, then restart the app and verify changes.
5. Disable notification access and confirm logging stops. Other apps' notifications must never create books.

The core date rules can be tested without the Android SDK. Full Android compilation and device acceptance are separate checks.
