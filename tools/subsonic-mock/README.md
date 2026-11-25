# Subsonic mock server

A throwaway, dev-only Subsonic/OpenSubsonic server for testing Orpheum without
Navidrome, LMS, Docker, or any backend changes.

It is **not** part of Orpheum's backend and **not packaged in the APK**. It
serves canned JSON, generated cover images, and a short WAV tone. It accepts any
username/password and does **not** validate tokens.

## Requirements

- Python 3.7+ (standard library only)

## Start

From the repository root:

```sh
python3 tools/subsonic-mock/mock_server.py
```

Defaults:

| Flag | Default | Notes |
|------|---------|-------|
| `--host` | `127.0.0.1` | Use `0.0.0.0` for a physical device |
| `--port` | `8080` | Any free port works |

Examples:

```sh
python3 tools/subsonic-mock/mock_server.py --port 8080
python3 tools/subsonic-mock/mock_server.py --host 0.0.0.0 --port 8080
```

Stop it with `Ctrl+C`.

## Sanity check

```sh
curl "http://127.0.0.1:8080/rest/ping.view?u=demo&f=json"
```

Expected:

```json
{"subsonic-response": {"status": "ok", "version": "1.16.1"}}
```

## Point Orpheum at it

Build and install the debug APK:

```sh
./gradlew assembleDebug
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/app-debug.apk
```

Open Orpheum → **Connect Subsonic server**:

| Field | Emulator | Physical device |
|-------|----------|-----------------|
| Server URL | `http://10.0.2.2:8080` | `http://<mac-lan-ip>:8080` |
| Username | `demo` (anything) | `demo` (anything) |
| Password | `demo` (anything) | `demo` (anything) |

For a physical device, start the mock with `--host 0.0.0.0`, keep both devices on
the same network, and allow the port through the macOS firewall if prompted.
Alternatively, use `adb reverse tcp:8080 tcp:8080` and connect to
`http://127.0.0.1:8080`.

## Test checklist

| Step | Expected |
|------|----------|
| Connect | Home shows `Welcome demo!`, 3 recent albums with covers, recent tracks |
| **New Releases** | `Mock Sunrise`, `Mock Night Drive`, `Mock Static` |
| Album detail | Cover, year, 3 tracks (2 for `Mock Static`), 0:08 durations |
| Play a track | Player opens, progress moves, seek works, auto-advance works |
| **New Tracks** | Tracks flattened from the newest albums (8 in the mock library) |
| Search `night` | `Mock Night Drive 1–3` |
| Download an album | Button becomes **Delete download**; Downloads shows the album |
| Settings | Source = Subsonic server, host shown, version/storage summary |
| **Switch source** | Dark dialog with Orpheum/Subsonic options; switching navigates Home |
| **Clear cover cache** | Toast confirms; feed covers reload from the mock |
| Offline | Stop the mock, open Downloads → album → playback still works from disk |
| Disconnect server | Settings button disconnects; Home returns to logged-out |

## Supported endpoints

| Method | Purpose |
|--------|---------|
| `ping` | Connection test |
| `getAlbumList2` | Newest albums (canned list) |
| `getAlbum` | Album + songs |
| `search3` / `search2` | Song search with `songOffset`/`songCount` |
| `getCoverArt` | Generated PNG cover |
| `stream` | 8-second WAV tone, with HTTP Range support for seeking |

Auth parameters (`u`, `t`, `s`, `v`, `c`, `f`) are accepted but ignored.

## Limitations

- No authentication validation; never use real credentials.
- No MP3 transcoding: `stream` returns WAV even when the client asks `format=mp3`.
- No playlists, starring, scrobbling, artists, or `getMusicFolders`.
- HTTP only; no TLS behavior is exercised.
- Not a compatibility guarantee for Navidrome, LMS, or Supysonic. Test those
  against a real server before release announcements.

## Reset

- Disconnect in Settings, or
- `adb uninstall id.neotica.orpheum`, or
- `adb shell pm clear id.neotica.orpheum`

## Security

The server binds to `127.0.0.1` by default. It has no auth and is only for local
development — do not expose it to untrusted networks or deploy it.
