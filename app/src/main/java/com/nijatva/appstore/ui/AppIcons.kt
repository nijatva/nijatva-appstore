package com.nijatva.appstore.ui

import androidx.annotation.DrawableRes
import com.nijatva.appstore.R

/**
 * Launcher icons taken from each app itself: bitmaps pulled from the release APKs, vectors rebuilt
 * from the APK resource table, and for the two apps without a release, from their repository source.
 */
@DrawableRes
fun appIconRes(id: String): Int? = when (id) {
    "nijatva-gallery" -> R.drawable.ic_app_gallery
    "nijatva-browser" -> R.drawable.ic_app_browser
    "nijatva-camera" -> R.drawable.ic_app_camera
    "nijatva-dialer" -> R.drawable.ic_app_dialer
    "nijatva-contacts" -> R.drawable.ic_app_contacts
    "nijatva-messages" -> R.drawable.ic_app_messages
    "nijatva-music" -> R.drawable.ic_app_music
    "nijatva-file" -> R.drawable.ic_app_file
    "nijatva-photo-editor" -> R.drawable.ic_app_editor
    else -> null
}

@DrawableRes
fun appIconResForName(name: String): Int? = when {
    name.contains("Gallery", ignoreCase = true) -> R.drawable.ic_app_gallery
    name.contains("Browser", ignoreCase = true) -> R.drawable.ic_app_browser
    name.contains("Camera", ignoreCase = true) -> R.drawable.ic_app_camera
    name.contains("Dialer", ignoreCase = true) -> R.drawable.ic_app_dialer
    name.contains("Contacts", ignoreCase = true) -> R.drawable.ic_app_contacts
    name.contains("Messages", ignoreCase = true) -> R.drawable.ic_app_messages
    name.contains("Music", ignoreCase = true) -> R.drawable.ic_app_music
    name.contains("File", ignoreCase = true) -> R.drawable.ic_app_file
    name.contains("Editor", ignoreCase = true) -> R.drawable.ic_app_editor
    else -> null
}
