# Prototype verification

- Reading-date validation checks passed using Eclipse Java Compiler 3.39.0 and Java 21 (Java 17 source level).
- All Android Java sources compiled against the Android API 35 signatures using that compiler. Four deprecation warnings relate to compatibility APIs used for Android 8+.
- An APK build, Android lint and physical-device testing have not been run. This environment initially provided a Java runtime without javac, Gradle or the Android SDK; the compiler and API signatures used for the checks were downloaded to temporary storage. GitHub Actions is configured for a full build when the repository is pushed.
- Local Git repository is initialized and committed. GitHub creation was rejected with `Resource not accessible by integration (createRepository)` and HTTP 403. No remote repository was created.

Notification field meanings, chapter behavior, background capture, debug export and UI behavior require the device acceptance checks in README.md. Automation cannot be considered verified until tested with actual Storytel playback.
