# Tryo Plugin Notifications (Android)

Native Android companion app for Tryo Plugin Manager push notifications using Firebase Cloud Messaging.

## Android application ID

`com.tryofp.pluginnotifications`

Register the Firebase Android app with this exact package name.

## Firebase setup

1. Open Firebase Console and select project `tryo-plugin-notifications`.
2. Add an Android app with package name `com.tryofp.pluginnotifications`.
3. Download `google-services.json`.
4. Put it at `app/google-services.json` in this repository.
5. Commit and push it. This is client configuration, not a service-account private key. Never put a service-account JSON/private key in this app or repository.

GitHub Actions builds a debug APK on every push to `main`, and can also be started manually from Actions.

## Included

- Firebase Cloud Messaging Android SDK.
- Android 13+ notification permission request.
- High-importance notification channel `tryo_updates`.
- FCM token registration to the existing Apps Script `FcmRegister` endpoint.
- Token refresh registration.
- No Firebase service-account credential in the APK.

## Backend follow-up

The Apps Script sender still needs Android-specific channel/priority configuration for native tokens while preserving the existing Web Push configuration for PWA tokens.
