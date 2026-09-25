<script setup>
// Vista 05 · Admin — Crear / Editar pel·lícula (amb la variant 05b d'error
// de pujada, al component PosterPicker).
//
// Desar són DUES peticions (§4, cicle de vida A):
//   1. POST /api/movies (o PUT si editem) amb les metadades en JSON → id
//   2. si hi ha un pòster nou, POST /api/movies/{id}/poster (multipart)
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PosterPicker from '../components/PosterPicker.vue'
import { createMovie, getMovie, updateMovie, uploadPoster } from '../api/movies.js'
import { useCatalogStore } from '../stores/catalog.js'
import { GENRES } from '../utils/genres.js'

const props = defineProps({
  id: { type: String, default: null }, // sense id → pel·lícula nova
})

const router = useRouter()
const catalog = useCatalogStore()

const isNew = computed(() => !props.id)
const form = reactive({
  title: '',
  year: new Date().getFullYear(),
  duration_min: null,
  director: '',
  synopsis: '',
  genres: [],
  status: 'draft',
})
const currentPosterUrl = ref(null)
const posterFile = ref(null)
const posterInvalid = ref(false)
const posterServerError = ref(null)

const status = ref(isNew.value ? 'loaded' : 'loading') // 'loading' | 'loaded' | 'notfound' | 'error'
const saving = ref(false)
const error = ref(null)

async function load() {
  try {
    const m = await getMovie(props.id)
    Object.assign(form, {
      title: m.title,
      year: m.year,
      duration_min: m.duration_min,
      director: m.director,
      synopsis: m.synopsis,
      genres: [...m.genres],
      status: m.status,
    })
    currentPosterUrl.value = m.poster_url
    status.value = 'loaded'
  } catch (err) {
    status.value = err.status === 404 ? 'notfound' : 'error'
    error.value = err.message
  }
}
if (!isNew.value) load()

// Un fitxer nou substitueix l'error que l'API hagués donat per l'anterior.
watch(posterFile, () => (posterServerError.value = null))

// --- Gèneres: xips amb "×" + desplegable amb els que falten ---
const availableGenres = computed(() => GENRES.filter((g) => !form.genres.includes(g)))
const genreToAdd = ref('')
watch(genreToAdd, (g) => {
  if (g) form.genres.push(g)
  genreToAdd.value = ''
})
function removeGenre(g) {
  form.genres = form.genres.filter((x) => x !== g)
}

async function save() {
  saving.value = true
  error.value = null
  posterServerError.value = null
  let movie
  try {
    const data = { ...form, year: Number(form.year), duration_min: Number(form.duration_min) }
    movie = isNew.value ? await createMovie(data) : await updateMovie(props.id, data)
  } catch (err) {
    // Validació de l'API (400): el missatge diu quin camp falla.
    error.value = err.message
    saving.value = false
    return
  }

  if (posterFile.value) {
    try {
      await uploadPoster(movie.id, posterFile.value)
    } catch (err) {
      // Les metadades JA s'han desat; només ha fallat el pòster. Si era
      // nova, passem a la ruta d'edició perquè un segon "Desa" no la torni
      // a crear.
      posterServerError.value = err.message
      saving.value = false
      catalog.invalidate()
      if (isNew.value) router.replace(`/admin/pelicules/${movie.id}`)
      return
    }
  }

  catalog.invalidate()
  saving.value = false
  router.push('/admin')
}
</script>

