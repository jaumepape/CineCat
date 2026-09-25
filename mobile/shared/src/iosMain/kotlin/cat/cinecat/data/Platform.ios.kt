package cat.cinecat.data

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin

// iOS: motor Darwin, que fa servir NSURLSession, el sistema HTTP natiu d'Apple.
actual fun createHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(Darwin, config)

// Al simulador, localhost és el Mac on corre el backend.
actual val defaultBaseUrl: String = "http://localhost:8080"
