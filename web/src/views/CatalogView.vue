<script setup>
// Vista Catàleg (docs/design 01, 01b, 01c).
//
// Flux de dades: la URL (/?q=...&genre=...) és la font de veritat dels
// filtres. Escriure a la cerca → (debounce) → canvia la URL → el watch de la
// URL demana les dades al store → el store crida l'API.
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MovieCard from '../components/MovieCard.vue'
import { useCatalogStore } from '../stores/catalog.js'
import { GENRES } from '../utils/genres.js'
import { formatCount } from '../utils/format.js'

const route = useRoute()
const router = useRouter()
const catalog = useCatalogStore()

const q = computed(() => (typeof route.query.q === 'string' ? route.query.q : ''))
const genre = computed(() => (typeof route.query.genre === 'string' ? route.query.genre : ''))

// El camp de text té el seu propi estat: s'actualitza a cada tecla, però la
// URL (i la petició) només quan l'usuari fa una pausa de 300 ms. Sense
// debounce, escriure "tramuntana" farien 10 peticions.
const search = ref(q.value)
let debounce
watch(search, (value) => {
  clearTimeout(debounce)
  debounce = setTimeout(() => setFilters({ q: value.trim() }), 300)
})
// Si la URL canvia per una altra via (enrere, "Esborra els filtres"), el camp
// s'hi ha d'ajustar.
watch(q, (value) => {
  if (value !== search.value.trim()) search.value = value
})

function setFilters(changes) {
  const query = { q: q.value, genre: genre.value, ...changes }
  for (const key of Object.keys(query)) if (!query[key]) delete query[key]
  // replace i no push: cada lletra no ha de ser una entrada a l'historial.
  router.replace({ query })
}

function clearFilters() {
  clearTimeout(debounce)
  search.value = ''
  setFilters({ q: '', genre: '' })
}

watch([q, genre], () => catalog.load({ q: q.value, genre: genre.value }), { immediate: true })

// Ordenació al client: amb unes desenes de pel·lícules no cal demanar-ho a l'API.
const sort = ref('title')
const sorted = computed(() => {
  if (sort.value !== 'score') return catalog.movies
  return [...catalog.movies].sort((a, b) => (b.avg_score ?? -1) - (a.avg_score ?? -1))
})

const hasFilters = computed(() => Boolean(q.value || genre.value))
</script>

<template>
  <div class="page catalog">
    <div class="toolbar">
      <label class="search">
        <span class="visually-hidden">Cerca per títol</span>
        <svg class="search-icon" viewBox="0 0 20 20" aria-hidden="true">
          <circle cx="9" cy="9" r="6" />
          <path d="m14 14 4 4" />
        </svg>
        <input v-model="search" class="input" type="search" placeholder="Cerca per títol…" />
      </label>
    </div>

    <div class="chips" role="group" aria-label="Filtra per gènere">
      <button type="button" class="chip" :class="{ active: !genre }" :aria-pressed="!genre" @click="setFilters({ genre: '' })">
        Tots
      </button>
      <button
        v-for="g in GENRES"
        :key="g"
        type="button"
        class="chip"
        :class="{ active: genre === g }"
        :aria-pressed="genre === g"
        @click="setFilters({ genre: genre === g ? '' : g })"
      >
        {{ g }}
      </button>
    </div>

    <div class="heading">
      <h1>
        Catàleg
        <span v-if="catalog.status === 'loaded'" class="count mono">· {{ formatCount(catalog.movies.length) }} pel·lícules</span>
      </h1>
      <label class="sort">
        Ordena:
        <select v-model="sort">
          <option value="title">Títol</option>
          <option value="score">Millor valorades</option>
        </select>
      </label>
    </div>

    <!-- 01c · Càrrega: spinner + graella d'skeletons -->
    <div v-if="catalog.status === 'loading' || catalog.status === 'idle'" aria-busy="true">
      <p class="loading"><span class="spinner" aria-hidden="true"></span>Carregant el catàleg…</p>
      <div class="grid">
        <div v-for="n in 10" :key="n">
          <div class="skeleton skel-poster"></div>
          <div class="skeleton skel-line"></div>
          <div class="skeleton skel-line short"></div>
        </div>
      </div>
    </div>

    <div v-else-if="catalog.status === 'error'" class="empty-state" role="alert">
      <h3>No s'ha pogut carregar el catàleg</h3>
      <p>{{ catalog.error }}</p>
      <button class="btn btn-primary" type="button" @click="catalog.load({ q, genre }, { force: true })">Torna-ho a provar</button>
    </div>

    <!-- 01b · Sense resultats -->
    <div v-else-if="catalog.movies.length === 0" class="empty-state">
      <span class="empty-poster" aria-hidden="true">
        <svg viewBox="0 0 20 20"><circle cx="9" cy="9" r="6" /><path d="m14 14 4 4" /></svg>
      </span>
      <h3>Cap pel·lícula coincideix</h3>
      <p v-if="hasFilters">
        No hem trobat res<template v-if="q"> per «{{ q }}»</template><template v-if="genre"> a {{ genre }}</template>.
        Prova amb un altre títol o gènere.
      </p>
      <p v-else>Encara no hi ha cap pel·lícula publicada.</p>
      <div v-if="hasFilters" class="empty-actions">
        <button class="btn btn-primary" type="button" @click="clearFilters">Esborra els filtres</button>
      </div>
    </div>

    <div v-else class="grid">
      <MovieCard v-for="movie in sorted" :key="movie.id" :movie="movie" />
    </div>
  </div>
