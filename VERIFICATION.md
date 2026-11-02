# Prototype verification

- Reading-date validation checks passed using Eclipse Java Compiler 3.39.0 and Java 21 (Java 17 source level).
- All Android Java sources compiled against the Android API 35 signatures using that compiler. Four deprecation warnings relate to compatibility APIs used for Android 8+.
- GitHub Actions passed date validation, Android lint and `assembleDebug` for commit `2271d99`. Successful run: https://github.com/rthorman/storytel-reading-log/actions/runs/41517828805. Its `reading-log-debug` artifact contains the installable debug APK. Physical-device testing has not been run.
- The owner created the private GitHub repository at https://github.com/rthorman/storytel-reading-log after the integration could not create it. The local project is connected to that repository for pushing and CI verification.

Notification field meanings, chapter behavior, background capture, debug export and UI behavior require the device acceptance checks in README.md. Automation cannot be considered verified until tested with actual Storytel playback.
