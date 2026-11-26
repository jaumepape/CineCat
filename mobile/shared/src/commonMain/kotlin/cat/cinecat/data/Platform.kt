package cat.cinecat.data

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

// Aquí és on es veu la frontera "compartit vs. natiu" (§5): tot el codi que
// crida l'API és comú, però QUIN motor HTTP fa servir i A ON és l'API depenen
// del sistema operatiu. `expect` declara què cal; cada plataforma ho
// implementa amb `actual` (androidMain / iosMain).

/**
 * Client HTTP amb el motor natiu de cada plataforma: OkHttp a Android i
 * NSURLSession (motor Darwin) a iOS. La configuració (JSON, timeouts...) és
 * la mateixa per a tots dos i la passa el codi comú.
 */
expect fun createHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient

/**
 * URL de l'API en desenvolupament (valor per defecte; cada app passa la seva
 * a AppContainer segons el build: vegeu androidApp/build.gradle.kts i
 * iosApp/Configuration/Config.xcconfig). Canvia per plataforma perquè "localhost"
 * no vol dir el mateix a tot arreu: a l'emulador d'Android és l'emulador
 * mateix, i l'ordinador amfitrió és 10.0.2.2. Al simulador d'iOS, localhost
 * sí que és el Mac.
 */
expect val defaultBaseUrl: String
