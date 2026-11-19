package com.nijatva.appstore.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.pm.PackageInstaller
import com.nijatva.appstore.install.ApkDownloader
import com.nijatva.appstore.install.ApkInspector
import com.nijatva.appstore.install.ApkInstaller
import com.nijatva.appstore.install.InstallBus
import com.nijatva.appstore.install.InstallResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

sealed interface InstallState {
    data object Idle : InstallState
    data object NotPublished : InstallState
    data class Downloading(val percent: Int) : InstallState
    data object Verifying : InstallState
    data object NeedsPermission : InstallState
    data object Installing : InstallState
    data object Installed : InstallState
    data class Failed(val reason: String) : InstallState
}

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val catalogRepo = CatalogRepository(application)
    private val downloader = ApkDownloader(application)
    private val installer = ApkInstaller(application)

    private val _catalog = MutableStateFlow(
        Catalog(apps = emptyList(), placeholder = false, note = "")
    )
    val catalog: StateFlow<Catalog> = _catalog.asStateFlow()

    private val _developer = MutableStateFlow<Developer?>(null)
    val developer: StateFlow<Developer?> = _developer.asStateFlow()

    private val _installedVersions = MutableStateFlow<Map<String, Int>>(emptyMap())
    val installedVersions: StateFlow<Map<String, Int>> = _installedVersions.asStateFlow()

    private val _installStates = MutableStateFlow<Map<String, InstallState>>(emptyMap())
    val installStates: StateFlow<Map<String, InstallState>> = _installStates.asStateFlow()

    private val _source = MutableStateFlow(CatalogSource.BUNDLED)
    val source: StateFlow<CatalogSource> = _source.asStateFlow()

    private val _fetchedAt = MutableStateFlow<Long?>(null)
    val fetchedAt: StateFlow<Long?> = _fetchedAt.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()

    private var pendingFile: File? = null

    init {
        viewModelScope.launch {
            // Cached copy first so the list is never empty, then the bundled one as the floor,
            // then a network refresh in the background.
            val cached = catalogRepo.loadCached()
            if (cached != null) {
                _catalog.value = cached.catalog
                _source.value = CatalogSource.CACHED
                _fetchedAt.value = cached.fetchedAt
            } else {
                _catalog.value = catalogRepo.loadBundled()
                _source.value = CatalogSource.BUNDLED
            }
            _developer.value = catalogRepo.loadDeveloper()
            refreshInstalled()
            refreshCatalog(silent = true)
        }
    }

    /** Pulls the hosted catalogue. Silent when it runs on its own, loud when the user asks. */
    fun refreshCatalog(silent: Boolean = false) {
        viewModelScope.launch {
            if (_refreshing.value) return@launch
            _refreshing.value = true
            _refreshError.value = null
            runCatching { catalogRepo.refresh() }
                .onSuccess { snapshot ->
                    _catalog.value = snapshot.catalog
                    _source.value = snapshot.source
                    _fetchedAt.value = snapshot.fetchedAt
                    _refreshError.value = null
                    refreshInstalled()
                }
                .onFailure { error ->
                    _refreshError.value = when {
                        error is java.net.UnknownHostException -> "No network — showing the saved catalogue"
                        error is java.net.SocketTimeoutException -> "Catalogue timed out — showing the saved catalogue"
                        else -> error.message ?: "Could not reach the catalogue"
                    }
                }
            _refreshing.value = false
        }
    }

    fun appById(id: String?): CatalogApp? = _catalog.value.apps.firstOrNull { it.id == id }

    fun installStateFor(id: String): InstallState = _installStates.value[id] ?: InstallState.Idle

    fun refreshInstalled() {
        val pm = getApplication<Application>().packageManager
        val versions = buildMap {
            for (app in _catalog.value.apps) {
                val info = runCatching {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(app.packageName, 0)
                }.getOrNull()
                if (info != null) {
                    @Suppress("DEPRECATION")
                    val code = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        info.longVersionCode.toInt()
                    } else {
                        info.versionCode
                    }
                    put(app.packageName, code)
                }
            }
        }
        _installedVersions.value = versions
    }

    fun install(app: CatalogApp) {
        if (!app.isPublished) {
            setState(app.id, InstallState.NotPublished)
            return
        }
        if (!isOnline()) {
            setState(app.id, InstallState.Failed("No network connection — connect and try again"))
            return
        }
        viewModelScope.launch {
            try {
                setState(app.id, InstallState.Downloading(0))
                val file = downloader.download(app) { percent ->
                    setState(app.id, InstallState.Downloading(percent))
                }
                setState(app.id, InstallState.Verifying)
                pendingFile = file

                if (!installer.canRequestInstall()) {
                    setState(app.id, InstallState.NeedsPermission)
                    return@launch
                }
                commit(app, file)
            } catch (e: Exception) {
                setState(app.id, InstallState.Failed(e.message ?: "Download failed"))
            }
        }
    }

    /** Called after the user returns from the "install unknown apps" settings screen. */
    fun resumePendingInstall() {
        val file = pendingFile ?: return
        val app = _catalog.value.apps.firstOrNull { file.name.startsWith("${it.id}-") && file.exists() } ?: return
        if (!installer.canRequestInstall()) return
        viewModelScope.launch { commit(app, file) }
    }

    private suspend fun commit(app: CatalogApp, file: File) {
        // A copy signed with another key can never be updated over the top, and the installer
        // reports that as a bare rejection. Say so before the session even starts.
        val installedCert = ApkInspector.installedCertificate(getApplication(), app.packageName)
        val archiveCert = ApkInspector.archiveCertificate(getApplication(), file.absolutePath)
        if (installedCert != null && archiveCert != null && installedCert != archiveCert) {
            setState(
                app.id,
                InstallState.Failed(
                    "An older copy of ${app.name} is installed with a different signing key. " +
                        "Uninstall it, then install again."
                ),
            )
            return
        }

        setState(app.id, InstallState.Installing)
        val sessionId = installer.install(file, app.packageName)
        val result = withTimeoutOrNull(180_000) {
            InstallBus.results.first { it.sessionId == sessionId }
        }
        refreshInstalled()
        val installed = _installedVersions.value.containsKey(app.packageName)
        setState(
            app.id,
            when {
                installed -> InstallState.Installed
                result == null -> InstallState.Failed("Timed out waiting for the installer")
                else -> InstallState.Failed(describe(result))
            },
        )
    }

    /** Turns a PackageInstaller status code into something a person can act on. */
    private fun describe(result: InstallResult): String {
        val detail = result.message?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
        return when (result.status) {
            PackageInstaller.STATUS_FAILURE_ABORTED -> "Install was cancelled$detail"
            PackageInstaller.STATUS_FAILURE_BLOCKED -> "The system blocked the install$detail"
            PackageInstaller.STATUS_FAILURE_CONFLICT ->
                "This build conflicts with a copy already on the device$detail"
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                "This build is incompatible with the device$detail"
            PackageInstaller.STATUS_FAILURE_INVALID -> "The APK is not a valid package$detail"
            PackageInstaller.STATUS_FAILURE_STORAGE -> "Not enough storage for the install$detail"
            PackageInstaller.STATUS_FAILURE -> "The installer rejected the package$detail"
            else -> "Install finished with status ${result.status}$detail"
        }
    }

    fun permissionIntent() = installer.permissionIntent()

    private fun isOnline(): Boolean {
        val manager = getApplication<Application>()
            .getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun setState(id: String, state: InstallState) {
        _installStates.value = _installStates.value.toMutableMap().apply { put(id, state) }
    }
}
