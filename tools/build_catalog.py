#!/usr/bin/env python3
"""Regenerate catalog/catalog.json from the GitHub releases of the Nijatva repos.

The app fetches this file at runtime, so a new app or a new release only needs a commit
here - no new store APK.

    python3 tools/build_catalog.py            # uses gh or GITHUB_TOKEN
    python3 tools/build_catalog.py --check    # do not write, just report

Each entry in SEED keeps the human-written text (description, permissions, category).
Everything that the release itself can prove - versionCode, versionName, minSdk, size,
sha256, download URL - is read from the APK asset.
"""
import argparse
import hashlib
import json
import os
import subprocess
import sys
import urllib.request

OWNER = "vasudev-work"
CATALOG = os.path.join(os.path.dirname(__file__), "..", "catalog", "catalog.json")
AAPT2 = os.environ.get("AAPT2", "aapt2")

# Third-party apps the store also lists. Same resolver, but tier "community".
EXTERNAL = [
    {"id": "newpipe", "repo": "TeamNewPipe/NewPipe", "name": "NewPipe",
     "packageName": "org.schabi.newpipe", "category": "Media", "publisher": "Team NewPipe",
     "tag": "v0.29.3", "asset": "NewPipe_v0.29.3.apk"},
    {"id": "fdroid", "repo": "f-droid/fdroidclient", "name": "F-Droid",
     "packageName": "org.fdroid.fdroid", "category": "Tools", "publisher": "F-Droid",
     "tag": "1.18.0", "asset": "org.fdroid.fdroid_1018050.apk"},
    {"id": "keepassdx", "repo": "Kunzisoft/KeePassDX", "name": "KeePassDX",
     "packageName": "com.kunzisoft.keepass.libre", "category": "Security", "publisher": "Kunzisoft",
     "tag": "4.5.8", "asset": "KeePassDX-4.5.8-libre.apk"},
]

# repo -> catalogue entry. `asset` pins which release asset to use when a repo ships
# more than one APK or also ships desktop builds.
SEED = [
    {"id": "nijatva-gallery", "repo": "nijatva-gallery", "name": "Nijatva Gallery",
     "packageName": "com.nijatva.gallery", "category": "Photos", "featured": True},
    {"id": "nijatva-browser", "repo": "nijatva-browser", "name": "Nijatva Browser",
     "packageName": "in.nijatva.browser.debug", "category": "Internet", "featured": True,
     "tag": "v2.0.3", "asset": "Nijatva-debug.apk"},
    {"id": "nijatva-camera", "repo": "nijatva-camera", "name": "Nijatva Camera",
     "packageName": "com.nijatva.camera", "category": "Camera", "featured": True},
    {"id": "nijatva-dialer", "repo": "nijatva-dialer", "name": "Nijatva Dialer",
     "packageName": "com.nijatva.dialer", "category": "Communication", "featured": True},
    {"id": "nijatva-contacts", "repo": "nijatva-contacts", "name": "Nijatva Contacts",
     "packageName": "com.nijatva.contacts", "category": "Communication"},
    {"id": "nijatva-messages", "repo": "nijatva-messages", "name": "Nijatva Messages",
     "packageName": "com.nijatva.messages", "category": "Communication"},
    {"id": "nijatva-music", "repo": "nijatva-music-player", "name": "Nijatva Music",
     "packageName": "com.nijatva.music.player", "category": "Audio"},
    {"id": "nijatva-file", "repo": "nijatva-file", "name": "Nijatva File",
     "packageName": "com.nijatva.file", "category": "Tools"},
    {"id": "nijatva-photo-editor", "repo": "nijatva-photo-editor", "name": "Nijatva Editor",
     "packageName": "com.nijatva.photo.editor", "category": "Photos"},
]


def gh_api(path):
    if os.environ.get("GITHUB_TOKEN"):
        req = urllib.request.Request(
            "https://api.github.com" + path,
            headers={"Authorization": "Bearer " + os.environ["GITHUB_TOKEN"],
                     "Accept": "application/vnd.github+json"})
        with urllib.request.urlopen(req, timeout=30) as response:
            return json.load(response)
    out = subprocess.run(["gh", "api", path], capture_output=True, text=True, check=True)
    return json.loads(out.stdout)


