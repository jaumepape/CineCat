<script setup>
// Targeta de pel·lícula (docs/design: MovieCard). Idèntica a tot arreu:
// pòster 2:3 + xip de nota + títol + any, amb estat "sense pòster".
import { formatScore } from '../utils/format.js'

defineProps({
  movie: { type: Object, required: true },
})
</script>

<template>
  <RouterLink :to="`/pelicula/${movie.id}`" class="card">
    <div class="poster" :class="{ empty: !movie.poster_url }">
      <template v-if="movie.poster_url">
        <!-- loading="lazy": el navegador només baixa els pòsters que es veuen. -->
        <img :src="movie.poster_url" :alt="`Pòster de ${movie.title}`" loading="lazy" />
        <div class="overlay" aria-hidden="true"></div>
      </template>
      <div v-else class="placeholder" aria-hidden="true">
        <span class="placeholder-box"></span>
        <span class="placeholder-text">SENSE PÒSTER</span>
      </div>
      <span class="score-chip" :aria-label="`Nota mitjana ${formatScore(movie.avg_score)}`">
        {{ formatScore(movie.avg_score) }}
      </span>
    </div>
    <h3 class="title">{{ movie.title }}</h3>
    <p class="year mono">{{ movie.year }}</p>
  </RouterLink>
</template>

<style scoped>
.card {
  display: block;
  min-width: 0;
}
.poster {
  position: relative;
  aspect-ratio: 2 / 3;
  border-radius: var(--r-card);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.07);
  box-shadow: var(--shadow-card);
  background: var(--no-poster);
  transition: transform 0.15s ease;
}
.card:hover .poster {
  transform: translateY(-3px);
}
.poster img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(155deg, rgba(255, 255, 255, 0.12), transparent 42%),
    linear-gradient(0deg, rgba(0, 0, 0, 0.45), transparent 40%);
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
  width: 42px;
  height: 42px;
  border: 1px dashed rgba(255, 255, 255, 0.22);
  border-radius: 6px;
}
.placeholder-text {
  font-family: var(--font-mono);
  font-size: 8.5px;
  letter-spacing: 0.14em;
  color: var(--text-faint);
}
.score-chip {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 3px 8px;
  border-radius: var(--r-pill);
  background: rgba(8, 9, 11, 0.82);
  border: 1px solid rgba(45, 212, 191, 0.32);
  color: var(--accent);
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 700;
}
.title {
  margin: 10px 0 2px;
  font-size: 13.5px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.year {
  margin: 0;
  font-size: 11.5px;
  color: var(--text-muted);
}
</style>
