# Photo Sweep Play release preparation — October 1, 2026

This is an internal-testing build, not a claim of Play approval or complete production readiness. Google reviews the final APK/AAB, listing, declarations and developer identity. Ads and checkout remain disabled.

## Implemented in the app

- API 36 target. Android photo permission requests are initiated by the user after an explanation; Android 14+ selected-photo access is supported. Legacy read permission stops at API 32.
- Android confirmations work for Trash and Restore without Manage media. Special media-management access is optional and separately explained, including permanent expiry/cap cleanup. Android Settings access and re-selection are available. Access is refreshed after resuming.
- No photo uploads or cloud progress sync. Backups are disabled to keep media identifiers, history and authentication state out of Android app backups. Network traffic requires HTTPS.
- Admin mode and saved Admin preferences remain available in this development build at the owner’s request. Remove the Admin controls before public Play Store release. Reset local progress is separate from cloud account deletion and preserves photos, Trash and future purchase ownership.
- Recognizable superhero/anime theme backgrounds have been removed from the repository and binary; their catalogue entries are filtered. Original country, elemental, balance and abstract themes remain. Original assets still need a final human rights review before release; generated art is not proof of licensing.
- Configurable Firebase Email/Password creation, sign-in, email verification/resend/refresh, password reset, sign-out and deletion. Deletion requires password reauthentication and only announces success after Firebase confirms it. Password inputs are not saved in view state or application preferences.
- Guest access remains available. Account requests are disabled unless Firebase Android config and both public HTTPS policy/deletion URLs are present. No real accounts can be tested until the owner creates the project.
- Privacy controls are accessible without granting photo access. The external deletion page signs in against Firebase then permanently deletes that same authentication account. It has no analytics, credential storage or application database. If account-linked cloud storage is added later, expand both deletion flows to delete that data first.

## Owner configuration still required

1. Choose a dedicated developer/studio identity and public support inbox. Fill the explicit placeholders in `store/public/privacy.html`. Do not publish the draft text or pretend a contact address exists.
2. Create the Firebase project and Android app `com.dominic.photosweep`. Enable **Email/Password** in Authentication. Enable email enumeration protection, configure a password policy of at least eight characters, and review abuse protections/quotas and email templates. Google sign-in is not implemented by this update.
3. Place the Android `google-services.json` in `app/` locally, or add its JSON as repository secret `PHOTO_SWEEP_FIREBASE_ANDROID_CONFIG` for CI. It is client configuration, not a private service-account key. Never upload an Admin SDK service-account key to the Android app.
4. Copy `store/public/account-config.example.json` to `account-config.json` and fill the Firebase Web API key from the same project. Use an appropriate browser API key/restrictions, not an Android-only restricted key. Verify only authentication APIs are available and follow Firebase key restrictions guidance.
5. Deploy `store/public` to a chosen HTTPS host (Firebase Hosting configuration is supplied in `store/firebase.json`). Check the privacy/deletion pages work from a private browser without the app installed. No site has been deployed by this change.
6. Set repository variables `PHOTO_SWEEP_PRIVACY_URL` and `PHOTO_SWEEP_DELETION_URL` to those deployed pages. CI passes them as Gradle properties. Local builds can use `-PprivacyUrl=... -PdeletionUrl=...`. Build with `-PplayProduction=true` to enforce the basic configuration gate. The gate checks configuration, not web content or Google approval.
7. Test a real account: create, verify email, sign out/in, reset password, delete in-app with incorrect and correct credentials, confirm login no longer works, then repeat external deletion. Test poor/offline network and rotation. Check Firebase Authentication confirms removal. No production account was created/deleted during this work.

## Play Console declarations and release verification

- Submit an accurate photo-access declaration: core photo-library management/review requires persistent photo access. Provide a clear permission-to-review demonstration video if requested. Selected access remains useful; permission denial must not block privacy/account controls.
- Complete the Data safety form using the **actual configured build**. Device-only photos are not uploaded; enabled Firebase accounts add email/authentication/service data. Do not declare "no collection" for an account-enabled build without auditing Firebase's current disclosures. No analytics or AdMob SDK is included now.
- Add the deployed privacy policy and account deletion URL. Complete target audience, content rating, app access/reviewer instructions and truthful ads declarations. Do not advertise unimplemented accounts, ads or purchases.
- New personal developer accounts require the applicable closed test (currently at least 12 testers opted in continuously for 14 days) and production-access approval. Developer/device verification and console identity/payment obligations are separate from app code.
- Test permissions denied/partial/full/revoked; Android 11 through current Android; Samsung and another vendor; portrait/landscape; giant/panoramic photos; canceled Trash/Restore prompts; restored gallery photos protected from cleanup; expiry/cap behavior; reinstall; memory/performance/accessibility with TalkBack and enlarged fonts. Test on disposable photos.
- Review Android lint and Play pre-launch reports. Resolve relevant findings before production. Review all store artwork and text for rights/accuracy. Public reset must not revoke paid entitlements.
- Future ads require UMP/consent, test ads and updated privacy disclosures. Future purchases require Play products, localized prices, pending payment handling, acknowledgement/consumption, restoration, refunds and trusted purchase verification. No fake ownership or live checkout is shipped.

## Official references

- https://support.google.com/googleplay/android-developer/answer/10144311
- https://support.google.com/googleplay/android-developer/answer/13327111
- https://support.google.com/googleplay/android-developer/answer/16935362
- https://support.google.com/googleplay/android-developer/answer/9888072
- https://support.google.com/googleplay/android-developer/answer/14151465
- https://firebase.google.com/docs/auth/android/password-auth
- https://firebase.google.com/docs/auth/android/manage-users
- https://firebase.google.com/docs/reference/rest/auth
