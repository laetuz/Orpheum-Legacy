#!/usr/bin/env python3
"""Dev-only Invidious/Piped mock for Orpheum's YouTube audio screen.

Usage:
    python3 tools/youtube-mock/mock_server.py [--port 8090]

Set Settings -> YouTube -> Instance list URL to:
    http://10.0.2.2:8090/instances.json

Any query works; the mock serves a small canned library and a generated audio
tone. Nothing here talks to YouTube. It is not part of Orpheum's backend.
"""

import argparse
import io
import json
import math
import os
import shutil
import struct
import subprocess
import tempfile
import wave
import zlib
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

SAMPLE_RATE = 44100
DURATION = 8

VIDEOS = [
    {"id": "mockvid001", "title": "Mock Song One", "artist": "YouTube Test", "duration": DURATION},
    {"id": "mockvid002", "title": "Mock Song Two", "artist": "YouTube Test", "duration": DURATION},
    {"id": "mockvid003", "title": "Night Mock", "artist": "Another Channel", "duration": DURATION},
]

WAV_BYTES = None
M4A_BYTES = None
MUXED_BYTES = None


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


def m4a_bytes():
    global M4A_BYTES
    if M4A_BYTES is not None:
        return M4A_BYTES
    try:
        with tempfile.TemporaryDirectory() as tmp:
            wav_path = os.path.join(tmp, "tone.wav")
            m4a_path = os.path.join(tmp, "tone.m4a")
            with open(wav_path, "wb") as wav_file:
                wav_file.write(wav_bytes())
            result = subprocess.run(
                ["afconvert", "-f", "m4af", "-d", "aac", "-b", "128000", wav_path, m4a_path],
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                check=False,
            )
            if result.returncode == 0 and os.path.exists(m4a_path):
                with open(m4a_path, "rb") as m4a_file:
                    M4A_BYTES = m4a_file.read()
                return M4A_BYTES
    except Exception:
        pass
    M4A_BYTES = b""
    return M4A_BYTES


def muxed_bytes():
    global MUXED_BYTES
    if MUXED_BYTES is not None:
        return MUXED_BYTES
    ffmpeg = shutil.which("ffmpeg")
    if not ffmpeg:
        MUXED_BYTES = b""
        return MUXED_BYTES
    try:
        with tempfile.TemporaryDirectory() as tmp:
            wav_path = os.path.join(tmp, "tone.wav")
            out_path = os.path.join(tmp, "muxed.mp4")
            with open(wav_path, "wb") as wav_file:
                wav_file.write(wav_bytes())
            result = subprocess.run(
                [ffmpeg, "-y", "-loglevel", "error",
                 "-f", "lavfi", "-i", "color=c=blue:s=160x90:d=%d" % DURATION,
                 "-i", wav_path,
                 "-c:v", "libx264", "-pix_fmt", "yuv420p", "-preset", "ultrafast",
                 "-c:a", "aac", "-shortest", "-movflags", "+faststart", out_path],
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                check=False,
            )
            if result.returncode == 0 and os.path.exists(out_path):
                with open(out_path, "rb") as muxed_file:
                    MUXED_BYTES = muxed_file.read()
                return MUXED_BYTES
    except Exception:
        pass
    MUXED_BYTES = b""
    return MUXED_BYTES


def png_chunk(kind, data):
    return (struct.pack(">I", len(data)) + kind + data
            + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF))


