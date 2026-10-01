# Photo Sweep: guided account and support setup

This release remains Guest-only with no active ads or checkout. It includes the Play Billing library for Play Console product setup, stable product IDs, offline provider interfaces, and signed APK/AAB outputs. Firebase sign-in, consent/ad adapters, checkout, purchase verification and entitlement restoration still need integration after the owner config exists. Reset account progress in Settings resets local progression only; it does not delete photos, clear Trash timers or create/delete a cloud account.

## 1. Create your developer account and app

Open https://play.google.com/console/signup using the Google account that should own Photo Sweep. Complete identity verification and the US$25 one-time registration yourself. Choose the account type matching your actual individual/business identity. Create **Photo Sweep**, language **English (United States)**, type **App**, pricing **Free**. In-app support and ad removal do not make the base app a paid download. Supply your own support contact and complete the declarations shown by Google.

Source: https://support.google.com/googleplay/android-developer/answer/6112435?hl=en

## 2. Prepare an internal test release

Use the supplied **Play Store AAB download** for Play Console; the APK is for direct phone installation. Package name is **com.dominic.photosweep**. The bundle targets API 36 and includes Play Billing 9.1.0. Upload to an internal testing release, follow the App Signing prompts, and complete the dashboard's required setup. Configure your payments profile before selling products.

The current downloaded APKs use our existing signing certificate. Decide the Play App Signing identity before distribution; using a different Google-generated signing key changes the certificate used for Play installs. Keep existing private signing credentials in repository secrets; never paste private keys into chat or commit them. Firebase must also receive the Play app-signing certificate fingerprint shown by Play Console, which can differ from the upload certificate.

Sources:
- https://support.google.com/googleplay/android-developer/answer/9859152?hl=en
- https://developer.android.com/google/play/billing/getting-ready
- https://support.google.com/googleplay/android-developer/answer/11926878

## 3. Set up optional Google accounts with Firebase

Open https://console.firebase.google.com/ and create a Photo Sweep project. Register an Android app with package **com.dominic.photosweep**. Add the release certificate fingerprints below. Enable **Google** in Authentication's sign-in providers, choose your support email, and download the updated **google-services.json** after enabling the provider. That Android client configuration is the file needed for the next integration; do not upload a service-account private key or share passwords.

Current APK signing certificate:

```text
SHA-1
79:BF:AD:12:9D:18:27:22:98:9F:6B:8E:DB:4E:15:75:CE:A6:C7:D0

SHA-256
43:08:29:8D:1D:E2:AD:22:5C:E9:ED:79:63:48:51:C4:97:65:8C:3A:D9:30:4B:0B:AA:52:69:58:A1:0C:A8:A8
```

Keep sign-in optional. Photos stay on-device. The next code stage connects Credential Manager to Firebase, adds sign-out and account deletion, and defines which progress/preferences can sync. Add the Play App Signing fingerprints too once the console supplies them.

Source: https://firebase.google.com/docs/auth/android/google-signin

## 4. Create support and Remove Ads products

After the Billing-enabled internal release is registered, open the one-time product section in Play Console and create these IDs. Prices below are suggestions for your choice, not hard-coded or live prices.

| Product ID | Product | Runtime behavior | Suggested US base price |
| --- | --- | --- | --- |
| remove_ads | Remove ads | One-time ownership; do not consume; restore via Play | $1.99 |
| support_small | Small support | Optional repeatable support purchase; consume after verified fulfillment | $0.99 |
| support_medium | Medium support | Optional repeatable support purchase; consume after verified fulfillment | $2.99 |
| support_large | Large support | Optional repeatable support purchase; consume after verified fulfillment | $4.99 |

Use support/tip wording. These support app development and do not unlock photo access or bypass theme progression. Display localized prices from ProductDetails. Before live checkout, implement server purchase-token verification, pending/cancelled states, acknowledgement/consumption, refunds/revocations and purchase restoration. Local progress reset must never remove verified paid ownership.

Sources:
- https://developer.android.com/google/play/billing/getting-ready
- https://developer.android.com/google/play/billing/integrate

## 5. Configure AdMob

Open https://admob.google.com/ and create the Android Photo Sweep app, initially marking it unpublished if it has no Play listing. Create a banner placement. Record the app ID and banner unit ID for integration. Start with official test ads; real ads remain disabled until consent, placement and verified Remove Ads ownership are connected. In Privacy & messaging, configure the applicable user messages for UMP. Keep review, full-screen inspection and Trash free of ad overlays.

Sources:
- https://developers.google.com/admob/android/quick-start
- https://developers.google.com/admob/android/privacy

## Where to start now

Complete step 1 first. The next useful checkpoint is a Play Console app dashboard for Photo Sweep. Then register the Firebase Android app and provide its updated google-services.json. Product prices, AdMob IDs and backend purchase verification can follow. No sign-in/payment/ad request is made by this APK merely because the libraries or setup guide are present.

## Updated app preparation

Email/password account flows are now implemented but disabled until configured. See `store/PLAY_RELEASE_READINESS.md` for the current instructions, including Firebase Email/Password, public privacy/deletion pages, CI configuration and release checks. Google sign-in remains a later integration. The account deletion and privacy website source is supplied in `store/public`; it is not deployed yet. Do not publish this internal-testing build as fully configured production.
