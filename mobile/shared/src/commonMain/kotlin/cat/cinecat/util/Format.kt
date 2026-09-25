package cat.cinecat.util

import kotlin.math.abs
import kotlin.math.roundToInt

// Format a la catalana, igual que web/src/utils/format.js: "7,8", "1.243",
// "1h 52min". Fet a mà (i no amb les APIs de cada sistema) perquè el
// resultat sigui idèntic a Android i a iOS, i perquè és ben poc codi.

/** 7.8 → "7,8"; 8.0 → "8,0"; null (sense valoracions) → "—". */
fun formatScore(score: Double?): String {
    if (score == null) return "—"
    val tenths = (score * 10).roundToInt()
    return "${tenths / 10},${abs(tenths % 10)}"
}

/** 1243 → "1.243" (punt de milers també amb 4 xifres, com el disseny). */
fun formatCount(n: Int): String {
    val digits = abs(n).toString()
    val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
    return if (n < 0) "-$grouped" else grouped
}

/** 1 → "1 valoració"; 1243 → "1.243 valoracions". */
fun formatRatingCount(n: Int): String = "${formatCount(n)} ${if (n == 1) "valoració" else "valoracions"}"

/** 112 → "1h 52min"; 45 → "45min"; 120 → "2h". */
fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}min"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}min"
    }
}

private val MONTHS = listOf("gen.", "febr.", "març", "abr.", "maig", "juny", "jul.", "ag.", "set.", "oct.", "nov.", "des.")

/** "2026-09-25T09:10:44Z" → "25 de set. 2026" (la data de l'ISO, en UTC). */
fun formatDate(iso: String): String {
    val (y, mo, d) = iso.take(10).split("-").map { it.toIntOrNull() ?: return iso }
    val month = MONTHS.getOrNull(mo - 1) ?: return iso
    val de = if (month.first() in "aeiouàèéíòóú") "d’" else "de "
    return "$d $de$month $y"
}

/** Etiquetes del RatingSelector (docs/design/README.md). */
val SCORE_LABELS = listOf(
    "", "Horrible", "Molt dolenta", "Dolenta", "Fluixa", "Regular",
    "Acceptable", "Bona", "Molt bona", "Excel·lent", "Obra mestra",
)

/** Mateixa llista tancada que valida el backend (internal/models/movie.go). */
val GENRES = listOf(
    "Drama", "Ciència-ficció", "Thriller", "Terror", "Romanç", "Aventura",
    "Misteri", "Crim", "Comèdia", "Acció", "Animació", "Documental",
)
