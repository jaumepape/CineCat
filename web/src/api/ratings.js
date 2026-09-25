import { request, queryString } from './client.js'

/** Mida de pàgina que fa servir l'API (backend: ratingsPageSize). */
export const RATINGS_PAGE_SIZE = 20

/** Una pàgina de valoracions, les més recents primer. */
export function listRatings(movieId, page = 1) {
  return request(`/movies/${encodeURIComponent(movieId)}/ratings${queryString({ page })}`)
}

/**
 * Valoració anònima: NO enviem cap capçalera Authorization, i per això l'API
 * la desa amb user_id = NULL. A la Fase 4, amb sessió, aquesta mateixa funció
 * afegirà el token i l'API omplirà user_id.
 */
export function createRating(movieId, { score, comment, authorLabel }) {
  return request(`/movies/${encodeURIComponent(movieId)}/ratings`, {
    method: 'POST',
    body: { score, comment, author_label: authorLabel },
  })
}
