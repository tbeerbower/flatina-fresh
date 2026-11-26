<script setup>
import { computed, onMounted, ref } from 'vue'
import { api, errorMessage } from '../api'
import { isLink, packageText } from '../orderList'
import ProductEditor from '../components/ProductEditor.vue'

const products = ref([])
const error = ref('')
const editing = ref(null)
const filter = ref('')

const shown = computed(() => {
  const f = filter.value.trim().toLowerCase()
  return products.value.filter(
    (p) => !f || [p.ingredientName, p.brand, p.productName].some((s) => s && s.toLowerCase().includes(f)),
  )
})

async function load() {
  editing.value = null
  try {
    const { data } = await api.get('/api/products')
    products.value = data
    error.value = ''
  } catch (e) {
    error.value = errorMessage(e)
  }
}

onMounted(load)
</script>

<template>
  <div class="page-head">
    <div>
      <h1>My products</h1>
      <p class="muted">The brands and sizes you buy, and the staples you always have. Set them from the shopping list, or add one here.</p>
    </div>
    <div class="actions">
      <input v-model="filter" placeholder="Search" style="max-width: 200px" />
      <button class="primary" @click="editing = 'new'">Add product</button>
      <router-link to="/shopping" class="button">Shopping list</router-link>
    </div>
  </div>
  <p v-if="error" class="error">{{ error }}</p>

  <div v-if="editing === 'new'" class="card" style="margin-bottom: 1rem">
    <ProductEditor @saved="load" @cancel="editing = null" />
  </div>

  <div v-if="!products.length && editing !== 'new'" class="card muted">
    No products yet. On the shopping list, choose <strong>Set product</strong> next to an ingredient to save the brand
    and size you buy.
  </div>

  <div v-for="p in shown" :key="p.id" class="shopping-item">
    <div style="flex: 1; min-width: 0">
      <div class="item-line">
        <span class="name">{{ p.ingredientName }}</span>
        <span v-if="p.brand || p.productName"> · {{ [p.brand, p.productName].filter(Boolean).join(' ') }}</span>
        <span v-if="packageText(p)" class="muted"> · {{ packageText(p) }}</span>
        <span v-if="p.staple" class="pill">Always have</span>
        <button class="link small" @click="editing = editing === p.id ? null : p.id">Edit</button>
      </div>
      <div v-if="p.storeItem" class="muted small">
        <a v-if="isLink(p.storeItem)" :href="p.storeItem" target="_blank" rel="noopener">{{ p.storeItem }}</a>
        <span v-else>#{{ p.storeItem.replace(/^#/, '') }}</span>
      </div>
      <ProductEditor v-if="editing === p.id" :product="p" @saved="load" @deleted="load" @cancel="editing = null" />
    </div>
  </div>
</template>
