# Dashboard App

Android dashboard companion for Tryo Plugin Manager services.

## Branch policy

- `main`: stable production branch. Changes land here only after beta verification and approval.
- `dashboard-v2`: active beta and modular development branch.
- `backup/dashboard-v2-before-modular-20261113`: preserved pre-refactor snapshot.

## Android identity

- App label: Dashboard App
- Application ID / namespace: `com.tryo.dashboard`
- Firebase project: `tryo-plugin-notifications`

The Firebase project is intentionally reused. The Android client registration for `com.tryo.dashboard` must exist in `app/google-services.json`.

## Product modules (initial scope)

1. Plugin Manager — primary dashboard and plugin/update status.
2. Tracker — activity and event history.
3. Usage Data — Firestore usage estimates and storage indicators.

The app backend remains Google Apps Script as the API bridge, with Firestore as the data store. Do not replace Firestore with Google Sheets.

## Modularization approach

Keep the Android shell, authentication, API client, secure key storage, and FCM integration in shared core components. Feature screens and their data contracts are separated by module so they can be developed and tested independently.

Remote configuration and remotely hosted presentation assets can be updated without reinstalling the Android shell. Changes to native Kotlin/Android code still require a signed app update and Android installation approval; Android does not permit silently replacing native app code from GitHub.

## Build

GitHub Actions builds a debug APK on pushes to `main` and `dashboard-v2`, and supports manual runs from Actions. Beta builds must be tested before promotion to `main`.
