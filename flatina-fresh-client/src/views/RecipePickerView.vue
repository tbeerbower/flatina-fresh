<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, errorMessage } from '../api'
import { addDays, formatDay, formatRange, isPastWeek, weekLabel, weekStart } from '../dates'
import RecipeBrowser from '../components/RecipeBrowser.vue'
import RecipeCard from '../components/RecipeCard.vue'

/** Pick recipes for a week. Every meal is planned on the week's Monday for now. */
const props = defineProps({ start: { type: String, required: true } })
const monday = computed(() => weekStart(props.start))

// Past weeks can't be changed; go to the week's page instead.
if (isPastWeek(weekStart(props.start))) useRouter().replace(`/weeks/${weekStart(props.start)}`)
const recipes = ref([])
const entries = ref([])
const busy = ref(new Set())
const loading = ref(true)
const error = ref('')

const entryByRecipe = computed(() => {
  const map = new Map()
  for (const e of entries.value) if (!map.has(e.recipeId)) map.set(e.recipeId, e)
  return map
})

async function loadWeek() {
  const { data } = await api.get('/api/plan', { params: { start: monday.value, end: addDays(monday.value, 6) } })
  entries.value = data
}

onMounted(async () => {
  try {
    const [r] = await Promise.all([api.get('/api/recipes'), loadWeek()])
    recipes.value = r.data
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
})

async function toggle(recipe) {
  if (busy.value.has(recipe.id)) return
  busy.value = new Set(busy.value).add(recipe.id)
  try {
    const existing = entryByRecipe.value.get(recipe.id)
    if (existing) await api.delete(`/api/plan/${existing.id}`)
    else await api.post('/api/plan', { date: monday.value, recipeId: recipe.id, servings: 2 })
    await loadWeek()
    error.value = ''
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    const next = new Set(busy.value)
    next.delete(recipe.id)
    busy.value = next
  }
}
</script>

<template>
  <div class="page-head">
    <div>
      <router-link :to="`/weeks/${monday}`" class="small">← {{ weekLabel(monday) }}</router-link>
      <h1>Add meals</h1>
      <p class="muted">
        {{ formatRange(monday, addDays(monday, 6)) }} ·
        {{ entries.length }} meal{{ entries.length === 1 ? '' : 's' }} picked
      </p>
    </div>
    <div class="actions">
      <router-link :to="`/weeks/${monday}`" class="button primary">Done</router-link>
    </div>
  </div>
  <p v-if="error" class="error">{{ error }}</p>
  <p v-if="!loading && !recipes.length" class="notice">
    You don't have any recipes yet. <router-link to="/recipes">Add some</router-link> to start planning.
  </p>
  <RecipeBrowser v-else v-slot="{ recipe }" :recipes="recipes">
    <RecipeCard :recipe="recipe" :to="`/recipes/${recipe.id}`" :selected="entryByRecipe.has(recipe.id)">
      <span class="muted small">
        <template v-if="recipe.lastPlanned">Last planned {{ formatDay(recipe.lastPlanned, { month: 'short', day: 'numeric' }) }}</template>
        <template v-else>Never planned</template>
      </span>
      <button
        class="small" :class="entryByRecipe.has(recipe.id) ? 'added' : 'primary'"
        :disabled="busy.has(recipe.id)" @click="toggle(recipe)"
      >
        {{ entryByRecipe.has(recipe.id) ? '✓ Added' : '+ Add' }}
      </button>
    </RecipeCard>
  </RecipeBrowser>
</template>
