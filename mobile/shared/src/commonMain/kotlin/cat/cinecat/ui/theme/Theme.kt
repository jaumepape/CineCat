package cat.cinecat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import cinecat.shared.generated.resources.Res
import cinecat.shared.generated.resources.geist_bold
import cinecat.shared.generated.resources.geist_medium
import cinecat.shared.generated.resources.geist_regular
import cinecat.shared.generated.resources.geist_semibold
import cinecat.shared.generated.resources.geistmono_bold
import cinecat.shared.generated.resources.geistmono_medium
import cinecat.shared.generated.resources.geistmono_regular
import org.jetbrains.compose.resources.Font

/**
 * Design tokens de docs/design/README.md: els mateixos valors que
 * web/src/styles/tokens.css, perquè web i mòbil siguin el mateix producte.
 */
object CineColors {
    val Bg = Color(0xFF0D0E11)
    val Surface = Color(0xFF16181D)
    val Raised = Color(0xFF1D2026)
    val NoPoster = Color(0xFF1A1D22)

    val BorderFaint = Color.White.copy(alpha = 0.06f)
    val Border = Color.White.copy(alpha = 0.08f)
    val BorderStrong = Color.White.copy(alpha = 0.12f)
    val BorderGhost = Color.White.copy(alpha = 0.16f)

    val Text = Color(0xFFEEF0F2)
    val Text2 = Color(0xFFC8CCD2)
    val TextMuted = Color(0xFF9298A1)
    val TextDim = Color(0xFF7C828B)
    val TextFaint = Color(0xFF5D636C)

    val Accent = Color(0xFF2DD4BF)
    val OnAccent = Color(0xFF06231F)
    val Accent12 = Accent.copy(alpha = 0.12f)
    val Accent15 = Accent.copy(alpha = 0.15f)
    val Accent30 = Accent.copy(alpha = 0.30f)

    val Error = Color(0xFFFF5252)
    val ErrorText = Color(0xFFFF8A8A)
    val ErrorBg = Error.copy(alpha = 0.06f)
}

/** Geist (interfície) i Geist Mono (notes, anys, metadades), llicència OFL. */
object CineFonts {
    val sans: FontFamily
        @Composable get() = FontFamily(
            Font(Res.font.geist_regular, FontWeight.Normal),
            Font(Res.font.geist_medium, FontWeight.Medium),
            Font(Res.font.geist_semibold, FontWeight.SemiBold),
            Font(Res.font.geist_bold, FontWeight.Bold),
        )
    val mono: FontFamily
        @Composable get() = FontFamily(
            Font(Res.font.geistmono_regular, FontWeight.Normal),
            Font(Res.font.geistmono_medium, FontWeight.Medium),
            Font(Res.font.geistmono_bold, FontWeight.Bold),
        )
}

@Composable
fun CineCatTheme(content: @Composable () -> Unit) {
    val sans = CineFonts.sans
    val base = MaterialTheme.typography
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = CineColors.Accent,
            onPrimary = CineColors.OnAccent,
            background = CineColors.Bg,
            onBackground = CineColors.Text,
            surface = CineColors.Bg,
            onSurface = CineColors.Text,
            surfaceVariant = CineColors.Surface,
            onSurfaceVariant = CineColors.TextMuted,
            surfaceContainer = CineColors.Surface,
            surfaceContainerHigh = CineColors.Raised,
            surfaceContainerLow = CineColors.Surface,
            outline = CineColors.BorderGhost,
            error = CineColors.Error,
        ),
        // Totes les mides de Material, però amb Geist.
        typography = base.copy(
            displayLarge = base.displayLarge.copy(fontFamily = sans),
            headlineMedium = base.headlineMedium.copy(fontFamily = sans),
            headlineSmall = base.headlineSmall.copy(fontFamily = sans),
            titleLarge = base.titleLarge.copy(fontFamily = sans),
            titleMedium = base.titleMedium.copy(fontFamily = sans),
            titleSmall = base.titleSmall.copy(fontFamily = sans),
            bodyLarge = base.bodyLarge.copy(fontFamily = sans),
            bodyMedium = base.bodyMedium.copy(fontFamily = sans),
            bodySmall = base.bodySmall.copy(fontFamily = sans),
            labelLarge = base.labelLarge.copy(fontFamily = sans),
            labelMedium = base.labelMedium.copy(fontFamily = sans),
            labelSmall = base.labelSmall.copy(fontFamily = sans),
        ),
        content = content,
    )
}
