<script setup>
// Selector de nota 1–10 (docs/design: RatingSelector). v-model = nota fixada.
// En hover es previsualitza la nota sense fixar-la; en clic es fixa.
import { computed, ref } from 'vue'
import { SCORE_LABELS } from '../utils/format.js'

const model = defineModel({ type: Number, default: null })
const hover = ref(null)

// El que es mostra: la nota en hover si n'hi ha, si no la fixada.
const shown = computed(() => hover.value ?? model.value)
</script>

<template>
  <div class="selector">
    <div class="head" aria-live="polite">
      <span class="big mono">{{ shown ?? 0 }}</span>
      <span class="of mono">/ 10</span>
      <span class="label">{{ shown ? SCORE_LABELS[shown] : 'Tria una nota' }}</span>
    </div>
    <div class="segments" role="radiogroup" aria-label="Nota de l'1 al 10" @mouseleave="hover = null">
      <button
        v-for="n in 10"
        :key="n"
        type="button"
        role="radio"
        class="segment mono"
        :class="{ active: shown !== null && n <= shown }"
        :aria-checked="model === n"
        :aria-label="`${n} — ${SCORE_LABELS[n]}`"
        @mouseenter="hover = n"
        @focus="hover = n"
        @blur="hover = null"
        @click="model = n"
      >
        {{ n }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}
.big {
  font-size: 38px;
  font-weight: 700;
  line-height: 1;
  color: var(--accent);
}
.of {
  color: var(--text-faint);
  font-size: 14px;
}
.label {
  margin-left: auto;
  font-size: 13px;
  color: var(--text-muted);
}
.segments {
  display: flex;
  gap: 5px;
}
.segment {
  flex: 1;
  min-width: 0;
  height: 38px;
  border: none;
  border-radius: 8px;
  background: var(--raised);
  color: var(--text-dim);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.12s;
}
.segment:hover {
  transform: translateY(-2px);
}
.segment.active {
  background: var(--accent);
  color: var(--on-accent);
}
@media (max-width: 420px) {
  .segments {
    gap: 3px;
  }
  .segment {
    font-size: 12px;
  }
}
</style>
