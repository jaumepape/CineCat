package cat.cinecat

import androidx.compose.ui.window.ComposeUIViewController
import cat.cinecat.data.AppContainer
import cat.cinecat.data.KeychainTokenStorage
import cat.cinecat.data.defaultBaseUrl
import platform.Foundation.NSBundle

// Punt d'entrada iOS: l'app Swift (iosApp/ContentView.swift) crida aquesta
// funció i mostra el UIViewController que en surt. La part NATIVA d'iOS és
// triar el TokenStorage (Keychain) i llegir la URL de l'API de l'Info.plist
// (clau CineCatApiUrl, que ve de Configuration/Config.xcconfig: Debug →
// localhost, Release → Railway). La resta és l'App() compartida.
private val container by lazy {
    val apiUrl = NSBundle.mainBundle.objectForInfoDictionaryKey("CineCatApiUrl") as? String
    AppContainer(KeychainTokenStorage(), baseUrl = apiUrl?.takeIf { it.startsWith("http") } ?: defaultBaseUrl)
}

fun MainViewController() = ComposeUIViewController { App(container) }
