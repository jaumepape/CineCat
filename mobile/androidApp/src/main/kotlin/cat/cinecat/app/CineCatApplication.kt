package cat.cinecat.app

import android.app.Application
import cat.cinecat.data.AppContainer
import cat.cinecat.data.KeystoreTokenStorage

/** Les dependències viuen a l'Application: sobreviuen a girar la pantalla. */
class CineCatApplication : Application() {
    val container by lazy { AppContainer(KeystoreTokenStorage(this)) }
}
