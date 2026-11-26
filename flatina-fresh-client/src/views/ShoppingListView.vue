<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, errorMessage } from '../api'
import { addDays, formatDay, formatRange, toIso, weekStart } from '../dates'
import { formatAmount } from '../format'

const route = useRoute()
const router = useRouter()
const start = ref(weekStart(route.query.week || toIso(new Date())))
const end = computed(() => addDays(start.value, 6))
const list = ref(null)
const error = ref('')
const checked = ref({})
const hideChecked = ref(false)

const storageKey = computed(() => `flatina.shopping.${start.value}`)

function loadChecked() {
  try {
    checked.value = JSON.parse(localStorage.getItem(storageKey.value) || '{}')
  } catch {
    checked.value = {}
  }
}

function toggle(key) {
  checked.value = { ...checked.value, [key]: !checked.value[key] }
  try {
    localStorage.setItem(storageKey.value, JSON.stringify(checked.value))
  } catch {
    // storage unavailable; ticks just won't persist
  }
}

const groups = computed(() => {
  if (!list.value) return []
  const out = []
  for (const item of list.value.items) {
    if (hideChecked.value && checked.value[item.key]) continue
    let g = out.find((x) => x.aisle === item.aisle)
    if (!g) out.push((g = { aisle: item.aisle, items: [] }))
    g.items.push(item)
  }
  return out
})

const remaining = computed(() => (list.value ? list.value.items.filter((i) => !i.pantry && !checked.value[i.key]).length : 0))

function amounts(item) {
  return item.amounts.map((a) => formatAmount(a.quantity, a.unit)).join(' + ')
}

async function load() {
  loadChecked()
  try {
    const { data } = await api.get('/api/shopping-list', { params: { start: start.value, end: end.value } })
    list.value = data
    error.value = ''
  } catch (e) {
    error.value = errorMessage(e)
  }
}

watch(start, (s) => {
  router.replace({ query: { week: s } })
  load()
})
function print() {
  window.print()
}

onMounted(load)
</script>

<template>
  <div class="page-head">
    <div>
      <h1>Shopping list</h1>
      <p class="muted">{{ formatRange(start, end) }}<span v-if="list"> · {{ remaining }} item{{ remaining === 1 ? '' : 's' }} to get</span></p>
    </div>
    <div class="actions no-print">
      <button @click="start = addDays(start, -7)">← Previous</button>
      <button @click="start = weekStart()">This week</button>
      <button @click="start = addDays(start, 7)">Next →</button>
      <label style="display: flex; gap: 0.3rem; align-items: center; margin: 0"><input v-model="hideChecked" type="checkbox" style="width: auto" /> Hide checked</label>
      <button @click="print">Print</button>
    </div>
  </div>
  <p v-if="error" class="error">{{ error }}</p>

  <template v-if="list">
    <div v-if="!list.meals.length" class="card muted">
      Nothing planned this week. <router-link :to="{ path: '/plan', query: { week: start } }">Plan some meals</router-link> first.
    </div>
    <div v-else class="card" style="margin-bottom: 1rem">
      <strong>Meals:</strong>
      <span v-for="(m, i) in list.meals" :key="i" class="small">
        {{ formatDay(m.date, { weekday: 'short' }) }} {{ m.recipeTitle }} ({{ m.servings }})<span v-if="i < list.meals.length - 1">, </span>
      </span>
    </div>

    <div v-for="g in groups" :key="g.aisle" class="shopping-group">
      <h2>{{ g.aisle === 'Pantry' ? 'Check your pantry' : g.aisle }}</h2>
      <div v-for="item in g.items" :key="item.key" class="shopping-item" :class="{ done: checked[item.key] }">
        <input type="checkbox" :checked="checked[item.key]" @change="toggle(item.key)" />
        <div>
          <span class="name">{{ item.name }}</span>
          <span v-if="item.amounts.length"> · {{ amounts(item) }}</span>
          <div class="muted small">{{ item.recipes.join(', ') }}</div>
        </div>
      </div>
    </div>
  </template>
</template>
