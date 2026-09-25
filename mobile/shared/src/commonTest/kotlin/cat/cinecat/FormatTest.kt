package cat.cinecat

import cat.cinecat.util.formatCount
import cat.cinecat.util.formatDate
import cat.cinecat.util.formatDuration
import cat.cinecat.util.formatScore
import kotlin.test.Test
import kotlin.test.assertEquals

/** Mateixos resultats que web/src/utils/format.js. */
class FormatTest {
    @Test
    fun score() {
        assertEquals("7,8", formatScore(7.8))
        assertEquals("8,0", formatScore(8.0))
        assertEquals("—", formatScore(null))
    }

    @Test
    fun count() {
        assertEquals("0", formatCount(0))
        assertEquals("999", formatCount(999))
        assertEquals("1.243", formatCount(1243))
        assertEquals("1.234.567", formatCount(1234567))
    }

    @Test
    fun duration() {
        assertEquals("1h 52min", formatDuration(112))
        assertEquals("45min", formatDuration(45))
        assertEquals("2h", formatDuration(120))
    }

    @Test
    fun date() {
        assertEquals("25 de set. 2026", formatDate("2026-09-25T09:10:44Z"))
        assertEquals("3 d’abr. 2025", formatDate("2025-04-03T00:00:00Z"))
    }
}