<template>
  <div class="page form-page">
    <nav class="breadcrumb" aria-label="Ruta">
      <RouterLink to="/admin">Gestió del catàleg</RouterLink>
      <span aria-hidden="true">/</span>
      <span aria-current="page">{{ isNew ? 'Nova pel·lícula' : form.title || 'Edita' }}</span>
    </nav>

    <div v-if="status === 'loading'" class="skeleton" style="height: 420px; border-radius: 12px" aria-busy="true"></div>

    <div v-else-if="status === 'notfound' || status === 'error'" class="empty-state" role="alert">
      <h3>{{ status === 'notfound' ? 'Aquesta pel·lícula no existeix' : "No s'ha pogut carregar" }}</h3>
      <p>{{ error }}</p>
      <RouterLink to="/admin" class="btn btn-primary">Torna al llistat</RouterLink>
    </div>

    <form v-else novalidate @submit.prevent="save">
      <div class="head">
        <h1>{{ isNew ? 'Nova pel·lícula' : 'Edita la pel·lícula' }}</h1>
        <div class="head-actions">
          <RouterLink to="/admin" class="btn btn-ghost">Cancel·la</RouterLink>
          <button class="btn btn-primary" type="submit" :disabled="saving || posterInvalid">
            {{ saving ? 'Desant…' : isNew ? 'Crea la pel·lícula' : 'Desa els canvis' }}
          </button>
        </div>
      </div>

      <p v-if="error" class="error" role="alert">{{ error }}</p>

      <div class="grid">
        <div class="fields">
          <label class="field">
            <span class="field-label">Títol</span>
            <input v-model="form.title" class="input" required />
          </label>
          <div class="pair">
            <label class="field">
              <span class="field-label">Any</span>
              <input v-model="form.year" class="input mono" type="number" min="1888" max="2100" required />
            </label>
            <label class="field">
              <span class="field-label">Durada (min)</span>
              <input v-model="form.duration_min" class="input mono" type="number" min="1" required />
            </label>
          </div>
          <label class="field">
            <span class="field-label">Direcció</span>
            <input v-model="form.director" class="input" required />
          </label>

          <div class="field">
            <span class="field-label" id="genres-label">Gèneres</span>
            <div class="genres" role="group" aria-labelledby="genres-label">
              <span v-for="g in form.genres" :key="g" class="chip active genre">
                {{ g }}
                <button type="button" :aria-label="`Treu ${g}`" @click="removeGenre(g)">×</button>
              </span>
              <select v-if="availableGenres.length" v-model="genreToAdd" class="add-genre" aria-label="Afegeix gènere">
                <option value="">+ Afegeix gènere</option>
                <option v-for="g in availableGenres" :key="g" :value="g">{{ g }}</option>
              </select>
            </div>
          </div>

          <label class="field">
            <span class="field-label">Sinopsi</span>
            <textarea v-model="form.synopsis" class="input" rows="5"></textarea>
          </label>

          <div class="field">
            <span class="field-label" id="status-label">Estat</span>
            <div class="segmented" role="radiogroup" aria-labelledby="status-label">
              <button
                type="button"
                role="radio"
                class="seg"
                :class="{ active: form.status === 'published' }"
                :aria-checked="form.status === 'published'"
                @click="form.status = 'published'"
              >
                Publicada
              </button>
              <button
                type="button"
                role="radio"
                class="seg"
                :class="{ active: form.status === 'draft' }"
                :aria-checked="form.status === 'draft'"
                @click="form.status = 'draft'"
              >
                Esborrany
              </button>
            </div>
            <span class="hint">Un esborrany només el veuen els admins.</span>
          </div>
        </div>

        <PosterPicker
          v-model:file="posterFile"
          v-model:invalid="posterInvalid"
          :current-url="currentPosterUrl"
          :title="form.title"
          :server-error="posterServerError"
        />
      </div>
    </form>
  </div>
</template>

<style scoped>
.form-page {
  padding-top: 20px;
  padding-bottom: 60px;
}
.breadcrumb {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12.5px;
  color: var(--text-faint);
  margin-bottom: 18px;
}
.breadcrumb a {
  color: var(--text-muted);
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 22px;
}
h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.head-actions {
  display: flex;
  gap: 10px;
}
.error {
  margin: 0 0 18px;
  padding: 10px 14px;
  border: 1px solid var(--error);
  border-radius: var(--r-control);
  background: var(--error-bg);
  color: var(--error-text);
  font-size: 13px;
}
.grid {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 28px;
  align-items: start;
}
.fields {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.field {
  display: block;
}
.pair {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.genres {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.genre {
  gap: 6px;
  padding-right: 6px;
}
.genre button {
  border: none;
  background: none;
  color: inherit;
  font-size: 15px;
  line-height: 1;
  padding: 0 4px;
  cursor: pointer;
}
.add-genre {
  padding: 6px 12px;
  border-radius: var(--r-pill);
  border: 1px dashed rgba(255, 255, 255, 0.2);
  background: transparent;
  color: var(--text-muted);
  font-size: 12.5px;
  cursor: pointer;
}
.add-genre option {
  background: var(--surface);
  color: var(--text);
}
.segmented {
  display: inline-flex;
  padding: 3px;
  gap: 2px;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 10px;
}
.seg {
  padding: 8px 14px;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: var(--text-dim);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}
.seg.active {
  background: var(--accent-15);
  color: var(--accent);
}
.hint {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-faint);
}
@media (max-width: 820px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
