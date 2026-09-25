import { request, queryString } from './client.js'

/**
 * Catàleg públic. Demanem només les publicades: sense auth (Fase 4) l'API
 * encara no sap si som admin, així que el web públic tria no veure els
 * esborranys.
 */
export function listMovies({ q, genre } = {}, { signal } = {}) {
  return request(`/movies${queryString({ q, genre, status: 'published' })}`, { signal })
}

/** Fitxa: pel·lícula + avg_score + rating_count. */
export function getMovie(id) {
  return request(`/movies/${encodeURIComponent(id)}`)
}
