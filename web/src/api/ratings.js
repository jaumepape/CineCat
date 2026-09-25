import { request, queryString } from './client.js'

/** Mida de pàgina que fa servir l'API (backend: ratingsPageSize). */
export const RATINGS_PAGE_SIZE = 20

/** Una pàgina de valoracions, les més recents primer. */
export function listRatings(movieId, page = 1) {
  return request(`/movies/${encodeURIComponent(movieId)}/ratings${queryString({ page })}`)
}

/**
 * Crea una valoració. MATEIX endpoint per als dos casos:
 *  - anonymous: true → sense capçalera Authorization → l'API desa user_id NULL
 *    (author_label és el nom lliure opcional).
 *  - amb sessió i anonymous: false → s'envia el token → l'API omple user_id i
 *    mostra l'àlies.
 */
export function createRating(movieId, { score, comment, authorLabel, anonymous }) {
  return request(`/movies/${encodeURIComponent(movieId)}/ratings`, {
    method: 'POST',
    body: anonymous ? { score, comment, author_label: authorLabel } : { score, comment },
    auth: !anonymous,
  })
}

/** Edita la valoració pròpia (només l'autor: si no, 403). */
export function updateRating(ratingId, { score, comment }) {
  return request(`/ratings/${encodeURIComponent(ratingId)}`, { method: 'PUT', body: { score, comment } })
}
