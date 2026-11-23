#!/usr/bin/env python3
"""Dev-only Subsonic/OpenSubsonic mock server.

Usage:
    python3 tools/subsonic-mock/mock_server.py [--port 8080]

Point the app at http://10.0.2.2:<port> from an Android emulator, with any
username/password. Authentication parameters are accepted but not validated.
The server exposes ping, getAlbumList2, getAlbum, search3/search2, getCoverArt
and stream. Stream returns a short WAV tone; real servers transcode MP3.
"""

import argparse
import io
import json
import math
import struct
import wave
import zlib
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

VERSION = "1.16.1"
SAMPLE_RATE = 8000
DURATION = 8

ALBUMS = [
    {
        "id": "album-1",
        "name": "Mock Sunrise",
        "artist": "Orpheum Test",
        "artistId": "artist-1",
        "coverArt": "cover-1",
        "songCount": 3,
        "duration": DURATION * 3,
        "created": "2026-01-03T10:00:00Z",
        "year": 2026,
    },
    {
        "id": "album-2",
        "name": "Mock Night Drive",
        "artist": "Orpheum Test",
        "artistId": "artist-1",
        "coverArt": "cover-2",
        "songCount": 3,
        "duration": DURATION * 3,
        "created": "2026-01-02T10:00:00Z",
        "year": 2026,
    },
    {
        "id": "album-3",
        "name": "Mock Static",
        "artist": "Another Artist",
        "artistId": "artist-2",
        "coverArt": "cover-3",
        "songCount": 2,
        "duration": DURATION * 2,
        "created": "2026-01-01T10:00:00Z",
        "year": 2025,
    },
]

SONGS = []


def _song(song_id, album_id, title, artist, track, cover, created):
    return {
        "id": song_id,
        "parent": album_id,
        "albumId": album_id,
        "isDir": False,
        "title": title,
        "album": next(a["name"] for a in ALBUMS if a["id"] == album_id),
        "artist": artist,
        "track": track,
        "year": next(a["year"] for a in ALBUMS if a["id"] == album_id),
        "genre": "Test",
        "coverArt": cover,
        "size": 128000,
        "contentType": "audio/wav",
        "suffix": "wav",
        "duration": DURATION,
        "bitRate": 128,
        "path": "%s/%02d %s.wav" % (album_id, track, title),
        "created": created,
    }


for _index, _album in enumerate(ALBUMS):
    for _track in range(1, _album["songCount"] + 1):
        SONGS.append(_song(
            "song-%d-%d" % (_index + 1, _track),
            _album["id"],
            "%s %d" % (_album["name"], _track),
            _album["artist"],
            _track,
            _album["coverArt"],
            "2026-01-%02dT10:0%d:00Z" % (3 - _index, _track),
        ))

WAV_BYTES = None


def wav_bytes():
    global WAV_BYTES
    if WAV_BYTES is not None:
        return WAV_BYTES
    buffer = io.BytesIO()
    writer = wave.open(buffer, "wb")
    writer.setnchannels(1)
    writer.setsampwidth(2)
    writer.setframerate(SAMPLE_RATE)
    frames = bytearray()
    for i in range(SAMPLE_RATE * DURATION):
        value = int(0.2 * 32767 * math.sin(2 * math.pi * 440 * i / SAMPLE_RATE))
        frames.extend(struct.pack("<h", value))
    writer.writeframes(bytes(frames))
    writer.close()
    WAV_BYTES = buffer.getvalue()
    return WAV_BYTES


def png_chunk(kind, data):
    return (struct.pack(">I", len(data)) + kind + data
            + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF))


