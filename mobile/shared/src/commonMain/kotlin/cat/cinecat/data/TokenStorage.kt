package cat.cinecat.data

/**
 * On es guarda la sessió (token + usuari) entre execucions de l'app.
 *
 * És codi de PLATAFORMA (§5: "emmagatzematge segur del token"): el token és
 * una credencial i cada sistema té el seu magatzem segur.
 *  - Android: xifrat amb una clau de l'Android Keystore (la clau no surt mai
 *    del maquinari segur del dispositiu).
 *  - iOS: el Keychain, xifrat pel sistema.
 * Al web, en canvi, vam fer servir localStorage: el navegador no té res
 * equivalent a l'abast del JavaScript.
 */
interface TokenStorage {
    fun load(): String?
    fun save(value: String?)
}

/** Per a tests i previsualitzacions: només en memòria. */
class InMemoryTokenStorage(private var value: String? = null) : TokenStorage {
    override fun load() = value
    override fun save(value: String?) {
        this.value = value
    }
}
