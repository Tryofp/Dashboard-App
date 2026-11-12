# Modular Architecture

## Product scope

The first release has exactly three user-facing feature areas:

- `plugin-manager` — primary landing area; plugin status, update state, and controls.
- `tracker` — activity/event history.
- `usage-data` — Firestore usage estimates, operation counters, and storage metrics.

## Boundaries

- `app`: Android entry point, navigation host, app lifecycle, and platform permissions.
- `core/network`: Apps Script HTTP transport and typed API contracts.
- `core/security`: encrypted local credential storage.
- `core/notifications`: FCM registration and notification channel/service integration.
- `feature/plugin-manager`: primary feature UI and its state.
- `feature/tracker`: tracker data presentation and pagination.
- `feature/usage-data`: Firestore estimate presentation and refresh lifecycle.

Feature modules must not call Firestore directly from the Android client. The Apps Script bridge remains the controlled API boundary; Firestore remains the database. Google Sheets is not part of this architecture.

## Remote update strategy

The app reads a versioned module manifest from GitHub. The manifest controls module availability, labels, compatible minimum app version, and remote presentation bundle versions. This permits safe UI/config/content updates without reinstalling the Android shell.

Native Android/Kotlin changes, permission changes, SDK changes, and new native capabilities require a new APK and Android's normal installation confirmation. The app must never silently execute arbitrary downloaded DEX/JAR code. Remote bundles are restricted to reviewed web assets/configuration and must be fetched over HTTPS with a pinned repository/release source.

## Compatibility and rollout

1. Keep `main` stable.
2. Develop on `dashboard-v2`.
3. Keep the pre-refactor snapshot branch intact.
4. Build and test beta before promotion.
5. Do not delete or migrate production Firestore data as part of UI refactoring.
