# Prototype verification

## v0.2.0

Code commit: `e4fd33958eb28a3b2082af0869608768e5c6cd40`.

[Successful GitHub Actions run](https://github.com/rthorman/storytel-reading-log/actions/runs/41525043514).

- Reading-date validation checks passed.
- All 13 JUnit/Robolectric tests passed: CSV quoting/multiline/Unicode handling and limits; complete backup/restore relationships and comments; rejection without mutation; transaction rollback; Unicode series fidelity; default-off debug mode; UI navigation, search, no-results reset, editor-draft preservation and small-screen/large-text scrolling.
- Android lint and `assembleRelease` passed.
- Packaged APK manifest inspected: version `0.2.0`, Android `debuggable=false`. Runtime Debug mode is independent of the APK variant and defaults off.
- UI captures in `docs/screenshots` come from native graphics rendering in Robolectric on Android API 35. Normal rendering uses 393 × 852 dp; the narrow-screen case uses 320 × 640 dp, with an additional 150% text-size capture. Captures were visually reviewed, and navigation wrapping at large text size was corrected.
- Repository visibility was confirmed public; GitHub recognizes the AGPL license. Release assets include the APK, a ZIP containing it, and SHA-256 checksums.

## Practical limits

No physical-device playback testing has been performed. Storytel metadata meanings, chapter behavior, notification delivery, battery restrictions and background capture need testing with the installed Storytel app on a real phone. System file-provider behavior and restricted-notification-access flows also need device testing. Automated backup tests exercise the actual SQLite data path, but do not simulate every Android storage provider or power-loss scenario.

This prototype uses development signing for its non-debuggable release variant. Long-term production signing is not yet configured; Android can reject updates between APKs produced with different development certificates. Export a CSV backup before reinstalling. The original v0.1 prototype had no CSV export, so do not uninstall it if it contains data you need without first arranging recovery.

## Original v0.1 work

The initial source compiled against Android API 35, date checks passed, and [the first APK build](https://github.com/rthorman/storytel-reading-log/actions/runs/41517828805) passed Android lint and `assembleDebug`. The owner subsequently created the repository and changed its visibility to public. v0.2 replaces always-on diagnostic recording with the default-off runtime switch.
