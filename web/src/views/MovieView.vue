<script setup>
// Vista Fitxa (docs/design 02 i 02b): pòster, metadades, mitjana, valoracions
// i formulari de valoració anònima.
import { computed, ref, watch } from 'vue'
import { getMovie } from '../api/movies.js'
import { listRatings, RATINGS_PAGE_SIZE } from '../api/ratings.js'
import RatingForm from '../components/RatingForm.vue'
import RatingItem from '../components/RatingItem.vue'
import { useAuthStore } from '../stores/auth.js'
import { useCatalogStore } from '../stores/catalog.js'
import { formatDuration, formatRatingCount, formatScore } from '../utils/format.js'

const props = defineProps({
  id: { type: String, required: true },
})

const catalog = useCatalogStore()
const auth = useAuthStore()

const movie = ref(null)
const status = ref('loading') // 'loading' | 'loaded' | 'notfound' | 'error'
const error = ref(null)

const ratings = ref([])
const page = ref(1)
const hasMore = ref(false)
const loadingMore = ref(false)

async function loadMovie() {
  movie.value = await getMovie(props.id)
}

// Carrega la pàgina 1 de valoracions (substituint la llista actual).
async function loadFirstRatings() {
  const items = await listRatings(props.id, 1)
  ratings.value = items
  page.value = 1
  // Si l'API n'ha tornat una pàgina plena, potser n'hi ha més.
  hasMore.value = items.length === RATINGS_PAGE_SIZE
}

async function loadAll() {
  status.value = 'loading'
  error.value = null
  try {
    // Les dues peticions en paral·lel: no depenen l'una de l'altra.
    await Promise.all([loadMovie(), loadFirstRatings()])
    status.value = 'loaded'
    document.title = `${movie.value.title} · CineCat`
  } catch (err) {
    status.value = err.status === 404 ? 'notfound' : 'error'
    error.value = err.message
  }
}

async function loadMore() {
  loadingMore.value = true
  try {
    const items = await listRatings(props.id, page.value + 1)
    ratings.value.push(...items)
    page.value += 1
    hasMore.value = items.length === RATINGS_PAGE_SIZE
  } catch (err) {
    error.value = err.message
  } finally {
    loadingMore.value = false
  }
}

// La valoració registrada de l'usuari amb sessió, si és a les pàgines
// carregades (les més recents primer: la pròpia sol ser a la primera).
// Si en té una però no s'ha carregat, l'API respondrà 409 en intentar-ne
// crear una altra i el formulari ho explicarà.
const ownRating = computed(() => {
  if (!auth.user) return null
  return ratings.value.find((r) => r.user_id === auth.user.id) ?? null
})

// Després de valorar o editar: tornem a demanar la fitxa (la mitjana i el
// recompte els CALCULA l'API a partir de la taula ratings; el web no els
// calcula mai) i la primera pàgina de valoracions, on ja hi ha la nova.
async function onRatingSaved() {
  catalog.invalidate() // les mitjanes del catàleg també han canviat
  await Promise.all([loadMovie(), loadFirstRatings()])
}

watch(() => props.id, loadAll, { immediate: true })

const meta = computed(() => {
  if (!movie.value) return ''
  const m = movie.value
  return [m.year, formatDuration(m.duration_min), `Dir. ${m.director}`].join(' · ')
})
const primaryGenre = computed(() => movie.value?.genres[0])