def cover_png(color):
    width = 128
    height = 128
    rows = []
    for y in range(height):
        row = bytearray([0])
        for x in range(width):
            row.extend((
                (color[0] + x) % 256,
                (color[1] + y) % 256,
                (color[2] + (x + y) // 2) % 256,
            ))
        rows.append(bytes(row))
    raw = b"".join(rows)
    return (b"\x89PNG\r\n\x1a\n"
            + png_chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
            + png_chunk(b"IDAT", zlib.compress(raw, 9))
            + png_chunk(b"IEND", b""))


COVERS = {
    "cover-1": cover_png((220, 80, 40)),
    "cover-2": cover_png((40, 90, 200)),
    "cover-3": cover_png((60, 170, 90)),
}


def ok(payload=None):
    response = {"status": "ok", "version": VERSION}
    if payload:
        response.update(payload)
    return {"subsonic-response": response}


def failed(message):
    return {"subsonic-response": {
        "status": "failed",
        "version": VERSION,
        "error": {"code": 0, "message": message},
    }}


class Handler(BaseHTTPRequestHandler):
    server_version = "OrpheumMock/1.0"

    def log_message(self, fmt, *args):
        print("%s %s" % (self.client_address[0], fmt % args))

    def do_HEAD(self):
        self._handle(head=True)

    def do_GET(self):
        self._handle(head=False)

    def _handle(self, head):
        parsed = urlparse(self.path)
        query = parse_qs(parsed.query)
        path = parsed.path
        if not path.startswith("/rest/"):
            self._send_json(failed("Not a Subsonic endpoint"), 404, head)
            return
        method = path[len("/rest/"):]
        if method.endswith(".view"):
            method = method[:-len(".view")]

        if method == "ping":
            self._send_json(ok(), 200, head)
            return
        if method == "getAlbumList2":
            self._album_list(query, head)
            return
        if method == "getAlbum":
            self._album(query, head)
            return
        if method in ("search3", "search2"):
            self._search(method, query, head)
            return
        if method == "getCoverArt":
            self._cover(query, head)
            return
        if method == "stream":
            self._stream(head)
            return
        self._send_json(failed("Unsupported mock method: %s" % method), 200, head)

    def _album_list(self, query, head):
        try:
            size = int(query.get("size", ["10"])[0])
            offset = int(query.get("offset", ["0"])[0])
        except ValueError:
            size = 10
            offset = 0
        size = max(1, min(size, 50))
        self._send_json(ok({"albumList2": {"album": ALBUMS[offset:offset + size]}}), 200, head)

    def _album(self, query, head):
        album_id = query.get("id", [""])[0]
        album = next((a for a in ALBUMS if a["id"] == album_id), None)
        if album is None:
            self._send_json(failed("Album not found"), 200, head)
            return
        album = dict(album)
        album["song"] = [s for s in SONGS if s["albumId"] == album_id]
        self._send_json(ok({"album": album}), 200, head)

    def _search(self, method, query, head):
        text = query.get("query", [""])[0].strip().lower()
        try:
            count = int(query.get("songCount", ["20"])[0])
            offset = int(query.get("songOffset", ["0"])[0])
        except ValueError:
            count = 20
            offset = 0
        song_list = SONGS
        if text:
            song_list = [
                s for s in SONGS
                if text in s["title"].lower()
                or text in s["album"].lower()
                or text in s["artist"].lower()
            ]
        song_list = song_list[offset:offset + max(0, count)]
        key = "searchResult3" if method == "search3" else "searchResult2"
        self._send_json(ok({key: {"song": song_list}}), 200, head)

    def _cover(self, query, head):
        cover_id = query.get("id", ["cover-1"])[0]
        data = COVERS.get(cover_id, COVERS["cover-1"])
        self._send_bytes(data, "image/png", head)

    def _stream(self, head):
        data = wav_bytes()
        start = 0
        end = len(data) - 1
        status = 200
        range_header = self.headers.get("Range")
        if range_header and range_header.startswith("bytes="):
            spec = range_header[len("bytes="):].split(",")[0].strip()
            pieces = spec.split("-", 1)
            try:
                if pieces[0] == "":
                    start = max(0, len(data) - int(pieces[1]))
                else:
                    start = int(pieces[0])
                    if len(pieces) > 1 and pieces[1] != "":
                        end = min(len(data) - 1, int(pieces[1]))
            except ValueError:
                start = 0
                end = len(data) - 1
            if start > end or start >= len(data):
                self.send_response(416)
                self.send_header("Content-Range", "bytes */%d" % len(data))
                self.end_headers()
                return
            status = 206
        payload = data[start:end + 1]
        self.send_response(status)
        self.send_header("Content-Type", "audio/wav")
        self.send_header("Accept-Ranges", "bytes")
        self.send_header("Content-Length", str(len(payload)))
        if status == 206:
            self.send_header("Content-Range", "bytes %d-%d/%d" % (start, end, len(data)))
        self.end_headers()
        if not head:
            self.wfile.write(payload)

    def _send_json(self, payload, status, head):
        self._send_bytes(json.dumps(payload).encode("utf-8"), "application/json", head, status)

    def _send_bytes(self, payload, content_type, head, status=200):
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        if not head:
            self.wfile.write(payload)


def main():
    parser = argparse.ArgumentParser(description="Dev-only Subsonic mock server")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8080)
    args = parser.parse_args()
    server = ThreadingHTTPServer((args.host, args.port), Handler)
    print("Mock Subsonic server on http://%s:%d" % (args.host, args.port))
    print("Emulator URL: http://10.0.2.2:%d (any username/password)" % args.port)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
