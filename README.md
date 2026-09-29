# Photo Sweep

An offline Android photo review app. Browse by **year → month → photos**. Drag the whole photo left to Trash or right to Keep. Exact byte-for-byte duplicates show a Duplicate bubble.

## Install and update

1. Open **Actions → Build Android APK** in this repository and choose the newest successful run.
2. Under **Artifacts**, download `PhotoSweep-installable-apk`. Unzip it to get `app-release.apk`.
3. Open `app-release.apk` on your Android phone and install it. Android may ask you to allow installs from your browser or Files app.

For updates, install the newer APK **over** the existing app. The GitHub workflow increases its version code and signs with the same private key. There is no need to uninstall or change code. Keep the signing backup private; never upload it to this public repository.

## Photo access and Trash

First grant photo access. To avoid a system confirmation on every left swipe, tap **Enable prompt-free Trash** and grant Android's **Manage media** special access once. This is a powerful permission: it lets Photo Sweep move and delete photos you allow it to access without individual prompts. If the grant is absent, a left swipe opens its setup and leaves the photo untouched. Prompt-free Trash requires Android 12 or newer.

**Recently trashed** lists the latest 20 photos moved by Photo Sweep. Tap **Restore** to return one to the gallery. The app permanently deletes a trashed photo when it is over seven days old, and deletes the oldest when a 21st photo enters its Trash. Cleanup runs when the app opens and from an Android periodic job. Android can defer background jobs, so the seven-day mark is a threshold rather than an exact clock time. If Manage media is revoked, cleanup waits for access to return. The system Trash can also contain photos from other apps; Photo Sweep only tracks its own actions.

Keep marks a photo reviewed without changing the original. Uninstalling removes the app's review and Trash list, but does not automatically restore photos in the system Trash. Android's own Trash expiry may still apply. Duplicate badges are based on SHA-256 hashes of photos currently accessible to the app.

## Build

The workflow uses Java 17, Gradle 8.12 and Android SDK 35. It reads the private signing key from the existing `PHOTO_SWEEP_KEYSTORE_BASE64` and `PHOTO_SWEEP_STORE_PASSWORD` repository secrets. The package ID stays `com.dominic.photosweep` for in-place updates.
