<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, errorMessage } from '../api'
import { addDays, formatDay, formatRange, toIso, weekDays, weekStart } from '../dates'

const route = useRoute()
const router = useRouter()
const start = ref(weekStart(route.query.week || toIso(new Date())))
const end = computed(() => addDays(start.value, 6))
const days = computed(() => weekDays(start.value))
const today = toIso(new Date())
const entries = ref([])
const recipes = ref([])
const adding = ref({})
const error = ref('')

const byDay = computed(() => {
  const map = {}
  for (const d of days.value) map[d] = []
  for (const e of entries.value) (map[e.date] ||= []).push(e)
  return map
})

async function load() {
  try {
    const { data } = await api.get('/api/plan', { params: { start: start.value, end: end.value } })
    entries.value = data
    error.value = ''
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function loadRecipes() {
  const { data } = await api.get('/api/recipes')
  recipes.value = data
}

function shiftWeek(delta) {
  start.value = addDays(start.value, 7 * delta)
}

watch(start, (s) => {
  router.replace({ query: { week: s } })
  load()
})

async function add(day) {
  const pick = adding.value[day]
  if (!pick || !pick.recipeId) return
  try {
    await api.post('/api/plan', { date: day, recipeId: pick.recipeId, servings: pick.servings || 2 })
    adding.value[day] = null
    load()
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function update(entry, changes) {
  try {
    await api.put(`/api/plan/${entry.id}`, { date: entry.date, servings: entry.servings, ...changes })
    load()
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function remove(entry) {
  await api.delete(`/api/plan/${entry.id}`)
  load()
}

onMounted(() => {
  load()
  loadRecipes().catch((e) => (error.value = errorMessage(e)))
})
</script>

<template>
  <div class="page-head">
    <div>
      <h1>Meal plan</h1>
      <p class="muted">{{ formatRange(start, end) }} · {{ entries.length }} meal{{ entries.length === 1 ? '' : 's' }}</p>
    </div>
    <div class="actions">
      <button @click="shiftWeek(-1)">← Previous</button>
      <button @click="start = weekStart()">This week</button>
      <button @click="shiftWeek(1)">Next →</button>
      <router-link :to="{ path: '/shopping', query: { week: start } }" class="button primary">Shopping list</router-link>
    </div>
  </div>
  <p v-if="error" class="error">{{ error }}</p>
  <p v-if="!recipes.length" class="notice">
    You don't have any recipes yet. <router-link to="/recipes">Add some</router-link> to start planning.
  </p>

  <div class="week">
    <div v-for="d in days" :key="d" class="day" :class="{ today: d === today }">
      <div class="day-head">{{ formatDay(d) }}</div>
      <div v-for="e in byDay[d]" :key="e.id" class="meal">
        <router-link :to="`/recipes/${e.recipeId}`">{{ e.recipeTitle }}</router-link>
        <div v-if="e.totalMinutes" class="muted small">{{ e.totalMinutes }} min</div>
        <div class="controls">
          <select :value="e.servings" @change="update(e, { servings: Number($event.target.value) })">
            <option :value="2">2 servings</option>
            <option :value="4">4 servings</option>
          </select>
          <select :value="e.date" title="Move to another day" @change="update(e, { date: $event.target.value })">
            <option v-for="o in days" :key="o" :value="o">{{ formatDay(o, { weekday: 'short' }) }}</option>
          </select>
          <button class="small danger" title="Remove" @click="remove(e)">✕</button>
        </div>
      </div>
      <div v-if="adding[d]" class="controls" style="display: grid; gap: 0.3rem">
        <select v-model.number="adding[d].recipeId">
          <option :value="null" disabled>Choose a recipe…</option>
          <option v-for="r in recipes" :key="r.id" :value="r.id">{{ r.title }}</option>
        </select>
        <select v-model.number="adding[d].servings">
          <option :value="2">2 servings</option>
          <option :value="4">4 servings</option>
        </select>
        <div class="actions">
          <button class="small primary" @click="add(d)">Add</button>
          <button class="small" @click="adding[d] = null">Cancel</button>
        </div>
      </div>
      <button v-else-if="recipes.length" class="small" style="margin-top: auto" @click="adding[d] = { recipeId: null, servings: 2 }">+ Add meal</button>
    </div>
  </div>
</template>
