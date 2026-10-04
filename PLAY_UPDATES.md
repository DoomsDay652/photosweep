# Google Play updates

Version 0.1.3 checks Google Play on launch and foreground return. For an eligible
newer release it opens Google's consent flow, preferring a flexible background
download. After download, Restart / Later lets the user choose when to install.
Interrupted immediate updates resume. Declining does not block photo review or
repeat the consent prompt during the same activity session (including rotation).
No Play connection or a sideloaded build stays usable without an error popup.

Publish a higher versionCode on the user's eligible track and install this build
through Google Play first. Existing builds without this integration cannot gain
the launch prompt until updated. Play decides availability, ownership, signing
and rollout eligibility; this cannot force installation or override store delay.

Validate on an opted-in tester's Play-installed copy, then publish a higher build:
launch to accept/decline, rotate after decline, complete the background download,
choose Later then restart on a later launch, and interrupt/resume an immediate
flow. Check offline and APK installs still review photos normally. Release CI
checks lint, signed APK/AAB, review/account logic and embedded crash mapping;
the live Play consent flow requires a real eligible newer release.

Official implementation and testing:
https://developer.android.com/guide/playcore/in-app-updates/kotlin-java
https://developer.android.com/guide/playcore/in-app-updates/test
