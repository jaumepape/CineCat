package cat.cinecat.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cat.cinecat.App
import cat.cinecat.data.AppContainer

/**
 * Punt d'entrada Android. La part NATIVA és mínima: crear el TokenStorage
 * amb el Keystore d'Android (necessita un Context) i mostrar l'App()
 * compartida amb Compose Multiplatform.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Barres del sistema fosques sobre el fons de l'app (#0D0E11).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val container = (application as CineCatApplication).container
        setContent { App(container) }
    }
}
