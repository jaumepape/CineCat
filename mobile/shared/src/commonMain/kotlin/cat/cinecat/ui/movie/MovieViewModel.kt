package cat.cinecat.ui.movie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.cinecat.data.ApiException
import cat.cinecat.data.CineCatApi
import cat.cinecat.data.Movie
import cat.cinecat.data.RATINGS_PAGE_SIZE
import cat.cinecat.data.Rating
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MovieState(
    val movie: Movie? = null,
    val ratings: List<Rating> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val status: Status = Status.Loading,
    val error: String? = null,
) {
    enum class Status { Loading, Loaded, NotFound, Error }
}

/** Formulari de valoració (el bottom sheet 08). */
data class RateState(
    val score: Int? = null,
    val comment: String = "",
    val authorLabel: String = "",
    val anonymous: Boolean = true,
    val sending: Boolean = false,
    val error: String? = null,
)

class MovieViewModel(private val api: CineCatApi, private val movieId: String) : ViewModel() {
    private val _state = MutableStateFlow(MovieState())
    val state: StateFlow<MovieState> = _state.asStateFlow()

    private val _rate = MutableStateFlow(RateState())
    val rate: StateFlow<RateState> = _rate.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(status = MovieState.Status.Loading, error = null) }
            try {
                reload()
                _state.update { it.copy(status = MovieState.Status.Loaded) }
            } catch (e: ApiException) {
                _state.update {
                    it.copy(status = if (e.status == 404) MovieState.Status.NotFound else MovieState.Status.Error, error = e.message)
                }
            }
        }
    }

    /** Fitxa + primera pàgina de valoracions, en paral·lel. */
    private suspend fun reload() {
        val movie = viewModelScope.async { api.getMovie(movieId) }
        val ratings = viewModelScope.async { api.listRatings(movieId, 1) }
        val items = ratings.await()
        _state.update { it.copy(movie = movie.await(), ratings = items, page = 1, hasMore = items.size == RATINGS_PAGE_SIZE) }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || !s.hasMore) return
        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true) }
            try {
                val items = api.listRatings(movieId, s.page + 1)
                _state.update { it.copy(ratings = it.ratings + items, page = it.page + 1, hasMore = items.size == RATINGS_PAGE_SIZE) }
            } catch (_: ApiException) {
                // Es pot tornar a provar amb el mateix botó.
            } finally {
                _state.update { it.copy(loadingMore = false) }
            }
        }
    }

    // --- Formulari de valoració ---

    /** En obrir el full: amb sessió, per defecte es publica amb l'àlies. */
    fun startRating(loggedIn: Boolean) = _rate.update { RateState(anonymous = !loggedIn) }

    fun setScore(score: Int) = _rate.update { it.copy(score = score, error = null) }
    fun setComment(comment: String) = _rate.update { it.copy(comment = comment) }
    fun setAuthorLabel(label: String) = _rate.update { it.copy(authorLabel = label) }
    fun setAnonymous(anonymous: Boolean) = _rate.update { it.copy(anonymous = anonymous) }

    /**
     * Envia la valoració. Retorna true si ha anat bé (el full es tanca). La
     * mitjana NO la calcula l'app: després de valorar, es torna a demanar la
     * fitxa i l'API la recalcula amb SQL.
     */
    suspend fun submitRating(): Boolean {
        val r = _rate.value
        if (r.score == null) {
            _rate.update { it.copy(error = "Tria una nota de l’1 al 10.") }
            return false
        }
        _rate.update { it.copy(sending = true, error = null) }
        return try {
            api.createRating(movieId, r.score, r.comment, r.authorLabel, r.anonymous)
            reload()
            _rate.value = RateState()
            true
        } catch (e: ApiException) {
            // "massa peticions seguides…" (429), "ja has valorat…" (409)...
            _rate.update { it.copy(sending = false, error = e.message) }
            false
        }
    }
}
