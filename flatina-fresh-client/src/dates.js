/** Local-date helpers. Dates travel as 'YYYY-MM-DD' strings. */

export function toIso(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function fromIso(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function addDays(iso, days) {
  const d = fromIso(iso)
  d.setDate(d.getDate() + days)
  return toIso(d)
}

/** Monday of the week containing the given date. */
export function weekStart(iso = toIso(new Date())) {
  const d = fromIso(iso)
  const offset = (d.getDay() + 6) % 7
  d.setDate(d.getDate() - offset)
  return toIso(d)
}

export function weekDays(startIso) {
  return Array.from({ length: 7 }, (_, i) => addDays(startIso, i))
}

export function formatDay(iso, opts = { weekday: 'short', month: 'short', day: 'numeric' }) {
  return fromIso(iso).toLocaleDateString(undefined, opts)
}

export function formatRange(startIso, endIso) {
  return `${formatDay(startIso, { month: 'short', day: 'numeric' })} – ${formatDay(endIso, { month: 'short', day: 'numeric', year: 'numeric' })}`
}

/** "This week", "Next week", "Last week", or "Week of Dec 7". */
export function weekLabel(startIso) {
  const diff = Math.round((fromIso(startIso) - fromIso(weekStart())) / (7 * 86400000))
  if (diff === 0) return 'This week'
  if (diff === 1) return 'Next week'
  if (diff === -1) return 'Last week'
  return `Week of ${formatDay(startIso, { month: 'short', day: 'numeric' })}`
}

/** Past weeks are read-only; this week and later can be changed by anyone in the family. */
export function isPastWeek(startIso) {
  return weekStart(startIso) < weekStart()
}

export function isIsoDate(value) {
  return typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)
}
