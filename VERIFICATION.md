# Prototype verification

- Reading-date validation checks passed using Eclipse Java Compiler 3.39.0 and Java 21 (Java 17 source level).
- All Android Java sources compiled against the Android API 35 signatures using that compiler. Four deprecation warnings relate to compatibility APIs used for Android 8+.
- An APK build, Android lint and physical-device testing have not been run. This environment initially provided a Java runtime without javac, Gradle or the Android SDK; the compiler and API signatures used for the checks were downloaded to temporary storage. GitHub Actions is configured for a full build when the repository is pushed.
- The owner created the private GitHub repository at https://github.com/rthorman/storytel-reading-log after the integration could not create it. The local project is connected to that repository for pushing and CI verification.

Notification field meanings, chapter behavior, background capture, debug export and UI behavior require the device acceptance checks in README.md. Automation cannot be considered verified until tested with actual Storytel playback.
