<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api, errorMessage } from '../api'
import { addDays, formatRange, isIsoDate, weekLabel, weekStart } from '../dates'
import RecipeCard from '../components/RecipeCard.vue'

/** The recipes planned for one week, as cards. Without a start date it shows the current week. */
const props = defineProps({ start: { type: String, default: null } })
const router = useRouter()
const monday = computed(() => weekStart(isIsoDate(props.start) ? props.start : undefined))
const end = computed(() => addDays(monday.value, 6))
const entries = ref([])
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  try {
    const { data } = await api.get('/api/plan', { params: { start: monday.value, end: end.value } })
    entries.value = data
    error.value = ''
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}

watch(monday, load, { immediate: true })

function go(delta) {
  router.push(`/weeks/${addDays(monday.value, 7 * delta)}`)
}

async function setServings(entry, servings) {
  try {
    await api.put(`/api/plan/${entry.id}`, { date: entry.date, servings })
    entry.servings = servings
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function remove(entry) {
  try {
    await api.delete(`/api/plan/${entry.id}`)
    entries.value = entries.value.filter((e) => e.id !== entry.id)
  } catch (e) {
    error.value = errorMessage(e)
  }
}
</script>

<template>
  <div class="page-head">
    <div>
      <router-link :to="{ path: '/weeks', query: { week: monday } }" class="small">← All weeks</router-link>
      <h1>{{ weekLabel(monday) }}</h1>
      <p class="muted">
        {{ formatRange(monday, end) }} · {{ entries.length }} meal{{ entries.length === 1 ? '' : 's' }}
      </p>
    </div>
    <div class="actions">
      <button aria-label="Previous week" @click="go(-1)">←</button>
      <button aria-label="Next week" @click="go(1)">→</button>
      <router-link :to="{ path: '/shopping', query: { week: monday } }" class="button">Shopping list</router-link>
      <router-link :to="`/weeks/${monday}/add`" class="button primary">+ Add meals</router-link>
    </div>
  </div>
  <p v-if="error" class="error">{{ error }}</p>

  <div v-if="!loading && !entries.length" class="empty-week card">
    <p>No meals picked for this week yet.</p>
    <router-link :to="`/weeks/${monday}/add`" class="button primary">Choose recipes</router-link>
  </div>
  <div class="recipe-grid">
    <RecipeCard v-for="e in entries" :key="e.id" :recipe="e" :to="`/recipes/${e.recipeId}`">
      <select :value="e.servings" class="auto small-select" aria-label="Servings" @change="setServings(e, Number($event.target.value))">
        <option :value="2">2 servings</option>
        <option :value="4">4 servings</option>
      </select>
      <button class="small danger" title="Remove from this week" @click="remove(e)">Remove</button>
    </RecipeCard>
  </div>
</template>