def cover_png():
    width = height = 128
    rows = []
    for y in range(height):
        row = bytearray([0])
        for x in range(width):
            row.extend(((40 + x) % 256, (180 + y) % 256, (90 + (x + y) // 2) % 256))
        rows.append(bytes(row))
    raw = b"".join(rows)
    return (b"\x89PNG\r\n\x1a\n"
            + png_chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
            + png_chunk(b"IDAT", zlib.compress(raw, 9))
            + png_chunk(b"IEND", b""))


COVER = cover_png()


class Handler(BaseHTTPRequestHandler):
    server_version = "OrpheumYouTubeMock/1.0"
    advertise_base = "http://10.0.2.2:8090"

    def log_message(self, fmt, *args):
        print("%s %s" % (self.client_address[0], fmt % args))

    def do_HEAD(self):
        self._handle(head=True)

    def do_GET(self):
        self._handle(head=False)

    def _handle(self, head):
        parsed = urlparse(self.path)
        path = parsed.path
        query = parse_qs(parsed.query)

        if path == "/instances.json":
            self._json({
                "invidious": [self.advertise_base],
                "piped": [self.advertise_base + "," + self.advertise_base],
            }, head)
            return
        if path == "/api/v1/search":
            self._invidious_search(query, head)
            return
        if path.startswith("/api/v1/videos/"):
            video_id = path[len("/api/v1/videos/"):]
            self._invidious_video(video_id, head)
            return
        if path == "/search":
            self._piped_search(query, head)
            return
        if path.startswith("/streams/"):
            video_id = path[len("/streams/"):]
            self._piped_streams(video_id, head)
            return
        if path.startswith("/vi/") or path.startswith("/cover"):
            self._bytes(COVER, "image/png", head)
            return
        if path.startswith("/audio/"):
            self._audio(head, "audio/mp4")
            return
        if path.startswith("/muxed/"):
            self._muxed(head)
            return
        self._json({"error": "not found"}, head, 404)

    def _invidious_search(self, query, head):
        text = query.get("q", [""])[0].lower()
        items = []
        for index, video in enumerate(VIDEOS):
            if text and text not in video["title"].lower() and text not in video["artist"].lower():
                continue
            items.append({
                "type": "video",
                "videoId": video["id"],
                "title": video["title"],
                "author": video["artist"],
                "lengthSeconds": video["duration"],
                "liveNow": False,
                "isUpcoming": False,
                "videoThumbnails": [{"url": "/vi/%s/mqdefault.jpg" % video["id"], "width": 320}],
            })
        self._json(items, head)

    def _invidious_video(self, video_id, head):
        video = next((v for v in VIDEOS if v["id"] == video_id.split("?")[0]), None)
        if video is None:
            self._json({"error": "Video unavailable"}, head, 404)
            return
        base = self.advertise_base
        self._json({
            "title": video["title"],
            "author": video["artist"],
            "lengthSeconds": video["duration"],
            "adaptiveFormats": [
                {
                    "itag": "251",
                    "type": 'audio/webm; codecs="opus"',
                    "bitrate": "160000",
                    "url": base + "/audio/" + video["id"],
                },
                {
                    "itag": "140",
                    "type": 'audio/mp4; codecs="mp4a.40.2"',
                    "bitrate": "128000",
                    "url": base + "/audio/" + video["id"],
                },
            ],
            "formatStreams": [
                {
                    "itag": "18",
                    "type": 'video/mp4; codecs="avc1.42001E, mp4a.40.2"',
                    "quality": "360p",
                    "resolution": "360p",
                    "bitrate": "512000",
                    "url": base + "/muxed/" + video["id"],
                },
            ],
        }, head)

    def _piped_search(self, query, head):
        text = query.get("q", [""])[0].lower()
        items = []
        for video in VIDEOS:
            if text and text not in video["title"].lower() and text not in video["artist"].lower():
                continue
            items.append({
                "url": "/watch?v=" + video["id"],
                "title": video["title"],
                "uploaderName": video["artist"],
                "duration": video["duration"],
                "thumbnail": "/vi/%s/mqdefault.jpg" % video["id"],
            })
        self._json({"items": items}, head)

    def _piped_streams(self, video_id, head):
        video = next((v for v in VIDEOS if v["id"] == video_id.split("?")[0]), None)
        if video is None:
            self._json({"error": "Video unavailable"}, head, 404)
            return
        base = self.advertise_base
        self._json({
            "title": video["title"],
            "uploader": video["artist"],
            "duration": video["duration"],
            "audioStreams": [
                {
                    "url": base + "/audio/" + video["id"],
                    "format": "WEBMA_OPUS",
                    "mimeType": "audio/webm",
                    "codec": "opus",
                    "bitrate": 160000,
                },
                {
                    "url": base + "/audio/" + video["id"],
                    "format": "M4A",
                    "mimeType": "audio/mp4",
                    "codec": "mp4a.40.2",
                    "bitrate": 128000,
                },
            ],
            "videoStreams": [
                {
                    "url": base + "/muxed/" + video["id"],
                    "mimeType": "video/mp4",
                    "quality": "360p",
                    "height": 360,
                    "videoOnly": False,
                },
            ],
        }, head)

    def _muxed(self, head):
        data = muxed_bytes()
        if data:
            self._bytes(data, "video/mp4", head)
        else:
            self._audio(head, "video/mp4")

    def _audio(self, head, content_type="audio/mp4"):
        data = m4a_bytes()
        if not data:
            data = wav_bytes()
            content_type = "audio/wav"
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
        self.send_header("Content-Type", content_type)
        self.send_header("Accept-Ranges", "bytes")
        self.send_header("Content-Length", str(len(payload)))
        if status == 206:
            self.send_header("Content-Range", "bytes %d-%d/%d" % (start, end, len(data)))
        self.end_headers()
        if not head:
            self.wfile.write(payload)

    def _json(self, payload, head, status=200):
        self._bytes(json.dumps(payload).encode("utf-8"), "application/json", head, status)

    def _bytes(self, payload, content_type, head, status=200):
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        if not head:
            self.wfile.write(payload)


def main():
    parser = argparse.ArgumentParser(description="Dev-only Invidious/Piped mock")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8090)
    parser.add_argument("--advertise-base", default="")
    args = parser.parse_args()
    Handler.advertise_base = (args.advertise_base
                              or "http://10.0.2.2:%d" % args.port).rstrip("/")
    server = ThreadingHTTPServer((args.host, args.port), Handler)
    print("Mock YouTube server on http://%s:%d" % (args.host, args.port))
    print("Instance list: %s/instances.json" % Handler.advertise_base)
    print("Use that URL in Settings -> YouTube (any search text).")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
