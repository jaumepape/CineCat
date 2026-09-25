package cat.cinecat.data

/**
 * Les dependències de l'app, creades una sola vegada. Cada plataforma el
 * construeix al seu punt d'entrada (MainActivity / MainViewController) amb el
 * seu TokenStorage. Sense framework d'injecció: per a tres objectes no cal.
 */
class AppContainer(
    tokenStorage: TokenStorage,
    baseUrl: String = defaultBaseUrl,
) {
    val session = SessionManager(tokenStorage)
    val api = CineCatApi(
        baseUrl = baseUrl,
        tokenProvider = { session.token },
        onUnauthorized = { session.logout() },
    )
}
