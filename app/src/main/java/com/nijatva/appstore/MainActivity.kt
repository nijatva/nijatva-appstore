package com.nijatva.appstore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nijatva.appstore.core.theme.NijatvaTheme
import com.nijatva.appstore.core.theme.ThemeState
import com.nijatva.appstore.data.themeStateFromKit
import com.nijatva.appstore.navigation.NijatvaRoot
import com.nijatva.signature.SignatureTheme

class MainActivity : ComponentActivity() {

    /**
     * Theme and accent live in the signature library's SharedPreferences file, the same
     * store every other Nijatva app uses. It is a plain synchronous read, so the first
     * Compose frame already has the right colours and no splash hold is needed.
     */
    private var themeState by mutableStateOf(ThemeState())

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        SignatureTheme.seedDefault(this, SignatureTheme.SYSTEM, SignatureTheme.GREEN)
        SignatureTheme.applyTo(this)
        enableEdgeToEdge()
        themeState = themeStateFromKit(this)

        setContent {
            NijatvaTheme(themeState = themeState) {
                NijatvaRoot(
                    settings = (application as NijatvaApp).settings,
                    themeState = themeState,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The Appearance screen is the library's own activity; pick up what it saved.
        themeState = themeStateFromKit(this)
        SignatureTheme.applyTo(this)
    }
}
