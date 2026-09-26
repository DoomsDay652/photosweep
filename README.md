# Photo Sweep

An offline Android photo review app. Pick a year and month, then swipe right to keep or left to request Android Trash. An exact duplicate gets a badge. The first run asks for photo permission; Android 14+ can grant access to selected photos. Photos stay in their existing folders.

## One-time GitHub setup

1. Unzip `PhotoSweep_Android_Source_v1.1.zip` on a computer. Upload **the contents** of the extracted folder (including `.github/workflows/android.yml`) to the root of your `photosweep` GitHub repository. Do not upload the ZIP as a single file.
2. Keep `PhotoSweep_Signing_Backup.zip` private. Unzip it locally. It contains `photosweep-signing.p12` and `github-secrets.txt`. **Never upload these two files to your repository**, especially if it is public.
3. In your GitHub repository, open **Settings → Secrets and variables → Actions → New repository secret**. Create these two secrets using the exact values in `github-secrets.txt`:
   - `PHOTO_SWEEP_KEYSTORE_BASE64`
   - `PHOTO_SWEEP_STORE_PASSWORD`
4. Open **Actions → Build Android APK → Run workflow** (or wait for the push build after the two secrets have been added). A successful run offers a `PhotoSweep-installable-apk` artifact. Download and unzip that artifact to get `app-release.apk`.
5. Transfer `app-release.apk` to your Android phone, tap it, and allow your browser or file manager to install apps if prompted.

If the first push ran before the secrets were added, it will fail safely. Run the workflow again after adding them. The workflow sets a higher Android version code from GitHub's run number automatically and uses the same signing key each time. Keep the backup ZIP safe: losing it prevents future builds from updating an already installed copy.

## Future updates

When the source changes, upload/commit the changed files to the same repository. The `push` workflow builds the next signed APK. Download the new `app-release.apk` artifact and tap it on the phone. Install **over** the existing app; do not uninstall first. The package ID and key stay the same and the version code increases automatically. Kept/reviewed progress remains in the app during an in-place update.

Uninstalling removes the app's local reviewed progress. It does not delete or restore your actual photos. Back up the signing ZIP separately; do not put it in the public repository.

## Behavior

Android confirms each Trash request. A cancelled request leaves the image in the deck. Duplicate badges mean byte-for-byte SHA-256 matches among photos the app can access, not merely similar-looking images. The year/month grouping uses the date taken, falling back to the date added. **Review this month again** resets reviewed marks for one month.

The app supports Android 11 or later. The build uses Gradle 8.12, Android Gradle Plugin 8.9.2, JDK 17, and Android SDK 35. It has no external runtime libraries.
