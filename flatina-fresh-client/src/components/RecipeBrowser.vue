<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CALORIE_OPTIONS, PROTEINS, SORTS, TIME_OPTIONS, activeFilterCount, allIngredients, emptyFilters,
  filterRecipes, filtersFromQuery, filtersToQuery, styleTags,
} from '../recipes'

/**
 * Search, filter and sort a list of recipe summaries. Filters live in the URL so Back returns to the same view.
 * Renders each matching recipe through the default slot ({ recipe }).
 */
const props = defineProps({ recipes: { type: Array, required: true } })

const route = useRoute()
const router = useRouter()
const filters = ref(filtersFromQuery(route.query))
const showMore = ref(Boolean(filters.value.withIng || filters.value.withoutIng))
// Phones fold the chips and dropdowns away behind a Filters button
const filtersOpen = ref(false)

const proteins = computed(() => {
  const present = new Set(props.recipes.map((r) => r.protein))
  return PROTEINS.filter((p) => present.has(p.value))
})
const tags = computed(() => styleTags(props.recipes).slice(0, 14))
const ingredients = computed(() => allIngredients(props.recipes))
const results = computed(() => filterRecipes(props.recipes, filters.value))
const activeCount = computed(() => activeFilterCount(filters.value))

function toggle(list, value) {
  const i = list.indexOf(value)
  if (i >= 0) list.splice(i, 1)
  else list.push(value)
}

function clear() {
  filters.value = { ...emptyFilters(), sort: filters.value.sort }
}

watch(
  filters,
  (f) => {
    const keep = Object.fromEntries(Object.entries(route.query).filter(([k]) => !['q', 'protein', 'tag', 'with', 'without', 'cal', 'time', 'sort'].includes(k)))
    router.replace({ query: { ...keep, ...filtersToQuery(f) } })
  },
  { deep: true },
)

defineExpose({ results })
</script>

<template>
  <div class="filters card" :class="{ open: filtersOpen }">
    <div class="filter-row search-row">
      <input v-model="filters.q" type="search" placeholder="Search by name, keyword or ingredient…" class="grow" aria-label="Search recipes" />
      <select v-model="filters.sort" class="auto" aria-label="Sort by">
        <option v-for="s in SORTS" :key="s.value" :value="s.value">{{ s.label }}</option>
      </select>
      <button
        type="button" class="filters-toggle" :class="{ on: activeCount }" :aria-expanded="filtersOpen"
        @click="filtersOpen = !filtersOpen"
      >
        Filters<span v-if="activeCount"> ({{ activeCount }})</span>
      </button>
    </div>
    <div class="filter-body">
      <div v-if="proteins.length" class="chips" role="group" aria-label="Protein">
        <button
          v-for="p in proteins" :key="p.value" type="button" class="chip"
          :class="{ on: filters.proteins.includes(p.value) }" @click="toggle(filters.proteins, p.value)"
        >
          {{ p.icon }} {{ p.label }}
        </button>
      </div>
      <div v-if="tags.length" class="chips" role="group" aria-label="Style">
        <button
          v-for="t in tags" :key="t" type="button" class="chip tag"
          :class="{ on: filters.tags.includes(t) }" @click="toggle(filters.tags, t)"
        >
          {{ t }}
        </button>
      </div>
      <div class="filter-row">
        <select v-model="filters.maxCal" class="auto" aria-label="Calories">
          <option v-for="o in CALORIE_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
        <select v-model="filters.maxTime" class="auto" aria-label="Cooking time">
          <option v-for="o in TIME_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
        <button type="button" class="link small" @click="showMore = !showMore">{{ showMore ? 'Fewer filters' : 'Ingredients…' }}</button>
        <span class="spacer hide-phone"></span>
        <span class="muted small hide-phone">{{ results.length }} of {{ recipes.length }}</span>
        <button v-if="activeCount" type="button" class="small hide-phone" @click="clear">Clear filters</button>
      </div>
      <div v-if="showMore" class="filter-row">
        <input v-model="filters.withIng" list="ingredient-names" placeholder="Has ingredient (e.g. mushrooms)" class="grow" />
        <input v-model="filters.withoutIng" list="ingredient-names" placeholder="Without ingredient" class="grow" />
        <datalist id="ingredient-names">
          <option v-for="i in ingredients" :key="i" :value="i" />
        </datalist>
      </div>
    </div>
    <div class="filter-row count-row phone-only">
      <span class="muted small">{{ results.length }} of {{ recipes.length }}</span>
      <button v-if="activeCount" type="button" class="small" @click="clear">Clear filters</button>
    </div>
  </div>

  <p v-if="recipes.length && !results.length" class="card muted">
    No recipes match these filters. <button class="link" @click="clear">Clear filters</button>
  </p>
  <div class="recipe-grid">
    <template v-for="r in results" :key="r.id">
      <slot :recipe="r" />
    </template>
  </div>
</template>
