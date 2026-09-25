import { request, queryString } from './client.js'

/**
 * Llistat de pel·lícules. Ara és l'API qui amaga els esborranys a qui no és
 * admin (abans de la Fase 4 ho havia de demanar el client).
 *  - Catàleg públic: status 'published' (fins i tot l'admin hi veu el mateix
 *    que un visitant).
 *  - Àrea d'admin: sense status → publicades i esborranys.
 */
export function listMovies({ q, genre, status } = {}, { signal } = {}) {
  return request(`/movies${queryString({ q, genre, status })}`, { signal })
}

/** Fitxa: pel·lícula + avg_score + rating_count. */
export function getMovie(id) {
  return request(`/movies/${encodeURIComponent(id)}`)
}

// --- Només admin (l'API respon 401/403 si no ho ets) ---

export function createMovie(data) {
  return request('/movies', { method: 'POST', body: data })
}

export function updateMovie(id, data) {
  return request(`/movies/${encodeURIComponent(id)}`, { method: 'PUT', body: data })
}

export function deleteMovie(id) {
  return request(`/movies/${encodeURIComponent(id)}`, { method: 'DELETE' })
}

/** Puja (o substitueix) el pòster: multipart/form-data amb el camp "file". */
export function uploadPoster(id, file) {
  const form = new FormData()
  form.append('file', file)
  return request(`/movies/${encodeURIComponent(id)}/poster`, { method: 'POST', body: form })
}
