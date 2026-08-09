#!/usr/bin/env python3
"""Serve DietCoach install page; APK uses package-archive MIME so Android can jump to installer."""
from __future__ import annotations

import mimetypes
import os
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer

ROOT = os.path.dirname(os.path.abspath(__file__))
PORT = int(os.environ.get("DIETCOACH_PORT", "8787"))


class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=ROOT, **kwargs)

    def end_headers(self):
        path = self.translate_path(self.path.split("?", 1)[0])
        if path.lower().endswith(".apk"):
            self.send_header("Content-Type", "application/vnd.android.package-archive")
            self.send_header(
                "Content-Disposition",
                'inline; filename="DietCoach.apk"',
            )
            self.send_header("Cache-Control", "no-store")
        super().end_headers()

    def guess_type(self, path):
        if path.lower().endswith(".apk"):
            return "application/vnd.android.package-archive"
        return super().guess_type(path)


def main() -> None:
    mimetypes.add_type("application/vnd.android.package-archive", ".apk")
    server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print(f"DietCoach install server on http://0.0.0.0:{PORT}/")
    print("APK MIME = application/vnd.android.package-archive (inline)")
    server.serve_forever()


if __name__ == "__main__":
    main()
