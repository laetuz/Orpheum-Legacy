# YouTube mock server

A dev-only **Invidious/Piped** mock for Orpheum's YouTube audio screen. It does
not talk to YouTube and is not part of Orpheum's backend or APK.

It serves canned search results, audio-format JSON, covers, and a generated
audio file so the search → resolve → play → next flow can be tested locally.

## Requirements

- Python 3.7+ (standard library)
- macOS `afconvert` for AAC/M4A output; without it the audio endpoint falls
  back to WAV while still advertising `audio/mp4` (may not play on every device)
- `ffmpeg` for the `/muxed/{id}` progressive MP4 test candidate; without it the
  muxed endpoint falls back to M4A bytes

## Start

```sh
python3 tools/youtube-mock/mock_server.py --port 8090
```

Defaults advertise `http://10.0.2.2:8090` so the emulator can reach it.

Physical device:

```sh
python3 tools/youtube-mock/mock_server.py --host 0.0.0.0 --advertise-base http://<mac-lan-ip>:8090
```

## Point Orpheum at it

1. Open **Settings → YouTube**
2. Set **Instance list URL** to:

```
http://10.0.2.2:8090/instances.json
```

3. Tap **Save YouTube instances**
4. Home → **YouTube** → search `mock`
5. Tap a result: it resolves the audio URL and opens the player

For `adb reverse`, run `adb reverse tcp:8090 tcp:8090` and start the mock with
`--advertise-base http://127.0.0.1:8090`; use
`http://127.0.0.1:8090/instances.json` in Settings.

## Test checklist

| Step | Expected |
|------|----------|
| Save instance list | Settings status shows `1 Invidious, 1 Piped` after a search |
| Search `mock` | 3 results with generated covers and 0:08 durations |
| Play first result | “Preparing audio…” → player opens and progress advances |
| Next / auto-advance | Later queue tracks resolve lazily and play |
| Seek | The mock supports HTTP Range on its audio endpoint |
| Muxed candidate | `/muxed/{id}` serves a generated MP4 with H.264 + AAC when `ffmpeg` is available; this is the Gingerbread strip path |
| Stop the mock | New searches fail; an already-resolved queue continues |
| Bad search | Empty-state text appears |

## Supported endpoints

| Path | Purpose |
|------|---------|
| `/instances.json` | Invidious/Piped instance list |
| `/api/v1/search` | Invidious search |
| `/api/v1/videos/{id}?local=true` | Invidious adaptive formats |
| `/search?filter=videos` | Piped search |
| `/streams/{id}` | Piped audio streams |
| `/vi/...` | Generated PNG cover |
| `/audio/{id}` | Generated AAC/M4A (or WAV fallback), Range-capable |
| `/muxed/{id}` | Generated progressive MP4 (video + AAC) when `ffmpeg` is available; M4A fallback |

## Limitations

- No authentication; any query works.
- No real YouTube content, playlists, channels, or pagination.
- AAC/M4A is generated locally with `afconvert`; WAV fallback may not play.
- Not a compatibility guarantee for public Invidious/Piped instances.
- The real YouTube screen uses unofficial third-party instances and is subject
  to YouTube's Terms of Service; this mock does not exercise that risk.
