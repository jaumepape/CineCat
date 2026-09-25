import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '../api/auth.js'
import { setAuthToken, setUnauthorizedHandler } from '../api/client.js'

const STORAGE_KEY = 'cinecat.session'

/**
 * Sessió de l'usuari: el token JWT i les dades de l'usuari (currentUser).
 *
 * On es guarda el token? A localStorage, perquè la sessió sobrevisqui a
 * recarregar la pàgina. Contrapartida: qualsevol JavaScript que s'executi a
 * la pàgina el pot llegir, així que un forat XSS permetria robar-lo. Aquí ho
 * acceptem perquè:
 *  - Vue escapa tot el text que mostrem (no fem servir v-html), i no carreguem
 *    scripts de tercers;
 *  - el token caduca (7 dies).
 * L'alternativa més segura és una cookie HttpOnly (el JS no la pot llegir),
 * però obliga a protegir-se de CSRF i complica el client mòbil, que també fa
 * servir aquesta API.
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref(null)
  const user = ref(null)

  const isLoggedIn = computed(() => user.value !== null)
  const isAdmin = computed(() => user.value?.role === 'admin')

  function save(session) {
    token.value = session?.token ?? null
    user.value = session?.user ?? null
    setAuthToken(token.value)
    try {
      if (session) localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
      else localStorage.removeItem(STORAGE_KEY)
    } catch {
      // Navegació privada o emmagatzematge bloquejat: la sessió dura fins
      // que es tanqui la pestanya, i prou.
    }
  }

  /** Recupera la sessió desada, si n'hi ha i no ha caducat. */
  function restore() {
    let session = null
    try {
      session = JSON.parse(localStorage.getItem(STORAGE_KEY))
    } catch {
      session = null
    }
    if (session?.token && !isExpired(session.token)) save(session)
    else save(null)
  }

  async function login(credentials) {
    save(await authApi.login(credentials))
  }

  async function register(data) {
    save(await authApi.register(data))
  }

  function logout() {
    save(null)
  }

  // Si l'API rebutja el token (caducat, secret canviat...), tanquem la sessió.
  setUnauthorizedHandler(logout)

  return { token, user, isLoggedIn, isAdmin, restore, login, register, logout }
})

/**
 * Llegeix la caducitat ("exp") del token. El client NO verifica la signatura
 * (no té el secret, ni l'ha de tenir): només mira la data per no fer servir
 * un token que ja sap caducat. Qui decideix de debò si és vàlid és l'API.
 */
function isExpired(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    return typeof payload.exp !== 'number' || payload.exp * 1000 <= Date.now()
  } catch {
    return true
  }
}
