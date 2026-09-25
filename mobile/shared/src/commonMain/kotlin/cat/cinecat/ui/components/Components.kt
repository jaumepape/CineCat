package cat.cinecat.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.cinecat.data.Movie
import cat.cinecat.ui.theme.CineColors
import cat.cinecat.ui.theme.CineFonts
import cat.cinecat.util.SCORE_LABELS
import cat.cinecat.util.formatScore
import coil3.compose.AsyncImage

/**
 * Pòster 2:3 amb l'estat "sense pòster" (docs/design: MovieCard). La URL ha
 * de ser absoluta: l'API la retorna relativa i CineCatApi.absoluteUrl hi
 * afegeix el servidor.
 */
@Composable
fun Poster(url: String?, title: String, modifier: Modifier = Modifier, radius: Dp = 8.dp, overlay: Boolean = true) {
    Box(
        modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(radius))
            .background(CineColors.NoPoster)
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(radius)),
    ) {
        if (url != null) {
            // Coil baixa la imatge (amb Ktor) i la guarda a la cache de
            // memòria i de disc: tornar a una pantalla no la torna a baixar.
            AsyncImage(
                model = url,
                contentDescription = "Pòster de $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (overlay) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.45f)),
                    ),
                )
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(42.dp).border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(6.dp)))
                Spacer(Modifier.height(10.dp))
                Text("SENSE PÒSTER", fontFamily = CineFonts.mono, fontSize = 8.5.sp, letterSpacing = 1.2.sp, color = CineColors.TextFaint)
            }
        }
    }
}

/** Xip de nota de la cantonada del pòster (sempre present; "—" sense notes). */
@Composable
fun ScoreChip(score: Double?, modifier: Modifier = Modifier) {
    Text(
        formatScore(score),
        modifier = modifier
            .background(Color(0xD108090B), CircleShape)
            .border(1.dp, CineColors.Accent.copy(alpha = 0.32f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = CineColors.Accent,
        fontFamily = CineFonts.mono,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
    )
}

/** Targeta de pel·lícula (docs/design: MovieCard): pòster + nota + títol + any. */
@Composable
fun MovieCard(movie: Movie, posterUrl: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clickable(onClickLabel = "Obre la fitxa", onClick = onClick)) {
        Box(Modifier.shadow(8.dp, RoundedCornerShape(8.dp), ambientColor = Color.Black, spotColor = Color.Black)) {
            Poster(posterUrl, movie.title, Modifier.fillMaxWidth())
            ScoreChip(movie.avgScore, Modifier.align(Alignment.TopEnd).padding(8.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(movie.title, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = CineColors.Text)
        Text(movie.year.toString(), fontFamily = CineFonts.mono, fontSize = 11.5.sp, color = CineColors.TextMuted)
    }
}

/**
 * Selector de nota 1–10 (docs/design: RatingSelector). En tocar un segment es
 * fixa la nota. (Al web hi ha previsualització amb el ratolí per sobre; en una
 * pantalla tàctil no hi ha "hover", així que el segment triat s'aixeca.)
 */
@Composable
fun RatingSelector(value: Int?, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text((value ?: 0).toString(), fontFamily = CineFonts.mono, fontWeight = FontWeight.Bold, fontSize = 38.sp, color = CineColors.Accent, lineHeight = 38.sp)
            Spacer(Modifier.width(8.dp))
            Text("/ 10", fontFamily = CineFonts.mono, fontSize = 14.sp, color = CineColors.TextFaint, modifier = Modifier.padding(bottom = 4.dp))
            Spacer(Modifier.weight(1f))
            Text(if (value != null) SCORE_LABELS[value] else "Tria una nota", fontSize = 13.sp, color = CineColors.TextMuted, modifier = Modifier.padding(bottom = 4.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (n in 1..10) {
                val active = value != null && n <= value
                val lift by animateFloatAsState(if (value == n) -2f else 0f)
                Box(
                    Modifier
                        .weight(1f)
                        .height(38.dp)
                        .graphicsLayer { translationY = lift * density }
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) CineColors.Accent else CineColors.Raised)
                        .clickable(role = Role.RadioButton) { onChange(n) }
                        .semantics {
                            selected = value == n
                            contentDescription = "$n, ${SCORE_LABELS[n]}"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(n.toString(), fontFamily = CineFonts.mono, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (active) CineColors.OnAccent else CineColors.TextDim)
                }
            }
        }
    }
}

/** Xip de filtre (gènere): inactiu gris, actiu teal. */
@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) CineColors.Accent15 else CineColors.Raised)
            .border(1.dp, if (selected) CineColors.Accent30 else CineColors.Border, CircleShape)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 13.dp, vertical = 7.dp),
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Medium,
        color = if (selected) CineColors.Accent else CineColors.TextMuted,
    )
}

/** Xip de gènere només informatiu (fitxa). */
@Composable
fun GenreTag(label: String) {
    Text(
        label,
        modifier = Modifier
            .background(CineColors.Raised, CircleShape)
            .border(1.dp, CineColors.Border, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = CineColors.TextMuted,
    )
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CineColors.Accent,
            contentColor = CineColors.OnAccent,
            disabledContainerColor = CineColors.Accent.copy(alpha = 0.4f),
            disabledContentColor = CineColors.OnAccent,
        ),
    ) { Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CineColors.BorderGhost),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) { Text(text, color = CineColors.Text, fontWeight = FontWeight.Medium) }
}

/** Missatge d'error de l'API dins d'un formulari. */
@Composable
fun ErrorBox(message: String, modifier: Modifier = Modifier) {
    Text(
        message,
        modifier = modifier
            .fillMaxWidth()
            .background(CineColors.ErrorBg, RoundedCornerShape(9.dp))
            .border(1.dp, CineColors.Error, RoundedCornerShape(9.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        color = CineColors.ErrorText,
        fontSize = 13.sp,
    )
}

/** Estat buit o d'error amb vora discontínua (01b, 02b). */
@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = CineColors.Text, textAlign = TextAlign.Center)
        Text(message, color = CineColors.TextMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(4.dp))
            action()
        }
    }
}

@Composable
fun Loading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = CineColors.Accent, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
    }
}

/** Segmented control (Anònim / @àlies). */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(CineColors.Surface, RoundedCornerShape(10.dp))
            .border(1.dp, CineColors.Border, RoundedCornerShape(10.dp))
            .padding(3.dp),
    ) {
        options.forEachIndexed { i, label ->
            Text(
                label,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (i == selected) CineColors.Accent15 else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelect(i) }
                    .semantics { this.selected = i == selected }
                    .padding(vertical = 9.dp),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (i == selected) CineColors.Accent else CineColors.TextDim,
            )
        }
    }
}
