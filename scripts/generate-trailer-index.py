"""Build the bundled trailer index from the series already mapped to TMDB."""

import argparse
from concurrent.futures import ThreadPoolExecutor
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import time
from urllib.error import HTTPError, URLError
from urllib.request import build_opener, HTTPRedirectHandler, Request


ASSETS = Path(__file__).resolve().parents[1] / "app/src/main/assets"
VIDEO_ID = re.compile(r"[A-Za-z0-9_-]{11}")


def read_token():
    if token := os.environ.get("TMDB_BEARER_TOKEN", "").strip():
        return token
    if sys.platform == "darwin":
        result = subprocess.run(
            ["/usr/bin/security", "find-generic-password", "-s", "LostFilm.TV TMDB Read Token",
             "-a", "LostFilm.TV", "-w"],
            capture_output=True,
            text=True,
            check=False,
        )
        if result.returncode == 0 and result.stdout.strip():
            return result.stdout.strip()
    raise ValueError("TMDB Read Access Token is missing from the environment and Keychain.")


def select_trailer(videos):
    candidates = [
        video for video in videos
        if video.get("site") == "YouTube"
        and video.get("type") == "Trailer"
        and video.get("official") is True
        and VIDEO_ID.fullmatch(video.get("key") or "")
    ]
    language_priority = {"ru": 2, "en": 1}
    best = max(candidates, key=lambda video: (
        language_priority.get(video.get("iso_639_1"), 0),
        video.get("size") or 0,
        video.get("published_at") or "",
        video["key"],
    ), default=None)
    return best["key"] if best else None


class NoRedirects(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def fetch_trailer(tmdb_id, token):
    request = Request(
        f"https://api.themoviedb.org/3/tv/{tmdb_id}/videos?include_video_language=ru,en",
        headers={"Authorization": f"Bearer {token}", "Accept": "application/json"},
    )
    opener = build_opener(NoRedirects())
    for attempt in range(3):
        try:
            with opener.open(request, timeout=10) as response:
                result = json.load(response)
        except HTTPError as error:
            if error.code == 404:
                return None
            if attempt < 2 and (error.code == 429 or 500 <= error.code < 600):
                retry_after = error.headers.get("Retry-After", "")
                time.sleep(min(int(retry_after), 30) if retry_after.isdigit() else 2 ** attempt)
                continue
            raise RuntimeError(f"TMDB returned HTTP {error.code} for series {tmdb_id}.") from None
        except (URLError, TimeoutError) as error:
            if attempt < 2:
                time.sleep(2 ** attempt)
                continue
            raise RuntimeError(f"TMDB is unreachable for series {tmdb_id}: {error}.") from None
        if result.get("id") != tmdb_id or not isinstance(result.get("results"), list):
            raise RuntimeError(f"Unexpected TMDB response for series {tmdb_id}.")
        return select_trailer(result["results"])


def write_index(output, index):
    with tempfile.NamedTemporaryFile(
        mode="w", encoding="utf-8", dir=output.parent, prefix=".catalog-trailers-", delete=False,
    ) as stream:
        temporary = Path(stream.name)
    try:
        temporary.write_text(json.dumps(index, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        temporary.replace(output)
    finally:
        temporary.unlink(missing_ok=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--poster-index", type=Path, default=ASSETS / "catalog-posters.json")
    parser.add_argument("--output", type=Path, default=ASSETS / "catalog-trailers.json")
    args = parser.parse_args()
    entries = json.loads(args.poster_index.read_text(encoding="utf-8"))["entries"]
    series = {
        path: entry["tmdbId"] for path, entry in entries.items()
        if re.fullmatch(r"/series/[^/]+", path) and entry["tmdbId"] > 0
    }
    if not series:
        raise ValueError("No series with TMDB IDs were found in the poster index.")
    token = read_token()
    tmdb_ids = sorted(set(series.values()))
    trailers = {tmdb_ids[0]: fetch_trailer(tmdb_ids[0], token)}
    with ThreadPoolExecutor(max_workers=4) as workers:
        remaining_ids = tmdb_ids[1:]
        remaining_trailers = workers.map(lambda tmdb_id: fetch_trailer(tmdb_id, token), remaining_ids)
        trailers.update(zip(remaining_ids, remaining_trailers))
    index = {path: trailers[tmdb_id] for path, tmdb_id in series.items() if trailers[tmdb_id]}
    if not index:
        raise ValueError("TMDB provided no official YouTube trailers. The existing index was preserved.")
    write_index(args.output, index)
    print(f"Indexed official YouTube trailers for {len(index)} of {len(series)} series.")


if __name__ == "__main__":
    try:
        main()
    except (OSError, RuntimeError, ValueError) as error:
        sys.exit(str(error))
