package cat.cinecat.data

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp

// Android: motor OkHttp, la llibreria HTTP de referència a Android.
actual fun createHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(OkHttp, config)

// A l'emulador, 10.0.2.2 és l'adreça de l'ordinador amfitrió (on corre el
// backend a :8080). "localhost" seria l'emulador mateix.
actual val defaultBaseUrl: String = "http://10.0.2.2:8080"
