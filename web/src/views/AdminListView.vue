<script setup>
// Vista 04 · Admin — Llistat del catàleg. Inclou els esborranys: l'API els
// retorna perquè la petició porta un token d'admin.
import { computed, ref, watch } from 'vue'
import { deleteMovie, listMovies } from '../api/movies.js'
import { useCatalogStore } from '../stores/catalog.js'
import { formatCount, formatScore } from '../utils/format.js'

const catalog = useCatalogStore()

const movies = ref([])
const status = ref('loading') // 'loading' | 'loaded' | 'error'
const error = ref(null)
const search = ref('')
const deleting = ref(null)

async function load() {
  error.value = null
  try {
    movies.value = await listMovies() // sense status: publicades + esborranys
    status.value = 'loaded'
  } catch (err) {
    error.value = err.message
    status.value = 'error'
  }
}
load()

// Cerca al client: la llista d'admin ja és sencera a memòria.
const filtered = computed(() => {
  const q = search.value.trim().toLocaleLowerCase('ca')
  if (!q) return movies.value
  return movies.value.filter(
    (m) => m.title.toLocaleLowerCase('ca').includes(q) || m.director.toLocaleLowerCase('ca').includes(q),
  )
})
watch(search, () => (error.value = null))

async function remove(movie) {
  // confirm() és bàsic però clar: esborrar no es pot desfer, i també
  // s'esborren les valoracions i el pòster.
  const ok = window.confirm(
    `Segur que vols esborrar «${movie.title}»?\nTambé se n'esborraran les ${movie.rating_count} valoracions i el pòster. No es pot desfer.`,
  )
  if (!ok) return
  deleting.value = movie.id
  try {
    await deleteMovie(movie.id)
    movies.value = movies.value.filter((m) => m.id !== movie.id)
    catalog.invalidate()
  } catch (err) {
    error.value = err.message
  } finally {
    deleting.value = null
  }
}
</script>

