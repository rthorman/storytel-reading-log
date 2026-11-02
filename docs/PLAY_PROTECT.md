# Play Protect: installation block and review packet

## Current status

A user reported **“App blocked to protect your device”** when installing v0.2.0. Google documents this message for internet-downloaded apps that declare certain sensitive capabilities, including notification-listener access. This app uses that capability for its core automatic Storytel listening log, so the report is consistent with that protection. We have not obtained a device-specific Google classification or a completed review.

Google's protection is active in certain markets. It is distinct from a recommended scan for an unfamiliar app, a harmful-app classification, and Android's post-installation restricted-settings screen. See [Google's developer guidance](https://developers.google.com/android/play-protect/warning-dev-guidance).

## Supported resolution

1. Request Google's review through the [Play Protect appeals form](https://support.google.com/googleplay/android-developer/contact/protectappeals), using the release details and draft below. The form currently asks for an APK SHA-256 associated with a VirusTotal submission. If submitting the public release APK for analysis, verify that its hash matches the value below. Do not upload personal CSV backups or diagnostic logs.
2. Alternatively, prepare distribution through Google Play's testing/review process using a developer account, the necessary listing/privacy disclosures and an enduring private signing identity. That remains subject to Google's review; creating a test track or signing with a new key does not guarantee approval.

The owner must submit the review using their contact details. **No appeal has been submitted by this project automation, and no approval is claimed.** A code edit cannot override Google's classification. Renaming the package, concealing notification access, switching Debug mode, or repeatedly re-signing the same implementation does not resolve the underlying sensitive-access requirement. Removing notification access would remove automatic listening capture, so it has been retained.

## v0.2.0 review details

| Field | Value |
| --- | --- |
| App | Storytel Reading Log |
| Package | `com.readinglog.storytel` |
| Version | `0.2.0` / version code `2` |
| APK | [storytel-reading-log.apk](https://github.com/rthorman/storytel-reading-log/releases/download/v0.2.0/storytel-reading-log.apk) |
| APK SHA-256 | `aad4f500f783d30b80982d194f0fe3ea26ff13f807f6ae2b5811df0692a045b6` |
| Signing certificate SHA-256 | `5778faf975d648e52b9b4d436efae3721eb1f208a5bc00db0b4b47544ce07cb3` |
| Signature inspection | APK signature verified; v2 signing present |
| Android debuggable | `false` |
| Sensitive capability | Notification listener protected by `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` |
| Source license | AGPL-3.0-or-later |

The v0.2.0 certificate subject is `CN=Android Debug, O=Android, C=US`. It is a temporary development signing identity, even though the installed release variant is not Android-debuggable. This is separate from the default-off runtime Debug mode. An enduring private release identity is still needed for reliable updates and publisher continuity; adopting it alone does not remove this notification-access installation restriction. Do not silently replace the published APK with a differently signed file or assume an existing installation can update across certificates.

## Permission and data-use evidence

- [Manifest](../app/src/main/AndroidManifest.xml): launcher activity and the system-bound notification-listener service. No Internet, SMS, accessibility, device-admin, overlay or package-install permission is declared.
- [Listener](../app/src/main/java/com/readinglog/storytel/StorytelListener.java): checks the notification's package before reading its content; processes only `grit.storytel.app` and `com.storytel.app`. Other packages are ignored. No notifications are cancelled or modified.
- [Settings and consent flow](../app/src/main/java/com/readinglog/storytel/MainActivity.java): describes the feature and opens Android's notification-access screen for the user's explicit action. The app does not grant itself access.
- [Database](../app/src/main/java/com/readinglog/storytel/LogDatabase.java): books, series, dates and comments stored in local SQLite. No server or analytics SDK is used.
- [Diagnostics](../app/src/main/java/com/readinglog/storytel/DebugLog.java): persistent event logging is gated by the user's Debug mode preference, default `false`. Exports require the user to choose a destination. [Diagnostic details](DIAGNOSTICS.md).
- [CSV transfer](BACKUPS.md): reading history is exported/imported through the system file picker on the user's request; it is not uploaded automatically.

These are source and packaging observations, not a Google certification or a claim that an automated test suite proves an app harmless.

## Draft for the appeal's additional-information field

> Please review the installation block for Storytel Reading Log, package com.readinglog.storytel, version 0.2.0. A user reports “App blocked to protect your device” when installing our public release APK.
>
> The app is an independent, open-source personal reading journal. Its notification listener is used to detect Storytel audiobook playback and save a book title, author and first observed listening date. Users are told what notification access is for and must enable it themselves in Android settings. The listener checks package names first and ignores all notifications except the two supported Storytel packages. It does not cancel or modify notifications, extract login credentials, read SMS, use accessibility services or perform device administration.
>
> The app declares no Internet permission and has no server, analytics SDK or automatic upload. Books and comments are stored locally. Optional diagnostics are off by default. Users can enable them to investigate a problem and explicitly export them; CSV backups also require a user-selected file destination.
>
> Source: https://github.com/rthorman/storytel-reading-log
>
> APK: https://github.com/rthorman/storytel-reading-log/releases/download/v0.2.0/storytel-reading-log.apk
>
> APK SHA-256: aad4f500f783d30b80982d194f0fe3ea26ff13f807f6ae2b5811df0692a045b6
>
> The non-debuggable prototype release currently uses a development signing certificate. We are disclosing this limitation rather than claiming established publisher verification. Please review whether the block is appropriate for this specific transparent, user-authorized notification use case.

Add the actual Android version, phone model, country/region, full warning body and any requested scan details before submitting. The title alone does not establish a complete device-specific diagnosis.
