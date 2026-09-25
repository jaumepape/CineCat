package cat.cinecat.data

import kotlin.io.encoding.Base64
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

@Serializable
data class Session(val token: String, val user: User)

/**
 * Sessió de l'usuari (equivalent a web/src/stores/auth.js). Exposa un
 * StateFlow: les pantalles s'hi subscriuen i es redibuixen soles quan s'entra
 * o se surt.
 */
class SessionManager(private val storage: TokenStorage) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _session = MutableStateFlow(restore())
    val session: StateFlow<Session?> = _session.asStateFlow()

    val token: String? get() = _session.value?.token

    fun start(response: AuthResponse) = set(Session(response.token, response.user))

    fun logout() = set(null)

    private fun set(session: Session?) {
        _session.value = session
        storage.save(session?.let { json.encodeToString(Session.serializer(), it) })
    }

    /** Recupera la sessió desada si n'hi ha i no ha caducat. */
    private fun restore(): Session? {
        val saved = storage.load() ?: return null
        val session = runCatching { json.decodeFromString(Session.serializer(), saved) }.getOrNull()
        if (session == null || isExpired(session.token)) {
            storage.save(null)
            return null
        }
        return session
    }

    /**
     * Llegeix la caducitat ("exp") del JWT. El client NO en verifica la
     * signatura (no té el secret, ni l'ha de tenir): només evita fer servir
     * un token que ja sap caducat. Qui decideix si és vàlid és l'API.
     */
    private fun isExpired(token: String): Boolean = runCatching {
        val payload = token.split(".")[1]
        val decoded = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(payload).decodeToString()
        val exp = json.parseToJsonElement(decoded).jsonObject["exp"]!!.jsonPrimitive.long
        exp * 1000 <= Clock.System.now().toEpochMilliseconds()
    }.getOrDefault(true)
}
