<script setup>
// Formulari de valoració, amb tres situacions:
//  - visitant sense sessió      → valoració anònima (nom lliure opcional);
//  - amb sessió                 → tria "Anònim" o "Com a @àlies";
//  - ja té valoració registrada → mode edició (PUT) d'aquella valoració.
import { computed, ref, watch } from 'vue'
import RatingSelector from './RatingSelector.vue'
import { createRating, updateRating } from '../api/ratings.js'
import { useAuthStore } from '../stores/auth.js'

const props = defineProps({
  movieId: { type: String, required: true },
  // La valoració registrada de l'usuari a aquesta pel·lícula, si en té.
  ownRating: { type: Object, default: null },
})
const emit = defineEmits(['saved'])

const auth = useAuthStore()

const score = ref(null)
const comment = ref('')
const authorLabel = ref('')
// 'anon' | 'registered'. Amb sessió, per defecte es publica amb l'àlies.
const mode = ref(auth.isLoggedIn ? 'registered' : 'anon')
const sending = ref(false)
const error = ref(null)

const editing = computed(() => props.ownRating !== null && mode.value === 'registered')

// En entrar en mode edició, el formulari parteix de la valoració actual.
watch(
  () => [props.ownRating, mode.value],
  () => {
    if (editing.value) {
      score.value = props.ownRating.score
      comment.value = props.ownRating.comment ?? ''
    }
  },
  { immediate: true },
)
// Si la sessió s'obre o es tanca amb la fitxa oberta, ajustem el mode.
watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    mode.value = loggedIn ? 'registered' : 'anon'
  },
)

async function submit() {
  // Validació al client: evita una petició que sabem que fallarà. L'API
  // valida igualment (el client no és de fiar: qualsevol pot fer un curl).
  if (!score.value) {
    error.value = 'Tria una nota de l’1 al 10.'
    return
  }
  sending.value = true
  error.value = null
  try {
    if (editing.value) {
      await updateRating(props.ownRating.id, { score: score.value, comment: comment.value })
    } else {
      await createRating(props.movieId, {
        score: score.value,
        comment: comment.value,
        authorLabel: authorLabel.value,
        anonymous: mode.value === 'anon',
      })
      score.value = null
      comment.value = ''
      authorLabel.value = ''
    }
    emit('saved')
  } catch (err) {
    // Missatge de l'API tal qual: "massa peticions seguides...", "ja has
    // valorat aquesta pel·lícula...", etc.
    error.value = err.message
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <form class="form" novalidate @submit.prevent="submit">
    <h3 class="form-title">{{ editing ? 'La teva valoració' : 'Deixa la teva valoració' }}</h3>

    <RatingSelector v-model="score" />

    <label class="visually-hidden" for="rating-comment">Ressenya</label>
    <textarea
      id="rating-comment"
      v-model="comment"
      class="input"
      rows="3"
      maxlength="2000"
      placeholder="Escriu la teva ressenya (opcional)…"
    ></textarea>

    <div class="row">
      <div class="segmented" role="radiogroup" aria-label="Com vols publicar-la">
        <button type="button" role="radio" class="seg" :class="{ active: mode === 'anon' }" :aria-checked="mode === 'anon'" @click="mode = 'anon'">
          Anònim
        </button>
        <button
          v-if="auth.isLoggedIn"
          type="button"
          role="radio"
          class="seg"
          :class="{ active: mode === 'registered' }"
          :aria-checked="mode === 'registered'"
          @click="mode = 'registered'"
        >
          Com a @{{ auth.user.alias }}
        </button>
        <RouterLink v-else class="seg" :to="{ name: 'login', query: { redirect: $route.fullPath } }">Com a @àlies</RouterLink>
      </div>
      <template v-if="mode === 'anon'">
        <label class="visually-hidden" for="rating-author">Nom (opcional)</label>
        <input
          id="rating-author"
          v-model="authorLabel"
          class="input name"
          maxlength="40"
          placeholder="El teu nom (opcional)"
          autocomplete="nickname"
        />
      </template>
    </div>

    <p v-if="error" class="error" role="alert">{{ error }}</p>

    <div class="footer">
      <p class="note">
        <template v-if="editing">Pots canviar la nota i el text sempre que vulguis.</template>
        <template v-else-if="mode === 'registered'">Es publicarà amb el teu àlies i la podràs editar.</template>
        <template v-else>No cal compte — pots valorar de manera anònima.</template>
      </p>
      <button class="btn btn-primary" type="submit" :disabled="sending">
        {{ sending ? 'Enviant…' : editing ? 'Desa els canvis' : 'Envia la valoració' }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.form {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 22px;
  background: var(--surface);
  border: 1px solid var(--border-faint);
  border-radius: var(--r-panel);
}
.form-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}
.row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.segmented {
  display: inline-flex;
  padding: 3px;
  gap: 2px;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 10px;
  max-width: 100%;
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
  transition: all 0.12s;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.seg:hover {
  color: var(--text);
}
.seg.active {
  background: var(--accent-15);
  color: var(--accent);
}
.name {
  flex: 1;
  min-width: 180px;
}
.error {
  margin: 0;
  padding: 10px 14px;
  border: 1px solid var(--error);
  border-radius: var(--r-control);
  background: var(--error-bg);
  color: var(--error-text);
  font-size: 13px;
}
.footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.note {
  margin: 0;
  font-size: 12.5px;
  color: var(--text-dim);
}
</style>
