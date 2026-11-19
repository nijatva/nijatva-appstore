package com.nijatva.appstore.data

import android.content.Context
import com.nijatva.appstore.core.theme.Accent
import com.nijatva.appstore.core.theme.ThemeMode
import com.nijatva.appstore.core.theme.ThemeState
import com.nijatva.signature.SignatureTheme

/**
 * The signature library's SharedPreferences file is the single source of truth for
 * theme and accent, exactly as in the other Nijatva apps. Compose reads it here
 * instead of keeping a second store of its own.
 */
fun themeStateFromKit(context: Context): ThemeState {
    val mode = when (SignatureTheme.theme(context)) {
        SignatureTheme.DARK -> ThemeMode.DARK
        SignatureTheme.LIGHT -> ThemeMode.LIGHT
        SignatureTheme.BLACK -> ThemeMode.BLACK
        else -> ThemeMode.SYSTEM
    }
    val accent = when (SignatureTheme.accent(context)) {
        SignatureTheme.SAFFRON -> Accent.SAFFRON
        SignatureTheme.INDIGO -> Accent.INDIGO
        else -> Accent.GREEN
    }
    return ThemeState(themeMode = mode, accent = accent)
}
