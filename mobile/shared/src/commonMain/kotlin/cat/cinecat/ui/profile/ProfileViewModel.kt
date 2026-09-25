package cat.cinecat.ui.profile

import androidx.lifecycle.ViewModel
import cat.cinecat.data.ApiException
import cat.cinecat.data.CineCatApi
import cat.cinecat.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ProfileState(
    val registering: Boolean = false, // false = entrar, true = crear compte
    val alias: String = "",
    val email: String = "",
    val password: String = "",
    val sending: Boolean = false,
    val error: String? = null,
)

class ProfileViewModel(private val api: CineCatApi, private val session: SessionManager) : ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    fun setMode(registering: Boolean) = _state.update { it.copy(registering = registering, error = null) }
    fun setAlias(v: String) = _state.update { it.copy(alias = v) }
    fun setEmail(v: String) = _state.update { it.copy(email = v) }
    fun setPassword(v: String) = _state.update { it.copy(password = v) }

    suspend fun submit(): Boolean {
        val s = _state.value
        _state.update { it.copy(sending = true, error = null) }
        return try {
            val response = if (s.registering) api.register(s.alias, s.email, s.password) else api.login(s.email, s.password)
            session.start(response)
            _state.value = ProfileState() // la contrasenya no es queda a memòria
            true
        } catch (e: ApiException) {
            _state.update { it.copy(sending = false, error = e.message) }
            false
        }
    }

    fun logout() = session.logout()
}
