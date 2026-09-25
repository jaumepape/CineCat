package cat.cinecat.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Les tres icones de la barra de pestanyes, dibuixades com a traç (24×24).
 * Per a tres icones no cal afegir tota la llibreria d'icones de Material.
 * El color real el posa qui les fa servir (tint).
 */
object Icons {
    private fun icon(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        ).build()

    val Home: ImageVector = icon("home") {
        moveTo(4f, 10.5f); lineTo(12f, 4f); lineTo(20f, 10.5f)
        moveTo(6f, 9f); verticalLineTo(20f); horizontalLineTo(18f); verticalLineTo(9f)
        moveTo(10f, 20f); verticalLineTo(14f); horizontalLineTo(14f); verticalLineTo(20f)
    }

    val Search: ImageVector = icon("search") {
        moveTo(17f, 11f)
        arcTo(6f, 6f, 0f, true, true, 5f, 11f)
        arcTo(6f, 6f, 0f, true, true, 17f, 11f)
        moveTo(15.5f, 15.5f); lineTo(20f, 20f)
    }

    val Person: ImageVector = icon("person") {
        moveTo(16f, 8f)
        arcTo(4f, 4f, 0f, true, true, 8f, 8f)
        arcTo(4f, 4f, 0f, true, true, 16f, 8f)
        moveTo(4.5f, 20f)
        curveTo(5.5f, 16.5f, 8.5f, 14.5f, 12f, 14.5f)
        curveTo(15.5f, 14.5f, 18.5f, 16.5f, 19.5f, 20f)
    }
}