def latest_apk(repo, tag=None, asset=None):
    """repo may be "owner/name" (community) or a bare name under OWNER (Nijatva)."""
    full = repo if "/" in repo else f"{OWNER}/{repo}"
    if tag:
        release = gh_api(f"/repos/{full}/releases/tags/{tag}")
        releases = [release]
    else:
        releases = gh_api(f"/repos/{full}/releases")
    for release in releases:
        for item in release.get("assets", []):
            if not item["name"].endswith(".apk"):
                continue
            if asset and item["name"] != asset:
                continue
            return release, item
    return None, None


def read_apk(path):
    """versionCode, versionName and minSdk straight from the APK, plus its sha256."""
    digest = hashlib.sha256(open(path, "rb").read()).hexdigest()
    info = {"sha256": digest, "size": os.path.getsize(path)}
    try:
        out = subprocess.run([AAPT2, "dump", "badging", path], capture_output=True, text=True, check=True).stdout
    except (subprocess.CalledProcessError, FileNotFoundError):
        return info
    for line in out.splitlines():
        if line.startswith("package:"):
            for part in line.split():
                if part.startswith("versionCode="):
                    info["versionCode"] = int(part.split("=", 1)[1].strip("'"))
                elif part.startswith("versionName="):
                    info["versionName"] = part.split("=", 1)[1].strip("'")
        elif line.startswith("minSdkVersion:"):
            info["minSdk"] = int(line.split(":", 1)[1].strip().strip("'"))
    return info


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    current = {}
    if os.path.exists(CATALOG):
        for entry in json.load(open(CATALOG)).get("apps", []):
            current[entry["id"]] = entry

    apps = []
    for seed in SEED + EXTERNAL:
        entry = dict(current.get(seed["id"], {}))
        entry.update({k: v for k, v in seed.items() if k != "repo"})
        entry["tier"] = "community" if seed in EXTERNAL else "official"
        entry.setdefault("publisher", "Nijatva" if seed in SEED else seed.get("publisher", ""))
        repo = seed["repo"] if "/" in seed["repo"] else f"{OWNER}/{seed['repo']}"
        release, asset = latest_apk(repo, seed.get("tag"), seed.get("asset"))
        if not asset:
            entry.update({"apkUrl": "", "sha256": ""})
            print(f"{seed['id']}: no APK release")
            apps.append(entry)
            continue
        tmp = f"/tmp/{seed['id']}.apk"
        urllib.request.urlretrieve(asset["browser_download_url"], tmp)
        info = read_apk(tmp)
        os.remove(tmp)
        entry.update({
            "apkUrl": asset["browser_download_url"],
            "sha256": info["sha256"],
            "sizeBytes": info["size"],
            "versionCode": info.get("versionCode", entry.get("versionCode", 0)),
            "versionName": info.get("versionName", entry.get("versionName", "")),
            "minSdk": info.get("minSdk", entry.get("minSdk", 26)),
            "releaseDate": release["published_at"][:10],
        })
        print(f"{seed['id']}: {entry['versionName']} ({entry['versionCode']}) {info['size']} bytes")
        apps.append(entry)

    catalog = {
        "schemaVersion": 2,
        "generatedAt": subprocess.run(["date", "-u", "+%Y-%m-%dT%H:%M:%SZ"],
                                      capture_output=True, text=True).stdout.strip(),
        "placeholder": False,
        "note": "Generated by tools/build_catalog.py from the GitHub releases.",
        "apps": apps,
    }
    if args.check:
        print(json.dumps(catalog, indent=2)[:800])
        return 0
    os.makedirs(os.path.dirname(CATALOG), exist_ok=True)
    with open(CATALOG, "w") as handle:
        json.dump(catalog, handle, indent=2, ensure_ascii=False)
        handle.write("\n")
    print(f"wrote {CATALOG} with {len(apps)} apps")
    return 0


if __name__ == "__main__":
    sys.exit(main())
