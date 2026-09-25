package cat.cinecat

import cat.cinecat.data.ApiException
import cat.cinecat.data.CineCatApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Tests del client de l'API amb un servidor fals (MockEngine): comproven que
 * el JSON REAL de l'API (copiat d'una resposta del backend) es llegeix bé i
 * que els errors {"error": "..."} arriben amb el seu missatge.
 */
class ApiTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun api(status: HttpStatusCode, body: String, onRequest: (String) -> Unit = {}) = CineCatApi(
        baseUrl = "http://api.test",
        tokenProvider = { "tok" },
        client = HttpClient(MockEngine { request ->
            onRequest(request.url.toString() + " auth=" + request.headers[HttpHeaders.Authorization])
            respond(body, status, jsonHeaders)
        }),
    )

    @Test
    fun movieSenseValoracionsTeAvgScoreNull() = runTest {
        val body = """[{"id":"b6a6","title":"Vidres Trencats","year":2021,"duration_min":110,
            "director":"Sergi Aloy","synopsis":"…","genres":["Drama","Crim"],"status":"published",
            "poster_url":null,"created_at":"2026-09-25T08:28:44Z","avg_score":null,"rating_count":0,
            "camp_nou_de_l_api":true}]"""
        var url = ""
        val movies = api(HttpStatusCode.OK, body) { url = it }.listMovies(q = "vidres")

        assertEquals(1, movies.size)
        assertNull(movies[0].avgScore)
        assertEquals(0, movies[0].ratingCount)
        assertEquals(110, movies[0].durationMin)
        // Sempre només les publicades.
        assertTrue("status=published" in url, url)
        assertTrue("q=vidres" in url, url)
    }

    @Test
    fun ratingRegistradaPortaAlies() = runTest {
        val body = """[{"id":"r1","movie_id":"m1","user_id":"u1","user_alias":"joancinema","score":7,
            "comment":"Bona","author_label":null,"created_at":"2026-09-25T09:10:44Z"},
            {"id":"r2","movie_id":"m1","user_id":null,"user_alias":null,"score":4,"comment":null,
            "author_label":"Marta","created_at":"2026-09-25T09:10:44Z"}]"""
        val ratings = api(HttpStatusCode.OK, body).listRatings("m1")

        assertEquals("joancinema", ratings[0].userAlias)
        assertNull(ratings[1].userId)
        assertEquals("Marta", ratings[1].authorLabel)
    }

    @Test
    fun valoracioAnonimaNoEnviaElToken() = runTest {
        val body = """{"id":"r3","movie_id":"m1","user_id":null,"user_alias":null,"score":8,
            "comment":null,"author_label":"Joan","created_at":"2026-09-25T09:10:44Z"}"""
        var sent = ""
        api(HttpStatusCode.Created, body) { sent = it }
            .createRating("m1", score = 8, comment = "", authorLabel = "Joan", anonymous = true)
        assertTrue(sent.endsWith("auth=null"), sent)
    }

    @Test
    fun errorsDeLApiPortenElMissatge() = runTest {
        val e = assertFailsWith<ApiException> {
            api(HttpStatusCode.Conflict, """{"error":"ja has valorat aquesta pel·lícula"}""")
                .createRating("m1", 5, null, null, anonymous = false)
        }
        assertEquals(409, e.status)
        assertEquals("ja has valorat aquesta pel·lícula", e.message)
    }
}
