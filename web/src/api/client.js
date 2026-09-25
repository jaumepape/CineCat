// Client HTTP mínim sobre fetch. Totes les crides a l'API passen per aquí,
// així el tractament d'errors és el mateix a tot arreu.

/** Error de l'API amb el codi HTTP i el missatge de {"error": "..."}. */
export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

/**
 * Fa una petició a /api. Retorna el JSON de la resposta (o null si és 204).
 * Si l'API respon amb un error, llança un ApiError amb el missatge de l'API,
 * que la vista pot mostrar tal qual ("score ha d'estar entre 1 i 10").
 */
export async function request(path, { method = 'GET', body, signal } = {}) {
  let res
  try {
    res = await fetch(`/api${path}`, {
      method,
      signal,
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined,
    })
  } catch (err) {
    if (err.name === 'AbortError') throw err
    // fetch només falla "de debò" si no hi ha connexió amb el servidor.
    throw new ApiError(0, "No s'ha pogut connectar amb el servidor.")
  }

  if (res.status === 204) return null
  const data = await res.json().catch(() => null)
  if (!res.ok) {
    throw new ApiError(res.status, data?.error ?? `Error ${res.status}`)
  }
  return data
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
