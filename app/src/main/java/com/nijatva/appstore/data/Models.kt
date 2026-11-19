package com.nijatva.appstore.data

data class CatalogApp(
    val id: String,
    val name: String,
    val packageName: String,
    val versionCode: Int,
    val versionName: String,
    val minSdk: Int,
    val category: String,
    val shortDescription: String,
    val description: String,
    val changelog: String,
    val permissions: List<String>,
    val sizeBytes: Long,
    val releaseDate: String,
    val featured: Boolean,
    val apkUrl: String,
    val sha256: String,
    /** True for the store's own row, which only the Updates tab shows. */
    val self: Boolean = false,
    val repoUrl: String = "",
    /** "official" for the Nijatva suite, "community" for anything else. */
    val tier: String = "official",
    val publisher: String = "Nijatva",
) {
    val isPublished: Boolean get() = apkUrl.isNotBlank() && sha256.isNotBlank()

    /** Nijatva builds get the official treatment: pinned sections, badge, priority in search. */
    val isOfficial: Boolean get() = tier.equals("official", ignoreCase = true)
    val initials: String
        get() = name.split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }
}

data class Catalog(
    val apps: List<CatalogApp>,
    val placeholder: Boolean,
    val note: String,
    val generatedAt: String = "",
) {
    val categories: List<String> get() = apps.map { it.category }.distinct().sorted()
    val featured: List<CatalogApp> get() = apps.filter { it.featured }
    val newReleases: List<CatalogApp> get() = apps.sortedByDescending { it.releaseDate }
}

data class Developer(
    val name: String,
    val handle: String,
    val initials: String,
    val bioLine1: String,
    val bioLine2: String,
    val repositories: Int,
    val appsPlanned: Int,
    val appsWithCode: Int,
    val location: String,
    val website: String,
    val email: String,
    val github: String,
    val maintains: List<String>,
    val figuresAsOf: String,
)
