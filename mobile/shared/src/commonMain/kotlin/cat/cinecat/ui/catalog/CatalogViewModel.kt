package cat.cinecat.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.cinecat.data.ApiException
import cat.cinecat.data.CineCatApi
import cat.cinecat.data.Movie
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estat de la pantalla de catàleg: el mateix que web/src/stores/catalog.js. */
data class CatalogState(
    val query: String = "",
    val genre: String = "",
    val movies: List<Movie> = emptyList(),
    val status: Status = Status.Loading,
    val error: String? = null,
) {
    enum class Status { Loading, Loaded, Error }
}

/**
 * ViewModel del catàleg. És codi COMÚ: la mateixa lògica (debounce de la
 * cerca, cancel·lar peticions velles, estats) serveix per a Android i iOS.
 * Sobreviu als canvis de configuració (girar la pantalla) i en tornar
 * d'una fitxa ja té les dades.
 */
@OptIn(FlowPreview::class)
class CatalogViewModel(private val api: CineCatApi) : ViewModel() {
    private val _state = MutableStateFlow(CatalogState())
    val state: StateFlow<CatalogState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
        // Cerca amb debounce: només es demana a l'API quan l'usuari fa una
        // pausa de 300 ms, no a cada lletra.
        _state.map { it.query.trim() }
            .distinctUntilChanged()
            .drop(1) // la càrrega inicial ja s'ha fet
            .debounce(300)
            .onEach { load() }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(query: String) = _state.update { it.copy(query = query) }

    fun onGenreClick(genre: String) {
        _state.update { it.copy(genre = if (it.genre == genre) "" else genre) }
        load()
    }

    fun clearFilters() {
        _state.update { it.copy(query = "", genre = "") }
        load()
    }

    /** Recarrega (p. ex. en tornar d'una fitxa on s'ha valorat). */
    fun refresh() = load(showLoading = false)

    private fun load(showLoading: Boolean = _state.value.movies.isEmpty()) {
        // Si arriba una cerca nova, la petició vella es cancel·la: la seva
        // resposta ja no ens interessa i podria trepitjar la nova.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (showLoading) _state.update { it.copy(status = CatalogState.Status.Loading, error = null) }
            val s = _state.value
            try {
                val movies = api.listMovies(q = s.query.trim(), genre = s.genre)
                _state.update { it.copy(movies = movies, status = CatalogState.Status.Loaded, error = null) }
            } catch (e: ApiException) {
                _state.update { it.copy(status = CatalogState.Status.Error, error = e.message) }
            }
        }
    }
}
