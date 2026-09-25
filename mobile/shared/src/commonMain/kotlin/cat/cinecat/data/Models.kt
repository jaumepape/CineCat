package cat.cinecat.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Models que calquen el contracte de l'API (docs/ESPECIFICACIO.md §4).
// Són EXACTAMENT el mateix JSON que consumeix el web: una sola API, dos
// clients. @SerialName tradueix el snake_case de l'API al camelCase de Kotlin.

@Serializable
data class Movie(
    val id: String,
    val title: String,
    val year: Int,
    @SerialName("duration_min") val durationMin: Int,
    val director: String,
    val synopsis: String = "",
    val genres: List<String> = emptyList(),
    val status: String,
    // Relativa ("/uploads/posters/<id>.jpg") o null si no té pòster.
    @SerialName("poster_url") val posterUrl: String? = null,
    // null quan encara no hi ha cap valoració (i no 0: "sense nota" ≠ "nota 0").
    @SerialName("avg_score") val avgScore: Double? = null,
    @SerialName("rating_count") val ratingCount: Int = 0,
)

@Serializable
data class Rating(
    val id: String,
    @SerialName("movie_id") val movieId: String,
    // null = valoració anònima. És l'única diferència amb una registrada.
    @SerialName("user_id") val userId: String? = null,
    // Àlies públic de l'autor registrat (mai l'email).
    @SerialName("user_alias") val userAlias: String? = null,
    val score: Int,
    val comment: String? = null,
    // Nom lliure que pot posar un anònim.
    @SerialName("author_label") val authorLabel: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class User(
    val id: String,
    val email: String,
    val alias: String,
    val role: String,
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: User,
)

// --- Cossos de les peticions ---

@Serializable
internal data class RatingBody(
    val score: Int,
    val comment: String? = null,
    @SerialName("author_label") val authorLabel: String? = null,
)

@Serializable
internal data class LoginBody(val email: String, val password: String)

@Serializable
internal data class RegisterBody(val alias: String, val email: String, val password: String)

@Serializable
internal data class ApiErrorBody(val error: String)
