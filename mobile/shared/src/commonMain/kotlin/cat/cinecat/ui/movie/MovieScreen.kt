package cat.cinecat.ui.movie

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.cinecat.data.CineCatApi
import cat.cinecat.data.Rating
import cat.cinecat.data.Session
import cat.cinecat.ui.components.EmptyState
import cat.cinecat.ui.components.GenreTag
import cat.cinecat.ui.components.GhostButton
import cat.cinecat.ui.components.Loading
import cat.cinecat.ui.components.PrimaryButton
import cat.cinecat.ui.theme.CineColors
import cat.cinecat.ui.theme.CineFonts
import cat.cinecat.util.formatCount
import cat.cinecat.util.formatDate
import cat.cinecat.util.formatDuration
import cat.cinecat.util.formatRatingCount
import cat.cinecat.util.formatScore
import coil3.compose.AsyncImage

/** Pantalla 07 · Fitxa mòbil, amb el CTA enganxat a baix i el full 08. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MovieScreen(
    viewModel: MovieViewModel,
    api: CineCatApi,
    session: Session?,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onRated: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheetOpen by remember { mutableStateOf(false) }
    val movie = state.movie

    Box(Modifier.fillMaxSize().background(CineColors.Bg)) {
        when {
            state.status == MovieState.Status.Loading && movie == null -> Loading(Modifier.statusBarsPadding().padding(top = 80.dp))
            state.status == MovieState.Status.NotFound -> Column(Modifier.statusBarsPadding().padding(16.dp)) {
                BackButton(onBack)
                Spacer(Modifier.height(24.dp))
                EmptyState("No hem trobat aquesta pel·lícula", "Potser s'ha esborrat o encara no s'ha publicat.")
            }
            state.status == MovieState.Status.Error && movie == null -> Column(Modifier.statusBarsPadding().padding(16.dp)) {
                BackButton(onBack)
                Spacer(Modifier.height(24.dp))
                EmptyState("No s'ha pogut carregar la fitxa", state.error ?: "") {
                    PrimaryButton("Torna-ho a provar", onClick = viewModel::load)
                }
            }
            movie != null -> {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp)) {
                    // Hero: el pòster de 300dp amb un gradient cap al fons.
                    item {
                        Box(Modifier.fillMaxWidth().height(300.dp).background(CineColors.NoPoster)) {
                            val url = api.absoluteUrl(movie.posterUrl)
                            if (url != null) {
                                AsyncImage(url, "Pòster de ${movie.title}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to CineColors.Bg)))
                            BackButton(onBack, Modifier.statusBarsPadding().padding(12.dp))
                        }
                    }
                    item {
                        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                movie.genres.forEach { GenreTag(it) }
                            }
                            Text(movie.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = CineColors.Text, lineHeight = 28.sp)
                            Text(
                                "${movie.year} · ${formatDuration(movie.durationMin)} · Dir. ${movie.director}",
                                fontFamily = CineFonts.mono,
                                fontSize = 12.5.sp,
                                color = CineColors.TextMuted,
                            )
                            Spacer(Modifier.height(6.dp))
                            ScoreBlock(movie.avgScore, movie.ratingCount)
                            if (movie.synopsis.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text("SINOPSI", fontFamily = CineFonts.mono, fontSize = 11.sp, letterSpacing = 1.5.sp, color = CineColors.TextFaint)
                                Text(movie.synopsis, fontSize = 15.sp, lineHeight = 23.sp, color = CineColors.Text2)
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("Valoracions", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = CineColors.Text)
                                Spacer(Modifier.width(8.dp))
                                Text("· ${formatCount(movie.ratingCount)}", fontFamily = CineFonts.mono, fontSize = 12.5.sp, color = CineColors.TextFaint, modifier = Modifier.padding(bottom = 3.dp))
                            }
                        }
                    }
                    if (state.ratings.isEmpty()) {
                        // 02b · Sense valoracions.
                        item {
                            EmptyState(
                                "Encara no hi ha valoracions",
                                "Sigues la primera persona a valorar aquesta pel·lícula.",
                                Modifier.padding(16.dp),
                            )
                        }
                    } else {
                        items(state.ratings, key = { it.id }) { rating ->
                            RatingItem(rating, own = session != null && rating.userId == session.user.id)
                        }
                        if (state.hasMore) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    GhostButton(if (state.loadingMore) "Carregant…" else "Carrega’n més", onClick = viewModel::loadMore)
                                }
                            }
                        }
                    }
                }

                // CTA enganxat a baix, per sobre de la barra de navegació del sistema.
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(0f to Color.Transparent, 0.35f to CineColors.Bg))
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
                ) {
                    PrimaryButton(
                        "Valora aquesta pel·lícula",
                        onClick = {
                            viewModel.startRating(loggedIn = session != null)
                            sheetOpen = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (sheetOpen) {
                    RateSheet(
                        viewModel = viewModel,
                        movie = movie,
                        posterUrl = api.absoluteUrl(movie.posterUrl),
                        session = session,
                        onDismiss = { sheetOpen = false },
                        onLogin = {
                            sheetOpen = false
                            onLogin()
                        },
                        onSent = {
                            sheetOpen = false
                            onRated()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(40.dp)
            .background(Color(0xB30D0E11), CircleShape)
            .border(1.dp, CineColors.Border, CircleShape)
            .clickable(role = Role.Button, onClick = onBack)
            .semantics { contentDescription = "Enrere" },
        contentAlignment = Alignment.Center,
    ) { Text("‹", fontSize = 26.sp, color = CineColors.Text, modifier = Modifier.padding(bottom = 3.dp)) }
}

/** Cercle de 62dp amb la mitjana + recompte (vora grisa si no hi ha notes). */
@Composable
private fun ScoreBlock(avg: Double?, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val color = if (avg == null) CineColors.TextFaint else CineColors.Accent
        Box(Modifier.size(62.dp).border(2.dp, color, CircleShape), contentAlignment = Alignment.Center) {
            Text(formatScore(avg), fontFamily = CineFonts.mono, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text("Nota mitjana", fontWeight = FontWeight.SemiBold, color = CineColors.Text)
            Text(formatRatingCount(count), fontFamily = CineFonts.mono, fontSize = 12.5.sp, color = CineColors.TextMuted)
        }
    }
}

