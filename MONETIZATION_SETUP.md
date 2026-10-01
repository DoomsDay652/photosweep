# Photo Sweep: account and support rollout

The current release works locally as Guest and has no ads or purchases. `AppServices` separates optional account identity, purchase ownership/prices and ad availability from photo review. The offline implementation is the only active provider. No accounts, payments, ad IDs or cloud photo uploads are simulated.

## Proposed products

| Product | Type | Behavior |
| --- | --- | --- |
| Remove ads | One-time, non-consumable | Removes all ad placements; restore ownership through Google Play |
| Small / medium / large support | Repeatable, consumable support purchases | Optional tips supporting development; no effect on photo access or theme unlocks |

Prices are still undecided. Read prices from Google Play ProductDetails so users see their local currency. Describe support as a tip/support purchase, not a charitable tax-deductible donation.

## Setup needed from the owner

1. Create and verify Google Play Console; register `com.dominic.photosweep`, set up a payments profile, and use an internal testing track. Preserve the app's signing identity and plan Play App Signing before production upload.
2. Configure the one-time product IDs and prices. Add Play Billing, restore owned purchases on foreground, handle pending/cancelled transactions, verify tokens on a backend, acknowledge Remove Ads, and consume support purchases only after verified fulfillment. Reconcile refunds/revocations. Never unlock Remove Ads using a local settings toggle.
3. Create AdMob app and placement IDs. Begin with official test ads. Obtain/update consent through UMP before requesting ads and provide privacy options when required. Hide every placement for verified Remove Ads owners. No ads over photo review, full-screen inspection or Trash/Restore controls; start with a home/month-list banner and avoid disruptive interstitials.
4. Create a Firebase project if Google sign-in is chosen, configure Credential Manager, register signing certificate fingerprints and supply app configuration. Accounts remain optional. Use provider identity rather than storing passwords in the app. Define exactly which progress/preferences can sync before adding cloud storage; photos stay on-device. Implement sign-out and account deletion before launch.
5. Complete the store listing, privacy disclosures and testing. Test reinstall/device change, purchase restoration, pending/offline/cancelled/refunded purchases, consent failure, sign-out and account deletion.

## Ready in this release

- Provider interfaces for account state, localized prices, Remove Ads ownership and consent/configuration gates.
- Guest-only implementation with unavailable purchases and ads disabled.
- A settings status describing upcoming optional accounts and support.
- Regression checks for the ad ownership/consent gates alongside review navigation and undo.

## Official implementation references

- Google Play Billing: https://developer.android.com/google/play/billing/integrate
- AdMob consent: https://developers.google.com/admob/android/privacy
- Firebase / Credential Manager Google sign-in: https://firebase.google.com/docs/auth/android/google-signin

Live SDK integrations and backend verification are the next stage after these provider accounts and product choices exist.

## Setup status visible in the app

Settings → Account & Support → Account & support details shows Guest behavior, support purchase plans, the one-time Remove Ads plan, and the missing provider configuration. It does not simulate checkout, ownership, purchase restoration, or sign-in. This APK remains offline with no payment collection or ad requests.
