const FRACTIONS = [
  [0.125, '⅛'], [0.25, '¼'], [0.333, '⅓'], [0.375, '⅜'], [0.5, '½'],
  [0.625, '⅝'], [0.667, '⅔'], [0.75, '¾'], [0.875, '⅞'],
]

/** 1.5 -> "1½", 0.25 -> "¼", 6.75 -> "6¾", 2.4 -> "2.4" */
export function formatQuantity(q) {
  if (q === null || q === undefined || q === '') return ''
  const n = Number(q)
  if (!Number.isFinite(n)) return String(q)
  const whole = Math.floor(n)
  const frac = n - whole
  if (frac < 0.01) return String(whole)
  const match = FRACTIONS.find(([v]) => Math.abs(v - frac) < 0.02)
  if (match) return `${whole > 0 ? whole : ''}${match[1]}`
  return String(Math.round(n * 100) / 100)
}

const PLURAL_UNITS = { clove: 'cloves', thumb: 'thumbs', can: 'cans', slice: 'slices', cup: 'cups', package: 'packages', bunch: 'bunches', piece: 'pieces' }

export function formatAmount(quantity, unit) {
  const q = formatQuantity(quantity)
  if (!q) return unit || ''
  if (!unit) return q
  const u = Number(quantity) > 1 && PLURAL_UNITS[unit] ? PLURAL_UNITS[unit] : unit
  return `${q} ${u}`
}
