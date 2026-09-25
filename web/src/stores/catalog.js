import { defineStore } from 'pinia'
import { ref } from 'vue'
import { listMovies } from '../api/movies.js'

/**
 * Estat del catàleg. Per què en un store (Pinia) i no dins la vista?
 * Perquè sobreviu quan canviem de pàgina: si obres una fitxa i tornes enrere,
 * el catàleg es mostra a l'instant amb les dades que ja teníem, sense tornar
 * a veure l'skeleton.
 *
 * Els FILTRES (q, genre), en canvi, viuen a la URL (/?q=...&genre=...), no
 * aquí: així es poden compartir i el botó enrere del navegador funciona.
 */
export const useCatalogStore = defineStore('catalog', () => {
  const movies = ref([])
  // 'idle' | 'loading' | 'loaded' | 'error'
  const status = ref('idle')
  const error = ref(null)
  // Filtres de l'última càrrega, per saber si les dades en memòria serveixen.
  const loadedKey = ref(null)

  let controller = null

  async function load({ q = '', genre = '' } = {}, { force = false } = {}) {
    const key = JSON.stringify({ q, genre })
    if (!force && key === loadedKey.value && status.value === 'loaded') return

    // Si l'usuari escriu ràpid, la resposta d'una cerca vella podria arribar
    // DESPRÉS de la nova i trepitjar-la. Cancel·lem la petició anterior.
    controller?.abort()
    controller = new AbortController()

    // Si ja teníem dades, les deixem visibles mentre arriben les noves (sense
    // tornar a l'skeleton a cada lletra que s'escriu).
    if (status.value !== 'loaded') status.value = 'loading'
    error.value = null
    try {
      movies.value = await listMovies({ q, genre }, { signal: controller.signal })
      loadedKey.value = key
      status.value = 'loaded'
    } catch (err) {
      if (err.name === 'AbortError') return
      error.value = err.message
      status.value = 'error'
    }
  }

  /** Les mitjanes han canviat (algú ha valorat): la propera visita recarrega. */
  function invalidate() {
    loadedKey.value = null
  }

  return { movies, status, error, load, invalidate }
})
