package cat.cinecat.ui.movie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.cinecat.data.Movie
import cat.cinecat.data.Session
import cat.cinecat.ui.catalog.fieldColors
import cat.cinecat.ui.components.ErrorBox
import cat.cinecat.ui.components.Poster
import cat.cinecat.ui.components.PrimaryButton
import cat.cinecat.ui.components.RatingSelector
import cat.cinecat.ui.components.Segmented
import cat.cinecat.ui.theme.CineColors
import cat.cinecat.ui.theme.CineFonts
import kotlinx.coroutines.launch

/**
 * Pantalla 08 · Valorar: un full inferior (bottom sheet) sobre la fitxa.
 *
 * "Publica com a" Anònim / @àlies: és el MATEIX endpoint en els dos casos;
 * l'única diferència és si la petició porta el token (vegeu CineCatApi).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateSheet(
    viewModel: MovieViewModel,
    movie: Movie,
    posterUrl: String?,
    session: Session?,
    onDismiss: () -> Unit,
    onLogin: () -> Unit,
    onSent: () -> Unit,
) {
    val rate by viewModel.rate.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CineColors.Surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Poster(posterUrl, movie.title, Modifier.width(44.dp), radius = 6.dp, overlay = false)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("VALORES", fontFamily = CineFonts.mono, fontSize = 10.5.sp, letterSpacing = 1.5.sp, color = CineColors.TextFaint)
                    Text(movie.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = CineColors.Text)
                    Text(movie.year.toString(), fontFamily = CineFonts.mono, fontSize = 12.sp, color = CineColors.TextMuted)
                }
            }

            Text("La teva nota", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = CineColors.TextMuted)
            RatingSelector(rate.score, viewModel::setScore)

            OutlinedTextField(
                value = rate.comment,
                onValueChange = viewModel::setComment,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Escriu la teva ressenya (opcional)…", color = CineColors.TextFaint) },
                minLines = 3,
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors(),
            )

            Text("Publica com a", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = CineColors.TextMuted)
            if (session != null) {
                Segmented(
                    options = listOf("Anònim", "@${session.user.alias}"),
                    selected = if (rate.anonymous) 0 else 1,
                    onSelect = { viewModel.setAnonymous(it == 0) },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Segmented(
                    options = listOf("Anònim", "@àlies"),
                    selected = 0,
                    // Sense sessió, "@àlies" porta a iniciar-ne una.
                    onSelect = { if (it == 1) onLogin() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (rate.anonymous) {
                OutlinedTextField(
                    value = rate.authorLabel,
                    onValueChange = viewModel::setAuthorLabel,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("El teu nom (opcional)", color = CineColors.TextFaint) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors(),
                )
            }

            rate.error?.let { ErrorBox(it) }

            PrimaryButton(
                if (rate.sending) "Enviant…" else "Envia la valoració",
                enabled = !rate.sending,
                onClick = {
                    scope.launch {
                        if (viewModel.submitRating()) {
                            sheetState.hide()
                            onSent()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                if (rate.anonymous) "Pots valorar sense iniciar sessió." else "Es publicarà amb el teu àlies.",
                fontSize = 12.5.sp,
                color = CineColors.TextDim,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            if (session == null) {
                TextButton(onClick = onLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Inicia sessió per valorar amb el teu àlies", color = CineColors.Accent, fontSize = 13.sp)
                }
            }
        }
    }
}
