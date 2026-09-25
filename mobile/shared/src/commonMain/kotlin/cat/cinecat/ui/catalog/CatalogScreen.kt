package cat.cinecat.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.cinecat.data.CineCatApi
import cat.cinecat.ui.components.EmptyState
import cat.cinecat.ui.components.FilterChip
import cat.cinecat.ui.components.MovieCard
import cat.cinecat.ui.components.PrimaryButton
import cat.cinecat.ui.theme.CineColors
import cat.cinecat.ui.theme.CineFonts
import cat.cinecat.util.GENRES
import cat.cinecat.util.formatCount

/** Pantalla 06 · Catàleg mòbil: cerca, xips de gènere i graella de 2 columnes. */
@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    api: CineCatApi,
    focusSearch: Boolean,
    onMovieClick: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    // La pestanya "Cerca" obre aquesta mateixa pantalla amb el camp enfocat.
    LaunchedEffect(focusSearch) { if (focusSearch) focus.requestFocus() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Tot el que no és una targeta ocupa les 2 columnes.
        item(span = { GridItemSpan(2) }) {
            Column {
                Logo(Modifier.padding(top = 14.dp, bottom = 14.dp))
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    placeholder = { Text("Cerca per títol…", color = CineColors.TextFaint) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors(),
                )
            }
        }
        item(span = { GridItemSpan(2) }) {
            // Una sola fila que llisca horitzontalment, com al disseny.
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 0.dp),
            ) {
                item { FilterChip("Tots", state.genre.isEmpty()) { viewModel.onGenreClick("") } }
                items(GENRES) { g -> FilterChip(g, state.genre == g) { viewModel.onGenreClick(g) } }
            }
        }
        item(span = { GridItemSpan(2) }) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Catàleg", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = CineColors.Text)
                if (state.status == CatalogState.Status.Loaded) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "· ${formatCount(state.movies.size)} pel·lícules",
                        fontFamily = CineFonts.mono,
                        fontSize = 12.5.sp,
                        color = CineColors.TextFaint,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
            }
        }

        when (state.status) {
            // 01c · Càrrega: targetes "esquelet".
            CatalogState.Status.Loading -> items(6) { SkeletonCard() }

            CatalogState.Status.Error -> item(span = { GridItemSpan(2) }) {
                EmptyState("No s'ha pogut carregar el catàleg", state.error ?: "") {
                    PrimaryButton("Torna-ho a provar", onClick = viewModel::refresh)
                }
            }

            CatalogState.Status.Loaded -> if (state.movies.isEmpty()) {
                // 01b · Sense resultats.
                item(span = { GridItemSpan(2) }) {
                    val filtered = state.query.isNotBlank() || state.genre.isNotEmpty()
                    EmptyState(
                        "Cap pel·lícula coincideix",
                        if (filtered) "Prova amb un altre títol o gènere." else "Encara no hi ha cap pel·lícula publicada.",
                    ) {
                        if (filtered) PrimaryButton("Esborra els filtres", onClick = viewModel::clearFilters)
                    }
                }
            } else {
                items(state.movies, key = { it.id }) { movie ->
                    MovieCard(movie, api.absoluteUrl(movie.posterUrl), onClick = { onMovieClick(movie.id) })
                }
            }
        }
    }
}

@Composable
fun Logo(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(28.dp).background(CineColors.Accent, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) { Text("C", color = CineColors.OnAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
        Spacer(Modifier.width(10.dp))
        Text("Cine", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CineColors.Text)
        Text("Cat", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CineColors.Accent)
    }
}

@Composable
private fun SkeletonCard() {
    Column {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).background(CineColors.Surface, RoundedCornerShape(8.dp)))
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth(0.8f).height(12.dp).background(CineColors.Surface, RoundedCornerShape(4.dp)))
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth(0.4f).height(10.dp).background(CineColors.Surface, RoundedCornerShape(4.dp)))
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CineColors.Accent,
    unfocusedBorderColor = CineColors.BorderStrong,
    focusedContainerColor = CineColors.Surface,
    unfocusedContainerColor = CineColors.Surface,
    cursorColor = CineColors.Accent,
    focusedTextColor = CineColors.Text,
    unfocusedTextColor = CineColors.Text,
)
