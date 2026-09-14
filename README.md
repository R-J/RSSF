# RSSF

Native Kotlin Android client for the RSS Reader Engine API described in `openapi.json`.

## Current slice

- Compose UI with a dark Feedly Classic-inspired navigation drawer.
- Username/password authorization with persisted access and refresh tokens.
- Content snapshots for categories, feeds, and entries using DataStore for offline reading.
- Article list, refresh, search, starred/read-later navigation, and sign out shell.
- API methods for category/feed administration, entry updates, similar threads, OPML, and processing are defined in `ReaderApi` for the next UI slices.

## Local API mock

Debug builds use an in-memory `MockReaderDataSource` by default, so the interface can be tested without a server. It returns valid examples for the complete `ReaderApi` surface and keeps category, feed, article, search, star, and read-state changes during the app session. Use any non-empty server, username, and password on the login screen.

The repository depends on the `ReaderDataSource` interface rather than the Retrofit service directly. To use the real API, set `USE_LOCAL_MOCK` to `false` in `app/build.gradle.kts`; the Retrofit adapter then becomes the only implementation change required.

## Run

Set the API URL in `app/build.gradle.kts` by changing `BuildConfig.API_BASE_URL`, then open the project in Android Studio and run the `app` configuration. The repository does not include an Android SDK, emulator, or server URL, so an APK cannot be built in this container.

## Planned slices

1. Wire category/feed create, rename, reorder, delete, and feed refresh actions to the edit drawer.
2. Add article detail, mark-read/star actions, similar threads, and external sharing.
3. Add OPML import/export, account settings, refresh scheduling, and robust token refresh handling.
