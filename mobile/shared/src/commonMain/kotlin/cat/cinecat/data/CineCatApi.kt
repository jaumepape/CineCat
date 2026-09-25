package cat.cinecat.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Error de l'API amb el codi HTTP i el missatge de {"error": "..."}, que la
 * UI pot mostrar tal qual ("score ha d'estar entre 1 i 10"). status = 0 vol
 * dir que no s'ha pogut connectar.
 */
class ApiException(val status: Int, message: String) : Exception(message)

/** Mida de pàgina de les valoracions (backend: ratingsPageSize). */
const val RATINGS_PAGE_SIZE = 20

/**
 * Client de l'API de CineCat: una funció per endpoint, com web/src/api/.
 *
 * @param tokenProvider retorna el token de la sessió actual (o null). És una
 *   funció i no un valor perquè el token canvia quan s'entra o se surt.
 * @param onUnauthorized es crida si l'API rebutja el nostre token (401):
 *   la sessió ja no val i s'ha de tancar.
 */
class CineCatApi(
    val baseUrl: String,
    private val tokenProvider: () -> String? = { null },
    private val onUnauthorized: () -> Unit = {},
    client: HttpClient? = null,
) {
    // ignoreUnknownKeys: si l'API afegeix un camp nou, l'app no peta; el
    // contracte pot créixer sense trencar clients vells.
    private val json = Json { ignoreUnknownKeys = true }

    private val http: HttpClient = (client ?: createHttpClient {}).config {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 20_000
        }
        defaultRequest { url(baseUrl) }
        // Els errors els tractem nosaltres (vegeu `send`), no Ktor.
        expectSuccess = false
    }

    /** URL absoluta d'un pòster: l'API la retorna relativa. */
    fun absoluteUrl(path: String?): String? = path?.let { baseUrl.trimEnd('/') + it }

    // --- Catàleg ---

    /** Només les publicades: els esborranys no són per al públic. */
    suspend fun listMovies(q: String = "", genre: String = ""): List<Movie> =
        send { get("api/movies") {
            if (q.isNotBlank()) parameter("q", q)
            if (genre.isNotBlank()) parameter("genre", genre)
            parameter("status", "published")
            withAuth()
        } }.body()

    suspend fun getMovie(id: String): Movie = send { get("api/movies/$id") { withAuth() } }.body()

    // --- Valoracions ---

    suspend fun listRatings(movieId: String, page: Int = 1): List<Rating> =
        send { get("api/movies/$movieId/ratings") { parameter("page", page); withAuth() } }.body()

    /**
     * MATEIX endpoint per a anònims i registrats:
     *  - anonymous = true  → sense token → l'API desa user_id = NULL;
     *  - anonymous = false → amb el token → l'API hi posa l'usuari i l'àlies.
     */
    suspend fun createRating(
        movieId: String,
        score: Int,
        comment: String?,
        authorLabel: String?,
        anonymous: Boolean,
    ): Rating = send {
        post("api/movies/$movieId/ratings") {
            contentType(ContentType.Application.Json)
            setBody(RatingBody(score, comment?.ifBlank { null }, if (anonymous) authorLabel?.ifBlank { null } else null))
            if (!anonymous) withAuth()
        }
    }.body()

    suspend fun updateRating(ratingId: String, score: Int, comment: String?): Rating = send {
        put("api/ratings/$ratingId") {
            contentType(ContentType.Application.Json)
            setBody(RatingBody(score, comment?.ifBlank { null }))
            withAuth()
        }
    }.body()

    // --- Sessió ---

    suspend fun login(email: String, password: String): AuthResponse = send {
        post("api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginBody(email, password))
        }
    }.body()

    suspend fun register(alias: String, email: String, password: String): AuthResponse = send {
        post("api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterBody(alias, email, password))
        }
    }.body()

    // --- Intern ---

    private fun HttpRequestBuilder.withAuth() {
        tokenProvider()?.let { bearerAuth(it) }
    }

    /**
     * Executa la petició i converteix qualsevol error en ApiException amb el
     * missatge de l'API. Tots els endpoints passen per aquí.
     */
    private suspend fun send(block: suspend HttpClient.() -> HttpResponse): HttpResponse {
        val response = try {
            http.block()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e // la cancel·lació d'una corrutina no és un error
        } catch (e: Exception) {
            throw ApiException(0, "No s'ha pogut connectar amb el servidor.")
        }
        if (response.status.isSuccess()) return response

        // 401 havent enviat token = la sessió ja no val (caducada, o el secret
        // del servidor ha canviat): la tanquem. Si era una lectura, la repetim
        // (ara ja sense token): el catàleg és públic i no cal mostrar cap error.
        val request = response.call.request
        if (response.status == HttpStatusCode.Unauthorized && request.headers["Authorization"] != null) {
            onUnauthorized()
            if (request.method == HttpMethod.Get) return send(block)
        }
        val message = runCatching { json.decodeFromString<ApiErrorBody>(response.bodyAsText()).error }
            .getOrNull() ?: "Error ${response.status.value}"
        throw ApiException(response.status.value, message)
    }
}
