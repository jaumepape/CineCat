// Client HTTP mínim sobre fetch. Totes les crides a l'API passen per aquí,
// així el tractament d'errors i del token és el mateix a tot arreu.

/** Error de l'API amb el codi HTTP i el missatge de {"error": "..."}. */
export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

// El token de sessió el posa el store d'auth (stores/auth.js). Aquest mòdul
// no importa el store: així la capa d'API no depèn de Pinia ni de Vue.
let authToken = null
let onUnauthorized = () => {}

export function setAuthToken(token) {
  authToken = token
}

/** Què fer si l'API rebutja el nostre token (caducat o invàlid). */
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler
}

/**
 * Fa una petició a /api. Retorna el JSON de la resposta (o null si és 204).
 * Si l'API respon amb un error, llança un ApiError amb el missatge de l'API,
 * que la vista pot mostrar tal qual ("score ha d'estar entre 1 i 10").
 *
 * Opcions:
 *  - body: objecte (s'envia com a JSON) o FormData (multipart, per al pòster).
 *  - auth: false per NO enviar el token encara que hi hagi sessió (p. ex. un
 *    usuari amb sessió que tria valorar de manera anònima).
 */
export async function request(path, { method = 'GET', body, signal, auth = true } = {}) {
  const headers = {}
  const sendToken = auth && authToken
  if (sendToken) headers.Authorization = `Bearer ${authToken}`

  let payload
  if (body instanceof FormData) {
    // Sense Content-Type: el navegador hi posa "multipart/form-data;
    // boundary=..." amb el separador correcte.
    payload = body
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    payload = JSON.stringify(body)
  }

  let res
  try {
    res = await fetch(`/api${path}`, { method, signal, headers, body: payload })
  } catch (err) {
    if (err.name === 'AbortError') throw err
    // fetch només falla "de debò" si no hi ha connexió amb el servidor.
    throw new ApiError(0, "No s'ha pogut connectar amb el servidor.")
  }

  if (res.status === 204) return null
  const data = await res.json().catch(() => null)
  if (!res.ok) {
    // 401 havent enviat token = la sessió ja no val (caducada, o el secret
    // del servidor ha canviat): la tanquem. Si era una lectura (GET), la
    // repetim sense token: el catàleg és públic i no cal mostrar cap error.
    // (Un 401 SENSE token és simplement "cal iniciar sessió".)
    if (res.status === 401 && sendToken) {
      onUnauthorized()
      if (method === 'GET') return request(path, { method, signal, auth: false })
    }
    throw new ApiError(res.status, data?.error ? sentence(data.error) : `Error ${res.status}`)
  }
  return data
}

/**
 * L'API escriu els errors com a fragments en minúscula ("la contrasenya ha de
 * tenir…"), pensats per a qualsevol client. Per mostrar-los sols en un quadre
 * d'error, en fem una frase: majúscula inicial i punt final.
 */
function sentence(text) {
  const t = text.trim()
  if (!t) return t
  const capital = t.charAt(0).toLocaleUpperCase('ca') + t.slice(1)
  return /[.!?…]$/.test(capital) ? capital : `${capital}.`
}

/** Construeix "?a=1&b=2" ometent els valors buits. */
export function queryString(params) {
  const qs = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') qs.set(key, value)
  }
  const s = qs.toString()
  return s ? `?${s}` : ''
}
