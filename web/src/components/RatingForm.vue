<script setup>
// Formulari de valoració. De moment NOMÉS anònim: l'opció "Com a @àlies"
// del disseny es mostra però desactivada fins que hi hagi sessions (Fase 4).
import { ref } from 'vue'
import RatingSelector from './RatingSelector.vue'
import { createRating } from '../api/ratings.js'

const props = defineProps({
  movieId: { type: String, required: true },
})
const emit = defineEmits(['created'])

const score = ref(null)
const comment = ref('')
const authorLabel = ref('')
const sending = ref(false)
const error = ref(null)

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
    const rating = await createRating(props.movieId, {
      score: score.value,
      comment: comment.value,
      authorLabel: authorLabel.value,
    })
    score.value = null
    comment.value = ''
    authorLabel.value = ''
    emit('created', rating)
  } catch (err) {
    // Missatge de l'API tal qual: "massa valoracions seguides...", etc.
    error.value = err.message
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <form class="form" novalidate @submit.prevent="submit">
    <h3 class="form-title">Deixa la teva valoració</h3>

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
      <div class="segmented" role="group" aria-label="Com vols publicar-la">
        <button type="button" class="seg active" aria-pressed="true">Anònim</button>
        <button type="button" class="seg" disabled title="Cal iniciar sessió (Fase 4)">Com a @àlies</button>
      </div>
      <label class="visually-hidden" for="rating-author">Nom (opcional)</label>
      <input
        id="rating-author"
        v-model="authorLabel"
        class="input name"
        maxlength="40"
        placeholder="El teu nom (opcional)"
        autocomplete="nickname"
      />
    </div>

    <p v-if="error" class="error" role="alert">{{ error }}</p>

    <div class="footer">
      <p class="note">No cal compte — pots valorar de manera anònima.</p>
      <button class="btn btn-primary" type="submit" :disabled="sending">
        {{ sending ? 'Enviant…' : 'Envia la valoració' }}
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
}
.seg {
  padding: 8px 14px;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: var(--text-dim);
  font-size: 13px;
  font-weight: 500;
  transition: all 0.12s;
}
.seg:disabled {
  cursor: not-allowed;
  opacity: 0.6;
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
