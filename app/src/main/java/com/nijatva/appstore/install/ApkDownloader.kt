package com.nijatva.appstore.install

import android.content.Context
import com.nijatva.appstore.data.CatalogApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class VerificationException(message: String) : Exception(message)

/**
 * Downloads an APK to the cache directory and verifies its SHA-256 against the catalogue.
 * Deliberately avoids DownloadManager: we need the bytes in one place so they can be hashed
 * and handed to PackageInstaller without a FileProvider round trip.
 */
class ApkDownloader(private val context: Context) {

    suspend fun download(app: CatalogApp, onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "apks").apply { mkdirs() }
        val outFile = File(dir, "${app.id}-${app.versionCode}.apk")
        if (outFile.exists()) outFile.delete()

        val connection = (URL(app.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            requestMethod = "GET"
        }

        try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("HTTP ${connection.responseCode}")
            }
            val total = connection.contentLengthLong
            var downloaded = 0L
            connection.inputStream.use { input ->
                outFile.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) onProgress(((downloaded * 100) / total).toInt())
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        val actual = sha256(outFile)
        if (!actual.equals(app.sha256, ignoreCase = true)) {
            outFile.delete()
            throw VerificationException("Checksum mismatch — file deleted")
        }
        outFile
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
