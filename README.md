# Orpheum

Legacy Android music streaming app targeting **Android 2.1+ (API 7)**.

Built with the same architecture as Neostore (AsyncTask + HttpURLConnection, Activities, XML layouts), with feeds feature adapted from OrpheumUploader.

## Prerequisites

- Android Studio 2.3.2
- Android SDK 7 (minimum) / SDK 21 (target)
- JDK 8

## local.properties

Required (gitignored). Must define:

```
BASE_URL
IMG_BASE_URL
AUTH_BASE_URL
```

## Build

```sh
./gradlew assembleDebug
```
