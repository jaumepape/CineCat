package cat.cinecat

import androidx.compose.ui.window.ComposeUIViewController
import cat.cinecat.data.AppContainer
import cat.cinecat.data.KeychainTokenStorage

// Punt d'entrada iOS: l'app Swift (iosApp/ContentView.swift) crida aquesta
// funció i mostra el UIViewController que en surt. La part NATIVA d'iOS és
// només triar el TokenStorage (Keychain); la resta és l'App() compartida.
private val container by lazy { AppContainer(KeychainTokenStorage()) }

fun MainViewController() = ComposeUIViewController { App(container) }
