package cat.cinecat.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.cinecat.data.Session
import cat.cinecat.ui.catalog.Logo
import cat.cinecat.ui.catalog.fieldColors
import cat.cinecat.ui.components.ErrorBox
import cat.cinecat.ui.components.GhostButton
import cat.cinecat.ui.components.PrimaryButton
import cat.cinecat.ui.components.Segmented
import cat.cinecat.ui.theme.CineColors
import cat.cinecat.ui.theme.CineFonts
import kotlinx.coroutines.launch

/**
 * Perfil: sense sessió, inicia sessió o crea un compte; amb sessió, l'àlies
 * i "Surt". El disseny no té pantalla de login mòbil: aquesta és la mínima
 * per poder valorar "com a @àlies", feta amb els tokens del disseny.
 */
@Composable
fun ProfileScreen(viewModel: ProfileViewModel, session: Session?, onLoggedIn: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Logo(Modifier.padding(top = 14.dp, bottom = 6.dp))

        if (session != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(CineColors.Surface, RoundedCornerShape(14.dp))
                    .border(1.dp, CineColors.BorderFaint, RoundedCornerShape(14.dp))
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(44.dp).background(CineColors.Accent15, CircleShape), contentAlignment = Alignment.Center) {
                    Text(session.user.alias.take(1).uppercase(), color = CineColors.Accent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("@${session.user.alias}", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = CineColors.Text)
                    Text(session.user.email, fontFamily = CineFonts.mono, fontSize = 12.sp, color = CineColors.TextMuted)
                }
            }
            Text(
                "Les teves valoracions es publiquen amb aquest àlies. El teu correu no el veu ningú.",
                color = CineColors.TextMuted,
                fontSize = 13.sp,
            )
            GhostButton("Surt", onClick = viewModel::logout, modifier = Modifier.fillMaxWidth())
            return@Column
        }

        Text(
            "Iniciar sessió és opcional: pots valorar de manera anònima. Amb un compte, les teves valoracions porten el teu àlies.",
            color = CineColors.Text2,
            fontSize = 13.5.sp,
            modifier = Modifier
                .fillMaxWidth()
                .background(CineColors.Accent12, RoundedCornerShape(12.dp))
                .border(1.dp, CineColors.Accent30, RoundedCornerShape(12.dp))
                .padding(14.dp),
        )

        Segmented(
            options = listOf("Inicia sessió", "Crea un compte"),
            selected = if (state.registering) 1 else 0,
            onSelect = { viewModel.setMode(it == 1) },
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.registering) {
            OutlinedTextField(
                value = state.alias,
                onValueChange = viewModel::setAlias,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Àlies públic") },
                prefix = { Text("@", fontFamily = CineFonts.mono, color = CineColors.TextFaint) },
                supportingText = { Text("De 3 a 20 caràcters: a-z, 0-9 o _.") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors(),
            )
        }
        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::setEmail,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Correu electrònic") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = fieldColors(),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::setPassword,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (state.registering) "Contrasenya (mínim 8 caràcters)" else "Contrasenya") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = fieldColors(),
        )

        state.error?.let { ErrorBox(it) }

        PrimaryButton(
            when {
                state.sending -> "Un moment…"
                state.registering -> "Crea el compte"
                else -> "Entra"
            },
            enabled = !state.sending,
            onClick = { scope.launch { if (viewModel.submit()) onLoggedIn() } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