<template>
  <div class="page admin">
    <div class="head">
      <h1>
        Gestió del catàleg
        <span v-if="status === 'loaded'" class="count mono">· {{ formatCount(movies.length) }}</span>
      </h1>
      <div class="tools">
        <label class="search">
          <span class="visually-hidden">Cerca per títol o direcció</span>
          <input v-model="search" class="input" type="search" placeholder="Cerca per títol o direcció…" />
        </label>
        <RouterLink to="/admin/pelicules/nova" class="btn btn-primary">+ Afegeix pel·lícula</RouterLink>
      </div>
    </div>

    <p v-if="error && status === 'loaded'" class="error" role="alert">{{ error }}</p>

    <div v-if="status === 'loading'" class="skeleton table-skel" aria-busy="true"></div>

    <div v-else-if="status === 'error'" class="empty-state" role="alert">
      <h3>No s'ha pogut carregar el catàleg</h3>
      <p>{{ error }}</p>
      <button class="btn btn-primary" type="button" @click="load">Torna-ho a provar</button>
    </div>

    <div v-else class="table-wrap">
      <table class="table">
        <thead>
          <tr class="mono">
            <th scope="col"><span class="visually-hidden">Pòster</span></th>
            <th scope="col">Títol</th>
            <th scope="col">Any</th>
            <th scope="col">Direcció</th>
            <th scope="col">Gènere</th>
            <th scope="col">Nota</th>
            <th scope="col">Estat</th>
            <th scope="col"><span class="visually-hidden">Accions</span></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in filtered" :key="m.id">
            <td>
              <img v-if="m.poster_url" class="thumb" :src="m.poster_url" alt="" loading="lazy" />
              <span v-else class="thumb empty" title="Sense pòster"></span>
            </td>
            <td>
              <RouterLink :to="`/pelicula/${m.id}`" class="title">{{ m.title }}</RouterLink>
              <span class="votes mono">{{ formatCount(m.rating_count) }} vots</span>
            </td>
            <td class="mono muted">{{ m.year }}</td>
            <td class="muted">{{ m.director }}</td>
            <td class="muted">{{ m.genres.join(' · ') }}</td>
            <td class="mono score">{{ formatScore(m.avg_score) }}</td>
            <td>
              <span class="status" :class="m.status">{{ m.status === 'published' ? 'Publicada' : 'Esborrany' }}</span>
            </td>
            <td class="actions">
              <RouterLink :to="`/admin/pelicules/${m.id}`" class="btn btn-ghost small">Edita</RouterLink>
              <button
                type="button"
                class="btn btn-ghost small danger"
                :disabled="deleting === m.id"
                :aria-label="`Esborra ${m.title}`"
                @click="remove(m)"
              >
                Esborra
              </button>
            </td>
          </tr>
          <tr v-if="filtered.length === 0">
            <td colspan="8" class="none">
              {{ movies.length === 0 ? 'Encara no hi ha cap pel·lícula.' : 'Cap pel·lícula coincideix amb la cerca.' }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.admin {
  padding-top: 26px;
  padding-bottom: 60px;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 20px;
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
}
.tools {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.search {
  width: 280px;
  max-width: 100%;
}
.search .input {
  background: var(--surface);
}
.error {
  margin: 0 0 16px;
  padding: 10px 14px;
  border: 1px solid var(--error);
  border-radius: var(--r-control);
  background: var(--error-bg);
  color: var(--error-text);
  font-size: 13px;
}
.table-skel {
  height: 360px;
  border-radius: var(--r-panel);
}

/* A pantalles estretes la taula llisca DINS del seu contenidor: la pàgina
   no fa scroll horitzontal. */
.table-wrap {
  /* position: relative fa que els elements amb posició absoluta de dins (les
     etiquetes .visually-hidden) quedin retallats pel contenidor i no
     eixamplin la pàgina. */
  position: relative;
  overflow-x: auto;
  border: 1px solid var(--border);
  border-radius: var(--r-panel);
  background: var(--surface);
}
.table {
  width: 100%;
  min-width: 860px;
  border-collapse: collapse;
}
th {
  padding: 12px 14px;
  text-align: left;
  font-size: 10.5px;
  font-weight: 500;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-faint);
  border-bottom: 1px solid var(--border);
}
td {
  padding: 10px 14px;
  border-bottom: 1px solid var(--border-faint);
  vertical-align: middle;
  font-size: 13.5px;
}
tbody tr:last-child td {
  border-bottom: none;
}
tbody tr:hover td {
  background: rgba(255, 255, 255, 0.015);
}
.thumb {
  display: block;
  width: 34px;
  height: 51px;
  border-radius: 4px;
  object-fit: cover;
  background: var(--no-poster);
}
.thumb.empty {
  border: 1px dashed rgba(255, 255, 255, 0.22);
}
.title {
  display: block;
  font-weight: 600;
}
.title:hover {
  color: var(--accent);
}
.votes {
  font-size: 11.5px;
  color: var(--text-faint);
}
.muted {
  color: var(--text-muted);
}
.score {
  color: var(--accent);
  font-weight: 700;
}
.status {
  display: inline-block;
  padding: 3px 10px;
  border-radius: var(--r-pill);
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}
.status.published {
  background: var(--accent-15);
  color: var(--accent);
}
.status.draft {
  background: var(--raised);
  color: var(--text-muted);
}
.actions {
  text-align: right;
  white-space: nowrap;
}
.small {
  padding: 6px 12px;
  font-size: 12.5px;
}
.actions .btn + .btn {
  margin-left: 6px;
}
.danger:not(:disabled):hover {
  border-color: var(--error);
  color: var(--error-text);
}
.none {
  padding: 40px;
  text-align: center;
  color: var(--text-muted);
}
@media (max-width: 560px) {
  .search {
    width: 100%;
  }
  .tools {
    width: 100%;
  }
}
</style>
