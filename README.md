# Orpheum

Legacy Android music streaming client with `minSdkVersion 3` (**Android 1.5+ / API 3, Cupcake**) — practically exercised on Eclair / Froyo / Gingerbread.

Built for vintage Android hardware locked out of modern streaming apps and TLS/SSL requirements, Orpheum speaks plain HTTP/JSON and plays catalog streams, offline downloads, and live internet radio through Android's built-in `MediaPlayer` as part of the Neotica retro ecosystem.

## Features

- **Home feed** — recent albums, recent tracks (`GET /orpheum/home`), and quick search
- **Browse & search** — paginated New Tracks and New Releases feeds, keyword search, and album detail with tracklist
- **Streaming playback** — queue playback with auto-advance, queue dialog, seek bar, previous/next, spinning circular album art, and a persistent Now Playing bar
- **Background audio service** — foreground `AudioService` with an ongoing notification (Play/Pause & Next controls on API 16+, reflection-based foreground fallback down to API 3)
- **Offline mode** — download albums (tracks + cover) via foreground `DownloadService` to `<external>/Orpheum/downloads/` (with internal storage fallback), automatic offline playback, and a Downloads screen accessible from Home
- **Internet radio** — browse and search TuneIn stations (via `opml.radiotime.com`) with M3U/PLS playlist resolution, live stream playback in the same player, and local favorite stations
- **Authentication & settings** — login, registration, and password reset with password-visibility toggles, JWT stored in `SharedPreferences`, and profile/logout
- **Trackball / D-pad navigation** — focusable rows with an orange selection highlight
- **Cover caching** — memory and disk caching via Universal Image Loader
- **CrashCatcher** — saves the last crash stacktrace and shows it on next launch

## Downloads

Download the latest APK from [GitHub Releases](https://github.com/laetuz/Orpheum-Legacy/releases/latest) (or browse all builds on the [releases](https://github.com/laetuz/Orpheum-Legacy/releases) page).

> Release APKs use one stable project key (`signingConfig signingConfigs.debug`), seeded in CI from `KEYSTORE_BASE64`. Upgrading from a build made before **v1.2.1** requires one uninstall; afterwards updates install cleanly in place as long as `versionCode` increases.

## Architecture

Built with the same feature-first MVP architecture as [HoloMarket](https://github.com/laetuz/HoloMarket) (see [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)), backed by [orpheum-api](https://github.com/laetuz/orpheum-api) — plain `AsyncTask` + `HttpURLConnection`, `ListView` + `ArrayAdapter`, `SdkCompat`, and Universal Image Loader `1.9.5` with no Support Library / AppCompat dependencies.

## Prerequisites

- JDK 8 (Gradle 3.5 will not run on newer JDKs)
- Android SDK platform 21 + build-tools 25.0.0
- Android Studio 2.3.2 optional (CLI builds work)

## local.properties

Required (gitignored). Must define quoted string values (e.g. `KEY="..."`) because `app/build.gradle` injects them directly into `BuildConfig`. Set `BASE_URL` to the host only — no `/orpheum` suffix (the app appends `/orpheum` itself, and defensively strips a trailing `/orpheum`):

```
BASE_URL
FILE_BASE_URL
AUTH_BASE_URL
```

## Build

```sh
export JAVA_HOME=/path/to/jdk8
./gradlew test assembleDebug
```

AGP 2.3.2 is resolved from JCenter mirrors (Aliyun + Huawei Cloud) configured in the root `build.gradle` — plain `jcenter()` no longer works.

## CI / Releasing

- **CI** (`.github/workflows/ci.yml`) — builds a debug APK and runs the unit tests on pushes and pull requests to `main`/`master`. The APK is uploaded as the `orpheum-debug` workflow artifact.
- **Release** (`.github/workflows/release.yml`) — runs on `v*` tags and via manual dispatch. It builds a release APK, verifies the signature, uploads an artifact, and attaches `Orpheum-<tag>.apk` to the GitHub Release. Tags of the form `v<versionName>.<suffix>` (e.g. `v1.4.2.1`) are accepted as test builds and published as **prereleases**.

Both workflows inject `local.properties` from repository secrets (Settings → Secrets and variables → Actions), using bare URL values without quotes (the workflows add the quotes). `KEYSTORE_BASE64` (base64-encoded `~/.android/debug.keystore`) is required by `release.yml` for stable signing and optional in `ci.yml` (which falls back to a throwaway debug key):

```
BASE_URL
FILE_BASE_URL
AUTH_BASE_URL
KEYSTORE_BASE64
```

Release flow:

1. Bump `versionCode` / `versionName` in `app/build.gradle` for a real release.
2. Commit and push to `main`.
3. `git tag vX.Y.Z && git push origin vX.Y.Z`.
4. The workflow builds `Orpheum-vX.Y.Z.apk` and attaches it to the GitHub Release.

## License

- **Code** — GNU General Public License v3.0 (see [`LICENSE`](LICENSE)).
- **Documentation** (`docs/`) — MIT License (see [`docs/LICENSE`](docs/LICENSE)), so the architecture guide stays reusable.
- **Third-party components** — see [`THIRD_PARTY_NOTICES`](THIRD_PARTY_NOTICES).
- **Brand** — "Orpheum" / "Neotica.id" names and logos are trademarks of Neotica.id; see [`TRADEMARKS.md`](TRADEMARKS.md).
