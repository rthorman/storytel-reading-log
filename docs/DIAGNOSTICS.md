# Diagnostics and privacy

**Debug mode is off by default.** In Settings, enable **Debug mode**, reproduce the problem, then use **Export debug log** and choose a location. Turn Debug mode off afterward to stop recording. Existing logs remain until cleared. The JSON Lines file contains one timestamped JSON object per event. For a clean trace, **Clear debug history**, reproduce the problem in Storytel and export. Clearing diagnostics keeps books and comments.

## Recorded events

- Recognized Storytel notification package, ID, post time, ongoing status and text/scalar extras.
- Complex extras are represented by type name. Tokens, images, pending intents and arbitrary parcelables are not serialized.
- Available media title, author, artist, album, playback state/position and duration.
- Capture/skip decisions, extracted book fields, record IDs, listener lifecycle, app startup and errors, including uncaught exceptions.
- Export header with app version, Android API and device manufacturer/model.

Only the two recognized Storytel packages are inspected. Events use UTC; book dates are local calendar dates. Two private rotating files retain approximately 2 MiB of recent events. Logs stay on-device until exported; nothing uploads automatically.

## Report a problem

Include Android and Storytel versions, approximate time, steps, expected result and actual result. **Logs contain personal book titles, authors and notification text.** Public GitHub issues are visible to everyone. Review and redact a copy before attaching it, preserving event types/timestamps/field names needed for investigation.

Debug logs are not backups. Use **Settings → Export reading data** for a complete [CSV backup](BACKUPS.md). Storage failure or native crashes may prevent events being written; a complete audit trail is not guaranteed.
