package com.nijatva.appstore.install

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/** Reads signing certificates so the store can explain a rejection instead of guessing. */
object ApkInspector {

    private val FLAGS: Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

    fun archiveCertificate(context: Context, apkPath: String): String? {
        val info = context.packageManager.getPackageArchiveInfo(apkPath, FLAGS) ?: return null
        return digestOf(info)
    }

    fun installedCertificate(context: Context, packageName: String): String? {
        val info = runCatching { context.packageManager.getPackageInfo(packageName, FLAGS) }
            .getOrNull() ?: return null
        return digestOf(info)
    }

    private fun digestOf(info: PackageInfo): String? {
        val bytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return null
            val certificate = if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners.firstOrNull()
            } else {
                signingInfo.signingCertificateHistory.lastOrNull()
            } ?: return null
            certificate.toByteArray()
        } else {
            @Suppress("DEPRECATION")
            info.signatures?.firstOrNull()?.toByteArray() ?: return null
        }
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
