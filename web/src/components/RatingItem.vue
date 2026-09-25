<script setup>
// Una valoració de la llista:
//  - registrada → "@àlies" (mai l'email: l'API ni tan sols l'envia);
//  - anònima    → el nom lliure author_label, si n'hi ha, o "Anònim".
import { computed } from 'vue'
import { formatDate } from '../utils/format.js'

const props = defineProps({
  rating: { type: Object, required: true },
  own: { type: Boolean, default: false }, // és de l'usuari amb sessió
})
defineEmits(['edit'])

const registered = computed(() => Boolean(props.rating.user_alias))
const name = computed(() =>
  registered.value ? `@${props.rating.user_alias}` : props.rating.author_label || 'Anònim',
)
const anonymous = computed(() => !registered.value && !props.rating.author_label)
const initial = computed(() => (anonymous.value ? 'A' : name.value.replace('@', '').charAt(0).toUpperCase()))
</script>

<template>
  <article class="item">
    <span class="avatar" :class="{ anon: anonymous, guest: !registered && !anonymous }" aria-hidden="true">
      {{ initial }}
    </span>
    <div class="body">
      <header class="meta">
        <span class="name" :class="{ anon: !registered }">{{ name }}</span>
        <span v-if="own" class="own">Teva</span>
        <span class="score mono">{{ rating.score }}</span>
        <time class="date mono" :datetime="rating.created_at">{{ formatDate(rating.created_at) }}</time>
      </header>
      <p v-if="rating.comment" class="text">{{ rating.comment }}</p>
      <button v-if="own" type="button" class="edit" @click="$emit('edit')">Edita la teva valoració</button>
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
.avatar.anon,
.avatar.guest {
  background: var(--raised);
  color: var(--text-dim);
}
.own {
  padding: 1px 7px;
  border-radius: var(--r-pill);
  border: 1px solid var(--accent-30);
  color: var(--accent);
  font-size: 11px;
  font-weight: 600;
}
.edit {
  margin-top: 8px;
  padding: 0;
  border: none;
  background: none;
  color: var(--accent);
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
}
.edit:hover {
  text-decoration: underline;
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