</template>

<style scoped>
.catalog {
  padding-top: 22px;
  padding-bottom: 60px;
}
.search {
  position: relative;
  display: block;
}
.search .input {
  padding-left: 40px;
  background: var(--surface);
}
.search-icon,
.empty-poster svg {
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
}
.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  width: 16px;
  height: 16px;
  transform: translateY(-50%);
  color: var(--text-faint);
  pointer-events: none;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 14px 0 24px;
}
/* A mòbil, una sola fila que llisca horitzontalment (sense scroll de pàgina). */
@media (max-width: 560px) {
  .chips {
    flex-wrap: nowrap;
    overflow-x: auto;
    margin-inline: calc(-1 * var(--page-x));
    padding-inline: var(--page-x);
    scrollbar-width: none;
  }
}
.heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}
h1 {
  margin: 0;
  font-size: 21px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.count {
  font-size: 12.5px;
  font-weight: 400;
  color: var(--text-faint);
  letter-spacing: 0;
  white-space: nowrap;
}
.sort {
  font-size: 12.5px;
  color: var(--text-muted);
  white-space: nowrap;
}
.sort select {
  margin-left: 4px;
  padding: 2px 4px;
  border: none;
  background: transparent;
  color: var(--text);
  font-weight: 600;
  cursor: pointer;
}
.sort option {
  background: var(--surface);
}

/* 5 columnes a escriptori (disseny), menys a pantalles estretes. */
.grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
}
@media (max-width: 1100px) {
  .grid {
    grid-template-columns: repeat(4, 1fr);
  }
}
@media (max-width: 820px) {
  .grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (max-width: 560px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 14px;
  }
}

.loading {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 18px;
  color: var(--text-muted);
}
.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--raised);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.skel-poster {
  aspect-ratio: 2 / 3;
  border-radius: var(--r-card);
}
.skel-line {
  height: 12px;
  margin-top: 10px;
}
.skel-line.short {
  width: 40%;
  margin-top: 6px;
}

.empty-poster {
  display: grid;
  place-items: center;
  width: 56px;
  height: 84px;
  border: 1px dashed rgba(255, 255, 255, 0.22);
  border-radius: 8px;
  color: var(--text-faint);
  margin-bottom: 6px;
}
.empty-poster svg {
  width: 22px;
  height: 22px;
}
.empty-actions {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}
</style>
