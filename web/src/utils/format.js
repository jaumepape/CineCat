// Format de números i dates a la catalana ("7,8", "1.243", "1h 52min").

const scoreFormat = new Intl.NumberFormat('ca-ES', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
})
// useGrouping 'always': en català, per defecte "1243" no porta punt de milers
// (només a partir de 5 xifres); el disseny mostra "1.243".
const countFormat = new Intl.NumberFormat('ca-ES', { useGrouping: 'always' })
const dateFormat = new Intl.DateTimeFormat('ca-ES', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})

/** 7.8 → "7,8"; null (sense valoracions) → "—". */
export function formatScore(score) {
  return score === null || score === undefined ? '—' : scoreFormat.format(score)
}

export function formatCount(n) {
  return countFormat.format(n)
}

/** 1 → "1 valoració"; 1243 → "1.243 valoracions". */
export function formatRatingCount(n) {
  return `${formatCount(n)} ${n === 1 ? 'valoració' : 'valoracions'}`
}

/** 112 → "1h 52min"; 45 → "45min". */
export function formatDuration(minutes) {
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  if (!h) return `${m}min`
  return m ? `${h}h ${m}min` : `${h}h`
}

/** ISO → "25 de set. 2026". */
export function formatDate(iso) {
  return dateFormat.format(new Date(iso))
}

/** Etiquetes del RatingSelector (docs/design/README.md). */
export const SCORE_LABELS = [
  '',
  'Horrible',
  'Molt dolenta',
  'Dolenta',
  'Fluixa',
  'Regular',
  'Acceptable',
  'Bona',
  'Molt bona',
  'Excel·lent',
  'Obra mestra',
]
