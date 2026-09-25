<script setup>
// Una valoració de la llista. Sense user_id → "Anònim" (o el nom lliure
// author_label, si n'ha posat). A la Fase 4 hi apareixerà l'àlies.
import { computed } from 'vue'
import { formatDate } from '../utils/format.js'

const props = defineProps({
  rating: { type: Object, required: true },
})

const name = computed(() => props.rating.author_label || 'Anònim')
const anonymous = computed(() => !props.rating.author_label)
</script>

<template>
  <article class="item">
    <span class="avatar" :class="{ anon: anonymous }" aria-hidden="true">
      {{ anonymous ? 'A' : name.charAt(0).toUpperCase() }}
    </span>
    <div class="body">
      <header class="meta">
        <span class="name" :class="{ anon: anonymous }">{{ name }}</span>
        <span class="score mono">{{ rating.score }}</span>
        <time class="date mono" :datetime="rating.created_at">{{ formatDate(rating.created_at) }}</time>
      </header>
      <p v-if="rating.comment" class="text">{{ rating.comment }}</p>
    </div>
  </article>
</template>

<style scoped>
.item {
  display: flex;
  gap: 14px;
  padding: 18px 0;
  border-top: 1px solid var(--border-faint);
}
.avatar {
  flex: none;
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--accent-12);
  color: var(--accent);
  font-weight: 600;
}
.avatar.anon {
  background: var(--raised);
  color: var(--text-dim);
}
.body {
  flex: 1;
  min-width: 0;
}
.meta {
  display: flex;
  align-items: center;
  gap: 10px;
}
.name {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.name.anon {
  color: var(--text-muted);
}
.score {
  padding: 1px 8px;
  border-radius: var(--r-pill);
  background: var(--accent-15);
  color: var(--accent);
  font-size: 12px;
  font-weight: 700;
}
.date {
  margin-left: auto;
  flex: none;
  font-size: 11.5px;
  color: var(--text-faint);
}
.text {
  margin: 6px 0 0;
  color: var(--text-2);
  line-height: 1.6;
  /* Respecta els salts de línia i talla paraules molt llargues. */
  white-space: pre-line;
  overflow-wrap: anywhere;
}
</style>
