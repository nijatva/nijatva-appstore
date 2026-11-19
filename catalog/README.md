# Hosted catalogue

`catalog.json` here is what every installed Nijatva Appstore fetches. The store asks for it at
launch and on the Refresh button, keeps the last good copy on disk, and falls back to the copy
bundled inside the APK when there is no network.

To publish an update:

```bash
python3 tools/build_catalog.py      # reads GitHub releases, rewrites catalog.json
git add catalog/catalog.json && git commit -m "catalogue: <what changed>" && git push
```

Nothing else needs to change — no new store APK.

Host it so this URL resolves:

```
https://raw.githubusercontent.com/nijatva/nijatva-appstore-catalog/main/catalog.json
```

i.e. a public repo `nijatva/nijatva-appstore-catalog` with this file at `catalog.json` on `main`.
If you prefer another path, change `CatalogUrls.REMOTE` in
`app/src/main/java/com/nijatva/appstore/data/CatalogRepository.kt`.

## What is verified

- Every APK URL is downloaded by the store and its SHA-256 is compared with `sha256` before the
  installer sees the file. A mismatch deletes the download.
- The catalogue itself is **not signed yet**. Whoever can serve that URL can change the list.
  Signing `catalog.json` and pinning the public key in the app is the next step.
