package com.nijatva.appstore.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Where the catalogue on screen came from. */
enum class CatalogSource { BUNDLED, CACHED, REMOTE }

data class CatalogSnapshot(
    val catalog: Catalog,
    val source: CatalogSource,
    val fetchedAt: Long?,
)

object CatalogUrls {
    /**
     * The hosted catalogue. Nothing here is baked into the APK any more: push a new
     * catalog.json and every installed store picks it up on its next refresh.
     */
    const val REMOTE =
        "https://raw.githubusercontent.com/nijatva/nijatva-appstore-catalog/main/catalog.json"
}

/**
 * Three layers, in order of preference: the copy fetched from GitHub (cached on disk),
 * the copy bundled in assets, and the developer profile which stays in assets.
 */
class CatalogRepository(private val context: Context) {

    private val cacheFile = File(context.filesDir, "catalog-cache.json")
    private val etagFile = File(context.filesDir, "catalog-cache.etag")

    fun loadBundled(): Catalog = parse(readAsset("sample_apps.json"))

    fun loadCached(): CatalogSnapshot? {
        if (!cacheFile.exists()) return null
        return runCatching {
            CatalogSnapshot(
                catalog = parse(cacheFile.readText()),
                source = CatalogSource.CACHED,
                fetchedAt = cacheFile.lastModified(),
            )
        }.getOrNull()
    }

    /**
     * Conditional GET against the hosted catalogue. A 304 keeps the cached copy, a 2xx
     * replaces it, and anything else throws so the caller can fall back.
     */
    suspend fun refresh(url: String = CatalogUrls.REMOTE): CatalogSnapshot = withContext(Dispatchers.IO) {
        val etag = if (etagFile.exists()) etagFile.readText().trim().ifBlank { null } else null
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            if (etag != null) setRequestProperty("If-None-Match", etag)
        }
        try {
            when (val code = connection.responseCode) {
                304 -> loadCached() ?: CatalogSnapshot(loadBundled(), CatalogSource.BUNDLED, null)
                in 200..299 -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val parsed = parse(body) // throws on a malformed body, cache untouched
                    cacheFile.writeText(body)
                    connection.getHeaderField("ETag")?.let { etagFile.writeText(it) }
                    CatalogSnapshot(parsed, CatalogSource.REMOTE, System.currentTimeMillis())
                }
                else -> throw IllegalStateException("HTTP $code")
            }
        } finally {
            connection.disconnect()
        }
    }

    fun loadDeveloper(): Developer {
        val o = JSONObject(readAsset("developer.json"))
        val maintains = o.optJSONArray("maintains")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList()
        return Developer(
            name = o.getString("name"),
            handle = o.getString("handle"),
            initials = o.getString("initials"),
            bioLine1 = o.getString("bioLine1"),
            bioLine2 = o.getString("bioLine2"),
            repositories = o.getInt("repositories"),
            appsPlanned = o.getInt("appsPlanned"),
            appsWithCode = o.getInt("appsWithCode"),
            location = o.getString("location"),
            website = o.getString("website"),
            email = o.getString("email"),
            github = o.getString("github"),
            maintains = maintains,
            figuresAsOf = o.getString("figuresAsOf"),
        )
    }

    private fun parse(json: String): Catalog {
        val root = JSONObject(json)
        val apps = root.getJSONArray("apps")
        val list = buildList {
            for (i in 0 until apps.length()) {
                val o = apps.getJSONObject(i)
                add(
                    CatalogApp(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        packageName = o.getString("packageName"),
                        versionCode = o.getInt("versionCode"),
                        versionName = o.getString("versionName"),
                        minSdk = o.optInt("minSdk", 26),
                        category = o.optString("category", "Tools"),
                        shortDescription = o.optString("shortDescription"),
                        description = o.optString("description"),
                        changelog = o.optString("changelog"),
                        permissions = o.optJSONArray("permissions")?.let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }
                        } ?: emptyList(),
                        sizeBytes = o.optLong("sizeBytes", 0L),
                        releaseDate = o.optString("releaseDate"),
                        featured = o.optBoolean("featured", false),
                        apkUrl = o.optString("apkUrl"),
                        sha256 = o.optString("sha256"),
                        self = o.optBoolean("self", false),
                        repoUrl = o.optString("repoUrl"),
                        tier = o.optString("tier", "official"),
                        publisher = o.optString("publisher", "Nijatva"),
                    )
                )
            }
        }
        return Catalog(
            apps = list,
            placeholder = root.optBoolean("placeholder", false),
            note = root.optString("note"),
            generatedAt = root.optString("generatedAt"),
        )
    }

    private fun readAsset(name: String): String =
        context.assets.open(name).bufferedReader().use { it.readText() }
}
