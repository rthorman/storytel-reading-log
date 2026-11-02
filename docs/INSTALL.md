# Install on an Android phone

**[Download APK](https://github.com/rthorman/storytel-reading-log/releases/latest/download/storytel-reading-log.apk)** · **[Download ZIP](https://github.com/rthorman/storytel-reading-log/releases/latest/download/storytel-reading-log.zip)**

Requires Android 8.0 or newer. Storytel must be installed separately. No Storytel credentials are entered into this app.

## Install

1. Download the APK in your phone’s browser. If you downloaded the ZIP, extract it with Files and open the APK inside.
2. Tap the APK. If Android asks, enable **Allow from this source** for the browser or Files app. Return and tap **Install**.
3. Open **Storytel Reading Log**. You can disable the browser/Files installation permission afterward.

To transfer between phones, use **Files → Share → Quick Share** with Bluetooth and Wi-Fi enabled on both devices, or download directly on the receiving phone. Email services often block APKs and archives containing them.

## Set up automatic listening capture

1. In the journal, open **Settings → Open notification access**.
2. Select **Storytel reading log automation**, enable notification access and accept Android’s prompt. This permission technically allows notification access; this app checks only the supported Storytel package IDs.
3. If blocked, open Android **Settings → Apps → Storytel Reading Log → ⋮ → Allow restricted settings**. Authenticate if requested and try notification access again. Wording and availability vary by device.
4. In Android **Settings → Apps → Storytel Reading Log → Battery**, choose **Unrestricted**, **Allow background usage** or your device’s equivalent. Some manufacturers also have an **Auto-start** setting or a sleeping-app list.
5. Make sure notifications are enabled in Android **Settings → Apps → Storytel → Notifications**.

Notification access is separate from permission to install APKs. The journal itself does not need to post notifications.

## Test capture

Play an audiobook in Storytel and verify its playback notification is visible. Return to **Library**; a book should appear with today’s start date. Check **Settings → Last detected** too. “Notification access enabled” indicates permission rather than proof of recent successful capture.

Review the title and author. Add series and comments yourself, and set a finish date when you finish the whole book. Pause and resume: identical title/author metadata should match the existing book.

## Troubleshooting

- Check notification access, Storytel’s playback notification and the journal’s background battery permission.
- Open the journal once after installation or a force-stop, then play a book again.
- Reset Library search and filters if a captured book seems missing.
- After reproducing an issue, enable **Settings → Debug mode**, reproduce the issue and use **Export debug log**. Include Android/Storytel versions, approximate time and expected behavior. Review the file before sharing it. [Diagnostic guide](DIAGNOSTICS.md).

## Update without losing data

**Export a CSV backup before updating:** Settings → Export reading data. Save it outside the app and check that the file exists. [Backup guide](BACKUPS.md).

Install a new APK over the existing app. Android requires matching signing certificates. These early releases use prototype signing, so APKs built on separate clean machines can have different certificates and Android may reject an update.

If that happens, keep your backup safe. Only after exporting and verifying it should you uninstall the old app, install the new APK and use **Settings → Import reading data**. Uninstalling deletes the original reading database. The very first prototype did not support CSV export: do not uninstall that version if it contains history you need without first arranging a way to recover it. A debug log is not a full backup.

Disable notification access to stop capture. Uninstalling removes the journal’s private database and diagnostic files. There is no cloud restore.
