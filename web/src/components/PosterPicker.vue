<script setup>
// Selector de pòster del formulari d'admin (docs/design 05 i 05b).
//
// NO puja res: només tria el fitxer, el valida i en mostra una
// previsualització local. La pujada la fa el formulari en desar, perquè
// primer cal que la pel·lícula existeixi (§4: el pòster és un endpoint a part).
import { computed, onBeforeUnmount, ref, watch } from 'vue'

const MAX_MB = 5
const ACCEPTED = ['image/jpeg', 'image/png']

const props = defineProps({
  currentUrl: { type: String, default: null }, // pòster ja desat
  title: { type: String, default: '' },
  serverError: { type: String, default: null }, // 413/415 de l'API
})
// v-model:file → el File triat (o null). v-model:error → hi ha un error?
const file = defineModel('file', { default: null })
const invalid = defineModel('invalid', { type: Boolean, default: false })

const rejected = ref(null) // { name, size, reason }
const dragging = ref(false)
const input = ref(null)

// Previsualització local: URL.createObjectURL crea una URL temporal
// ("blob:...") que apunta al fitxer a la memòria del navegador. Així es veu
// el pòster ABANS de pujar-lo.
const previewUrl = ref(null)
watch(file, (f) => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value) // allibera la memòria
  previewUrl.value = f ? URL.createObjectURL(f) : null
})
onBeforeUnmount(() => previewUrl.value && URL.revokeObjectURL(previewUrl.value))

const shownUrl = computed(() => previewUrl.value ?? props.currentUrl)
const errorMessage = computed(() => rejected.value?.reason ?? props.serverError)
watch(errorMessage, (e) => (invalid.value = Boolean(e)), { immediate: true })

function formatMB(bytes) {
  return `${(bytes / 1024 / 1024).toLocaleString('ca-ES', { maximumFractionDigits: 1 })} MB`
}

// Validació al CLIENT: avisa a l'instant, sense pujar 8 MB per res. És
// comoditat: l'API torna a validar-ho (i pels magic bytes, que és el que
// compta de veritat; file.type el deduceix el navegador de l'extensió).
function choose(f) {
  if (!f) return
  if (!ACCEPTED.includes(f.type)) {
    const ext = f.name.split('.').pop()?.toUpperCase() ?? '?'
    reject(f, `El fitxer és un ${ext} de ${formatMB(f.size)}. Fes servir JPG o PNG de fins a ${MAX_MB} MB.`, 'format no admès')
    return
  }
  if (f.size > MAX_MB * 1024 * 1024) {
    reject(f, `El fitxer fa ${formatMB(f.size)}. El màxim són ${MAX_MB} MB.`, 'massa gran')
    return
  }
  rejected.value = null
  file.value = f
}

function reject(f, reason, short) {
  rejected.value = { name: f.name, size: formatMB(f.size), reason, short }
  file.value = null
}

function clearRejected() {
  rejected.value = null
}

function onDrop(e) {
  dragging.value = false
  choose(e.dataTransfer.files[0])
}

function onChange(e) {
  choose(e.target.files[0])
  e.target.value = '' // permet tornar a triar el mateix fitxer
}
</script>

<template>
  <div class="picker">
    <div class="preview">
      <span class="preview-label mono">PREVISUALITZACIÓ</span>
      <div class="poster" :class="{ empty: !shownUrl }">
        <img v-if="shownUrl" :src="shownUrl" :alt="`Pòster de ${title || 'la pel·lícula'}`" />
        <div v-else class="placeholder" aria-hidden="true">
          <span class="placeholder-box"></span>
          <span class="placeholder-text">SENSE PÒSTER</span>
        </div>
      </div>
      <div class="preview-foot">
        <span class="preview-title">{{ title || 'Sense títol' }}</span>
        <button type="button" class="btn btn-ghost small" @click="input.click()">Canvia</button>
      </div>
      <p v-if="file" class="pending mono">Nou: {{ file.name }} · es pujarà en desar</p>
    </div>

    <label
      class="drop"
      :class="{ dragging, error: errorMessage }"
      @dragover.prevent="dragging = true"
      @dragleave="dragging = false"
      @drop.prevent="onDrop"
    >
      <input ref="input" class="visually-hidden" type="file" accept="image/jpeg,image/png" @change="onChange" />
      <template v-if="errorMessage">
        <span class="icon error-icon" aria-hidden="true">!</span>
        <strong>No s'ha pogut pujar la imatge</strong>
        <span class="msg" role="alert">{{ errorMessage }}</span>
      </template>
      <template v-else>
        <span class="icon" aria-hidden="true">↑</span>
        <span><strong>Arrossega una imatge</strong> o <span class="link">navega</span></span>
        <span class="hint mono">JPG o PNG · proporció 2:3 · màx. {{ MAX_MB }} MB</span>
      </template>
    </label>

    <div v-if="rejected" class="rejected">
      <span class="mono">{{ rejected.name }} · {{ rejected.size }} · {{ rejected.short }}</span>
      <button type="button" aria-label="Descarta el fitxer rebutjat" @click="clearRejected">×</button>
    </div>
  </div>
</template>

<style scoped>
.picker {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.preview {
  padding: 16px;
  border-radius: var(--r-panel);
  background: var(--surface);
  border: 1px solid var(--border-faint);
}
.preview-label {
  display: block;
  margin-bottom: 10px;
  font-size: 10.5px;
  letter-spacing: 0.14em;
  color: var(--text-faint);
}
.poster {
  position: relative;
  width: min(100%, 220px);
  margin: 0 auto;
  aspect-ratio: 2 / 3;
  border-radius: var(--r-card);
  overflow: hidden;
  background: var(--no-poster);
  border: 1px solid rgba(255, 255, 255, 0.07);
  box-shadow: var(--shadow-card);
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
  gap: 10px;
}
.placeholder-box {
  width: 48px;
  height: 48px;
  border: 1px dashed rgba(255, 255, 255, 0.22);
  border-radius: 6px;
}
.placeholder-text {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.14em;
  color: var(--text-faint);
}
.preview-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 12px;
}
.preview-title {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.small {
  padding: 6px 12px;
  font-size: 12.5px;
}
.pending {
  margin: 8px 0 0;
  font-size: 11.5px;
  color: var(--accent);
  overflow-wrap: anywhere;
}
.drop {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 22px 16px;
  text-align: center;
  border: 1px dashed rgba(255, 255, 255, 0.2);
  border-radius: var(--r-panel);
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.12s;
}
.drop:hover,
.drop.dragging {
  border-color: var(--accent);
  background: var(--accent-12);
}
.drop.error {
  border-color: rgba(255, 82, 82, 0.6);
  background: var(--error-bg);
}
.icon {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--raised);
  color: var(--text);
  font-weight: 700;
}
.error-icon {
  background: rgba(255, 82, 82, 0.15);
  color: var(--error);
}
.drop strong {
  color: var(--text);
}
.link {
  color: var(--accent);
  text-decoration: underline;
}
.msg {
  color: var(--error-text);
  font-size: 13px;
}
.hint {
  font-size: 11px;
  color: var(--text-faint);
}
.rejected {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 12px;
  border-radius: var(--r-control);
  border: 1px solid var(--error);
  color: var(--error-text);
  font-size: 12px;
}
.rejected span {
  overflow-wrap: anywhere;
}
.rejected button {
  border: none;
  background: none;
  color: inherit;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
}
</style>
