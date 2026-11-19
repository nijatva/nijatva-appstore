package com.nijatva.appstore.install

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** One result from the PackageInstaller callback: session id, status code, status message. */
data class InstallResult(val sessionId: Int, val status: Int, val message: String?)

/** Bridges the PackageInstaller result broadcast back to the running ViewModel. */
object InstallBus {
    private val _results = MutableSharedFlow<InstallResult>(extraBufferCapacity = 8)
    val results = _results.asSharedFlow()

    fun emit(result: InstallResult) {
        _results.tryEmit(result)
    }
}
