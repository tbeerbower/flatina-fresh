<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, errorMessage } from '../api'
import { addDays, formatDay, formatRange, toIso, weekStart } from '../dates'
import { amountText, buyText, isLink, orderText, packageText, productName } from '../orderList'
import ProductEditor from '../components/ProductEditor.vue'

const route = useRoute()
const router = useRouter()
const start = ref(weekStart(route.query.week || toIso(new Date())))
const end = computed(() => addDays(start.value, 6))
const list = ref(null)
const error = ref('')
// Per-week ticks: true = have it / got it, false = need it. Unset means the default for the item.
const checked = ref({})
const hideChecked = ref(false)
const editing = ref(null)
const copied = ref('')

const storageKey = computed(() => `flatina.shopping.${start.value}`)

function loadChecked() {
  try {
    checked.value = JSON.parse(localStorage.getItem(storageKey.value) || '{}')
  } catch {
    checked.value = {}
  }
}

/** Pantry items and staples start out as "have it"; everything else starts as "need it". */
function have(item) {
  const v = checked.value[item.key]
  return v === undefined ? item.pantry || item.staple : v
}

function toggle(item) {
  checked.value = { ...checked.value, [item.key]: !have(item) }
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
    if (hideChecked.value && have(item)) continue
    let g = out.find((x) => x.aisle === item.aisle)
    if (!g) out.push((g = { aisle: item.aisle, items: [] }))
    g.items.push(item)
  }
  return out
})

const toBuy = computed(() => (list.value ? list.value.items.filter((i) => !have(i)) : []))

async function load() {
  loadChecked()
  editing.value = null
  copied.value = ''
  try {
    const { data } = await api.get('/api/shopping-list', { params: { start: start.value, end: end.value } })
    list.value = data
    error.value = ''
  } catch (e) {
    error.value = errorMessage(e)
  }
}

/** A saved or deleted product changes every line for that ingredient, so reload the list. */
async function productChanged() {
  editing.value = null
  const { data } = await api.get('/api/shopping-list', { params: { start: start.value, end: end.value } })
  list.value = data
}

async function copyOrder() {
  const text = orderText(`Groceries for ${formatRange(start.value, end.value)}`, toBuy.value)
  try {
    await navigator.clipboard.writeText(text)
    copied.value = `Copied ${toBuy.value.length} item${toBuy.value.length === 1 ? '' : 's'}. Paste it into a note, or keep it open beside the store's site.`
  } catch {
    copied.value = 'Your browser blocked copying. The list is below to select by hand.'
    showText.value = true
  }
}
const showText = ref(false)
const orderPreview = computed(() => orderText(`Groceries for ${formatRange(start.value, end.value)}`, toBuy.value))

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
      <p class="muted">{{ formatRange(start, end) }}<span v-if="list"> · {{ toBuy.length }} item{{ toBuy.length === 1 ? '' : 's' }} to get</span></p>
    </div>
    <div class="actions no-print">
      <button @click="start = addDays(start, -7)">← Previous</button>
      <button @click="start = weekStart()">This week</button>
      <button @click="start = addDays(start, 7)">Next →</button>
      <label style="display: flex; gap: 0.3rem; align-items: center; margin: 0"><input v-model="hideChecked" type="checkbox" style="width: auto" /> Hide checked</label>
      <button @click="print">Print</button>
      <router-link to="/products" class="button">My products</router-link>
    </div>
  </div>
  <p v-if="error" class="error">{{ error }}</p>

  <template v-if="list">
    <div v-if="!list.meals.length" class="card muted">
      Nothing planned this week. <router-link :to="{ path: '/plan', query: { week: start } }">Plan some meals</router-link> first.
    </div>
    <template v-else>
      <div class="card" style="margin-bottom: 1rem">
        <strong>Meals:</strong>
        <span v-for="(m, i) in list.meals" :key="i" class="small">
          {{ formatDay(m.date, { weekday: 'short' }) }} {{ m.recipeTitle }} ({{ m.servings }})<span v-if="i < list.meals.length - 1">, </span>
        </span>
      </div>

      <div class="card order-bar no-print">
        <div>
          <strong>Ready to order?</strong>
          <div class="muted small">Tick what you already have. The copied list has your brands, sizes and package counts for everything left.</div>
        </div>
        <button class="primary" :disabled="!toBuy.length" @click="copyOrder">Copy order list ({{ toBuy.length }})</button>
        <p v-if="copied" class="notice small" style="flex-basis: 100%; margin: 0">{{ copied }}</p>
        <details v-if="toBuy.length" :open="showText" style="flex-basis: 100%" @toggle="showText = $event.target.open">
          <summary class="small">Show the list</summary>
          <pre class="order-text">{{ orderPreview }}</pre>
        </details>
      </div>
    </template>

    <div v-for="g in groups" :key="g.aisle" class="shopping-group">
      <h2>{{ g.aisle === 'Pantry' ? 'Check your pantry' : g.aisle }}</h2>
      <p v-if="g.aisle === 'Pantry'" class="muted small" style="margin-top: -0.4rem">
        Ticked means you have it. Untick anything you're out of to add it to the order.
      </p>
      <div v-for="item in g.items" :key="item.key" class="shopping-item" :class="{ done: have(item) }">
        <input type="checkbox" :checked="have(item)" :aria-label="`Have ${item.name}`" @change="toggle(item)" />
        <div style="flex: 1; min-width: 0">
          <div class="item-line">
            <span class="name">{{ productName(item) }}</span>
            <span v-if="buyText(item)"> · {{ buyText(item) }}</span>
            <button class="link small no-print" @click="editing = editing === item.key ? null : item.key">
              {{ item.product ? 'Edit product' : 'Set product' }}
            </button>
          </div>
          <div class="muted small">
            <span v-if="item.product && (item.product.brand || item.product.productName)">{{ item.name }}<span v-if="item.amounts.length">, </span></span>
            <span v-if="item.product && item.packages && item.amounts.length">recipes need {{ amountText(item) }} · </span>
            <span v-else-if="item.product && item.amounts.length && packageText(item.product)">can't count {{ packageText(item.product) }}s from these units · </span>
            {{ item.recipes.join(', ') }}
            <template v-if="item.product && item.product.storeItem">
              · <a v-if="isLink(item.product.storeItem)" :href="item.product.storeItem" target="_blank" rel="noopener">store page</a>
              <span v-else>#{{ item.product.storeItem.replace(/^#/, '') }}</span>
            </template>
          </div>
          <ProductEditor
            v-if="editing === item.key"
            :product="item.product"
            :ingredient-name="item.name"
            :suggested-unit="item.amounts.length ? item.amounts[0].unit || '' : ''"
            @saved="productChanged"
            @deleted="productChanged"
            @cancel="editing = null"
          />
        </div>
      </div>
    </div>
  </template>
</template>
