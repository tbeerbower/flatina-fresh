/** Recipe browsing: proteins, filters and sorting shared by the Recipes page and the recipe picker. */

export const PROTEINS = [
  { value: 'beef', label: 'Beef', icon: '🐄' },
  { value: 'pork', label: 'Pork', icon: '🐖' },
  { value: 'chicken', label: 'Chicken', icon: '🐔' },
  { value: 'turkey', label: 'Turkey', icon: '🦃' },
  { value: 'lamb', label: 'Lamb', icon: '🐑' },
  { value: 'seafood', label: 'Seafood', icon: '🐟' },
  { value: 'vegetarian', label: 'Veggie', icon: '🥕' },
]

export function proteinInfo(value) {
  return PROTEINS.find((p) => p.value === value) || (value ? { value, label: value, icon: '🍽️' } : null)
}

/** HelloFresh calls meals of 650 calories or less "Calorie Smart". */
export const LOW_CALORIE = 650

export const CALORIE_OPTIONS = [
  { value: '', label: 'Any calories' },
  { value: String(LOW_CALORIE), label: `Low calorie (≤ ${LOW_CALORIE})` },
  { value: '800', label: '≤ 800 cal' },
]

export const TIME_OPTIONS = [
  { value: '', label: 'Any time' },
  { value: '30', label: '≤ 30 min' },
  { value: '45', label: '≤ 45 min' },
  { value: '60', label: '≤ 60 min' },
]

const nullsLast = (a, b, cmp) => (a == null ? (b == null ? 0 : 1) : b == null ? -1 : cmp(a, b))
const byTitle = (a, b) => a.title.localeCompare(b.title)

export const SORTS = [
  { value: 'title', label: 'Name A–Z', compare: byTitle },
  { value: 'quick', label: 'Quickest first', compare: (a, b) => nullsLast(a.totalMinutes, b.totalMinutes, (x, y) => x - y) || byTitle(a, b) },
  { value: 'light', label: 'Fewest calories', compare: (a, b) => nullsLast(a.calories, b.calories, (x, y) => x - y) || byTitle(a, b) },
  { value: 'new', label: 'Newest added', compare: (a, b) => (b.createdAt || '').localeCompare(a.createdAt || '') || byTitle(a, b) },
  // Never-planned recipes count as the least recent
  { value: 'stale', label: 'Not made lately', compare: (a, b) => (a.lastPlanned || '').localeCompare(b.lastPlanned || '') || byTitle(a, b) },
  { value: 'popular', label: 'Most planned', compare: (a, b) => (b.timesPlanned || 0) - (a.timesPlanned || 0) || byTitle(a, b) },
]

export function emptyFilters() {
  return { q: '', proteins: [], tags: [], withIng: '', withoutIng: '', maxCal: '', maxTime: '', sort: 'title' }
}

export function splitTags(tags) {
  return (tags || '').split(',').map((t) => t.trim().toLowerCase()).filter(Boolean)
}

/** Tags other than proteins (pasta, tacos, rice bowl, …), most common first. */
export function styleTags(recipes) {
  const counts = new Map()
  const proteinWords = new Set(PROTEINS.map((p) => p.value).concat(['veggie', 'vegan']))
  for (const r of recipes) {
    for (const t of new Set(splitTags(r.tags))) {
      if (!proteinWords.has(t)) counts.set(t, (counts.get(t) || 0) + 1)
    }
  }
  return [...counts.entries()].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0])).map(([t]) => t)
}

export function allIngredients(recipes) {
  const names = new Map()
  for (const r of recipes) for (const i of r.ingredients || []) names.set(i.toLowerCase(), i)
  return [...names.values()].sort((a, b) => a.localeCompare(b))
}

const has = (text, needle) => (text || '').toLowerCase().includes(needle)

function matchesIngredient(r, needle) {
  return (r.ingredients || []).some((i) => has(i, needle))
}

export function filterRecipes(recipes, f) {
  const q = f.q.trim().toLowerCase()
  const withIng = f.withIng.trim().toLowerCase()
  const withoutIng = f.withoutIng.trim().toLowerCase()
  const maxCal = f.maxCal ? Number(f.maxCal) : null
  const maxTime = f.maxTime ? Number(f.maxTime) : null
  const sort = (SORTS.find((s) => s.value === f.sort) || SORTS[0]).compare
  return recipes
    .filter((r) => {
      if (f.proteins.length && !f.proteins.includes(r.protein)) return false
      if (f.tags.length) {
        const tags = splitTags(r.tags)
        if (!f.tags.some((t) => tags.includes(t))) return false
      }
      if (maxCal != null && (r.calories == null || r.calories > maxCal)) return false
      if (maxTime != null && (r.totalMinutes == null || r.totalMinutes > maxTime)) return false
      if (withIng && !matchesIngredient(r, withIng)) return false
      if (withoutIng && matchesIngredient(r, withoutIng)) return false
      if (q && !(has(r.title, q) || has(r.subtitle, q) || has(r.description, q) || has(r.tags, q) || matchesIngredient(r, q))) return false
      return true
    })
    .sort(sort)
}

export function activeFilterCount(f) {
  return f.proteins.length + f.tags.length + [f.q, f.withIng, f.withoutIng, f.maxCal, f.maxTime].filter((v) => v && v.trim()).length
}

/** Filters <-> route query, so Back from a recipe returns to the same filtered list. */
export function filtersFromQuery(query) {
  const f = emptyFilters()
  const list = (v) => (v ? String(v).split(',').filter(Boolean) : [])
  f.q = query.q || ''
  f.proteins = list(query.protein)
  f.tags = list(query.tag)
  f.withIng = query.with || ''
  f.withoutIng = query.without || ''
  f.maxCal = query.cal || ''
  f.maxTime = query.time || ''
  f.sort = query.sort || 'title'
  return f
}

export function filtersToQuery(f) {
  const q = {
    q: f.q || undefined,
    protein: f.proteins.join(',') || undefined,
    tag: f.tags.join(',') || undefined,
    with: f.withIng || undefined,
    without: f.withoutIng || undefined,
    cal: f.maxCal || undefined,
    time: f.maxTime || undefined,
    sort: f.sort !== 'title' ? f.sort : undefined,
  }
  return Object.fromEntries(Object.entries(q).filter(([, v]) => v !== undefined))
}
