# Updating Photo Sweep testers

1. Build the latest main commit with the existing Build Android APK workflow.
2. Confirm that the workflow succeeded. Download `PhotoSweep-play-store-bundle`
   from its Artifacts section and extract `app-release.aab` from the ZIP.
3. Open Photo Sweep in Play Console, then Test and release > Testing >
   Internal testing > Create new release.
4. Upload the new AAB, add a release name and notes, click Next, resolve any
   errors, and publish/start rollout to internal testing. If Play requests review,
   send the changes for review from Publishing overview.
5. Keep the existing tester email list selected. Existing opted-in testers use
   the same testing link and can update through Google Play; they do not need
   another invitation or an uninstall. Availability can take time to propagate.

For 0.1.1, enable Dreamy Sweep music in Options > Audio after updating.
Use RELEASE_NOTES_0.1.1.txt for this release's notes.

Version names use prerelease numbers: 0.1.0, 0.1.1, 0.1.2; a larger feature set
can become 0.2.0. Reserve 1.0.0 for the first production-ready release.
The workflow sets Android versionCode to its increasing GITHUB_RUN_NUMBER.
Every uploaded bundle needs a new, higher versionCode. Keep the package name,
Play app signing setup and private upload key unchanged for updates.

Use the APK artifact only for direct installation outside Google Play.
Testers who installed from Google Play should receive updates through the
internal testing track. Upload the AAB, not the ZIP or APK, to Play Console.