/**
 * Una valoració: registrada → "@àlies"; anònima → el nom lliure o "Anònim".
 * Mai l'email: l'API ni tan sols l'envia.
 */
@Composable
private fun RatingItem(rating: Rating, own: Boolean) {
    val registered = rating.userAlias != null
    val name = rating.userAlias?.let { "@$it" } ?: rating.authorLabel ?: "Anònim"
    val anonymous = !registered && rating.authorLabel == null
    Column(Modifier.padding(horizontal = 16.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(CineColors.BorderFaint))
        Row(Modifier.padding(vertical = 14.dp)) {
            Box(
                Modifier.size(34.dp).background(if (registered) CineColors.Accent12 else CineColors.Raised, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (anonymous) "A" else name.removePrefix("@").take(1).uppercase(),
                    color = if (registered) CineColors.Accent else CineColors.TextDim,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(name, fontWeight = FontWeight.SemiBold, color = if (registered) CineColors.Text else CineColors.TextMuted, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                    if (own) Text("Teva", fontSize = 11.sp, color = CineColors.Accent, modifier = Modifier.border(1.dp, CineColors.Accent30, CircleShape).padding(horizontal = 7.dp, vertical = 1.dp))
                    Text(
                        rating.score.toString(),
                        fontFamily = CineFonts.mono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = CineColors.Accent,
                        modifier = Modifier.background(CineColors.Accent15, CircleShape).padding(horizontal = 8.dp, vertical = 1.dp),
                    )
                }
                Text(formatDate(rating.createdAt), fontFamily = CineFonts.mono, fontSize = 11.sp, color = CineColors.TextFaint)
                rating.comment?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = CineColors.Text2, fontSize = 14.sp, lineHeight = 21.sp)
                }
            }
        }
    }
}
