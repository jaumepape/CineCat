package cat.cinecat

import cat.cinecat.data.CineCatApi
import cat.cinecat.data.Rating
import cat.cinecat.ui.movie.MovieViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * El full de valorar: amb valoració pròpia edita (PUT), si no en crea una
 * (POST). Servidor fals que registra quines peticions arriben.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MovieViewModelTest {
    // viewModelScope fa servir Dispatchers.Main: als tests el substituïm.
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val own = Rating(
        id = "r-own", movieId = "m1", userId = "u1", userAlias = "joan",
        score = 6, comment = "Correcta", createdAt = "2026-11-26T10:24:00Z",
    )

    private val requests = mutableListOf<String>()

    private fun viewModel(): MovieViewModel {
        val json = headersOf(HttpHeaders.ContentType, "application/json")
        val rating = """{"id":"r-own","movie_id":"m1","user_id":"u1","user_alias":"joan","score":9,
            "comment":"Millor","author_label":null,"created_at":"2026-11-26T10:24:00Z"}"""
        val client = HttpClient(MockEngine { req ->
            val auth = if (req.headers[HttpHeaders.Authorization] != null) " amb token" else " sense token"
            requests += "${req.method.value} ${req.url.encodedPath}$auth"
            when {
                req.method == HttpMethod.Get && req.url.encodedPath.endsWith("/ratings") -> respond("[]", HttpStatusCode.OK, json)
                req.method == HttpMethod.Get -> respond(
                    """{"id":"m1","title":"X","year":2021,"duration_min":90,"director":"Y","status":"published"}""",
                    HttpStatusCode.OK, json,
                )
                req.method == HttpMethod.Put -> respond(rating, HttpStatusCode.OK, json)
                else -> respond(rating, HttpStatusCode.Created, json)
            }
        })
        return MovieViewModel(CineCatApi("http://api.test", tokenProvider = { "tok" }, client = client), "m1")
    }

    @Test
    fun ambValoracioPropiaElFullEditaIFaPut() = runTest {
        val vm = viewModel()
        vm.startRating(loggedIn = true, own = own)

        val rate = vm.rate.value
        assertTrue(rate.editing)
        assertEquals(6, rate.score)
        assertEquals("Correcta", rate.comment)

        vm.setScore(9)
        vm.setComment("Millor")
        assertTrue(vm.submitRating())
        assertTrue("PUT /api/ratings/r-own amb token" in requests, requests.toString())
        assertFalse(requests.any { it.startsWith("POST") }, requests.toString())
    }

    @Test
    fun ambValoracioPropiaAnonimCreaUnaDeNovaSenseToken() = runTest {
        val vm = viewModel()
        vm.startRating(loggedIn = true, own = own)
        vm.setAnonymous(true)

        val rate = vm.rate.value
        assertFalse(rate.editing)
        assertNull(rate.score) // el formulari es buida: és una valoració nova

        vm.setScore(4)
        assertTrue(vm.submitRating())
        assertTrue("POST /api/movies/m1/ratings sense token" in requests, requests.toString())

        // Tornar a "@àlies" recupera la valoració pròpia per editar-la.
        vm.startRating(loggedIn = true, own = own)
        vm.setAnonymous(true)
        vm.setAnonymous(false)
        assertEquals(6, vm.rate.value.score)
        assertTrue(vm.rate.value.editing)
    }

    @Test
    fun senseValoracioPropiaCreaAmbLAlies() = runTest {
        val vm = viewModel()
        vm.startRating(loggedIn = true, own = null)
        assertFalse(vm.rate.value.editing)

        vm.setScore(8)
        assertTrue(vm.submitRating())
        assertTrue("POST /api/movies/m1/ratings amb token" in requests, requests.toString())
    }
}