function scrollToForm() {
  document.getElementById('valorar')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<template>
  <div class="page detail">
    <p v-if="status === 'loaded' && movie.status === 'draft'" class="draft-note">
      Esborrany: només la veus perquè ets admin.
      <RouterLink :to="`/admin/pelicules/${movie.id}`">Edita-la</RouterLink>
    </p>
    <div v-if="status === 'loading'" class="hero" aria-busy="true">
      <div class="skeleton poster-skel"></div>
      <div class="info">
        <div class="skeleton" style="height: 38px; width: 60%"></div>
        <div class="skeleton" style="height: 14px; width: 40%; margin-top: 14px"></div>
        <div class="skeleton" style="height: 84px; width: 220px; margin-top: 24px; border-radius: 42px"></div>
      </div>
    </div>

    <div v-else-if="status === 'notfound'" class="empty-state">
      <h3>No hem trobat aquesta pel·lícula</h3>
      <p>Potser s'ha esborrat o l'enllaç no és correcte.</p>
      <RouterLink to="/" class="btn btn-primary">Torna al catàleg</RouterLink>
    </div>

    <div v-else-if="status === 'error'" class="empty-state" role="alert">
      <h3>No s'ha pogut carregar la fitxa</h3>
      <p>{{ error }}</p>
      <button class="btn btn-primary" type="button" @click="loadAll">Torna-ho a provar</button>
    </div>

    <template v-else>
      <nav class="breadcrumb" aria-label="Ruta">
        <RouterLink to="/">Catàleg</RouterLink>
        <template v-if="primaryGenre">
          <span aria-hidden="true">/</span>
          <RouterLink :to="{ path: '/', query: { genre: primaryGenre } }">{{ primaryGenre }}</RouterLink>
        </template>
        <span aria-hidden="true">/</span>
        <span class="current" aria-current="page">{{ movie.title }}</span>
      </nav>

      <section class="hero">
        <div class="poster" :class="{ empty: !movie.poster_url }">
          <img v-if="movie.poster_url" :src="movie.poster_url" :alt="`Pòster de ${movie.title}`" />
          <div v-else class="placeholder" aria-hidden="true">
            <span class="placeholder-box"></span>
            <span class="placeholder-text">SENSE PÒSTER</span>
          </div>
        </div>

        <div class="info">
          <div class="genres">
            <RouterLink v-for="g in movie.genres" :key="g" class="chip" :to="{ path: '/', query: { genre: g } }">{{ g }}</RouterLink>
          </div>
          <h1 class="title">{{ movie.title }}</h1>
          <p class="meta mono">{{ meta }}</p>

          <div class="score-row">
            <div class="score-block">
              <span class="score-circle mono" :class="{ none: movie.avg_score === null }">
                {{ formatScore(movie.avg_score) }}
              </span>
              <div>
                <p class="score-label">Nota mitjana</p>
                <p class="score-count mono">{{ formatRatingCount(movie.rating_count) }}</p>
              </div>
            </div>
            <div class="actions">
              <button class="btn btn-primary" type="button" @click="scrollToForm">Valora aquesta pel·lícula</button>
              <button class="btn btn-ghost" type="button" disabled title="Properament">+ A la meva llista</button>
            </div>
          </div>

          <template v-if="movie.synopsis">
            <p class="eyebrow">Sinopsi</p>
            <p class="synopsis">{{ movie.synopsis }}</p>
          </template>
        </div>
      </section>

      <section class="ratings" aria-labelledby="ratings-title">
        <div class="ratings-head">
          <h2 id="ratings-title">
            Valoracions <span class="count mono">· {{ movie.rating_count }}</span>
          </h2>
          <span class="order">Ordena: <strong>Més recents</strong></span>
        </div>

        <div id="valorar">
          <RatingForm :movie-id="movie.id" :own-rating="ownRating" @saved="onRatingSaved" />
        </div>

        <!-- 02b · Sense valoracions -->
        <div v-if="ratings.length === 0" class="empty-state no-ratings">
          <h3>Encara no hi ha valoracions</h3>
          <p>Sigues la primera persona a valorar aquesta pel·lícula.</p>
        </div>

        <div v-else class="list">
          <RatingItem v-for="r in ratings" :key="r.id" :rating="r" :own="r.id === ownRating?.id" @edit="scrollToForm" />
          <button v-if="hasMore" class="btn btn-ghost more" type="button" :disabled="loadingMore" @click="loadMore">
            {{ loadingMore ? 'Carregant…' : 'Carrega’n més' }}
          </button>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.detail {
  padding-top: 20px;
  padding-bottom: 80px;
}
.draft-note {
  margin: 0 0 16px;
  padding: 10px 14px;
  border-radius: var(--r-control);
  background: var(--raised);
  border: 1px dashed rgba(255, 255, 255, 0.2);
  color: var(--text-muted);
  font-size: 13px;
}
.draft-note a {
  color: var(--accent);
  margin-left: 6px;
}
.breadcrumb {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12.5px;
  color: var(--text-faint);
  margin-bottom: 22px;
}
.breadcrumb a {
  color: var(--text-muted);
}
.breadcrumb a:hover {
  color: var(--text);
}
.breadcrumb .current {
  color: var(--text-2);
}

.hero {
  display: flex;
  gap: 34px;
  align-items: flex-start;
}
.poster,
.poster-skel {
  flex: none;
  position: relative;
  width: 296px;
  aspect-ratio: 2 / 3;
  border-radius: var(--r-panel);
  overflow: hidden;
  box-shadow: var(--shadow-poster);
}
.poster {
  background: var(--no-poster);
  border: 1px solid rgba(255, 255, 255, 0.07);
}
.poster img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}
.placeholder-box {
  width: 56px;
  height: 56px;
  border: 1px dashed rgba(255, 255, 255, 0.22);
  border-radius: 8px;
}
.placeholder-text {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.14em;
  color: var(--text-faint);
}

.info {
  flex: 1;
  min-width: 0;
}
.genres {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.genres .chip:hover {
  color: var(--text);
}
.title {
  margin: 14px 0 8px;
  font-size: 38px;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.02em;
}
.meta {
  margin: 0;
  font-size: 13px;
  color: var(--text-muted);
}
.score-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 20px;
  margin: 26px 0 28px;
}
.score-block {
  display: flex;
  align-items: center;
  gap: 16px;
}
.score-circle {
  display: grid;
  place-items: center;
  width: 84px;
  height: 84px;
  border-radius: 50%;
  border: 2px solid var(--accent);
  color: var(--accent);
  font-size: 26px;
  font-weight: 700;
}
.score-circle.none {
  border-color: var(--text-faint);
  color: var(--text-faint);
}
.score-label {
  margin: 0;
  font-weight: 600;
}
.score-count {
  margin: 2px 0 0;
  font-size: 12.5px;
  color: var(--text-muted);
}
.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.eyebrow {
  margin: 0 0 8px;
}
.synopsis {
  margin: 0;
  max-width: 680px;
  font-size: 15px;
  line-height: 1.6;
  color: var(--text-2);
}

.ratings {
  margin-top: 44px;
  padding-top: 30px;
  border-top: 1px solid var(--border-faint);
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.ratings-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}
.ratings-head h2 {
  margin: 0;
  font-size: 19px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.ratings-head .count {
  font-size: 12.5px;
  font-weight: 400;
  color: var(--text-faint);
  letter-spacing: 0;
}
.order {
  font-size: 12.5px;
  color: var(--text-muted);
}
.order strong {
  color: var(--text);
  font-weight: 600;
}
#valorar {
  scroll-margin-top: 80px; /* que la capçalera fixa no el tapi */
}
.more {
  align-self: center;
  margin-top: 12px;
}
.list {
  display: flex;
  flex-direction: column;
}

@media (max-width: 760px) {
  .hero {
    flex-direction: column;
    gap: 22px;
  }
  .poster,
  .poster-skel {
    width: min(240px, 70%);
    align-self: center;
  }
  .title {
    font-size: 30px;
  }
}
</style>
