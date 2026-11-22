# Orpheum

Legacy Android music streaming app targeting **Android 1.5+ (API 3, Cupcake)** — practically exercised on Eclair / Froyo / Gingerbread.

Built with the same feature-first MVP architecture as HoloMarket (Activity implements a View contract, Presenter owns state/networking/parsing; AsyncTask + HttpURLConnection, ListView + ArrayAdapter), with feeds adapted from OrpheumUploader.

## Features

- Auth (login, register, password reset) with password visibility toggle
- Home feed with recent albums and recent tracks (`GET /home`)
- New tracks / new releases feeds, search, album detail
- Streaming playback with queue, seek, previous/next, spinning album art, a queue dialog, a background notification, and a persistent Now Playing bar
- Offline mode: download an album (tracks + cover) and play it from disk; downloads are managed from the home screen's Downloads list
- Internet radio: browse and search TuneIn stations (via `opml.radiotime.com`), play them as live streams in the same player, and keep favorite stations locally

## Prerequisites

- JDK 8 (Gradle 3.5 will not run on newer JDKs)
- Android SDK platform 21 + build-tools 25.0.0
- Android Studio 2.3.2 optional (CLI builds work)

## local.properties

Required (gitignored). Bare values only — no quotes, no `/orpheum` suffix:

```
BASE_URL=<api-host>
FILE_BASE_URL=<storage-host>
AUTH_BASE_URL=<api-host>
```

`BASE_URL` is the host; the app appends `/orpheum` (catalog/stream/media) itself. (A trailing `/orpheum` is stripped defensively, but set the host only.)

## Build

```sh
export JAVA_HOME=/path/to/jdk8
./gradlew test assembleDebug
```

AGP 2.3.2 is resolved from JCenter mirrors (Aliyun + Huawei Cloud) configured in the root `build.gradle` — plain `jcenter()` no longer works.

## CI / Releasing

- **CI** (`.github/workflows/ci.yml`) — builds a debug APK and runs the unit tests on pushes and pull requests to `main`/`master`. The APK is uploaded as the `orpheum-debug` workflow artifact.
- **Release** (`.github/workflows/release.yml`) — runs on `v*` tags and via manual dispatch. It builds a release APK, verifies the signature, uploads an artifact, and attaches `Orpheum-<tag>.apk` to the GitHub Release. Tags of the form `v<versionName>.<suffix>` (e.g. `v1.0.1`) are accepted as test builds and published as **prereleases**.

Both workflows inject `local.properties` from repository secrets (Settings → Secrets and variables → Actions), using bare URLs without quotes (the workflow adds the quotes):

```
BASE_URL=<api-host>      # host only — no /orpheum
FILE_BASE_URL=<storage-host>
AUTH_BASE_URL=<api-host>
KEYSTORE_BASE64          # base64 of ~/.android/debug.keystore (stable signing)
```

Release flow:

1. Bump `versionCode` / `versionName` in `app/build.gradle` for a real release.
2. Commit and push to `main`.
3. `git tag vX.Y.Z && git push origin vX.Y.Z`.
4. The workflow builds `Orpheum-vX.Y.Z.apk` and attaches it to the GitHub Release.

> Release APKs use the project debug key (`signingConfig signingConfigs.debug`), seeded in CI from the `KEYSTORE_BASE64` secret, so every build shares one stable signature. Installing over a build made before v1.2.1 requires one uninstall; afterwards updates work normally as long as `versionCode` increases.
